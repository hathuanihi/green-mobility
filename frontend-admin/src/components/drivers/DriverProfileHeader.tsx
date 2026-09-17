"use client";

import React from "react";
import { Star, FileText, UserCheck, Car, History } from "lucide-react";
import { DriverDetail } from "@/types";
import StatusBadge from "@/components/ui/StatusBadge";

export type DetailTabType = "DOCS" | "BIOMETRIC" | "VEHICLE" | "LOGS";

interface DriverProfileHeaderProps {
  driver: DriverDetail;
  logsCount: number;
  activeTab: DetailTabType;
  onTabChange: (tab: DetailTabType) => void;
}

const TABS: { key: DetailTabType; label: string; icon: any }[] = [
  { key: "DOCS", label: "Đối chiếu Giấy tờ KYC (CCCD & GPLX)", icon: FileText },
  { key: "BIOMETRIC", label: "Sinh trắc học Khuôn mặt", icon: UserCheck },
  { key: "VEHICLE", label: "Phương tiện Xe điện", icon: Car },
  { key: "LOGS", label: "Nhật ký Xác thực Ca", icon: History },
];

export default function DriverProfileHeader({
  driver,
  logsCount,
  activeTab,
  onTabChange,
}: DriverProfileHeaderProps) {
  const avatarText = driver.fullName
    ? driver.fullName.substring(0, 2).toUpperCase()
    : "TX";

  return (
    <div className="p-6 rounded-3xl bg-slate-900/70 border border-slate-800 shadow-xl space-y-6">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex items-start gap-4">
          <div className="w-16 h-16 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center font-bold text-xl text-emerald-400 shrink-0 shadow-lg shadow-emerald-950">
            {avatarText}
          </div>
          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-3">
              <h2 className="text-2xl font-bold text-white tracking-tight">{driver.fullName}</h2>
              <StatusBadge status={driver.kycStatus} />
              {driver.isActiveShift && (
                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  Đang bật ca trực tuyến
                </span>
              )}
            </div>
            <p className="text-xs text-slate-400 flex flex-wrap items-center gap-3 pt-0.5">
              <span>
                Số điện thoại: <strong className="text-slate-200">{driver.phoneNumber}</strong>
              </span>
              <span>•</span>
              <span>
                CCCD: <strong className="text-slate-200 font-mono">{driver.citizenId}</strong>
              </span>
              <span>•</span>
              <span>
                GPLX ({driver.licenseClass}):{" "}
                <strong className="text-slate-200 font-mono">{driver.driverLicenseNumber}</strong>
              </span>
            </p>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="flex items-center gap-4 self-start md:self-auto pt-4 md:pt-0 border-t md:border-t-0 border-slate-800">
          <div className="text-center px-4 py-2 rounded-xl bg-slate-950 border border-slate-800/80">
            <div className="flex items-center justify-center gap-1 text-amber-400 text-xs font-semibold">
              <Star className="w-3.5 h-3.5 fill-amber-400" />
              <span>{driver.ratingAvg || "5.00"}</span>
            </div>
            <p className="text-[10px] text-slate-400 uppercase tracking-wider mt-0.5">Đánh giá</p>
          </div>

          <div className="text-center px-4 py-2 rounded-xl bg-slate-950 border border-slate-800/80">
            <p className="text-sm font-bold text-white">{driver.totalTripsCompleted || 0}</p>
            <p className="text-[10px] text-slate-400 uppercase tracking-wider mt-0.5">Chuyến đi</p>
          </div>

          <div className="text-center px-4 py-2 rounded-xl bg-slate-950 border border-slate-800/80">
            <p className="text-sm font-bold text-emerald-400">
              {driver.totalCo2SavedKg || "0.00"} <span className="text-[10px] text-slate-400">kg</span>
            </p>
            <p className="text-[10px] text-slate-400 uppercase tracking-wider mt-0.5">CO2 Giảm</p>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex items-center gap-2 border-b border-slate-800 pt-2 overflow-x-auto">
        {TABS.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.key;
          const labelWithCount =
            tab.key === "LOGS" ? `${tab.label} (${logsCount})` : tab.label;

          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => onTabChange(tab.key)}
              className={`flex items-center gap-2 pb-3 px-3 text-xs font-semibold transition border-b-2 whitespace-nowrap ${
                isActive
                  ? "border-emerald-400 text-emerald-400"
                  : "border-transparent text-slate-400 hover:text-slate-200"
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{labelWithCount}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
