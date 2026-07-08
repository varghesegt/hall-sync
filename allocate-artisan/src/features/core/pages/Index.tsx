import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { LogOut, ShieldCheck, GraduationCap, ArrowRight, LayoutGrid, Users, AlertCircle, Search, FileText, Settings } from "lucide-react";
import { toast } from "sonner";
import apiClient from "@/api/axios";

export default function Index() {
  const navigate = useNavigate();
  const userEmail = localStorage.getItem("user_email") || "Admin";

  const handleLogout = async () => {
    try {
      await apiClient.post("/auth/logout");
    } catch (e) {
      console.warn("Logout request failed", e);
    }
    localStorage.removeItem("user_role");
    localStorage.removeItem("tenant_id");
    localStorage.removeItem("user_email");
    toast.success("Logged Out", {
      description: "Session closed successfully.",
    });
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex flex-col relative overflow-hidden font-sans selection:bg-indigo-500/20">
      {/* Elegant Background Gradients */}
      <div className="absolute top-0 left-0 w-full h-[500px] bg-gradient-to-b from-indigo-50/80 to-transparent pointer-events-none" />
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] bg-indigo-500/10 blur-[120px] rounded-full pointer-events-none" />
      <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-purple-500/10 blur-[120px] rounded-full pointer-events-none" />

      <header className="relative z-50 w-full border-b border-slate-200/50 bg-white/70 backdrop-blur-xl supports-[backdrop-filter]:bg-white/60">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 lg:px-8 py-5">
          <div className="flex items-center gap-4">
            <div className="flex h-10 w-10 items-center justify-center overflow-hidden">
              <img src="/logo.png" alt="HallSync Logo" className="w-full h-full object-contain" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
                HallSync <span className="text-indigo-600 font-semibold">Control Center</span>
              </h1>
              <p className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mt-0.5">Production v1.1.0-ELITE</p>
            </div>
          </div>

          <div className="flex items-center gap-6">
            <div className="hidden sm:flex flex-col items-end">
              <span className="text-[10px] uppercase font-bold text-slate-400 tracking-widest">Authenticated</span>
              <span className="text-sm font-semibold text-slate-800">{userEmail}</span>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={handleLogout}
              className="h-9 px-4 border-slate-200 text-slate-600 hover:text-red-600 hover:border-red-200 hover:bg-red-50 gap-2 transition-all rounded-full shadow-sm"
            >
              <LogOut size={14} strokeWidth={2.5} />
              <span className="font-bold text-[11px] uppercase tracking-wider">Sign Out</span>
            </Button>
          </div>
        </div>
      </header>

      <main className="flex-1 w-full relative z-10 flex flex-col items-center justify-center py-16 px-6 lg:px-8">
        <div className="max-w-5xl w-full space-y-16 text-center">
          <div className="space-y-6">
            <h2 className="text-4xl lg:text-6xl font-extrabold text-slate-900 tracking-tight leading-tight">
              Select Allocation <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-purple-600">Mode</span>
            </h2>
            <p className="text-slate-500 max-w-2xl mx-auto font-medium text-lg lg:text-xl leading-relaxed">
              Choose the appropriate seat allocation engine based on the examination cycle. Designed for uncompromising scale and precision.
            </p>
          </div>

          <div className="grid lg:grid-cols-2 gap-8 max-w-4xl mx-auto w-full">
            {/* Semester Exams Card */}
            <Card 
              onClick={() => navigate("/semester")}
              className="bg-white border border-slate-200/60 shadow-[0_8px_30px_rgb(0,0,0,0.04)] hover:shadow-[0_20px_40px_rgb(79,70,229,0.1)] transition-all duration-500 cursor-pointer group hover:-translate-y-2 rounded-3xl overflow-hidden relative"
            >
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-indigo-500 to-blue-500 transform origin-left scale-x-0 group-hover:scale-x-100 transition-transform duration-500" />
              <CardContent className="p-10 lg:p-12 text-left flex flex-col h-full">
                <div className="h-16 w-16 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mb-8 group-hover:scale-110 transition-transform duration-500 shadow-sm border border-indigo-100/50">
                  <GraduationCap size={32} strokeWidth={1.5} />
                </div>
                <h3 className="text-2xl font-bold text-slate-900 mb-2">Semester Exams</h3>
                <p className="text-indigo-600 font-bold uppercase tracking-wider text-xs mb-6">External Seating Engine</p>
                <p className="text-slate-600 leading-relaxed mb-10 flex-grow">
                  Configured for End Semester Exams with dynamic seating, complete department/subject separation, and validation reports.
                </p>
                <div className="inline-flex items-center text-indigo-600 font-bold text-sm group-hover:translate-x-2 transition-transform duration-300">
                  Launch External Engine <ArrowRight size={16} className="ml-2" />
                </div>
              </CardContent>
            </Card>

            {/* Internal Exams Card */}
            <Card 
              onClick={() => navigate("/internal")}
              className="bg-white border border-slate-200/60 shadow-[0_8px_30px_rgb(0,0,0,0.04)] hover:shadow-[0_20px_40px_rgb(16,185,129,0.1)] transition-all duration-500 cursor-pointer group hover:-translate-y-2 rounded-3xl overflow-hidden relative"
            >
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-emerald-400 to-teal-500 transform origin-left scale-x-0 group-hover:scale-x-100 transition-transform duration-500" />
              <CardContent className="p-10 lg:p-12 text-left flex flex-col h-full">
                <div className="h-16 w-16 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-8 group-hover:scale-110 transition-transform duration-500 shadow-sm border border-emerald-100/50">
                  <LayoutGrid size={32} strokeWidth={1.5} />
                </div>
                <h3 className="text-2xl font-bold text-slate-900 mb-2">Internal Exams</h3>
                <p className="text-emerald-600 font-bold uppercase tracking-wider text-xs mb-6">Continuous Assessment Engine</p>
                <p className="text-slate-600 leading-relaxed mb-10 flex-grow">
                  Configured for mid-semester assessments using flexible seating layouts, support for multiple courses per hall, and streamlined QP rosters.
                </p>
                <div className="inline-flex items-center text-emerald-600 font-bold text-sm group-hover:translate-x-2 transition-transform duration-300">
                  Launch Internal Engine <ArrowRight size={16} className="ml-2" />
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="pt-12 border-t border-slate-200/60 w-full max-w-5xl mx-auto">
            <h3 className="text-xl lg:text-2xl font-bold text-slate-800 tracking-tight mb-8">
              Core Modules
            </h3>
            
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-7 gap-4">
              {[
                { name: 'Faculty', icon: Users, color: 'text-indigo-600', bg: 'bg-indigo-50', border: 'hover:border-indigo-500', path: '/faculty' },
                { name: 'Duty Engine', icon: ShieldCheck, color: 'text-blue-600', bg: 'bg-blue-50', border: 'hover:border-blue-500', path: '/duties' },
                { name: 'Malpractice', icon: AlertCircle, color: 'text-rose-600', bg: 'bg-rose-50', border: 'hover:border-rose-500', path: '/malpractice' },
                { name: 'Analytics', icon: LayoutGrid, color: 'text-amber-600', bg: 'bg-amber-50', border: 'hover:border-amber-500', path: '/analytics' },
                { name: 'Audit Vault', icon: Search, color: 'text-purple-600', bg: 'bg-purple-50', border: 'hover:border-purple-500', path: '/history' },
                { name: 'Reports', icon: FileText, color: 'text-cyan-600', bg: 'bg-cyan-50', border: 'hover:border-cyan-500', path: '/reports' },
                { name: 'Settings', icon: Settings, color: 'text-slate-600', bg: 'bg-slate-100', border: 'hover:border-slate-500', path: '/settings' }
              ].map((module, idx) => (
                <button 
                  key={idx}
                  onClick={() => navigate(module.path)}
                  className={`flex flex-col items-center justify-center p-6 bg-white border border-slate-200/80 rounded-2xl shadow-sm ${module.border} hover:shadow-lg transition-all duration-300 gap-3 group`}
                >
                  <div className={`p-3 ${module.bg} rounded-xl group-hover:scale-110 transition-transform duration-300`}>
                    <module.icon className={module.color} size={24} strokeWidth={1.5} />
                  </div>
                  <span className="text-[13px] font-bold text-slate-700">{module.name}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </main>

      <footer className="relative z-50 py-8 border-t border-slate-200/50 bg-white/50 backdrop-blur-md text-center mt-auto">
        <p className="text-[11px] font-black text-slate-400 uppercase tracking-widest">
          Hall<span className="text-slate-600">Sync Control Center</span>
        </p>
      </footer>
    </div>
  );
}

