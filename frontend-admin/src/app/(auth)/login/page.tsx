"use client";

import React, { useState } from "react";
import { Leaf, Lock, Phone, ArrowRight, ShieldCheck, Sparkles, AlertCircle } from "lucide-react";
import { useAuth } from "@/lib/auth";

export default function LoginPage() {
  const { login } = useAuth();
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");

    if (!phoneNumber.trim() || !password) {
      setErrorMessage("Vui lòng nhập đầy đủ Số điện thoại và Mật khẩu");
      return;
    }

    setIsLoading(true);
    try {
      await login(phoneNumber.trim(), password);
    } catch (err: any) {
      console.error(err);
      setErrorMessage(err?.response?.data?.message || err.message || "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin!");
    } finally {
      setIsLoading(false);
    }
  };

  const fillDemoAccount = () => {
    setPhoneNumber("0900000001");
    setPassword("Admin@123456");
    setErrorMessage("");
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-slate-950 relative overflow-hidden">
      {/* Ambient background glow */}
      <div className="absolute top-1/4 -left-32 w-96 h-96 bg-emerald-600/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 -right-32 w-96 h-96 bg-cyan-600/10 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md relative z-10 space-y-6">
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 mb-2 shadow-lg shadow-emerald-950">
            <Leaf className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Green Mobility</h1>
          <p className="text-xs text-slate-400">
            Cổng Quản trị & Điều hành Xe điện Giảm Phát thải CO2
          </p>
        </div>

        {/* Login Card */}
        <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-2xl backdrop-blur-xl space-y-6">
          <div className="space-y-1">
            <h2 className="text-lg font-semibold text-white">Đăng nhập Quản trị viên</h2>
            <p className="text-xs text-slate-400">
              Nhập tài khoản Admin hoặc Điều hành viên được cấp phép
            </p>
          </div>

          {errorMessage && (
            <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-start gap-3 text-rose-400 text-xs animate-in fade-in">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-slate-300">
                Số điện thoại
              </label>
              <div className="relative">
                <Phone className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="0900000001"
                  disabled={isLoading}
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-medium text-slate-300">
                Mật khẩu
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  disabled={isLoading}
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-emerald-500 focus:ring-1 focus:ring-emerald-500 transition"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full mt-2 flex items-center justify-center gap-2 py-3 px-4 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-sm rounded-xl shadow-lg shadow-emerald-950 transition disabled:opacity-50"
            >
              {isLoading ? (
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
              ) : (
                <>
                  <span>Truy cập Hệ thống</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Login helper */}
          <div className="pt-4 border-t border-slate-800/80">
            <button
              type="button"
              onClick={fillDemoAccount}
              className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-xl bg-slate-950 border border-slate-800 text-xs font-medium text-slate-300 hover:text-emerald-400 hover:border-emerald-500/40 transition"
            >
              <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
              <span>Điền nhanh tài khoản Admin Demo (0900000001)</span>
            </button>
          </div>
        </div>

        {/* Security badge */}
        <div className="flex items-center justify-center gap-2 text-xs text-slate-400">
          <ShieldCheck className="w-4 h-4 text-emerald-500" />
          <span>Bảo mật chuẩn JWT HMAC-SHA256 & Spring Security 6</span>
        </div>
      </div>
    </div>
  );
}
