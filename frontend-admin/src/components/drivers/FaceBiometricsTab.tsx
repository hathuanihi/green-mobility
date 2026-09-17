"use client";

import React from "react";
import { UserCheck, CheckCircle2, ShieldCheck, ZoomIn } from "lucide-react";
import { BIOMETRIC_CONFIG } from "@/constants";
import { getFullMediaUrl } from "@/lib/api";

interface FaceBiometricsTabProps {
  fullName: string;
  facePortraitUrl?: string;
  onPreview: (url: string, title: string, subtitle?: string) => void;
}

export default function FaceBiometricsTab({
  fullName,
  facePortraitUrl,
  onPreview,
}: FaceBiometricsTabProps) {
  const fullUrl = getFullMediaUrl(facePortraitUrl);

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-in fade-in">
      {/* Portrait Photo */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <h3 className="font-semibold text-white text-sm flex items-center gap-2">
          <UserCheck className="w-4 h-4 text-emerald-400" />
          <span>Ảnh Chân Dung Chuẩn (Mẫu KYC)</span>
        </h3>

        <div className="relative aspect-square rounded-2xl bg-slate-950 border border-slate-800 overflow-hidden group flex items-center justify-center">
          {facePortraitUrl ? (
            <>
              <img
                src={fullUrl}
                alt="Chân dung tài xế"
                className="w-full h-full object-cover transition group-hover:scale-105 duration-300 select-none"
              />
              <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition flex items-center justify-center">
                <button
                  type="button"
                  onClick={() =>
                    onPreview(
                      fullUrl,
                      "Ảnh chân dung mẫu KYC",
                      `Tài xế: ${fullName}`
                    )
                  }
                  className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 text-white text-xs font-medium border border-slate-700 shadow-xl"
                >
                  <ZoomIn className="w-4 h-4 text-emerald-400" />
                  <span>Xem rõ khuôn mặt</span>
                </button>
              </div>
            </>
          ) : (
            <div className="text-center p-6 text-slate-500 space-y-1">
              <UserCheck className="w-10 h-10 mx-auto opacity-30" />
              <p className="text-xs">Chưa tải ảnh chân dung</p>
            </div>
          )}
        </div>
      </div>

      {/* Engine Parameters & Pipeline */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 md:col-span-2 space-y-5">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-white text-sm">
            Hệ Thống Trích Xuất Đặc Trưng Sinh Trắc Học
          </h3>
          <span className="px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-medium border border-emerald-500/20">
            {BIOMETRIC_CONFIG.MODEL_NAME}
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
            <p className="text-xs text-slate-400 font-medium">Vectơ đặc trưng chuẩn hóa</p>
            <p className="text-xl font-bold text-white">
              {BIOMETRIC_CONFIG.EMBEDDING_DIMENSIONS} Chiều
            </p>
            <p className="text-[11px] text-emerald-400 flex items-center gap-1">
              <CheckCircle2 className="w-3 h-3" />
              Đã lưu trữ và đánh chỉ mục
            </p>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
            <p className="text-xs text-slate-400 font-medium">Ngưỡng Chấp Nhận (Threshold τ)</p>
            <p className="text-xl font-bold text-emerald-400">
              ≥ {(BIOMETRIC_CONFIG.THRESHOLD * 100).toFixed(1)}%
            </p>
            <p className="text-[11px] text-slate-400">Cosine Similarity Score</p>
          </div>
        </div>

        <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80 space-y-3">
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-200">
            <ShieldCheck className="w-4 h-4 text-cyan-400" />
            <span>Quy trình Bảo mật & Chống Giả mạo (Anti-Spoofing):</span>
          </div>
          <ul className="text-xs text-slate-400 space-y-2 list-disc pl-5 leading-relaxed">
            <li>
              Mỗi lần tài xế kích hoạt ca trực tuyến (bật nhận chuyến), hệ thống bắt buộc chụp selfie trực tiếp.
            </li>
            <li>
              Selfie được vector hóa tức thì và so khớp Cosine với ảnh mẫu KYC trên.
            </li>
            <li>
              Nếu độ tương đồng &lt; 0.75, hệ thống lập tức khóa quyền bật ca và gửi cảnh báo đến Trung tâm Giám sát.
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
