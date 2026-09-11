"use client";

import React from "react";
import { ShieldCheck } from "lucide-react";
import { useAuth } from "@/lib/auth";

interface HeaderProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
}

export default function Header({ title, description, actions }: HeaderProps) {
  const { user } = useAuth();

  return (
    <header className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-6 border-b border-slate-800/80">
      <div>
        <h2 className="text-2xl font-extrabold text-white tracking-tight">{title}</h2>
        {description && <p className="text-sm text-slate-400 mt-1">{description}</p>}
      </div>

      <div className="flex items-center gap-3 self-start md:self-auto">
        {actions}

        <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-xs font-medium text-emerald-400">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span>Online • TPS: 142/s</span>
        </div>

        <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-slate-900 border border-slate-800 text-xs font-medium text-slate-300">
          <ShieldCheck className="w-3.5 h-3.5 text-cyan-400" />
          <span>{user?.role === "ROLE_ADMIN" ? "Quản trị viên cấp cao" : "Điều hành viên"}</span>
        </div>
      </div>
    </header>
  );
}
