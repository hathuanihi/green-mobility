"use client";

import React from "react";
import { FileText, ZoomIn, LucideIcon } from "lucide-react";
import { getFullMediaUrl } from "@/lib/api";

interface DocumentCardProps {
  title: string;
  subtitle?: string;
  badge?: string;
  imageUrl?: string;
  emptyIcon?: LucideIcon;
  emptyText?: string;
  onPreview: (url: string, title: string, subtitle?: string) => void;
}

export default function DocumentCard({
  title,
  subtitle,
  badge = "Bắt buộc",
  imageUrl,
  emptyIcon: EmptyIcon = FileText,
  emptyText = "Chưa có hình ảnh tài liệu",
  onPreview,
}: DocumentCardProps) {
  const fullUrl = getFullMediaUrl(imageUrl);

  return (
    <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-3 hover:border-slate-700/80 transition">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-semibold text-white text-sm">{title}</h3>
          {subtitle && <p className="text-xs text-slate-400 mt-0.5">{subtitle}</p>}
        </div>
        {badge && (
          <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            {badge}
          </span>
        )}
      </div>

      <div className="relative aspect-video rounded-xl bg-slate-950 border border-slate-800 overflow-hidden group flex items-center justify-center">
        {imageUrl ? (
          <>
            <img
              src={fullUrl}
              alt={title}
              className="w-full h-full object-cover transition group-hover:scale-105 duration-300 select-none"
            />
            <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition flex items-center justify-center">
              <button
                type="button"
                onClick={() => onPreview(fullUrl, title, subtitle)}
                className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-900/90 text-white text-xs font-medium border border-slate-700 shadow-xl hover:bg-slate-800 transition"
              >
                <ZoomIn className="w-3.5 h-3.5 text-emerald-400" />
                <span>Phóng to kiểm tra</span>
              </button>
            </div>
          </>
        ) : (
          <div className="text-center p-6 text-slate-500 space-y-1">
            <EmptyIcon className="w-8 h-8 mx-auto opacity-30" />
            <p className="text-xs">{emptyText}</p>
          </div>
        )}
      </div>
    </div>
  );
}
