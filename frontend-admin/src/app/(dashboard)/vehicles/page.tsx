"use client";

import React, { useState, useEffect, useMemo } from "react";
import apiClient from "@/lib/api";
import { AdminVehicle, VehicleType, ApiResponse } from "@/types";
import {
  Car,
  Bike,
  BatteryCharging,
  Zap,
  CheckCircle2,
  AlertCircle,
  Search,
  Filter,
  ShieldCheck,
  RefreshCw,
  Gauge,
  Calendar,
  User,
  Phone,
  Sparkles,
  ChevronRight,
} from "lucide-react";

export default function VehiclesPage() {
  const [vehicles, setVehicles] = useState<AdminVehicle[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [typeFilter, setTypeFilter] = useState<string>("ALL");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [selectedVehicle, setSelectedVehicle] = useState<AdminVehicle | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);

  const fetchVehicles = async () => {
    setIsLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<AdminVehicle[]>>("/admin/vehicles");
      if (res.data.success && res.data.data) {
        setVehicles(res.data.data);
      }
    } catch (err) {
      console.error("Lỗi khi tải danh sách phương tiện:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchVehicles();
  }, []);

  const handleToggleVerify = async (v: AdminVehicle) => {
    try {
      setIsVerifying(true);
      const newStatus = !v.isVerified;
      const res = await apiClient.patch<ApiResponse<AdminVehicle>>(
        `/admin/vehicles/${v.id}/verify?verified=${newStatus}`
      );
      if (res.data.success) {
        setVehicles((prev) =>
          prev.map((item) => (item.id === v.id ? { ...item, isVerified: newStatus } : item))
        );
        if (selectedVehicle?.id === v.id) {
          setSelectedVehicle((prev) => (prev ? { ...prev, isVerified: newStatus } : null));
        }
      }
    } catch (err) {
      console.error("Lỗi cập nhật kiểm định xe:", err);
      alert("Không thể cập nhật trạng thái kiểm định!");
    } finally {
      setIsVerifying(false);
    }
  };

  const filteredVehicles = useMemo(() => {
    return vehicles.filter((v) => {
      const matchSearch =
        v.licensePlate.toLowerCase().includes(searchQuery.toLowerCase()) ||
        v.model.toLowerCase().includes(searchQuery.toLowerCase()) ||
        v.make.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (v.driverName && v.driverName.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchType = typeFilter === "ALL" || v.vehicleType === typeFilter;
      const matchStatus =
        statusFilter === "ALL" ||
        (statusFilter === "VERIFIED" && v.isVerified) ||
        (statusFilter === "UNVERIFIED" && !v.isVerified);

      return matchSearch && matchType && matchStatus;
    });
  }, [vehicles, searchQuery, typeFilter, statusFilter]);

  // Statistics
  const totalVehicles = vehicles.length;
  const motorbikeCount = vehicles.filter((v) => v.vehicleType === "ELECTRIC_MOTORBIKE").length;
  const carCount = vehicles.filter((v) => v.vehicleType !== "ELECTRIC_MOTORBIKE").length;
  const verifiedCount = vehicles.filter((v) => v.isVerified).length;
  const verifiedPercent = totalVehicles > 0 ? Math.round((verifiedCount / totalVehicles) * 100) : 0;

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="px-2.5 py-1 rounded-md text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              Fleet Management
            </span>
            <span className="text-xs text-slate-400">• 100% Thuần Điện</span>
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight mt-2 flex items-center gap-3">
            <Car className="w-8 h-8 text-emerald-400" />
            Quản lý Đội Xe Điện (VinFast / Dat Bike)
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Giám sát kỹ thuật dung lượng pin (kWh), tầm hoạt động và trạng thái kiểm định chuẩn an toàn
          </p>
        </div>

        <button
          onClick={fetchVehicles}
          disabled={isLoading}
          className="flex items-center gap-2 px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-xl border border-slate-700 transition active:scale-95 disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin text-emerald-400" : ""}`} />
          Làm mới
        </button>
      </div>

      {/* KPI Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-md relative overflow-hidden group hover:border-emerald-500/30 transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Tổng Đội Xe</span>
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Car className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-black text-white mt-3">{totalVehicles}</div>
          <div className="text-xs text-emerald-400 mt-1 flex items-center gap-1">
            <Sparkles className="w-3.5 h-3.5" /> 100% Phương tiện không phát thải trực tiếp
          </div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-md relative overflow-hidden group hover:border-blue-500/30 transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Xe Máy Điện</span>
            <div className="w-10 h-10 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Bike className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-black text-white mt-3">{motorbikeCount}</div>
          <div className="text-xs text-slate-400 mt-1">VinFast Feliz S, Dat Bike Weaver++</div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-md relative overflow-hidden group hover:border-cyan-500/30 transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Ô Tô Điện</span>
            <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
              <Zap className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-black text-white mt-3">{carCount}</div>
          <div className="text-xs text-slate-400 mt-1">VinFast VF e34, VF8 Eco</div>
        </div>

        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-md relative overflow-hidden group hover:border-emerald-500/30 transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Đã Kiểm Định</span>
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-black text-white mt-3">{verifiedCount} <span className="text-sm font-normal text-slate-400">/ {totalVehicles}</span></div>
          <div className="text-xs text-emerald-400 mt-1">{verifiedPercent}% đạt tiêu chuẩn hoạt động</div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 flex flex-col md:flex-row gap-4 justify-between items-center">
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm biển số xe, mẫu xe, tài xế..."
            className="w-full pl-10 pr-4 py-2 bg-slate-800/80 border border-slate-700/80 rounded-xl text-sm text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-emerald-500 transition"
          />
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <div className="flex items-center gap-2 bg-slate-800/60 border border-slate-700/60 rounded-xl px-3 py-1.5 text-xs text-slate-300">
            <Filter className="w-3.5 h-3.5 text-slate-400" />
            <span>Phân loại:</span>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="bg-transparent text-emerald-400 font-medium focus:outline-none cursor-pointer"
            >
              <option value="ALL" className="bg-slate-900 text-white">Tất cả xe</option>
              <option value="ELECTRIC_MOTORBIKE" className="bg-slate-900 text-white">Xe máy điện</option>
              <option value="ELECTRIC_CAR_4SEAT" className="bg-slate-900 text-white">Ô tô 4 chỗ</option>
              <option value="ELECTRIC_CAR_7SEAT" className="bg-slate-900 text-white">Ô tô 7 chỗ</option>
            </select>
          </div>

          <div className="flex items-center gap-2 bg-slate-800/60 border border-slate-700/60 rounded-xl px-3 py-1.5 text-xs text-slate-300">
            <span>Kiểm định:</span>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-transparent text-emerald-400 font-medium focus:outline-none cursor-pointer"
            >
              <option value="ALL" className="bg-slate-900 text-white">Tất cả</option>
              <option value="VERIFIED" className="bg-slate-900 text-white">Đã duyệt</option>
              <option value="UNVERIFIED" className="bg-slate-900 text-white">Chờ duyệt</option>
            </select>
          </div>
        </div>
      </div>

      {/* Vehicles Table */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl overflow-hidden shadow-xl backdrop-blur-md">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-800/60 text-xs uppercase font-semibold text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-6 py-4">Phương tiện / Mẫu xe</th>
                <th className="px-6 py-4">Biển số</th>
                <th className="px-6 py-4">Tài xế quản lý</th>
                <th className="px-6 py-4">Thông số Pin & Cự ly</th>
                <th className="px-6 py-4">Hạn Đăng kiểm</th>
                <th className="px-6 py-4 text-center">Trạng thái Kiểm định</th>
                <th className="px-6 py-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-slate-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto text-emerald-400 mb-2" />
                    Đang tải dữ liệu đội xe điện...
                  </td>
                </tr>
              ) : filteredVehicles.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-slate-400">
                    Không tìm thấy xe điện phù hợp bộ lọc.
                  </td>
                </tr>
              ) : (
                filteredVehicles.map((v) => {
                  const isMotorbike = v.vehicleType === "ELECTRIC_MOTORBIKE";
                  return (
                    <tr key={v.id} className="hover:bg-slate-800/40 transition">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div
                            className={`w-10 h-10 rounded-xl flex items-center justify-center border ${
                              isMotorbike
                                ? "bg-blue-500/10 border-blue-500/20 text-blue-400"
                                : "bg-emerald-500/10 border-emerald-500/20 text-emerald-400"
                            }`}
                          >
                            {isMotorbike ? <Bike className="w-5 h-5" /> : <Car className="w-5 h-5" />}
                          </div>
                          <div>
                            <div className="font-semibold text-white flex items-center gap-2">
                              {v.make} {v.model}
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
                                {v.color}
                              </span>
                            </div>
                            <div className="text-xs text-slate-400 mt-0.5">
                              {isMotorbike ? "Xe máy điện" : v.vehicleType === "ELECTRIC_CAR_4SEAT" ? "Ô tô 4 chỗ" : "Ô tô 7 chỗ"}
                            </div>
                          </div>
                        </div>
                      </td>

                      <td className="px-6 py-4">
                        <span className="font-mono px-2.5 py-1 bg-slate-950 border border-slate-700 rounded-lg text-xs font-bold text-amber-300 tracking-wider">
                          {v.licensePlate}
                        </span>
                      </td>

                      <td className="px-6 py-4">
                        {v.driverName ? (
                          <div>
                            <div className="font-medium text-slate-200 flex items-center gap-1.5">
                              <User className="w-3.5 h-3.5 text-slate-400" />
                              {v.driverName}
                            </div>
                            {v.driverPhone && (
                              <div className="text-xs text-slate-400 flex items-center gap-1.5 mt-0.5">
                                <Phone className="w-3 h-3 text-slate-500" />
                                {v.driverPhone}
                              </div>
                            )}
                          </div>
                        ) : (
                          <span className="text-xs text-slate-500 italic">Chưa gán tài xế</span>
                        )}
                      </td>

                      <td className="px-6 py-4">
                        <div className="space-y-1">
                          <div className="flex items-center justify-between text-xs">
                            <span className="text-slate-400 flex items-center gap-1">
                              <BatteryCharging className="w-3.5 h-3.5 text-emerald-400" />
                              {v.batteryCapacityKwh} kWh
                            </span>
                            <span className="text-emerald-400 font-semibold flex items-center gap-1">
                              <Gauge className="w-3.5 h-3.5" />
                              ~{v.rangePerChargeKm} km/sạc
                            </span>
                          </div>
                          {/* Battery Bar */}
                          <div className="w-36 h-1.5 bg-slate-800 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-gradient-to-r from-emerald-500 to-teal-400 rounded-full"
                              style={{ width: `${Math.min(100, (v.batteryCapacityKwh / 45) * 100)}%` }}
                            />
                          </div>
                        </div>
                      </td>

                      <td className="px-6 py-4">
                        <div className="text-xs text-slate-300 flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          {v.inspectionExpiryDate ? new Date(v.inspectionExpiryDate).toLocaleDateString("vi-VN") : "Chưa cập nhật"}
                        </div>
                      </td>

                      <td className="px-6 py-4 text-center">
                        {v.isVerified ? (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                            <CheckCircle2 className="w-3.5 h-3.5" /> Đã kiểm định
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                            <AlertCircle className="w-3.5 h-3.5" /> Chờ kiểm định
                          </span>
                        )}
                      </td>

                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => setSelectedVehicle(v)}
                            className="px-3 py-1.5 text-xs font-medium text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 rounded-lg border border-slate-700 transition"
                          >
                            Chi tiết
                          </button>

                          <button
                            onClick={() => handleToggleVerify(v)}
                            disabled={isVerifying}
                            className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition border ${
                              v.isVerified
                                ? "bg-rose-500/10 text-rose-400 hover:bg-rose-500/20 border-rose-500/20"
                                : "bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 border-emerald-500/20"
                            }`}
                          >
                            {v.isVerified ? "Hủy duyệt" : "Duyệt an toàn"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Vehicle Detail Modal */}
      {selectedVehicle && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 space-y-6 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
                  <Car className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-lg text-white">
                    {selectedVehicle.make} {selectedVehicle.model}
                  </h3>
                  <p className="text-xs text-slate-400">ID: {selectedVehicle.id}</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedVehicle(null)}
                className="text-slate-400 hover:text-white text-lg font-bold"
              >
                ✕
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm">
              <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
                <span className="text-xs text-slate-400">Biển số đăng ký</span>
                <p className="font-mono font-bold text-amber-300 mt-1">{selectedVehicle.licensePlate}</p>
              </div>

              <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
                <span className="text-xs text-slate-400">Màu sắc sơn</span>
                <p className="font-semibold text-slate-200 mt-1">{selectedVehicle.color}</p>
              </div>

              <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
                <span className="text-xs text-slate-400">Dung lượng Pin</span>
                <p className="font-semibold text-emerald-400 mt-1">{selectedVehicle.batteryCapacityKwh} kWh</p>
              </div>

              <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
                <span className="text-xs text-slate-400">Cự ly tối đa / sạc</span>
                <p className="font-semibold text-cyan-400 mt-1">{selectedVehicle.rangePerChargeKm} km</p>
              </div>

              <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800 col-span-2">
                <span className="text-xs text-slate-400">Tài xế quản trị</span>
                <p className="font-semibold text-slate-200 mt-1">
                  {selectedVehicle.driverName || "Chưa phân bổ tài xế"}
                  {selectedVehicle.driverPhone ? ` (${selectedVehicle.driverPhone})` : ""}
                </p>
              </div>

              <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800 col-span-2">
                <span className="text-xs text-slate-400">Hạn đăng kiểm an toàn</span>
                <p className="font-semibold text-slate-200 mt-1">
                  {selectedVehicle.inspectionExpiryDate
                    ? new Date(selectedVehicle.inspectionExpiryDate).toLocaleDateString("vi-VN")
                    : "Chưa cập nhật"}
                </p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
              <button
                onClick={() => setSelectedVehicle(null)}
                className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white bg-slate-800 rounded-xl"
              >
                Đóng
              </button>
              <button
                onClick={() => handleToggleVerify(selectedVehicle)}
                disabled={isVerifying}
                className={`px-4 py-2 text-sm font-semibold rounded-xl transition ${
                  selectedVehicle.isVerified
                    ? "bg-rose-500 hover:bg-rose-600 text-white"
                    : "bg-emerald-500 hover:bg-emerald-600 text-white"
                }`}
              >
                {selectedVehicle.isVerified ? "Hủy phê duyệt xe" : "Xác nhận kiểm định"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
