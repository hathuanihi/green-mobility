"use client";

import React from "react";
import Link from "next/link";
import {
  Car,
  Leaf,
  Activity,
  Trees,
  CheckCircle2,
  Users,
  ArrowRight,
  ShieldCheck,
} from "lucide-react";
import Header from "@/components/layout/Header";

export default function AdminDashboardPage() {
  return (
    <div className="max-w-7xl mx-auto space-y-8">
      <Header
        title="Trung tâm Điều hành & Báo cáo Carbon"
        description="Giám sát thời gian thực hoạt động xe điện và chỉ số giảm phát thải CO2 tại TP. Hồ Chí Minh"
        actions={
          <Link
            href="/drivers"
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-lg shadow-emerald-950 transition"
          >
            <Users className="w-3.5 h-3.5" />
            <span>Hồ sơ Chờ Duyệt KYC</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-2 hover:border-slate-700 transition">
          <div className="flex justify-between items-center text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">Tổng CO2 Đã Giảm</span>
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 flex items-center justify-center text-emerald-400">
              <Leaf className="w-4 h-4" />
            </div>
          </div>
          <p className="text-3xl font-extrabold text-white">
            124.85 <span className="text-lg font-normal text-emerald-400">tấn</span>
          </p>
          <p className="text-xs text-emerald-400 font-medium">+18.2% so với tháng trước</p>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-2 hover:border-slate-700 transition">
          <div className="flex justify-between items-center text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">Tương Đương Cây Xanh</span>
            <div className="w-8 h-8 rounded-lg bg-cyan-500/10 flex items-center justify-center text-cyan-400">
              <Trees className="w-4 h-4" />
            </div>
          </div>
          <p className="text-3xl font-extrabold text-white">
            2.08M <span className="text-lg font-normal text-cyan-400">ngày cây</span>
          </p>
          <p className="text-xs text-slate-400">Hấp thụ CO2 tích lũy theo IPCC</p>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-2 hover:border-slate-700 transition">
          <div className="flex justify-between items-center text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">Chuyến Xe Xanh</span>
            <div className="w-8 h-8 rounded-lg bg-amber-500/10 flex items-center justify-center text-amber-400">
              <Car className="w-4 h-4" />
            </div>
          </div>
          <p className="text-3xl font-extrabold text-white">158,420</p>
          <p className="text-xs text-slate-400">Thời gian ghép TB: 18.2 giây</p>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-2 hover:border-slate-700 transition">
          <div className="flex justify-between items-center text-slate-400">
            <span className="text-xs font-medium uppercase tracking-wider">Xe Đang Trực Tuyến</span>
            <div className="w-8 h-8 rounded-lg bg-indigo-500/10 flex items-center justify-center text-indigo-400">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <p className="text-3xl font-extrabold text-white">1,420</p>
          <p className="text-xs text-slate-400">980 E-Bike | 440 E-Car</p>
        </div>
      </div>

      {/* Quick Actions & Status */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-white">Đội xe & Trạng thái Ca trực tuyến (TP.HCM)</h3>
            <span className="text-xs text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-full border border-emerald-500/20 font-medium">
              Real-time Active
            </span>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between hover:border-slate-700 transition">
            <div className="space-y-1">
              <p className="text-sm font-semibold text-slate-200">VinFast Feliz S & Klara S (E-Bike 2 bánh)</p>
              <p className="text-xs text-slate-400">980 xe đang nhận cuốc • Tiết kiệm 45.4 gCO2/km</p>
            </div>
            <span className="px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-medium border border-emerald-500/20">
              Hoạt động 98%
            </span>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between hover:border-slate-700 transition">
            <div className="space-y-1">
              <p className="text-sm font-semibold text-slate-200">VinFast VF e34 & VF 5 (E-Car 4 chỗ)</p>
              <p className="text-xs text-slate-400">440 xe đang nhận cuốc • Tiết kiệm 54.5 gCO2/km</p>
            </div>
            <span className="px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-medium border border-emerald-500/20">
              Hoạt động 95%
            </span>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
          <h3 className="font-semibold text-white">Hệ số Lưới điện Quốc gia</h3>
          <div className="p-4 rounded-xl bg-emerald-950/20 border border-emerald-800/30 space-y-3">
            <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
              <CheckCircle2 className="w-4 h-4" /> Chuẩn Bộ TN&MT / IPCC
            </div>
            <p className="text-3xl font-extrabold text-white">
              722.1 <span className="text-sm font-normal text-slate-400">gCO2/kWh</span>
            </p>
            <p className="text-xs text-slate-400 leading-relaxed">
              Áp dụng cho toàn bộ tính toán phát thải gián tiếp của xe điện trong hệ thống điều hành.
            </p>
          </div>

          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
            <div className="flex items-center gap-2 text-slate-300 font-medium text-xs">
              <ShieldCheck className="w-4 h-4 text-cyan-400" />
              <span>Chính sách Bảo mật & KYC</span>
            </div>
            <p className="text-xs text-slate-400">
              Mỗi tài xế xe điện bắt buộc chụp selfie nhận diện khuôn mặt sinh trắc học trước khi mở ca nhận chuyến.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
