"use client";

import React, { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import {
  Leaf,
  ShieldCheck,
  Share2,
  Download,
  TreePine,
  Lightbulb,
  Smartphone,
  ExternalLink,
  Check,
  AlertCircle,
  RefreshCw,
} from "lucide-react";
import Link from "next/link";
import apiClient from "@/lib/api";
import { ApiResponse } from "@/types";

interface TripImpactReceiptResponse {
  id: string;
  tripId: string;
  tripCode: string;
  vehicleType: string;
  distanceM: number;
  co2SavedGrams: number;
  co2SavedKg: number;
  baselineGasolineCo2Grams: number;
  evEmittedCo2Grams: number;
  treeAbsorptionDaysEquiv: number;
  ledBulbHoursEquiv: number;
  smartphoneChargesEquiv: number;
  carbonCreditsEarned: number;
  loyaltyPointsEarned: number;
  shareableSlug: string;
  createdAt: string;
}

export default function EcoCertificatePage() {
  const params = useParams();
  const slug = (params?.slug as string) || "";
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [receipt, setReceipt] = useState<TripImpactReceiptResponse | null>(null);

  useEffect(() => {
    if (!slug) {
      setLoading(false);
      setError("Thiếu mã chứng nhận hợp lệ.");
      return;
    }
    let mounted = true;
    const loadReceipt = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await apiClient.get<ApiResponse<TripImpactReceiptResponse>>(
          `/carbon/receipts/share/${encodeURIComponent(slug)}`
        );
        if (mounted) {
          if (res.data.success && res.data.data) {
            setReceipt(res.data.data);
          } else {
            setError("Không tìm thấy chứng nhận tác động xanh này.");
          }
        }
      } catch (err: any) {
        if (mounted) {
          setError(err.response?.data?.message || "Chứng nhận không tồn tại hoặc chưa được phát hành.");
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };
    loadReceipt();
    return () => {
      mounted = false;
    };
  }, [slug]);

  const handleShare = () => {
    if (navigator.share && receipt) {
      navigator.share({
        title: "Chứng nhận Hành trình Xanh - Green Mobility",
        text: `Tôi vừa giảm thiểu ${Number(receipt.co2SavedKg || 0).toFixed(3)} kg CO2 cùng Green Mobility!`,
        url: window.location.href,
      });
    } else {
      navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    }
  };

  const handlePrint = () => window.print();

  const vehicleLabel = (type?: string) => {
    if (!type) return "Phương tiện Thuần điện";
    if (type.includes("MOTORBIKE")) return "Xe Máy Điện Thuần Chủng";
    if (type.includes("7SEAT")) return "Ô Tô Điện 7 Chỗ";
    if (type.includes("4SEAT")) return "Ô Tô Điện 4 Chỗ";
    return type;
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between p-4 sm:p-8 font-sans selection:bg-emerald-500 selection:text-white">
      <div className="max-w-4xl mx-auto w-full flex items-center justify-between py-3 border-b border-slate-800 text-xs text-slate-400">
        <div className="flex items-center gap-2">
          <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span>Cổng Xác Thực Chứng Chỉ Môi Trường Điện Tử Green Mobility</span>
        </div>
        <div className="font-mono text-[11px] text-slate-500">CV 11232/BNNMT-BĐKH • EF 618.70 g/kWh</div>
      </div>

      <main className="max-w-3xl mx-auto w-full my-8">
        {loading ? (
          <div className="bg-slate-900/70 border border-slate-800 rounded-3xl p-12 text-center space-y-3">
            <RefreshCw className="w-8 h-8 animate-spin text-emerald-400 mx-auto" />
            <p className="text-sm text-slate-300">Đang xác thực chứng chỉ Carbon từ hệ thống...</p>
          </div>
        ) : error || !receipt ? (
          <div className="bg-slate-900/70 border border-red-500/30 rounded-3xl p-10 text-center space-y-4">
            <AlertCircle className="w-10 h-10 text-red-400 mx-auto" />
            <h2 className="text-lg font-bold text-white">Không tìm thấy chứng nhận</h2>
            <p className="text-sm text-slate-400">{error}</p>
            <Link
              href="/"
              className="inline-flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 rounded-xl text-xs font-semibold text-slate-200"
            >
              Về trang chủ
            </Link>
          </div>
        ) : (
          <>
            <div className="relative bg-gradient-to-b from-slate-900/90 to-slate-950/95 border-2 border-emerald-500/40 rounded-3xl p-8 sm:p-12 shadow-2xl shadow-emerald-950/50 backdrop-blur-xl">
              <div className="text-center space-y-3 border-b border-slate-800 pb-8">
                <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 mb-2">
                  <Leaf className="w-8 h-8" />
                </div>
                <div className="inline-block px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-bold uppercase tracking-widest">
                  Green Mobility ESG Certificate
                </div>
                <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight uppercase">
                  Chứng Nhận Hành Trình Xanh &amp; Giảm Phát Thải
                </h1>
              </div>

              <div className="py-8 space-y-6 text-center">
                <p className="text-sm text-slate-300 max-w-xl mx-auto leading-relaxed">
                  Chuyến đi mã <strong className="text-white font-mono">{receipt.tripCode}</strong> với cự ly{" "}
                  <strong className="text-white">{((receipt.distanceM || 0) / 1000).toFixed(2)} km</strong> bằng{" "}
                  <strong className="text-emerald-400">{vehicleLabel(receipt.vehicleType)}</strong> đã trực tiếp cắt giảm phát thải đô thị.
                </p>

                <div className="my-6 p-6 rounded-2xl bg-gradient-to-r from-emerald-950/40 via-slate-900/60 to-emerald-950/40 border border-emerald-500/30 flex flex-col items-center">
                  <span className="text-xs uppercase font-bold tracking-widest text-emerald-400 mb-1">
                    Tổng Lượng CO2 Cắt Giảm Ròng
                  </span>
                  <div className="text-4xl sm:text-5xl font-black text-emerald-300">
                    {Number(receipt.co2SavedGrams || 0).toFixed(2)}{" "}
                    <span className="text-2xl font-bold text-slate-300">g CO₂</span>
                  </div>
                  <span className="text-xs text-slate-400 mt-1">
                    Tương đương {Number(receipt.co2SavedKg || 0).toFixed(3)} kg CO₂ •{" "}
                    +{Number(receipt.carbonCreditsEarned || 0).toFixed(4)} PCC • +{receipt.loyaltyPointsEarned || 0} Eco-Points
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
                  <div className="bg-slate-900/70 border border-slate-800 p-4 rounded-xl flex items-center gap-3 text-left">
                    <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 shrink-0">
                      <TreePine className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="text-lg font-bold text-white">
                        {Number(receipt.treeAbsorptionDaysEquiv || 0).toFixed(1)} ngày
                      </div>
                      <div className="text-[11px] text-slate-400">Hấp thụ của 1 cây xanh</div>
                    </div>
                  </div>
                  <div className="bg-slate-900/70 border border-slate-800 p-4 rounded-xl flex items-center gap-3 text-left">
                    <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 shrink-0">
                      <Lightbulb className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="text-lg font-bold text-white">
                        {Number(receipt.ledBulbHoursEquiv || 0).toFixed(1)} giờ
                      </div>
                      <div className="text-[11px] text-slate-400">Thắp sáng bóng đèn LED 10W</div>
                    </div>
                  </div>
                  <div className="bg-slate-900/70 border border-slate-800 p-4 rounded-xl flex items-center gap-3 text-left">
                    <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400 shrink-0">
                      <Smartphone className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="text-lg font-bold text-white">
                        {Number(receipt.smartphoneChargesEquiv || 0).toFixed(1)} lần
                      </div>
                      <div className="text-[11px] text-slate-400">Sạc đầy Smartphone</div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="border-t border-slate-800 pt-6 mt-2 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-400">
                <div className="text-left space-y-1">
                  <div>
                    <span className="text-slate-500">Mã chứng nhận: </span>
                    <span className="font-mono font-bold text-slate-200">{receipt.shareableSlug}</span>
                  </div>
                  <div>
                    <span className="text-slate-500">Ngày ghi nhận: </span>
                    <span>{new Date(receipt.createdAt).toLocaleString("vi-VN")}</span>
                  </div>
                </div>
                <div className="flex items-center gap-2.5 px-3 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 font-semibold text-xs">
                  <ShieldCheck className="w-5 h-5 text-emerald-400" />
                  <span>Xác Thực Dữ Liệu Thật</span>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-center gap-3 mt-6 print:hidden">
              <button
                onClick={handleShare}
                className="flex items-center gap-2 px-5 py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white font-semibold text-sm rounded-xl transition"
              >
                {copied ? <Check className="w-4 h-4" /> : <Share2 className="w-4 h-4" />}
                <span>{copied ? "Đã sao chép liên kết!" : "Chia sẻ chứng nhận"}</span>
              </button>
              <button
                onClick={handlePrint}
                className="flex items-center gap-2 px-5 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 font-medium text-sm rounded-xl border border-slate-700 transition"
              >
                <Download className="w-4 h-4" />
                <span>In / Lưu PDF</span>
              </button>
              <Link
                href="/"
                className="flex items-center gap-2 px-5 py-2.5 text-xs text-slate-400 hover:text-slate-200 transition"
              >
                <span>Về Cổng quản trị</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </Link>
            </div>
          </>
        )}
      </main>

      <footer className="text-center text-xs text-slate-500 py-4 border-t border-slate-900 max-w-4xl mx-auto w-full">
        © 2026 Green Mobility Platform. Hệ số lưới điện 618.70 gCO₂/kWh theo CV 11232/BNNMT-BĐKH.
      </footer>
    </div>
  );
}
