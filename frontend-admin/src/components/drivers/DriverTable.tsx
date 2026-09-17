"use client";

import React from "react";
import Link from "next/link";
import { Users, Car, BatteryCharging, Eye, ArrowUpRight } from "lucide-react";
import { DriverSummary } from "@/types";
import StatusBadge from "@/components/ui/StatusBadge";

interface DriverTableProps {
  drivers: DriverSummary[];
  isLoading: boolean;
  error: string | null;
  searchQuery: string;
  onRetry: () => void;
  onClearFilter: () => void;
}

export default function DriverTable({
  drivers,
  isLoading,
  error,
  searchQuery,
  onRetry,
  onClearFilter,
}: DriverTableProps) {
  if (isLoading) {
    return (
      <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-16 flex flex-col items-center justify-center gap-3 text-slate-400">
        <div className="w-8 h-8 border-3 border-emerald-500/20 border-t-emerald-500 rounded-full animate-spin"></div>
        <p className="text-xs">Đang tải danh sách tài xế...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-12 text-center space-y-3">
        <p className="text-rose-400 text-sm">{error}</p>
        <button
          type="button"
          onClick={onRetry}
          className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs rounded-xl font-medium transition"
        >
          Thử lại
        </button>
      </div>
    );
  }

  if (drivers.length === 0) {
    return (
      <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-16 text-center space-y-3 text-slate-500">
        <Users className="w-12 h-12 mx-auto opacity-30" />
        <p className="text-sm font-medium text-slate-400">
          {searchQuery
            ? `Không tìm thấy tài xế nào khớp với "${searchQuery}"`
            : "Không có hồ sơ nào trong trạng thái này"}
        </p>
        <button
          type="button"
          onClick={onClearFilter}
          className="text-xs text-emerald-400 hover:underline"
        >
          Xem tất cả hồ sơ
        </button>
      </div>
    );
  }

  return (
    <div className="rounded-2xl bg-slate-900/60 border border-slate-800 overflow-hidden shadow-xl">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-800 bg-slate-950/60 text-slate-400 text-xs uppercase tracking-wider">
              <th className="py-3.5 px-4 font-semibold">Tài xế</th>
              <th className="py-3.5 px-4 font-semibold">Số CCCD / GPLX</th>
              <th className="py-3.5 px-4 font-semibold">Phương tiện xe điện</th>
              <th className="py-3.5 px-4 font-semibold">Biển số</th>
              <th className="py-3.5 px-4 font-semibold">Trạng thái KYC</th>
              <th className="py-3.5 px-4 font-semibold">Ngày nộp</th>
              <th className="py-3.5 px-4 font-semibold text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {drivers.map((driver) => {
              const avatarText = driver.fullName
                ? driver.fullName.substring(0, 2).toUpperCase()
                : "TX";

              return (
                <tr
                  key={driver.driverId}
                  className="hover:bg-slate-800/40 transition group"
                >
                  <td className="py-3.5 px-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center font-bold text-emerald-400 text-xs shrink-0">
                        {avatarText}
                      </div>
                      <div>
                        <p className="font-semibold text-slate-100 group-hover:text-emerald-400 transition">
                          {driver.fullName}
                        </p>
                        <p className="text-xs text-slate-400">{driver.phoneNumber}</p>
                      </div>
                    </div>
                  </td>

                  <td className="py-3.5 px-4">
                    <p className="text-slate-300 font-mono text-xs">{driver.citizenId}</p>
                    <p className="text-slate-400 font-mono text-xs">
                      GPLX: {driver.licenseNumber}
                    </p>
                  </td>

                  <td className="py-3.5 px-4">
                    <div className="flex items-center gap-2">
                      <Car className="w-4 h-4 text-emerald-400 shrink-0" />
                      <span className="text-slate-200 font-medium text-xs">
                        {driver.vehicleModel}
                      </span>
                    </div>
                    {driver.batteryCapacityKwh && (
                      <p className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
                        <BatteryCharging className="w-3 h-3 text-cyan-400" />
                        <span>{driver.batteryCapacityKwh} kWh</span>
                      </p>
                    )}
                  </td>

                  <td className="py-3.5 px-4">
                    <span className="px-2.5 py-1 rounded-lg bg-slate-950 border border-slate-800 text-slate-200 font-mono text-xs font-semibold tracking-wider">
                      {driver.licensePlate}
                    </span>
                  </td>

                  <td className="py-3.5 px-4">
                    <StatusBadge status={driver.kycStatus} size="sm" />
                  </td>

                  <td className="py-3.5 px-4 text-xs text-slate-400">
                    {driver.submittedAt
                      ? new Date(driver.submittedAt).toLocaleDateString("vi-VN", {
                          day: "2-digit",
                          month: "2-digit",
                          year: "numeric",
                          hour: "2-digit",
                          minute: "2-digit",
                        })
                      : "—"}
                  </td>

                  <td className="py-3.5 px-4 text-right">
                    <Link
                      href={`/drivers/${driver.driverId}`}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-600/20 hover:bg-emerald-600 text-emerald-400 hover:text-white border border-emerald-500/30 text-xs font-semibold transition group-hover:shadow-md group-hover:shadow-emerald-950"
                    >
                      <Eye className="w-3.5 h-3.5" />
                      <span>Xem & Phê duyệt</span>
                      <ArrowUpRight className="w-3 h-3" />
                    </Link>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
