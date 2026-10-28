// Sprint 5: Driver Earnings & Withdrawal models

class DriverEarningsSummaryModel {
  final int totalGrossVnd;
  final int totalCommissionVnd;
  final int netEarningsVnd;
  final int onlineEarningsVnd;
  final int cashCollectedVnd;
  final int cashDebtRemainingVnd;
  final int tripCount;
  final String period; // TODAY, WEEK, MONTH

  const DriverEarningsSummaryModel({
    required this.totalGrossVnd,
    required this.totalCommissionVnd,
    required this.netEarningsVnd,
    required this.onlineEarningsVnd,
    required this.cashCollectedVnd,
    required this.cashDebtRemainingVnd,
    required this.tripCount,
    required this.period,
  });

  factory DriverEarningsSummaryModel.fromJson(Map<String, dynamic> json) {
    return DriverEarningsSummaryModel(
      totalGrossVnd: (json['totalGrossVnd'] is num)
          ? (json['totalGrossVnd'] as num).toInt()
          : int.tryParse(json['totalGrossVnd']?.toString() ?? '0') ?? 0,
      totalCommissionVnd: (json['totalCommissionVnd'] is num)
          ? (json['totalCommissionVnd'] as num).toInt()
          : int.tryParse(json['totalCommissionVnd']?.toString() ?? '0') ?? 0,
      netEarningsVnd: (json['netEarningsVnd'] is num)
          ? (json['netEarningsVnd'] as num).toInt()
          : int.tryParse(json['netEarningsVnd']?.toString() ?? '0') ?? 0,
      onlineEarningsVnd: (json['onlineEarningsVnd'] is num)
          ? (json['onlineEarningsVnd'] as num).toInt()
          : int.tryParse(json['onlineEarningsVnd']?.toString() ?? '0') ?? 0,
      cashCollectedVnd: (json['cashCollectedVnd'] is num)
          ? (json['cashCollectedVnd'] as num).toInt()
          : int.tryParse(json['cashCollectedVnd']?.toString() ?? '0') ?? 0,
      cashDebtRemainingVnd: (json['cashDebtRemainingVnd'] is num)
          ? (json['cashDebtRemainingVnd'] as num).toInt()
          : int.tryParse(json['cashDebtRemainingVnd']?.toString() ?? '0') ?? 0,
      tripCount: (json['tripCount'] is num)
          ? (json['tripCount'] as num).toInt()
          : int.tryParse(json['tripCount']?.toString() ?? '0') ?? 0,
      period: json['period']?.toString() ?? 'TODAY',
    );
  }
}

class DriverTripEarningModel {
  final String tripId;
  final String tripCode;
  final String completedAt;
  final int grossAmountVnd;
  final int commissionVnd;
  final int netEarnedVnd;
  final String paymentMethod;
  final bool isCash;

  const DriverTripEarningModel({
    required this.tripId,
    required this.tripCode,
    required this.completedAt,
    required this.grossAmountVnd,
    required this.commissionVnd,
    required this.netEarnedVnd,
    required this.paymentMethod,
    required this.isCash,
  });

  factory DriverTripEarningModel.fromJson(Map<String, dynamic> json) {
    return DriverTripEarningModel(
      tripId: json['tripId']?.toString() ?? '',
      tripCode: json['tripCode']?.toString() ?? '',
      completedAt: json['completedAt']?.toString() ?? '',
      grossAmountVnd: (json['grossAmountVnd'] is num)
          ? (json['grossAmountVnd'] as num).toInt()
          : int.tryParse(json['grossAmountVnd']?.toString() ?? '0') ?? 0,
      commissionVnd: (json['commissionVnd'] is num)
          ? (json['commissionVnd'] as num).toInt()
          : int.tryParse(json['commissionVnd']?.toString() ?? '0') ?? 0,
      netEarnedVnd: (json['netEarnedVnd'] is num)
          ? (json['netEarnedVnd'] as num).toInt()
          : int.tryParse(json['netEarnedVnd']?.toString() ?? '0') ?? 0,
      paymentMethod: json['paymentMethod']?.toString() ?? 'CASH',
      isCash: json['paymentMethod']?.toString() == 'CASH',
    );
  }
}

