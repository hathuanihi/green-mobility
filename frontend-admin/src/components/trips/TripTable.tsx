"use client";

import React from "react";
import { Eye, MapPin, ArrowRight, User, Car, Leaf } from "lucide-react";
import { Trip } from "@/types";
import { VEHICLE_CONFIG } from "@/constants";
import { formatCurrency, formatTime } from "@/lib/formatters";
import TripStatusBadge from "./TripStatusBadge";

interface TripTableProps {
  trips: Trip[];
  isLoading: boolean;
  onViewDetails: (trip: Trip) => void;
}

export default function TripTable({
  trips,
  isLoading,
  onViewDetails,
}: TripTableProps) {
  if (isLoading) {
    return (
      <div className="w-full bg-slate-900 border border-slate-800 rounded-2xl p-12 text-center">
        <div className="w-10 h-10 border-4 border-emerald-500/20 border-t-emerald-500 rounded-full animate-spin mx-auto mb-3" />
        <p className="text-sm text-slate-400 font-medium">Đang tải dữ liệu cuốc xe từ máy chủ...</p>
      </div>
    );
  }

  if (trips.length === 0) {
    return (
      <div className="w-full bg-slate-900 border border-slate-800 rounded-2xl p-12 text-center text-slate-400">
        <div className="w-12 h-12 rounded-2xl bg-slate-800 flex items-center justify-center mx-auto mb-3 text-slate-500">
          <Car className="w-6 h-6" />
        </div>
        <h4 className="text-base font-semibold text-white mb-1">Không tìm thấy cuốc xe nào</h4>
        <p className="text-xs text-slate-500 max-w-sm mx-auto">
          Hiện tại không có chuyến đi nào phù hợp với bộ lọc hoặc từ khóa tìm kiếm của bạn.
        </p>
      </div>
    );
  }

  return (
    <div className="w-full overflow-hidden border border-slate-800 rounded-2xl bg-slate-900/60 shadow-xl backdrop-blur-sm">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-800 bg-slate-950/60 text-slate-400 text-xs font-semibold uppercase tracking-wider">
              <th className="py-3.5 px-4">Mã Cuốc Xe</th>
              <th className="py-3.5 px-4">Phương Tiện</th>
              <th className="py-3.5 px-4">Lộ Trình</th>
              <th className="py-3.5 px-4">Tài Xế Nhận</th>
              <th className="py-3.5 px-4">Cước Phí</th>
              <th className="py-3.5 px-4">Giảm CO2</th>
              <th className="py-3.5 px-4">Trạng Thái</th>
              <th className="py-3.5 px-4 text-right">Chi Tiết</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 text-slate-300">
            {trips.map((trip) => (
              <tr
                key={trip.tripId}
                className="hover:bg-slate-800/40 transition group cursor-pointer"
                onClick={() => onViewDetails(trip)}
              >
                {/* Trip Code */}
                <td className="py-4 px-4 font-mono font-bold text-white whitespace-nowrap">
                  <div>
                    <span className="text-emerald-400 group-hover:underline">{trip.tripCode}</span>
                    <p className="text-[11px] font-sans font-normal text-slate-500 mt-0.5">
                      {formatTime(trip.requestedAt)}
                    </p>
                  </div>
                </td>

                {/* Vehicle */}
                <td className="py-4 px-4 whitespace-nowrap">
                  <span className="text-xs text-slate-300 font-medium">
                    {VEHICLE_CONFIG[trip.vehicleType]?.icon}{" "}
                    {VEHICLE_CONFIG[trip.vehicleType]?.shortLabel || trip.vehicleType}
                  </span>
                </td>

                {/* Route */}
                <td className="py-4 px-4 max-w-xs">
                  <div className="space-y-0.5">
                    <p className="text-xs font-medium text-slate-200 truncate flex items-center gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 shrink-0" />
                      {trip.pickupAddress}
                    </p>
                    <p className="text-xs text-slate-400 truncate flex items-center gap-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 shrink-0" />
                      {trip.dropoffAddress}
                    </p>
                  </div>
                  <span className="text-[11px] text-slate-500 font-mono">
                    {trip.estimatedDistanceKm} km • {trip.estimatedDurationMinutes}m
                  </span>
                </td>

                {/* Driver */}
                <td className="py-4 px-4 whitespace-nowrap">
                  {trip.driver ? (
                    <div>
                      <p className="text-xs font-semibold text-white">{trip.driver.fullName}</p>
                      <p className="text-[11px] text-slate-400 font-mono">{trip.driver.licensePlate}</p>
                    </div>
                  ) : (
                    <span className="text-xs text-slate-500 italic">
                      {trip.status === "SEARCHING" ? "Đang quét..." : "Chưa có"}
                    </span>
                  )}
                </td>

                {/* Fare */}
                <td className="py-4 px-4 whitespace-nowrap font-mono font-bold text-slate-100">
                  {formatCurrency(trip.finalAmountVnd)}
                </td>

                {/* CO2 Saved */}
                <td className="py-4 px-4 whitespace-nowrap">
                  <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-400 font-mono">
                    <Leaf className="w-3 h-3" />
                    +{trip.co2SavedGrams}g
                  </span>
                </td>

                {/* Status */}
                <td className="py-4 px-4 whitespace-nowrap">
                  <TripStatusBadge status={trip.status} size="sm" />
                </td>

                {/* Action */}
                <td className="py-4 px-4 text-right whitespace-nowrap">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onViewDetails(trip);
                    }}
                    className="p-1.5 rounded-lg bg-slate-800/80 hover:bg-emerald-500/20 text-slate-400 hover:text-emerald-400 transition"
                    title="Xem chi tiết chuyến đi"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
