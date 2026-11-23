"use client";

import React, { createContext, useCallback, useContext, useState } from "react";
import { CheckCircle2, AlertTriangle, Info, X, XCircle } from "lucide-react";

type ToastType = "success" | "error" | "warning" | "info";

interface ToastItem {
  id: string;
  type: ToastType;
  message: string;
}

interface ToastContextType {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextType>({
  showToast: () => {},
});

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (message: string, type: ToastType = "info") => {
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
      setToasts((prev) => [...prev.slice(-4), { id, type, message }]);
      setTimeout(() => removeToast(id), 4500);
    },
    [removeToast]
  );

  const getStyle = (type: ToastType) => {
    switch (type) {
      case "success":
        return "bg-emerald-950/95 border-emerald-500/40 text-emerald-200";
      case "error":
        return "bg-red-950/95 border-red-500/40 text-red-200";
      case "warning":
        return "bg-amber-950/95 border-amber-500/40 text-amber-200";
      default:
        return "bg-slate-900/95 border-slate-700 text-slate-200";
    }
  };

  const getIcon = (type: ToastType) => {
    switch (type) {
      case "success":
        return <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />;
      case "error":
        return <XCircle className="w-4 h-4 text-red-400 shrink-0" />;
      case "warning":
        return <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />;
      default:
        return <Info className="w-4 h-4 text-sky-400 shrink-0" />;
    }
  };

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed bottom-5 right-5 z-[100] flex flex-col gap-2.5 max-w-md w-full pointer-events-none px-4">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`pointer-events-auto flex items-start gap-3 px-4 py-3 rounded-xl border shadow-2xl backdrop-blur-md text-xs font-medium transition-all ${getStyle(t.type)}`}
          >
            {getIcon(t.type)}
            <span className="flex-1 leading-relaxed">{t.message}</span>
            <button
              onClick={() => removeToast(t.id)}
              className="text-slate-400 hover:text-white shrink-0"
              aria-label="Đóng thông báo"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  return useContext(ToastContext);
}
