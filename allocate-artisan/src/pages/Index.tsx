import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { LogOut, ShieldCheck, GraduationCap, ArrowRight, LayoutGrid, Users, AlertCircle, Search, FileText } from "lucide-react";
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
    <div className="min-h-screen bg-slate-50/50 mesh-gradient flex flex-col selection:bg-primary/10">
      <header className="glass border-b border-white/20 sticky top-0 z-50 shadow-sm transition-all duration-300">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4 w-full">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center overflow-hidden transition-colors animate-in zoom-in duration-700">
              <img src="/logo.png" alt="HallSync Logo" className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-500" />
            </div>
            <div className="animate-in slide-in-from-left duration-700">
              <h1 className="text-2xl font-black tracking-tighter text-slate-900 flex items-center gap-2">
                HallSync <span className="text-primary/80 font-medium tracking-normal text-lg">Control Center</span>
              </h1>
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-tighter block mt-0.5">Production v1.1.0-ELITE</span>
            </div>
          </div>

          <div className="flex items-center gap-5">
            <div className="hidden md:flex flex-col items-end">
              <span className="text-[10px] uppercase font-black text-slate-400 tracking-[0.1em]">Authenticated</span>
              <span className="text-sm font-bold text-slate-700 mt-0.5">{userEmail}</span>
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={handleLogout}
              className="h-10 px-4 border-slate-200 bg-white/50 backdrop-blur-sm text-slate-600 hover:text-destructive hover:border-destructive/30 hover:bg-destructive/5 gap-2 transition-all shadow-sm rounded-xl active:scale-[0.98]"
            >
              <LogOut size={16} strokeWidth={2.5} />
              <span className="font-bold text-xs uppercase tracking-wider">Secure Sign Out</span>
            </Button>
          </div>
        </div>
      </header>

      <main className="flex-1 flex items-center justify-center py-16 px-6">
        <div className="max-w-4xl w-full space-y-10 text-center">
          <div className="space-y-4 animate-in fade-in duration-1000">
            <h2 className="text-4xl md:text-5xl font-extrabold text-slate-900 tracking-tight">
              Select Allocation <span className="text-primary">Mode</span>
            </h2>
            <p className="text-slate-500 max-w-xl mx-auto font-medium text-lg">
              Choose the appropriate seat allocation engine based on the examination cycle.
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-8 max-w-3xl mx-auto">
            {/* Semester Exams Card */}
            <Card 
              onClick={() => navigate("/semester")}
              className="glass border-white/20 hover:border-primary/30 shadow-xl hover:shadow-primary/5 transition-all duration-300 cursor-pointer group hover:-translate-y-1 overflow-hidden"
            >
              <div className="h-2 bg-primary w-full" />
              <CardHeader className="space-y-4 pt-8">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10 text-primary group-hover:scale-110 transition-transform duration-500">
                  <GraduationCap size={32} strokeWidth={1.5} />
                </div>
                <div className="space-y-2">
                  <CardTitle className="text-2xl font-bold">Semester Exams</CardTitle>
                  <CardDescription className="text-slate-400 font-bold uppercase tracking-wider text-xs">External Seating Engine</CardDescription>
                </div>
              </CardHeader>
              <CardContent className="space-y-6 pb-8">
                <p className="text-sm text-slate-500 leading-relaxed min-h-[60px]">
                  Configured for End Semester Exams with dynamic seating, complete department/subject separation, and validation reports.
                </p>
                <Button className="w-full h-11 bg-primary hover:bg-primary/90 rounded-xl gap-2 font-bold transition-all shadow-md group-hover:shadow-primary/25">
                  Launch External Engine
                  <ArrowRight size={16} className="group-hover:translate-x-1 transition-transform" />
                </Button>
              </CardContent>
            </Card>

            {/* Internal Exams Card */}
            <Card 
              onClick={() => navigate("/internal")}
              className="glass border-white/20 hover:border-emerald-500/30 shadow-xl hover:shadow-emerald-500/5 transition-all duration-300 cursor-pointer group hover:-translate-y-1 overflow-hidden"
            >
              <div className="h-2 bg-emerald-500 w-full" />
              <CardHeader className="space-y-4 pt-8">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-500/10 text-emerald-600 group-hover:scale-110 transition-transform duration-500">
                  <LayoutGrid size={32} strokeWidth={1.5} />
                </div>
                <div className="space-y-2">
                  <CardTitle className="text-2xl font-bold">Internal Exams</CardTitle>
                  <CardDescription className="text-emerald-600 font-bold uppercase tracking-wider text-xs">Continuous Assessment Engine</CardDescription>
                </div>
              </CardHeader>
              <CardContent className="space-y-6 pb-8">
                <p className="text-sm text-slate-500 leading-relaxed min-h-[60px]">
                  Configured for mid-semester assessments using flexible seating layouts, support for multiple courses per hall, and streamlined QP rosters.
                </p>
                <Button className="w-full h-11 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl gap-2 font-bold transition-all shadow-md group-hover:shadow-emerald-500/25">
                  Launch Internal Engine
                  <ArrowRight size={16} className="group-hover:translate-x-1 transition-transform" />
                </Button>
              </CardContent>
            </Card>
          </div>

          <div className="mt-12 space-y-4 animate-in fade-in duration-1000 delay-300">
            <h3 className="text-xl font-bold text-slate-800 tracking-tight">
              Enterprise Modules
            </h3>
            
            <div className="grid grid-cols-2 md:grid-cols-6 gap-4 max-w-4xl mx-auto">
              <button 
                onClick={() => navigate('/faculty')}
                className="flex flex-col items-center justify-center p-4 bg-white border rounded-xl shadow-sm hover:border-emerald-500 hover:shadow-md transition-all gap-2 group"
              >
                <div className="p-2 bg-emerald-50 rounded-lg group-hover:scale-110 transition-transform">
                  <Users className="text-emerald-600" size={24} />
                </div>
                <span className="text-xs font-bold text-slate-700 mt-1">Faculty</span>
              </button>
              
              <button 
                onClick={() => navigate('/duties')}
                className="flex flex-col items-center justify-center p-4 bg-white border rounded-xl shadow-sm hover:border-blue-500 hover:shadow-md transition-all gap-2 group"
              >
                <div className="p-2 bg-blue-50 rounded-lg group-hover:scale-110 transition-transform">
                  <ShieldCheck className="text-blue-600" size={24} />
                </div>
                <span className="text-xs font-bold text-slate-700 mt-1">Duty Engine</span>
              </button>

              <button 
                onClick={() => navigate('/malpractice')}
                className="flex flex-col items-center justify-center p-4 bg-white border rounded-xl shadow-sm hover:border-rose-500 hover:shadow-md transition-all gap-2 group"
              >
                <div className="p-2 bg-rose-50 rounded-lg group-hover:scale-110 transition-transform">
                  <AlertCircle className="text-rose-600" size={24} />
                </div>
                <span className="text-xs font-bold text-slate-700 mt-1">Malpractice</span>
              </button>

              <button 
                onClick={() => navigate('/analytics')}
                className="flex flex-col items-center justify-center p-4 bg-white border rounded-xl shadow-sm hover:border-amber-500 hover:shadow-md transition-all gap-2 group"
              >
                <div className="p-2 bg-amber-50 rounded-lg group-hover:scale-110 transition-transform">
                  <LayoutGrid className="text-amber-600" size={24} />
                </div>
                <span className="text-xs font-bold text-slate-700 mt-1">Analytics</span>
              </button>

              <button 
                onClick={() => navigate('/history')}
                className="flex flex-col items-center justify-center p-4 bg-white border rounded-xl shadow-sm hover:border-purple-500 hover:shadow-md transition-all gap-2 group"
              >
                <div className="p-2 bg-purple-50 rounded-lg group-hover:scale-110 transition-transform">
                  <Search className="text-purple-600" size={24} />
                </div>
                <span className="text-xs font-bold text-slate-700 mt-1">Audit Vault</span>
              </button>

              <button 
                onClick={() => navigate('/reports')}
                className="flex flex-col items-center justify-center p-4 bg-white border rounded-xl shadow-sm hover:border-cyan-500 hover:shadow-md transition-all gap-2 group"
              >
                <div className="p-2 bg-cyan-50 rounded-lg group-hover:scale-110 transition-transform">
                  <FileText className="text-cyan-600" size={24} />
                </div>
                <span className="text-xs font-bold text-slate-700 mt-1">Reports</span>
              </button>

              <button 
                onClick={() => navigate('/settings')}
                className="flex flex-col items-center justify-center p-4 bg-white border rounded-xl shadow-sm hover:border-gray-500 hover:shadow-md transition-all gap-2 group md:col-span-6 max-w-[200px] mx-auto mt-4 w-full"
              >
                <div className="p-2 bg-gray-50 rounded-lg group-hover:scale-110 transition-transform">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-gray-600 lucide lucide-settings"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
                </div>
                <span className="text-xs font-bold text-slate-700 mt-1">Settings</span>
              </button>
            </div>
          </div>
        </div>
      </main>

      <footer className="py-6 border-t border-slate-200/80 bg-white/50 backdrop-blur-md text-center">
        <p className="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em]">
          Hall<span className="text-slate-600">Sync CONTROL CENTER</span>
        </p>
      </footer>
    </div>
  );
}

