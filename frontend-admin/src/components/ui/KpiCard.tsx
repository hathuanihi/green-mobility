"use client";

import React from "react";
import { LucideIcon } from "lucide-react";

interface KpiCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  iconColor?: string;
  iconBgColor?: string;
  valueColor?: string;
  isActive?: boolean;
  activeBorderClass?: string;
  activeBgClass?: string;
  onClick?: () => void;
}

export default function KpiCard({
  title,
  value,
  subtitle,
  icon: Icon,
  iconColor = "text-emerald-400",
  iconBgColor = "bg-emerald-500/10",
  valueColor = "text-white",
  isActive = false,
  activeBorderClass = "border-emerald-500/40 shadow-lg shadow-emerald-950/30",
  activeBgClass = "bg-emerald-950/20",
  onClick,
}: KpiCardProps) {
  const isClickable = !!onClick;

  return (
    <div
      onClick={onClick}
      className={`p-4 rounded-2xl border transition ${
        isClickable ? "cursor-pointer" : ""
      } ${
        isActive
          ? `${activeBgClass} ${activeBorderClass}`
          : "bg-slate-900/60 border-slate-800 hover:border-slate-700"
      }`}
    >
      <div className="flex items-center justify-between text-slate-400">
        <span className="text-xs font-medium uppercase tracking-wider">{title}</span>
        <div className={`w-8 h-8 rounded-lg ${iconBgColor} flex items-center justify-center ${iconColor}`}>
          <Icon className="w-4 h-4" />
        </div>
      </div>
      <p className={`text-2xl font-bold mt-1.5 ${valueColor}`}>{value}</p>
      {subtitle && <p className="text-xs text-slate-400 mt-0.5 truncate">{subtitle}</p>}
    </div>
  );
}
