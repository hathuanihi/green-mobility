"use client";

import React from "react";
import { TripStatus } from "@/types";
import { TRIP_STATUS_CONFIG } from "@/constants";

interface TripStatusBadgeProps {
  status: TripStatus;
  size?: "sm" | "md";
  showPulse?: boolean;
}

export default function TripStatusBadge({
  status,
  size = "md",
  showPulse = true,
}: TripStatusBadgeProps) {
  const config = TRIP_STATUS_CONFIG[status] || TRIP_STATUS_CONFIG.REQUESTED;
  const Icon = config.icon;
  const isSmall = size === "sm";

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full font-semibold border ${
        config.badgeClass
      } ${isSmall ? "px-2 py-0.5 text-[11px]" : "px-3 py-1.5 text-xs"}`}
    >
      {(status === "SEARCHING" || status === "IN_TRIP") && showPulse ? (
        <span
          className={`w-1.5 h-1.5 rounded-full animate-pulse ${
            status === "SEARCHING" ? "bg-amber-400" : "bg-emerald-400"
          }`}
        />
      ) : (
        <Icon className={isSmall ? "w-3 h-3" : "w-3.5 h-3.5"} />
      )}
      <span>{config.label}</span>
    </span>
  );
}
