// Sprint 5: Green Wallet & Ledger models

class WalletBalanceModel {
  final int balanceVnd;
  final int pendingHoldVnd;
  final int cashDebtVnd;
  final int availableBalanceVnd;
  final String currency;
  final String updatedAt;

  const WalletBalanceModel({
    required this.balanceVnd,
    this.pendingHoldVnd = 0,
    this.cashDebtVnd = 0,
    required this.availableBalanceVnd,
    this.currency = 'VND',
    required this.updatedAt,
  });

  factory WalletBalanceModel.fromJson(Map<String, dynamic> json) {
    return WalletBalanceModel(
      balanceVnd: (json['balanceVnd'] is num)
          ? (json['balanceVnd'] as num).toInt()
          : int.tryParse(json['balanceVnd']?.toString() ?? '0') ?? 0,
      pendingHoldVnd: (json['pendingHoldVnd'] is num)
          ? (json['pendingHoldVnd'] as num).toInt()
          : int.tryParse(json['pendingHoldVnd']?.toString() ?? '0') ?? 0,
      cashDebtVnd: (json['cashDebtVnd'] is num)
          ? (json['cashDebtVnd'] as num).toInt()
          : int.tryParse(json['cashDebtVnd']?.toString() ?? '0') ?? 0,
      availableBalanceVnd: (json['availableBalanceVnd'] is num)
          ? (json['availableBalanceVnd'] as num).toInt()
          : int.tryParse(json['availableBalanceVnd']?.toString() ?? '0') ?? 0,
      currency: json['currency']?.toString() ?? 'VND',
      updatedAt: json['updatedAt']?.toString() ?? DateTime.now().toIso8601String(),
    );
  }

  Map<String, dynamic> toJson() => {
    'balanceVnd': balanceVnd,
    'pendingHoldVnd': pendingHoldVnd,
    'cashDebtVnd': cashDebtVnd,
    'availableBalanceVnd': availableBalanceVnd,
    'currency': currency,
    'updatedAt': updatedAt,
  };
}

class WalletTransactionModel {
  final String id;
  final String type; // TOPUP, TRIP_FARE, TRIP_EARNING, COMMISSION, WITHDRAWAL, REFUND
  final int amountVnd;
  final String direction; // CREDIT, DEBIT
  final int balanceAfterVnd;
  final String description;
  final String? referenceId;
  final String createdAt;

  const WalletTransactionModel({
    required this.id,
    required this.type,
    required this.amountVnd,
    required this.direction,
    required this.balanceAfterVnd,
    required this.description,
    this.referenceId,
    required this.createdAt,
  });

  factory WalletTransactionModel.fromJson(Map<String, dynamic> json) {
    return WalletTransactionModel(
      id: json['id']?.toString() ?? '',
      type: json['type']?.toString() ?? 'TRANSACTION',
      amountVnd: (json['amountVnd'] is num)
          ? (json['amountVnd'] as num).toInt()
          : int.tryParse(json['amountVnd']?.toString() ?? '0') ?? 0,
      direction: json['direction']?.toString() ?? 'CREDIT',
      balanceAfterVnd: (json['balanceAfterVnd'] is num)
          ? (json['balanceAfterVnd'] as num).toInt()
          : int.tryParse(json['balanceAfterVnd']?.toString() ?? '0') ?? 0,
      description: json['description']?.toString() ?? '',
      referenceId: json['referenceId']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

class WalletTransactionsCursorResponse {
  final List<WalletTransactionModel> items;
  final String? nextCursor;
  final bool hasMore;

  const WalletTransactionsCursorResponse({
    required this.items,
    this.nextCursor,
    required this.hasMore,
  });

  factory WalletTransactionsCursorResponse.fromJson(Map<String, dynamic> json) {
    final list = (json['items'] as List<dynamic>?)
            ?.map((e) => WalletTransactionModel.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [];
    return WalletTransactionsCursorResponse(
      items: list,
      nextCursor: json['nextCursor']?.toString(),
      hasMore: json['hasMore'] == true,
    );
  }
}

class TopupInitiateRequest {
  final int amountVnd;
  final String gateway; // VNPAY, MOMO
  final String? returnUrl;

  const TopupInitiateRequest({
    required this.amountVnd,
    required this.gateway,
    this.returnUrl,
  });

  Map<String, dynamic> toJson() => {
    'amountVnd': amountVnd,
    'gateway': gateway,
    if (returnUrl != null) 'returnUrl': returnUrl,
  };
}

class TopupInitiateResponse {
  final String topupId;
  final int amountVnd;
  final String payUrl;
  final String? deepLink;
  final String? qrCodeUrl;
  final String expiresAt;

  const TopupInitiateResponse({
    required this.topupId,
    required this.amountVnd,
    required this.payUrl,
    this.deepLink,
    this.qrCodeUrl,
    required this.expiresAt,
  });

  factory TopupInitiateResponse.fromJson(Map<String, dynamic> json) {
    return TopupInitiateResponse(
      topupId: json['topupId']?.toString() ?? '',
      amountVnd: (json['amountVnd'] is num)
          ? (json['amountVnd'] as num).toInt()
          : int.tryParse(json['amountVnd']?.toString() ?? '0') ?? 0,
      payUrl: json['payUrl']?.toString() ?? '',
      deepLink: json['deepLink']?.toString(),
      qrCodeUrl: json['qrCodeUrl']?.toString(),
      expiresAt: json['expiresAt']?.toString() ?? '',
    );
  }
}

