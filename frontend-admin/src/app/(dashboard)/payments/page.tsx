"use client";

import React, { useState, useEffect } from "react";
import {
  CreditCard,
  Search,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Clock,
  ArrowUpRight,
  ShieldAlert,
  RotateCcw,
  Eye,
  X,
  FileText,
  DollarSign,
  TrendingUp,
} from "lucide-react";
import Header from "@/components/layout/Header";
import KpiCard from "@/components/ui/KpiCard";
import apiClient from "@/lib/api";
import { ApiResponse, PaymentItem, PaymentDetailData } from "@/types";

export default function PaymentsPage() {
  const [payments, setPayments] = useState<PaymentItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [methodFilter, setMethodFilter] = useState("ALL");

  // Detail Modal state
  const [selectedPaymentId, setSelectedPaymentId] = useState<string | null>(null);
  const [detailData, setDetailData] = useState<PaymentDetailData | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);

  // Refund Modal state
  const [showRefundModal, setShowRefundModal] = useState(false);
  const [refundAmount, setRefundAmount] = useState<number>(0);
  const [refundReason, setRefundReason] = useState("");
  const [refundCommission, setRefundCommission] = useState(true);
  const [isRefunding, setIsRefunding] = useState(false);
  const [refundError, setRefundError] = useState<string | null>(null);

  // KPIs
  const [totalGmv, setTotalGmv] = useState(0);
  const [totalCommission, setTotalCommission] = useState(0);
  const [paidCount, setPaidCount] = useState(0);

  const fetchPayments = async () => {
    setIsLoading(true);
    try {
      const params = new URLSearchParams();
      if (statusFilter !== "ALL") params.append("status", statusFilter);
      if (methodFilter !== "ALL") params.append("method", methodFilter);
      params.append("page", "0");
      params.append("size", "50");

      const res = await apiClient.get<ApiResponse<{ content: PaymentItem[]; totalElements: number }>>(
        `/admin/payments?${params.toString()}`
      );
      if (res.data.success && res.data.data?.content) {
        setPayments(res.data.data.content);

        // Calculate summary
        const paid = res.data.data.content.filter((p) => p.status === "PAID");
        setPaidCount(paid.length);
        setTotalGmv(paid.reduce((acc, p) => acc + (p.amountVnd || 0), 0));
        setTotalCommission(paid.reduce((acc, p) => acc + (p.commissionVnd || 0), 0));
      }
    } catch (err) {
      console.error("Lỗi khi tải danh sách thanh toán:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchPayments();
  }, [statusFilter, methodFilter]);

  const openDetail = async (id: string) => {
    setSelectedPaymentId(id);
    setIsDetailLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<PaymentDetailData>>(`/admin/payments/${id}`);
      if (res.data.success && res.data.data) {
        setDetailData(res.data.data);
      }
    } catch (err) {
      console.error("Lỗi khi tải chi tiết giao dịch:", err);
    } finally {
      setIsDetailLoading(false);
    }
  };

  const handleRequery = async () => {
    if (!selectedPaymentId) return;
    try {
      await apiClient.post(`/admin/payments/${selectedPaymentId}/requery`);
      await openDetail(selectedPaymentId);
      await fetchPayments();
      alert("Đã hoàn tất kiểm tra trạng thái từ cổng thanh toán!");
    } catch (err: any) {
      alert("Lỗi requery: " + (err.response?.data?.message || err.message));
    }
  };

  const handleRefundSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPaymentId) return;
    if (refundReason.trim().length < 10) {
      setRefundError("Lý do hoàn tiền phải có ít nhất 10 ký tự.");
      return;
    }
    if (refundAmount <= 0) {
      setRefundError("Số tiền hoàn phải lớn hơn 0.");
      return;
    }

    setIsRefunding(true);
    setRefundError(null);
    try {
      await apiClient.post(`/admin/payments/${selectedPaymentId}/refund`, {
        amountVnd: refundAmount,
        reason: refundReason,
        refundCommission: refundCommission,
      });
      setShowRefundModal(false);
      setRefundReason("");
      await openDetail(selectedPaymentId);
      await fetchPayments();
      alert("Xử lý hoàn tiền thành công!");
    } catch (err: any) {
      setRefundError(err.response?.data?.message || "Lỗi xử lý hoàn tiền");
    } finally {
      setIsRefunding(false);
    }
  };

  const formatVnd = (val: number) => {
    return new Intl.NumberFormat("vi-VN").format(val) + " đ";
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "PAID":
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">Đã thanh toán</span>;
      case "PENDING":
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-400 border border-amber-500/30">Chờ thanh toán</span>;
      case "FAILED":
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-red-500/20 text-red-400 border border-red-500/30">Thất bại</span>;
      case "REFUNDED":
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-purple-500/20 text-purple-400 border border-purple-500/30">Đã hoàn tiền</span>;
      case "PARTIALLY_REFUNDED":
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">Hoàn một phần</span>;
      default:
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-700 text-slate-300">{status}</span>;
    }
  };

  const filteredPayments = payments.filter((p) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      p.paymentId.toLowerCase().includes(q) ||
      p.tripId.toLowerCase().includes(q) ||
      p.method.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-8">
      <Header
        title="Quản lý Giao dịch & Thanh toán"
        description="Theo dõi toàn bộ luồng tiền cuốc xe, Ví Xanh, đối soát cổng thanh toán và xử lý hoàn tiền"
      />

      {/* KPI Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <KpiCard
          title="Tổng GMV Đã Thu"
          value={formatVnd(totalGmv)}
          subtitle="Tổng cước hoàn tất"
          icon={DollarSign}
        />
        <KpiCard
          title="Doanh thu Hoa hồng"
          value={formatVnd(totalCommission)}
          subtitle="Thu nhập nền tảng (20%)"
          icon={TrendingUp}
        />
        <KpiCard
          title="Số Giao dịch Thành công"
          value={paidCount.toString()}
          subtitle={`Trên tổng ${payments.length} lượt`}
          icon={CheckCircle2}
        />
        <KpiCard
          title="Tỷ lệ Thanh toán"
          value={payments.length > 0 ? ((paidCount / payments.length) * 100).toFixed(1) + "%" : "100%"}
          subtitle="Tỷ lệ thanh toán thành công"
          icon={CreditCard}
        />
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row gap-4 justify-between items-center bg-slate-900/40 p-4 rounded-2xl border border-slate-800">
        <div className="flex flex-wrap gap-3 items-center w-full md:w-auto">
          <div className="relative flex-1 md:w-80">
            <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
            <input
              type="text"
              placeholder="Tìm theo Mã thanh toán / Mã chuyến..."
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
            <option value="PAID">Đã thanh toán (PAID)</option>
            <option value="PENDING">Chờ thanh toán (PENDING)</option>
            <option value="FAILED">Thất bại (FAILED)</option>
            <option value="REFUNDED">Đã hoàn tiền (REFUNDED)</option>
            <option value="PARTIALLY_REFUNDED">Hoàn 1 phần (PARTIALLY_REFUNDED)</option>
          </select>

          <select
            value={methodFilter}
            onChange={(e) => setMethodFilter(e.target.value)}
            className="bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-200 px-3 py-2 focus:outline-none focus:border-emerald-500/50"
          >
            <option value="ALL">Tất cả phương thức</option>
            <option value="GREEN_WALLET">Ví Xanh (GREEN_WALLET)</option>
            <option value="CASH">Tiền mặt (CASH)</option>
            <option value="VNPAY">VNPay Sandbox</option>
            <option value="MOMO">MoMo Sandbox</option>
          </select>
        </div>

        <button
          onClick={fetchPayments}
          className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-sm font-medium transition"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin" : ""}`} />
          Làm mới
        </button>
      </div>

      {/* Payments Table */}
      <div className="bg-slate-900/60 rounded-2xl border border-slate-800 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-950/70 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-6 py-4">Mã Giao Dịch</th>
                <th className="px-6 py-4">Phương Thức</th>
                <th className="px-6 py-4">Cước Chuyến Đi</th>
                <th className="px-6 py-4">Hoa Hồng (20%)</th>
                <th className="px-6 py-4">Tài Xế Nhận</th>
                <th className="px-6 py-4">Trạng Thái</th>
                <th className="px-6 py-4">Thời Gian</th>
                <th className="px-6 py-4 text-right">Thao Tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-slate-500">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <div className="w-6 h-6 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
                      <span>Đang tải dữ liệu giao dịch...</span>
                    </div>
                  </td>
                </tr>
              ) : filteredPayments.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-slate-500">
                    Không tìm thấy giao dịch nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                filteredPayments.map((p) => (
                  <tr key={p.paymentId} className="hover:bg-slate-800/30 transition">
                    <td className="px-6 py-4 font-mono text-xs">
                      <div className="text-slate-200 font-semibold">{p.paymentId.substring(0, 8)}...</div>
                      <div className="text-slate-500">Trip: {p.tripId.substring(0, 8)}...</div>
                    </td>
                    <td className="px-6 py-4 font-medium">
                      <span className="inline-flex items-center gap-1.5 text-xs text-slate-300">
                        {p.method}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-100">
                      {formatVnd(p.amountVnd)}
                    </td>
                    <td className="px-6 py-4 text-emerald-400">
                      +{formatVnd(p.commissionVnd || 0)}
                    </td>
                    <td className="px-6 py-4 text-sky-400">
                      {formatVnd(p.driverAmountVnd || 0)}
                    </td>
                    <td className="px-6 py-4">{getStatusBadge(p.status)}</td>
                    <td className="px-6 py-4 text-xs text-slate-400">
                      {new Date(p.createdAt).toLocaleString("vi-VN")}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => openDetail(p.paymentId)}
                        className="inline-flex items-center gap-1 px-3 py-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/20 rounded-lg text-xs font-medium transition"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        Chi tiết
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Payment Detail Modal */}
      {selectedPaymentId && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto shadow-2xl">
            <div className="p-6 border-b border-slate-800 flex justify-between items-center sticky top-0 bg-slate-900/90 backdrop-blur-md z-10">
              <div>
                <h3 className="text-lg font-bold text-white flex items-center gap-2">
                  <CreditCard className="w-5 h-5 text-emerald-400" />
                  Chi tiết Giao dịch: {selectedPaymentId}
                </h3>
                <p className="text-xs text-slate-400 mt-1">Truy vết bút toán sổ cái kép & nhật ký callback cổng</p>
              </div>
              <button
                onClick={() => setSelectedPaymentId(null)}
                className="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {isDetailLoading || !detailData ? (
              <div className="py-20 text-center text-slate-500">
                <div className="w-8 h-8 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
                Đang tải chi tiết bút toán...
              </div>
            ) : (
              <div className="p-6 space-y-6">
                {/* Status & Actions Header */}
                <div className="flex flex-wrap items-center justify-between gap-4 bg-slate-950 p-4 rounded-xl border border-slate-800">
                  <div className="flex items-center gap-3">
                    {getStatusBadge(detailData.payment.status)}
                    <span className="text-xs text-slate-400">
                      Phương thức: <strong className="text-slate-200">{detailData.payment.method}</strong>
                    </span>
                    <span className="text-xs text-slate-400">
                      Số tiền: <strong className="text-slate-200">{formatVnd(detailData.payment.amountVnd)}</strong>
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    {["VNPAY", "MOMO"].includes(detailData.payment.method) && (
                      <button
                        onClick={handleRequery}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-lg flex items-center gap-1.5"
                      >
                        <RefreshCw className="w-3.5 h-3.5" />
                        Requery Cổng
                      </button>
                    )}

                    {["PAID", "PARTIALLY_REFUNDED"].includes(detailData.payment.status) && (
                      <button
                        onClick={() => {
                          setRefundAmount(detailData.payment.amountVnd);
                          setShowRefundModal(true);
                        }}
                        className="px-3 py-1.5 bg-red-500/20 hover:bg-red-500/30 text-red-400 border border-red-500/30 text-xs font-semibold rounded-lg flex items-center gap-1.5"
                      >
                        <RotateCcw className="w-3.5 h-3.5" />
                        Hoàn Tiền
                      </button>
                    )}
                  </div>
                </div>

                {/* Double-Entry Ledger Entries */}
                <div>
                  <h4 className="text-sm font-bold text-slate-200 mb-3 flex items-center gap-2">
                    <FileText className="w-4 h-4 text-emerald-400" />
                    Bút toán Sổ cái kép (Double-Entry Ledger)
                  </h4>
                  {detailData.ledgerEntries.length === 0 ? (
                    <div className="text-xs text-slate-500 bg-slate-950 p-4 rounded-xl border border-slate-800">
                      Chưa có bút toán sổ cái cho giao dịch này (có thể giao dịch chưa hoàn tất hoặc cước 0đ).
                    </div>
                  ) : (
                    <div className="bg-slate-950 rounded-xl border border-slate-800 overflow-hidden">
                      <table className="w-full text-left text-xs text-slate-300">
                        <thead className="bg-slate-900/80 text-slate-400 border-b border-slate-800 uppercase">
                          <tr>
                            <th className="px-4 py-3">Loại Bút toán</th>
                            <th className="px-4 py-3">Tài khoản</th>
                            <th className="px-4 py-3">Mã Tài khoản</th>
                            <th className="px-4 py-3 text-right">Số tiền</th>
                            <th className="px-4 py-3 text-right">Số dư sau GD</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800/60 font-mono">
                          {detailData.ledgerEntries.map((e) => (
                            <tr key={e.entryId}>
                              <td className="px-4 py-2.5">
                                <span
                                  className={`px-2 py-0.5 rounded font-bold text-[10px] ${
                                    e.entryType === "DEBIT"
                                      ? "bg-amber-500/20 text-amber-400"
                                      : "bg-emerald-500/20 text-emerald-400"
                                  }`}
                                >
                                  {e.entryType}
                                </span>
                              </td>
                              <td className="px-4 py-2.5 text-slate-300">{e.accountType}</td>
                              <td className="px-4 py-2.5 text-slate-400">{e.accountNumber}</td>
                              <td className="px-4 py-2.5 text-right font-semibold text-slate-100">
                                {formatVnd(e.amountVnd)}
                              </td>
                              <td className="px-4 py-2.5 text-right text-slate-400">
                                {formatVnd(e.balanceAfterVnd)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>

                {/* Raw Webhook Logs */}
                <div>
                  <h4 className="text-sm font-bold text-slate-200 mb-3 flex items-center gap-2">
                    <ShieldAlert className="w-4 h-4 text-sky-400" />
                    Nhật ký Callback Cổng (IPN Webhook Events)
                  </h4>
                  {detailData.webhookEvents.length === 0 ? (
                    <div className="text-xs text-slate-500 bg-slate-950 p-4 rounded-xl border border-slate-800">
                      Chưa ghi nhận sự kiện IPN nào từ cổng thanh toán.
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {detailData.webhookEvents.map((w) => (
                        <div
                          key={w.eventId}
                          className="bg-slate-950 p-3 rounded-xl border border-slate-800 flex justify-between items-center text-xs"
                        >
                          <div>
                            <span className="font-bold text-slate-200 mr-2">[{w.gateway}]</span>
                            <span className="text-slate-400">Ref: {w.gatewayTxnRef || "N/A"}</span>
                            <span className="text-slate-500 ml-2">IP: {w.sourceIp}</span>
                          </div>
                          <div className="flex items-center gap-3">
                            <span
                              className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                                w.signatureValid
                                  ? "bg-emerald-500/20 text-emerald-400"
                                  : "bg-red-500/20 text-red-400"
                              }`}
                            >
                              {w.signatureValid ? "Chữ ký hợp lệ" : "Sai chữ ký"}
                            </span>
                            <span className="text-slate-400">{w.processingResult}</span>
                            <span className="text-slate-500">
                              {new Date(w.receivedAt).toLocaleTimeString("vi-VN")}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Refund Submodal */}
      {showRefundModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
              <RotateCcw className="w-5 h-5 text-red-400" />
              Xác nhận Hoàn tiền
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Thực hiện giao dịch bù trừ (reversal) hoàn trả tiền cho khách hàng và điều chỉnh doanh thu/thu nhập tài xế.
            </p>

            <form onSubmit={handleRefundSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">Số tiền hoàn (VNĐ)</label>
                <input
                  type="number"
                  value={refundAmount}
                  onChange={(e) => setRefundAmount(Number(e.target.value))}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 focus:outline-none focus:border-red-500/50"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1.5">
                  Lý do hoàn tiền (Tối thiểu 10 ký tự) *
                </label>
                <textarea
                  rows={3}
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  placeholder="Nhập chi tiết lý do hoàn tiền cho khách hàng..."
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 focus:outline-none focus:border-red-500/50"
                  required
                />
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="refundComm"
                  checked={refundCommission}
                  onChange={(e) => setRefundCommission(e.target.checked)}
                  className="rounded bg-slate-950 border-slate-800 text-emerald-500 focus:ring-0"
                />
                <label htmlFor="refundComm" className="text-xs text-slate-300">
                  Hoàn trả lại phí hoa hồng tương ứng cho tài xế
                </label>
              </div>

              {refundError && (
                <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-xs text-red-400 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  {refundError}
                </div>
              )}

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowRefundModal(false)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isRefunding}
                  className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 disabled:opacity-50"
                >
                  {isRefunding ? "Đang xử lý..." : "Xác nhận Hoàn tiền"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
