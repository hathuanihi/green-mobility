"use client";

import React, { useEffect } from "react";
import {
  X,
  MapPin,
  Car,
  User,
  Phone,
  Star,
  Leaf,
  Lightbulb,
  Clock,
  CheckCircle2,
  AlertCircle,
  CreditCard,
  ArrowRight,
  TrendingUp,
} from "lucide-react";
import { Trip } from "@/types";
import { VEHICLE_TYPE_LABELS } from "@/constants";
import { formatCurrency, formatDateTime, calculateCarbonEquivalents } from "@/lib/formatters";
import TripStatusBadge from "./TripStatusBadge";

interface TripDetailModalProps {
  trip: Trip | null;
  onClose: () => void;
}

export default function TripDetailModal({ trip, onClose }: TripDetailModalProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  if (!trip) return null;

  const { treeDays, ledHours } = calculateCarbonEquivalents(trip.co2SavedGrams);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 text-slate-200">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-4 mb-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <TrendingUp className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-bold text-white tracking-tight">{trip.tripCode}</h3>
                <TripStatusBadge status={trip.status} size="sm" />
              </div>
              <p className="text-xs text-slate-400">
                Khởi tạo lúc: {formatDateTime(trip.requestedAt)}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-6">
          {/* Route Section */}
          <div className="bg-slate-950/50 border border-slate-800/80 rounded-xl p-4">
            <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
              Lộ trình di chuyển
            </h4>
            <div className="space-y-3 relative pl-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-gradient-to-b before:from-emerald-400 before:to-cyan-500">
              <div className="relative">
                <span className="absolute -left-6 top-1 w-3 h-3 rounded-full bg-emerald-400 border-2 border-slate-900" />
                <p className="text-xs text-slate-400 font-medium">Điểm đón khách</p>
                <p className="text-sm font-semibold text-white">{trip.pickupAddress}</p>
              </div>
              <div className="relative">
                <span className="absolute -left-6 top-1 w-3 h-3 rounded-full bg-cyan-400 border-2 border-slate-900" />
                <p className="text-xs text-slate-400 font-medium">Điểm trả khách</p>
                <p className="text-sm font-semibold text-white">{trip.dropoffAddress}</p>
              </div>
            </div>
            <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-300">
              <span>
                Quãng đường: <strong className="text-emerald-400 font-semibold">{trip.estimatedDistanceKm} km</strong>
              </span>
              <span>
                Thời gian dự kiến: <strong className="text-cyan-400 font-semibold">{trip.estimatedDurationMinutes} phút</strong>
              </span>
              <span>
                Phương tiện: <strong className="text-slate-100">{VEHICLE_TYPE_LABELS[trip.vehicleType] || trip.vehicleType}</strong>
              </span>
            </div>
          </div>

          {/* Driver Information (if assigned) */}
          {trip.driver ? (
            <div className="bg-slate-950/50 border border-slate-800/80 rounded-xl p-4">
              <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
                Tài xế tiếp nhận
              </h4>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300 overflow-hidden">
                    {trip.driver.avatarUrl ? (
                      <img src={trip.driver.avatarUrl} alt={trip.driver.fullName} className="w-full h-full object-cover" />
                    ) : (
                      <User className="w-6 h-6" />
                    )}
                  </div>
                  <div>
                    <h5 className="font-bold text-white text-sm">{trip.driver.fullName}</h5>
                    <p className="text-xs text-slate-400 flex items-center gap-1.5 mt-0.5">
                      <Phone className="w-3 h-3 text-emerald-400" />
                      {trip.driver.phoneNumber}
                    </p>
                    <div className="flex items-center gap-1 mt-1 text-xs text-amber-400">
                      <Star className="w-3 h-3 fill-amber-400" />
                      <span>{trip.driver.ratingAvg?.toFixed(2) || "5.00"}</span>
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-medium">
                    <Car className="w-3.5 h-3.5" />
                    <span>{trip.driver.vehicleModel}</span>
                  </div>
                  <p className="text-xs font-mono font-bold text-slate-300 mt-1">
                    {trip.driver.licensePlate}
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="p-3 bg-slate-950/30 border border-dashed border-slate-800 rounded-xl text-center text-xs text-slate-400">
              {trip.status === "SEARCHING"
                ? "Matching Engine đang quét tìm tài xế xe điện gần nhất..."
                : "Chưa có tài xế nhận cuốc xe này."}
            </div>
          )}

          {/* Green Impact Card */}
          <div className="bg-gradient-to-br from-emerald-950/40 via-slate-900 to-teal-950/30 border border-emerald-500/20 rounded-xl p-4">
            <div className="flex items-center gap-2 mb-3 text-emerald-400">
              <Leaf className="w-4 h-4" />
              <h4 className="text-xs font-bold uppercase tracking-wider">Hóa đơn Tác động Môi trường</h4>
            </div>
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="p-2.5 bg-slate-900/80 rounded-lg border border-emerald-500/10">
                <p className="text-xs text-slate-400">CO2 Giảm được</p>
                <p className="text-base font-extrabold text-emerald-400 mt-0.5">
                  +{trip.co2SavedGrams} <span className="text-xs font-normal">g</span>
                </p>
              </div>
              <div className="p-2.5 bg-slate-900/80 rounded-lg border border-emerald-500/10">
                <p className="text-xs text-slate-400">Cây xanh tương đương</p>
                <p className="text-base font-extrabold text-teal-400 mt-0.5">
                  ~{treeDays} <span className="text-xs font-normal">ngày</span>
                </p>
              </div>
              <div className="p-2.5 bg-slate-900/80 rounded-lg border border-emerald-500/10">
                <p className="text-xs text-slate-400">Bóng đèn LED 10W</p>
                <p className="text-base font-extrabold text-amber-400 mt-0.5">
                  ~{ledHours} <span className="text-xs font-normal">giờ</span>
                </p>
              </div>
            </div>
          </div>

          {/* Financial & Payment */}
          <div className="flex items-center justify-between p-4 bg-slate-950/50 border border-slate-800 rounded-xl">
            <div className="flex items-center gap-2 text-xs text-slate-400">
              <CreditCard className="w-4 h-4 text-slate-400" />
              <span>Thanh toán: <strong className="text-slate-200">{trip.paymentMethod}</strong></span>
              <span className="px-2 py-0.5 text-[10px] rounded bg-slate-800 text-slate-300 font-mono">
                {trip.paymentStatus}
              </span>
            </div>
            <div className="text-right">
              <p className="text-xs text-slate-400">Tổng cước chuyến đi</p>
              <p className="text-lg font-bold text-emerald-400">
                {formatCurrency(trip.finalAmountVnd)}
              </p>
            </div>

          </div>

          {/* Cancellation Notice (if cancelled) */}
          {trip.status === "CANCELLED" && (
            <div className="p-3 bg-rose-950/20 border border-rose-500/30 rounded-xl text-xs text-rose-300 flex items-start gap-2">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div>
                <strong className="font-semibold">Cuốc xe đã bị hủy bởi: {trip.cancelledBy || "Hệ thống"}</strong>
                <p className="text-rose-400/80 mt-0.5">Lý do: {trip.cancelReason || "Không xác định"}</p>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="mt-6 pt-4 border-t border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 text-sm font-semibold rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 transition"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
