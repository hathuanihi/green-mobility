"use client";

import React from "react";
import { KycStatus } from "@/types";
import { KYC_STATUS_CONFIG } from "@/constants";

interface StatusBadgeProps {
  status: KycStatus;
  size?: "sm" | "md";
  showPulse?: boolean;
}

export default function StatusBadge({
  status,
  size = "md",
  showPulse = true,
}: StatusBadgeProps) {
  const config = KYC_STATUS_CONFIG[status];
  const Icon = config.icon;

  const isSmall = size === "sm";

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full font-semibold border ${
        config.badgeClass
      } ${isSmall ? "px-2 py-0.5 text-[11px]" : "px-3 py-1.5 text-xs"}`}
    >
      {status === "PENDING" && showPulse ? (
        <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
      ) : (
        <Icon className={isSmall ? "w-3 h-3" : "w-3.5 h-3.5"} />
      )}
      <span>{config.label}</span>
    </span>
  );
}
