// Sprint 5: Payment models

enum PaymentMethod {
  cash('CASH'),
  greenWallet('GREEN_WALLET'),
  vnpay('VNPAY'),
  momo('MOMO');

  final String value;
  const PaymentMethod(this.value);

  static PaymentMethod fromString(String val) {
    switch (val.toUpperCase()) {
      case 'GREEN_WALLET':
        return PaymentMethod.greenWallet;
      case 'VNPAY':
        return PaymentMethod.vnpay;
      case 'MOMO':
        return PaymentMethod.momo;
      case 'CASH':
      default:
        return PaymentMethod.cash;
    }
  }
}

enum PaymentStatus {
  pending('PENDING'),
  paid('PAID'),
  failed('FAILED'),
  refunded('REFUNDED'),
  partiallyRefunded('PARTIALLY_REFUNDED');

  final String value;
  const PaymentStatus(this.value);

  static PaymentStatus fromString(String val) {
    switch (val.toUpperCase()) {
      case 'PAID':
        return PaymentStatus.paid;
      case 'FAILED':
        return PaymentStatus.failed;
      case 'REFUNDED':
        return PaymentStatus.refunded;
      case 'PARTIALLY_REFUNDED':
        return PaymentStatus.partiallyRefunded;
      case 'PENDING':
      default:
        return PaymentStatus.pending;
    }
  }
}

class PaymentResponseModel {
  final String paymentId;
  final String tripId;
  final PaymentMethod method;
  final PaymentStatus status;
  final int amountVnd;
  final int commissionVnd;
  final int driverAmountVnd;
  final String? payUrl;
  final String? deepLink;
  final String? qrCodeUrl;
  final String? expiresAt;
  final String? paidAt;
  final String? createdAt;

  const PaymentResponseModel({
    required this.paymentId,
    required this.tripId,
    required this.method,
    required this.status,
    required this.amountVnd,
    this.commissionVnd = 0,
    this.driverAmountVnd = 0,
    this.payUrl,
    this.deepLink,
    this.qrCodeUrl,
    this.expiresAt,
    this.paidAt,
    this.createdAt,
  });

  factory PaymentResponseModel.fromJson(Map<String, dynamic> json) {
    return PaymentResponseModel(
      paymentId: json['paymentId']?.toString() ?? '',
      tripId: json['tripId']?.toString() ?? '',
      method: PaymentMethod.fromString(json['method']?.toString() ?? 'CASH'),
      status: PaymentStatus.fromString(json['status']?.toString() ?? 'PENDING'),
      amountVnd: (json['amountVnd'] is num)
          ? (json['amountVnd'] as num).toInt()
          : int.tryParse(json['amountVnd']?.toString() ?? '0') ?? 0,
      commissionVnd: (json['commissionVnd'] is num)
          ? (json['commissionVnd'] as num).toInt()
          : int.tryParse(json['commissionVnd']?.toString() ?? '0') ?? 0,
      driverAmountVnd: (json['driverAmountVnd'] is num)
          ? (json['driverAmountVnd'] as num).toInt()
          : int.tryParse(json['driverAmountVnd']?.toString() ?? '0') ?? 0,
      payUrl: json['payUrl']?.toString(),
      deepLink: json['deepLink']?.toString(),
      qrCodeUrl: json['qrCodeUrl']?.toString(),
      expiresAt: json['expiresAt']?.toString(),
      paidAt: json['paidAt']?.toString(),
      createdAt: json['createdAt']?.toString(),
    );
  }

  Map<String, dynamic> toJson() => {
    'paymentId': paymentId,
    'tripId': tripId,
    'method': method.value,
    'status': status.value,
    'amountVnd': amountVnd,
    'commissionVnd': commissionVnd,
    'driverAmountVnd': driverAmountVnd,
    'payUrl': payUrl,
    'deepLink': deepLink,
    'qrCodeUrl': qrCodeUrl,
    'expiresAt': expiresAt,
    'paidAt': paidAt,
    'createdAt': createdAt,
  };
}

class SettlePaymentRequest {
  final String tripId;
  final PaymentMethod method;
  final String? returnUrl;

  const SettlePaymentRequest({
    required this.tripId,
    required this.method,
    this.returnUrl,
  });

  Map<String, dynamic> toJson() => {
    'tripId': tripId,
    'method': method.value,
    if (returnUrl != null) 'returnUrl': returnUrl,
  };
}

