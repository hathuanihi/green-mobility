"use client";

import React, { useState } from "react";
import { Trip } from "@/types";
import { isActiveTrip } from "@/constants";
import { formatCurrency } from "@/lib/formatters";
import { Car, Navigation, MapPin, Radio, Zap, Info, Maximize2 } from "lucide-react";

interface LiveDispatchMapProps {
  trips: Trip[];
  onSelectTrip: (trip: Trip) => void;
}

// Bounding box for TP.HCM Urban Area
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
  const [hoveredTrip, setHoveredTrip] = useState<Trip | null>(null);

  const width = 900;
  const height = 520;

  // Filter searching and active trips for live map rendering
  const searchingTrips = trips.filter((t) => t.status === "SEARCHING");
  const activeTrips = trips.filter((t) => isActiveTrip(t.status));


  return (
    <div className="relative w-full overflow-hidden border border-slate-800 rounded-2xl bg-slate-950 shadow-2xl">
      {/* Top Map Control Bar */}
      <div className="absolute top-4 left-4 z-10 flex items-center gap-3">
        <div className="px-3.5 py-1.5 rounded-xl bg-slate-900/90 border border-slate-700/80 backdrop-blur-md flex items-center gap-2 shadow-lg">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span className="text-xs font-bold text-white tracking-wide">Live Dispatch Radar</span>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300">
            {searchingTrips.length} đang quét • {activeTrips.length} đang chạy
          </span>
        </div>
      </div>

      {/* Map Legend */}
      <div className="absolute top-4 right-4 z-10 flex items-center gap-2 bg-slate-900/90 border border-slate-800 px-3 py-1.5 rounded-xl text-[11px] text-slate-300 backdrop-blur-md">
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

      {/* Interactive SVG Canvas */}
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full h-auto bg-slate-950 select-none"
        style={{ minHeight: "440px" }}
      >
        <defs>
          {/* Glowing Filters */}
          <filter id="glow-amber" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="6" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
          <filter id="glow-emerald" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="6" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>

          {/* Grid Pattern */}
          <pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#1e293b" strokeWidth="0.8" strokeOpacity="0.4" />
          </pattern>
        </defs>

        {/* Background Grid */}
        <rect width={width} height={height} fill="url(#grid)" />

        {/* Stylized Saigon River Contour */}
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

        {/* Landmarks */}
        <text x="360" y="240" fill="#64748b" fontSize="10" fontWeight="600" opacity="0.6">
          Quận 1 (Trung tâm)
        </text>
        <text x="640" y="140" fill="#64748b" fontSize="10" fontWeight="600" opacity="0.6">
          TP. Thủ Đức
        </text>
        <text x="180" y="160" fill="#64748b" fontSize="10" fontWeight="600" opacity="0.6">
          Sân bay Tân Sơn Nhất
        </text>

        {/* Render Active Trips Route Polylines */}
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
              {/* Dropoff marker */}
              <circle cx={dropoff.x} cy={dropoff.y} r="4" fill="#06b6d4" />
            </g>
          );
        })}

        {/* Render Searching Trips with Radar Pulse Ripples */}
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
              {/* Pulsing Radar Ripples */}
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

              {/* Vehicle Indicator Icon */}
              <text x={pos.x - 8} y={pos.y - 12} fontSize="11" fill="#fef08a">
                🛵
              </text>
            </g>
          );
        })}

        {/* Render Active Moving Trips */}
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
          <span>Bấm vào biểu tượng cuốc xe trên bản đồ để xem chi tiết điều phối</span>
        </div>
        <span className="font-mono text-[11px] text-slate-500">
          Tọa độ tham chiếu: TP. Hồ Chí Minh [10.77°N, 106.70°E]
        </span>
      </div>
    </div>
  );
}
