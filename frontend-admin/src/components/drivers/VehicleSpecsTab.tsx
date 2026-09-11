"use client";

import React from "react";
import { Car, BatteryCharging, Calendar, CheckCircle2, Clock } from "lucide-react";
import { Vehicle } from "@/types";
import { VEHICLE_TYPE_LABELS } from "@/constants";

interface VehicleSpecsTabProps {
  vehicle?: Vehicle;
}

export default function VehicleSpecsTab({ vehicle }: VehicleSpecsTabProps) {
  if (!vehicle) {
    return (
      <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-12 text-center text-slate-500">
        <Car className="w-10 h-10 mx-auto opacity-30 mb-2" />
        <p className="text-sm">Chưa có thông tin phương tiện xe điện</p>
      </div>
    );
  }

  const isCar = vehicle.vehicleType !== "ELECTRIC_MOTORBIKE";
  const emissionSavings = isCar ? "54.5" : "45.4";
  const vehicleTypeLabel =
    VEHICLE_TYPE_LABELS[vehicle.vehicleType] || vehicle.vehicleType;

  return (
    <div className="rounded-2xl bg-slate-900/60 border border-slate-800 p-6 space-y-6 animate-in fade-in">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-white text-base flex items-center gap-2">
          <Car className="w-5 h-5 text-emerald-400" />
          <span>Thông Số Kỹ Thuật Phương Tiện Xe Điện</span>
        </h3>
        {vehicle.isVerified ? (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Xe điện đã xác minh
          </span>
        ) : (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <Clock className="w-3.5 h-3.5" />
            Chờ duyệt kiểm định
          </span>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
          <span className="text-xs text-slate-400">Hãng sản xuất & Dòng xe</span>
          <p className="text-lg font-bold text-white">
            {vehicle.make} {vehicle.model}
          </p>
          <p className="text-xs text-emerald-400 font-medium">{vehicleTypeLabel}</p>
        </div>

        <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
          <span className="text-xs text-slate-400">Biển số đăng ký</span>
          <p className="text-lg font-bold font-mono text-white tracking-wider">
            {vehicle.licensePlate}
          </p>
          <p className="text-xs text-slate-400">Màu sắc: {vehicle.color}</p>
        </div>

        <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
          <span className="text-xs text-slate-400">Dung lượng Pin danh định</span>
          <p className="text-lg font-bold text-white flex items-center gap-2">
            <BatteryCharging className="w-5 h-5 text-cyan-400" />
            <span>{vehicle.batteryCapacityKwh} kWh</span>
          </p>
          <p className="text-xs text-slate-400">
            Tầm vận hành: {vehicle.rangePerChargeKm} km / lần sạc
          </p>
        </div>

        <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
          <span className="text-xs text-slate-400">Hạn kiểm định phương tiện</span>
          <p className="text-lg font-bold text-white flex items-center gap-2">
            <Calendar className="w-4 h-4 text-slate-400" />
            <span>{vehicle.inspectionExpiryDate}</span>
          </p>
          <p className="text-xs text-slate-400">Theo quy định đăng kiểm Việt Nam</p>
        </div>

        <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
          <span className="text-xs text-slate-400">Hệ số giảm phát thải CO2 trung bình</span>
          <p className="text-lg font-bold text-emerald-400">
            {emissionSavings} gCO2/km
          </p>
          <p className="text-xs text-slate-400">So với xe chạy xăng tương đương</p>
        </div>
      </div>
    </div>
  );
}
