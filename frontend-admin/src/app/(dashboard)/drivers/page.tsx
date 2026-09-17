"use client";

import React, { useEffect, useState, useMemo } from "react";
import { Search, RefreshCw, Users, Clock, CheckCircle2, XCircle } from "lucide-react";
import Header from "@/components/layout/Header";
import KpiCard from "@/components/ui/KpiCard";
import DriverTable from "@/components/drivers/DriverTable";
import apiClient from "@/lib/api";
import { ApiResponse, DriverSummary, KycStatus } from "@/types";
import { KYC_STATUS_CONFIG } from "@/constants";

type TabFilterType = "ALL" | KycStatus;

export default function DriversKycListPage() {
  const [drivers, setDrivers] = useState<DriverSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedTab, setSelectedTab] = useState<TabFilterType>("PENDING");
  const [searchQuery, setSearchQuery] = useState("");
  const [error, setError] = useState<string | null>(null);

  const fetchDrivers = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiClient.get<ApiResponse<DriverSummary[]>>("/admin/drivers");
      if (response.data.success && response.data.data) {
        setDrivers(response.data.data);
      } else {
        setDrivers([]);
      }
    } catch (err: any) {
      console.error("Lỗi tải danh sách tài xế:", err);
      try {
        const fallbackRes = await apiClient.get<ApiResponse<DriverSummary[]>>("/admin/drivers/kyc/pending");
        if (fallbackRes.data.success && fallbackRes.data.data) {
          setDrivers(fallbackRes.data.data);
        }
      } catch (fallbackErr) {
        setError("Không thể tải danh sách tài xế. Vui lòng kiểm tra lại kết nối backend.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDrivers();
  }, []);

  const stats = useMemo(() => {
    const pending = drivers.filter((d) => d.kycStatus === "PENDING").length;
    const approved = drivers.filter((d) => d.kycStatus === "APPROVED").length;
    const rejected = drivers.filter((d) => d.kycStatus === "REJECTED").length;
    return { pending, approved, rejected, total: drivers.length };
  }, [drivers]);

  const filteredDrivers = useMemo(() => {
    return drivers.filter((driver) => {
      const matchStatus =
        selectedTab === "ALL" ? true : driver.kycStatus === selectedTab;

      const q = searchQuery.toLowerCase().trim();
      const matchSearch =
        !q ||
        driver.fullName.toLowerCase().includes(q) ||
        driver.phoneNumber.includes(q) ||
        driver.citizenId.toLowerCase().includes(q) ||
        driver.licensePlate.toLowerCase().includes(q) ||
        driver.vehicleModel.toLowerCase().includes(q);

      return matchStatus && matchSearch;
    });
  }, [drivers, selectedTab, searchQuery]);

  const filterTabs: { key: TabFilterType; label: string; count: number; activeClass: string }[] = [
    {
      key: "PENDING",
      label: "Chờ duyệt",
      count: stats.pending,
      activeClass: "bg-amber-500/20 text-amber-300 border-amber-500/30",
    },
    {
      key: "APPROVED",
      label: "Đã duyệt",
      count: stats.approved,
      activeClass: "bg-emerald-500/20 text-emerald-300 border-emerald-500/30",
    },
    {
      key: "REJECTED",
      label: "Từ chối",
      count: stats.rejected,
      activeClass: "bg-rose-500/20 text-rose-300 border-rose-500/30",
    },
    {
      key: "ALL",
      label: "Tất cả",
      count: stats.total,
      activeClass: "bg-slate-800 text-slate-100 border-slate-700",
    },
  ];

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <Header
        title="Quản lý & Xét duyệt KYC Tài xế"
        description="Xác thực hồ sơ đăng ký tài xế, giấy tờ phương tiện xe điện và kiểm tra nhận diện khuôn mặt sinh trắc học"
        actions={
          <button
            type="button"
            onClick={fetchDrivers}
            disabled={isLoading}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 text-xs font-medium transition"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? "animate-spin text-emerald-400" : ""}`} />
            <span>Làm mới</span>
          </button>
        }
      />

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          title="Hồ sơ chờ duyệt"
          value={stats.pending}
          subtitle={KYC_STATUS_CONFIG.PENDING.description}
          icon={Clock}
          iconColor="text-amber-400"
          iconBgColor="bg-amber-500/10"
          isActive={selectedTab === "PENDING"}
          activeBgClass={KYC_STATUS_CONFIG.PENDING.bgHoverClass}
          activeBorderClass={KYC_STATUS_CONFIG.PENDING.borderActiveClass}
          onClick={() => setSelectedTab("PENDING")}
        />

        <KpiCard
          title="Tài xế đã duyệt"
          value={stats.approved}
          subtitle={KYC_STATUS_CONFIG.APPROVED.description}
          icon={CheckCircle2}
          iconColor="text-emerald-400"
          iconBgColor="bg-emerald-500/10"
          isActive={selectedTab === "APPROVED"}
          activeBgClass={KYC_STATUS_CONFIG.APPROVED.bgHoverClass}
          activeBorderClass={KYC_STATUS_CONFIG.APPROVED.borderActiveClass}
          onClick={() => setSelectedTab("APPROVED")}
        />

        <KpiCard
          title="Hồ sơ từ chối"
          value={stats.rejected}
          subtitle={KYC_STATUS_CONFIG.REJECTED.description}
          icon={XCircle}
          iconColor="text-rose-400"
          iconBgColor="bg-rose-500/10"
          isActive={selectedTab === "REJECTED"}
          activeBgClass={KYC_STATUS_CONFIG.REJECTED.bgHoverClass}
          activeBorderClass={KYC_STATUS_CONFIG.REJECTED.borderActiveClass}
          onClick={() => setSelectedTab("REJECTED")}
        />

        <KpiCard
          title="Tổng số hồ sơ"
          value={stats.total}
          subtitle="Toàn bộ hồ sơ trên hệ thống"
          icon={Users}
          iconColor="text-cyan-400"
          iconBgColor="bg-cyan-500/10"
          isActive={selectedTab === "ALL"}
          activeBgClass="bg-slate-800"
          activeBorderClass="border-slate-600 shadow-lg"
          onClick={() => setSelectedTab("ALL")}
        />
      </div>

      {/* Filter Tabs & Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 rounded-2xl bg-slate-900/80 border border-slate-800">
        <div className="flex items-center gap-1.5 p-1 bg-slate-950 rounded-xl border border-slate-800/80 self-stretch sm:self-auto overflow-x-auto">
          {filterTabs.map((tab) => {
            const isActive = selectedTab === tab.key;
            return (
              <button
                key={tab.key}
                type="button"
                onClick={() => setSelectedTab(tab.key)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition whitespace-nowrap ${isActive
                    ? `${tab.activeClass} border shadow-sm`
                    : "text-slate-400 hover:text-slate-200"
                  }`}
              >
                {tab.label} ({tab.count})
              </button>
            );
          })}
        </div>

        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Tìm theo tên, SĐT, biển số..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-emerald-500 transition"
          />
        </div>
      </div>

      {/* Driver Data Table */}
      <DriverTable
        drivers={filteredDrivers}
        isLoading={isLoading}
        error={error}
        searchQuery={searchQuery}
        onRetry={fetchDrivers}
        onClearFilter={() => setSelectedTab("ALL")}
      />
    </div>
  );
}
