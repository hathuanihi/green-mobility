"use client";

import React, { useState, useEffect, useMemo } from "react";
import apiClient from "@/lib/api";
import { FraudAlert, FraudStats, ApiResponse } from "@/types";
import {
  ShieldAlert,
  ShieldCheck,
  AlertTriangle,
  Radio,
  ZapOff,
  Navigation,
  CheckCircle,
  XCircle,
  RefreshCw,
  Search,
  Filter,
  User,
  Clock,
  ExternalLink,
  ChevronRight,
  Flame,
} from "lucide-react";

export default function FraudMonitorPage() {
  const [alerts, setAlerts] = useState<FraudAlert[]>([]);
  const [stats, setStats] = useState<FraudStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [typeFilter, setTypeFilter] = useState<string>("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedAlert, setSelectedAlert] = useState<FraudAlert | null>(null);
  const [isUpdating, setIsUpdating] = useState(false);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      const [alertsRes, statsRes] = await Promise.all([
        apiClient.get<ApiResponse<FraudAlert[]>>("/admin/fraud/alerts"),
        apiClient.get<ApiResponse<FraudStats>>("/admin/fraud/stats"),
      ]);

      if (alertsRes.data.success && alertsRes.data.data) {
        setAlerts(alertsRes.data.data);
      }
      if (statsRes.data.success && statsRes.data.data) {
        setStats(statsRes.data.data);
      }
    } catch (err) {
      console.error("Lỗi khi tải cảnh báo gian lận:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleResolveAlert = async (id: string, status: "RESOLVED" | "DISMISSED") => {
    try {
      setIsUpdating(true);
      const res = await apiClient.patch<ApiResponse<FraudAlert>>(
        `/admin/fraud/alerts/${id}/resolve?status=${status}`
      );
      if (res.data.success) {
        setAlerts((prev) =>
          prev.map((item) => (item.id === id ? { ...item, resolutionStatus: status } : item))
        );
        if (selectedAlert?.id === id) {
          setSelectedAlert((prev) => (prev ? { ...prev, resolutionStatus: status } : null));
        }
        // Refresh stats
        const statsRes = await apiClient.get<ApiResponse<FraudStats>>("/admin/fraud/stats");
        if (statsRes.data.success && statsRes.data.data) {
          setStats(statsRes.data.data);
        }
      }
    } catch (err) {
      console.error("Lỗi cập nhật cảnh báo:", err);
      alert("Không thể cập nhật trạng thái cảnh báo!");
    } finally {
      setIsUpdating(false);
    }
  };

  const filteredAlerts = useMemo(() => {
    return alerts.filter((a) => {
      const matchSearch =
        (a.driverName && a.driverName.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (a.driverPhone && a.driverPhone.includes(searchQuery)) ||
        (a.vehiclePlate && a.vehiclePlate.toLowerCase().includes(searchQuery.toLowerCase())) ||
        a.alertType.toLowerCase().includes(searchQuery.toLowerCase());

      const matchStatus = statusFilter === "ALL" || a.resolutionStatus === statusFilter;
      const matchType = typeFilter === "ALL" || a.alertType === typeFilter;

      return matchSearch && matchStatus && matchType;
    });
  }, [alerts, searchQuery, statusFilter, typeFilter]);

  const parseDetails = (detailsStr: string) => {
    try {
      return JSON.parse(detailsStr);
    } catch {
      return { reason: detailsStr };
    }
  };

  const getAlertIcon = (type: string) => {
    switch (type) {
      case "GPS_SPOOFING":
        return <Radio className="w-5 h-5 text-rose-400" />;
      case "HIGH_SPEED_ANOMALY":
        return <Flame className="w-5 h-5 text-amber-400" />;
      case "BATTERY_DRAIN_MISMATCH":
        return <ZapOff className="w-5 h-5 text-yellow-400" />;
      default:
        return <AlertTriangle className="w-5 h-5 text-rose-400" />;
    }
  };

  const getAlertTitle = (type: string) => {
    switch (type) {
      case "GPS_SPOOFING":
        return "Nghi vấn Giả lập GPS (Mock Location)";
      case "HIGH_SPEED_ANOMALY":
        return "Cảnh báo Tốc độ Bất thường";
      case "BATTERY_DRAIN_MISMATCH":
        return "Lệch Tiêu hao Pin vs Quãng đường";
      default:
        return type;
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-12">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="px-2.5 py-1 rounded-md text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20 flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping" />
              Safety & Anti-Fraud
            </span>
            <span className="text-xs text-slate-400">• Giám sát Thuật toán Real-time</span>
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight mt-2 flex items-center gap-3">
            <ShieldAlert className="w-8 h-8 text-rose-400" />
            Giám sát Gian Lận & An Toàn GPS
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Tự động phát hiện Mock Location, teleportation, vi phạm tốc độ và khống hành trình nhận điểm Carbon
          </p>
        </div>

        <button
          onClick={fetchData}
          disabled={isLoading}
          className="flex items-center gap-2 px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-xl border border-slate-700 transition active:scale-95 disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin text-rose-400" : ""}`} />
          Làm mới dữ liệu
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-md group hover:border-slate-700 transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Tổng Sự Cố</span>
            <div className="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-slate-300">
              <ShieldAlert className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-black text-white mt-3">{stats?.totalAlerts ?? 0}</div>
          <div className="text-xs text-slate-400 mt-1">Toàn bộ lịch sử cảnh báo</div>
        </div>

        <div className="bg-slate-900/60 border border-rose-500/20 rounded-2xl p-5 backdrop-blur-md group hover:border-rose-500/40 transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-rose-400 uppercase tracking-wider">Chờ Xử Lý (Pending)</span>
            <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
              <AlertTriangle className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-black text-rose-400 mt-3">{stats?.pendingAlerts ?? 0}</div>
          <div className="text-xs text-rose-400/80 mt-1">Cần điều hành viên rà soát ngay</div>
        </div>

        <div className="bg-slate-900/60 border border-emerald-500/20 rounded-2xl p-5 backdrop-blur-md group hover:border-emerald-500/40 transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wider">Đã Xử Lý</span>
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-black text-emerald-400 mt-3">{stats?.resolvedAlerts ?? 0}</div>
          <div className="text-xs text-slate-400 mt-1">Đã xác minh hoặc đóng hồ sơ</div>
        </div>

        <div className="bg-slate-900/60 border border-amber-500/20 rounded-2xl p-5 backdrop-blur-md group hover:border-amber-500/40 transition">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-amber-400 uppercase tracking-wider">Tỉ Lệ Rủi Ro Cao</span>
            <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <Flame className="w-5 h-5" />
            </div>
          </div>
          <div className="text-3xl font-black text-amber-400 mt-3">{stats?.highRiskRatio ?? 0}%</div>
          <div className="text-xs text-slate-400 mt-1">Tỉ lệ sự cố chưa xử lý</div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 flex flex-col md:flex-row gap-4 justify-between items-center">
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm tài xế, biển số, loại cảnh báo..."
            className="w-full pl-10 pr-4 py-2 bg-slate-800/80 border border-slate-700/80 rounded-xl text-sm text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-rose-500 transition"
          />
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <div className="flex items-center gap-2 bg-slate-800/60 border border-slate-700/60 rounded-xl px-3 py-1.5 text-xs text-slate-300">
            <Filter className="w-3.5 h-3.5 text-slate-400" />
            <span>Trạng thái:</span>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-transparent text-rose-400 font-medium focus:outline-none cursor-pointer"
            >
              <option value="ALL" className="bg-slate-900 text-white">Tất cả</option>
              <option value="PENDING" className="bg-slate-900 text-white">Chờ xử lý (Pending)</option>
              <option value="RESOLVED" className="bg-slate-900 text-white">Đã giải quyết (Resolved)</option>
              <option value="DISMISSED" className="bg-slate-900 text-white">Bỏ qua (Dismissed)</option>
            </select>
          </div>

          <div className="flex items-center gap-2 bg-slate-800/60 border border-slate-700/60 rounded-xl px-3 py-1.5 text-xs text-slate-300">
            <span>Loại cảnh báo:</span>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="bg-transparent text-rose-400 font-medium focus:outline-none cursor-pointer"
            >
              <option value="ALL" className="bg-slate-900 text-white">Tất cả loại</option>
              <option value="GPS_SPOOFING" className="bg-slate-900 text-white">GPS Mocking</option>
              <option value="HIGH_SPEED_ANOMALY" className="bg-slate-900 text-white">Quá tốc độ</option>
              <option value="BATTERY_DRAIN_MISMATCH" className="bg-slate-900 text-white">Lệch pin kWh</option>
            </select>
          </div>
        </div>
      </div>

      {/* Alerts Table */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl overflow-hidden shadow-xl backdrop-blur-md">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-800/60 text-xs uppercase font-semibold text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-6 py-4">Loại Cảnh báo & Rủi ro</th>
                <th className="px-6 py-4">Tài xế & Phương tiện</th>
                <th className="px-6 py-4">Chi tiết Phát hiện (Telemetries)</th>
                <th className="px-6 py-4">Thời gian</th>
                <th className="px-6 py-4 text-center">Trạng thái</th>
                <th className="px-6 py-4 text-right">Xử lý</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-slate-400">
                    <RefreshCw className="w-6 h-6 animate-spin mx-auto text-rose-400 mb-2" />
                    Đang tải dữ liệu cảnh báo gian lận...
                  </td>
                </tr>
              ) : filteredAlerts.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-slate-400">
                    Không có cảnh báo gian lận nào phù hợp điều kiện lọc.
                  </td>
                </tr>
              ) : (
                filteredAlerts.map((a) => {
                  const details = parseDetails(a.details);
                  const isPending = a.resolutionStatus === "PENDING";
                  const isResolved = a.resolutionStatus === "RESOLVED";

                  return (
                    <tr key={a.id} className="hover:bg-slate-800/40 transition">
                      <td className="px-6 py-4">
                        <div className="flex items-start gap-3">
                          <div className="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center shrink-0 mt-0.5">
                            {getAlertIcon(a.alertType)}
                          </div>
                          <div>
                            <div className="font-semibold text-white">
                              {getAlertTitle(a.alertType)}
                            </div>
                            <div className="mt-1 flex items-center gap-2">
                              <span
                                className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                                  a.riskScore >= 90
                                    ? "bg-rose-500/20 text-rose-400 border border-rose-500/30"
                                    : a.riskScore >= 75
                                    ? "bg-amber-500/20 text-amber-400 border border-amber-500/30"
                                    : "bg-blue-500/20 text-blue-400 border border-blue-500/30"
                                }`}
                              >
                                Risk Score: {a.riskScore}%
                              </span>
                            </div>
                          </div>
                        </div>
                      </td>

                      <td className="px-6 py-4">
                        <div className="font-medium text-slate-200 flex items-center gap-1.5">
                          <User className="w-3.5 h-3.5 text-slate-400" />
                          {a.driverName}
                        </div>
                        <div className="text-xs text-slate-400 mt-0.5">
                          SĐT: {a.driverPhone}
                        </div>
                        {a.vehiclePlate !== "-" && (
                          <span className="font-mono text-[10px] px-1.5 py-0.5 rounded bg-slate-950 border border-slate-700 text-amber-300 mt-1 inline-block">
                            {a.vehiclePlate}
                          </span>
                        )}
                      </td>

                      <td className="px-6 py-4 max-w-xs">
                        <div className="text-xs text-slate-300 line-clamp-2 leading-relaxed">
                          {details.reason || a.details}
                        </div>
                        {details.speed_recorded && (
                          <div className="text-[11px] text-amber-400 mt-1 font-mono">
                            ⚡ Tốc độ ghi nhận: {details.speed_recorded}
                          </div>
                        )}
                        {details.app && (
                          <div className="text-[11px] text-rose-400 mt-0.5 font-mono">
                            📱 Ứng dụng gian lận: {details.app}
                          </div>
                        )}
                      </td>

                      <td className="px-6 py-4">
                        <div className="text-xs text-slate-300 flex items-center gap-1.5">
                          <Clock className="w-3.5 h-3.5 text-slate-400" />
                          {new Date(a.createdAt).toLocaleTimeString("vi-VN", {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}{" "}
                          - {new Date(a.createdAt).toLocaleDateString("vi-VN")}
                        </div>
                      </td>

                      <td className="px-6 py-4 text-center">
                        {isPending ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                            <AlertTriangle className="w-3 h-3" /> Chờ xử lý
                          </span>
                        ) : isResolved ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                            <CheckCircle className="w-3 h-3" /> Đã giải quyết
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-700/50 text-slate-400 border border-slate-700">
                            <XCircle className="w-3 h-3" /> Đã bỏ qua
                          </span>
                        )}
                      </td>

                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => setSelectedAlert(a)}
                            className="px-3 py-1.5 text-xs font-medium text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 rounded-lg border border-slate-700 transition"
                          >
                            Chi tiết
                          </button>

                          {isPending && (
                            <button
                              onClick={() => handleResolveAlert(a.id, "RESOLVED")}
                              disabled={isUpdating}
                              className="px-3 py-1.5 text-xs font-semibold bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 border border-emerald-500/20 rounded-lg transition"
                            >
                              Xử lý
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Alert Detail Modal */}
      {selectedAlert && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-lg p-6 space-y-6 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
                  <ShieldAlert className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-lg text-white">
                    {getAlertTitle(selectedAlert.alertType)}
                  </h3>
                  <p className="text-xs text-slate-400">ID: {selectedAlert.id}</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedAlert(null)}
                className="text-slate-400 hover:text-white text-lg font-bold"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4 text-sm">
              <div className="bg-slate-800/40 p-4 rounded-xl border border-slate-800">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                  Bằng chứng vi phạm thuật toán
                </span>
                <div className="mt-2 text-slate-200 leading-relaxed text-sm bg-slate-950 p-3 rounded-lg font-mono text-xs border border-slate-800">
                  {selectedAlert.details}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
                  <span className="text-xs text-slate-400">Tài xế vi phạm</span>
                  <p className="font-semibold text-white mt-1">{selectedAlert.driverName}</p>
                  <p className="text-xs text-slate-400">{selectedAlert.driverPhone}</p>
                </div>

                <div className="bg-slate-800/40 p-3 rounded-xl border border-slate-800">
                  <span className="text-xs text-slate-400">Risk Score Đánh giá</span>
                  <p className="text-xl font-bold text-rose-400 mt-1">{selectedAlert.riskScore}%</p>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-slate-800">
              <button
                onClick={() => setSelectedAlert(null)}
                className="px-4 py-2 text-sm font-medium text-slate-300 hover:text-white bg-slate-800 rounded-xl"
              >
                Đóng
              </button>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleResolveAlert(selectedAlert.id, "DISMISSED")}
                  disabled={isUpdating}
                  className="px-4 py-2 text-sm font-medium text-slate-400 hover:text-slate-200 bg-slate-800 hover:bg-slate-700 rounded-xl transition"
                >
                  Bỏ qua
                </button>
                <button
                  onClick={() => handleResolveAlert(selectedAlert.id, "RESOLVED")}
                  disabled={isUpdating}
                  className="px-4 py-2 text-sm font-semibold bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl transition"
                >
                  Xác nhận xử lý cảnh báo
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
