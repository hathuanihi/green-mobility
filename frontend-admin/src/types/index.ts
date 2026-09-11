export type UserRole = "ROLE_ADMIN" | "ROLE_OPERATOR" | "ROLE_DRIVER" | "ROLE_CUSTOMER";

export interface User {
  id: string;
  phoneNumber: string;
  fullName: string;
  role: UserRole;
  avatarUrl?: string;
}

export type KycStatus = "PENDING" | "APPROVED" | "REJECTED";
export type VehicleType = "ELECTRIC_MOTORBIKE" | "ELECTRIC_CAR_4SEAT" | "ELECTRIC_CAR_7SEAT";

export interface Vehicle {
  id: string;
  vehicleType: VehicleType;
  make: string;
  model: string;
  licensePlate: string;
  color: string;
  batteryCapacityKwh: number;
  rangePerChargeKm: number;
  registrationCertificateUrl?: string;
  inspectionExpiryDate: string;
  isVerified: boolean;
}

export interface DriverSummary {
  driverId: string;
  fullName: string;
  phoneNumber: string;
  citizenId: string;
  licenseNumber: string;
  vehicleModel: string;
  licensePlate: string;
  batteryCapacityKwh: number | null;
  kycStatus: KycStatus;
  submittedAt: string;
}

export interface DriverDetail {
  driverId: string;
  userId: string;
  fullName: string;
  phoneNumber: string;
  citizenId: string;
  driverLicenseNumber: string;
  licenseClass: string;
  kycStatus: KycStatus;
  kycRejectionReason?: string;
  citizenCardFrontUrl?: string;
  citizenCardBackUrl?: string;
  driverLicenseImageUrl?: string;
  facePortraitUrl?: string;
  isActiveShift: boolean;
  ratingAvg: number;
  totalTripsCompleted: number;
  totalCo2SavedKg: number;
  vehicle?: Vehicle;
  createdAt: string;
}

export interface FaceVerificationLog {
  id: string;
  driverId: string;
  selfieImageUrl: string;
  similarityScore: number;
  isPassed: boolean;
  verifiedAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface AuthResponseData {
  userId: string;
  phoneNumber: string;
  fullName: string;
  role: UserRole;
  token: string;
  expiresIn: number;
  kycStatus?: string;
}
