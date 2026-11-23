package com.greenmobility.modules.incentive.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.common.platform.service.OutboxService;
import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.modules.carbon.api.CarbonModuleApi;
import com.greenmobility.modules.incentive.domain.CarbonConversion;
import com.greenmobility.modules.incentive.dto.IncentiveDtos;
import com.greenmobility.modules.incentive.repository.CarbonConversionRepository;
import com.greenmobility.modules.incentive.repository.RewardRedemptionRepository;
import com.greenmobility.modules.ledger.api.LedgerModuleApi;
import com.greenmobility.modules.ledger.api.PostingEntry;
import com.greenmobility.modules.ledger.api.PostingRequest;
import com.greenmobility.modules.ledger.domain.*;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.ledger.repository.LedgerEntryRepository;
import com.greenmobility.modules.ledger.repository.LedgerTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class IncentiveWalletService {

    private static final Logger log = LoggerFactory.getLogger(IncentiveWalletService.class);
    private static final ZoneId GMT7_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LedgerModuleApi ledgerModuleApi;
    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final CarbonConversionRepository conversionRepository;
    private final RewardRedemptionRepository redemptionRepository;
    private final PlatformSettingsService settingsService;
    private final CarbonModuleApi carbonModuleApi;

    public IncentiveWalletService(LedgerModuleApi ledgerModuleApi,
                                  LedgerAccountRepository accountRepository,
                                  LedgerEntryRepository entryRepository,
                                  LedgerTransactionRepository transactionRepository,
                                  CarbonConversionRepository conversionRepository,
                                  RewardRedemptionRepository redemptionRepository,
                                  PlatformSettingsService settingsService,
                                  CarbonModuleApi carbonModuleApi) {
        this.ledgerModuleApi = ledgerModuleApi;
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.transactionRepository = transactionRepository;
        this.conversionRepository = conversionRepository;
        this.redemptionRepository = redemptionRepository;
        this.settingsService = settingsService;
        this.carbonModuleApi = carbonModuleApi;
    }

    @Transactional(readOnly = true)
    public IncentiveDtos.IncentiveSummaryDto getSummary(UUID userId) {
        Optional<LedgerAccount> pccAcc = ledgerModuleApi.findAccount(userId, AccountType.USER_CARBON_WALLET, "PCC");
        Optional<LedgerAccount> ptsAcc = ledgerModuleApi.findAccount(userId, AccountType.USER_POINTS_WALLET, "ECO_POINT");

        BigDecimal pccBalance = pccAcc.map(LedgerAccount::getBalance).orElse(BigDecimal.ZERO.setScale(4));
        boolean pccFrozen = pccAcc.map(LedgerAccount::isFrozen).orElse(false);
        String pccFrozenReason = pccAcc.map(LedgerAccount::getFrozenReason).orElse(null);

        int ecoPoints = ptsAcc.map(a -> a.getBalance().intValue()).orElse(0);
        boolean pointsFrozen = ptsAcc.map(LedgerAccount::isFrozen).orElse(false);

        Instant monthStart = ZonedDateTime.now(GMT7_ZONE).withDayOfMonth(1)
                .toLocalDate().atStartOfDay(GMT7_ZONE).toInstant();
        BigDecimal monthlyConverted = conversionRepository.sumConvertedPccSince(userId, monthStart);
        BigDecimal monthlyCap = settingsService.getPccConvertMonthlyCap();
        BigDecimal minConvert = settingsService.getPccConvertMin();
        long rateVnd = settingsService.getPccToVndRate();

        long availableVouchers = redemptionRepository
                .findByUserIdFiltered(userId, "AVAILABLE", PageRequest.of(0, 1))
                .getTotalElements();

        BigDecimal totalCo2 = carbonModuleApi != null
                ? carbonModuleApi.getCustomerLifetimeCo2SavedGrams(userId)
                : BigDecimal.ZERO;

        return new IncentiveDtos.IncentiveSummaryDto(
                pccBalance,
                pccFrozen,
                pccFrozenReason,
                ecoPoints,
                pointsFrozen,
                monthlyConverted,
                monthlyCap,
                minConvert,
                rateVnd,
                settingsService.isRedemptionsFrozen(),
                availableVouchers,
                totalCo2
        );
    }

    @Transactional(readOnly = true)
    public IncentiveDtos.LedgerPageResponse getLedgerHistory(UUID userId, String currencyFilter, String txType,
                                                             Instant fromTime, Instant toTime, String cursor, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 50);

        List<UUID> accountIds = new ArrayList<>();
        if (currencyFilter == null || currencyFilter.isBlank() || "PCC".equalsIgnoreCase(currencyFilter)) {
            ledgerModuleApi.findAccount(userId, AccountType.USER_CARBON_WALLET, "PCC")
                    .ifPresent(a -> accountIds.add(a.getId()));
        }
        if (currencyFilter == null || currencyFilter.isBlank() || "ECO_POINT".equalsIgnoreCase(currencyFilter)) {
            ledgerModuleApi.findAccount(userId, AccountType.USER_POINTS_WALLET, "ECO_POINT")
                    .ifPresent(a -> accountIds.add(a.getId()));
        }

        if (accountIds.isEmpty()) {
            return new IncentiveDtos.LedgerPageResponse(List.of(), null, false);
        }

        Instant cursorTime = null;
        UUID cursorId = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
                String[] parts = decoded.split("\\|");
                if (parts.length == 2) {
                    cursorTime = Instant.parse(parts[0]);
                    cursorId = UUID.fromString(parts[1]);
                }
            } catch (Exception ignored) {}
        }

        List<LedgerEntry> rows = entryRepository.findFilteredEntriesForAccounts(
                accountIds,
                (txType != null && !txType.isBlank()) ? txType.trim().toUpperCase() : null,
                fromTime, toTime, cursorTime, cursorId, pageSize + 1
        );

        boolean hasMore = rows.size() > pageSize;
        List<LedgerEntry> slice = hasMore ? rows.subList(0, pageSize) : rows;

        Map<UUID, LedgerAccount> accCache = new HashMap<>();
        accountRepository.findAllById(accountIds).forEach(a -> accCache.put(a.getId(), a));
        Map<UUID, LedgerTransaction> txCache = new HashMap<>();
        List<UUID> txIds = slice.stream().map(LedgerEntry::getTransactionId).distinct().toList();
        if (!txIds.isEmpty()) {
            transactionRepository.findAllById(txIds).forEach(t -> txCache.put(t.getId(), t));
        }

        List<IncentiveDtos.IncentiveLedgerEntryDto> items = slice.stream().map(e -> {
            LedgerAccount acc = accCache.get(e.getAccountId());
            LedgerTransaction tx = txCache.get(e.getTransactionId());
            return new IncentiveDtos.IncentiveLedgerEntryDto(
                    e.getId(),
                    acc != null ? acc.getCurrency() : "UNKNOWN",
                    e.getEntryType().name(),
                    e.getAmount(),
                    e.getBalanceAfter(),
                    tx != null ? tx.getTransactionCode() : "",
                    tx != null ? tx.getTransactionType().name() : "",
                    tx != null ? tx.getDescription() : "",
                    tx != null ? tx.getTripId() : null,
                    e.getCreatedAt()
            );
        }).toList();

        String nextCursor = null;
        if (hasMore && !slice.isEmpty()) {
            LedgerEntry last = slice.get(slice.size() - 1);
            String raw = last.getCreatedAt().toString() + "|" + last.getId().toString();
            nextCursor = Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        return new IncentiveDtos.LedgerPageResponse(items, nextCursor, hasMore);
    }

    @Transactional
    public IncentiveDtos.ConvertPccResponse convertPccToVnd(UUID userId, String userRole,
                                                            BigDecimal rawPccAmount, String idempotencyKey) {
        if (settingsService.isRedemptionsFrozen()) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "REDEMPTION_FROZEN_CIRCUIT_BREAKER",
                    "Hệ thống đổi thưởng Carbon đang tạm khóa để kiểm toán an toàn");
        }
        if (rawPccAmount == null || rawPccAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "PCC_INVALID_AMOUNT",
                    "Số lượng PCC quy đổi phải lớn hơn 0");
        }

        BigDecimal pccAmount = rawPccAmount.setScale(4, RoundingMode.FLOOR);
        BigDecimal minConvert = settingsService.getPccConvertMin();
        if (pccAmount.compareTo(minConvert) < 0) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "PCC_BELOW_MINIMUM",
                    "Số lượng PCC quy đổi tối thiểu mỗi lần là " + minConvert + " PCC");
        }

        // Acquire FOR UPDATE lock BEFORE loading LedgerAccount into L1 cache to avoid StaleObjectStateException
        String pccAccountNumber = "WALLET:PCC:" + userId;
        LedgerAccount lockedPccAcc = accountRepository.findByAccountNumberWithLock(pccAccountNumber).orElse(null);
        if (lockedPccAcc == null) {
            ledgerModuleApi.getOrCreateCarbonWallet(userId);
            lockedPccAcc = accountRepository.findByAccountNumberWithLock(pccAccountNumber)
                    .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "Không tìm thấy ví PCC"));
        }

        if (lockedPccAcc.isFrozen()) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "WALLET_FROZEN",
                    "Ví Carbon của bạn đang tạm khóa kiểm tra: " + lockedPccAcc.getFrozenReason());
        }

        Instant monthStart = ZonedDateTime.now(GMT7_ZONE).withDayOfMonth(1)
                .toLocalDate().atStartOfDay(GMT7_ZONE).toInstant();
        BigDecimal alreadyConvertedThisMonth = conversionRepository.sumConvertedPccSince(userId, monthStart);
        BigDecimal monthlyCap = settingsService.getPccConvertMonthlyCap();

        if (alreadyConvertedThisMonth.add(pccAmount).compareTo(monthlyCap) > 0) {
            BigDecimal remainingCap = monthlyCap.subtract(alreadyConvertedThisMonth).max(BigDecimal.ZERO);
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "PCC_MONTHLY_CAP_EXCEEDED",
                    "Vượt hạn mức quy đổi Carbon tháng (" + monthlyCap + " PCC). Hạn mức còn lại: " + remainingCap + " PCC");
        }

        if (lockedPccAcc.getBalance().compareTo(pccAmount) < 0) {
            throw new PaymentBusinessException(HttpStatus.CONFLICT, "WALLET_INSUFFICIENT_BALANCE",
                    "Số dư Tín chỉ Carbon không đủ (hiện có: " + lockedPccAcc.getBalance() + " PCC)");
        }

        long rateVnd = settingsService.getPccToVndRate();
        long vndAmount = pccAmount.multiply(BigDecimal.valueOf(rateVnd))
                .setScale(0, RoundingMode.FLOOR)
                .longValue();
        if (vndAmount <= 0) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "PCC_CONVERSION_TOO_SMALL",
                    "Giá trị quy đổi VNĐ phải lớn hơn 0đ");
        }

        boolean isDriver = userRole != null && userRole.toUpperCase().contains("DRIVER");
        LedgerAccount targetVndAcc = isDriver
                ? ledgerModuleApi.getOrCreateDriverEarningsAccount(userId)
                : ledgerModuleApi.getOrCreateUserWallet(userId);
        String targetAccountType = targetVndAcc.getAccountType().name();

        LedgerAccount retiredPcc = ledgerModuleApi.getSystemAccount(AccountType.SYSTEM_CARBON_RETIRED);
        LedgerAccount buybackVnd = ledgerModuleApi.getSystemAccount(AccountType.CARBON_BUYBACK_POOL);

        String baseKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : UUID.randomUUID().toString();

        LedgerTransaction pccBurnTx = ledgerModuleApi.post(new PostingRequest(
                null, null, LedgerTxType.PCC_CONVERT_BURN,
                "Quy doi " + pccAmount + " PCC sang " + vndAmount + "d",
                "CONV-PCC-" + baseKey,
                "CARBON_CONVERSION", null, null,
                List.of(
                        new PostingEntry(lockedPccAcc.getId(), EntryType.DEBIT, pccAmount),
                        new PostingEntry(retiredPcc.getId(), EntryType.CREDIT, pccAmount)
                )
        ));

        LedgerTransaction vndPayoutTx = ledgerModuleApi.post(new PostingRequest(
                null, null, LedgerTxType.PCC_CONVERT_PAYOUT,
                "Chi tra tro gia xanh " + vndAmount + "d tu quy doi " + pccAmount + " PCC",
                "CONV-VND-" + baseKey,
                "CARBON_CONVERSION", null, null,
                List.of(
                        new PostingEntry(buybackVnd.getId(), EntryType.DEBIT, BigDecimal.valueOf(vndAmount)),
                        new PostingEntry(targetVndAcc.getId(), EntryType.CREDIT, BigDecimal.valueOf(vndAmount))
                )
        ));

        CarbonConversion conversion = new CarbonConversion();
        conversion.setUserId(userId);
        conversion.setUserRole(userRole != null ? userRole : "ROLE_CUSTOMER");
        conversion.setTargetAccountType(targetAccountType);
        conversion.setPccAmount(pccAmount);
        conversion.setRateVndPerPcc(rateVnd);
        conversion.setVndAmount(vndAmount);
        conversion.setPccLedgerTxId(pccBurnTx.getId());
        conversion.setVndLedgerTxId(vndPayoutTx.getId());
        conversion.setStatus("COMPLETED");
        CarbonConversion saved = conversionRepository.save(conversion);

        LedgerAccount refreshedPcc = accountRepository.findById(lockedPccAcc.getId()).orElse(lockedPccAcc);
        LedgerAccount refreshedVnd = accountRepository.findById(targetVndAcc.getId()).orElse(targetVndAcc);

        log.info("User {} converted {} PCC -> {} VND into {}", userId, pccAmount, vndAmount, targetAccountType);
        return new IncentiveDtos.ConvertPccResponse(
                saved.getId(),
                pccAmount,
                rateVnd,
                vndAmount,
                targetAccountType,
                refreshedPcc.getBalance(),
                refreshedVnd.getBalance().longValue(),
                saved.getCreatedAt()
        );
    }
}
