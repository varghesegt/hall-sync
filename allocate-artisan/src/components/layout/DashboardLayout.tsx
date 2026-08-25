import { useState, useEffect } from "react";
import { Outlet, useNavigate, useLocation, Navigate } from "react-router-dom";
import { SidebarProvider, SidebarInset, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "./AppSidebar";
import { Button } from "@/components/ui/button";
import { LogOut, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Separator } from "@/components/ui/separator";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/api/axios";

export function DashboardLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();

  const handleLogout = async () => {
    try {
      await apiClient.post("/auth/logout");
    } catch (e) {
      console.warn("Logout API failed, continuing with local cleanup");
    } finally {
      localStorage.removeItem("coe_auth");
      toast.success("Logged Out", { description: "Session closed successfully." });
      navigate("/login");
    }
  };

  useEffect(() => {
    const handleUnauthorized = () => {
      localStorage.removeItem("coe_auth");
      toast.error("Session Expired", { description: "Please log in again." });
      navigate("/login");
    };

    window.addEventListener("hallsync:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("hallsync:unauthorized", handleUnauthorized);
  }, [navigate]);

  const { data: settings, isLoading } = useQuery({
    queryKey: ["tenant-settings"],
    queryFn: async () => {
      const res = await apiClient.get("/settings");
      return res.data;
    },
    staleTime: Infinity,
  });

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50/50">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  // Force onboarding if not configured
  if (settings && settings.isConfigured === false && location.pathname !== "/dashboard/settings") {
    toast.warning("Initial Setup Required", { description: "Please complete college configuration first." });
    return <Navigate to="/dashboard/settings" replace />;
  }

  const userEmail = localStorage.getItem("user_email") || "coe1@krce.ac.in";

  return (
    <SidebarProvider>
      <AppSidebar settings={settings} />
      <SidebarInset className="bg-slate-50 min-w-0 flex-1 w-full max-w-full">
        <header className="flex h-[72px] shrink-0 items-center gap-2 border-b border-slate-200 bg-white px-4 md:px-8 transition-[width,height] ease-linear group-has-[[data-collapsible=icon]]/sidebar-wrapper:h-16 sticky top-0 z-50 min-w-0 w-full max-w-full shadow-sm">
          <div className="flex items-center gap-4 px-2 w-full min-w-0">
            <SidebarTrigger className="-ml-2 shrink-0 hover:bg-slate-100 hover:text-indigo-600 transition-colors rounded-xl" />
            <Separator orientation="vertical" className="mr-2 h-6 bg-slate-200 shrink-0" />
            <div className="flex-1 min-w-0" />
            
            <div className="flex items-center gap-6 shrink-0">
              <div className="hidden md:flex flex-col items-end mr-2 justify-center">
                <span className="text-[13px] font-extrabold text-slate-800 tracking-tight">{userEmail}</span>
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={handleLogout}
                className="group h-10 px-5 border-slate-200 bg-white text-slate-700 hover:bg-slate-100 hover:text-slate-900 shadow-sm gap-2 transition-all duration-300 rounded-lg"
              >
                <LogOut size={16} strokeWidth={2.5} className="transition-transform duration-300 group-hover:-translate-x-0.5" />
                <span className="font-bold text-[12px] uppercase tracking-widest hidden sm:inline-block">Sign Out</span>
              </Button>
            </div>
          </div>
        </header>

        <main className="flex-1 p-4 md:p-8 min-w-0 w-full max-w-full overflow-hidden">
          <Outlet />
        </main>
      </SidebarInset>
    </SidebarProvider>
  );
}

export default DashboardLayout;
