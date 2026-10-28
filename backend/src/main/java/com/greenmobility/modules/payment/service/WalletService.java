package com.greenmobility.modules.payment.service;

import com.greenmobility.modules.payment.domain.AccountType;
import com.greenmobility.modules.payment.domain.EntryType;
import com.greenmobility.modules.payment.domain.LedgerAccount;
import com.greenmobility.modules.payment.domain.LedgerEntry;
import com.greenmobility.modules.payment.domain.LedgerTransaction;
import com.greenmobility.modules.payment.dto.WalletDtos;
import com.greenmobility.modules.payment.ledger.LedgerAccountRepository;
import com.greenmobility.modules.payment.ledger.LedgerEntryRepository;
import com.greenmobility.modules.payment.ledger.LedgerService;
import com.greenmobility.modules.payment.ledger.LedgerTransactionRepository;
import com.greenmobility.modules.payment.repository.WithdrawalRequestRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class WalletService {

    private final LedgerService ledgerService;
    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final PlatformSettingsService settingsService;

    public WalletService(LedgerService ledgerService,
                         LedgerAccountRepository accountRepository,
                         LedgerEntryRepository entryRepository,
                         LedgerTransactionRepository transactionRepository,
                         WithdrawalRequestRepository withdrawalRepository,
                         PlatformSettingsService settingsService) {
        this.ledgerService = ledgerService;
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.transactionRepository = transactionRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public WalletDtos.WalletSummaryResponse getWalletSummary(UUID userId) {
        LedgerAccount userWallet = ledgerService.getOrCreateUserWallet(userId);
        long walletBal = userWallet.getBalance().longValue();

        Optional<LedgerAccount> driverAccOpt = accountRepository.findByOwnerIdAndAccountTypeAndCurrency(
                userId, AccountType.DRIVER_EARNINGS, "VND"
        );

        Long earningsBal = null;
        long cashDebt = 0;
        boolean cashTripsAllowed = true;

        if (driverAccOpt.isPresent()) {
            LedgerAccount driverAcc = driverAccOpt.get();
            long rawEarnings = driverAcc.getBalance().longValue();
            earningsBal = rawEarnings;
            if (rawEarnings < 0) {
                cashDebt = Math.abs(rawEarnings);
            }
            long limit = settingsService.getCashDebtLimitVnd();
            if (cashDebt > limit) {
                cashTripsAllowed = false;
            }
        }

        return new WalletDtos.WalletSummaryResponse(
                userWallet.getId(),
                walletBal,
                earningsBal,
                0L,
                cashDebt,
                cashTripsAllowed
        );
    }

    @Transactional(readOnly = true)
    public WalletDtos.CursorPaginationResponse<WalletDtos.WalletTransactionResponse> getTransactionHistory(
            UUID userId, String type, Instant from, Instant to, int limit, String cursor
    ) {
        LedgerAccount userWallet = ledgerService.getOrCreateUserWallet(userId);
        int pageSize = Math.min(Math.max(limit, 1), 50);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        Instant cursorTime = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                cursorTime = Instant.parse(new String(Base64.getDecoder().decode(cursor)));
            } catch (Exception ignored) {
                try {
                    cursorTime = Instant.parse(cursor);
                } catch (Exception ignored2) {}
            }
        }

        List<LedgerEntry> entries;
        if (cursorTime != null) {
            entries = entryRepository.findByAccountIdWithCursor(userWallet.getId(), cursorTime, pageable);
        } else {
            entries = entryRepository.findByAccountIdRecent(userWallet.getId(), pageable);
        }

        boolean hasMore = entries.size() > pageSize;
        List<LedgerEntry> pageItems = hasMore ? entries.subList(0, pageSize) : entries;

        List<WalletDtos.WalletTransactionResponse> dtoList = new ArrayList<>();
        for (LedgerEntry e : pageItems) {
            LedgerTransaction tx = transactionRepository.findById(e.getTransactionId()).orElse(null);
            String txType = tx != null ? tx.getTransactionType().name() : "TRANSACTION";
            String desc = tx != null ? tx.getDescription() : "";
            String refType = tx != null ? tx.getReferenceType() : null;
            UUID refId = tx != null ? tx.getReferenceId() : null;

            String direction = (e.getEntryType() == EntryType.CREDIT) ? "IN" : "OUT";
            dtoList.add(new WalletDtos.WalletTransactionResponse(
                    e.getId(),
                    direction,
                    e.getAmount().longValue(),
                    e.getBalanceAfter() != null ? e.getBalanceAfter().longValue() : 0,
                    txType,
                    desc,
                    refType,
                    refId,
                    e.getCreatedAt()
            ));
        }

        String nextCursor = null;
        if (hasMore && !pageItems.isEmpty()) {
            Instant lastItemTime = pageItems.get(pageItems.size() - 1).getCreatedAt();
            nextCursor = Base64.getEncoder().encodeToString(lastItemTime.toString().getBytes());
        }

        return new WalletDtos.CursorPaginationResponse<>(dtoList, nextCursor, hasMore);
    }
}

