package com.greenmobility.modules.payment.ledger;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.modules.payment.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerAccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;

    public LedgerService(LedgerAccountRepository accountRepository,
                         LedgerTransactionRepository transactionRepository,
                         LedgerEntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * Post a double-entry ledger transaction atomically.
     * Guaranteed:
     * 1. Idempotent by idempotency_key
     * 2. Sum Debit == Sum Credit
     * 3. Accounts locked in ascending UUID order (Deadlock-free)
     * 4. Balance updated credit-normal: balance += (CREDIT ? +amount : -amount)
     * 5. Non-negative balance check unless allow_negative is true
     */
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

        // 2. Validate entries: >=2 entries, amount > 0, sum(Debit) == sum(Credit)
        if (request.entries() == null || request.entries().size() < 2) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "LEDGER_INVALID_ENTRIES",
                    "Giao dịch sổ cái phải có ít nhất 2 bút toán đối ứng");
        }

        BigDecimal sumDebit = BigDecimal.ZERO;
        BigDecimal sumCredit = BigDecimal.ZERO;

        for (PostingEntry entry : request.entries()) {
            if (entry.amount() == null || entry.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "LEDGER_INVALID_AMOUNT",
                        "Số tiền bút toán phải lớn hơn 0");
            }
            if (entry.entryType() == EntryType.DEBIT) {
                sumDebit = sumDebit.add(entry.amount());
            } else if (entry.entryType() == EntryType.CREDIT) {
                sumCredit = sumCredit.add(entry.amount());
            }
        }

        if (sumDebit.compareTo(sumCredit) != 0) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "LEDGER_UNBALANCED",
                    String.format("Bút toán sổ cái không cân bằng: Tổng Nợ (%s) != Tổng Có (%s)", sumDebit, sumCredit));
        }

        // 3. Collect distinct account IDs and sort in ascending order (R5: deadlock prevention)
        List<UUID> sortedAccountIds = request.entries().stream()
                .map(PostingEntry::accountId)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        Map<UUID, LedgerAccount> accountMap = new LinkedHashMap<>();
        for (UUID accountId : sortedAccountIds) {
            LedgerAccount account = accountRepository.findByIdWithLock(accountId)
                    .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "LEDGER_ACCOUNT_NOT_FOUND",
                            "Không tìm thấy tài khoản sổ cái: " + accountId));
            accountMap.put(accountId, account);
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

        // 5. Apply each entry, compute balance_after, check negative
        for (PostingEntry entry : request.entries()) {
            LedgerAccount account = accountMap.get(entry.accountId());
            BigDecimal amount = entry.amount().setScale(4, RoundingMode.HALF_UP);

            BigDecimal currentBalance = account.getBalance();
            BigDecimal newBalance;
            if (entry.entryType() == EntryType.CREDIT) {
                newBalance = currentBalance.add(amount);
            } else {
                newBalance = currentBalance.subtract(amount);
            }

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

        log.info("Posted ledger tx code={}, id={}, type={}, totalDebit={}",
                savedTx.getTransactionCode(), savedTx.getId(), savedTx.getTransactionType(), sumDebit);
        return savedTx;
    }

    /**
     * Get or lazily create USER_GREEN_WALLET for user.
     */
    @Transactional
    public LedgerAccount getOrCreateUserWallet(UUID userId) {
        String accNum = "WALLET:VND:" + userId;
        return accountRepository.findByAccountNumber(accNum)
                .orElseGet(() -> {
                    LedgerAccount newAcc = new LedgerAccount(accNum, userId, AccountType.USER_GREEN_WALLET, "VND", false);
                    return accountRepository.save(newAcc);
                });
    }

    /**
     * Get or lazily create DRIVER_EARNINGS account for driver user.
     */
    @Transactional
    public LedgerAccount getOrCreateDriverEarningsAccount(UUID driverUserId) {
        String accNum = "EARN:VND:" + driverUserId;
        return accountRepository.findByAccountNumber(accNum)
                .orElseGet(() -> {
                    // allowNegative=true so driver can accumulate cash debt up to configured limit
                    LedgerAccount newAcc = new LedgerAccount(accNum, driverUserId, AccountType.DRIVER_EARNINGS, "VND", true);
                    return accountRepository.save(newAcc);
                });
    }

    /**
     * Retrieve system account (e.g., PLATFORM_REVENUE, GATEWAY_CLEARING, PAYOUT_CLEARING, EXTERNAL_BANK).
     */
    @Transactional(readOnly = true)
    public LedgerAccount getSystemAccount(AccountType accountType) {
        String accNum = switch (accountType) {
            case PLATFORM_REVENUE -> "SYS:REVENUE:VND";
            case GATEWAY_CLEARING -> "SYS:GATEWAY_CLEARING:VND";
            case PAYOUT_CLEARING -> "SYS:PAYOUT_CLEARING:VND";
            case EXTERNAL_BANK -> "SYS:EXTERNAL_BANK:VND";
            default -> throw new IllegalArgumentException("Not a system account type: " + accountType);
        };

        return accountRepository.findByAccountNumber(accNum)
                .orElseThrow(() -> new IllegalStateException("System account missing from database: " + accNum));
    }
}

