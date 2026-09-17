"use client";

import React from "react";
import { History, ZoomIn, CheckCircle2, XCircle } from "lucide-react";
import { FaceVerificationLog } from "@/types";
import { BIOMETRIC_CONFIG } from "@/constants";
import { getFullMediaUrl } from "@/lib/api";

interface FaceLogsTabProps {
  logs: FaceVerificationLog[];
  onPreview: (url: string, title: string, subtitle?: string) => void;
}

export default function FaceLogsTab({ logs, onPreview }: FaceLogsTabProps) {
  return (
    <div className="rounded-2xl bg-slate-900/60 border border-slate-800 overflow-hidden shadow-xl animate-in fade-in space-y-4 p-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-semibold text-white text-base">
            Nhật Ký So Khớp Khuôn Mặt Bật Ca (Audit Logs)
          </h3>
          <p className="text-xs text-slate-400 mt-0.5">
            Lưu vết toàn bộ lịch sử bật ca với ảnh selfie và độ tương đồng Cosine
          </p>
        </div>
        <span className="text-xs text-slate-400 font-mono">
          Tổng số lần quét: {logs.length}
        </span>
      </div>

      {logs.length === 0 ? (
        <div className="p-12 text-center text-slate-500 space-y-2">
          <History className="w-10 h-10 mx-auto opacity-30" />
          <p className="text-sm">Tài xế chưa thực hiện lần xác thực bật ca nào.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-800">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/70 text-slate-400 uppercase tracking-wider">
                <th className="py-3 px-4">Thời gian</th>
                <th className="py-3 px-4">Ảnh Selfie</th>
                <th className="py-3 px-4">Độ tương đồng Cosine</th>
                <th className="py-3 px-4">Ngưỡng yêu cầu</th>
                <th className="py-3 px-4">Kết quả</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {logs.map((log) => {
                const fullSelfieUrl = getFullMediaUrl(log.selfieImageUrl);
                const scorePercent = (log.similarityScore * 100).toFixed(2);

                return (
                  <tr key={log.id} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4 text-slate-300 font-mono">
                      {new Date(log.verifiedAt).toLocaleString("vi-VN")}
                    </td>
                    <td className="py-3 px-4">
                      {log.selfieImageUrl ? (
                        <button
                          type="button"
                          onClick={() =>
                            onPreview(
                              fullSelfieUrl,
                              "Ảnh selfie bật ca",
                              `Độ tương đồng: ${scorePercent}%`
                            )
                          }
                          className="inline-flex items-center gap-1.5 px-2 py-1 rounded bg-slate-950 border border-slate-800 text-slate-300 hover:text-emerald-400 text-[11px] transition"
                        >
                          <ZoomIn className="w-3 h-3" />
                          <span>Xem ảnh</span>
                        </button>
                      ) : (
                        <span className="text-slate-500">—</span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`font-bold font-mono ${
                          log.isPassed ? "text-emerald-400" : "text-rose-400"
                        }`}
                      >
                        {scorePercent}%
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-400 font-mono">
                      ≥ {(BIOMETRIC_CONFIG.THRESHOLD * 100).toFixed(1)}%
                    </td>
                    <td className="py-3 px-4">
                      {log.isPassed ? (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                          <CheckCircle2 className="w-3 h-3" />
                          Đạt • Đã kích hoạt ca
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                          <XCircle className="w-3 h-3" />
                          Không khớp
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
