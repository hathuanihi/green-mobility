export type UserRole =
  | "ROLE_ADMIN"
  | "ROLE_FINANCE"
  | "ROLE_OPERATOR"
  | "ROLE_DRIVER"
  | "ROLE_CUSTOMER";

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

export interface EmissionFactor {
  id: string;
  vehicleCategory: string;
  baselineGasolineFactorGco2Km: number;
  evEnergyConsumptionKwhKm: number;
  gridEmissionFactorGco2Kwh: number;
  calculatedEvFactorGco2Km: number;
  netCo2SavingPerKm: number;
  region: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
  isActive: boolean;
  createdBy?: string | null;
  createdAt: string;
}

export interface CarbonSimulationResult {
  vehicleCategory: string;
  distanceKm: number;
  baselineGasolineFactorGco2Km: number;
  evEnergyConsumptionKwhKm: number;
  gridEmissionFactorGco2Kwh: number;
  calculatedEvFactorGco2Km: number;
  netCo2SavingPerKm: number;
  baselineGasolineCo2Grams: number;
  evEmittedCo2Grams: number;
  netCo2SavedGrams: number;
  netCo2SavedKg: number;
  treeAbsorptionDaysEquiv: number;
  ledBulbHoursEquiv: number;
  smartphoneChargesEquiv: number;
  carbonCreditsEarned: number;
  ecoPoints: number;
}

export interface AdminVehicle {
  id: string;
  driverId?: string;
  driverName?: string;
  driverPhone?: string;
  vehicleType: VehicleType;
  make: string;
  model: string;
  licensePlate: string;
  color: string;
  batteryCapacityKwh: number;
  rangePerChargeKm: number;
  registrationCertificateUrl?: string;
  inspectionExpiryDate?: string;
  isVerified: boolean;
  createdAt: string;
}

export interface FraudAlert {
  id: string;
  tripId?: string;
  driverId?: string;
  driverUserId?: string;
  customerId?: string;
  driverName?: string;
  driverPhone?: string;
  vehiclePlate?: string;
  alertType: string;
  riskScore: number;
  details: string;
  resolutionStatus: "PENDING" | "RESOLVED" | "DISMISSED";
  resolvedBy?: string;
  resolvedAt?: string;
  createdAt: string;
}

export interface FraudStats {
  totalAlerts: number;
  pendingAlerts: number;
  resolvedAlerts: number;
  highRiskRatio: number;
}

// Sprint 5 Payment, Wallet & Financial Types
export interface PaymentItem {
  paymentId: string;
  tripId: string;
  method: string;
  status: string;
  amountVnd: number;
  commissionVnd: number;
  driverAmountVnd: number;
  payUrl?: string;
  expiresAt?: string;
  paidAt?: string;
  createdAt: string;
}

export interface LedgerEntryItem {
  entryId: string;
  accountType: string;
  accountNumber: string;
  entryType: string;
  amountVnd: number;
  balanceAfterVnd: number;
  createdAt: string;
}

export interface WebhookEventItem {
  eventId: string;
  gateway: string;
  gatewayTxnRef?: string;
  signatureValid: boolean;
  processingResult: string;
  sourceIp?: string;
  receivedAt: string;
}

export interface PaymentDetailData {
  payment: PaymentItem;
  ledgerEntries: LedgerEntryItem[];
  webhookEvents: WebhookEventItem[];
}

export interface WithdrawalItem {
  id: string;
  driverUserId: string;
  amountVnd: number;
  bankCode: string;
  bankAccountLast4: string;
  accountHolderName: string;
  status: "PENDING" | "APPROVED_PAID" | "REJECTED" | "CANCELLED";
  createdAt: string;
  reviewedAt?: string;
  rejectReason?: string;
}

export interface FinanceOverviewData {
  todayGmv: number;
  todayPlatformRevenue: number;
  todayDriverEarnings: number;
  totalWalletBalance: number;
  activeDriversCount: number;
  totalGmvVnd?: number;
  totalCommissionVnd?: number;
  successRate?: number;
  gatewayClearingVnd?: number;
  payoutClearingVnd?: number;
  paymentsByMethod?: Record<string, number>;
  paymentsByStatus?: Record<string, number>;
  revenueByDay?: Array<{
    date: string;
    gmv: number;
    revenue: number;
    driverEarnings: number;
  }>;
  paymentMethodBreakdown?: Array<{
    method: string;
    count: number;
    amount: number;
  }>;
}

export interface ReconciliationData {
  lastRunAt: string;
  status: "BALANCED" | "DISCREPANCY";
  totalSystemDebit: number;
  totalSystemCredit: number;
  systemBalanceDifference: number;
  anomaliesCount: number;
  checks: {
    zeroSumLedger: boolean;
    userBalanceNonNegative: boolean;
    gatewayMatching: boolean;
    cashDebtCompliant: boolean;
    pendingWithdrawalsAudited: boolean;
  };
  discrepancies: string[];
  isBalanced?: boolean;
  systemTotalVnd?: number;
  unbalancedTransactionsCount?: number;
  accountDiscrepanciesCount?: number;
  latePendingPaymentsCount?: number;
  checkedAt?: string;
}

export interface RewardCatalogItem {
  id: string;
  title: string;
  description?: string;
  partnerName: string;
  category: "VOUCHER_RIDE" | "TREE_PLANTING" | "ECO_PRODUCT" | string;
  carbonCreditCost: number;
  loyaltyPointCost: number;
  imageUrl?: string;
  quantityAvailable: number;
  isActive: boolean;
  discountType?: "FIXED_VND" | "PERCENT" | null;
  discountValue?: number | null;
  maxDiscountVnd?: number | null;
  minFareVnd: number;
  validityDays: number;
  maxRedemptionsPerUser: number;
  createdAt: string;
}

export interface TripAwardItem {
  id: string;
  tripId: string;
  customerId: string;
  driverUserId?: string;
  co2SavedGrams: number;
  totalPcc: number;
  customerPcc: number;
  driverPcc: number;
  customerPoints: number;
  driverPoints: number;
  carbonReady: boolean;
  paymentReady: boolean;
  status: string;
  holdReason?: string;
  pccLedgerTxId?: string;
  pointsLedgerTxId?: string;
  creditedAt?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewNote?: string;
  createdAt: string;
}

export interface MultiCurrencyAuditReport {
  isBalanced?: boolean;
  balanced?: boolean;
  totalsByCurrency?: Record<string, number>;
  systemTotalsByCurrency?: Record<string, number>;
  unbalancedTransactionsCount: number;
  crossCurrencyTransactionsCount: number;
  accountDiscrepanciesCount?: number;
  accountDriftsCount?: number;
  discrepancies: string[];
  redemptionsFrozen: boolean;
  checkedAt: string;
}

export interface LedgerTransactionItem {
  id: string;
  transactionCode: string;
  tripId?: string;
  transactionType: string;
  currency: string;
  totalAmount?: number;
  totalDebit?: number;
  description?: string;
  referenceType?: string;
  referenceId?: string;
  reversalOf?: string;
  alreadyReversed: boolean;
  createdAt: string;
}

export interface LedgerEntryViewItem {
  id: string;
  accountId: string;
  accountNumber: string;
  accountType: string;
  currency: string;
  entryType: "DEBIT" | "CREDIT";
  amount: number;
  balanceAfter: number;
  createdAt: string;
}

export interface OutboxFailedItem {
  id: string;
  aggregateType: string;
  aggregateId: string;
  routingKey: string;
  status: string;
  attempts?: number;
  retryCount?: number;
  lastError?: string;
  createdAt: string;
}
