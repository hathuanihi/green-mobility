"use client";

import React, { useState } from "react";
import { useParams } from "next/navigation";
import {
  Leaf,
  ShieldCheck,
  Award,
  Share2,
  Download,
  Calendar,
  Sparkles,
  TreePine,
  Lightbulb,
  Smartphone,
  ExternalLink,
  CheckCircle2,
  Copy,
  Check,
} from "lucide-react";
import Link from "next/link";

export default function EcoCertificatePage() {
  const params = useParams();
  const slug = (params?.slug as string) || "GM-ECO-2026-9872";
  const [copied, setCopied] = useState(false);

  // Sample or decoded data based on slug
  const certificateData = {
    certId: slug.toUpperCase(),
    customerName: "Nguyễn Văn An",
    date: new Date().toLocaleDateString("vi-VN", {
      year: "numeric",
      month: "long",
      day: "numeric",
    }),
    vehicleType: "Xe Máy Điện VinFast Feliz S",
    distanceKm: 14.6,
    co2SavedKg: 1.04,
    co2SavedGrams: 1042,
    treeEquivDays: 14.1,
    ledEquivHours: 78.0,
    smartphoneCharges: 128,
    ecoPoints: 104,
    carbonCreditsEarned: 0.00104,
    verificationHash: "0x8f4c2e91b539a2d6e4b81c7f93a1024e",
    baselineGasolineFactor: "42.0 g CO2/km",
    evGridFactor: "0.6552 kg CO2/kWh",
  };

  const handleShare = () => {
    if (navigator.share) {
      navigator.share({
        title: "Chứng nhận Hành trình Xanh - Green Mobility",
        text: `Tôi vừa giảm thiểu ${certificateData.co2SavedKg}kg CO2 cùng Green Mobility!`,
        url: window.location.href,
      });
    } else {
      navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between p-4 sm:p-8 font-sans selection:bg-emerald-500 selection:text-white">
      {/* Background radial glows */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none -z-10">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-emerald-500/10 rounded-full blur-[140px]" />
        <div className="absolute bottom-10 right-1/4 w-[400px] h-[400px] bg-teal-500/10 rounded-full blur-[120px]" />
      </div>

      {/* Top Banner */}
      <div className="max-w-4xl mx-auto w-full flex items-center justify-between py-3 border-b border-slate-800 text-xs text-slate-400">
        <div className="flex items-center gap-2">
          <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span>Cổng Xác Thực Chứng Chỉ Môi Trường Điện Tử Green Mobility</span>
        </div>
        <div className="font-mono text-[11px] text-slate-500">ISO 14064-1 & IPCC 2006/2019</div>
      </div>

      {/* Main Certificate Content */}
      <main className="max-w-3xl mx-auto w-full my-8">
        <div className="relative bg-gradient-to-b from-slate-900/90 to-slate-950/95 border-2 border-emerald-500/40 rounded-3xl p-8 sm:p-12 shadow-2xl shadow-emerald-950/50 backdrop-blur-xl">
          {/* Decorative Corner Borders */}
          <div className="absolute top-3 left-3 w-6 h-6 border-t-2 border-l-2 border-emerald-400/70 rounded-tl-lg" />
          <div className="absolute top-3 right-3 w-6 h-6 border-t-2 border-r-2 border-emerald-400/70 rounded-tr-lg" />
          <div className="absolute bottom-3 left-3 w-6 h-6 border-b-2 border-l-2 border-emerald-400/70 rounded-bl-lg" />
          <div className="absolute bottom-3 right-3 w-6 h-6 border-b-2 border-r-2 border-emerald-400/70 rounded-br-lg" />

          {/* Header */}
          <div className="text-center space-y-3 border-b border-slate-800 pb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 shadow-lg shadow-emerald-900/40 mb-2">
              <Leaf className="w-8 h-8" />
            </div>

            <div className="inline-block px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-bold uppercase tracking-widest">
              Green Mobility ESG Certificate
            </div>

            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight uppercase">
              Chứng Nhận Hành Trình Xanh & Giảm Phát Thải
            </h1>
            <p className="text-sm text-slate-400 max-w-lg mx-auto">
              Hệ thống giao thông thông minh xác nhận đóng góp giảm thiểu khí nhà kính trực tiếp qua việc sử dụng phương tiện thuần điện 100%
            </p>
          </div>

          {/* Certificate Body */}
          <div className="py-8 space-y-6 text-center">
            <div className="space-y-1">
              <span className="text-xs uppercase tracking-wider text-slate-400">Trao tặng cho Người đồng hành Xanh</span>
              <h2 className="text-2xl sm:text-3xl font-black text-emerald-300 font-serif">
                {certificateData.customerName}
              </h2>
            </div>

            <p className="text-sm text-slate-300 max-w-xl mx-auto leading-relaxed">
              Đã hoàn thành chuyến đi thuần điện cự ly <strong className="text-white">{certificateData.distanceKm} km</strong> bằng phương tiện{" "}
              <strong className="text-emerald-400">{certificateData.vehicleType}</strong>, trực tiếp ngăn chặn việc phát thải khí carbon ra môi trường đô thị.
            </p>

            {/* Impact Metric Spotlight */}
            <div className="my-6 p-6 rounded-2xl bg-gradient-to-r from-emerald-950/40 via-slate-900/60 to-emerald-950/40 border border-emerald-500/30 flex flex-col items-center justify-center">
              <span className="text-xs uppercase font-bold tracking-widest text-emerald-400 mb-1">
                Tổng Lượng CO2 Cắt Giảm Ròng
              </span>
              <div className="text-4xl sm:text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-emerald-300 via-teal-200 to-emerald-400">
                {certificateData.co2SavedGrams} <span className="text-2xl font-bold text-slate-300">g CO₂</span>
              </div>
              <span className="text-xs text-slate-400 mt-1">
                Tương đương {certificateData.co2SavedKg} kg CO₂ theo chuẩn kiểm kê khí thải MONRE
              </span>
            </div>

            {/* Real-world Equivalencies */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
              <div className="bg-slate-900/70 border border-slate-800 p-4 rounded-xl flex items-center gap-3 text-left">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 shrink-0">
                  <TreePine className="w-5 h-5" />
                </div>
                <div>
                  <div className="text-lg font-bold text-white">{certificateData.treeEquivDays} ngày</div>
                  <div className="text-[11px] text-slate-400">Hấp thụ của 1 cây xanh</div>
                </div>
              </div>

              <div className="bg-slate-900/70 border border-slate-800 p-4 rounded-xl flex items-center gap-3 text-left">
                <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 shrink-0">
                  <Lightbulb className="w-5 h-5" />
                </div>
                <div>
                  <div className="text-lg font-bold text-white">{certificateData.ledEquivHours} giờ</div>
                  <div className="text-[11px] text-slate-400">Thắp sáng bóng đèn LED</div>
                </div>
              </div>

              <div className="bg-slate-900/70 border border-slate-800 p-4 rounded-xl flex items-center gap-3 text-left">
                <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400 shrink-0">
                  <Smartphone className="w-5 h-5" />
                </div>
                <div>
                  <div className="text-lg font-bold text-white">{certificateData.smartphoneCharges} lần</div>
                  <div className="text-[11px] text-slate-400">Sạc đầy Smartphone</div>
                </div>
              </div>
            </div>
          </div>

          {/* Official Verification Seal & Details */}
          <div className="border-t border-slate-800 pt-6 mt-2 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-400">
            <div className="text-left space-y-1">
              <div>
                <span className="text-slate-500">Mã chứng nhận: </span>
                <span className="font-mono font-bold text-slate-200">{certificateData.certId}</span>
              </div>
              <div>
                <span className="text-slate-500">Chữ ký toàn vẹn (SHA-256): </span>
                <span className="font-mono text-emerald-400">{certificateData.verificationHash}</span>
              </div>
              <div>
                <span className="text-slate-500">Ngày cấp: </span>
                <span>{certificateData.date}</span>
              </div>
            </div>

            {/* Seal Graphic */}
            <div className="flex items-center gap-2.5 px-3 py-2 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 font-semibold text-xs">
              <ShieldCheck className="w-5 h-5 text-emerald-400" />
              <span>Xác Thực Chính Thức</span>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-wrap items-center justify-center gap-3 mt-6 print:hidden">
          <button
            onClick={handleShare}
            className="flex items-center gap-2 px-5 py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white font-semibold text-sm rounded-xl transition shadow-lg shadow-emerald-950/40"
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
      </main>

      {/* Footer */}
      <footer className="text-center text-xs text-slate-500 py-4 border-t border-slate-900 max-w-4xl mx-auto w-full">
        © 2026 Green Mobility Platform. Chuẩn hóa đo đạc theo IPCC 2006/2019 Guidelines & Hệ số phát thải lưới điện Việt Nam.
      </footer>
    </div>
  );
}
