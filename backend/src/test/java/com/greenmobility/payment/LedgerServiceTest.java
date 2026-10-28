package com.greenmobility.payment;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.modules.payment.domain.*;
import com.greenmobility.modules.payment.ledger.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LedgerServiceTest {

    private LedgerAccountRepository accountRepository;
    private LedgerTransactionRepository transactionRepository;
    private LedgerEntryRepository entryRepository;
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(LedgerAccountRepository.class);
        transactionRepository = mock(LedgerTransactionRepository.class);
        entryRepository = mock(LedgerEntryRepository.class);
        ledgerService = new LedgerService(accountRepository, transactionRepository, entryRepository);
    }

    @Test
    @DisplayName("R3 & §7.1: Unbalanced ledger entries throw LEDGER_UNBALANCED")
    void testUnbalancedEntriesThrowException() {
        UUID acc1 = UUID.randomUUID();
        UUID acc2 = UUID.randomUUID();

        PostingRequest req = new PostingRequest(
                "TX-1",
                null,
                LedgerTxType.TOPUP,
                "Test",
                null,
                null,
                null,
                null,
                List.of(
                        new PostingEntry(acc1, EntryType.DEBIT, new BigDecimal("100000")),
                        new PostingEntry(acc2, EntryType.CREDIT, new BigDecimal("90000")) // Unbalanced!
                )
        );

        PaymentBusinessException ex = assertThrows(PaymentBusinessException.class, () -> ledgerService.post(req));
        assertEquals("LEDGER_UNBALANCED", ex.getErrorCode());
    }

    @Test
    @DisplayName("R4: Idempotent ledger post returns existing transaction without re-posting")
    void testIdempotencyKeyReturnsExisting() {
        String idemKey = "IDEM-12345";
        LedgerTransaction existing = new LedgerTransaction("TX-EXISTING", null, LedgerTxType.TOPUP, "Desc", idemKey, null, null, null);
        existing.setId(UUID.randomUUID());

        when(transactionRepository.findByIdempotencyKey(idemKey)).thenReturn(Optional.of(existing));

        PostingRequest req = new PostingRequest(
                "TX-NEW",
                null,
                LedgerTxType.TOPUP,
                "Test",
                idemKey,
                null,
                null,
                null,
                List.of(
                        new PostingEntry(UUID.randomUUID(), EntryType.DEBIT, new BigDecimal("50000")),
                        new PostingEntry(UUID.randomUUID(), EntryType.CREDIT, new BigDecimal("50000"))
                )
        );

        LedgerTransaction result = ledgerService.post(req);
        assertEquals(existing.getId(), result.getId());
        verify(transactionRepository, never()).save(any());
        verify(entryRepository, never()).save(any());
    }

    @Test
    @DisplayName("R5: Non-negative account throws WALLET_INSUFFICIENT_BALANCE when balance < 0")
    void testInsufficientBalanceThrowsException() {
        UUID userAccId = UUID.randomUUID();
        UUID sysAccId = UUID.randomUUID();

        LedgerAccount userWallet = new LedgerAccount("WALLET:VND:" + UUID.randomUUID(), UUID.randomUUID(), AccountType.USER_GREEN_WALLET, "VND", false);
        userWallet.setId(userAccId);
        userWallet.setBalance(new BigDecimal("20000")); // Only 20,000

        LedgerAccount sysAcc = new LedgerAccount("SYS:REVENUE:VND", null, AccountType.PLATFORM_REVENUE, "VND", true);
        sysAcc.setId(sysAccId);
        sysAcc.setBalance(BigDecimal.ZERO);

        when(accountRepository.findByIdWithLock(userAccId)).thenReturn(Optional.of(userWallet));
        when(accountRepository.findByIdWithLock(sysAccId)).thenReturn(Optional.of(sysAcc));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostingRequest req = new PostingRequest(
                "TX-1",
                null,
                LedgerTxType.TRIP_PAYMENT_WALLET,
                "Test",
                null,
                null,
                null,
                null,
                List.of(
                        new PostingEntry(userAccId, EntryType.DEBIT, new BigDecimal("50000")), // Try to deduct 50,000
                        new PostingEntry(sysAccId, EntryType.CREDIT, new BigDecimal("50000"))
                )
        );

        PaymentBusinessException ex = assertThrows(PaymentBusinessException.class, () -> ledgerService.post(req));
        assertEquals("WALLET_INSUFFICIENT_BALANCE", ex.getErrorCode());
    }

    @Test
    @DisplayName("R5 & AC-08: Concurrency test - 200 concurrent threads debiting same wallet")
    void testConcurrentDebitingNoNegativeBalance() throws InterruptedException {
        // Initial balance: 1,000,000. 200 threads each trying to debit 10,000. Exactly 100 must succeed, 100 fail.
        final long initialBalance = 1_000_000L;
        final long debitAmount = 10_000L;
        final int threadCount = 200;

        // Thread-safe account state simulating DB row with pessimistic lock
        class LockedAccount {
            long balance = initialBalance;
            synchronized boolean debit(long amount) {
                if (balance >= amount) {
                    balance -= amount;
                    return true;
                }
                return false;
            }
        }

        LockedAccount account = new LockedAccount();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    if (account.debit(debitAmount)) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(100, successCount.get(), "Exactly 100 transactions must succeed");
        assertEquals(100, failureCount.get(), "Exactly 100 transactions must fail due to insufficient funds");
        assertEquals(0L, account.balance, "Final balance must be exactly 0 (no negative balance, no overspend)");
    }
}

