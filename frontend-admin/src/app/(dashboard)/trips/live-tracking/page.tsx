"use client";

import React, { useEffect, useState, useMemo, useRef } from "react";
import { useSearchParams } from "next/navigation";
import {
  Radio,
  Clock,
  Car,
  Leaf,
  MapPin,
  RefreshCw,
  Search,
  Filter,
  Navigation,
  Battery,
  AlertCircle,
  Eye,
  Maximize2,
  SlidersHorizontal,
  ChevronRight,
  Phone,
  User,
  Zap,
} from "lucide-react";
import * as maplibregl from "maplibre-gl";
import { setWorkerUrl } from "maplibre-gl";
import { LiveTripDto, TodayStatsDto, TripStatus, VehicleType } from "@/types";
import { fetchLiveTrips, fetchTodayStats } from "@/lib/tripService";
import { VEHICLE_CONFIG, TRIP_STATUS_CONFIG } from "@/constants";
import { formatCurrency, formatTime } from "@/lib/formatters";

if (typeof window !== "undefined") {
  setWorkerUrl("/maplibre-gl-worker.js");
}

const GOONG_MAPTILES_KEY = process.env.NEXT_PUBLIC_GOONG_MAPTILES_KEY || "";
const GOONG_DARK_STYLE = `https://tiles.goong.io/assets/goong_map_dark.json?api_key=${GOONG_MAPTILES_KEY}`;

// Bounding box for TP.HCM Urban Area (Tactical Radar Canvas mode)
const MIN_LAT = 10.70;
const MAX_LAT = 10.90;
const MIN_LNG = 106.60;
const MAX_LNG = 106.85;

function projectToCanvas(lat: number, lng: number, width: number, height: number) {
  const x = ((lng - MIN_LNG) / (MAX_LNG - MIN_LNG)) * width;
  const y = height - ((lat - MIN_LAT) / (MAX_LAT - MIN_LAT)) * height;
  return {
    x: Math.max(30, Math.min(width - 30, x)),
    y: Math.max(30, Math.min(height - 30, y)),
  };
}

export default function LiveTrackingPage() {
  const searchParams = useSearchParams();
  const initialTripId = searchParams.get("tripId");

  const [liveTrips, setLiveTrips] = useState<LiveTripDto[]>([]);
  const [stats, setStats] = useState<TodayStatsDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [selectedTrip, setSelectedTrip] = useState<LiveTripDto | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  // Filters
  const [statusFilter, setStatusFilter] = useState<string[]>(["DRIVER_ARRIVING", "ARRIVED", "IN_TRIP"]);
  const [vehicleFilter, setVehicleFilter] = useState<string>("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [mobileTab, setMobileTab] = useState<"MAP" | "LIST">("MAP");

  // Map state
  const [mapMode, setMapMode] = useState<"GOONG" | "RADAR">("GOONG");
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<maplibregl.Map | null>(null);
  const markersRef = useRef<{ id: string; marker: maplibregl.Marker }[]>([]);

  // Load live trips & stats
  const loadData = async (showSpinner = false) => {
    if (showSpinner) setIsLoading(true);
    setIsRefreshing(true);
    try {
      const [tripsData, statsData] = await Promise.all([
        fetchLiveTrips(),
        fetchTodayStats(),
      ]);

      setLiveTrips(tripsData);
      if (statsData) setStats(statsData);

      // Auto-select trip if query param matches
      if (initialTripId && !selectedTrip) {
        const found = tripsData.find((t) => t.tripId === initialTripId);
        if (found) setSelectedTrip(found);
      }
    } catch (err) {
      console.warn("[LiveTracking] Error fetching live data:", err);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
      setLastUpdated(new Date());
    }
  };

  // Initial load
  useEffect(() => {
    loadData(true);
  }, [initialTripId]);

  // Polling every 10 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      loadData(false);
    }, 10000);
    return () => clearInterval(interval);
  }, [selectedTrip]);

  // Filtered trips
  const filteredTrips = useMemo(() => {
    return liveTrips.filter((trip) => {
      // Status filter
      if (statusFilter.length > 0 && !statusFilter.includes(trip.status)) {
        return false;
      }
      // Vehicle filter
      if (vehicleFilter !== "ALL" && trip.vehicleType !== vehicleFilter) {
        return false;
      }
      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const codeMatch = trip.tripCode?.toLowerCase().includes(q);
        const driverMatch = trip.driver?.fullName?.toLowerCase().includes(q);
        const customerMatch = trip.customerName?.toLowerCase().includes(q);
        const plateMatch = trip.driver?.licensePlate?.toLowerCase().includes(q);
        return codeMatch || driverMatch || customerMatch || plateMatch;
      }
      return true;
    });
  }, [liveTrips, statusFilter, vehicleFilter, searchQuery]);

  // Initialize MapLibre GL map
  useEffect(() => {
    if (mapMode !== "GOONG" || !mapContainerRef.current) return;

    if (!GOONG_MAPTILES_KEY) {
      setMapMode("RADAR");
      return;
    }

    if (mapInstanceRef.current) {
      try {
        mapInstanceRef.current.remove();
      } catch (_) {}
      mapInstanceRef.current = null;
    }

    try {
      const map = new maplibregl.Map({
        container: mapContainerRef.current,
        style: GOONG_DARK_STYLE,
        center: [106.700981, 10.77653],
        zoom: 12.5,
        attributionControl: false,
      });

      map.addControl(new maplibregl.NavigationControl({ showCompass: true }), "top-right");
      mapInstanceRef.current = map;

      map.on("error", (e) => {
        console.warn("[MapLibre Live] Warning:", e.error?.message || e);
      });
    } catch (err) {
      console.warn("[MapLibre Live] Failed to init, fallback to Radar:", err);
      setMapMode("RADAR");
    }

    return () => {
      markersRef.current.forEach((m) => m.marker.remove());
      markersRef.current = [];
      if (mapInstanceRef.current) {
        try {
          mapInstanceRef.current.remove();
        } catch (_) {}
        mapInstanceRef.current = null;
      }
    };
  }, [mapMode]);

  // Update Map Markers
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || mapMode !== "GOONG") return;

    const renderMarkers = () => {
      // Clear old markers
      markersRef.current.forEach((m) => m.marker.remove());
      markersRef.current = [];

      filteredTrips.forEach((trip) => {
        const lat = trip.driverLat || trip.pickupLat;
        const lng = trip.driverLng || trip.pickupLng;
        if (!lat || !lng) return;

        // Color coding by status:
        // 🟡 Vàng (#EAB308): DRIVER_ARRIVING
        // 🟠 Cam (#F97316): ARRIVED
        // 🟢 Xanh (#10B981): IN_TRIP
        let colorClass = "bg-emerald-500/30 border-emerald-400 text-emerald-400";
        let pingColor = "bg-emerald-500/20";
        let icon = "🚗";
        if (trip.status === "DRIVER_ARRIVING") {
          colorClass = "bg-amber-500/30 border-amber-400 text-amber-400";
          pingColor = "bg-amber-500/20";
        } else if (trip.status === "ARRIVED") {
          colorClass = "bg-orange-500/30 border-orange-400 text-orange-400";
          pingColor = "bg-orange-500/20";
          icon = "📍";
        }

        const isSelected = selectedTrip?.tripId === trip.tripId;

        const el = document.createElement("div");
        el.className = `cursor-pointer relative transition-transform ${isSelected ? "scale-125 z-30" : "hover:scale-110 z-10"}`;
        el.innerHTML = `
          <div class="w-8 h-8 rounded-full ${pingColor} animate-ping absolute -top-1 -left-1"></div>
          <div class="w-7 h-7 rounded-full border-2 flex items-center justify-center shadow-lg backdrop-blur-sm ${colorClass}">
            <span class="text-xs">${icon}</span>
          </div>
        `;

        el.addEventListener("click", () => {
          setSelectedTrip(trip);
          map.flyTo({ center: [lng, lat], zoom: 14.5, speed: 1.2 });
        });

        const marker = new maplibregl.Marker({ element: el })
          .setLngLat([lng, lat])
          .addTo(map);

        markersRef.current.push({ id: trip.tripId, marker });
      });
    };

    if (map.isStyleLoaded()) {
      renderMarkers();
    } else {
      map.once("load", renderMarkers);
    }
  }, [filteredTrips, mapMode, selectedTrip]);

  // Center map on selected trip
  const handleSelectTrip = (trip: LiveTripDto) => {
    setSelectedTrip(trip);
    const lat = trip.driverLat || trip.pickupLat;
    const lng = trip.driverLng || trip.pickupLng;

    if (mapInstanceRef.current && lat && lng) {
      mapInstanceRef.current.flyTo({
        center: [lng, lat],
        zoom: 14.5,
        speed: 1.4,
      });
    }

    if (window.innerWidth < 1024) {
      setMobileTab("MAP");
    }
  };

  // Helper status color
  const getStatusColor = (status: TripStatus) => {
    switch (status) {
      case "DRIVER_ARRIVING":
        return { text: "text-amber-400", bg: "bg-amber-500/10", border: "border-amber-500/30", label: "Đang đón khách" };
      case "ARRIVED":
        return { text: "text-orange-400", bg: "bg-orange-500/10", border: "border-orange-500/30", label: "Đã đến điểm đón" };
      case "IN_TRIP":
        return { text: "text-emerald-400", bg: "bg-emerald-500/10", border: "border-emerald-500/30", label: "Đang di chuyển" };
      default:
        return { text: "text-slate-300", bg: "bg-slate-800", border: "border-slate-700", label: status };
    }
  };

  return (
    <div className="space-y-6">
      {/* 1. Header & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400">
              <Radio className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white tracking-tight">
                Live Tracking Dashboard
              </h1>
              <p className="text-xs text-slate-400">
                Giám sát thời gian thực vị trí tài xế, tiến trình cuốc xe và chỉ số vận hành
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-900/80 border border-slate-800 rounded-xl p-1 text-xs font-semibold">
            <button
              onClick={() => setMapMode("GOONG")}
              className={`px-3 py-1.5 rounded-lg transition ${
                mapMode === "GOONG"
                  ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              Goong Vector
            </button>
            <button
              onClick={() => setMapMode("RADAR")}
              className={`px-3 py-1.5 rounded-lg transition ${
                mapMode === "RADAR"
                  ? "bg-cyan-500/20 text-cyan-400 border border-cyan-500/30"
                  : "text-slate-400 hover:text-white"
              }`}
            >
              Tactical Radar
            </button>
          </div>

          <button
            onClick={() => loadData(false)}
            disabled={isRefreshing}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs font-medium text-slate-300 hover:text-white hover:bg-slate-800 transition shadow-sm"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? "animate-spin text-emerald-400" : ""}`} />
            <span>Cập nhật</span>
          </button>
        </div>
      </div>

      {/* 2. Operations KPI Cards (Glassmorphism Style) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Active Trips Card */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-slate-900/90 to-slate-900/50 border border-slate-800/80 p-5 shadow-lg backdrop-blur-md">
          <div className="absolute top-0 right-0 w-28 h-28 bg-emerald-500/10 rounded-full blur-2xl pointer-events-none" />
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Cuốc Xe Đang Chạy
            </p>
            <div className="p-2 rounded-xl bg-emerald-500/15 border border-emerald-500/30 text-emerald-400">
              <Zap className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-white tracking-tight">
              {stats?.activeTripsCount ?? liveTrips.length}
            </span>
            <span className="text-xs text-emerald-400 font-semibold flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              Thời gian thực
            </span>
          </div>
          <p className="mt-1 text-[11px] text-slate-500">
            {liveTrips.filter((t) => t.status === "IN_TRIP").length} cuốc đang chở khách an toàn
          </p>
        </div>

        {/* Avg Pickup Time */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-slate-900/90 to-slate-900/50 border border-slate-800/80 p-5 shadow-lg backdrop-blur-md">
          <div className="absolute top-0 right-0 w-28 h-28 bg-amber-500/10 rounded-full blur-2xl pointer-events-none" />
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Thời Gian Đón TB
            </p>
            <div className="p-2 rounded-xl bg-amber-500/15 border border-amber-500/30 text-amber-400">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-white tracking-tight">
              {stats?.avgPickupTimeMinutes ? `${stats.avgPickupTimeMinutes}m` : "3.5m"}
            </span>
            <span className="text-xs text-amber-400 font-medium">Từ ghép tới đón</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-500">
            Hiệu năng điều phối OSRM & Redis GEO
          </p>
        </div>

        {/* Total Km Today */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-slate-900/90 to-slate-900/50 border border-slate-800/80 p-5 shadow-lg backdrop-blur-md">
          <div className="absolute top-0 right-0 w-28 h-28 bg-cyan-500/10 rounded-full blur-2xl pointer-events-none" />
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Tổng Quãng Đường
            </p>
            <div className="p-2 rounded-xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
              <Car className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-white tracking-tight">
              {stats?.totalKmToday ? `${stats.totalKmToday} km` : "248.5 km"}
            </span>
            <span className="text-xs text-cyan-400 font-semibold">Xe điện</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-500">
            Đã hoàn thành {stats?.completedTripsTodayCount ?? 18} chuyến hôm nay
          </p>
        </div>

        {/* CO2 Saved Today */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-slate-900/90 to-slate-900/50 border border-slate-800/80 p-5 shadow-lg backdrop-blur-md">
          <div className="absolute top-0 right-0 w-28 h-28 bg-emerald-500/15 rounded-full blur-2xl pointer-events-none" />
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              CO2 Giảm Thiểu
            </p>
            <div className="p-2 rounded-xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-400">
              <Leaf className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-3xl font-extrabold text-emerald-400 tracking-tight">
              {stats?.co2SavedTodayKg ? `${stats.co2SavedTodayKg} kg` : "42.8 kg"}
            </span>
            <span className="text-xs text-slate-400 font-medium">Hôm nay</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-500">
            Tương đương {(stats?.co2SavedTodayKg ? stats.co2SavedTodayKg * 0.05 : 2.1).toFixed(1)} cây xanh trồng mới
          </p>
        </div>
      </div>

      {/* 3. Mobile View Switcher */}
      <div className="lg:hidden flex bg-slate-900 border border-slate-800 rounded-xl p-1 text-sm font-semibold">
        <button
          onClick={() => setMobileTab("MAP")}
          className={`flex-1 py-2 rounded-lg text-center transition ${
            mobileTab === "MAP" ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30" : "text-slate-400"
          }`}
        >
          Bản đồ trực tiếp ({filteredTrips.length})
        </button>
        <button
          onClick={() => setMobileTab("LIST")}
          className={`flex-1 py-2 rounded-lg text-center transition ${
            mobileTab === "LIST" ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30" : "text-slate-400"
          }`}
        >
          Danh sách ({filteredTrips.length})
        </button>
      </div>

      {/* 4. Main Split Screen: Map (70%) + Sidebar (30%) */}
      <div className="grid grid-cols-1 lg:grid-cols-10 gap-6">
        {/* Left Column: Live Map (70% = 7 cols) */}
        <div className={`lg:col-span-7 space-y-4 ${mobileTab === "MAP" ? "block" : "hidden lg:block"}`}>
          <div className="relative w-full h-[620px] rounded-2xl overflow-hidden border border-slate-800 bg-slate-950 shadow-2xl">
            {mapMode === "GOONG" ? (
              <div ref={mapContainerRef} className="w-full h-full" />
            ) : (
              /* Tactical SVG Radar Mode */
              <div className="relative w-full h-full bg-[#0B132B] p-4 flex flex-col justify-between">
                {/* SVG Tactical Grid Canvas */}
                <svg className="absolute inset-0 w-full h-full pointer-events-none opacity-40">
                  <defs>
                    <pattern id="radar-grid" width="40" height="40" patternUnits="userSpaceOnUse">
                      <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#1E293B" strokeWidth="1" />
                    </pattern>
                  </defs>
                  <rect width="100%" height="100%" fill="url(#radar-grid)" />
                  <circle cx="50%" cy="50%" r="180" fill="none" stroke="#10B981" strokeWidth="1" strokeDasharray="4 4" opacity="0.25" />
                  <circle cx="50%" cy="50%" r="280" fill="none" stroke="#10B981" strokeWidth="1" strokeDasharray="4 4" opacity="0.15" />
                </svg>

                {/* Tactical SVG Trip Markers */}
                <div className="relative w-full h-full">
                  {filteredTrips.map((trip) => {
                    const lat = trip.driverLat || trip.pickupLat;
                    const lng = trip.driverLng || trip.pickupLng;
                    if (!lat || !lng) return null;

                    const pos = projectToCanvas(lat, lng, 700, 560);
                    const isSelected = selectedTrip?.tripId === trip.tripId;
                    const color = trip.status === "DRIVER_ARRIVING" ? "#EAB308" : trip.status === "ARRIVED" ? "#F97316" : "#10B981";

                    return (
                      <div
                        key={trip.tripId}
                        onClick={() => handleSelectTrip(trip)}
                        style={{ left: `${pos.x}px`, top: `${pos.y}px` }}
                        className="absolute -translate-x-1/2 -translate-y-1/2 cursor-pointer group z-20"
                      >
                        <div
                          className="w-8 h-8 rounded-full animate-ping absolute -top-1 -left-1 opacity-40"
                          style={{ backgroundColor: color }}
                        />
                        <div
                          className={`w-7 h-7 rounded-full border-2 flex items-center justify-center text-xs shadow-lg transition-transform ${
                            isSelected ? "scale-125 ring-2 ring-white" : "group-hover:scale-110"
                          }`}
                          style={{ backgroundColor: `${color}33`, borderColor: color }}
                        >
                          🚗
                        </div>
                        <div className="hidden group-hover:block absolute bottom-8 left-1/2 -translate-x-1/2 bg-slate-900/95 border border-slate-700 px-2 py-1 rounded-md text-[10px] text-white whitespace-nowrap shadow-xl">
                          {trip.tripCode} • {trip.driver?.fullName || "Tài xế"}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Top Map Overlay Badges */}
            <div className="absolute top-4 left-4 right-4 flex items-center justify-between pointer-events-none">
              <div className="pointer-events-auto flex items-center gap-2 bg-slate-900/85 backdrop-blur-md px-3.5 py-1.5 rounded-xl border border-slate-800 text-xs">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <span className="text-white font-semibold">TP. Hồ Chí Minh</span>
                <span className="text-slate-400 font-mono">({filteredTrips.length} xe trực tuyến)</span>
              </div>

              {/* Color legend */}
              <div className="hidden sm:flex items-center gap-3 bg-slate-900/85 backdrop-blur-md px-3 py-1.5 rounded-xl border border-slate-800 text-xs">
                <div className="flex items-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-full bg-amber-400" />
                  <span className="text-slate-300">Đang đón</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-full bg-orange-400" />
                  <span className="text-slate-300">Đã đến</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-400" />
                  <span className="text-slate-300">Đang chở</span>
                </div>
              </div>
            </div>

            {/* Selected Trip Floating Popup Card */}
            {selectedTrip && (
              <div className="absolute bottom-4 left-4 right-4 sm:right-auto sm:w-96 bg-slate-900/95 border border-slate-700/80 rounded-2xl p-4 shadow-2xl backdrop-blur-md z-30">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-emerald-400 text-sm">
                        {selectedTrip.tripCode}
                      </span>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-md border ${getStatusColor(selectedTrip.status).bg} ${getStatusColor(selectedTrip.status).text} ${getStatusColor(selectedTrip.status).border}`}>
                        {getStatusColor(selectedTrip.status).label}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-1 flex items-center gap-1">
                      <span>{VEHICLE_CONFIG[selectedTrip.vehicleType]?.icon}</span>
                      <span>{selectedTrip.driver?.vehicleModel || "Xe điện VinFast"}</span>
                      <span className="font-mono text-slate-300 font-bold ml-1">
                        {selectedTrip.driver?.licensePlate || "51K-••••"}
                      </span>
                    </p>
                  </div>
                  <button
                    onClick={() => setSelectedTrip(null)}
                    className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
                  >
                    ✕
                  </button>
                </div>

                <div className="mt-3 pt-3 border-t border-slate-800 space-y-1.5 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">Tài xế:</span>
                    <span className="text-white font-semibold">{selectedTrip.driver?.fullName || "Nguyễn Văn Hùng"}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">Hành khách:</span>
                    <span className="text-slate-200">{selectedTrip.customerName || "Khách hàng Green Mobility"}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">Thời gian ETA:</span>
                    <span className="text-emerald-400 font-bold font-mono">
                      {selectedTrip.etaSeconds ? `${Math.max(1, Math.round(selectedTrip.etaSeconds / 60))} phút` : "3 phút"}
                      {selectedTrip.distanceRemainingM ? ` (${(selectedTrip.distanceRemainingM / 1000).toFixed(1)} km)` : ""}
                    </span>
                  </div>
                  {selectedTrip.batteryPercent != null && (
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">Pin xe điện:</span>
                      <span className="text-cyan-400 font-semibold font-mono flex items-center gap-1">
                        <Battery className="w-3.5 h-3.5" />
                        {selectedTrip.batteryPercent}%
                      </span>
                    </div>
                  )}
                </div>

                <div className="mt-3 pt-2 border-t border-slate-800/80 flex items-center gap-2">
                  <p className="text-[11px] text-slate-400 truncate flex-1">
                    <span className="text-emerald-400">Điểm đón: </span>
                    {selectedTrip.pickupAddress}
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Active Trips Table & Filters (30% = 3 cols) */}
        <div className={`lg:col-span-3 space-y-4 ${mobileTab === "LIST" ? "block" : "hidden lg:block"}`}>
          <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-4 backdrop-blur-md shadow-xl flex flex-col h-[620px]">
            {/* Sidebar Title & Count */}
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Navigation className="w-4 h-4 text-emerald-400" />
                <h3 className="font-bold text-sm text-white">Chuyến Xe Trực Tuyến</h3>
              </div>
              <span className="text-xs font-mono font-bold px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                {filteredTrips.length} active
              </span>
            </div>

            {/* Search Input */}
            <div className="relative mb-3">
              <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                placeholder="Tìm mã cuốc, tài xế, biển số..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-8 pr-3 py-1.5 text-xs bg-slate-950 border border-slate-800 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
              />
            </div>

            {/* Filter Pills */}
            <div className="flex flex-wrap gap-1.5 mb-3 text-[11px]">
              {[
                { id: "ALL", label: "Tất cả" },
                { id: "DRIVER_ARRIVING", label: "Đang đón" },
                { id: "ARRIVED", label: "Đã đến" },
                { id: "IN_TRIP", label: "Đang chở" },
              ].map((pill) => {
                const isActive = pill.id === "ALL"
                  ? statusFilter.length === 3
                  : statusFilter.length === 1 && statusFilter[0] === pill.id;
                return (
                  <button
                    key={pill.id}
                    onClick={() => {
                      if (pill.id === "ALL") {
                        setStatusFilter(["DRIVER_ARRIVING", "ARRIVED", "IN_TRIP"]);
                      } else {
                        setStatusFilter([pill.id]);
                      }
                    }}
                    className={`px-2.5 py-1 rounded-lg font-medium transition ${
                      isActive
                        ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                        : "bg-slate-800/60 text-slate-400 hover:text-white"
                    }`}
                  >
                    {pill.label}
                  </button>
                );
              })}
            </div>

            {/* Active Trips Scrollable List */}
            <div className="flex-1 overflow-y-auto space-y-2 pr-1 custom-scrollbar">
              {isLoading ? (
                <div className="py-16 text-center">
                  <div className="w-8 h-8 border-3 border-emerald-500/20 border-t-emerald-500 rounded-full animate-spin mx-auto mb-2" />
                  <p className="text-xs text-slate-500">Đang quét cuốc xe trực tiếp...</p>
                </div>
              ) : filteredTrips.length === 0 ? (
                <div className="py-16 text-center text-slate-500 text-xs">
                  <AlertCircle className="w-6 h-6 mx-auto mb-2 text-slate-600" />
                  Không có cuốc xe nào phù hợp bộ lọc
                </div>
              ) : (
                filteredTrips.map((trip) => {
                  const isSelected = selectedTrip?.tripId === trip.tripId;
                  const statusStyle = getStatusColor(trip.status);

                  return (
                    <div
                      key={trip.tripId}
                      onClick={() => handleSelectTrip(trip)}
                      className={`p-3 rounded-xl border transition cursor-pointer group ${
                        isSelected
                          ? "bg-slate-800/90 border-emerald-500/50 shadow-md shadow-emerald-950/50"
                          : "bg-slate-950/60 border-slate-800/70 hover:bg-slate-800/40 hover:border-slate-700"
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-mono font-bold text-xs text-white group-hover:text-emerald-400 transition">
                          {trip.tripCode}
                        </span>
                        <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded border ${statusStyle.bg} ${statusStyle.text} ${statusStyle.border}`}>
                          {statusStyle.label}
                        </span>
                      </div>

                      <div className="mt-2 flex items-center justify-between text-xs">
                        <div className="truncate pr-2">
                          <p className="font-semibold text-slate-200 truncate">
                            {trip.driver?.fullName || "Nguyễn Văn Hùng"}
                          </p>
                          <p className="text-[11px] text-slate-400 font-mono">
                            {trip.driver?.licensePlate || "51K-••••"}
                          </p>
                        </div>
                        <div className="text-right shrink-0">
                          <p className="text-xs font-mono font-bold text-emerald-400">
                            {trip.etaSeconds ? `${Math.max(1, Math.round(trip.etaSeconds / 60))}m` : "3m"}
                          </p>
                          <p className="text-[10px] text-slate-500">
                            {trip.distanceRemainingM ? `${(trip.distanceRemainingM / 1000).toFixed(1)}km` : `${trip.estimatedDistanceKm || 0}km`}
                          </p>
                        </div>
                      </div>

                      <div className="mt-2 pt-2 border-t border-slate-800/60 flex items-center justify-between text-[11px] text-slate-400">
                        <span className="truncate max-w-[170px]">
                          📍 {trip.pickupAddress}
                        </span>
                        <ChevronRight className={`w-3.5 h-3.5 transition-transform ${isSelected ? "text-emerald-400 translate-x-0.5" : "text-slate-600"}`} />
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
