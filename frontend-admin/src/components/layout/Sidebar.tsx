"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Car,
  Leaf,
  TrendingUp,
  ShieldCheck,
  Sliders,
  Users,
  Bot,
  Activity,
  LogOut,
  ChevronRight,
} from "lucide-react";
import { useAuth } from "@/lib/auth";

const navItems = [
  { href: "/", label: "Tổng quan Hệ thống", icon: Activity },
  { href: "/drivers", label: "Tài xế & KYC", icon: Users },
  { href: "/vehicles", label: "Phương tiện Xe điện", icon: Car },
  { href: "/trips", label: "Chuyến đi & Replay", icon: TrendingUp },
  { href: "/emissions", label: "Hệ số Phát thải", icon: Sliders },
  { href: "/fraud-monitor", label: "Giám sát Gian lận", icon: ShieldCheck },
  { href: "/ai-copilot", label: "AI Admin Copilot", icon: Bot },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  const isActive = (path: string) => {
    if (path === "/") {
      return pathname === "/";
    }
    return pathname?.startsWith(path);
  };

  return (
    <aside className="w-64 border-r border-slate-800 bg-slate-900/60 backdrop-blur-md p-6 flex flex-col justify-between shrink-0 h-screen sticky top-0">
      <div>
        {/* Brand Logo */}
        <Link href="/" className="flex items-center gap-3 mb-8 group">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 group-hover:scale-105 transition">
            <Leaf className="w-6 h-6" />
          </div>
          <div>
            <h1 className="font-bold text-lg leading-tight text-white tracking-tight">
              Green Mobility
            </h1>
            <p className="text-xs text-emerald-400 font-medium">Admin Portal 1.0</p>
          </div>
        </Link>

        {/* Navigation */}
        <nav className="space-y-1.5 text-sm">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center justify-between px-3.5 py-2.5 rounded-xl font-medium transition ${
                  active
                    ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-sm shadow-emerald-950"
                    : "text-slate-400 hover:text-slate-100 hover:bg-slate-800/60"
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon className={`w-4 h-4 ${active ? "text-emerald-400" : "text-slate-400"}`} />
                  <span>{item.label}</span>
                </div>
                {active && <ChevronRight className="w-4 h-4 text-emerald-400" />}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* User Info & Logout */}
      <div className="pt-4 border-t border-slate-800 space-y-3">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-xs font-bold text-emerald-400">
            {user?.fullName ? user.fullName.substring(0, 2).toUpperCase() : "AD"}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold text-slate-200 truncate">
              {user?.fullName || "Admin Quản trị"}
            </p>
            <p className="text-xs text-slate-400 truncate">
              {user?.phoneNumber || "admin@greenmobility.vn"}
            </p>
          </div>
        </div>

        <button
          onClick={logout}
          className="w-full flex items-center gap-2.5 px-3 py-2 text-xs font-medium text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition border border-transparent hover:border-rose-500/20"
        >
          <LogOut className="w-3.5 h-3.5" />
          <span>Đăng xuất hệ thống</span>
        </button>
      </div>
    </aside>
  );
}
