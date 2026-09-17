"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  ArrowLeft,
  CheckCircle2,
  XCircle,
  AlertTriangle,
} from "lucide-react";
import ConfirmModal from "@/components/ui/ConfirmModal";
import RejectModal from "@/components/ui/RejectModal";
import ImageModal from "@/components/ui/ImageModal";
import DocumentCard from "@/components/drivers/DocumentCard";
import FaceBiometricsTab from "@/components/drivers/FaceBiometricsTab";
import VehicleSpecsTab from "@/components/drivers/VehicleSpecsTab";
import FaceLogsTab from "@/components/drivers/FaceLogsTab";
import DriverProfileHeader, { DetailTabType } from "@/components/drivers/DriverProfileHeader";
import apiClient from "@/lib/api";
import { ApiResponse, DriverDetail, FaceVerificationLog } from "@/types";
import { KYC_DOCUMENTS } from "@/constants";

export default function DriverDetailPage() {
  const params = useParams();
  const driverId = params?.id as string;

  const [driver, setDriver] = useState<DriverDetail | null>(null);
  const [faceLogs, setFaceLogs] = useState<FaceVerificationLog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<DetailTabType>("DOCS");

  // Modal states
  const [showApproveModal, setShowApproveModal] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // High-res Image zoom modal
  const [previewImage, setPreviewImage] = useState<{
    url: string;
    title: string;
    subtitle?: string;
  } | null>(null);

  const fetchDriverData = async () => {
    if (!driverId) return;
    setIsLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<DriverDetail>>(
        `/admin/drivers/${driverId}`
      );
      if (res.data.success && res.data.data) {
        setDriver(res.data.data);
      }

      try {
        const logsRes = await apiClient.get<ApiResponse<FaceVerificationLog[]>>(
          `/admin/drivers/${driverId}/face-logs`
        );
        if (logsRes.data.success && logsRes.data.data) {
          setFaceLogs(logsRes.data.data);
        }
      } catch (logErr) {
        console.warn("Không thể tải face logs:", logErr);
      }
    } catch (err: any) {
      console.error("Lỗi tải chi tiết hồ sơ tài xế:", err);
      setNotification({
        type: "error",
        message:
          err?.response?.data?.message ||
          "Không thể tải hồ sơ tài xế. Vui lòng kiểm tra lại!",
      });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDriverData();
  }, [driverId]);

  const handleApprove = async () => {
    setIsSubmitting(true);
    try {
      const res = await apiClient.post<ApiResponse<void>>(
        `/admin/drivers/${driverId}/kyc/approve`
      );
      setShowApproveModal(false);
      setNotification({
        type: "success",
        message:
          res.data.message ||
          "Phê duyệt hồ sơ tài xế và xe điện thành công!",
      });
      await fetchDriverData();
    } catch (err: any) {
      console.error("Lỗi duyệt hồ sơ:", err);
      setNotification({
        type: "error",
        message: err?.response?.data?.message || "Phê duyệt hồ sơ thất bại!",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReject = async (reason: string) => {
    setIsSubmitting(true);
    try {
      const res = await apiClient.post<ApiResponse<void>>(
        `/admin/drivers/${driverId}/kyc/reject`,
        { rejectionReason: reason }
      );
      setShowRejectModal(false);
      setNotification({
        type: "success",
        message:
          res.data.message ||
          "Đã từ chối hồ sơ và gửi thông báo cho tài xế.",
      });
      await fetchDriverData();
    } catch (err: any) {
      console.error("Lỗi từ chối hồ sơ:", err);
      setNotification({
        type: "error",
        message: err?.response?.data?.message || "Từ chối hồ sơ thất bại!",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const openPreview = (url: string, title: string, subtitle?: string) => {
    setPreviewImage({ url, title, subtitle });
  };

  const getDocImageUrl = (docKey: string): string | undefined => {
    if (!driver) return undefined;
    if (docKey === "citizenCardFrontUrl") return driver.citizenCardFrontUrl;
    if (docKey === "citizenCardBackUrl") return driver.citizenCardBackUrl;
    if (docKey === "driverLicenseImageUrl") return driver.driverLicenseImageUrl;
    if (docKey === "registrationCertificateUrl") return driver.vehicle?.registrationCertificateUrl;
    return undefined;
  };

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto space-y-6">
        <div className="flex items-center gap-3">
          <Link
            href="/drivers"
            className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div className="h-6 w-48 bg-slate-800 rounded animate-pulse"></div>
        </div>
        <div className="p-12 rounded-2xl bg-slate-900/60 border border-slate-800 flex flex-col items-center justify-center gap-3 text-slate-400">
          <div className="w-8 h-8 border-3 border-emerald-500/20 border-t-emerald-500 rounded-full animate-spin"></div>
          <p className="text-xs font-medium">Đang tải hồ sơ tài xế...</p>
        </div>
      </div>
    );
  }

  if (!driver) {
    return (
      <div className="max-w-7xl mx-auto space-y-6">
        <Link
          href="/drivers"
          className="inline-flex items-center gap-2 text-xs text-slate-400 hover:text-white"
        >
          <ArrowLeft className="w-4 h-4" /> Quay lại danh sách
        </Link>
        <div className="p-12 rounded-2xl bg-slate-900/60 border border-slate-800 text-center space-y-3">
          <AlertTriangle className="w-12 h-12 mx-auto text-rose-400 opacity-60" />
          <h3 className="text-lg font-semibold text-white">Không tìm thấy hồ sơ</h3>
          <p className="text-xs text-slate-400">
            Hồ sơ tài xế này không tồn tại hoặc đã bị xóa khỏi hệ thống.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-12">
      {/* Top Breadcrumb & Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            href="/drivers"
            className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white hover:bg-slate-800 transition shadow-sm"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-2 text-xs text-slate-400">
              <Link href="/drivers" className="hover:text-slate-200">
                Quản lý tài xế
              </Link>
              <span>/</span>
              <span className="text-emerald-400 font-mono">
                Hồ sơ #{driver.driverId.substring(0, 8)}
              </span>
            </div>
            <h1 className="text-xl font-bold text-white mt-0.5">{driver.fullName}</h1>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-3">
          {driver.kycStatus === "PENDING" && (
            <>
              <button
                type="button"
                onClick={() => setShowRejectModal(true)}
                className="flex items-center gap-2 px-4 py-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 text-xs font-semibold transition shadow-sm"
              >
                <XCircle className="w-4 h-4" />
                <span>Từ chối hồ sơ</span>
              </button>
              <button
                type="button"
                onClick={() => setShowApproveModal(true)}
                className="flex items-center gap-2 px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-lg shadow-emerald-950 transition"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>Phê duyệt hồ sơ</span>
              </button>
            </>
          )}

          {driver.kycStatus === "APPROVED" && (
            <button
              type="button"
              onClick={() => setShowRejectModal(true)}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-900 hover:bg-rose-500/10 text-slate-400 hover:text-rose-400 border border-slate-800 hover:border-rose-500/30 text-xs font-medium transition"
            >
              <XCircle className="w-4 h-4" />
              <span>Thu hồi phê duyệt</span>
            </button>
          )}

          {driver.kycStatus === "REJECTED" && (
            <button
              type="button"
              onClick={() => setShowApproveModal(true)}
              className="flex items-center gap-2 px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-lg shadow-emerald-950 transition"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>Xem xét & Phê duyệt lại</span>
            </button>
          )}
        </div>
      </div>

      {/* Alert Notification Toast */}
      {notification && (
        <div
          className={`p-4 rounded-2xl border flex items-center justify-between animate-in fade-in ${notification.type === "success"
              ? "bg-emerald-950/40 border-emerald-500/30 text-emerald-300"
              : "bg-rose-950/40 border-rose-500/30 text-rose-300"
            }`}
        >
          <div className="flex items-center gap-3 text-sm">
            {notification.type === "success" ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
            ) : (
              <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
            )}
            <span>{notification.message}</span>
          </div>
          <button
            type="button"
            onClick={() => setNotification(null)}
            className="text-xs opacity-70 hover:opacity-100 underline"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Rejection Reason Notice */}
      {driver.kycStatus === "REJECTED" && driver.kycRejectionReason && (
        <div className="p-4 rounded-2xl bg-rose-950/20 border border-rose-800/40 text-rose-300 space-y-1">
          <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-rose-400">
            <XCircle className="w-4 h-4" />
            <span>Lý do từ chối hồ sơ hiện tại:</span>
          </div>
          <p className="text-sm pl-6">{driver.kycRejectionReason}</p>
        </div>
      )}

      {/* Profile Header & Tabs Navigation */}
      <DriverProfileHeader
        driver={driver}
        logsCount={faceLogs.length}
        activeTab={activeTab}
        onTabChange={setActiveTab}
      />

      {/* Tab 1: Documents Inspection (Rendered cleanly via map over KYC_DOCUMENTS) */}
      {activeTab === "DOCS" && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 animate-in fade-in">
          {KYC_DOCUMENTS.map((doc) => (
            <DocumentCard
              key={doc.key}
              title={doc.title}
              subtitle={doc.subtitle}
              badge={doc.badge}
              imageUrl={getDocImageUrl(doc.key)}
              onPreview={openPreview}
            />
          ))}
        </div>
      )}

      {/* Tab 2: Face Biometrics Tab */}
      {activeTab === "BIOMETRIC" && (
        <FaceBiometricsTab
          fullName={driver.fullName}
          facePortraitUrl={driver.facePortraitUrl}
          onPreview={openPreview}
        />
      )}

      {/* Tab 3: EV Vehicle Specs Tab */}
      {activeTab === "VEHICLE" && <VehicleSpecsTab vehicle={driver.vehicle} />}

      {/* Tab 4: Shift Verification Logs Tab */}
      {activeTab === "LOGS" && (
        <FaceLogsTab logs={faceLogs} onPreview={openPreview} />
      )}

      {/* Confirmation & Action Modals */}
      <ConfirmModal
        isOpen={showApproveModal}
        onClose={() => setShowApproveModal(false)}
        onConfirm={handleApprove}
        title="Xác nhận Phê duyệt Hồ sơ KYC"
        description={`Bạn có chắc chắn muốn phê duyệt hồ sơ cho tài xế "${driver.fullName}" và phương tiện xe điện "${driver.vehicle?.licensePlate || ''}"? Sau khi duyệt, tài xế sẽ được phép bật ca và nhận cuốc xe trong hệ thống.`}
        confirmText="Phê duyệt ngay"
        isLoading={isSubmitting}
      />

      <RejectModal
        isOpen={showRejectModal}
        onClose={() => setShowRejectModal(false)}
        onReject={handleReject}
        title={`Từ chối hồ sơ tài xế: ${driver.fullName}`}
        isLoading={isSubmitting}
      />

      {/* Zoomable Image Modal */}
      {previewImage && (
        <ImageModal
          isOpen={!!previewImage}
          onClose={() => setPreviewImage(null)}
          imageUrl={previewImage.url}
          title={previewImage.title}
          subtitle={previewImage.subtitle}
        />
      )}
    </div>
  );
}
