import { KycStatus, VehicleType, TripStatus } from "@/types";
import {
  Clock,
  CheckCircle2,
  XCircle,
  Search,
  Zap,
  Navigation,
  MapPin,
  CheckCheck,
  AlertCircle,
} from "lucide-react";

export const KYC_STATUS_CONFIG: Record<
  KycStatus,
  {
    label: string;
    description: string;
    badgeClass: string;
    bgHoverClass: string;
    borderActiveClass: string;
    shadowClass: string;
    icon: any;
    color: string;
  }
> = {
  PENDING: {
    label: "Chờ duyệt KYC",
    description: "Cần quản trị viên kiểm tra",
    badgeClass: "bg-amber-500/10 text-amber-400 border-amber-500/20",
    bgHoverClass: "bg-amber-950/20",
    borderActiveClass: "border-amber-500/40",
    shadowClass: "shadow-amber-950/30",
    icon: Clock,
    color: "amber",
  },
  APPROVED: {
    label: "Đã duyệt",
    description: "Đủ điều kiện nhận cuốc",
    badgeClass: "bg-emerald-500/10 text-emerald-400 border-emerald-500/20",
    bgHoverClass: "bg-emerald-950/20",
    borderActiveClass: "border-emerald-500/40",
    shadowClass: "shadow-emerald-950/30",
    icon: CheckCircle2,
    color: "emerald",
  },
  REJECTED: {
    label: "Bị từ chối",
    description: "Yêu cầu bổ sung lại ảnh",
    badgeClass: "bg-rose-500/10 text-rose-400 border-rose-500/20",
    bgHoverClass: "bg-rose-950/20",
    borderActiveClass: "border-rose-500/40",
    shadowClass: "shadow-rose-950/30",
    icon: XCircle,
    color: "rose",
  },
};

export const CARBON_CONSTANTS = {
  TREE_DAILY_ABSORPTION_GRAMS: 60.0,
  LED_HOURLY_GRAMS: 7.221,
} as const;

export const ACTIVE_TRIP_STATUSES: readonly TripStatus[] = [
  "MATCHED",
  "DRIVER_ARRIVING",
  "ARRIVED",
  "IN_TRIP",
] as const;

export function isActiveTrip(status: TripStatus): boolean {
  return ACTIVE_TRIP_STATUSES.includes(status);
}

export const VEHICLE_CONFIG: Record<
  VehicleType,
  {
    label: string;
    shortLabel: string;
    icon: string;
  }
> = {
  ELECTRIC_MOTORBIKE: {
    label: "Xe máy điện (E-Bike 2 bánh)",
    shortLabel: "E-Bike",
    icon: "🛵",
  },
  ELECTRIC_CAR_4SEAT: {
    label: "Ô tô điện 4 chỗ (E-Car)",
    shortLabel: "E-Car 4S",
    icon: "🚗",
  },
  ELECTRIC_CAR_7SEAT: {
    label: "Ô tô điện 7 chỗ (E-Car)",
    shortLabel: "E-Car 7S",
    icon: "🚙",
  },
};

export const VEHICLE_TYPE_LABELS: Record<VehicleType, string> = {
  ELECTRIC_MOTORBIKE: VEHICLE_CONFIG.ELECTRIC_MOTORBIKE.label,
  ELECTRIC_CAR_4SEAT: VEHICLE_CONFIG.ELECTRIC_CAR_4SEAT.label,
  ELECTRIC_CAR_7SEAT: VEHICLE_CONFIG.ELECTRIC_CAR_7SEAT.label,
};


export const BIOMETRIC_CONFIG = {
  THRESHOLD: 0.75, // Ngưỡng Cosine Similarity τ >= 75%
  EMBEDDING_DIMENSIONS: 512,
  MODEL_NAME: "ArcFace / FaceNet 512D",
};

export const KYC_DOCUMENTS = [
  {
    key: "citizenCardFrontUrl",
    title: "1. CCCD / Định danh (Mặt trước)",
    subtitle: "Ảnh chụp rõ nét 4 góc mặt trước căn cước công dân",
    badge: "Bắt buộc",
  },
  {
    key: "citizenCardBackUrl",
    title: "2. CCCD / Định danh (Mặt sau)",
    subtitle: "Kiểm tra chip điện tử & vân tay",
    badge: "Bắt buộc",
  },
  {
    key: "driverLicenseImageUrl",
    title: "3. Giấy phép lái xe (GPLX)",
    subtitle: "Số hiệu GPLX và hạng giấy phép điều khiển",
    badge: "Bắt buộc",
  },
  {
    key: "registrationCertificateUrl",
    title: "4. Giấy đăng ký xe (Cà vẹt xe)",
    subtitle: "Biển số đăng ký và thông tin chủ phương tiện",
    badge: "Bắt buộc",
  },
] as const;

export const TRIP_STATUS_CONFIG: Record<
  TripStatus,
  {
    label: string;
    badgeClass: string;
    icon: any;
    color: string;
  }
> = {
  REQUESTED: {
    label: "Yêu cầu mới",
    badgeClass: "bg-blue-500/10 text-blue-400 border-blue-500/20",
    icon: Clock,
    color: "blue",
  },
  SEARCHING: {
    label: "Đang tìm tài xế",
    badgeClass: "bg-amber-500/10 text-amber-400 border-amber-500/20",
    icon: Search,
    color: "amber",
  },
  MATCHED: {
    label: "Đã ghép tài xế",
    badgeClass: "bg-cyan-500/10 text-cyan-400 border-cyan-500/20",
    icon: Zap,
    color: "cyan",
  },
  DRIVER_ARRIVING: {
    label: "Tài xế đang đến",
    badgeClass: "bg-indigo-500/10 text-indigo-400 border-indigo-500/20",
    icon: Navigation,
    color: "indigo",
  },
  ARRIVED: {
    label: "Đã tới điểm đón",
    badgeClass: "bg-teal-500/10 text-teal-400 border-teal-500/20",
    icon: MapPin,
    color: "teal",
  },
  IN_TRIP: {
    label: "Đang di chuyển",
    badgeClass: "bg-emerald-500/10 text-emerald-400 border-emerald-500/20",
    icon: Navigation,
    color: "emerald",
  },
  COMPLETED: {
    label: "Đã hoàn thành",
    badgeClass: "bg-purple-500/10 text-purple-400 border-purple-500/20",
    icon: CheckCheck,
    color: "purple",
  },
  CANCELLED: {
    label: "Đã hủy",
    badgeClass: "bg-rose-500/10 text-rose-400 border-rose-500/20",
    icon: AlertCircle,
    color: "rose",
  },
};

