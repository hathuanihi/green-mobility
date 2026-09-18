// Core Domain Models for Green Mobility Mobile Apps

class UserProfile {
  final String id;
  final String phoneNumber;
  final String fullName;
  final String role;
  final String? avatarUrl;

  const UserProfile({
    required this.id,
    required this.phoneNumber,
    required this.fullName,
    required this.role,
    this.avatarUrl,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id']?.toString() ?? '',
      phoneNumber: json['phoneNumber']?.toString() ?? '',
      fullName: json['fullName']?.toString() ?? '',
      role: json['role']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString(),
    );
  }
}

class AuthResponseModel {
  final String userId;
  final String phoneNumber;
  final String fullName;
  final String role;
  final String token;
  final int expiresIn;
  final String? kycStatus;

  const AuthResponseModel({
    required this.userId,
    required this.phoneNumber,
    required this.fullName,
    required this.role,
    required this.token,
    required this.expiresIn,
    this.kycStatus,
  });

  factory AuthResponseModel.fromJson(Map<String, dynamic> json) {
    return AuthResponseModel(
      userId: json['userId']?.toString() ?? '',
      phoneNumber: json['phoneNumber']?.toString() ?? '',
      fullName: json['fullName']?.toString() ?? '',
      role: json['role']?.toString() ?? '',
      token: json['token']?.toString() ?? '',
      expiresIn: (json['expiresIn'] is num) ? (json['expiresIn'] as num).toInt() : 86400,
      kycStatus: json['kycStatus']?.toString(),
    );
  }
}

class VehicleModel {
  final String? id;
  final String vehicleType;
  final String make;
  final String model;
  final String licensePlate;
  final String color;
  final double batteryCapacityKwh;
  final int rangePerChargeKm;
  final String? registrationCertificateUrl;
  final String? inspectionExpiryDate;
  final bool isVerified;

  const VehicleModel({
    this.id,
    required this.vehicleType,
    required this.make,
    required this.model,
    required this.licensePlate,
    required this.color,
    required this.batteryCapacityKwh,
    required this.rangePerChargeKm,
    this.registrationCertificateUrl,
    this.inspectionExpiryDate,
    this.isVerified = false,
  });

  factory VehicleModel.fromJson(Map<String, dynamic> json) {
    return VehicleModel(
      id: json['id']?.toString(),
      vehicleType: json['vehicleType']?.toString() ?? '',
      make: json['make']?.toString() ?? '',
      model: json['model']?.toString() ?? '',
      licensePlate: json['licensePlate']?.toString() ?? '',
      color: json['color']?.toString() ?? '',
      batteryCapacityKwh: (json['batteryCapacityKwh'] is num)
          ? (json['batteryCapacityKwh'] as num).toDouble()
          : double.tryParse(json['batteryCapacityKwh']?.toString() ?? '0') ?? 0.0,
      rangePerChargeKm: (json['rangePerChargeKm'] is num)
          ? (json['rangePerChargeKm'] as num).toInt()
          : int.tryParse(json['rangePerChargeKm']?.toString() ?? '0') ?? 0,
      registrationCertificateUrl: json['registrationCertificateUrl']?.toString(),
      inspectionExpiryDate: json['inspectionExpiryDate']?.toString(),
      isVerified: json['isVerified'] == true,
    );
  }
}

class DriverProfile {
  final String? driverId;
  final String userId;
  final String fullName;
  final String phoneNumber;
  final String? citizenId;
  final String? driverLicenseNumber;
  final String? licenseClass;
  final String kycStatus;
  final String? kycRejectionReason;
  final String? citizenCardFrontUrl;
  final String? citizenCardBackUrl;
  final String? driverLicenseImageUrl;
  final String? facePortraitUrl;
  final bool isActiveShift;
  final double ratingAvg;
  final int totalTripsCompleted;
  final double totalCo2SavedKg;
  final VehicleModel? vehicle;

  const DriverProfile({
    this.driverId,
    required this.userId,
    required this.fullName,
    required this.phoneNumber,
    this.citizenId,
    this.driverLicenseNumber,
    this.licenseClass,
    required this.kycStatus,
    this.kycRejectionReason,
    this.citizenCardFrontUrl,
    this.citizenCardBackUrl,
    this.driverLicenseImageUrl,
    this.facePortraitUrl,
    this.isActiveShift = false,
    this.ratingAvg = 5.0,
    this.totalTripsCompleted = 0,
    this.totalCo2SavedKg = 0.0,
    this.vehicle,
  });

  factory DriverProfile.fromJson(Map<String, dynamic> json) {
    return DriverProfile(
      driverId: json['driverId']?.toString(),
      userId: json['userId']?.toString() ?? '',
      fullName: json['fullName']?.toString() ?? '',
      phoneNumber: json['phoneNumber']?.toString() ?? '',
      citizenId: json['citizenId']?.toString(),
      driverLicenseNumber: json['driverLicenseNumber']?.toString(),
      licenseClass: json['licenseClass']?.toString(),
      kycStatus: json['kycStatus']?.toString() ?? 'NOT_SUBMITTED',
      kycRejectionReason: json['kycRejectionReason']?.toString(),
      citizenCardFrontUrl: json['citizenCardFrontUrl']?.toString(),
      citizenCardBackUrl: json['citizenCardBackUrl']?.toString(),
      driverLicenseImageUrl: json['driverLicenseImageUrl']?.toString() ?? json['driverLicenseUrl']?.toString(),
      facePortraitUrl: json['facePortraitUrl']?.toString(),
      isActiveShift: json['isActiveShift'] == true,
      ratingAvg: (json['ratingAvg'] is num)
          ? (json['ratingAvg'] as num).toDouble()
          : double.tryParse(json['ratingAvg']?.toString() ?? '5.0') ?? 5.0,
      totalTripsCompleted: (json['totalTripsCompleted'] is num)
          ? (json['totalTripsCompleted'] as num).toInt()
          : int.tryParse(json['totalTripsCompleted']?.toString() ?? '0') ?? 0,
      totalCo2SavedKg: (json['totalCo2SavedKg'] is num)
          ? (json['totalCo2SavedKg'] as num).toDouble()
          : double.tryParse(json['totalCo2SavedKg']?.toString() ?? '0.0') ?? 0.0,
      vehicle: json['vehicle'] != null ? VehicleModel.fromJson(json['vehicle']) : null,
    );
  }
}

class FaceVerifyResult {
  final bool isPassed;
  final double similarityScore;
  final bool isActiveShift;
  final String? verifiedAt;
  final String? message;

  const FaceVerifyResult({
    required this.isPassed,
    required this.similarityScore,
    required this.isActiveShift,
    this.verifiedAt,
    this.message,
  });

  factory FaceVerifyResult.fromJson(Map<String, dynamic> json, {String? defaultMessage}) {
    return FaceVerifyResult(
      isPassed: json['isPassed'] == true,
      similarityScore: (json['similarityScore'] is num)
          ? (json['similarityScore'] as num).toDouble()
          : double.tryParse(json['similarityScore']?.toString() ?? '0') ?? 0.0,
      isActiveShift: json['isActiveShift'] == true,
      verifiedAt: json['verifiedAt']?.toString(),
      message: defaultMessage,
    );
  }
}

class KycSubmissionModel {
  final String citizenId;
  final String licenseNumber;
  final String licenseClass;
  final String vehicleType;
  final String make;
  final String model;
  final String licensePlate;
  final String color;
  final double batteryCapacityKwh;
  final int rangePerChargeKm;
  final String inspectionExpiryDate;
  final String citizenFrontPath;
  final String citizenBackPath;
  final String licenseImagePath;
  final String vehicleRegistrationPath;
  final String facePortraitPath;

  const KycSubmissionModel({
    required this.citizenId,
    required this.licenseNumber,
    required this.licenseClass,
    required this.vehicleType,
    required this.make,
    required this.model,
    required this.licensePlate,
    required this.color,
    required this.batteryCapacityKwh,
    required this.rangePerChargeKm,
    required this.inspectionExpiryDate,
    required this.citizenFrontPath,
    required this.citizenBackPath,
    required this.licenseImagePath,
    required this.vehicleRegistrationPath,
    required this.facePortraitPath,
  });
}

class CarbonImpact {
  final double co2SavedGrams;
  final double treeAbsorptionDays;
  final double ledBulbHours;
  final double carbonCredits;

  const CarbonImpact({
    required this.co2SavedGrams,
    required this.treeAbsorptionDays,
    required this.ledBulbHours,
    required this.carbonCredits,
  });

  factory CarbonImpact.fromJson(Map<String, dynamic> json) {
    return CarbonImpact(
      co2SavedGrams: (json['co2SavedGrams'] is num)
          ? (json['co2SavedGrams'] as num).toDouble()
          : double.tryParse(json['co2SavedGrams']?.toString() ?? '0') ?? 0.0,
      treeAbsorptionDays: (json['treeAbsorptionDays'] is num)
          ? (json['treeAbsorptionDays'] as num).toDouble()
          : double.tryParse(json['treeAbsorptionDays']?.toString() ?? '0') ?? 0.0,
      ledBulbHours: (json['ledBulbHours'] is num)
          ? (json['ledBulbHours'] as num).toDouble()
          : double.tryParse(json['ledBulbHours']?.toString() ?? '0') ?? 0.0,
      carbonCredits: (json['carbonCredits'] is num)
          ? (json['carbonCredits'] as num).toDouble()
          : double.tryParse(json['carbonCredits']?.toString() ?? '0') ?? 0.0,
    );
  }
}

class CarbonEstimateModel {
  final double co2SavedGrams;
  final double treeAbsorptionDays;
  final double ledBulbHours;
  final double baselineGasolineGrams;
  final double evEmittedGrams;

  const CarbonEstimateModel({
    required this.co2SavedGrams,
    required this.treeAbsorptionDays,
    required this.ledBulbHours,
    required this.baselineGasolineGrams,
    required this.evEmittedGrams,
  });

  factory CarbonEstimateModel.fromJson(Map<String, dynamic> json) {
    return CarbonEstimateModel(
      co2SavedGrams: (json['co2SavedGrams'] is num)
          ? (json['co2SavedGrams'] as num).toDouble()
          : double.tryParse(json['co2SavedGrams']?.toString() ?? '0') ?? 0.0,
      treeAbsorptionDays: (json['treeAbsorptionDays'] is num)
          ? (json['treeAbsorptionDays'] as num).toDouble()
          : double.tryParse(json['treeAbsorptionDays']?.toString() ?? '0') ?? 0.0,
      ledBulbHours: (json['ledBulbHours'] is num)
          ? (json['ledBulbHours'] as num).toDouble()
          : double.tryParse(json['ledBulbHours']?.toString() ?? '0') ?? 0.0,
      baselineGasolineGrams: (json['baselineGasolineGrams'] is num)
          ? (json['baselineGasolineGrams'] as num).toDouble()
          : double.tryParse(json['baselineGasolineGrams']?.toString() ?? '0') ?? 0.0,
      evEmittedGrams: (json['evEmittedGrams'] is num)
          ? (json['evEmittedGrams'] as num).toDouble()
          : double.tryParse(json['evEmittedGrams']?.toString() ?? '0') ?? 0.0,
    );
  }
}

class TripEstimateModel {
  final String vehicleType;
  final int distanceMeters;
  final double distanceKm;
  final int durationSeconds;
  final int durationMinutes;
  final double fareAmountVnd;
  final CarbonEstimateModel? carbonEstimate;
  final String? routePolyline;

  const TripEstimateModel({
    required this.vehicleType,
    required this.distanceMeters,
    required this.distanceKm,
    required this.durationSeconds,
    required this.durationMinutes,
    required this.fareAmountVnd,
    this.carbonEstimate,
    this.routePolyline,
  });

  factory TripEstimateModel.fromJson(Map<String, dynamic> json) {
    return TripEstimateModel(
      vehicleType: json['vehicleType']?.toString() ?? '',
      distanceMeters: (json['distanceMeters'] is num)
          ? (json['distanceMeters'] as num).toInt()
          : int.tryParse(json['distanceMeters']?.toString() ?? '0') ?? 0,
      distanceKm: (json['distanceKm'] is num)
          ? (json['distanceKm'] as num).toDouble()
          : double.tryParse(json['distanceKm']?.toString() ?? '0') ?? 0.0,
      durationSeconds: (json['durationSeconds'] is num)
          ? (json['durationSeconds'] as num).toInt()
          : int.tryParse(json['durationSeconds']?.toString() ?? '0') ?? 0,
      durationMinutes: (json['durationMinutes'] is num)
          ? (json['durationMinutes'] as num).toInt()
          : int.tryParse(json['durationMinutes']?.toString() ?? '0') ?? 0,
      fareAmountVnd: (json['fareAmountVnd'] is num)
          ? (json['fareAmountVnd'] as num).toDouble()
          : double.tryParse(json['fareAmountVnd']?.toString() ?? '0') ?? 0.0,
      carbonEstimate: json['carbonEstimate'] != null
          ? CarbonEstimateModel.fromJson(json['carbonEstimate'] as Map<String, dynamic>)
          : null,
      routePolyline: json['routePolyline']?.toString(),
    );
  }
}

class TripEstimateRequestModel {
  final String pickupAddress;
  final double pickupLat;
  final double pickupLng;
  final String dropoffAddress;
  final double dropoffLat;
  final double dropoffLng;
  final String? vehicleType;

  const TripEstimateRequestModel({
    required this.pickupAddress,
    required this.pickupLat,
    required this.pickupLng,
    required this.dropoffAddress,
    required this.dropoffLat,
    required this.dropoffLng,
    this.vehicleType,
  });

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{
      'pickupAddress': pickupAddress,
      'pickupLat': pickupLat,
      'pickupLng': pickupLng,
      'dropoffAddress': dropoffAddress,
      'dropoffLat': dropoffLat,
      'dropoffLng': dropoffLng,
    };
    if (vehicleType != null) {
      map['vehicleType'] = vehicleType;
    }
    return map;
  }
}

class TripRequestModel {
  final String pickupAddress;
  final double pickupLat;
  final double pickupLng;
  final String dropoffAddress;
  final double dropoffLat;
  final double dropoffLng;
  final String vehicleType;
  final String paymentMethod;
  final String? notes;

  const TripRequestModel({
    required this.pickupAddress,
    required this.pickupLat,
    required this.pickupLng,
    required this.dropoffAddress,
    required this.dropoffLat,
    required this.dropoffLng,
    required this.vehicleType,
    this.paymentMethod = 'CASH',
    this.notes,
  });

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{
      'pickupAddress': pickupAddress,
      'pickupLat': pickupLat,
      'pickupLng': pickupLng,
      'dropoffAddress': dropoffAddress,
      'dropoffLat': dropoffLat,
      'dropoffLng': dropoffLng,
      'vehicleType': vehicleType,
      'paymentMethod': paymentMethod,
    };
    if (notes != null && notes!.isNotEmpty) {
      map['notes'] = notes;
    }
    return map;
  }
}

class DriverSummaryModel {
  final String driverId;
  final String fullName;
  final String phoneNumber;
  final String? avatarUrl;
  final double ratingAvg;
  final String vehicleModel;
  final String licensePlate;
  final double? currentLat;
  final double? currentLng;

  const DriverSummaryModel({
    required this.driverId,
    required this.fullName,
    required this.phoneNumber,
    this.avatarUrl,
    required this.ratingAvg,
    required this.vehicleModel,
    required this.licensePlate,
    this.currentLat,
    this.currentLng,
  });

  factory DriverSummaryModel.fromJson(Map<String, dynamic> json) {
    return DriverSummaryModel(
      driverId: json['driverId']?.toString() ?? '',
      fullName: json['fullName']?.toString() ?? '',
      phoneNumber: json['phoneNumber']?.toString() ?? '',
      avatarUrl: json['avatarUrl']?.toString(),
      ratingAvg: (json['ratingAvg'] is num)
          ? (json['ratingAvg'] as num).toDouble()
          : double.tryParse(json['ratingAvg']?.toString() ?? '5.0') ?? 5.0,
      vehicleModel: json['vehicleModel']?.toString() ?? '',
      licensePlate: json['licensePlate']?.toString() ?? '',
      currentLat: (json['currentLat'] is num)
          ? (json['currentLat'] as num).toDouble()
          : double.tryParse(json['currentLat']?.toString() ?? ''),
      currentLng: (json['currentLng'] is num)
          ? (json['currentLng'] as num).toDouble()
          : double.tryParse(json['currentLng']?.toString() ?? ''),
    );
  }
}

class TripModel {
  final String tripId;
  final String tripCode;
  final String status;
  final String vehicleType;
  final String pickupAddress;
  final double pickupLat;
  final double pickupLng;
  final String dropoffAddress;
  final double dropoffLat;
  final double dropoffLng;
  final double fareAmountVnd;
  final double discountAmountVnd;
  final double finalAmountVnd;
  final String paymentMethod;
  final String paymentStatus;
  final double estimatedDistanceKm;
  final int estimatedDurationMinutes;
  final double co2SavedGrams;
  final DriverSummaryModel? driver;
  final String? cancelReason;
  final String? cancelledBy;
  final String? requestedAt;
  final String? matchedAt;

  const TripModel({
    required this.tripId,
    required this.tripCode,
    required this.status,
    required this.vehicleType,
    required this.pickupAddress,
    required this.pickupLat,
    required this.pickupLng,
    required this.dropoffAddress,
    required this.dropoffLat,
    required this.dropoffLng,
    required this.fareAmountVnd,
    required this.discountAmountVnd,
    required this.finalAmountVnd,
    required this.paymentMethod,
    required this.paymentStatus,
    required this.estimatedDistanceKm,
    required this.estimatedDurationMinutes,
    required this.co2SavedGrams,
    this.driver,
    this.cancelReason,
    this.cancelledBy,
    this.requestedAt,
    this.matchedAt,
  });

  factory TripModel.fromJson(Map<String, dynamic> json) {
    return TripModel(
      tripId: json['tripId']?.toString() ?? '',
      tripCode: json['tripCode']?.toString() ?? '',
      status: json['status']?.toString() ?? 'REQUESTED',
      vehicleType: json['vehicleType']?.toString() ?? '',
      pickupAddress: json['pickupAddress']?.toString() ?? '',
      pickupLat: (json['pickupLat'] is num)
          ? (json['pickupLat'] as num).toDouble()
          : double.tryParse(json['pickupLat']?.toString() ?? '0') ?? 0.0,
      pickupLng: (json['pickupLng'] is num)
          ? (json['pickupLng'] as num).toDouble()
          : double.tryParse(json['pickupLng']?.toString() ?? '0') ?? 0.0,
      dropoffAddress: json['dropoffAddress']?.toString() ?? '',
      dropoffLat: (json['dropoffLat'] is num)
          ? (json['dropoffLat'] as num).toDouble()
          : double.tryParse(json['dropoffLat']?.toString() ?? '0') ?? 0.0,
      dropoffLng: (json['dropoffLng'] is num)
          ? (json['dropoffLng'] as num).toDouble()
          : double.tryParse(json['dropoffLng']?.toString() ?? '0') ?? 0.0,
      fareAmountVnd: (json['fareAmountVnd'] is num)
          ? (json['fareAmountVnd'] as num).toDouble()
          : double.tryParse(json['fareAmountVnd']?.toString() ?? '0') ?? 0.0,
      discountAmountVnd: (json['discountAmountVnd'] is num)
          ? (json['discountAmountVnd'] as num).toDouble()
          : double.tryParse(json['discountAmountVnd']?.toString() ?? '0') ?? 0.0,
      finalAmountVnd: (json['finalAmountVnd'] is num)
          ? (json['finalAmountVnd'] as num).toDouble()
          : double.tryParse(json['finalAmountVnd']?.toString() ?? '0') ?? 0.0,
      paymentMethod: json['paymentMethod']?.toString() ?? 'CASH',
      paymentStatus: json['paymentStatus']?.toString() ?? 'PENDING',
      estimatedDistanceKm: (json['estimatedDistanceKm'] is num)
          ? (json['estimatedDistanceKm'] as num).toDouble()
          : double.tryParse(json['estimatedDistanceKm']?.toString() ?? '0') ?? 0.0,
      estimatedDurationMinutes: (json['estimatedDurationMinutes'] is num)
          ? (json['estimatedDurationMinutes'] as num).toInt()
          : int.tryParse(json['estimatedDurationMinutes']?.toString() ?? '0') ?? 0,
      co2SavedGrams: (json['co2SavedGrams'] is num)
          ? (json['co2SavedGrams'] as num).toDouble()
          : double.tryParse(json['co2SavedGrams']?.toString() ?? '0') ?? 0.0,
      driver: json['driver'] != null
          ? DriverSummaryModel.fromJson(json['driver'] as Map<String, dynamic>)
          : null,
      cancelReason: json['cancelReason']?.toString(),
      cancelledBy: json['cancelledBy']?.toString(),
      requestedAt: json['requestedAt']?.toString(),
      matchedAt: json['matchedAt']?.toString(),
    );
  }
}

class DispatchNotificationModel {
  final String tripId;
  final String tripCode;
  final String pickupAddress;
  final double pickupLat;
  final double pickupLng;
  final double distanceToPickupKm;
  final String dropoffAddress;
  final double dropoffLat;
  final double dropoffLng;
  final double tripDistanceKm;
  final int estimatedDurationMinutes;
  final double estimatedEarningsVnd;
  final double co2SavedGrams;
  final int countdownSeconds;

  const DispatchNotificationModel({
    required this.tripId,
    required this.tripCode,
    required this.pickupAddress,
    required this.pickupLat,
    required this.pickupLng,
    required this.distanceToPickupKm,
    required this.dropoffAddress,
    required this.dropoffLat,
    required this.dropoffLng,
    required this.tripDistanceKm,
    required this.estimatedDurationMinutes,
    required this.estimatedEarningsVnd,
    required this.co2SavedGrams,
    this.countdownSeconds = 15,
  });

  factory DispatchNotificationModel.fromJson(Map<String, dynamic> json) {
    return DispatchNotificationModel(
      tripId: json['tripId']?.toString() ?? '',
      tripCode: json['tripCode']?.toString() ?? '',
      pickupAddress: json['pickupAddress']?.toString() ?? '',
      pickupLat: (json['pickupLat'] is num)
          ? (json['pickupLat'] as num).toDouble()
          : double.tryParse(json['pickupLat']?.toString() ?? '0') ?? 0.0,
      pickupLng: (json['pickupLng'] is num)
          ? (json['pickupLng'] as num).toDouble()
          : double.tryParse(json['pickupLng']?.toString() ?? '0') ?? 0.0,
      distanceToPickupKm: (json['distanceToPickupKm'] is num)
          ? (json['distanceToPickupKm'] as num).toDouble()
          : double.tryParse(json['distanceToPickupKm']?.toString() ?? '0') ?? 0.0,
      dropoffAddress: json['dropoffAddress']?.toString() ?? '',
      dropoffLat: (json['dropoffLat'] is num)
          ? (json['dropoffLat'] as num).toDouble()
          : double.tryParse(json['dropoffLat']?.toString() ?? '0') ?? 0.0,
      dropoffLng: (json['dropoffLng'] is num)
          ? (json['dropoffLng'] as num).toDouble()
          : double.tryParse(json['dropoffLng']?.toString() ?? '0') ?? 0.0,
      tripDistanceKm: (json['tripDistanceKm'] is num)
          ? (json['tripDistanceKm'] as num).toDouble()
          : double.tryParse(json['tripDistanceKm']?.toString() ?? '0') ?? 0.0,
      estimatedDurationMinutes: (json['estimatedDurationMinutes'] is num)
          ? (json['estimatedDurationMinutes'] as num).toInt()
          : int.tryParse(json['estimatedDurationMinutes']?.toString() ?? '0') ?? 0,
      estimatedEarningsVnd: (json['estimatedEarningsVnd'] is num)
          ? (json['estimatedEarningsVnd'] as num).toDouble()
          : double.tryParse(json['estimatedEarningsVnd']?.toString() ?? '0') ?? 0.0,
      co2SavedGrams: (json['co2SavedGrams'] is num)
          ? (json['co2SavedGrams'] as num).toDouble()
          : double.tryParse(json['co2SavedGrams']?.toString() ?? '0') ?? 0.0,
      countdownSeconds: (json['countdownSeconds'] is num)
          ? (json['countdownSeconds'] as num).toInt()
          : int.tryParse(json['countdownSeconds']?.toString() ?? '15') ?? 15,
    );
  }
}

class DriverTripModel {
  final String tripId;
  final String tripCode;
  final String status;
  final String customerName;
  final String customerPhone;
  final String pickupAddress;
  final double pickupLat;
  final double pickupLng;
  final String dropoffAddress;
  final double dropoffLat;
  final double dropoffLng;
  final double estimatedDistanceKm;
  final double netIncomeVnd;
  final double co2SavedGrams;
  final String? matchedAt;

  const DriverTripModel({
    required this.tripId,
    required this.tripCode,
    required this.status,
    required this.customerName,
    required this.customerPhone,
    required this.pickupAddress,
    required this.pickupLat,
    required this.pickupLng,
    required this.dropoffAddress,
    required this.dropoffLat,
    required this.dropoffLng,
    required this.estimatedDistanceKm,
    required this.netIncomeVnd,
    required this.co2SavedGrams,
    this.matchedAt,
  });

  factory DriverTripModel.fromJson(Map<String, dynamic> json) {
    return DriverTripModel(
      tripId: json['tripId']?.toString() ?? '',
      tripCode: json['tripCode']?.toString() ?? '',
      status: json['status']?.toString() ?? 'MATCHED',
      customerName: json['customerName']?.toString() ?? '',
      customerPhone: json['customerPhone']?.toString() ?? '',
      pickupAddress: json['pickupAddress']?.toString() ?? '',
      pickupLat: (json['pickupLat'] is num)
          ? (json['pickupLat'] as num).toDouble()
          : double.tryParse(json['pickupLat']?.toString() ?? '0') ?? 0.0,
      pickupLng: (json['pickupLng'] is num)
          ? (json['pickupLng'] as num).toDouble()
          : double.tryParse(json['pickupLng']?.toString() ?? '0') ?? 0.0,
      dropoffAddress: json['dropoffAddress']?.toString() ?? '',
      dropoffLat: (json['dropoffLat'] is num)
          ? (json['dropoffLat'] as num).toDouble()
          : double.tryParse(json['dropoffLat']?.toString() ?? '0') ?? 0.0,
      dropoffLng: (json['dropoffLng'] is num)
          ? (json['dropoffLng'] as num).toDouble()
          : double.tryParse(json['dropoffLng']?.toString() ?? '0') ?? 0.0,
      estimatedDistanceKm: (json['estimatedDistanceKm'] is num)
          ? (json['estimatedDistanceKm'] as num).toDouble()
          : double.tryParse(json['estimatedDistanceKm']?.toString() ?? '0') ?? 0.0,
      netIncomeVnd: (json['netIncomeVnd'] is num)
          ? (json['netIncomeVnd'] as num).toDouble()
          : double.tryParse(json['netIncomeVnd']?.toString() ?? '0') ?? 0.0,
      co2SavedGrams: (json['co2SavedGrams'] is num)
          ? (json['co2SavedGrams'] as num).toDouble()
          : double.tryParse(json['co2SavedGrams']?.toString() ?? '0') ?? 0.0,
      matchedAt: json['matchedAt']?.toString(),
    );
  }
}

class DriverLocationPingModel {
  final double lat;
  final double lng;
  final double? speedKmh;
  final double? bearing;
  final int batteryPercent;

  const DriverLocationPingModel({
    required this.lat,
    required this.lng,
    this.speedKmh,
    this.bearing,
    this.batteryPercent = 100,
  });

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{
      'lat': lat,
      'lng': lng,
      'batteryPercent': batteryPercent,
    };
    if (speedKmh != null) map['speedKmh'] = speedKmh;
    if (bearing != null) map['bearing'] = bearing;
    return map;
  }
}
