package com.greenmobility.modules.payment.dto;

import java.time.Instant;
import java.util.UUID;

public class WithdrawalDtos {

    public record CreateWithdrawalRequest(
            long amountVnd,
            String bankCode,
            String accountNo,
            String accountHolder
    ) {}

    public record WithdrawalResponse(
            UUID id,
            UUID driverUserId,
            long amountVnd,
            String bankCode,
            String bankAccountLast4,
            String accountHolderName,
            String status,
            Instant createdAt,
            Instant reviewedAt,
            String rejectReason
    ) {}

    public record RejectWithdrawalRequest(
            String reason
    ) {}

    public record ApproveWithdrawalRequest(
            String bankReference
    ) {}
}

