"use client";

import React, { useEffect, useState } from "react";
import {
  Leaf,
  Sliders,
  Plus,
  RefreshCw,
  Zap,
  Calculator,
  CheckCircle2,
  AlertCircle,
  TrendingDown,
  Clock,
  Layers,
  Sparkles,
  TreePine,
  Lightbulb,
  Smartphone,
  ExternalLink,
  Edit2,
  Power,
  Info,
} from "lucide-react";
import Header from "@/components/layout/Header";
import KpiCard from "@/components/ui/KpiCard";
import apiClient from "@/lib/api";
import { ApiResponse, EmissionFactor, CarbonSimulationResult } from "@/types";

export default function EmissionFactorsPage() {
  const [factors, setFactors] = useState<EmissionFactor[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<"MATRIX" | "SIMULATOR">("MATRIX");
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState<string>("ALL");

  // Simulator state
  const [simVehicleCategory, setSimVehicleCategory] = useState("ELECTRIC_MOTORBIKE");
  const [simDistanceKm, setSimDistanceKm] = useState<number>(10);
  const [simResult, setSimResult] = useState<CarbonSimulationResult | null>(null);
  const [isSimulating, setIsSimulating] = useState(false);

  // Edit / Add modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingFactor, setEditingFactor] = useState<EmissionFactor | null>(null);
  const [modalFormData, setModalFormData] = useState({
    vehicleCategory: "ELECTRIC_MOTORBIKE",
    baselineGasolineFactorGco2Km: 70.0,
    evEnergyConsumptionKwhKm: 0.025,
    gridEmissionFactorGco2Kwh: 580.0,
    region: "VIETNAM_NATIONAL",
    effectiveFrom: new Date().toISOString().split("T")[0],
    isActive: true,
  });
  const [isSaving, setIsSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const fetchFactors = async () => {
    setIsLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<EmissionFactor[]>>("/admin/emissions");
      if (res.data.success && res.data.data) {
        setFactors(res.data.data);
      }
    } catch (err) {
      console.warn("Lỗi tải danh sách hệ số phát thải, nạp fallback mặc định:", err);
      // Fallback display if network issue
      if (factors.length === 0) {
        setFactors([
          {
            id: "b3a1aa69-7152-4824-9d69-045b2f06c3c4",
            vehicleCategory: "ELECTRIC_MOTORBIKE",
            baselineGasolineFactorGco2Km: 70.0,
            evEnergyConsumptionKwhKm: 0.025,
            gridEmissionFactorGco2Kwh: 580.0,
            calculatedEvFactorGco2Km: 14.5,
            netCo2SavingPerKm: 55.5,
            region: "VIETNAM_NATIONAL",
            effectiveFrom: "2026-01-01",
            isActive: true,
            createdAt: new Date().toISOString(),
          },
          {
            id: "1b6b1b8b-29d6-443a-932e-d485850b7b03",
            vehicleCategory: "ELECTRIC_CAR_4SEAT",
            baselineGasolineFactorGco2Km: 140.0,
            evEnergyConsumptionKwhKm: 0.14,
            gridEmissionFactorGco2Kwh: 580.0,
            calculatedEvFactorGco2Km: 81.2,
            netCo2SavingPerKm: 58.8,
            region: "VIETNAM_NATIONAL",
            effectiveFrom: "2026-01-01",
            isActive: true,
            createdAt: new Date().toISOString(),
          },
          {
            id: "76c549a0-358d-46fa-8373-b16f3574ae36",
            vehicleCategory: "ELECTRIC_CAR_7SEAT",
            baselineGasolineFactorGco2Km: 180.0,
            evEnergyConsumptionKwhKm: 0.18,
            gridEmissionFactorGco2Kwh: 580.0,
            calculatedEvFactorGco2Km: 104.4,
            netCo2SavingPerKm: 75.6,
            region: "VIETNAM_NATIONAL",
            effectiveFrom: "2026-01-01",
            isActive: true,
            createdAt: new Date().toISOString(),
          },
        ]);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const runSimulation = async (category: string, km: number) => {
    setIsSimulating(true);
    try {
      const res = await apiClient.post<ApiResponse<CarbonSimulationResult>>("/admin/emissions/simulate", {
        vehicleCategory: category,
        distanceKm: km,
      });
      if (res.data.success && res.data.data) {
        setSimResult(res.data.data);
      }
    } catch (err) {
      console.warn("Lỗi chạy sandbox simulate, tự tính toán fallback:", err);
      // Local client fallback calculation
      const baseline = category === "ELECTRIC_MOTORBIKE" ? 70.0 : category === "ELECTRIC_CAR_4SEAT" ? 140.0 : 180.0;
      const sec = category === "ELECTRIC_MOTORBIKE" ? 0.025 : category === "ELECTRIC_CAR_4SEAT" ? 0.14 : 0.18;
      const grid = 580.0;
      const evFactor = sec * grid;
      const netPerKm = baseline - evFactor;
      const netSaved = netPerKm * km;

      setSimResult({
        vehicleCategory: category,
        distanceKm: km,
        baselineGasolineFactorGco2Km: baseline,
        evEnergyConsumptionKwhKm: sec,
        gridEmissionFactorGco2Kwh: grid,
        calculatedEvFactorGco2Km: evFactor,
        netCo2SavingPerKm: netPerKm,
        baselineGasolineCo2Grams: baseline * km,
        evEmittedCo2Grams: evFactor * km,
        netCo2SavedGrams: netSaved,
        netCo2SavedKg: netSaved / 1000,
        treeAbsorptionDaysEquiv: netSaved / 60.0,
        ledBulbHoursEquiv: netSaved / 7.221,
        smartphoneChargesEquiv: netSaved / 8.22,
        carbonCreditsEarned: netSaved / 1000000.0,
        ecoPoints: Math.floor(netSaved / 100.0),
      });
    } finally {
      setIsSimulating(false);
    }
  };

  useEffect(() => {
    fetchFactors();
    runSimulation("ELECTRIC_MOTORBIKE", 10);
  }, []);

  const handleOpenAddModal = () => {
    setEditingFactor(null);
    setModalFormData({
      vehicleCategory: "ELECTRIC_MOTORBIKE",
      baselineGasolineFactorGco2Km: 70.0,
      evEnergyConsumptionKwhKm: 0.025,
      gridEmissionFactorGco2Kwh: 580.0,
      region: "VIETNAM_NATIONAL",
      effectiveFrom: new Date().toISOString().split("T")[0],
      isActive: true,
    });
    setSaveMessage(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (factor: EmissionFactor) => {
    setEditingFactor(factor);
    setModalFormData({
      vehicleCategory: factor.vehicleCategory,
      baselineGasolineFactorGco2Km: factor.baselineGasolineFactorGco2Km,
      evEnergyConsumptionKwhKm: factor.evEnergyConsumptionKwhKm,
      gridEmissionFactorGco2Kwh: factor.gridEmissionFactorGco2Kwh,
      region: factor.region || "VIETNAM_NATIONAL",
      effectiveFrom: factor.effectiveFrom,
      isActive: factor.isActive,
    });
    setSaveMessage(null);
    setIsModalOpen(true);
  };

  const handleSaveFactor = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setSaveMessage(null);

    try {
      if (editingFactor) {
        await apiClient.put(`/admin/emissions/${editingFactor.id}`, modalFormData);
        setSaveMessage({ type: "success", text: "Cập nhật hệ số phát thải thành công!" });
      } else {
        await apiClient.post("/admin/emissions", modalFormData);
        setSaveMessage({ type: "success", text: "Tạo mới hệ số phát thải thành công!" });
      }
      setTimeout(() => {
        setIsModalOpen(false);
        fetchFactors();
      }, 800);
    } catch (err: any) {
      setSaveMessage({
        type: "error",
        text: err?.response?.data?.message || "Có lỗi xảy ra khi lưu dữ liệu. Vui lòng thử lại!",
      });
    } finally {
      setIsSaving(false);
    }
  };

  const handleToggleStatus = async (factor: EmissionFactor) => {
    try {
      await apiClient.patch(`/admin/emissions/${factor.id}/status?active=${!factor.isActive}`);
      setFactors((prev) =>
        prev.map((f) => (f.id === factor.id ? { ...f, isActive: !f.isActive } : f))
      );
    } catch (err) {
      console.error("Lỗi thay đổi trạng thái hệ số:", err);
    }
  };

  // Preview calculations in modal
  const previewEvFactor = (modalFormData.evEnergyConsumptionKwhKm * modalFormData.gridEmissionFactorGco2Kwh).toFixed(2);
  const previewNetSaving = (modalFormData.baselineGasolineFactorGco2Km - Number(previewEvFactor)).toFixed(2);

  const filteredFactors = factors.filter((f) => {
    if (selectedCategoryFilter === "ALL") return true;
    return f.vehicleCategory.includes(selectedCategoryFilter);
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Header
        title="Ma trận Hệ số Phát thải Carbon (IPCC / GHG Protocol)"
        description="Định cấu hình hệ số phát thải nền xe xăng, suất tiêu hao năng lượng xe điện và hệ số phát thải lưới điện quốc gia phục vụ Sprint 4 Carbon Engine"
      />

      <main className="flex-1 p-6 space-y-6 max-w-7xl mx-auto w-full">
        {/* Top Action Bar */}
        <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-900/60 backdrop-blur-md p-4 rounded-2xl border border-slate-800 shadow-xl">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setActiveTab("MATRIX")}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl font-medium text-sm transition ${
                activeTab === "MATRIX"
                  ? "bg-emerald-500 text-white shadow-lg shadow-emerald-500/20"
                  : "bg-slate-800 text-slate-400 hover:text-white"
              }`}
            >
              <Sliders className="w-4 h-4" />
              Ma trận Hệ số Chuẩn ({factors.length})
            </button>
            <button
              onClick={() => setActiveTab("SIMULATOR")}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl font-medium text-sm transition ${
                activeTab === "SIMULATOR"
                  ? "bg-emerald-500 text-white shadow-lg shadow-emerald-500/20"
                  : "bg-slate-800 text-slate-400 hover:text-white"
              }`}
            >
              <Calculator className="w-4 h-4" />
              Carbon Sandbox Simulator
            </button>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={fetchFactors}
              disabled={isLoading}
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700/80 transition text-sm font-medium border border-slate-700/50"
            >
              <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin text-emerald-400" : ""}`} />
              Làm mới
            </button>
            <button
              onClick={handleOpenAddModal}
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-medium text-sm transition shadow-lg shadow-emerald-600/25"
            >
              <Plus className="w-4 h-4" />
              Thêm Hệ số Mới
            </button>
          </div>
        </div>

        {/* 4 Core Carbon KPIs */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiCard
            title="Chuẩn Đo lường"
            value="IPCC 2006 / MoNRE"
            subtitle="Hệ số lưới điện Việt Nam"
            icon={Leaf}
            iconColor="text-emerald-400"
            iconBgColor="bg-emerald-500/10"
          />
          <KpiCard
            title="Lưới điện Quốc gia (EF_grid)"
            value="580.0 g/kWh"
            subtitle="Hệ số phát thải lưới cập nhật 2026"
            icon={Zap}
            iconColor="text-amber-400"
            iconBgColor="bg-amber-500/10"
          />
          <KpiCard
            title="Cắt giảm Xe máy Điện"
            value="+55.50 g/km"
            subtitle="Tiết kiệm 79.3% so với xe xăng"
            icon={TrendingDown}
            iconColor="text-teal-400"
            iconBgColor="bg-teal-500/10"
          />
          <KpiCard
            title="Cắt giảm Ô tô Điện (4 & 7 chỗ)"
            value="58.8 - 75.6 g/km"
            subtitle="Giảm phát thải trực tiếp nội đô"
            icon={Sparkles}
            iconColor="text-cyan-400"
            iconBgColor="bg-cyan-500/10"
          />
        </div>

        {/* TAB 1: EMISSION MATRIX TABLE */}
        {activeTab === "MATRIX" && (
          <div className="space-y-4">
            {/* Filter pills */}
            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-400 font-medium">Lọc theo loại xe:</span>
              {["ALL", "MOTORBIKE", "4SEAT", "7SEAT"].map((cat) => (
                <button
                  key={cat}
                  onClick={() => setSelectedCategoryFilter(cat)}
                  className={`text-xs px-3 py-1.5 rounded-lg font-medium transition ${
                    selectedCategoryFilter === cat
                      ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/40"
                      : "bg-slate-900 text-slate-400 border border-slate-800 hover:text-white"
                  }`}
                >
                  {cat === "ALL"
                    ? "Tất cả"
                    : cat === "MOTORBIKE"
                    ? "Xe máy (2 bánh)"
                    : cat === "4SEAT"
                    ? "Ô tô 4 chỗ"
                    : "Ô tô 7 chỗ"}
                </button>
              ))}
            </div>

            {/* Table */}
            <div className="bg-slate-900/70 backdrop-blur-md rounded-2xl border border-slate-800 shadow-xl overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-slate-300">
                  <thead className="bg-slate-800/80 text-xs uppercase text-slate-400 font-semibold tracking-wider border-b border-slate-800">
                    <tr>
                      <th className="py-3.5 px-4">Danh mục Phương tiện</th>
                      <th className="py-3.5 px-4 text-right">EF Baseline (Xăng)</th>
                      <th className="py-3.5 px-4 text-right">Suất tiêu thụ (SEC)</th>
                      <th className="py-3.5 px-4 text-right">EF Lưới điện</th>
                      <th className="py-3.5 px-4 text-right">Phát thải EV (gián tiếp)</th>
                      <th className="py-3.5 px-4 text-right">Giảm phát thải Ròng</th>
                      <th className="py-3.5 px-4">Vùng & Hiệu lực</th>
                      <th className="py-3.5 px-4 text-center">Trạng thái</th>
                      <th className="py-3.5 px-4 text-center">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 font-mono text-xs">
                    {isLoading ? (
                      <tr>
                        <td colSpan={9} className="py-12 text-center text-slate-500 font-sans">
                          <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-emerald-500" />
                          Đang tải ma trận hệ số phát thải...
                        </td>
                      </tr>
                    ) : filteredFactors.length === 0 ? (
                      <tr>
                        <td colSpan={9} className="py-8 text-center text-slate-500 font-sans">
                          Không tìm thấy hệ số phát thải phù hợp
                        </td>
                      </tr>
                    ) : (
                      filteredFactors.map((factor) => {
                        const isMotorbike = factor.vehicleCategory.includes("MOTORBIKE");
                        const is7Seat = factor.vehicleCategory.includes("7SEAT");

                        return (
                          <tr key={factor.id} className="hover:bg-slate-800/40 transition">
                            <td className="py-4 px-4 font-sans font-medium text-white flex items-center gap-2.5">
                              <div
                                className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                                  isMotorbike
                                    ? "bg-teal-500/10 text-teal-400 border border-teal-500/30"
                                    : is7Seat
                                    ? "bg-indigo-500/10 text-indigo-400 border border-indigo-500/30"
                                    : "bg-emerald-500/10 text-emerald-400 border border-emerald-500/30"
                                }`}
                              >
                                <Zap className="w-4 h-4" />
                              </div>
                              <div>
                                <span className="font-semibold block text-slate-100">
                                  {isMotorbike
                                    ? "Xe máy điện (E-Bike)"
                                    : is7Seat
                                    ? "Ô tô điện 7 chỗ (E-SUV)"
                                    : "Ô tô điện 4 chỗ (E-Car)"}
                                </span>
                                <span className="text-[11px] text-slate-500 font-mono">
                                  {factor.vehicleCategory}
                                </span>
                              </div>
                            </td>
                            <td className="py-4 px-4 text-right font-medium text-slate-300">
                              {factor.baselineGasolineFactorGco2Km.toFixed(2)}{" "}
                              <span className="text-slate-500 font-sans text-[11px]">g/km</span>
                            </td>
                            <td className="py-4 px-4 text-right text-slate-300">
                              {factor.evEnergyConsumptionKwhKm.toFixed(4)}{" "}
                              <span className="text-slate-500 font-sans text-[11px]">kWh/km</span>
                            </td>
                            <td className="py-4 px-4 text-right text-slate-300">
                              {factor.gridEmissionFactorGco2Kwh.toFixed(2)}{" "}
                              <span className="text-slate-500 font-sans text-[11px]">g/kWh</span>
                            </td>
                            <td className="py-4 px-4 text-right text-amber-400 font-medium">
                              {factor.calculatedEvFactorGco2Km.toFixed(2)}{" "}
                              <span className="text-slate-500 font-sans text-[11px]">g/km</span>
                            </td>
                            <td className="py-4 px-4 text-right">
                              <span className="inline-flex items-center gap-1 font-bold text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-lg border border-emerald-500/20 text-xs">
                                +{factor.netCo2SavingPerKm.toFixed(2)} g/km
                              </span>
                            </td>
                            <td className="py-4 px-4 font-sans text-xs">
                              <span className="text-slate-300 block">{factor.region || "VIETNAM_NATIONAL"}</span>
                              <span className="text-slate-500 text-[11px]">
                                Từ {factor.effectiveFrom}
                              </span>
                            </td>
                            <td className="py-4 px-4 text-center">
                              <button
                                onClick={() => handleToggleStatus(factor)}
                                className={`inline-flex items-center gap-1 text-[11px] font-sans font-semibold px-2.5 py-1 rounded-full transition ${
                                  factor.isActive
                                    ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                                    : "bg-slate-800 text-slate-500 border border-slate-700"
                                }`}
                              >
                                <Power className="w-3 h-3" />
                                {factor.isActive ? "Đang áp dụng" : "Đã tắt"}
                              </button>
                            </td>
                            <td className="py-4 px-4 text-center">
                              <button
                                onClick={() => handleOpenEditModal(factor)}
                                className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition"
                                title="Chỉnh sửa tham số"
                              >
                                <Edit2 className="w-3.5 h-3.5" />
                              </button>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Formula Reference Card */}
            <div className="bg-slate-900/50 rounded-2xl border border-slate-800/80 p-5 text-xs text-slate-400 space-y-2">
              <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
                <Info className="w-4 h-4" />
                Công thức Tính toán Giảm Phát thải Ròng Chuẩn Sprint 4
              </div>
              <p className="font-mono text-slate-300">
                ΔE_CO2 = d_actual × [ EF_baseline - ( SEC × EF_grid ) ]
              </p>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2 text-[11px] text-slate-400 border-t border-slate-800">
                <div>
                  <strong className="text-slate-200">EF_baseline:</strong> Hệ số xe xăng tương đương (g CO2/km).
                </div>
                <div>
                  <strong className="text-slate-200">SEC (Specific Energy Consumption):</strong> Suất tiêu thụ điện riêng (kWh/km).
                </div>
                <div>
                  <strong className="text-slate-200">EF_grid:</strong> Hệ số phát thải lưới điện quốc gia Việt Nam (g CO2/kWh).
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: CARBON SANDBOX SIMULATOR */}
        {activeTab === "SIMULATOR" && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Left Column: Simulator Inputs */}
            <div className="lg:col-span-5 bg-slate-900/80 backdrop-blur-md rounded-2xl border border-slate-800 p-6 space-y-6 shadow-xl">
              <div>
                <h3 className="font-bold text-base text-white flex items-center gap-2">
                  <Calculator className="w-5 h-5 text-emerald-400" />
                  Tham số Mô phỏng Sandbox
                </h3>
                <p className="text-xs text-slate-400 mt-1">
                  Nhập thông số chuyến đi để mô phỏng tức thì kết quả đo lường và lượng tín chỉ sinh ra
                </p>
              </div>

              <div className="space-y-4 text-sm">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Danh mục Phương tiện Xe điện
                  </label>
                  <select
                    value={simVehicleCategory}
                    onChange={(e) => {
                      setSimVehicleCategory(e.target.value);
                      runSimulation(e.target.value, simDistanceKm);
                    }}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-emerald-500 text-sm font-medium"
                  >
                    <option value="ELECTRIC_MOTORBIKE">Xe máy điện (E-Bike - VinFast Feliz/Klara)</option>
                    <option value="ELECTRIC_CAR_4SEAT">Ô tô điện 4 chỗ (E-Car - VinFast VF e34/VF 5)</option>
                    <option value="ELECTRIC_CAR_7SEAT">Ô tô điện 7 chỗ (E-SUV - VinFast VF 8/VF 9)</option>
                  </select>
                </div>

                <div>
                  <div className="flex justify-between items-center mb-1.5">
                    <label className="text-xs font-semibold text-slate-300">
                      Khoảng cách Cuốc xe (d_actual)
                    </label>
                    <span className="font-mono text-emerald-400 font-bold text-sm">
                      {simDistanceKm.toFixed(1)} km
                    </span>
                  </div>
                  <input
                    type="range"
                    min={0.5}
                    max={50}
                    step={0.5}
                    value={simDistanceKm}
                    onChange={(e) => {
                      const km = parseFloat(e.target.value);
                      setSimDistanceKm(km);
                      runSimulation(simVehicleCategory, km);
                    }}
                    className="w-full accent-emerald-500 cursor-pointer"
                  />
                  <div className="flex justify-between text-[11px] text-slate-500 font-mono mt-1">
                    <span>0.5 km (chặng ngắn)</span>
                    <span>25 km (liên quận)</span>
                    <span>50 km (sân bay/ngoại thành)</span>
                  </div>
                </div>

                <div className="pt-2">
                  <button
                    onClick={() => runSimulation(simVehicleCategory, simDistanceKm)}
                    disabled={isSimulating}
                    className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-sm transition shadow-lg shadow-emerald-600/20 flex items-center justify-center gap-2"
                  >
                    {isSimulating ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <Sparkles className="w-4 h-4" />
                    )}
                    Tính toán lại Kết quả
                  </button>
                </div>
              </div>
            </div>

            {/* Right Column: Simulation Results & Certificate Card */}
            <div className="lg:col-span-7 space-y-4">
              {simResult ? (
                <>
                  {/* Hero Impact Badge */}
                  <div className="bg-gradient-to-br from-emerald-950/80 via-slate-900 to-slate-900 border border-emerald-500/40 rounded-2xl p-6 shadow-2xl relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-64 h-64 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

                    <div className="flex items-center justify-between mb-4">
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
                        <Leaf className="w-3.5 h-3.5" />
                        Chuyến đi Không Phát thải Trực tiếp
                      </span>
                      <span className="text-xs text-slate-400 font-mono">
                        {simResult.distanceKm.toFixed(1)} km lộ trình
                      </span>
                    </div>

                    <div className="flex flex-col md:flex-row md:items-baseline justify-between gap-4 mb-6">
                      <div>
                        <div className="text-4xl font-extrabold text-white tracking-tight flex items-baseline gap-2">
                          <span className="text-emerald-400">-{simResult.netCo2SavedGrams.toFixed(1)}</span>
                          <span className="text-lg font-medium text-slate-300 font-sans">gam CO2</span>
                        </div>
                        <p className="text-xs text-slate-400 mt-1">
                          Lượng khí nhà kính đã ngăn ngừa được thải vào bầu khí quyển đô thị
                        </p>
                      </div>

                      <div className="bg-slate-950/60 border border-slate-800 rounded-xl px-4 py-2.5 shrink-0 text-right">
                        <span className="text-[11px] text-slate-400 block">Tín chỉ Tích lũy (PCC)</span>
                        <span className="font-mono text-emerald-400 font-bold text-sm">
                          +{simResult.carbonCreditsEarned.toFixed(6)} PCC
                        </span>
                        <span className="text-[10px] text-teal-400 block mt-0.5">
                          +{simResult.ecoPoints} EcoPoints
                        </span>
                      </div>
                    </div>

                    {/* Comparison Bar: Gasoline vs EV */}
                    <div className="space-y-2 bg-slate-950/40 p-4 rounded-xl border border-slate-800/80">
                      <div className="flex justify-between text-xs">
                        <span className="text-slate-400">So sánh phát thải theo IPCC:</span>
                        <span className="text-emerald-400 font-bold">
                          Cắt giảm {((simResult.netCo2SavedGrams / (simResult.baselineGasolineCo2Grams || 1)) * 100).toFixed(1)}%
                        </span>
                      </div>

                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between text-[11px]">
                          <span className="text-rose-400 flex items-center gap-1">
                            <span className="w-2 h-2 rounded-full bg-rose-500" />
                            Xe xăng tương đương (baseline)
                          </span>
                          <span className="font-mono font-medium text-slate-200">
                            {simResult.baselineGasolineCo2Grams.toFixed(1)}g
                          </span>
                        </div>
                        <div className="w-full bg-slate-800 h-2.5 rounded-full overflow-hidden">
                          <div className="bg-rose-500 h-full rounded-full w-full" />
                        </div>

                        <div className="flex items-center justify-between text-[11px] pt-1">
                          <span className="text-amber-400 flex items-center gap-1">
                            <span className="w-2 h-2 rounded-full bg-amber-400" />
                            Xe điện (phát thải gián tiếp từ lưới)
                          </span>
                          <span className="font-mono font-medium text-slate-200">
                            {simResult.evEmittedCo2Grams.toFixed(1)}g
                          </span>
                        </div>
                        <div className="w-full bg-slate-800 h-2.5 rounded-full overflow-hidden">
                          <div
                            className="bg-amber-400 h-full rounded-full transition-all duration-500"
                            style={{
                              width: `${Math.min(
                                100,
                                (simResult.evEmittedCo2Grams / (simResult.baselineGasolineCo2Grams || 1)) * 100
                              )}%`,
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* 3 Ecological Equivalents */}
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 flex items-center gap-3.5">
                      <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
                        <TreePine className="w-5 h-5" />
                      </div>
                      <div>
                        <span className="text-[11px] text-slate-400 block">Cây xanh hấp thụ</span>
                        <span className="text-base font-bold text-white font-mono">
                          {simResult.treeAbsorptionDaysEquiv.toFixed(2)}
                        </span>
                        <span className="text-[11px] text-slate-500 block">ngày tuổi cây</span>
                      </div>
                    </div>

                    <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 flex items-center gap-3.5">
                      <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400 shrink-0">
                        <Lightbulb className="w-5 h-5" />
                      </div>
                      <div>
                        <span className="text-[11px] text-slate-400 block">Bóng LED 10W</span>
                        <span className="text-base font-bold text-white font-mono">
                          {simResult.ledBulbHoursEquiv.toFixed(1)}
                        </span>
                        <span className="text-[11px] text-slate-500 block">giờ thắp sáng</span>
                      </div>
                    </div>

                    <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 flex items-center gap-3.5">
                      <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400 shrink-0">
                        <Smartphone className="w-5 h-5" />
                      </div>
                      <div>
                        <span className="text-[11px] text-slate-400 block">Sạc smartphone</span>
                        <span className="text-base font-bold text-white font-mono">
                          {simResult.smartphoneChargesEquiv.toFixed(0)}
                        </span>
                        <span className="text-[11px] text-slate-500 block">lần sạc đầy 100%</span>
                      </div>
                    </div>
                  </div>
                </>
              ) : (
                <div className="bg-slate-900/60 rounded-2xl border border-slate-800 p-8 text-center text-slate-500">
                  Nhập tham số để xem kết quả mô phỏng
                </div>
              )}
            </div>
          </div>
        )}
      </main>

      {/* MODAL: ADD / EDIT EMISSION FACTOR */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-bold text-base text-white flex items-center gap-2">
                <Sliders className="w-5 h-5 text-emerald-400" />
                {editingFactor ? "Chỉnh sửa Hệ số Phát thải" : "Thêm Hệ số Phát thải Mới"}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-white text-lg font-bold"
              >
                ✕
              </button>
            </div>

            {saveMessage && (
              <div
                className={`p-3 rounded-xl text-xs flex items-center gap-2 ${
                  saveMessage.type === "success"
                    ? "bg-emerald-500/15 text-emerald-300 border border-emerald-500/30"
                    : "bg-rose-500/15 text-rose-300 border border-rose-500/30"
                }`}
              >
                {saveMessage.type === "success" ? (
                  <CheckCircle2 className="w-4 h-4 shrink-0" />
                ) : (
                  <AlertCircle className="w-4 h-4 shrink-0" />
                )}
                <span>{saveMessage.text}</span>
              </div>
            )}

            <form onSubmit={handleSaveFactor} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Danh mục Xe điện</label>
                <select
                  value={modalFormData.vehicleCategory}
                  onChange={(e) => setModalFormData({ ...modalFormData, vehicleCategory: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white text-xs"
                >
                  <option value="ELECTRIC_MOTORBIKE">ELECTRIC_MOTORBIKE (Xe máy điện)</option>
                  <option value="ELECTRIC_CAR_4SEAT">ELECTRIC_CAR_4SEAT (Ô tô điện 4 chỗ)</option>
                  <option value="ELECTRIC_CAR_7SEAT">ELECTRIC_CAR_7SEAT (Ô tô điện 7 chỗ)</option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 font-semibold mb-1">
                    EF Xe xăng nền (g/km)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={modalFormData.baselineGasolineFactorGco2Km}
                    onChange={(e) =>
                      setModalFormData({
                        ...modalFormData,
                        baselineGasolineFactorGco2Km: parseFloat(e.target.value) || 0,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white font-mono text-xs"
                  />
                </div>
                <div>
                  <label className="block text-slate-300 font-semibold mb-1">
                    Suất tiêu thụ điện (kWh/km)
                  </label>
                  <input
                    type="number"
                    step="0.0001"
                    required
                    value={modalFormData.evEnergyConsumptionKwhKm}
                    onChange={(e) =>
                      setModalFormData({
                        ...modalFormData,
                        evEnergyConsumptionKwhKm: parseFloat(e.target.value) || 0,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white font-mono text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 font-semibold mb-1">
                    EF Lưới điện (g/kWh)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={modalFormData.gridEmissionFactorGco2Kwh}
                    onChange={(e) =>
                      setModalFormData({
                        ...modalFormData,
                        gridEmissionFactorGco2Kwh: parseFloat(e.target.value) || 0,
                      })
                    }
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white font-mono text-xs"
                  />
                </div>
                <div>
                  <label className="block text-slate-300 font-semibold mb-1">Ngày bắt đầu hiệu lực</label>
                  <input
                    type="date"
                    required
                    value={modalFormData.effectiveFrom}
                    onChange={(e) => setModalFormData({ ...modalFormData, effectiveFrom: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-white text-xs"
                  />
                </div>
              </div>

              {/* Dynamic Preview Calculation */}
              <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 flex justify-between items-center text-xs">
                <div>
                  <span className="text-slate-400 block">Tự động tính toán:</span>
                  <span className="font-mono text-amber-400">EF EV = {previewEvFactor} g/km</span>
                </div>
                <div className="text-right">
                  <span className="text-slate-400 block">Giảm phát thải ròng:</span>
                  <span className="font-mono text-emerald-400 font-bold text-sm">
                    +{previewNetSaving} g/km
                  </span>
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 hover:text-white text-xs font-medium"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs shadow-lg shadow-emerald-600/20"
                >
                  {isSaving ? "Đang lưu..." : editingFactor ? "Cập nhật" : "Tạo Mới"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
