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
    int parseInt(dynamic v) => (v is num) ? v.toInt() : int.tryParse(v?.toString() ?? '0') ?? 0;
    return DriverEarningsSummaryModel(
      totalGrossVnd: parseInt(json['grossFareVnd'] ?? json['totalGrossVnd']),
      totalCommissionVnd: parseInt(json['commissionVnd'] ?? json['totalCommissionVnd']),
      netEarningsVnd: parseInt(json['netEarningsVnd']),
      onlineEarningsVnd: parseInt(json['onlineCollectedVnd'] ?? json['onlineEarningsVnd']),
      cashCollectedVnd: parseInt(json['cashCollectedVnd']),
      cashDebtRemainingVnd: parseInt(json['cashDebtRemainingVnd']),
      tripCount: parseInt(json['tripCount']),
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
    int parseInt(dynamic v) => (v is num) ? v.toInt() : int.tryParse(v?.toString() ?? '0') ?? 0;
    return DriverTripEarningModel(
      tripId: json['tripId']?.toString() ?? '',
      tripCode: json['tripCode']?.toString() ?? '',
      completedAt: json['completedAt']?.toString() ?? '',
      grossAmountVnd: parseInt(json['fareVnd'] ?? json['grossAmountVnd']),
      commissionVnd: parseInt(json['commissionVnd']),
      netEarnedVnd: parseInt(json['netIncomeVnd'] ?? json['netEarnedVnd']),
      paymentMethod: json['paymentMethod']?.toString() ?? 'CASH',
      isCash: json['paymentMethod']?.toString() == 'CASH',
    );
  }
}

