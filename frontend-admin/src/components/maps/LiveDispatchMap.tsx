"use client";

import React, { useEffect, useRef, useState } from "react";
import { Trip } from "@/types";
import { isActiveTrip } from "@/constants";
import { formatCurrency } from "@/lib/formatters";
import { Layers, MapPin, Radio, Zap, Info, Maximize2, Compass } from "lucide-react";
import * as maplibregl from "maplibre-gl";
import { setWorkerUrl } from "maplibre-gl";

// Fix: maplibre-gl v6 cần WebWorker để decode vector tiles.
// Dùng setWorkerUrl() để trỏ đến file worker bundle sẵn trong /public
// tránh bị chặn bởi CSP hoặc Next.js bundler.
if (typeof window !== "undefined") {
  setWorkerUrl("/maplibre-gl-worker.js");
}

interface LiveDispatchMapProps {
  trips: Trip[];
  onSelectTrip: (trip: Trip) => void;
}

const GOONG_MAPTILES_KEY = process.env.NEXT_PUBLIC_GOONG_MAPTILES_KEY || "";
const GOONG_DARK_STYLE = `https://tiles.goong.io/assets/goong_map_dark.json?api_key=${GOONG_MAPTILES_KEY}`;

// Bounding box for TP.HCM Urban Area (for Tactical SVG mode)
const MIN_LAT = 10.72;
const MAX_LAT = 10.89;
const MIN_LNG = 106.63;
const MAX_LNG = 106.82;

function projectToCanvas(lat: number, lng: number, width: number, height: number) {
  const x = ((lng - MIN_LNG) / (MAX_LNG - MIN_LNG)) * width;
  const y = height - ((lat - MIN_LAT) / (MAX_LAT - MIN_LAT)) * height;
  return { x: Math.max(20, Math.min(width - 20, x)), y: Math.max(20, Math.min(height - 20, y)) };
}

export default function LiveDispatchMap({ trips, onSelectTrip }: LiveDispatchMapProps) {
  const [mapMode, setMapMode] = useState<"GOONG" | "RADAR">("GOONG");
  const [hoveredTrip, setHoveredTrip] = useState<Trip | null>(null);

  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<maplibregl.Map | null>(null);
  const markersRef = useRef<maplibregl.Marker[]>([]);

  const searchingTrips = trips.filter((t) => t.status === "SEARCHING");
  const activeTrips = trips.filter((t) => isActiveTrip(t.status));

  // Destroy map when switching AWAY from GOONG mode
  useEffect(() => {
    if (mapMode !== "GOONG") {
      markersRef.current.forEach((m) => m.remove());
      markersRef.current = [];
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    }
  }, [mapMode]);

  // Initialize MapLibre GL when entering GOONG mode
  useEffect(() => {
    if (mapMode !== "GOONG" || !mapContainerRef.current) return;

    // Guard: no key configured
    if (!GOONG_MAPTILES_KEY) {
      console.warn("NEXT_PUBLIC_GOONG_MAPTILES_KEY không được cấu hình, chuyển sang chế độ Radar");
      setMapMode("RADAR");
      return;
    }

    // Always recreate map when entering GOONG mode
    // (container div unmounts/remounts when switching modes)
    if (mapInstanceRef.current) {
      try { mapInstanceRef.current.remove(); } catch (_) {}
      mapInstanceRef.current = null;
    }

    let map: maplibregl.Map;
    try {
      map = new maplibregl.Map({
        container: mapContainerRef.current,
        style: GOONG_DARK_STYLE,
        center: [106.700981, 10.77653],
        zoom: 12.2,
        attributionControl: false,
      });

      // Log errors but do NOT auto-switch to RADAR on tile errors
      map.on("error", (e) => {
        console.warn("[MapLibre] lỗi:", e.error?.message || e);
      });

      map.addControl(new maplibregl.NavigationControl({ showCompass: true }), "top-right");
      mapInstanceRef.current = map;
    } catch (err) {
      console.warn("[MapLibre] Không thể khởi tạo bản đồ:", err);
      // Only switch to RADAR on hard initialization failure
      setMapMode("RADAR");
      return;
    }

    // Add markers after style loads
    const updateMapContent = () => {
      if (!mapInstanceRef.current) return;
      markersRef.current.forEach((m) => m.remove());
      markersRef.current = [];

      searchingTrips.forEach((trip) => {
        if (!trip.pickupLng || !trip.pickupLat) return;
        const el = document.createElement("div");
        el.className = "goong-marker-searching cursor-pointer relative group";
        el.innerHTML = `
          <div class="w-8 h-8 rounded-full bg-amber-500/20 animate-ping absolute -top-1 -left-1"></div>
          <div class="w-6 h-6 rounded-full bg-amber-500/40 border-2 border-amber-400 flex items-center justify-center shadow-lg shadow-amber-500/50">
            <span class="text-xs">🛵</span>
          </div>
        `;
        el.addEventListener("click", () => onSelectTrip(trip));
        el.addEventListener("mouseenter", () => setHoveredTrip(trip));
        el.addEventListener("mouseleave", () => setHoveredTrip(null));
        const marker = new maplibregl.Marker({ element: el })
          .setLngLat([trip.pickupLng, trip.pickupLat])
          .addTo(map);
        markersRef.current.push(marker);
      });

      activeTrips.forEach((trip) => {
        if (trip.pickupLng && trip.pickupLat) {
          const el = document.createElement("div");
          el.className = "goong-marker-active cursor-pointer relative group";
          el.innerHTML = `
            <div class="w-7 h-7 rounded-full bg-emerald-500/30 border-2 border-emerald-400 flex items-center justify-center shadow-lg shadow-emerald-500/50">
              <span class="text-xs">🚗</span>
            </div>
          `;
          el.addEventListener("click", () => onSelectTrip(trip));
          el.addEventListener("mouseenter", () => setHoveredTrip(trip));
          el.addEventListener("mouseleave", () => setHoveredTrip(null));
          const marker = new maplibregl.Marker({ element: el })
            .setLngLat([trip.pickupLng, trip.pickupLat])
            .addTo(map);
          markersRef.current.push(marker);
        }
        if (trip.dropoffLng && trip.dropoffLat) {
          const dropEl = document.createElement("div");
          dropEl.className = "w-4 h-4 rounded-full bg-cyan-400 border-2 border-slate-950 shadow-md";
          const dropMarker = new maplibregl.Marker({ element: dropEl })
            .setLngLat([trip.dropoffLng, trip.dropoffLat])
            .addTo(map);
          markersRef.current.push(dropMarker);
        }
      });
    };

    if (map.isStyleLoaded()) {
      updateMapContent();
    } else {
      map.once("load", updateMapContent);
    }
  }, [mapMode]);

  // Update markers when trips change (without reinitializing the map)
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map || mapMode !== "GOONG") return;

    const updateMarkers = () => {
      if (!mapInstanceRef.current) return;
      markersRef.current.forEach((m) => m.remove());
      markersRef.current = [];

      searchingTrips.forEach((trip) => {
        if (!trip.pickupLng || !trip.pickupLat) return;
        const el = document.createElement("div");
        el.className = "goong-marker-searching cursor-pointer relative group";
        el.innerHTML = `
          <div class="w-8 h-8 rounded-full bg-amber-500/20 animate-ping absolute -top-1 -left-1"></div>
          <div class="w-6 h-6 rounded-full bg-amber-500/40 border-2 border-amber-400 flex items-center justify-center shadow-lg shadow-amber-500/50">
            <span class="text-xs">🛵</span>
          </div>
        `;
        el.addEventListener("click", () => onSelectTrip(trip));
        el.addEventListener("mouseenter", () => setHoveredTrip(trip));
        el.addEventListener("mouseleave", () => setHoveredTrip(null));
        new maplibregl.Marker({ element: el })
          .setLngLat([trip.pickupLng, trip.pickupLat])
          .addTo(map);
      });

      activeTrips.forEach((trip) => {
        if (trip.pickupLng && trip.pickupLat) {
          const el = document.createElement("div");
          el.className = "goong-marker-active cursor-pointer relative group";
          el.innerHTML = `
            <div class="w-7 h-7 rounded-full bg-emerald-500/30 border-2 border-emerald-400 flex items-center justify-center shadow-lg shadow-emerald-500/50">
              <span class="text-xs">🚗</span>
            </div>
          `;
          el.addEventListener("click", () => onSelectTrip(trip));
          el.addEventListener("mouseenter", () => setHoveredTrip(trip));
          el.addEventListener("mouseleave", () => setHoveredTrip(null));
          new maplibregl.Marker({ element: el })
            .setLngLat([trip.pickupLng, trip.pickupLat])
            .addTo(map);
        }
        if (trip.dropoffLng && trip.dropoffLat) {
          const dropEl = document.createElement("div");
          dropEl.className = "w-4 h-4 rounded-full bg-cyan-400 border-2 border-slate-950 shadow-md";
          new maplibregl.Marker({ element: dropEl })
            .setLngLat([trip.dropoffLng, trip.dropoffLat])
            .addTo(map);
        }
      });
    };

    if (map.isStyleLoaded()) {
      updateMarkers();
    } else {
      map.once("load", updateMarkers);
    }
  }, [trips]);

  // Cleanup on component unmount
  useEffect(() => {
    return () => {
      markersRef.current.forEach((m) => m.remove());
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  const width = 900;
  const height = 520;

  return (
    <div className="relative w-full overflow-hidden border border-slate-800 rounded-2xl bg-slate-950 shadow-2xl">
      {/* Top Map Control Bar */}
      <div className="absolute top-4 left-4 z-10 flex items-center gap-3">
        <div className="px-3.5 py-1.5 rounded-xl bg-slate-900/90 border border-slate-700/80 backdrop-blur-md flex items-center gap-2 shadow-lg">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span className="text-xs font-bold text-white tracking-wide">
            {mapMode === "GOONG" ? "Bản đồ Goong Realtime" : "Radar Chiến thuật"}
          </span>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300">
            {searchingTrips.length} đang quét • {activeTrips.length} đang chạy
          </span>
        </div>

        {/* View Switcher: Goong Map vs Tactical SVG Radar */}
        <div className="flex items-center bg-slate-900/90 border border-slate-700/80 p-1 rounded-xl shadow-lg backdrop-blur-md">
          <button
            onClick={() => setMapMode("GOONG")}
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition flex items-center gap-1.5 ${
              mapMode === "GOONG"
                ? "bg-emerald-500 text-slate-950 shadow"
                : "text-slate-400 hover:text-white"
            }`}
            title="Sử dụng nền bản đồ Goong Map Vector Tiles"
          >
            <Compass className="w-3.5 h-3.5" />
            <span>Goong Tiles</span>
          </button>
          <button
            onClick={() => setMapMode("RADAR")}
            className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition flex items-center gap-1.5 ${
              mapMode === "RADAR"
                ? "bg-emerald-500 text-slate-950 shadow"
                : "text-slate-400 hover:text-white"
            }`}
            title="Chế độ Radar lưới đồ họa"
          >
            <Radio className="w-3.5 h-3.5" />
            <span>Radar Grid</span>
          </button>
        </div>
      </div>

      {/* Map Legend */}
      <div className="absolute top-4 right-14 z-10 flex items-center gap-2 bg-slate-900/90 border border-slate-800 px-3 py-1.5 rounded-xl text-[11px] text-slate-300 backdrop-blur-md">
        <div className="flex items-center gap-1.5">
          <span className="w-2.5 h-2.5 rounded-full bg-amber-400 ring-2 ring-amber-400/30" />
          <span>Tìm tài xế</span>
        </div>
        <div className="flex items-center gap-1.5 ml-2">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 ring-2 ring-emerald-400/30" />
          <span>Đang di chuyển</span>
        </div>
        <div className="flex items-center gap-1.5 ml-2">
          <span className="w-2.5 h-2.5 rounded-full bg-cyan-400" />
          <span>Điểm trả</span>
        </div>
      </div>

      {/* Primary Map Display */}
      {mapMode === "GOONG" ? (
        <div
          ref={mapContainerRef}
          className="w-full bg-slate-950"
          style={{ height: "520px", minHeight: "520px" }}
        />
      ) : (
        /* Tactical SVG Canvas */
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="w-full h-auto bg-slate-950 select-none"
          style={{ minHeight: "440px" }}
        >
          <defs>
            <filter id="glow-amber" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="6" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>
            <filter id="glow-emerald" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="6" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>
            <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
              <path
                d="M 40 0 L 0 0 0 40"
                fill="none"
                stroke="#1e293b"
                strokeWidth="0.8"
                strokeOpacity="0.4"
              />
            </pattern>
          </defs>

          <rect width={width} height={height} fill="url(#grid)" />

          <path
            d="M 120 40 Q 250 140 380 200 T 520 310 T 680 410 T 880 480"
            fill="none"
            stroke="#0e7490"
            strokeWidth="14"
            strokeOpacity="0.18"
            strokeLinecap="round"
          />
          <path
            d="M 120 40 Q 250 140 380 200 T 520 310 T 680 410 T 880 480"
            fill="none"
            stroke="#06b6d4"
            strokeWidth="3"
            strokeOpacity="0.35"
          />

          <text x="360" y="240" fill="#64748b" fontSize="10" fontWeight="600" opacity="0.6">
            Quận 1 (Trung tâm)
          </text>
          <text x="640" y="140" fill="#64748b" fontSize="10" fontWeight="600" opacity="0.6">
            TP. Thủ Đức
          </text>
          <text x="180" y="160" fill="#64748b" fontSize="10" fontWeight="600" opacity="0.6">
            Sân bay Tân Sơn Nhất
          </text>

          {activeTrips.map((trip) => {
            const pickup = projectToCanvas(trip.pickupLat, trip.pickupLng, width, height);
            const dropoff = projectToCanvas(trip.dropoffLat, trip.dropoffLng, width, height);
            return (
              <g key={`route-${trip.tripId}`}>
                <line
                  x1={pickup.x}
                  y1={pickup.y}
                  x2={dropoff.x}
                  y2={dropoff.y}
                  stroke="#10b981"
                  strokeWidth="2"
                  strokeDasharray="4 4"
                  strokeOpacity="0.6"
                />
                <circle cx={dropoff.x} cy={dropoff.y} r="4" fill="#06b6d4" />
              </g>
            );
          })}

          {searchingTrips.map((trip) => {
            const pos = projectToCanvas(trip.pickupLat, trip.pickupLng, width, height);
            return (
              <g
                key={`search-${trip.tripId}`}
                className="cursor-pointer group"
                onClick={() => onSelectTrip(trip)}
                onMouseEnter={() => setHoveredTrip(trip)}
                onMouseLeave={() => setHoveredTrip(null)}
              >
                <circle
                  cx={pos.x}
                  cy={pos.y}
                  r="24"
                  fill="#f59e0b"
                  fillOpacity="0.1"
                  stroke="#f59e0b"
                  strokeWidth="1"
                  strokeOpacity="0.4"
                  className="animate-ping"
                  style={{ transformOrigin: `${pos.x}px ${pos.y}px` }}
                />
                <circle cx={pos.x} cy={pos.y} r="14" fill="#f59e0b" fillOpacity="0.2" />
                <circle cx={pos.x} cy={pos.y} r="6" fill="#f59e0b" filter="url(#glow-amber)" />
                <text x={pos.x - 8} y={pos.y - 12} fontSize="11" fill="#fef08a">
                  🛵
                </text>
              </g>
            );
          })}

          {activeTrips.map((trip) => {
            const pos = projectToCanvas(trip.pickupLat, trip.pickupLng, width, height);
            return (
              <g
                key={`active-${trip.tripId}`}
                className="cursor-pointer group"
                onClick={() => onSelectTrip(trip)}
                onMouseEnter={() => setHoveredTrip(trip)}
                onMouseLeave={() => setHoveredTrip(null)}
              >
                <circle cx={pos.x} cy={pos.y} r="16" fill="#10b981" fillOpacity="0.15" />
                <circle cx={pos.x} cy={pos.y} r="7" fill="#10b981" filter="url(#glow-emerald)" />
                <text x={pos.x - 8} y={pos.y - 12} fontSize="11">
                  🚗
                </text>
              </g>
            );
          })}
        </svg>
      )}

      {/* Hover Info Tooltip Card */}
      {hoveredTrip && (
        <div className="absolute bottom-4 left-4 z-20 p-4 rounded-xl bg-slate-900/95 border border-slate-700 shadow-2xl backdrop-blur-md max-w-sm text-xs text-slate-200 animate-in fade-in zoom-in-95 duration-150">
          <div className="flex items-center justify-between gap-2 mb-2">
            <span className="font-mono font-bold text-emerald-400">{hoveredTrip.tripCode}</span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-800 text-slate-300">
              {hoveredTrip.status}
            </span>
          </div>
          <p className="truncate text-slate-300 mb-1">
            <strong>Đón:</strong> {hoveredTrip.pickupAddress}
          </p>
          <p className="truncate text-slate-400 mb-2">
            <strong>Trả:</strong> {hoveredTrip.dropoffAddress}
          </p>
          <div className="flex items-center justify-between pt-2 border-t border-slate-800 text-[11px]">
            <span className="text-emerald-400 font-bold">
              {formatCurrency(hoveredTrip.finalAmountVnd)}
            </span>
            <span className="text-slate-400">+{hoveredTrip.co2SavedGrams}g CO2</span>
          </div>
        </div>
      )}

      {/* Bottom Summary Bar */}
      <div className="p-3 bg-slate-950/80 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
        <div className="flex items-center gap-2">
          <Info className="w-3.5 h-3.5 text-slate-500" />
          <span>
            {mapMode === "GOONG"
              ? "Bản đồ Goong Map Tiles - Tự động tải vector đường phố & địa danh chuẩn Việt Nam"
              : "Bản đồ Radar chiến thuật - Bấm vào biểu tượng cuốc xe để xem chi tiết điều phối"}
          </span>
        </div>
        <span className="font-mono text-[11px] text-slate-500">
          Goong Tiles: {GOONG_MAPTILES_KEY ? "✓ Loaded" : "⚠ Missing"} | TP.HCM [10.77°N, 106.70°E]
        </span>
      </div>
    </div>
  );
}
