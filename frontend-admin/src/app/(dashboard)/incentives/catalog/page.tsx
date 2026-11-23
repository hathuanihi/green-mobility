"use client";

import React, { useEffect, useState } from "react";
import {
  Gift,
  Plus,
  Edit2,
  RefreshCw,
  Save,
  Tag,
  TreePine,
  ShoppingBag,
  Settings,
  Lock,
  CheckCircle2,
  X,
} from "lucide-react";
import Header from "@/components/layout/Header";
import apiClient from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useToast } from "@/components/Toast";
import { ApiResponse, RewardCatalogItem } from "@/types";

export default function IncentiveCatalogPage() {
  const { user } = useAuth();
  const { showToast } = useToast();
  const isReadOnly = user?.role === "ROLE_OPERATOR";

  const [rewards, setRewards] = useState<RewardCatalogItem[]>([]);
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);

  const [settings, setSettings] = useState({
    pcc_customer_share_ratio: "0.70",
    driver_eco_point_ratio: "0.50",
    pcc_to_vnd_rate: "500",
    pcc_convert_min: "0.5000",
    pcc_convert_monthly_cap: "100.0000",
    incentive_reconcile_minutes: "5",
  });
  const [savingSettings, setSavingSettings] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RewardCatalogItem | null>(null);
  const [savingReward, setSavingReward] = useState(false);
  const [form, setForm] = useState({
    title: "",
    description: "",
    partnerName: "Green Mobility",
    category: "VOUCHER_RIDE",
    carbonCreditCost: "0.5000",
    loyaltyPointCost: "20",
    imageUrl: "",
    quantityAvailable: "100",
    isActive: true,
    discountType: "FIXED_VND",
    discountValue: "20000",
    maxDiscountVnd: "20000",
    minFareVnd: "30000",
    validityDays: "30",
    maxRedemptionsPerUser: "5",
  });

  const fetchAll = async () => {
    setLoading(true);
    try {
      const catParam = categoryFilter !== "ALL" ? `&category=${categoryFilter}` : "";
      const [catRes, setRes] = await Promise.all([
        apiClient.get<ApiResponse<{ content: RewardCatalogItem[] }>>(
          `/admin/incentive/catalog?activeOnly=false&size=50${catParam}`
        ),
        apiClient.get<ApiResponse<Record<string, string>>>("/admin/incentive/settings"),
      ]);
      if (catRes.data.success && catRes.data.data?.content) {
        setRewards(catRes.data.data.content);
      }
      if (setRes.data.success && setRes.data.data) {
        const s = setRes.data.data;
        setSettings((prev) => ({
          pcc_customer_share_ratio: s.pcc_customer_share_ratio ?? prev.pcc_customer_share_ratio,
          driver_eco_point_ratio: s.driver_eco_point_ratio ?? prev.driver_eco_point_ratio,
          pcc_to_vnd_rate: s.pcc_to_vnd_rate ?? prev.pcc_to_vnd_rate,
          pcc_convert_min: s.pcc_convert_min ?? prev.pcc_convert_min,
          pcc_convert_monthly_cap: s.pcc_convert_monthly_cap ?? prev.pcc_convert_monthly_cap,
          incentive_reconcile_minutes: s.incentive_reconcile_minutes ?? prev.incentive_reconcile_minutes,
        }));
      }
    } catch (err: any) {
      showToast("Không thể tải dữ liệu Eco-Store: " + (err.response?.data?.message || err.message), "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, [categoryFilter]);

  const openCreateModal = () => {
    setEditing(null);
    setForm({
      title: "",
      description: "",
      partnerName: "Green Mobility",
      category: "VOUCHER_RIDE",
      carbonCreditCost: "0.5000",
      loyaltyPointCost: "20",
      imageUrl: "",
      quantityAvailable: "100",
      isActive: true,
      discountType: "FIXED_VND",
      discountValue: "20000",
      maxDiscountVnd: "20000",
      minFareVnd: "30000",
      validityDays: "30",
      maxRedemptionsPerUser: "5",
    });
    setModalOpen(true);
  };

  const openEditModal = (r: RewardCatalogItem) => {
    setEditing(r);
    setForm({
      title: r.title,
      description: r.description || "",
      partnerName: r.partnerName,
      category: r.category,
      carbonCreditCost: String(r.carbonCreditCost ?? 0),
      loyaltyPointCost: String(r.loyaltyPointCost ?? 0),
      imageUrl: r.imageUrl || "",
      quantityAvailable: String(r.quantityAvailable ?? 0),
      isActive: r.isActive,
      discountType: r.discountType || "FIXED_VND",
      discountValue: r.discountValue != null ? String(r.discountValue) : "",
      maxDiscountVnd: r.maxDiscountVnd != null ? String(r.maxDiscountVnd) : "",
      minFareVnd: String(r.minFareVnd ?? 0),
      validityDays: String(r.validityDays ?? 30),
      maxRedemptionsPerUser: String(r.maxRedemptionsPerUser ?? 5),
    });
    setModalOpen(true);
  };

  const handleSaveReward = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isReadOnly) return;
    setSavingReward(true);
    try {
      const payload = {
        title: form.title,
        description: form.description,
        partnerName: form.partnerName,
        category: form.category,
        carbonCreditCost: Number(form.carbonCreditCost),
        loyaltyPointCost: parseInt(form.loyaltyPointCost, 10) || 0,
        imageUrl: form.imageUrl || null,
        quantityAvailable: parseInt(form.quantityAvailable, 10) || 0,
        isActive: form.isActive,
        discountType: form.category === "VOUCHER_RIDE" ? form.discountType : null,
        discountValue: form.category === "VOUCHER_RIDE" && form.discountValue ? Number(form.discountValue) : null,
        maxDiscountVnd: form.category === "VOUCHER_RIDE" && form.maxDiscountVnd ? Number(form.maxDiscountVnd) : null,
        minFareVnd: parseInt(form.minFareVnd, 10) || 0,
        validityDays: parseInt(form.validityDays, 10) || 30,
        maxRedemptionsPerUser: parseInt(form.maxRedemptionsPerUser, 10) || 5,
      };
      if (editing) {
        await apiClient.put(`/admin/incentive/catalog/${editing.id}`, payload);
        showToast("Đã cập nhật ưu đãi Eco-Store!", "success");
      } else {
        await apiClient.post("/admin/incentive/catalog", payload);
        showToast("Đã tạo ưu đãi Eco-Store mới!", "success");
      }
      setModalOpen(false);
      await fetchAll();
    } catch (err: any) {
      showToast("Lỗi lưu ưu đãi: " + (err.response?.data?.message || err.message), "error");
    } finally {
      setSavingReward(false);
    }
  };

  const handleSaveSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isReadOnly) return;
    setSavingSettings(true);
    try {
      await apiClient.put("/admin/incentive/settings", settings);
      showToast("Đã cập nhật cấu hình tỷ lệ & hạn mức khuyến khích!", "success");
    } catch (err: any) {
      showToast("Lỗi lưu cấu hình: " + (err.response?.data?.message || err.message), "error");
    } finally {
      setSavingSettings(false);
    }
  };

  const getCategoryIcon = (cat: string) => {
    if (cat === "TREE_PLANTING") return <TreePine className="w-4 h-4 text-emerald-400" />;
    if (cat === "ECO_PRODUCT") return <ShoppingBag className="w-4 h-4 text-sky-400" />;
    return <Tag className="w-4 h-4 text-amber-400" />;
  };

  return (
    <div className="space-y-8">
      <Header
        title="Eco-Store & Chính sách Khuyến khích"
        description="Quản lý danh mục voucher, quà tặng đối tác, chứng nhận trồng cây và tỷ lệ chia sẻ PCC / Eco-Points"
        actions={
          <div className="flex items-center gap-3">
            {isReadOnly && (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-semibold">
                <Lock className="w-3.5 h-3.5" /> Chỉ xem (Operator)
              </span>
            )}
            <button
              onClick={fetchAll}
              disabled={loading}
              className="flex items-center gap-2 px-3.5 py-2 bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-200 rounded-xl text-xs font-semibold transition"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? "animate-spin text-emerald-400" : ""}`} />
              <span>Làm mới</span>
            </button>
            {!isReadOnly && (
              <button
                onClick={openCreateModal}
                className="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold transition"
              >
                <Plus className="w-4 h-4" />
                <span>Thêm Quà / Voucher</span>
              </button>
            )}
          </div>
        }
      />

      {/* Category Filter */}
      <div className="flex flex-wrap gap-2">
        {[
          { key: "ALL", label: "Tất cả danh mục" },
          { key: "VOUCHER_RIDE", label: "Voucher Chuyến xe" },
          { key: "TREE_PLANTING", label: "Trồng cây Rừng xanh" },
          { key: "ECO_PRODUCT", label: "Sản phẩm Đối tác" },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setCategoryFilter(tab.key)}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition ${
              categoryFilter === tab.key
                ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/40"
                : "bg-slate-900 text-slate-400 border border-slate-800 hover:text-white"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Rewards Table */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/70 uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="px-5 py-4">Ưu đãi / Quà tặng</th>
                <th className="px-5 py-4">Danh mục</th>
                <th className="px-5 py-4 text-right">Giá PCC</th>
                <th className="px-5 py-4 text-right">Điểm Xanh</th>
                <th className="px-5 py-4 text-right">Tồn kho</th>
                <th className="px-5 py-4 text-center">Trạng thái</th>
                <th className="px-5 py-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {rewards.map((r) => (
                <tr key={r.id} className="hover:bg-slate-800/30 transition">
                  <td className="px-5 py-4">
                    <div className="font-semibold text-slate-100 text-sm">{r.title}</div>
                    <div className="text-slate-400 mt-0.5">{r.partnerName}</div>
                  </td>
                  <td className="px-5 py-4">
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-950 border border-slate-800 font-mono text-[11px]">
                      {getCategoryIcon(r.category)}
                      {r.category}
                    </span>
                  </td>
                  <td className="px-5 py-4 text-right font-mono text-emerald-400 font-semibold">
                    {Number(r.carbonCreditCost).toFixed(4)} PCC
                  </td>
                  <td className="px-5 py-4 text-right font-mono text-amber-400 font-semibold">
                    {r.loyaltyPointCost.toLocaleString("vi-VN")} PTS
                  </td>
                  <td className="px-5 py-4 text-right font-mono">{r.quantityAvailable}</td>
                  <td className="px-5 py-4 text-center">
                    <span
                      className={`px-2.5 py-1 rounded-full text-[11px] font-semibold ${
                        r.isActive
                          ? "bg-emerald-500/20 text-emerald-400 border border-emerald-500/30"
                          : "bg-slate-800 text-slate-500 border border-slate-700"
                      }`}
                    >
                      {r.isActive ? "Đang mở" : "Tạm ẩn"}
                    </span>
                  </td>
                  <td className="px-5 py-4 text-right">
                    {!isReadOnly && (
                      <button
                        onClick={() => openEditModal(r)}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 transition"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                        Sửa
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {!loading && rewards.length === 0 && (
                <tr>
                  <td colSpan={7} className="py-10 text-center text-slate-500">
                    Chưa có ưu đãi nào trong danh mục này.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Incentive Governance Settings */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6">
        <div className="flex items-center gap-2.5 pb-4 border-b border-slate-800 mb-6">
          <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <Settings className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-base font-bold text-white">Tham số Thưởng Carbon & Trợ giá Xanh</h3>
            <p className="text-xs text-slate-400">
              Cấu hình tỷ lệ chia PCC, Eco-Points và hạn mức quy đổi PCC → VNĐ (trợ giá marketing xanh)
            </p>
          </div>
        </div>

        <form onSubmit={handleSaveSettings} className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {[
            { key: "pcc_customer_share_ratio", label: "Tỷ lệ PCC cho Hành khách (0..1)", step: "0.05" },
            { key: "driver_eco_point_ratio", label: "Tỷ lệ Điểm Xanh Tài xế / Khách", step: "0.05" },
            { key: "pcc_to_vnd_rate", label: "Tỷ giá Trợ giá (VNĐ / 1 PCC)", step: "50" },
            { key: "pcc_convert_min", label: "Số PCC quy đổi tối thiểu / lần", step: "0.1" },
            { key: "pcc_convert_monthly_cap", label: "Trần quy đổi PCC mỗi tháng", step: "10" },
            { key: "incentive_reconcile_minutes", label: "Ngưỡng đối soát kẹt (phút)", step: "1" },
          ].map((field) => (
            <div key={field.key}>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">{field.label}</label>
              <input
                type="number"
                step={field.step}
                disabled={isReadOnly}
                value={(settings as any)[field.key]}
                onChange={(e) => setSettings({ ...settings, [field.key]: e.target.value })}
                className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-sm text-slate-100 font-mono disabled:opacity-60"
              />
            </div>
          ))}
          {!isReadOnly && (
            <div className="md:col-span-3 flex justify-end pt-2">
              <button
                type="submit"
                disabled={savingSettings}
                className="inline-flex items-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold transition disabled:opacity-60"
              >
                <Save className="w-4 h-4" />
                <span>{savingSettings ? "Đang lưu..." : "Lưu Tham số Khuyến khích"}</span>
              </button>
            </div>
          )}
        </form>
      </div>

      {/* Create/Edit Reward Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl p-6 max-h-[90vh] overflow-y-auto space-y-5">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-bold text-lg text-white flex items-center gap-2">
                <Gift className="w-5 h-5 text-emerald-400" />
                {editing ? "Chỉnh sửa Ưu đãi Eco-Store" : "Thêm Mới Ưu đãi Eco-Store"}
              </h3>
              <button onClick={() => setModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveReward} className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="sm:col-span-2">
                <label className="block text-slate-300 font-semibold mb-1">Tên quà / voucher *</label>
                <input
                  required
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100"
                />
              </div>
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Đối tác *</label>
                <input
                  required
                  value={form.partnerName}
                  onChange={(e) => setForm({ ...form, partnerName: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100"
                />
              </div>
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Danh mục *</label>
                <select
                  value={form.category}
                  onChange={(e) => setForm({ ...form, category: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100"
                >
                  <option value="VOUCHER_RIDE">VOUCHER_RIDE</option>
                  <option value="TREE_PLANTING">TREE_PLANTING</option>
                  <option value="ECO_PRODUCT">ECO_PRODUCT</option>
                </select>
              </div>
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Chi phí PCC (4 thập phân)</label>
                <input
                  type="number"
                  step="0.0001"
                  min="0"
                  value={form.carbonCreditCost}
                  onChange={(e) => setForm({ ...form, carbonCreditCost: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100 font-mono"
                />
              </div>
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Chi phí Điểm Xanh</label>
                <input
                  type="number"
                  min="0"
                  value={form.loyaltyPointCost}
                  onChange={(e) => setForm({ ...form, loyaltyPointCost: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100 font-mono"
                />
              </div>
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Số lượng tồn kho</label>
                <input
                  type="number"
                  min="0"
                  value={form.quantityAvailable}
                  onChange={(e) => setForm({ ...form, quantityAvailable: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100 font-mono"
                />
              </div>
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Hiệu lực (ngày) / Trần mỗi người</label>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="number"
                    min="1"
                    value={form.validityDays}
                    onChange={(e) => setForm({ ...form, validityDays: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100 font-mono"
                  />
                  <input
                    type="number"
                    min="1"
                    value={form.maxRedemptionsPerUser}
                    onChange={(e) => setForm({ ...form, maxRedemptionsPerUser: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100 font-mono"
                  />
                </div>
              </div>

              {form.category === "VOUCHER_RIDE" && (
                <>
                  <div>
                    <label className="block text-slate-300 font-semibold mb-1">Loại giảm giá</label>
                    <select
                      value={form.discountType}
                      onChange={(e) => setForm({ ...form, discountType: e.target.value })}
                      className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100"
                    >
                      <option value="FIXED_VND">FIXED_VND (VNĐ cố định)</option>
                      <option value="PERCENT">PERCENT (% cước)</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-slate-300 font-semibold mb-1">Giá trị giảm / Trần tối đa (VNĐ)</label>
                    <div className="grid grid-cols-2 gap-2">
                      <input
                        type="number"
                        value={form.discountValue}
                        onChange={(e) => setForm({ ...form, discountValue: e.target.value })}
                        className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100 font-mono"
                      />
                      <input
                        type="number"
                        value={form.maxDiscountVnd}
                        onChange={(e) => setForm({ ...form, maxDiscountVnd: e.target.value })}
                        className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100 font-mono"
                      />
                    </div>
                  </div>
                </>
              )}

              <div className="sm:col-span-2">
                <label className="block text-slate-300 font-semibold mb-1">Mô tả chi tiết</label>
                <textarea
                  rows={2}
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-slate-100"
                />
              </div>

              <div className="sm:col-span-2 flex items-center gap-2">
                <input
                  id="rewardActive"
                  type="checkbox"
                  checked={form.isActive}
                  onChange={(e) => setForm({ ...form, isActive: e.target.checked })}
                />
                <label htmlFor="rewardActive" className="text-slate-300">
                  Kích hoạt hiển thị trên Eco-Store
                </label>
              </div>

              <div className="sm:col-span-2 flex justify-end gap-3 pt-2 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 bg-slate-800 text-slate-300 rounded-xl"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={savingReward}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl font-semibold flex items-center gap-1.5"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>{savingReward ? "Đang lưu..." : "Lưu Ưu đãi"}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
