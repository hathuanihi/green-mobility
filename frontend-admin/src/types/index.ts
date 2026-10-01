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

export type TripStatus =
  | "REQUESTED"
  | "SEARCHING"
  | "MATCHED"
  | "DRIVER_ARRIVING"
  | "ARRIVED"
  | "IN_TRIP"
  | "COMPLETED"
  | "CANCELLED";

export type PaymentMethod = "CASH" | "VNPAY" | "MOMO" | "GREEN_WALLET";
export type PaymentStatus = "PENDING" | "PAID" | "FAILED" | "REFUNDED";

export interface TripDriverSummary {
  driverId: string;
  fullName: string;
  phoneNumber: string;
  avatarUrl?: string;
  ratingAvg: number;
  vehicleModel: string;
  licensePlate: string;
  currentLat?: number;
  currentLng?: number;
}

export interface Trip {
  tripId: string;
  tripCode: string;
  status: TripStatus;
  vehicleType: VehicleType;
  pickupAddress: string;
  pickupLat: number;
  pickupLng: number;
  dropoffAddress: string;
  dropoffLat: number;
  dropoffLng: number;
  fareAmountVnd: number;
  discountAmountVnd?: number;
  finalAmountVnd: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  estimatedDistanceKm: number;
  estimatedDurationMinutes: number;
  actualDistanceM?: number;
  actualDurationS?: number;
  co2SavedGrams: number;
  driver?: TripDriverSummary;
  cancelReason?: string;
  cancelledBy?: string;
  requestedAt: string;
  matchedAt?: string;
  arrivedPickupAt?: string;
  startedTripAt?: string;
  completedAt?: string;
}

export interface TripTrackingInfo {
  driverLat?: number;
  driverLng?: number;
  bearing?: number;
  speedKmh?: number;
  batteryPercent?: number;
  etaSeconds?: number;
  distanceRemainingM?: number;
  phase?: string;
  routePolyline?: string;
  lastPingEpoch?: number;
}

export interface LiveTripDto {
  tripId: string;
  tripCode: string;
  status: TripStatus;
  vehicleType: VehicleType;
  pickupAddress: string;
  pickupLat: number;
  pickupLng: number;
  dropoffAddress: string;
  dropoffLat: number;
  dropoffLng: number;
  customerName?: string;
  customerPhone?: string;
  driver?: TripDriverSummary;
  driverLat?: number;
  driverLng?: number;
  bearing?: number;
  speedKmh?: number;
  batteryPercent?: number;
  etaSeconds?: number;
  distanceRemainingM?: number;
  phase?: string;
  routePolyline?: string;
  lastPingEpoch?: number;
  requestedAt: string;
  matchedAt?: string;
  estimatedDistanceKm?: number;
  estimatedDurationMinutes?: number;
  fareAmountVnd?: number;
  co2SavedGrams?: number;
}

export interface TodayStatsDto {
  activeTripsCount: number;
  avgPickupTimeMinutes: number;
  avgPickupTimeSeconds: number;
  totalKmToday: number;
  co2SavedTodayGrams: number;
  co2SavedTodayKg: number;
  completedTripsTodayCount: number;
}


