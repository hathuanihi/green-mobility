"use client";

import React from "react";
import { X, ZoomIn, ExternalLink } from "lucide-react";

interface ImageModalProps {
  isOpen: boolean;
  onClose: () => void;
  imageUrl: string;
  title: string;
  subtitle?: string;
}

export default function ImageModal({
  isOpen,
  onClose,
  imageUrl,
  title,
  subtitle,
}: ImageModalProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in">
      <div className="relative w-full max-w-4xl bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-950/50">
          <div>
            <h3 className="text-lg font-semibold text-white">{title}</h3>
            {subtitle && <p className="text-xs text-slate-400 mt-0.5">{subtitle}</p>}
          </div>
          <div className="flex items-center gap-2">
            <a
              href={imageUrl}
              target="_blank"
              rel="noreferrer"
              className="p-2 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition"
              title="Mở tab mới"
            >
              <ExternalLink className="w-4 h-4" />
            </a>
            <button
              onClick={onClose}
              className="p-2 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Content Image */}
        <div className="flex-1 overflow-auto p-4 flex items-center justify-center bg-slate-950 min-h-[400px]">
          {imageUrl ? (
            <img
              src={imageUrl}
              alt={title}
              className="max-w-full max-h-[70vh] object-contain rounded-lg border border-slate-800/80 shadow-lg select-none"
            />
          ) : (
            <div className="text-center py-16 text-slate-500">
              <ZoomIn className="w-12 h-12 mx-auto mb-2 opacity-40" />
              <p>Chưa có hình ảnh tài liệu</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
