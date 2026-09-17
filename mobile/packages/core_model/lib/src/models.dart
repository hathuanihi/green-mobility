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
}
