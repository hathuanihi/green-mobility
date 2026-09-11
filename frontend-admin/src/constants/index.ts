import { KycStatus, VehicleType } from "@/types";
import { Clock, CheckCircle2, XCircle } from "lucide-react";

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

export const VEHICLE_TYPE_LABELS: Record<VehicleType, string> = {
  ELECTRIC_MOTORBIKE: "Xe máy điện (E-Bike 2 bánh)",
  ELECTRIC_CAR_4SEAT: "Ô tô điện 4 chỗ (E-Car)",
  ELECTRIC_CAR_7SEAT: "Ô tô điện 7 chỗ (E-Car)",
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
