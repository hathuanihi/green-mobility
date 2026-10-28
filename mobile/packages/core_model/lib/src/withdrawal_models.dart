// Sprint 5: Driver Withdrawal models

class WithdrawalRequestModel {
  final int amountVnd;
  final String bankCode;
  final String bankAccountNumber;
  final String accountHolderName;

  const WithdrawalRequestModel({
    required this.amountVnd,
    required this.bankCode,
    required this.bankAccountNumber,
    required this.accountHolderName,
  });

  Map<String, dynamic> toJson() => {
    'amountVnd': amountVnd,
    'bankCode': bankCode,
    'bankAccountNumber': bankAccountNumber,
    'accountHolderName': accountHolderName,
  };
}

class WithdrawalItemModel {
  final String id;
  final int amountVnd;
  final String bankCode;
  final String bankAccountLast4;
  final String accountHolderName;
  final String status; // PENDING, APPROVED_PAID, REJECTED, CANCELLED
  final String createdAt;
  final String? reviewedAt;
  final String? rejectReason;

  const WithdrawalItemModel({
    required this.id,
    required this.amountVnd,
    required this.bankCode,
    required this.bankAccountLast4,
    required this.accountHolderName,
    required this.status,
    required this.createdAt,
    this.reviewedAt,
    this.rejectReason,
  });

  factory WithdrawalItemModel.fromJson(Map<String, dynamic> json) {
    return WithdrawalItemModel(
      id: json['id']?.toString() ?? '',
      amountVnd: (json['amountVnd'] is num)
          ? (json['amountVnd'] as num).toInt()
          : int.tryParse(json['amountVnd']?.toString() ?? '0') ?? 0,
      bankCode: json['bankCode']?.toString() ?? '',
      bankAccountLast4: json['bankAccountLast4']?.toString() ?? '****',
      accountHolderName: json['accountHolderName']?.toString() ?? '',
      status: json['status']?.toString() ?? 'PENDING',
      createdAt: json['createdAt']?.toString() ?? '',
      reviewedAt: json['reviewedAt']?.toString(),
      rejectReason: json['rejectReason']?.toString(),
    );
  }
}

