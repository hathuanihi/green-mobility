package com.greenmobility.modules.payment.service;

import com.greenmobility.common.platform.service.PlatformSettingsService;
import com.greenmobility.modules.ledger.domain.AccountType;
import com.greenmobility.modules.ledger.domain.EntryType;
import com.greenmobility.modules.ledger.domain.LedgerAccount;
import com.greenmobility.modules.ledger.domain.LedgerEntry;
import com.greenmobility.modules.ledger.domain.LedgerTransaction;
import com.greenmobility.modules.ledger.repository.LedgerAccountRepository;
import com.greenmobility.modules.ledger.repository.LedgerEntryRepository;
import com.greenmobility.modules.ledger.repository.LedgerTransactionRepository;
import com.greenmobility.modules.payment.dto.WalletDtos;
import com.greenmobility.modules.payment.repository.WithdrawalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final PlatformSettingsService settingsService;

    public WalletService(LedgerAccountRepository accountRepository,
                         LedgerEntryRepository entryRepository,
                         LedgerTransactionRepository transactionRepository,
                         WithdrawalRequestRepository withdrawalRepository,
                         PlatformSettingsService settingsService) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
        this.transactionRepository = transactionRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public WalletDtos.WalletSummaryResponse getWalletSummary(UUID userId) {
        Optional<LedgerAccount> walletOpt = accountRepository.findByOwnerIdAndAccountTypeAndCurrency(
                userId, AccountType.USER_GREEN_WALLET, "VND");
        UUID walletId = walletOpt.map(LedgerAccount::getId).orElse(null);
        long walletBal = walletOpt.map(a -> a.getBalance().longValue()).orElse(0L);

        Optional<LedgerAccount> driverAccOpt = accountRepository.findByOwnerIdAndAccountTypeAndCurrency(
                userId, AccountType.DRIVER_EARNINGS, "VND");

        Long earningsBal = null;
        long cashDebt = 0L;
        boolean cashTripsAllowed = true;
        long holdVnd = 0L;

        if (driverAccOpt.isPresent()) {
            long rawEarnings = driverAccOpt.get().getBalance().longValue();
            earningsBal = rawEarnings;
            if (rawEarnings < 0) {
                cashDebt = Math.abs(rawEarnings);
            }
            long limit = settingsService.getCashDebtLimitVnd();
            if (cashDebt > limit) {
                cashTripsAllowed = false;
            }
            Long pendingHold = withdrawalRepository.calculatePendingHoldAmount(userId);
            holdVnd = pendingHold != null ? pendingHold : 0L;
        }

        return new WalletDtos.WalletSummaryResponse(
                walletId,
                walletBal,
                earningsBal,
                holdVnd,
                cashDebt,
                cashTripsAllowed
        );
    }

    @Transactional(readOnly = true)
    public WalletDtos.CursorPaginationResponse<WalletDtos.WalletTransactionResponse> getTransactionHistory(
            UUID userId, String type, Instant from, Instant to, int limit, String cursor
    ) {
        Optional<LedgerAccount> walletOpt = accountRepository.findByOwnerIdAndAccountTypeAndCurrency(
                userId, AccountType.USER_GREEN_WALLET, "VND");
        if (walletOpt.isEmpty()) {
            return new WalletDtos.CursorPaginationResponse<>(List.of(), null, false);
        }

        int pageSize = Math.min(Math.max(limit, 1), 50);
        Instant cursorTime = null;
        UUID cursorId = null;

        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
                String[] parts = decoded.split("\\|");
                cursorTime = Instant.parse(parts[0]);
                if (parts.length > 1) {
                    cursorId = UUID.fromString(parts[1]);
                }
            } catch (Exception ignored) {
                try {
                    cursorTime = Instant.parse(cursor);
                } catch (Exception ignored2) {}
            }
        }

        String normalizedType = (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type))
                ? type.trim().toUpperCase() : null;

        List<LedgerEntry> entries = entryRepository.findFilteredEntriesForAccounts(
                List.of(walletOpt.get().getId()),
                normalizedType,
                from,
                to,
                cursorTime,
                cursorId,
                pageSize + 1
        );

        boolean hasMore = entries.size() > pageSize;
        List<LedgerEntry> pageItems = hasMore ? entries.subList(0, pageSize) : entries;

        Set<UUID> txIds = pageItems.stream().map(LedgerEntry::getTransactionId).collect(Collectors.toSet());
        Map<UUID, LedgerTransaction> txMap = txIds.isEmpty()
                ? Map.of()
                : transactionRepository.findAllById(txIds).stream()
                        .collect(Collectors.toMap(LedgerTransaction::getId, Function.identity()));

        List<WalletDtos.WalletTransactionResponse> dtoList = new ArrayList<>();
        for (LedgerEntry e : pageItems) {
            LedgerTransaction tx = txMap.get(e.getTransactionId());
            String txType = tx != null ? tx.getTransactionType().name() : "TRANSACTION";
            String desc = tx != null ? tx.getDescription() : "";
            String refType = tx != null ? tx.getReferenceType() : null;
            UUID refId = tx != null ? tx.getReferenceId() : null;

            String direction = (e.getEntryType() == EntryType.CREDIT) ? "IN" : "OUT";
            dtoList.add(new WalletDtos.WalletTransactionResponse(
                    e.getId(),
                    direction,
                    e.getAmount().longValue(),
                    e.getBalanceAfter() != null ? e.getBalanceAfter().longValue() : 0L,
                    txType,
                    desc,
                    refType,
                    refId,
                    e.getCreatedAt()
            ));
        }

        String nextCursor = null;
        if (hasMore && !pageItems.isEmpty()) {
            LedgerEntry last = pageItems.get(pageItems.size() - 1);
            String raw = last.getCreatedAt().toString() + "|" + last.getId().toString();
            nextCursor = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        return new WalletDtos.CursorPaginationResponse<>(dtoList, nextCursor, hasMore);
    }
}
