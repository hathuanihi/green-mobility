"use client";

import React, { useEffect, useState } from "react";
import {
  BookOpenCheck,
  ShieldAlert,
  RefreshCw,
  RotateCcw,
  CheckCircle2,
  XCircle,
  Lock,
  Send,
  Eye,
  AlertTriangle,
  Power,
  X,
} from "lucide-react";
import Header from "@/components/layout/Header";
import apiClient from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useToast } from "@/components/Toast";
import {
  ApiResponse,
  LedgerEntryViewItem,
  LedgerTransactionItem,
  MultiCurrencyAuditReport,
  OutboxFailedItem,
  TripAwardItem,
} from "@/types";

export default function LedgerAuditPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const isReadOnly = user?.role === "ROLE_OPERATOR";

  const [tab, setTab] = useState<"TRANSACTIONS" | "AWARDS" | "OUTBOX">("TRANSACTIONS");
  const [loading, setLoading] = useState(true);

  const [audit, setAudit] = useState<MultiCurrencyAuditReport | null>(null);
  const [txs, setTxs] = useState<LedgerTransactionItem[]>([]);
  const [currencyFilter, setCurrencyFilter] = useState("ALL");
  const [txCodeSearch, setTxCodeSearch] = useState("");

  const [awards, setAwards] = useState<TripAwardItem[]>([]);
  const [awardStatus, setAwardStatus] = useState("HELD_FOR_FRAUD");
  const [outboxFailed, setOutboxFailed] = useState<OutboxFailedItem[]>([]);

  // Detail & Reversal Modals
  const [detailTx, setDetailTx] = useState<{
    transaction: LedgerTransactionItem;
    entries: LedgerEntryViewItem[];
  } | null>(null);
  const [reverseTarget, setReverseTarget] = useState<LedgerTransactionItem | null>(null);
  const [reverseReason, setReverseReason] = useState("");
  const [reversing, setReversing] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const ccyParam = currencyFilter !== "ALL" ? `&currency=${currencyFilter}` : "";
      const codeParam = txCodeSearch.trim() ? `&txCode=${encodeURIComponent(txCodeSearch.trim())}` : "";
      const awardParam = awardStatus !== "ALL" ? `&status=${awardStatus}` : "";

      const [auditRes, txRes, awardRes, outboxRes] = await Promise.all([
        apiClient.get<ApiResponse<MultiCurrencyAuditReport>>("/admin/ledger/audit"),
        apiClient.get<ApiResponse<{ content: LedgerTransactionItem[] }>>(
          `/admin/ledger/transactions?size=30${ccyParam}${codeParam}`
        ),
        apiClient.get<ApiResponse<{ content: TripAwardItem[] }>>(
          `/admin/incentive/awards?size=30${awardParam}`
        ),
        apiClient.get<ApiResponse<{ content: OutboxFailedItem[] }>>("/admin/outbox/failed?size=30"),
      ]);

      if (auditRes.data.success) setAudit(auditRes.data.data);
      if (txRes.data.success && txRes.data.data?.content) setTxs(txRes.data.data.content);
      if (awardRes.data.success && awardRes.data.data?.content) setAwards(awardRes.data.data.content);
      if (outboxRes.data.success && outboxRes.data.data?.content) setOutboxFailed(outboxRes.data.data.content);
    } catch (err: any) {
      showToast("Không thể tải dữ liệu kiểm toán: " + (err.response?.data?.message || err.message), "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, [currencyFilter, awardStatus]);

  const handleToggleCircuitBreaker = async () => {
    if (isReadOnly || !audit) return;
    const nextFrozen = !audit.redemptionsFrozen;
    try {
      await apiClient.post("/admin/ledger/circuit-breaker", {
        frozen: nextFrozen,
        reason: nextFrozen ? "Admin bật khóa khẩn cấp đổi thưởng" : "Admin mở lại đổi thưởng",
      });
      showToast(
        nextFrozen ? "Đã BẬT Circuit-Breaker khóa đổi thưởng!" : "Đã TẮT Circuit-Breaker!",
        nextFrozen ? "warning" : "success"
      );
      await fetchAll();
    } catch (err: any) {
      showToast("Lỗi Circuit-Breaker: " + (err.response?.data?.message || err.message), "error");
    }
  };

  const openTxDetail = async (id: string) => {
    try {
      const res = await apiClient.get<
        ApiResponse<{ transaction: LedgerTransactionItem; entries: LedgerEntryViewItem[] }>
      >(`/admin/ledger/transactions/${id}`);
      if (res.data.success && res.data.data) {
        setDetailTx(res.data.data);
      }
    } catch (err: any) {
      showToast("Lỗi tải chi tiết bút toán: " + (err.response?.data?.message || err.message), "error");
    }
  };

  const handleReverseTx = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isReadOnly || !reverseTarget) return;
    if (reverseReason.trim().length < 10) {
      showToast("Lý do đảo giao dịch bắt buộc tối thiểu 10 ký tự!", "warning");
      return;
    }
    setReversing(true);
    try {
      await apiClient.post(
        `/admin/ledger/transactions/${reverseTarget.id}/reverse`,
        { reason: reverseReason.trim() },
        { headers: { "Idempotency-Key": crypto.randomUUID() } }
      );
      showToast("Đã đảo giao dịch sổ cái nguyên tử (ADMIN_REVERSAL)!", "success");
      setReverseTarget(null);
      setReverseReason("");
      await fetchAll();
    } catch (err: any) {
      showToast("Không thể đảo giao dịch: " + (err.response?.data?.message || err.message), "error");
    } finally {
      setReversing(false);
    }
  };

  const handleAwardDecision = async (awardId: string, action: "approve" | "void") => {
    if (isReadOnly) return;
    const note = action === "void" ? "Vi phạm gian lận hành trình - Admin từ chối giải ngân" : "Admin xác nhận hợp lệ";
    try {
      await apiClient.post(`/admin/incentive/awards/${awardId}/${action}`, { note });
      showToast(
        action === "approve" ? "Đã duyệt giải ngân PCC & Điểm Xanh!" : "Đã hủy vĩnh viễn thưởng vi phạm!",
        "success"
      );
      await fetchAll();
    } catch (err: any) {
      showToast("Lỗi xử lý thưởng: " + (err.response?.data?.message || err.message), "error");
    }
  };

  const handleRedriveOne = async (id: string) => {
    if (isReadOnly) return;
    try {
      await apiClient.post(`/admin/outbox/${id}/redrive`);
      showToast("Đã đưa sự kiện Outbox vào hàng đợi gửi lại!", "success");
      await fetchAll();
    } catch (err: any) {
      showToast("Lỗi re-drive Outbox: " + (err.response?.data?.message || err.message), "error");
    }
  };

  const handleRedriveAll = async () => {
    if (isReadOnly) return;
    try {
      await apiClient.post("/admin/outbox/redrive-all");
      showToast("Đã re-drive toàn bộ bản ghi Outbox FAILED!", "success");
      await fetchAll();
    } catch (err: any) {
      showToast("Lỗi re-drive toàn bộ: " + (err.response?.data?.message || err.message), "error");
    }
  };

  const currencies = ["VND", "PCC", "ECO_POINT"];

  return (
    <div className="space-y-8">
      <Header
        title="Kiểm toán Sổ cái Đa Tiền tệ & Điều phối"
        description="Giám sát bất biến Δ = 0 (VND / PCC / ECO_POINT), Circuit-Breaker, HELD_FOR_FRAUD và RabbitMQ Outbox"
        actions={
          <div className="flex items-center gap-3">
            {isReadOnly && (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-semibold">
                <Lock className="w-3.5 h-3.5" /> Chỉ xem (Operator)
              </span>
            )}
            {!isReadOnly && audit && (
              <button
                onClick={handleToggleCircuitBreaker}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-semibold border transition ${
                  audit.redemptionsFrozen
                    ? "bg-red-500/20 border-red-500/40 text-red-300 hover:bg-red-500/30"
                    : "bg-slate-900 border-slate-700 text-slate-300 hover:text-white"
                }`}
              >
                <Power className="w-3.5 h-3.5" />
                <span>
                  {audit.redemptionsFrozen ? "Circuit-Breaker: ĐANG KHÓA (Bấm để mở)" : "Bật khóa Circuit-Breaker"}
                </span>
              </button>
            )}
            <button
              onClick={fetchAll}
              disabled={loading}
              className="flex items-center gap-2 px-3.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold transition"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? "animate-spin" : ""}`} />
              <span>Chạy kiểm toán</span>
            </button>
          </div>
        }
      />

      {/* 3-Currency Zero-Sum Audit Banner */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {currencies.map((ccy) => {
          const delta = Number(audit?.systemTotalsByCurrency?.[ccy] ?? 0);
          const ok = Math.abs(delta) < 0.0001;
          return (
            <div
              key={ccy}
              className={`p-5 rounded-2xl border ${
                ok ? "bg-slate-900/60 border-emerald-500/30" : "bg-red-950/30 border-red-500/50"
              }`}
            >
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span className="font-semibold">Tổng Hệ thống {ccy}</span>
                {ok ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                ) : (
                  <AlertTriangle className="w-4 h-4 text-red-400" />
                )}
              </div>
              <div className={`text-2xl font-bold font-mono mt-2 ${ok ? "text-emerald-400" : "text-red-400"}`}>
                Δ = {delta.toFixed(ccy === "PCC" ? 4 : 0)}
              </div>
              <div className="text-[11px] text-slate-500 mt-1">
                {ok ? "Zero-Sum Conservation Đạt" : "CẢNH BÁO LỆCH SỔ CÁI"}
              </div>
            </div>
          );
        })}

        <div className="p-5 rounded-2xl border bg-slate-900/60 border-slate-800">
          <div className="text-xs text-slate-400 font-semibold">Toàn vẹn Giao dịch & Số dư</div>
          <div className="mt-2 space-y-1 text-xs">
            <div className="flex justify-between">
              <span className="text-slate-400">Lệch Debit/Credit:</span>
              <span className="font-mono text-white">{audit?.unbalancedTransactionsCount ?? 0}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Trộn tiền tệ:</span>
              <span className="font-mono text-white">{audit?.crossCurrencyTransactionsCount ?? 0}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Drift tài khoản:</span>
              <span className="font-mono text-white">{audit?.accountDriftsCount ?? 0}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Sub-navigation Tabs */}
      <div className="flex flex-wrap gap-2">
        <button
          onClick={() => setTab("TRANSACTIONS")}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition ${
            tab === "TRANSACTIONS"
              ? "bg-emerald-500 text-white"
              : "bg-slate-900 text-slate-400 border border-slate-800 hover:text-white"
          }`}
        >
          <BookOpenCheck className="w-4 h-4" />
          Giao dịch Sổ cái ({txs.length})
        </button>
        <button
          onClick={() => setTab("AWARDS")}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition ${
            tab === "AWARDS"
              ? "bg-emerald-500 text-white"
              : "bg-slate-900 text-slate-400 border border-slate-800 hover:text-white"
          }`}
        >
          <ShieldAlert className="w-4 h-4" />
          Duyệt Thưởng / Fraud ({awards.length})
        </button>
        <button
          onClick={() => setTab("OUTBOX")}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition ${
            tab === "OUTBOX"
              ? "bg-emerald-500 text-white"
              : "bg-slate-900 text-slate-400 border border-slate-800 hover:text-white"
          }`}
        >
          <Send className="w-4 h-4" />
          Outbox FAILED ({outboxFailed.length})
        </button>
      </div>

      {/* TAB 1: LEDGER TRANSACTIONS */}
      {tab === "TRANSACTIONS" && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            {["ALL", "VND", "PCC", "ECO_POINT"].map((ccy) => (
              <button
                key={ccy}
                onClick={() => setCurrencyFilter(ccy)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold ${
                  currencyFilter === ccy
                    ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/40"
                    : "bg-slate-900 text-slate-400 border border-slate-800"
                }`}
              >
                {ccy}
              </button>
            ))}
            <input
              placeholder="Tìm mã TX..."
              value={txCodeSearch}
              onChange={(e) => setTxCodeSearch(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && fetchAll()}
              className="px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-200 w-56"
            />
          </div>

          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/70 uppercase text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-4 py-3.5">Mã GD</th>
                    <th className="px-4 py-3.5">Loại</th>
                    <th className="px-4 py-3.5">Tiền tệ</th>
                    <th className="px-4 py-3.5 text-right">Tổng Debit</th>
                    <th className="px-4 py-3.5">Diễn giải</th>
                    <th className="px-4 py-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {txs.map((t) => (
                    <tr key={t.id} className="hover:bg-slate-800/30">
                      <td className="px-4 py-3 font-mono text-emerald-400">{t.transactionCode}</td>
                      <td className="px-4 py-3 font-mono">{t.transactionType}</td>
                      <td className="px-4 py-3 font-bold">{t.currency}</td>
                      <td className="px-4 py-3 text-right font-mono">{Number(t.totalDebit).toLocaleString("vi-VN")}</td>
                      <td className="px-4 py-3 text-slate-400 max-w-xs truncate">{t.description}</td>
                      <td className="px-4 py-3 text-right">
                        <div className="inline-flex items-center gap-2">
                          <button
                            onClick={() => openTxDetail(t.id)}
                            className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 rounded-lg text-slate-200 inline-flex items-center gap-1"
                          >
                            <Eye className="w-3.5 h-3.5" /> Chi tiết
                          </button>
                          {!isReadOnly && !t.alreadyReversed && t.transactionType !== "ADMIN_REVERSAL" && (
                            <button
                              onClick={() => setReverseTarget(t)}
                              className="px-2.5 py-1 bg-red-500/20 hover:bg-red-500/30 text-red-300 border border-red-500/30 rounded-lg inline-flex items-center gap-1"
                            >
                              <RotateCcw className="w-3.5 h-3.5" /> Đảo GD
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: HELD AWARDS */}
      {tab === "AWARDS" && (
        <div className="space-y-4">
          <div className="flex flex-wrap gap-2">
            {["HELD_FOR_FRAUD", "PENDING_CONDITIONS", "CLAWBACK_SHORTFALL_AUDIT", "CREDITED", "ALL"].map((st) => (
              <button
                key={st}
                onClick={() => setAwardStatus(st)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold ${
                  awardStatus === st
                    ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/40"
                    : "bg-slate-900 text-slate-400 border border-slate-800"
                }`}
              >
                {st}
              </button>
            ))}
          </div>

          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/70 uppercase text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-4 py-3.5">Trip ID</th>
                    <th className="px-4 py-3.5">CO2 / PCC / PTS</th>
                    <th className="px-4 py-3.5">Join Barrier</th>
                    <th className="px-4 py-3.5">Trạng thái</th>
                    <th className="px-4 py-3.5">Ghi chú</th>
                    <th className="px-4 py-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {awards.map((a) => (
                    <tr key={a.id} className="hover:bg-slate-800/30">
                      <td className="px-4 py-3 font-mono">{a.tripId.substring(0, 8)}...</td>
                      <td className="px-4 py-3 font-mono">
                        {a.co2SavedGrams}g | {a.totalPcc} PCC | {a.customerPoints}+{a.driverPoints} PTS
                      </td>
                      <td className="px-4 py-3">
                        C:{a.carbonReady ? "OK" : ".."} / P:{a.paymentReady ? "OK" : ".."}
                      </td>
                      <td className="px-4 py-3 font-semibold text-amber-400">{a.status}</td>
                      <td className="px-4 py-3 text-slate-400 max-w-xs truncate">
                        {a.holdReason || a.reviewNote || "-"}
                      </td>
                      <td className="px-4 py-3 text-right">
                        {!isReadOnly && a.status === "HELD_FOR_FRAUD" && (
                          <div className="inline-flex gap-2">
                            <button
                              onClick={() => handleAwardDecision(a.id, "approve")}
                              className="px-2.5 py-1 bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 rounded-lg inline-flex items-center gap-1"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" /> Duyệt
                            </button>
                            <button
                              onClick={() => handleAwardDecision(a.id, "void")}
                              className="px-2.5 py-1 bg-red-500/20 text-red-300 border border-red-500/30 rounded-lg inline-flex items-center gap-1"
                            >
                              <XCircle className="w-3.5 h-3.5" /> Hủy thưởng
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: FAILED OUTBOX */}
      {tab === "OUTBOX" && (
        <div className="space-y-4">
          {!isReadOnly && outboxFailed.length > 0 && (
            <div className="flex justify-end">
              <button
                onClick={handleRedriveAll}
                className="px-4 py-2 bg-amber-600 hover:bg-amber-500 text-white rounded-xl text-xs font-semibold inline-flex items-center gap-2"
              >
                <Send className="w-4 h-4" />
                Re-drive Tất cả ({outboxFailed.length})
              </button>
            </div>
          )}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/70 uppercase text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-4 py-3.5">Routing Key</th>
                    <th className="px-4 py-3.5">Aggregate</th>
                    <th className="px-4 py-3.5">Retry</th>
                    <th className="px-4 py-3.5">Lỗi gần nhất</th>
                    <th className="px-4 py-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {outboxFailed.map((o) => (
                    <tr key={o.id}>
                      <td className="px-4 py-3 font-mono text-amber-400">{o.routingKey}</td>
                      <td className="px-4 py-3 font-mono">
                        {o.aggregateType}:{o.aggregateId.substring(0, 8)}
                      </td>
                      <td className="px-4 py-3 font-mono">{o.retryCount}</td>
                      <td className="px-4 py-3 text-red-400 max-w-sm truncate">{o.lastError || "-"}</td>
                      <td className="px-4 py-3 text-right">
                        {!isReadOnly && (
                          <button
                            onClick={() => handleRedriveOne(o.id)}
                            className="px-3 py-1 bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 rounded-lg"
                          >
                            Re-drive
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {outboxFailed.length === 0 && (
                    <tr>
                      <td colSpan={5} className="py-10 text-center text-slate-500">
                        Không có sự kiện Outbox FAILED nào.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {detailTx && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl p-6 space-y-4">
            <div className="flex justify-between items-center border-b border-slate-800 pb-3">
              <h3 className="font-bold text-white">Bút toán Sổ cái: {detailTx.transaction.transactionCode}</h3>
              <button onClick={() => setDetailTx(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            <table className="w-full text-left text-xs text-slate-300 font-mono">
              <thead className="bg-slate-950 text-slate-400">
                <tr>
                  <th className="px-3 py-2">Loại</th>
                  <th className="px-3 py-2">Tài khoản</th>
                  <th className="px-3 py-2 text-right">Số lượng</th>
                  <th className="px-3 py-2 text-right">Số dư sau</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {detailTx.entries.map((e) => (
                  <tr key={e.id}>
                    <td className="px-3 py-2">{e.entryType}</td>
                    <td className="px-3 py-2">{e.accountNumber}</td>
                    <td className="px-3 py-2 text-right">{e.amount}</td>
                    <td className="px-3 py-2 text-right">{e.balanceAfter}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Reverse Modal */}
      {reverseTarget && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <form
            onSubmit={handleReverseTx}
            className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 space-y-4"
          >
            <h3 className="font-bold text-white flex items-center gap-2">
              <RotateCcw className="w-5 h-5 text-red-400" />
              Đảo Giao dịch {reverseTarget.transactionCode}
            </h3>
            <p className="text-xs text-slate-400">
              Hệ thống sẽ sinh giao dịch ADMIN_REVERSAL đối ứng nguyên tử cho giao dịch {reverseTarget.currency}.
            </p>
            <textarea
              rows={3}
              required
              placeholder="Nhập lý do đảo giao dịch (tối thiểu 10 ký tự)..."
              value={reverseReason}
              onChange={(e) => setReverseReason(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-slate-100"
            />
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setReverseTarget(null)}
                className="px-4 py-2 bg-slate-800 rounded-xl text-xs text-slate-300"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={reversing}
                className="px-4 py-2 bg-red-600 hover:bg-red-500 rounded-xl text-xs text-white font-semibold"
              >
                {reversing ? "Đang xử lý..." : "Xác nhận Đảo GD"}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
