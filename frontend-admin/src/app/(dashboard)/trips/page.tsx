"use client";

import React, { useEffect, useState, useMemo } from "react";
import {
  Search,
  RefreshCw,
  TrendingUp,
  Zap,
  CheckCircle2,
  AlertCircle,
  Leaf,
  Clock,
  Radio,
  SlidersHorizontal,
  Map,
  List,
} from "lucide-react";
import Header from "@/components/layout/Header";
import KpiCard from "@/components/ui/KpiCard";
import TripTable from "@/components/trips/TripTable";
import TripDetailModal from "@/components/trips/TripDetailModal";
import LiveDispatchMap from "@/components/maps/LiveDispatchMap";
import apiClient from "@/lib/api";
import { ApiResponse, Trip, TripStatus } from "@/types";
import { isActiveTrip } from "@/constants";
import { formatTime, calculateCarbonEquivalents } from "@/lib/formatters";

type TripFilterType = "ALL" | "SEARCHING" | "ACTIVE" | "COMPLETED" | "CANCELLED";

export default function TripsMonitoringPage() {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isAutoRefresh, setIsAutoRefresh] = useState(true);
  const [viewMode, setViewMode] = useState<"MAP" | "TABLE">("MAP");
  const [selectedTab, setSelectedTab] = useState<TripFilterType>("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  const fetchTrips = async (showLoadingSpinner = true) => {
    if (showLoadingSpinner) setIsLoading(true);
    try {
      const response = await apiClient.get<ApiResponse<Trip[]>>("/admin/trips");
      if (response.data.success && response.data.data) {
        setTrips(response.data.data);
      }
    } catch (err) {
      console.warn("Lỗi gọi API /admin/trips, nạp dữ liệu demo trực quan:", err);
      // Fallback demo data if backend has no active trips yet
      if (trips.length === 0) {
        setTrips(generateMockTrips());
      }
    } finally {
      if (showLoadingSpinner) setIsLoading(false);
      setLastUpdated(new Date());
    }
  };

  useEffect(() => {
    fetchTrips(true);
  }, []);

  // Auto-refresh interval every 5 seconds
  useEffect(() => {
    if (!isAutoRefresh) return;
    const interval = setInterval(() => {
      fetchTrips(false);
    }, 5000);
    return () => clearInterval(interval);
  }, [isAutoRefresh]);

  // Real-time KPI Stats
  const stats = useMemo(() => {
    const searching = trips.filter((t) => t.status === "SEARCHING").length;
    const active = trips.filter((t) => isActiveTrip(t.status)).length;
    const completed = trips.filter((t) => t.status === "COMPLETED").length;
    const totalGrams = trips.reduce((sum, t) => sum + (t.co2SavedGrams || 0), 0);
    const { co2Kg: totalCo2Kg } = calculateCarbonEquivalents(totalGrams);

    return { searching, active, completed, totalCo2Kg, total: trips.length };
  }, [trips]);

  // Filtered trips based on tab and search
  const filteredTrips = useMemo(() => {
    return trips.filter((trip) => {
      // Tab matching
      let matchTab = true;
      if (selectedTab === "SEARCHING") {
        matchTab = trip.status === "SEARCHING";
      } else if (selectedTab === "ACTIVE") {
        matchTab = isActiveTrip(trip.status);
      } else if (selectedTab === "COMPLETED") {
        matchTab = trip.status === "COMPLETED";
      } else if (selectedTab === "CANCELLED") {
        matchTab = trip.status === "CANCELLED";
      }


      // Query matching
      const query = searchQuery.trim().toLowerCase();
      const matchQuery =
        !query ||
        trip.tripCode.toLowerCase().includes(query) ||
        trip.pickupAddress.toLowerCase().includes(query) ||
        trip.dropoffAddress.toLowerCase().includes(query) ||
        (trip.driver && trip.driver.fullName.toLowerCase().includes(query)) ||
        (trip.driver && trip.driver.licensePlate.toLowerCase().includes(query));

      return matchTab && matchQuery;
    });
  }, [trips, selectedTab, searchQuery]);

  return (
    <div className="space-y-8 max-w-7xl mx-auto pb-12">
      {/* Header */}
      <Header
        title="Giám sát Điều phối & Chuyến đi Xanh"
        description="Theo dõi thời gian thực các cuốc xe điện, trạng thái matching tài xế và đo lường lượng CO2 giảm phát thải"
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        <KpiCard
          title="Đang tìm tài xế"
          value={stats.searching}
          subtitle="Matching Engine đang quét"
          icon={Radio}
          iconColor="text-amber-400"
          iconBgColor="bg-amber-500/10"
        />
        <KpiCard
          title="Đang di chuyển"
          value={stats.active}
          subtitle="Đã ghép xe & đang đón/chở"
          icon={Zap}
          iconColor="text-cyan-400"
          iconBgColor="bg-cyan-500/10"
        />
        <KpiCard
          title="Đã hoàn thành"
          value={stats.completed}
          subtitle="Hoàn tất chuyến đi an toàn"
          icon={CheckCircle2}
          iconColor="text-purple-400"
          iconBgColor="bg-purple-500/10"
        />
        <KpiCard
          title="Lũy kế CO2 Đã Giảm"
          value={`${stats.totalCo2Kg} kg`}
          subtitle="Đóng góp vào Net Zero"
          icon={Leaf}
          iconColor="text-emerald-400"
          iconBgColor="bg-emerald-500/10"
          valueColor="text-emerald-400"
        />
      </div>

      {/* Action Bar & Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-900/60 border border-slate-800 p-4 rounded-2xl shadow-lg backdrop-blur-sm">
        {/* Filter Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-2 md:pb-0 text-xs">
          {[
            { key: "ALL", label: "Tất cả", count: stats.total },
            { key: "SEARCHING", label: "Đang tìm xe", count: stats.searching },
            { key: "ACTIVE", label: "Đang chạy", count: stats.active },
            { key: "COMPLETED", label: "Hoàn thành", count: stats.completed },
            { key: "CANCELLED", label: "Đã hủy" },
          ].map((tab) => {
            const isActive = selectedTab === tab.key;
            return (
              <button
                key={tab.key}
                onClick={() => setSelectedTab(tab.key as TripFilterType)}
                className={`px-3.5 py-2 rounded-xl font-semibold transition whitespace-nowrap flex items-center gap-1.5 ${
                  isActive
                    ? "bg-emerald-500 text-slate-950 shadow-md shadow-emerald-500/20"
                    : "text-slate-400 hover:text-slate-200 hover:bg-slate-800"
                }`}
              >
                <span>{tab.label}</span>
                {tab.count !== undefined && (
                  <span
                    className={`px-1.5 py-0.5 rounded-full text-[10px] font-bold ${
                      isActive ? "bg-slate-950/20 text-slate-950" : "bg-slate-800 text-slate-400"
                    }`}
                  >
                    {tab.count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* Search & Live Tools */}
        <div className="flex items-center gap-3">
          <div className="relative flex-1 md:w-64">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Tìm mã cuốc, địa chỉ, tài xế..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-4 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-emerald-500/50"
            />
          </div>

          {/* View Mode Switch */}
          <div className="flex items-center bg-slate-950 p-1 rounded-xl border border-slate-800">
            <button
              onClick={() => setViewMode("MAP")}
              className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-semibold transition ${
                viewMode === "MAP"
                  ? "bg-emerald-500 text-slate-950 shadow"
                  : "text-slate-400 hover:text-white"
              }`}
              title="Xem bản đồ radar trực tiếp"
            >
              <Map className="w-3.5 h-3.5" />
              <span>Bản đồ</span>
            </button>
            <button
              onClick={() => setViewMode("TABLE")}
              className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-semibold transition ${
                viewMode === "TABLE"
                  ? "bg-emerald-500 text-slate-950 shadow"
                  : "text-slate-400 hover:text-white"
              }`}
              title="Xem bảng danh sách chi tiết"
            >
              <List className="w-3.5 h-3.5" />
              <span>Bảng</span>
            </button>
          </div>

          {/* Live Auto-refresh Switch */}
          <button
            onClick={() => setIsAutoRefresh(!isAutoRefresh)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold border transition ${
              isAutoRefresh
                ? "bg-emerald-500/10 border-emerald-500/40 text-emerald-400"
                : "bg-slate-800 border-slate-700 text-slate-400"
            }`}
            title="Bật/Tắt tự động cập nhật thời gian thực mỗi 5s"
          >
            <span
              className={`w-2 h-2 rounded-full ${
                isAutoRefresh ? "bg-emerald-400 animate-pulse" : "bg-slate-500"
              }`}
            />
            <span className="hidden sm:inline">Live 5s</span>
          </button>

          {/* Manual Refresh Button */}
          <button
            onClick={() => fetchTrips(true)}
            disabled={isLoading}
            className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
            title={`Cập nhật lúc: ${formatTime(lastUpdated)}`}
          >

            <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin text-emerald-400" : ""}`} />
          </button>
        </div>
      </div>

      {/* Main Content: Map View or Table View */}
      {viewMode === "MAP" ? (
        <div className="space-y-6">
          <LiveDispatchMap
            trips={filteredTrips}
            onSelectTrip={(trip) => setSelectedTrip(trip)}
          />
          <TripTable
            trips={filteredTrips}
            isLoading={isLoading}
            onViewDetails={(trip) => setSelectedTrip(trip)}
          />
        </div>
      ) : (
        <TripTable
          trips={filteredTrips}
          isLoading={isLoading}
          onViewDetails={(trip) => setSelectedTrip(trip)}
        />
      )}

      {/* Trip Details Modal */}
      <TripDetailModal
        trip={selectedTrip}
        onClose={() => setSelectedTrip(null)}
      />
    </div>
  );
}


// Visual mock generator if database has no live trips yet
function generateMockTrips(): Trip[] {
  return [
    {
      tripId: "7bb192a0-4318-4a92-b68e-5b1287c80521",
      tripCode: "GM-20261005-9981",
      status: "SEARCHING",
      vehicleType: "ELECTRIC_MOTORBIKE",
      pickupAddress: "Nhà hát Thành phố, Quận 1, TP.HCM",
      pickupLat: 10.77653,
      pickupLng: 106.700981,
      dropoffAddress: "Trường ĐH Công nghệ Thông tin, TP. Thủ Đức",
      dropoffLat: 10.87002,
      dropoffLng: 106.803054,
      fareAmountVnd: 76000,
      finalAmountVnd: 76000,
      paymentMethod: "CASH",
      paymentStatus: "PENDING",
      estimatedDistanceKm: 16.2,
      estimatedDurationMinutes: 32,
      co2SavedGrams: 817.29,
      requestedAt: new Date(Date.now() - 45000).toISOString(),
    },
    {
      tripId: "a2dd6ec6-d201-41d2-9e13-9bd1c7fdac31",
      tripCode: "GM-20261005-4421",
      status: "MATCHED",
      vehicleType: "ELECTRIC_CAR_4SEAT",
      pickupAddress: "Tòa nhà Landmark 81, Bình Thạnh",
      pickupLat: 10.795,
      pickupLng: 106.721,
      dropoffAddress: "Sân bay Quốc tế Tân Sơn Nhất, Tân Bình",
      dropoffLat: 10.818,
      dropoffLng: 106.658,
      fareAmountVnd: 140000,
      finalAmountVnd: 140000,
      paymentMethod: "VNPAY",
      paymentStatus: "PENDING",
      estimatedDistanceKm: 12.0,
      estimatedDurationMinutes: 28,
      co2SavedGrams: 723.6,
      requestedAt: new Date(Date.now() - 120000).toISOString(),
      matchedAt: new Date(Date.now() - 95000).toISOString(),
      driver: {
        driverId: "9c12b7a8-1234-5678-9abc-def012345678",
        fullName: "Nguyễn Minh Thiện",
        phoneNumber: "0987654321",
        avatarUrl: "",
        ratingAvg: 4.95,
        vehicleModel: "VinFast VF e34",
        licensePlate: "51K-889.12",
      },
    },
    {
      tripId: "c3f81e10-1122-3344-5566-778899aabbcc",
      tripCode: "GM-20261005-1102",
      status: "COMPLETED",
      vehicleType: "ELECTRIC_MOTORBIKE",
      pickupAddress: "Ký túc xá Khu B ĐHQG, Dĩ An",
      pickupLat: 10.881,
      pickupLng: 106.782,
      dropoffAddress: "Chợ Bến Thành, Quận 1",
      dropoffLat: 10.772,
      dropoffLng: 106.698,
      fareAmountVnd: 98000,
      finalAmountVnd: 98000,
      paymentMethod: "MOMO",
      paymentStatus: "PAID",
      estimatedDistanceKm: 21.0,
      estimatedDurationMinutes: 45,
      co2SavedGrams: 1059.45,
      requestedAt: new Date(Date.now() - 3600000).toISOString(),
      matchedAt: new Date(Date.now() - 3550000).toISOString(),
      driver: {
        driverId: "8b12b7a8-4321-8765-9abc-def012345678",
        fullName: "Trần Hoàng Nam",
        phoneNumber: "0912345678",
        ratingAvg: 4.88,
        vehicleModel: "VinFast Feliz S",
        licensePlate: "59-P2 345.67",
      },
    },
  ];
}
