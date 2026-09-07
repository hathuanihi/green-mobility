import Link from "next/link";
import { 
  Car, 
  Leaf, 
  TrendingUp, 
  ShieldCheck, 
  Sliders, 
  Users, 
  Bot, 
  Activity,
  Trees,
  CheckCircle2
} from "lucide-react";

export default function AdminDashboardPage() {
  return (
    <div className="flex h-screen bg-slate-950 text-slate-100">
      {/* Sidebar */}
      <aside className="w-64 border-r border-slate-800 bg-slate-900/50 p-6 flex flex-col justify-between">
        <div>
          <div className="flex items-center gap-3 mb-8">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400">
              <Leaf className="w-6 h-6" />
            </div>
            <div>
              <h1 className="font-bold text-lg leading-tight text-white">Green Mobility</h1>
              <p className="text-xs text-emerald-400 font-medium">Admin Portal 1.0</p>
            </div>
          </div>

          <nav className="space-y-1.5 text-sm">
            <Link href="/" className="flex items-center gap-3 px-3.5 py-2.5 rounded-lg bg-emerald-500/10 text-emerald-400 font-medium border border-emerald-500/20">
              <Activity className="w-4 h-4" /> Tổng quan Hệ thống
            </Link>
            <Link href="/drivers" className="flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 transition">
              <Users className="w-4 h-4" /> Tài xế & KYC
            </Link>
            <Link href="/vehicles" className="flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 transition">
              <Car className="w-4 h-4" /> Phương tiện Xe điện
            </Link>
            <Link href="/trips" className="flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 transition">
              <TrendingUp className="w-4 h-4" /> Chuyến đi & Replay
            </Link>
            <Link href="/emissions" className="flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 transition">
              <Sliders className="w-4 h-4" /> Hệ số Phát thải
            </Link>
            <Link href="/fraud-monitor" className="flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 transition">
              <ShieldCheck className="w-4 h-4" /> Giám sát Gian lận
            </Link>
            <Link href="/ai-copilot" className="flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-slate-400 hover:text-slate-100 hover:bg-slate-800/60 transition">
              <Bot className="w-4 h-4" /> AI Admin Copilot
            </Link>
          </nav>
        </div>

        <div className="pt-4 border-t border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-xs font-bold text-emerald-400">
              AD
            </div>
            <div>
              <p className="text-sm font-medium text-slate-200">Admin Quản trị</p>
              <p className="text-xs text-slate-500">admin@greenmobility.vn</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto p-8">
        <div className="max-w-7xl mx-auto space-y-8">
          {/* Header */}
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-2xl font-bold text-white tracking-tight">Trung tâm Điều hành & Báo cáo Carbon</h2>
              <p className="text-sm text-slate-400 mt-1">Giám sát hoạt động xe điện và chỉ số giảm phát thải thời gian thực tại TP.HCM</p>
            </div>
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-xs font-medium text-emerald-400">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span> Hệ thống đang vận hành ổn định
            </div>
          </div>

          {/* KPI Cards */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-5">
            <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-2">
              <div className="flex justify-between items-center text-slate-400">
                <span className="text-xs font-medium uppercase tracking-wider">Tổng CO2 Đã Giảm</span>
                <Leaf className="w-4 h-4 text-emerald-400" />
              </div>
              <p className="text-3xl font-extrabold text-white">124.85 <span className="text-lg font-normal text-emerald-400">tấn</span></p>
              <p className="text-xs text-emerald-400 font-medium">+18.2% so với tháng trước</p>
            </div>

            <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-2">
              <div className="flex justify-between items-center text-slate-400">
                <span className="text-xs font-medium uppercase tracking-wider">Tương Đương Cây Xanh</span>
                <Trees className="w-4 h-4 text-cyan-400" />
              </div>
              <p className="text-3xl font-extrabold text-white">2.08M <span className="text-lg font-normal text-cyan-400">ngày cây</span></p>
              <p className="text-xs text-slate-400">Hấp thụ CO2 tích lũy</p>
            </div>

            <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-2">
              <div className="flex justify-between items-center text-slate-400">
                <span className="text-xs font-medium uppercase tracking-wider">Chuyến Xe Xanh</span>
                <Car className="w-4 h-4 text-amber-400" />
              </div>
              <p className="text-3xl font-extrabold text-white">158,420</p>
              <p className="text-xs text-slate-400">Thời gian ghép TB: 18.2 giây</p>
            </div>

            <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-2">
              <div className="flex justify-between items-center text-slate-400">
                <span className="text-xs font-medium uppercase tracking-wider">Xe Đang Trực Tuyến</span>
                <Activity className="w-4 h-4 text-indigo-400" />
              </div>
              <p className="text-3xl font-extrabold text-white">1,420</p>
              <p className="text-xs text-slate-400">980 Xe máy điện | 440 Ô tô điện</p>
            </div>
          </div>

          {/* Quick Actions & Status */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 col-span-2 space-y-4">
              <h3 className="font-semibold text-white">Đội xe & Trạng thái Ca trực tuyến (TP.HCM)</h3>
              <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between">
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-200">VinFast Feliz S & Klara S (E-Bike)</p>
                  <p className="text-xs text-slate-500">980 xe đang nhận cuốc • Tiết kiệm 45.4 gCO2/km</p>
                </div>
                <span className="px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-medium border border-emerald-500/20">Hoạt động 98%</span>
              </div>
              <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center justify-between">
                <div className="space-y-1">
                  <p className="text-sm font-medium text-slate-200">VinFast VF e34 & VF 5 (E-Car 4 chỗ)</p>
                  <p className="text-xs text-slate-500">440 xe đang nhận cuốc • Tiết kiệm 54.5 gCO2/km</p>
                </div>
                <span className="px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-medium border border-emerald-500/20">Hoạt động 95%</span>
              </div>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
              <h3 className="font-semibold text-white">Hệ số Lưới điện Quốc gia</h3>
              <div className="p-4 rounded-xl bg-emerald-950/20 border border-emerald-800/30 space-y-2">
                <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
                  <CheckCircle2 className="w-4 h-4" /> Chuẩn Bộ TN&MT / IPCC
                </div>
                <p className="text-2xl font-bold text-white">722.1 <span className="text-sm font-normal text-slate-400">gCO2/kWh</span></p>
                <p className="text-xs text-slate-400">Áp dụng cho toàn bộ tính toán phát thải gián tiếp của xe điện trong hệ thống.</p>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
