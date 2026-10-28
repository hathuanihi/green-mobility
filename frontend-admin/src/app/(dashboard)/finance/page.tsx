'use client';

import React, { useState, useEffect } from 'react';
import { 
  DollarSign, 
  TrendingUp, 
  ArrowUpRight, 
  ArrowDownLeft, 
  ShieldCheck, 
  AlertTriangle, 
  RefreshCw, 
  Save, 
  CheckCircle2, 
  XCircle,
  HelpCircle,
  Settings
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import { FinanceOverviewData, ReconciliationData } from '@/types';

const COLORS = ['#10B981', '#3B82F6', '#8B5CF6', '#F59E0B', '#EF4444'];

export default function FinancePage() {
  const [loading, setLoading] = useState(true);
  const [reconciling, setReconciling] = useState(false);
  const [savingSettings, setSavingSettings] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const [financeData, setFinanceData] = useState<FinanceOverviewData | null>(null);
  const [reconciliationData, setReconciliationData] = useState<ReconciliationData | null>(null);

  // Settings form state
  const [settings, setSettings] = useState({
    commissionRate: 0.20,
    topupMinVnd: 50000,
    topupMaxVnd: 10000000,
    withdrawMinVnd: 100000,
    withdrawMaxVnd: 20000000,
    cashDebtLimitVnd: 500000,
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      // Mock / API call
      // In production: await fetch('/api/v1/admin/finance/overview')...
      setTimeout(() => {
        setFinanceData({
          todayGmv: 45200000,
          todayPlatformRevenue: 9040000,
          todayDriverEarnings: 36160000,
          totalWalletBalance: 128500000,
          activeDriversCount: 142,
          revenueByDay: [
            { date: '22/10', gmv: 38000000, revenue: 7600000, driverEarnings: 30400000 },
            { date: '23/10', gmv: 41000000, revenue: 8200000, driverEarnings: 32800000 },
            { date: '24/10', gmv: 39500000, revenue: 7900000, driverEarnings: 31600000 },
            { date: '25/10', gmv: 44000000, revenue: 8800000, driverEarnings: 35200000 },
            { date: '26/10', gmv: 48000000, revenue: 9600000, driverEarnings: 38400000 },
            { date: '27/10', gmv: 52000000, revenue: 10400000, driverEarnings: 41600000 },
            { date: '28/10', gmv: 45200000, revenue: 9040000, driverEarnings: 36160000 },
          ],
          paymentMethodBreakdown: [
            { method: 'GREEN_WALLET', count: 420, amount: 25200000 },
            { method: 'VNPAY', count: 180, amount: 11000000 },
            { method: 'MOMO', count: 90, amount: 5500000 },
            { method: 'CASH', count: 50, amount: 3500000 },
          ],
        });

        setReconciliationData({
          lastRunAt: new Date().toISOString(),
          status: 'BALANCED',
          totalSystemDebit: 254800000,
          totalSystemCredit: 254800000,
          systemBalanceDifference: 0,
          anomaliesCount: 0,
          checks: {
            zeroSumLedger: true,
            userBalanceNonNegative: true,
            gatewayMatching: true,
            cashDebtCompliant: true,
            pendingWithdrawalsAudited: true,
          },
          discrepancies: []
        });

        setLoading(false);
      }, 500);
    } catch {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleRunReconciliation = async () => {
    setReconciling(true);
    // Simulating running check
    setTimeout(() => {
      setReconciliationData(prev => prev ? ({
        ...prev,
        lastRunAt: new Date().toISOString(),
        status: 'BALANCED',
        systemBalanceDifference: 0,
        anomaliesCount: 0,
        checks: {
          zeroSumLedger: true,
          userBalanceNonNegative: true,
          gatewayMatching: true,
          cashDebtCompliant: true,
          pendingWithdrawalsAudited: true,
        },
        discrepancies: []
      }) : null);
      setReconciling(false);
    }, 1200);
  };

  const handleSaveSettings = (e: React.FormEvent) => {
    e.preventDefault();
    setSavingSettings(true);
    setTimeout(() => {
      setSavingSettings(false);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    }, 800);
  };

  return (
    <div className="p-8 space-y-8 bg-slate-50 min-h-screen">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Tài chính & Đối soát</h1>
          <p className="text-sm text-slate-500 mt-1">
            Tổng quan doanh thu, cân đối sổ cái Double-Entry, và cấu hình ví & hoa hồng
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={fetchData}
            disabled={loading}
            className="inline-flex items-center gap-2 px-3.5 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 bg-white hover:bg-slate-50 transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
          <button
            onClick={handleRunReconciliation}
            disabled={reconciling}
            className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium transition shadow-sm"
          >
            <ShieldCheck className={`w-4 h-4 ${reconciling ? 'animate-spin' : ''}`} />
            {reconciling ? 'Đang chạy đối soát...' : 'Chạy lại đối soát'}
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">GMV Hôm Nay</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">
              {financeData?.todayGmv.toLocaleString('vi-VN')} đ
            </p>
            <span className="inline-flex items-center text-xs font-medium text-emerald-600 mt-2">
              <TrendingUp className="w-3.5 h-3.5 mr-1" /> +12.5% so với tuần trước
            </span>
          </div>
          <div className="p-3 bg-emerald-50 rounded-xl">
            <DollarSign className="w-6 h-6 text-emerald-600" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Doanh thu sàn (20%)</p>
            <p className="text-2xl font-bold text-emerald-600 mt-1">
              {financeData?.todayPlatformRevenue.toLocaleString('vi-VN')} đ
            </p>
            <span className="inline-flex items-center text-xs font-medium text-slate-500 mt-2">
              Hoa hồng thuần sau chiết khấu
            </span>
          </div>
          <div className="p-3 bg-blue-50 rounded-xl">
            <ArrowUpRight className="w-6 h-6 text-blue-600" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Thu nhập tài xế (80%)</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">
              {financeData?.todayDriverEarnings.toLocaleString('vi-VN')} đ
            </p>
            <span className="inline-flex items-center text-xs font-medium text-slate-500 mt-2">
              Cho {financeData?.activeDriversCount} tài xế hoạt động
            </span>
          </div>
          <div className="p-3 bg-purple-50 rounded-xl">
            <ArrowDownLeft className="w-6 h-6 text-purple-600" />
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Tổng số dư Ví Xanh</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">
              {financeData?.totalWalletBalance.toLocaleString('vi-VN')} đ
            </p>
            <span className="inline-flex items-center text-xs font-medium text-emerald-600 mt-2">
              Bảo chứng 100% tại ngân hàng
            </span>
          </div>
          <div className="p-3 bg-amber-50 rounded-xl">
            <ShieldCheck className="w-6 h-6 text-amber-600" />
          </div>
        </div>
      </div>

      {/* Reconciliation Block (AC-15 & AC-18) */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-slate-900 text-white">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-emerald-500/20 text-emerald-400 rounded-lg border border-emerald-500/30">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-lg font-bold">Kiểm toán & Đối soát Sổ cái Tự động</h2>
              <p className="text-xs text-slate-400">
                Lần chạy gần nhất: {reconciliationData?.lastRunAt ? new Date(reconciliationData.lastRunAt).toLocaleString('vi-VN') : 'Chưa chạy'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {reconciliationData?.status === 'BALANCED' ? (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                <CheckCircle2 className="w-4 h-4" /> Hệ thống Sổ cái Cân Bằng (Diff = 0 đ)
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold bg-red-500/20 text-red-300 border border-red-500/30">
                <AlertTriangle className="w-4 h-4" /> Phát hiện chênh lệch!
              </span>
            )}
          </div>
        </div>

        <div className="p-6 grid grid-cols-1 md:grid-cols-5 gap-4 bg-slate-50/50">
          <div className="p-4 bg-white rounded-lg border border-slate-200">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Zero-Sum Ledger</span>
              {reconciliationData?.checks.zeroSumLedger ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              ) : (
                <XCircle className="w-4 h-4 text-red-500" />
              )}
            </div>
            <p className="text-sm font-semibold text-slate-800 mt-2">Σ Debit = Σ Credit</p>
            <p className="text-xs text-slate-400 mt-0.5">Không có giao dịch lệch</p>
          </div>

          <div className="p-4 bg-white rounded-lg border border-slate-200">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Ví không âm</span>
              {reconciliationData?.checks.userBalanceNonNegative ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              ) : (
                <XCircle className="w-4 h-4 text-red-500" />
              )}
            </div>
            <p className="text-sm font-semibold text-slate-800 mt-2">Balance ≥ 0 đ</p>
            <p className="text-xs text-slate-400 mt-0.5">Mọi ví khách & tài xế</p>
          </div>

          <div className="p-4 bg-white rounded-lg border border-slate-200">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Đối chiếu Gateway</span>
              {reconciliationData?.checks.gatewayMatching ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              ) : (
                <XCircle className="w-4 h-4 text-red-500" />
              )}
            </div>
            <p className="text-sm font-semibold text-slate-800 mt-2">VNPay & MoMo</p>
            <p className="text-xs text-slate-400 mt-0.5">Khớp 100% webhook IPN</p>
          </div>

          <div className="p-4 bg-white rounded-lg border border-slate-200">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Nợ tiền mặt tài xế</span>
              {reconciliationData?.checks.cashDebtCompliant ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              ) : (
                <XCircle className="w-4 h-4 text-red-500" />
              )}
            </div>
            <p className="text-sm font-semibold text-slate-800 mt-2">Dưới ngưỡng trần</p>
            <p className="text-xs text-slate-400 mt-0.5">≤ 500.000 đ</p>
          </div>

          <div className="p-4 bg-white rounded-lg border border-slate-200">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-slate-500">Rút tiền chờ duyệt</span>
              {reconciliationData?.checks.pendingWithdrawalsAudited ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              ) : (
                <XCircle className="w-4 h-4 text-red-500" />
              )}
            </div>
            <p className="text-sm font-semibold text-slate-800 mt-2">Hold quỹ hợp lệ</p>
            <p className="text-xs text-slate-400 mt-0.5">Đã phong tỏa sổ cái</p>
          </div>
        </div>

        {reconciliationData && reconciliationData.anomaliesCount > 0 && (
          <div className="p-4 bg-red-50 border-t border-red-200 flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm text-red-800">
              <AlertTriangle className="w-5 h-5 text-red-600" />
              <span>Phát hiện <strong>{reconciliationData.anomaliesCount}</strong> giao dịch bất thường cần xử lý thủ công</span>
            </div>
            <button className="text-xs font-semibold text-red-700 underline">Xem chi tiết bất thường</button>
          </div>
        )}
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* GMV and Revenue Trend */}
        <div className="lg:col-span-2 bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <h2 className="text-base font-bold text-slate-900 mb-4">Xu hướng GMV & Doanh thu 7 ngày qua</h2>
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={financeData?.revenueByDay || []} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                <XAxis dataKey="date" stroke="#64748B" fontSize={12} />
                <YAxis stroke="#64748B" fontSize={12} tickFormatter={(v) => `${v / 1000000}M`} />
                <Tooltip 
                  formatter={(val: number) => [`${val.toLocaleString('vi-VN')} đ`]}
                  labelStyle={{ fontWeight: 'bold' }}
                />
                <Legend />
                <Bar dataKey="gmv" fill="#3B82F6" name="Tổng GMV" radius={[4, 4, 0, 0]} />
                <Bar dataKey="revenue" fill="#10B981" name="Doanh thu sàn" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Payment Methods Distribution */}
        <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm">
          <h2 className="text-base font-bold text-slate-900 mb-4">Cơ cấu phương thức thanh toán</h2>
          <div className="h-56 w-full flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={financeData?.paymentMethodBreakdown || []}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="amount"
                  nameKey="method"
                >
                  {(financeData?.paymentMethodBreakdown || []).map((entry, index: number) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(val: number) => [`${val.toLocaleString('vi-VN')} đ`]} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-4 space-y-2">
            {(financeData?.paymentMethodBreakdown || []).map((item, idx: number) => (
              <div key={item.method} className="flex justify-between items-center text-xs">
                <span className="flex items-center gap-1.5 text-slate-600">
                  <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: COLORS[idx % COLORS.length] }} />
                  {item.method}
                </span>
                <span className="font-semibold text-slate-900">{item.amount.toLocaleString('vi-VN')} đ ({item.count} đơn)</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Platform Settings Form (AC-18) */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100">
          <div className="flex items-center gap-2">
            <Settings className="w-5 h-5 text-slate-700" />
            <h2 className="text-base font-bold text-slate-900">Cấu hình Thông số Tài chính & Ví (Platform Settings)</h2>
          </div>
          {saveSuccess && (
            <span className="inline-flex items-center gap-1.5 text-xs text-emerald-600 font-semibold">
              <CheckCircle2 className="w-4 h-4" /> Đã cập nhật thành công!
            </span>
          )}
        </div>

        <form onSubmit={handleSaveSettings} className="mt-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Tỷ lệ chiết khấu sàn (Commission Rate)
              </label>
              <div className="relative mt-1">
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  max="0.5"
                  value={settings.commissionRate}
                  onChange={(e) => setSettings({ ...settings, commissionRate: parseFloat(e.target.value) })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
                <span className="absolute right-3 top-2.5 text-xs text-slate-400">
                  {(settings.commissionRate * 100).toFixed(0)}%
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-1">Mặc định: 0.20 (20% trên mỗi cuốc xe)</p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Hạn mức nạp tối thiểu (VND)
              </label>
              <input
                type="number"
                step="10000"
                value={settings.topupMinVnd}
                onChange={(e) => setSettings({ ...settings, topupMinVnd: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              />
              <p className="text-xs text-slate-400 mt-1">Số tiền nạp tối thiểu vào Ví Xanh</p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Hạn mức nạp tối đa (VND)
              </label>
              <input
                type="number"
                step="100000"
                value={settings.topupMaxVnd}
                onChange={(e) => setSettings({ ...settings, topupMaxVnd: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              />
              <p className="text-xs text-slate-400 mt-1">Hạn mức nạp tối đa mỗi lần giao dịch</p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Hạn mức rút tối thiểu (VND)
              </label>
              <input
                type="number"
                step="10000"
                value={settings.withdrawMinVnd}
                onChange={(e) => setSettings({ ...settings, withdrawMinVnd: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              />
              <p className="text-xs text-slate-400 mt-1">Tài xế rút tối thiểu mỗi lần</p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Hạn mức rút tối đa (VND)
              </label>
              <input
                type="number"
                step="1000000"
                value={settings.withdrawMaxVnd}
                onChange={(e) => setSettings({ ...settings, withdrawMaxVnd: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              />
              <p className="text-xs text-slate-400 mt-1">Hạn mức rút tối đa mỗi lần</p>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Trần nợ tiền mặt tài xế (VND)
              </label>
              <input
                type="number"
                step="50000"
                value={settings.cashDebtLimitVnd}
                onChange={(e) => setSettings({ ...settings, cashDebtLimitVnd: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              />
              <p className="text-xs text-slate-400 mt-1">Vượt ngưỡng này tài xế bị tạm khóa nhận cuốc tiền mặt</p>
            </div>
          </div>

          <div className="flex justify-end pt-4 border-t border-slate-100">
            <button
              type="submit"
              disabled={savingSettings}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-sm font-medium transition shadow-sm"
            >
              <Save className="w-4 h-4" />
              {savingSettings ? 'Đang lưu...' : 'Lưu thay đổi cấu hình'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
