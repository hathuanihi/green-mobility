// Sprint 6: Carbon Impact Receipt, PCC/Eco-Point Wallets, Eco-Store & PCC Conversion Models

int _toInt(dynamic v, [int fallback = 0]) {
  if (v is num) return v.toInt();
  return int.tryParse(v?.toString() ?? '') ?? fallback;
}

double _toDouble(dynamic v, [double fallback = 0.0]) {
  if (v is num) return v.toDouble();
  return double.tryParse(v?.toString() ?? '') ?? fallback;
}

class IncentiveSummaryModel {
  final double pccBalance;
  final bool pccFrozen;
  final String? pccFrozenReason;
  final int ecoPointsBalance;
  final bool pointsFrozen;
  final double monthlyPccConverted;
  final double monthlyPccConvertCap;
  final double pccConvertMin;
  final int pccToVndRateVnd;
  final bool redemptionsFrozen;
  final int availableVouchersCount;
  final double lifetimeCo2SavedGrams;

  const IncentiveSummaryModel({
    required this.pccBalance,
    this.pccFrozen = false,
    this.pccFrozenReason,
    required this.ecoPointsBalance,
    this.pointsFrozen = false,
    this.monthlyPccConverted = 0.0,
    this.monthlyPccConvertCap = 100.0,
    this.pccConvertMin = 0.5,
    this.pccToVndRateVnd = 500,
    this.redemptionsFrozen = false,
    this.availableVouchersCount = 0,
    this.lifetimeCo2SavedGrams = 0.0,
  });

  double get remainingMonthlyCap =>
      (monthlyPccConvertCap - monthlyPccConverted).clamp(0.0, monthlyPccConvertCap);

  factory IncentiveSummaryModel.fromJson(Map<String, dynamic> json) {
    return IncentiveSummaryModel(
      pccBalance: _toDouble(json['pccBalance']),
      pccFrozen: json['pccFrozen'] == true,
      pccFrozenReason: json['pccFrozenReason']?.toString(),
      ecoPointsBalance: _toInt(json['ecoPointsBalance']),
      pointsFrozen: json['pointsFrozen'] == true,
      monthlyPccConverted: _toDouble(json['monthlyPccConverted']),
      monthlyPccConvertCap: _toDouble(json['monthlyPccConvertCap'], 100.0),
      pccConvertMin: _toDouble(json['pccConvertMin'], 0.5),
      pccToVndRateVnd: _toInt(json['pccToVndRateVnd'], 500),
      redemptionsFrozen: json['redemptionsFrozen'] == true,
      availableVouchersCount: _toInt(json['availableVouchersCount']),
      lifetimeCo2SavedGrams: _toDouble(json['lifetimeCo2SavedGrams']),
    );
  }
}

class IncentiveLedgerEntryModel {
  final String id;
  final String currency; // PCC or ECO_POINT
  final String entryType; // CREDIT or DEBIT
  final double amount;
  final double balanceAfter;
  final String transactionCode;
  final String transactionType;
  final String description;
  final String? tripId;
  final String createdAt;

  const IncentiveLedgerEntryModel({
    required this.id,
    required this.currency,
    required this.entryType,
    required this.amount,
    required this.balanceAfter,
    required this.transactionCode,
    required this.transactionType,
    required this.description,
    this.tripId,
    required this.createdAt,
  });

  bool get isCredit => entryType.toUpperCase() == 'CREDIT';

  factory IncentiveLedgerEntryModel.fromJson(Map<String, dynamic> json) {
    return IncentiveLedgerEntryModel(
      id: json['id']?.toString() ?? '',
      currency: json['currency']?.toString() ?? 'PCC',
      entryType: json['entryType']?.toString() ?? 'CREDIT',
      amount: _toDouble(json['amount']),
      balanceAfter: _toDouble(json['balanceAfter']),
      transactionCode: json['transactionCode']?.toString() ?? '',
      transactionType: json['transactionType']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      tripId: json['tripId']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

class IncentiveLedgerPageModel {
  final List<IncentiveLedgerEntryModel> items;
  final String? nextCursor;
  final bool hasMore;

  const IncentiveLedgerPageModel({
    required this.items,
    this.nextCursor,
    required this.hasMore,
  });

  factory IncentiveLedgerPageModel.fromJson(Map<String, dynamic> json) {
    final list = (json['items'] as List<dynamic>?)
            ?.map((e) => IncentiveLedgerEntryModel.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [];
    return IncentiveLedgerPageModel(
      items: list,
      nextCursor: json['nextCursor']?.toString(),
      hasMore: json['hasMore'] == true,
    );
  }
}

class RewardCatalogItemModel {
  final String id;
  final String title;
  final String description;
  final String partnerName;
  final String category; // VOUCHER_RIDE, TREE_PLANTING, ECO_PRODUCT
  final double carbonCreditCost;
  final int loyaltyPointCost;
  final String? imageUrl;
  final int quantityAvailable;
  final bool isActive;
  final String? discountType;
  final int? discountValue;
  final int? maxDiscountVnd;
  final int minFareVnd;
  final int validityDays;
  final int maxRedemptionsPerUser;

  const RewardCatalogItemModel({
    required this.id,
    required this.title,
    this.description = '',
    required this.partnerName,
    required this.category,
    required this.carbonCreditCost,
    required this.loyaltyPointCost,
    this.imageUrl,
    required this.quantityAvailable,
    this.isActive = true,
    this.discountType,
    this.discountValue,
    this.maxDiscountVnd,
    this.minFareVnd = 0,
    this.validityDays = 30,
    this.maxRedemptionsPerUser = 5,
  });

  factory RewardCatalogItemModel.fromJson(Map<String, dynamic> json) {
    return RewardCatalogItemModel(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      partnerName: json['partnerName']?.toString() ?? '',
      category: json['category']?.toString() ?? 'VOUCHER_RIDE',
      carbonCreditCost: _toDouble(json['carbonCreditCost']),
      loyaltyPointCost: _toInt(json['loyaltyPointCost']),
      imageUrl: json['imageUrl']?.toString(),
      quantityAvailable: _toInt(json['quantityAvailable']),
      isActive: json['isActive'] != false,
      discountType: json['discountType']?.toString(),
      discountValue: json['discountValue'] != null ? _toInt(json['discountValue']) : null,
      maxDiscountVnd: json['maxDiscountVnd'] != null ? _toInt(json['maxDiscountVnd']) : null,
      minFareVnd: _toInt(json['minFareVnd']),
      validityDays: _toInt(json['validityDays'], 30),
      maxRedemptionsPerUser: _toInt(json['maxRedemptionsPerUser'], 5),
    );
  }
}

class RedemptionItemModel {
  final String id;
  final String rewardId;
  final String rewardTitle;
  final String partnerName;
  final String category;
  final String redemptionCode;
  final double carbonSpent;
  final int pointsSpent;
  final String status; // AVAILABLE, RESERVED, USED, CANCELLED, EXPIRED, COMPLETED
  final String? discountType;
  final int? discountValue;
  final int? maxDiscountVnd;
  final int? minFareVnd;
  final String? appliedTripId;
  final int? discountAppliedVnd;
  final String? expiresAt;
  final String? usedAt;
  final String? cancelledAt;
  final String? certificateMetadata;
  final String createdAt;

  const RedemptionItemModel({
    required this.id,
    required this.rewardId,
    required this.rewardTitle,
    required this.partnerName,
    required this.category,
    required this.redemptionCode,
    required this.carbonSpent,
    required this.pointsSpent,
    required this.status,
    this.discountType,
    this.discountValue,
    this.maxDiscountVnd,
    this.minFareVnd,
    this.appliedTripId,
    this.discountAppliedVnd,
    this.expiresAt,
    this.usedAt,
    this.cancelledAt,
    this.certificateMetadata,
    required this.createdAt,
  });

  bool get isUsableVoucher =>
      category == 'VOUCHER_RIDE' && (status == 'AVAILABLE' || status == 'ISSUED');

  factory RedemptionItemModel.fromJson(Map<String, dynamic> json) {
    return RedemptionItemModel(
      id: json['id']?.toString() ?? '',
      rewardId: json['rewardId']?.toString() ?? '',
      rewardTitle: json['rewardTitle']?.toString() ?? '',
      partnerName: json['partnerName']?.toString() ?? '',
      category: json['category']?.toString() ?? 'VOUCHER_RIDE',
      redemptionCode: json['redemptionCode']?.toString() ?? '',
      carbonSpent: _toDouble(json['carbonSpent']),
      pointsSpent: _toInt(json['pointsSpent']),
      status: json['status']?.toString() ?? 'AVAILABLE',
      discountType: json['discountType']?.toString(),
      discountValue: json['discountValue'] != null ? _toInt(json['discountValue']) : null,
      maxDiscountVnd: json['maxDiscountVnd'] != null ? _toInt(json['maxDiscountVnd']) : null,
      minFareVnd: json['minFareVnd'] != null ? _toInt(json['minFareVnd']) : null,
      appliedTripId: json['appliedTripId']?.toString(),
      discountAppliedVnd: json['discountAppliedVnd'] != null ? _toInt(json['discountAppliedVnd']) : null,
      expiresAt: json['expiresAt']?.toString(),
      usedAt: json['usedAt']?.toString(),
      cancelledAt: json['cancelledAt']?.toString(),
      certificateMetadata: json['certificateMetadata']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

class ConvertPccResultModel {
  final String conversionId;
  final double pccAmount;
  final int rateVndPerPcc;
  final int vndCredited;
  final String targetAccountType;
  final double remainingPccBalance;
  final int newVndBalance;
  final String createdAt;

  const ConvertPccResultModel({
    required this.conversionId,
    required this.pccAmount,
    required this.rateVndPerPcc,
    required this.vndCredited,
    required this.targetAccountType,
    required this.remainingPccBalance,
    required this.newVndBalance,
    required this.createdAt,
  });

  factory ConvertPccResultModel.fromJson(Map<String, dynamic> json) {
    return ConvertPccResultModel(
      conversionId: json['conversionId']?.toString() ?? '',
      pccAmount: _toDouble(json['pccAmount']),
      rateVndPerPcc: _toInt(json['rateVndPerPcc']),
      vndCredited: _toInt(json['vndCredited']),
      targetAccountType: json['targetAccountType']?.toString() ?? 'USER_GREEN_WALLET',
      remainingPccBalance: _toDouble(json['remainingPccBalance']),
      newVndBalance: _toInt(json['newVndBalance']),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

class VoucherPreviewModel {
  final String voucherRedemptionId;
  final String voucherCode;
  final String title;
  final double grossFareVnd;
  final double discountAmountVnd;
  final double finalFareVnd;

  const VoucherPreviewModel({
    required this.voucherRedemptionId,
    required this.voucherCode,
    required this.title,
    required this.grossFareVnd,
    required this.discountAmountVnd,
    required this.finalFareVnd,
  });

  factory VoucherPreviewModel.fromJson(Map<String, dynamic> json) {
    return VoucherPreviewModel(
      voucherRedemptionId: json['voucherRedemptionId']?.toString() ?? '',
      voucherCode: json['voucherCode']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      grossFareVnd: _toDouble(json['grossFareVnd']),
      discountAmountVnd: _toDouble(json['discountAmountVnd']),
      finalFareVnd: _toDouble(json['finalFareVnd']),
    );
  }
}

class TripImpactReceiptModel {
  final String id;
  final String tripId;
  final String tripCode;
  final String vehicleType;
  final int distanceM;
  final double co2SavedGrams;
  final double co2SavedKg;
  final double baselineGasolineCo2Grams;
  final double evEmittedCo2Grams;
  final double treeAbsorptionDaysEquiv;
  final double ledBulbHoursEquiv;
  final double smartphoneChargesEquiv;
  final double carbonCreditsEarned;
  final int loyaltyPointsEarned;
  final String shareableSlug;
  final String shareUrl;
  final String createdAt;

  const TripImpactReceiptModel({
    required this.id,
    required this.tripId,
    required this.tripCode,
    required this.vehicleType,
    required this.distanceM,
    required this.co2SavedGrams,
    required this.co2SavedKg,
    required this.baselineGasolineCo2Grams,
    required this.evEmittedCo2Grams,
    required this.treeAbsorptionDaysEquiv,
    required this.ledBulbHoursEquiv,
    required this.smartphoneChargesEquiv,
    required this.carbonCreditsEarned,
    required this.loyaltyPointsEarned,
    required this.shareableSlug,
    required this.shareUrl,
    required this.createdAt,
  });

  factory TripImpactReceiptModel.fromJson(Map<String, dynamic> json) {
    return TripImpactReceiptModel(
      id: json['id']?.toString() ?? '',
      tripId: json['tripId']?.toString() ?? '',
      tripCode: json['tripCode']?.toString() ?? '',
      vehicleType: json['vehicleType']?.toString() ?? '',
      distanceM: _toInt(json['distanceM']),
      co2SavedGrams: _toDouble(json['co2SavedGrams']),
      co2SavedKg: _toDouble(json['co2SavedKg']),
      baselineGasolineCo2Grams: _toDouble(json['baselineGasolineCo2Grams']),
      evEmittedCo2Grams: _toDouble(json['evEmittedCo2Grams']),
      treeAbsorptionDaysEquiv: _toDouble(json['treeAbsorptionDaysEquiv']),
      ledBulbHoursEquiv: _toDouble(json['ledBulbHoursEquiv']),
      smartphoneChargesEquiv: _toDouble(json['smartphoneChargesEquiv']),
      carbonCreditsEarned: _toDouble(json['carbonCreditsEarned']),
      loyaltyPointsEarned: _toInt(json['loyaltyPointsEarned']),
      shareableSlug: json['shareableSlug']?.toString() ?? '',
      shareUrl: json['shareUrl']?.toString() ?? '',
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

class UserCarbonSummaryModel {
  final int totalTrips;
  final double totalDistanceKm;
  final double totalCo2SavedGrams;
  final double totalCo2SavedKg;
  final double totalCarbonCredits;
  final int totalLoyaltyPoints;
  final double treeDaysEquiv;
  final double ledHoursEquiv;

  const UserCarbonSummaryModel({
    required this.totalTrips,
    required this.totalDistanceKm,
    required this.totalCo2SavedGrams,
    required this.totalCo2SavedKg,
    required this.totalCarbonCredits,
    required this.totalLoyaltyPoints,
    required this.treeDaysEquiv,
    required this.ledHoursEquiv,
  });

  factory UserCarbonSummaryModel.fromJson(Map<String, dynamic> json) {
    return UserCarbonSummaryModel(
      totalTrips: _toInt(json['totalTrips']),
      totalDistanceKm: _toDouble(json['totalDistanceKm']),
      totalCo2SavedGrams: _toDouble(json['totalCo2SavedGrams']),
      totalCo2SavedKg: _toDouble(json['totalCo2SavedKg']),
      totalCarbonCredits: _toDouble(json['totalCarbonCredits']),
      totalLoyaltyPoints: _toInt(json['totalLoyaltyPoints']),
      treeDaysEquiv: _toDouble(json['treeDaysEquiv']),
      ledHoursEquiv: _toDouble(json['ledHoursEquiv']),
    );
  }
}
