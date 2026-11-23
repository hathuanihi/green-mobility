package com.greenmobility.modules.ledger.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.platform.service.AuditLogService;
import com.greenmobility.modules.ledger.api.LedgerModuleApi;
import com.greenmobility.modules.ledger.api.PostingEntry;
import com.greenmobility.modules.ledger.api.PostingRequest;
import com.greenmobility.modules.ledger.domain.*;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.ledger.repository.LedgerEntryRepository;
import com.greenmobility.modules.ledger.repository.LedgerTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class LedgerService implements LedgerModuleApi {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerAccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final AuditLogService auditLogService;

    public LedgerService(LedgerAccountRepository accountRepository,
                         LedgerTransactionRepository transactionRepository,
                         LedgerEntryRepository entryRepository) {
        this(accountRepository, transactionRepository, entryRepository, null);
    }

    @Autowired
    public LedgerService(LedgerAccountRepository accountRepository,
                         LedgerTransactionRepository transactionRepository,
                         LedgerEntryRepository entryRepository,
                         @Autowired(required = false) AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public LedgerTransaction post(PostingRequest request) {
        // 1. Idempotency check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<LedgerTransaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Ledger transaction already exists for idempotencyKey={}, txId={}",
                        request.idempotencyKey(), existing.get().getId());
                return existing.get();
            }
        }

        if (request.entries() == null || request.entries().size() < 2) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "LEDGER_INVALID_ENTRIES",
                    "Giao dịch sổ cái phải có ít nhất 2 bút toán đối ứng");
        }

        // 2. Collect distinct account IDs and lock in ascending UUID order (deadlock-free)
        List<UUID> sortedAccountIds = request.entries().stream()
                .map(PostingEntry::accountId)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        Map<UUID, LedgerAccount> accountMap = new LinkedHashMap<>();
        String txCurrencyCode = null;
        for (UUID accountId : sortedAccountIds) {
            LedgerAccount account = accountRepository.findByIdWithLock(accountId)
                    .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "LEDGER_ACCOUNT_NOT_FOUND",
                            "Không tìm thấy tài khoản sổ cái: " + accountId));
            if (txCurrencyCode == null) {
                txCurrencyCode = account.getCurrency();
            } else if (!txCurrencyCode.equalsIgnoreCase(account.getCurrency())) {
                throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "LEDGER_INVALID_CURRENCY",
                        String.format("Không được phép trộn lẫn tiền tệ trong cùng 1 giao dịch (%s vs %s)",
                                txCurrencyCode, account.getCurrency()));
            }
            accountMap.put(accountId, account);
        }

        LedgerCurrency currency = LedgerCurrency.fromCode(txCurrencyCode);
        int scale = currency.scale();

        // 3. Scale FIRST, then validate positive amounts & zero-sum balance (R2)
        List<BigDecimal> scaledAmounts = new ArrayList<>(request.entries().size());
        BigDecimal sumDebit = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;

        for (PostingEntry entry : request.entries()) {
            BigDecimal scaled = entry.amount().setScale(scale, RoundingMode.HALF_UP);
            if (scaled.compareTo(BigDecimal.ZERO) <= 0) {
                throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "LEDGER_INVALID_AMOUNT",
                        "Số tiền bút toán sau làm tròn phải lớn hơn 0");
            }
            scaledAmounts.add(scaled);
            if (entry.entryType() == EntryType.DEBIT) {
                sumDebit = sumDebit.add(scaled);
            } else if (entry.entryType() == EntryType.CREDIT) {
                sumCredit = sumCredit.add(scaled);
            }
        }

        if (sumDebit.compareTo(sumCredit) != 0) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "LEDGER_UNBALANCED",
                    String.format("Bút toán sổ cái không cân bằng: Tổng Nợ (%s) != Tổng Có (%s)", sumDebit, sumCredit));
        }

        // 4. Create LedgerTransaction
        String txCode = request.transactionCode();
        if (txCode == null || txCode.isBlank()) {
            txCode = "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        }

        LedgerTransaction tx = new LedgerTransaction(
                txCode,
                request.tripId(),
                request.transactionType(),
                request.description(),
                request.idempotencyKey(),
                request.referenceType(),
                request.referenceId(),
                request.reversalOf()
        );
        LedgerTransaction savedTx = transactionRepository.save(tx);

        // 5. Apply each entry, enforce frozen/non-negative checks, save entries
        for (int i = 0; i < request.entries().size(); i++) {
            PostingEntry entry = request.entries().get(i);
            BigDecimal amount = scaledAmounts.get(i).setScale(4, RoundingMode.HALF_UP);
            LedgerAccount account = accountMap.get(entry.accountId());

            if (!request.bypassFrozenCheck() && account.isFrozen() && entry.entryType() == EntryType.DEBIT) {
                throw new PaymentBusinessException(HttpStatus.CONFLICT, "WALLET_FROZEN",
                        "Ví đang bị đóng băng do kiểm tra an toàn: "
                                + (account.getFrozenReason() != null ? account.getFrozenReason() : account.getAccountNumber()));
            }

            BigDecimal currentBalance = account.getBalance();
            BigDecimal newBalance = (entry.entryType() == EntryType.CREDIT)
                    ? currentBalance.add(amount)
                    : currentBalance.subtract(amount);

            if (!account.isAllowNegative() && newBalance.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Insufficient funds for account {}: current={}, required deduction={}",
                        account.getAccountNumber(), currentBalance, amount);
                throw new PaymentBusinessException(HttpStatus.CONFLICT, "WALLET_INSUFFICIENT_BALANCE",
                        "Số dư tài khoản không đủ để thực hiện giao dịch: " + account.getAccountNumber());
            }

            account.setBalance(newBalance);
            accountRepository.save(account);

            LedgerEntry ledgerEntry = new LedgerEntry(
                    savedTx.getId(),
                    account.getId(),
                    entry.entryType(),
                    amount,
                    newBalance
            );
            entryRepository.save(ledgerEntry);
        }

        log.info("Posted ledger tx code={}, id={}, currency={}, type={}, totalDebit={}",
                savedTx.getTransactionCode(), savedTx.getId(), currency.name(), savedTx.getTransactionType(), sumDebit);
        return savedTx;
    }

    @Transactional
    public LedgerAccount getOrCreateAccount(String accountNumber, UUID ownerId, AccountType accountType,
                                            String currency, boolean allowNegative) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseGet(() -> {
                    accountRepository.insertIfNotExists(accountNumber, ownerId, accountType.name(), currency, allowNegative);
                    return accountRepository.findByAccountNumber(accountNumber)
                            .orElseGet(() -> accountRepository.save(
                                    new LedgerAccount(accountNumber, ownerId, accountType, currency, allowNegative)));
                });
    }

    @Override
    @Transactional
    public LedgerAccount getOrCreateUserWallet(UUID userId) {
        return getOrCreateAccount("WALLET:VND:" + userId, userId, AccountType.USER_GREEN_WALLET, "VND", false);
    }

    @Override
    @Transactional
    public LedgerAccount getOrCreateDriverEarningsAccount(UUID driverUserId) {
        return getOrCreateAccount("EARN:VND:" + driverUserId, driverUserId, AccountType.DRIVER_EARNINGS, "VND", true);
    }

    @Override
    @Transactional
    public LedgerAccount getOrCreateCarbonWallet(UUID userId) {
        return getOrCreateAccount("WALLET:PCC:" + userId, userId, AccountType.USER_CARBON_WALLET, "PCC", false);
    }

    @Override
    @Transactional
    public LedgerAccount getOrCreatePointsWallet(UUID userId) {
        return getOrCreateAccount("WALLET:PTS:" + userId, userId, AccountType.USER_POINTS_WALLET, "ECO_POINT", false);
    }

    @Override
    @Transactional
    public void provisionUserWallets(UUID userId, boolean isDriver) {
        getOrCreateUserWallet(userId);
        getOrCreateCarbonWallet(userId);
        getOrCreatePointsWallet(userId);
        if (isDriver) {
            getOrCreateDriverEarningsAccount(userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LedgerAccount> findAccount(UUID ownerId, AccountType accountType, String currency) {
        return accountRepository.findByOwnerIdAndAccountTypeAndCurrency(ownerId, accountType, currency);
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerAccount getSystemAccount(AccountType accountType) {
        String accNum = switch (accountType) {
            case PLATFORM_REVENUE -> "SYS:REVENUE:VND";
            case GATEWAY_CLEARING -> "SYS:GATEWAY_CLEARING:VND";
            case PAYOUT_CLEARING -> "SYS:PAYOUT_CLEARING:VND";
            case EXTERNAL_BANK -> "SYS:EXTERNAL_BANK:VND";
            case PROMO_EXPENSE -> "SYS:PROMO_EXPENSE:VND";
            case CARBON_BUYBACK_POOL -> "SYS:CARBON_BUYBACK:VND";
            case SYSTEM_CARBON_RESERVE -> "SYS:CARBON_RESERVE:PCC";
            case SYSTEM_CARBON_RETIRED -> "SYS:CARBON_RETIRED:PCC";
            case SYSTEM_LOYALTY_RESERVE -> "SYS:LOYALTY_RESERVE:ECO_POINT";
            case SYSTEM_BURN_ACCOUNT -> "SYS:ECO_BURN:ECO_POINT";
            default -> throw new IllegalArgumentException("Specify currency for system account type: " + accountType);
        };
        return accountRepository.findByAccountNumber(accNum)
                .orElseThrow(() -> new IllegalStateException("System account missing from database: " + accNum));
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerAccount getSystemAccount(AccountType accountType, String currency) {
        String accNum = switch (accountType) {
            case PARTNER_REDEMPTION_POOL -> "SYS:PARTNER_POOL:" + currency.toUpperCase();
            default -> getSystemAccount(accountType).getAccountNumber();
        };
        return accountRepository.findByAccountNumber(accNum)
                .orElseThrow(() -> new IllegalStateException("System account missing from database: " + accNum));
    }

    @Override
    @Transactional
    public LedgerTransaction reverseTransaction(UUID adminId, String adminEmail, UUID transactionId,
                                              String reason, String ipAddress) {
        if (reason == null || reason.trim().length() < 10) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "LEDGER_REVERSAL_INVALID_REASON",
                    "Lý do đảo giao dịch bắt buộc tối thiểu 10 ký tự");
        }
        LedgerTransaction orig = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "LEDGER_TX_NOT_FOUND",
                        "Không tìm thấy giao dịch sổ cái: " + transactionId));

        if (transactionRepository.existsByReversalOf(transactionId)) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "LEDGER_ALREADY_REVERSED",
                    "Giao dịch này đã được đảo trước đó");
        }
        if (orig.getTransactionType() == LedgerTxType.ADMIN_REVERSAL) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "LEDGER_ALREADY_REVERSED",
                    "Không thể đảo một giao dịch ADMIN_REVERSAL");
        }

        List<LedgerEntry> origEntries = entryRepository.findByTransactionId(transactionId);
        if (origEntries.size() < 2) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "LEDGER_INVALID_ENTRIES",
                    "Giao dịch gốc không đủ bút toán để đảo");
        }

        List<PostingEntry> reversedEntries = origEntries.stream()
                .map(e -> new PostingEntry(
                        e.getAccountId(),
                        e.getEntryType() == EntryType.DEBIT ? EntryType.CREDIT : EntryType.DEBIT,
                        e.getAmount()
                ))
                .toList();

        PostingRequest req = new PostingRequest(
                null,
                orig.getTripId(),
                LedgerTxType.ADMIN_REVERSAL,
                "Dao giao dich " + orig.getTransactionCode() + ": " + reason.trim(),
                "ADMIN-REV-" + orig.getId(),
                "ADMIN_REVERSAL",
                orig.getId(),
                orig.getId(),
                reversedEntries,
                true
        );
        LedgerTransaction revTx = post(req);

        if (auditLogService != null) {
            auditLogService.record(adminId, adminEmail, "ADMIN_LEDGER_REVERSAL",
                    "LedgerTransaction", orig.getId().toString(),
                    Map.of("originalTxCode", orig.getTransactionCode(), "type", orig.getTransactionType().name()),
                    Map.of("reversalTxId", revTx.getId(), "reversalTxCode", revTx.getTransactionCode(), "reason", reason.trim()),
                    ipAddress);
        }
        return revTx;
    }

    @Override
    @Transactional
    public LedgerAccount freezeUserCarbonWallets(UUID userId, UUID adminId, String adminEmail,
                                                   String reason, String ipAddress) {
        String cleanReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Tạm đóng băng kiểm tra gian lận";
        LedgerAccount pccWallet = getOrCreateCarbonWallet(userId);
        LedgerAccount ptsWallet = getOrCreatePointsWallet(userId);
        Instant now = Instant.now();

        for (LedgerAccount acc : List.of(pccWallet, ptsWallet)) {
            acc.setStatus("FROZEN");
            acc.setFrozenReason(cleanReason);
            acc.setFrozenBy(adminId);
            acc.setFrozenAt(now);
            accountRepository.save(acc);
        }

        if (auditLogService != null) {
            auditLogService.record(adminId, adminEmail, "FREEZE_CARBON_WALLET",
                    "User", userId.toString(),
                    Map.of("status", "ACTIVE"),
                    Map.of("status", "FROZEN", "reason", cleanReason),
                    ipAddress);
        }
        return pccWallet;
    }

    @Override
    @Transactional
    public LedgerAccount unfreezeUserCarbonWallets(UUID userId, UUID adminId, String adminEmail,
                                                     String reason, String ipAddress) {
        LedgerAccount pccWallet = getOrCreateCarbonWallet(userId);
        LedgerAccount ptsWallet = getOrCreatePointsWallet(userId);

        for (LedgerAccount acc : List.of(pccWallet, ptsWallet)) {
            acc.setStatus("ACTIVE");
            acc.setFrozenReason(null);
            acc.setFrozenBy(null);
            acc.setFrozenAt(null);
            accountRepository.save(acc);
        }

        if (auditLogService != null) {
            auditLogService.record(adminId, adminEmail, "UNFREEZE_CARBON_WALLET",
                    "User", userId.toString(),
                    Map.of("status", "FROZEN"),
                    Map.of("status", "ACTIVE", "reason", reason != null ? reason : "Mở khóa ví"),
                    ipAddress);
        }
        return pccWallet;
    }
}
