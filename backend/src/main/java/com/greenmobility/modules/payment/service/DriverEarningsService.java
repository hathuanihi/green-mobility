package com.greenmobility.modules.payment.service;

import com.greenmobility.common.exception.PaymentBusinessException;
import com.greenmobility.modules.payment.domain.AccountType;
import com.greenmobility.modules.payment.domain.LedgerAccount;
import com.greenmobility.modules.payment.domain.Payment;
import com.greenmobility.modules.payment.dto.DriverEarningsDtos;
import com.greenmobility.modules.payment.dto.WalletDtos;
import com.greenmobility.modules.payment.ledger.LedgerAccountRepository;
import com.greenmobility.modules.payment.repository.PaymentRepository;
import com.greenmobility.modules.trip.api.TripModuleApi;
import com.greenmobility.modules.trip.api.TripSnapshotDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DriverEarningsService {

    private static final Logger log = LoggerFactory.getLogger(DriverEarningsService.class);
    private static final ZoneId GMT7_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PaymentRepository paymentRepository;
    private final LedgerAccountRepository accountRepository;
    private final PlatformSettingsService settingsService;
    private final PaymentService paymentService;
    private final TripModuleApi tripModuleApi;

    public DriverEarningsService(PaymentRepository paymentRepository,
                                 LedgerAccountRepository accountRepository,
                                 PlatformSettingsService settingsService,
                                 PaymentService paymentService,
                                 TripModuleApi tripModuleApi) {
        this.paymentRepository = paymentRepository;
        this.accountRepository = accountRepository;
        this.settingsService = settingsService;
        this.paymentService = paymentService;
        this.tripModuleApi = tripModuleApi;
    }

    @Transactional(readOnly = true)
    public DriverEarningsDtos.EarningsSummaryResponse getSummary(UUID driverUserId, String range) {
        ZonedDateTime now = ZonedDateTime.now(GMT7_ZONE);
        ZonedDateTime startTime;

        if ("TODAY".equalsIgnoreCase(range)) {
            startTime = now.toLocalDate().atStartOfDay(GMT7_ZONE);
        } else if ("MONTH".equalsIgnoreCase(range)) {
            startTime = now.toLocalDate().withDayOfMonth(1).atStartOfDay(GMT7_ZONE);
        } else { // Default: WEEK (last 7 days)
            startTime = now.minusDays(6).toLocalDate().atStartOfDay(GMT7_ZONE);
        }

        Instant fromInstant = startTime.toInstant();
        Instant toInstant = now.toInstant();

        // In production/sandbox, we aggregate from payments where driver_user_id = driverUserId and status = PAID
        // For simplicity and efficiency, query payments in window
        List<Payment> paidPayments = paymentRepository.findAll().stream()
                .filter(p -> driverUserId.equals(p.getDriverUserId()))
                .filter(p -> "PAID".equals(p.getStatus()))
                .filter(p -> p.getPaidAt() != null && !p.getPaidAt().isBefore(fromInstant) && !p.getPaidAt().isAfter(toInstant))
                .toList();

        long grossFare = 0;
        long commission = 0;
        long netEarnings = 0;
        long cashCollected = 0;
        long onlineCollected = 0;

        Map<String, DailyAccumulator> dailyMap = new LinkedHashMap<>();

        // Initialize series days
        LocalDate curDate = startTime.toLocalDate();
        LocalDate endDate = now.toLocalDate();
        while (!curDate.isAfter(endDate)) {
            dailyMap.put(curDate.format(DATE_FMT), new DailyAccumulator());
            curDate = curDate.plusDays(1);
        }

        for (Payment p : paidPayments) {
            long fare = p.getAmountVnd();
            long comm = p.getCommissionVnd() != null ? p.getCommissionVnd() : 0L;
            long net = p.getDriverAmountVnd() != null ? p.getDriverAmountVnd() : (fare - comm);

            grossFare += fare;
            commission += comm;
            netEarnings += net;

            if ("CASH".equalsIgnoreCase(p.getMethod())) {
                cashCollected += fare;
            } else {
                onlineCollected += fare;
            }

            if (p.getPaidAt() != null) {
                String dayKey = p.getPaidAt().atZone(GMT7_ZONE).toLocalDate().format(DATE_FMT);
                DailyAccumulator acc = dailyMap.get(dayKey);
                if (acc != null) {
                    acc.netVnd += net;
                    acc.trips += 1;
                }
            }
        }

        int tripCount = paidPayments.size();
        long avgPerTrip = tripCount > 0 ? (netEarnings / tripCount) : 0L;

        List<DriverEarningsDtos.DailySeries> series = dailyMap.entrySet().stream()
                .map(e -> new DriverEarningsDtos.DailySeries(e.getKey(), e.getValue().netVnd, e.getValue().trips))
                .toList();

        return new DriverEarningsDtos.EarningsSummaryResponse(
                grossFare,
                commission,
                netEarnings,
                tripCount,
                cashCollected,
                onlineCollected,
                avgPerTrip,
                series
        );
    }

    @Transactional(readOnly = true)
    public WalletDtos.CursorPaginationResponse<DriverEarningsDtos.DriverTripEarningResponse> getTrips(
            UUID driverUserId, int limit, String cursor) {
        int pageSize = Math.min(Math.max(limit, 1), 50);

        List<Payment> allPaid = paymentRepository.findAll().stream()
                .filter(p -> driverUserId.equals(p.getDriverUserId()))
                .filter(p -> "PAID".equals(p.getStatus()))
                .sorted(Comparator.comparing(Payment::getPaidAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<DriverEarningsDtos.DriverTripEarningResponse> dtoList = new ArrayList<>();
        for (Payment p : allPaid) {
            String tripCode = tripModuleApi.getTrip(p.getTripId()).map(TripSnapshotDto::tripCode).orElse("N/A");
            dtoList.add(new DriverEarningsDtos.DriverTripEarningResponse(
                    p.getTripId(),
                    tripCode,
                    p.getAmountVnd(),
                    p.getCommissionVnd() != null ? p.getCommissionVnd() : 0,
                    p.getDriverAmountVnd() != null ? p.getDriverAmountVnd() : 0,
                    p.getMethod(),
                    p.getPaidAt()
            ));
        }

        return new WalletDtos.CursorPaginationResponse<>(dtoList, null, false);
    }

    @Transactional(readOnly = true)
    public DriverEarningsDtos.CashEligibilityResponse getEligibility(UUID driverUserId) {
        Optional<LedgerAccount> accOpt = accountRepository.findByOwnerIdAndAccountTypeAndCurrency(
                driverUserId, AccountType.DRIVER_EARNINGS, "VND"
        );

        long debt = 0;
        if (accOpt.isPresent() && accOpt.get().getBalance().longValue() < 0) {
            debt = Math.abs(accOpt.get().getBalance().longValue());
        }

        long limit = settingsService.getCashDebtLimitVnd();
        boolean allowed = debt <= limit;

        return new DriverEarningsDtos.CashEligibilityResponse(allowed, debt, limit);
    }

    @Transactional
    public void confirmCashCollected(UUID driverUserId, UUID tripId) {
        Payment payment = paymentRepository.findActivePaymentByTripIdWithLock(tripId)
                .orElseThrow(() -> new PaymentBusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy thanh toán"));

        if (payment.getDriverUserId() == null || !payment.getDriverUserId().equals(driverUserId)) {
            throw new PaymentBusinessException(HttpStatus.FORBIDDEN, "PAYMENT_FORBIDDEN", "Bạn không phải tài xế của chuyến đi này");
        }

        if (!"CASH".equalsIgnoreCase(payment.getMethod())) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "PAYMENT_INVALID_METHOD", "Chuyến đi này không thanh toán bằng tiền mặt");
        }

        if ("PAID".equals(payment.getStatus())) {
            log.info("Trip {} cash payment is already paid. Idempotent return.", tripId);
            return;
        }

        if (!"PENDING".equals(payment.getStatus())) {
            throw new PaymentBusinessException(HttpStatus.BAD_REQUEST, "PAYMENT_INVALID_STATUS", "Giao dịch không ở trạng thái chờ thu tiền mặt");
        }

        paymentService.settle(payment.getId(), "CASH_COLLECTED_CONFIRMED", null);
        log.info("Driver {} confirmed cash collection for trip {}", driverUserId, tripId);
    }

    private static class DailyAccumulator {
        long netVnd = 0;
        int trips = 0;
    }
}

