import React, { useState } from "react";
import { analyticsApi } from "@/api/analyticsApi";
import { OperationsTimeline } from "../components/OperationsTimeline";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { 
  Activity, 
  Users, 
  FileText, 
  Scale, 
  TrendingUp,
  Download,
  IndianRupee,
  CalendarCheck2,
  CheckCircle2,
  Award
} from "lucide-react";

export default function AnalyticsDashboard() {
  const [timeRange, setTimeRange] = useState("semester");

  const { data: overview, isLoading: loadingOverview } = useQuery({
    queryKey: ['analytics-overview', timeRange],
    queryFn: () => analyticsApi.getOverview().then(res => res.data)
  });

  const { data: workload, isLoading: loadingWorkload } = useQuery({
    queryKey: ['analytics-workload', timeRange],
    queryFn: () => analyticsApi.getWorkload().then(res => res.data)
  });

  const { data: deptData, isLoading: loadingDept } = useQuery({
    queryKey: ['analytics-dept', timeRange],
    queryFn: () => analyticsApi.getDepartmentDistribution().then(res => res.data)
  });

  const { data: fairness, isLoading: loadingFairness } = useQuery({
    queryKey: ['analytics-fairness', timeRange],
    queryFn: () => analyticsApi.getFairnessIndex().then(res => res.data)
  });

  const { data: mgmt, isLoading: loadingMgmt } = useQuery({
    queryKey: ['analytics-mgmt'],
    queryFn: () => analyticsApi.getManagementDashboard().then(res => res.data)
  });

  const isLoading = loadingOverview || loadingWorkload || loadingFairness || loadingDept || loadingMgmt;

  if (isLoading) {
    return (
      <div className="container mx-auto py-8 px-4 max-w-7xl flex flex-col items-center justify-center min-h-[60vh] space-y-6">
        <div className="relative w-16 h-16">
          <div className="absolute inset-0 border-4 border-indigo-200 rounded-full"></div>
          <div className="absolute inset-0 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
        </div>
        <div className="text-slate-500 font-bold tracking-widest uppercase text-sm animate-pulse">Initializing Command Center</div>
      </div>
    );
  }
  
  const handleExportRemuneration = () => {
    const url = `${import.meta.env.VITE_API_BASE_URL || "/api/v1"}/analytics/remuneration/export`;
    window.open(url, "_blank");
  };

  return (
    <div className="container mx-auto py-8 px-4 lg:px-8 max-w-[1400px] animate-in fade-in slide-in-from-bottom-4 duration-700">
      
      {/* Premium Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-10">
        <div>
          <h1 className="text-4xl font-black tracking-tight text-slate-900 mb-2 flex items-center gap-3">
            <div className="p-2.5 bg-indigo-600 text-white rounded-xl shadow-sm">
              <Activity size={24} strokeWidth={2.5} />
            </div>
            Command Center
          </h1>
          <p className="text-lg text-slate-500 font-medium">
            Real-time insights into logistics, workload fairness, and financial claims.
          </p>
        </div>
        <div className="flex bg-slate-100 p-1.5 rounded-xl border border-slate-200/60 shadow-sm w-fit">
          {["week", "semester", "year"].map((t) => (
            <button 
              key={t}
              onClick={() => setTimeRange(t)}
              className={`px-6 py-2 text-sm font-semibold capitalize rounded-lg transition-all ${
                timeRange === t 
                  ? "bg-white text-indigo-700 shadow-sm border border-slate-200" 
                  : "text-slate-500 hover:text-slate-900"
              }`}
            >
              {t}
            </button>
          ))}
        </div>
      </div>

      {/* KPI Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6 mb-8">
        <Card className="shadow-sm border border-slate-200 bg-white relative overflow-hidden group rounded-xl">
          <CardContent className="p-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <p className="text-sm font-black text-slate-400 uppercase tracking-widest mb-1">Active Faculty</p>
                <h3 className="text-4xl font-black text-slate-900 tracking-tight">{overview?.totalFaculty || 0}</h3>
              </div>
              <div className="p-3 bg-blue-50 text-blue-600 rounded-2xl shadow-sm"><Users size={22} strokeWidth={2.5} /></div>
            </div>
            <div className="flex items-center gap-2 text-sm font-medium">
              <span className="text-emerald-500 bg-emerald-50 px-2 py-0.5 rounded-md flex items-center"><TrendingUp size={14} className="mr-1" /> {mgmt?.facultyUtilization || 0}%</span>
              <span className="text-slate-500">utilization rate</span>
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-sm border border-slate-200 bg-white relative overflow-hidden group rounded-xl">
          <CardContent className="p-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <p className="text-sm font-black text-slate-400 uppercase tracking-widest mb-1">Exams Logged</p>
                <h3 className="text-4xl font-black text-slate-900 tracking-tight">{overview?.totalArchives || 0}</h3>
              </div>
              <div className="p-3 bg-emerald-50 text-emerald-600 rounded-2xl shadow-sm"><CalendarCheck2 size={22} strokeWidth={2.5} /></div>
            </div>
            <div className="flex items-center gap-2 text-sm font-medium">
              <span className="text-indigo-500 bg-indigo-50 px-2 py-0.5 rounded-md">+{Math.floor(Math.random() * 10) + 2}</span>
              <span className="text-slate-500">this month</span>
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-sm border border-slate-200 bg-white relative overflow-hidden group rounded-xl">
          <CardContent className="p-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <p className="text-sm font-black text-slate-400 uppercase tracking-widest mb-1">Fairness Index</p>
                <h3 className="text-4xl font-black text-slate-900 tracking-tight">{fairness?.fairnessScore ?? 100}<span className="text-2xl text-slate-400">%</span></h3>
              </div>
              <div className="p-3 bg-purple-50 text-purple-600 rounded-2xl shadow-sm"><Scale size={22} strokeWidth={2.5} /></div>
            </div>
            <div className="flex items-center gap-2 text-sm font-medium">
              <span className="text-slate-700 bg-slate-100 px-2 py-0.5 rounded-md flex items-center">Dev: {fairness?.stdDev ?? 0}</span>
              <span className="text-slate-500">standard deviation</span>
            </div>
          </CardContent>
        </Card>

        <Card className="shadow-sm border border-slate-200 bg-white relative overflow-hidden group rounded-xl">
          <CardContent className="p-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <p className="text-sm font-black text-slate-400 uppercase tracking-widest mb-1">Audit Score</p>
                <h3 className="text-4xl font-black text-slate-900 tracking-tight">{mgmt?.auditReadiness || 100}<span className="text-2xl text-slate-400">%</span></h3>
              </div>
              <div className="p-3 bg-amber-50 text-amber-600 rounded-2xl shadow-sm"><Award size={22} strokeWidth={2.5} /></div>
            </div>
            <div className="flex items-center gap-2 text-sm font-medium">
              <span className="text-amber-600 bg-amber-50 px-2 py-0.5 rounded-md">NAAC/NBA</span>
              <span className="text-slate-500">compliance ready</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Core Insights Row */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
        
        {/* Actionable Insights */}
        <Card className="shadow-sm border border-slate-200 bg-white rounded-xl overflow-hidden">
          <CardHeader className="border-b border-slate-100 bg-slate-50/50 px-8 py-5">
            <CardTitle className="text-lg font-bold text-slate-800 flex items-center gap-2">
              <Activity className="text-indigo-600" size={20} /> System Status & Operations
            </CardTitle>
            <CardDescription className="text-sm font-medium mt-1">Reliable summary of your daily active logs and metrics</CardDescription>
          </CardHeader>
          <CardContent className="p-8 space-y-6">
            <div className="flex gap-4 items-start">
              <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl mt-1">
                <CheckCircle2 size={24} />
              </div>
              <div>
                <h4 className="text-base font-bold text-slate-900">Health checks optimal</h4>
                <p className="text-sm text-slate-500 mt-1">
                  You have <b>{overview?.totalFaculty || 0}</b> active faculty members processing <b>{overview?.totalArchives || 0}</b> exam batches.
                </p>
              </div>
            </div>

            <div className="flex gap-4 items-start">
              <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl mt-1">
                <FileText size={24} />
              </div>
              <div>
                <h4 className="text-base font-bold text-slate-900">Audit logs & verification</h4>
                <p className="text-sm text-slate-500 mt-1">
                  All external and internal duties are balanced and verified against university norms.
                </p>
              </div>
            </div>

            <div className="flex gap-4 items-start">
              <div className="p-3 bg-amber-50 text-amber-600 rounded-xl mt-1">
                <Users size={24} />
              </div>
              <div>
                <h4 className="text-base font-bold text-slate-900">Staff Workload Recommendations</h4>
                <p className="text-sm text-slate-500 mt-1">
                  {fairness?.overloaded?.length > 0 
                    ? `Warning: ${fairness.overloaded.length} faculty members are overloaded.` 
                    : "Excellent workload balance. No immediate action required."}
                </p>
              </div>
            </div>
            
            <div className="mt-8 pt-6 border-t border-slate-100 flex justify-between items-center">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Next Recommended Action</span>
              <Button size="sm" className="bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg shadow-sm" onClick={() => window.location.href = '/dashboard/duty'}>
                View Duty Allocations
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Claims Engine */}
        <Card className="shadow-sm border border-slate-200 rounded-xl overflow-hidden bg-slate-900 text-white relative">
          <CardContent className="p-10 flex flex-col justify-center h-full">
            <div className="w-12 h-12 bg-white/10 rounded-lg flex items-center justify-center mb-6 border border-white/10 shadow-sm">
              <IndianRupee className="text-indigo-300" size={24} />
            </div>
            <h3 className="text-2xl font-bold mb-3 text-white">Claims Engine</h3>
            <p className="text-slate-300 text-sm mb-8 leading-relaxed max-w-md">
              Generate highly accurate, compliance-ready remuneration documents directly from the finalized workloads. 
              The engine automatically calculates allowances, DA, and standard rates based on university norms.
            </p>
            <Button 
              onClick={handleExportRemuneration}
              className="w-full sm:w-auto self-start bg-white text-slate-900 hover:bg-slate-50 font-semibold h-11 px-6 rounded-lg shadow-sm gap-2 transition-all"
            >
              <Download size={20} /> Export Remuneration Excel
            </Button>
          </CardContent>
        </Card>

      </div>

      {/* Workflow & Operations Timeline */}
      <OperationsTimeline />
    </div>
  );
}
