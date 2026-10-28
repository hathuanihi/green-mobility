"use client";

import React, { useState, useEffect } from "react";
import {
  ArrowDownCircle,
  Search,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Clock,
  AlertCircle,
  Check,
  X,
  Building,
  UserCheck,
} from "lucide-react";
import Header from "@/components/layout/Header";
import apiClient from "@/lib/api";
import { ApiResponse, WithdrawalItem } from "@/types";

export default function WithdrawalsPage() {
  const [withdrawals, setWithdrawals] = useState<WithdrawalItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [searchQuery, setSearchQuery] = useState("");

  // Approve Modal
  const [approvingItem, setApprovingItem] = useState<WithdrawalItem | null>(null);
  const [bankRef, setBankRef] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Reject Modal
  const [rejectingItem, setRejectingItem] = useState<WithdrawalItem | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  const fetchWithdrawals = async () => {
    setIsLoading(true);
    try {
      const params = new URLSearchParams();
      if (statusFilter !== "ALL") params.append("status", statusFilter);
      params.append("page", "0");
      params.append("size", "50");

      const res = await apiClient.get<ApiResponse<{ content: WithdrawalItem[]; totalElements: number }>>(
        `/admin/withdrawals?${params.toString()}`
      );
      if (res.data.success && res.data.data?.content) {
        setWithdrawals(res.data.data.content);
      }
    } catch (err) {
      console.error("Lỗi khi tải danh sách rút tiền:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchWithdrawals();
  }, [statusFilter]);

  const handleApprove = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!approvingItem) return;
    setIsSubmitting(true);
    try {
      await apiClient.post(`/admin/withdrawals/${approvingItem.id}/approve`, {
        bankReference: bankRef || "MANUAL_TRANSFER",
      });
      setApprovingItem(null);
      setBankRef("");
      await fetchWithdrawals();
      alert("Đã phê duyệt yêu cầu rút tiền thành công!");
    } catch (err: any) {
      alert("Lỗi khi duyệt rút tiền: " + (err.response?.data?.message || err.message));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectingItem) return;
    if (!rejectReason.trim()) {
      alert("Vui lòng nhập lý do từ chối yêu cầu.");
      return;
    }
    setIsSubmitting(true);
    try {
      await apiClient.post(`/admin/withdrawals/${rejectingItem.id}/reject`, {
        reason: rejectReason,
      });
      setRejectingItem(null);
      setRejectReason("");
      await fetchWithdrawals();
      alert("Đã từ chối và hoàn tiền giữ lại về ví tài xế!");
    } catch (err: any) {
      alert("Lỗi khi từ chối rút tiền: " + (err.response?.data?.message || err.message));
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatVnd = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + " đ";
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "APPROVED_PAID":
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">Đã chi trả</span>;
      case "PENDING":
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-400 border border-amber-500/30">Chờ duyệt</span>;
      case "REJECTED":
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-red-500/20 text-red-400 border border-red-500/30">Đã từ chối</span>;
      case "CANCELLED":
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-700 text-slate-300">Tài xế hủy</span>;
      default:
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-700 text-slate-300">{status}</span>;
    }
  };

  const filtered = withdrawals.filter((w) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      w.accountHolderName.toLowerCase().includes(q) ||
      w.bankCode.toLowerCase().includes(q) ||
      w.driverUserId.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-8">
      <Header
        title="Duyệt Yêu Cầu Rút Tiền Tài Xế"
        description="Kiểm soát, đối soát tài khoản ngân hàng và giải ngân thu nhập định kỳ cho đối tác tài xế"
      />

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row gap-4 justify-between items-center bg-slate-900/40 p-4 rounded-2xl border border-slate-800">
        <div className="flex flex-wrap gap-3 items-center w-full md:w-auto">
          <div className="relative flex-1 md:w-80">
            <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
            <input
              type="text"
              placeholder="Tìm theo Tên chủ tài khoản / Ngân hàng..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-emerald-500/50"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-200 px-3 py-2 focus:outline-none focus:border-emerald-500/50"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="PENDING">Chờ phê duyệt (PENDING)</option>
            <option value="APPROVED_PAID">Đã giải ngân (APPROVED_PAID)</option>
            <option value="REJECTED">Đã từ chối (REJECTED)</option>
          </select>
        </div>

        <button
          onClick={fetchWithdrawals}
          className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-sm font-medium transition"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin" : ""}`} />
          Làm mới
        </button>
      </div>

      {/* Withdrawals Table */}
      <div className="bg-slate-900/60 rounded-2xl border border-slate-800 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-950/70 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-6 py-4">Tài Xế</th>
                <th className="px-6 py-4">Số Tiền Rút</th>
                <th className="px-6 py-4">Ngân Hàng</th>
                <th className="px-6 py-4">Số Tài Khoản</th>
                <th className="px-6 py-4">Chủ Tài Khoản</th>
                <th className="px-6 py-4">Trạng Thái</th>
                <th className="px-6 py-4">Ngày Yêu Cầu</th>
                <th className="px-6 py-4 text-right">Hành Động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-slate-500">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <div className="w-6 h-6 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
                      <span>Đang tải danh sách rút tiền...</span>
                    </div>
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-slate-500">
                    Không có yêu cầu rút tiền nào cần xử lý.
                  </td>
                </tr>
              ) : (
                filtered.map((w) => (
                  <tr key={w.id} className="hover:bg-slate-800/30 transition">
                    <td className="px-6 py-4 font-mono text-xs text-slate-400">
                      {w.driverUserId.substring(0, 8)}...
                    </td>
                    <td className="px-6 py-4 font-bold text-slate-100">
                      {formatVnd(w.amountVnd)}
                    </td>
                    <td className="px-6 py-4 font-semibold text-emerald-400">
                      {w.bankCode}
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-300">
                      •••• {w.bankAccountLast4}
                    </td>
                    <td className="px-6 py-4 font-medium text-slate-200 uppercase">
                      {w.accountHolderName}
                    </td>
                    <td className="px-6 py-4">{getStatusBadge(w.status)}</td>
                    <td className="px-6 py-4 text-xs text-slate-400">
                      {new Date(w.createdAt).toLocaleString("vi-VN")}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {w.status === "PENDING" ? (
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => setApprovingItem(w)}
                            className="inline-flex items-center gap-1 px-3 py-1.5 bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-400 border border-emerald-500/30 rounded-lg text-xs font-semibold transition"
                          >
                            <Check className="w-3.5 h-3.5" />
                            Duyệt
                          </button>
                          <button
                            onClick={() => setRejectingItem(w)}
                            className="inline-flex items-center gap-1 px-3 py-1.5 bg-red-500/20 hover:bg-red-500/30 text-red-400 border border-red-500/30 rounded-lg text-xs font-semibold transition"
                          >
                            <X className="w-3.5 h-3.5" />
                            Từ chối
                          </button>
                        </div>
                      ) : (
                        <span className="text-xs text-slate-500 italic">Đã xử lý</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Approve Modal */}
      {approvingItem && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-400" />
              Xác nhận Phê duyệt Rút tiền
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Xác nhận bạn đã chuyển khoản số tiền{" "}
              <strong className="text-emerald-400">{formatVnd(approvingItem.amountVnd)}</strong> tới tài khoản{" "}
              <strong>{approvingItem.bankCode}</strong> (•••• {approvingItem.bankAccountLast4} -{" "}
              {approvingItem.accountHolderName}).
            </p>

            <form onSubmit={handleApprove} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Mã tham chiếu ngân hàng (Bank Reference Code)
                </label>
                <input
                  type="text"
                  placeholder="Ví dụ: FT2630099818..."
                  value={bankRef}
                  onChange={(e) => setBankRef(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 focus:outline-none focus:border-emerald-500/50"
                />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setApprovingItem(null)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 disabled:opacity-50"
                >
                  {isSubmitting ? "Đang xử lý..." : "Xác nhận Đã Chuyển Tiền"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reject Modal */}
      {rejectingItem && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
              <XCircle className="w-5 h-5 text-red-400" />
              Từ chối Yêu cầu Rút tiền
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Số tiền giữ lại sẽ được tự động hoàn về ví thu nhập của tài xế. Vui lòng nêu rõ lý do để tài xế được biết.
            </p>

            <form onSubmit={handleReject} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Lý do từ chối *
                </label>
                <textarea
                  rows={3}
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="Ví dụ: Số tài khoản và tên chủ thẻ không trùng khớp..."
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 focus:outline-none focus:border-red-500/50"
                  required
                />
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setRejectingItem(null)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 disabled:opacity-50"
                >
                  {isSubmitting ? "Đang xử lý..." : "Xác nhận Từ chối"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

