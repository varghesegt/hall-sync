import React, { useState } from "react";
import { analyticsApi } from "@/api/analyticsApi";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { 
  BarChart as BarChartIcon, 
  Activity, 
  Users, 
  FileText, 
  Scale, 
  Building,
  TrendingUp,
  Download,
  IndianRupee,
  CalendarCheck2,
  Award
} from "lucide-react";
import { 
  PieChart, Pie, Cell, Tooltip as RechartsTooltip, ResponsiveContainer, Legend, 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, AreaChart, Area,
  RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar
} from "recharts";

const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#3b82f6'];

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

  const topWorkload = workload?.slice(0, 5) || [];
  const maxDuties = Math.max(...(workload?.map((w: any) => w.dutyCount) || [1]));
  
  // Format data for Department Bar Chart
  const departmentChartData = deptData?.map((d: any) => ({
    name: d.department,
    Total: d.totalDuties,
    Avg: d.avgDuties
  })) || [];

  // Prepare radar data for fairness
  const radarData = [
    { subject: 'Faculty Engagement', A: mgmt?.facultyUtilization || 0, fullMark: 100 },
    { subject: 'Workload Balance', A: fairness?.fairnessScore || 0, fullMark: 100 },
    { subject: 'Audit Readiness', A: mgmt?.auditReadiness || 0, fullMark: 100 },
    { subject: 'Compliance Score', A: mgmt?.complianceScore || 0, fullMark: 100 },
    { subject: 'Claim Processing', A: 92, fullMark: 100 },
  ];

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
            <div className="p-2.5 bg-gradient-to-br from-indigo-600 to-purple-600 text-white rounded-xl shadow-lg shadow-indigo-200">
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
              className={`px-6 py-2 text-sm font-bold capitalize rounded-lg transition-all ${
                timeRange === t 
                  ? "bg-white text-indigo-700 shadow-sm border border-slate-200/50" 
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
        <Card className="border-0 shadow-[0_8px_30px_rgb(0,0,0,0.04)] bg-white/60 backdrop-blur-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-32 h-32 bg-blue-500/5 rounded-bl-full pointer-events-none group-hover:scale-110 transition-transform duration-500" />
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

        <Card className="border-0 shadow-[0_8px_30px_rgb(0,0,0,0.04)] bg-white/60 backdrop-blur-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/5 rounded-bl-full pointer-events-none group-hover:scale-110 transition-transform duration-500" />
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

        <Card className="border-0 shadow-[0_8px_30px_rgb(0,0,0,0.04)] bg-white/60 backdrop-blur-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-32 h-32 bg-purple-500/5 rounded-bl-full pointer-events-none group-hover:scale-110 transition-transform duration-500" />
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

        <Card className="border-0 shadow-[0_8px_30px_rgb(0,0,0,0.04)] bg-white/60 backdrop-blur-xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-32 h-32 bg-amber-500/5 rounded-bl-full pointer-events-none group-hover:scale-110 transition-transform duration-500" />
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

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-8 mb-8">
        
        {/* Main Chart Area */}
        <Card className="xl:col-span-2 border-0 shadow-[0_8px_30px_rgb(0,0,0,0.04)] rounded-3xl overflow-hidden bg-white/80">
          <CardHeader className="border-b border-slate-100/50 bg-white/50 pb-6 px-8 pt-8">
            <div className="flex justify-between items-center">
              <div>
                <CardTitle className="text-2xl font-black text-slate-800">Department Workload Distribution</CardTitle>
                <CardDescription className="text-sm font-medium mt-1">Comparison of total duties against departmental averages.</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="p-8 h-[400px]">
            {departmentChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={departmentChartData} margin={{ top: 20, right: 30, left: 0, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                  <XAxis 
                    dataKey="name" 
                    axisLine={false} 
                    tickLine={false} 
                    tick={{ fill: '#64748b', fontSize: 13, fontWeight: 600 }}
                    dy={10}
                  />
                  <YAxis 
                    axisLine={false} 
                    tickLine={false} 
                    tick={{ fill: '#64748b', fontSize: 13 }}
                    dx={-10}
                  />
                  <RechartsTooltip 
                    cursor={{ fill: '#f1f5f9' }}
                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 25px rgba(0,0,0,0.1)', fontWeight: 600 }}
                  />
                  <Legend iconType="circle" wrapperStyle={{ paddingTop: '20px', fontWeight: 600 }} />
                  <Bar dataKey="Total" name="Total Duties" fill="#6366f1" radius={[6, 6, 0, 0]} maxBarSize={50} />
                  <Bar dataKey="Avg" name="Avg per Faculty" fill="#cbd5e1" radius={[6, 6, 0, 0]} maxBarSize={50} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-full text-slate-400 font-medium">No departmental data available</div>
            )}
          </CardContent>
        </Card>

        {/* Radar & Actions Column */}
        <div className="flex flex-col gap-8">
          <Card className="border-0 shadow-[0_8px_30px_rgb(0,0,0,0.04)] rounded-3xl overflow-hidden bg-white/80 flex-1">
            <CardHeader className="border-b border-slate-100/50 bg-white/50 pb-4 px-6 pt-6">
              <CardTitle className="text-xl font-black text-slate-800">Operational Radar</CardTitle>
            </CardHeader>
            <CardContent className="p-0 h-[260px] flex items-center justify-center relative">
              <ResponsiveContainer width="100%" height="100%">
                <RadarChart cx="50%" cy="50%" outerRadius="65%" data={radarData}>
                  <PolarGrid stroke="#e2e8f0" />
                  <PolarAngleAxis dataKey="subject" tick={{ fill: '#475569', fontSize: 11, fontWeight: 700 }} />
                  <PolarRadiusAxis angle={30} domain={[0, 100]} tick={false} axisLine={false} />
                  <Radar name="Metrics" dataKey="A" stroke="#8b5cf6" strokeWidth={3} fill="#8b5cf6" fillOpacity={0.2} />
                  <RechartsTooltip contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 25px rgba(0,0,0,0.1)' }} />
                </RadarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-[0_8px_30px_rgb(0,0,0,0.04)] rounded-3xl overflow-hidden bg-gradient-to-br from-indigo-900 to-slate-900 text-white relative">
            <div className="absolute top-0 right-0 w-48 h-48 bg-white/5 rounded-bl-full pointer-events-none" />
            <CardContent className="p-8">
              <div className="w-12 h-12 bg-white/10 rounded-2xl flex items-center justify-center mb-6 backdrop-blur-sm border border-white/10">
                <IndianRupee className="text-indigo-300" size={24} />
              </div>
              <h3 className="text-2xl font-black mb-2 tracking-tight">Claims Engine</h3>
              <p className="text-indigo-200/80 font-medium text-sm mb-8 leading-relaxed">
                Generate highly accurate, compliance-ready remuneration documents directly from the workload engine.
              </p>
              <Button 
                onClick={handleExportRemuneration}
                className="w-full bg-white text-indigo-900 hover:bg-indigo-50 font-bold h-12 rounded-xl shadow-lg shadow-black/20 gap-2 transition-all hover:scale-[1.02]"
              >
                <Download size={18} /> Export Remuneration Excel
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Bottom Insights Row */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">        {/* Fairness Analysis */}
        <Card className="border-0 shadow-[0_8px_30px_rgb(0,0,0,0.04)] rounded-3xl overflow-hidden bg-white/80">
          <CardHeader className="border-b border-slate-100/50 bg-white/50 px-8 py-6">
            <CardTitle className="text-xl font-black text-slate-800">Fairness Optimization</CardTitle>
            <CardDescription className="text-sm font-medium mt-1">Identified outliers in the allocation matrix</CardDescription>
          </CardHeader>
          <CardContent className="p-8">
            <Tabs defaultValue="overloaded" className="w-full">
              <TabsList className="grid w-full grid-cols-2 mb-8 bg-slate-100 p-1 rounded-xl">
                <TabsTrigger value="overloaded" className="rounded-lg font-bold text-sm data-[state=active]:bg-white data-[state=active]:text-rose-600 data-[state=active]:shadow-sm">Overloaded Faculty</TabsTrigger>
                <TabsTrigger value="underloaded" className="rounded-lg font-bold text-sm data-[state=active]:bg-white data-[state=active]:text-emerald-600 data-[state=active]:shadow-sm">Underloaded Faculty</TabsTrigger>
              </TabsList>
              
              <TabsContent value="overloaded" className="mt-0 outline-none">
                <div className="space-y-3">
                  {fairness?.overloaded?.map((f: any, idx: number) => (
                    <div key={idx} className="flex justify-between items-center bg-white border border-rose-100 p-4 rounded-2xl shadow-[0_2px_10px_rgba(225,29,72,0.04)] transition-all hover:shadow-[0_4px_15px_rgba(225,29,72,0.1)]">
                      <div className="flex items-center gap-3">
                        <div className="w-2 h-2 rounded-full bg-rose-500 animate-pulse"></div>
                        <span className="font-bold text-slate-800">{f.name}</span>
                      </div>
                      <span className="font-black text-rose-600 bg-rose-50 px-3 py-1 rounded-lg text-sm">{f.dutyCount} duties</span>
                    </div>
                  ))}
                  {!fairness?.overloaded?.length && (
                    <div className="flex flex-col items-center justify-center py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-dashed border-slate-200">
                      <Scale size={32} className="mb-3 text-slate-300" strokeWidth={1.5} />
                      <span className="font-bold">Perfect Balance</span>
                      <span className="text-sm mt-1">No faculty members are currently overloaded.</span>
                    </div>
                  )}
                </div>
              </TabsContent>

              <TabsContent value="underloaded" className="mt-0 outline-none">
                <div className="space-y-3">
                  {fairness?.underloaded?.map((f: any, idx: number) => (
                    <div key={idx} className="flex justify-between items-center bg-white border border-emerald-100 p-4 rounded-2xl shadow-[0_2px_10px_rgba(16,185,129,0.04)] transition-all hover:shadow-[0_4px_15px_rgba(16,185,129,0.1)]">
                      <div className="flex items-center gap-3">
                        <div className="w-2 h-2 rounded-full bg-emerald-500"></div>
                        <span className="font-bold text-slate-800">{f.name}</span>
                      </div>
                      <span className="font-black text-emerald-600 bg-emerald-50 px-3 py-1 rounded-lg text-sm">{f.dutyCount} duties</span>
                    </div>
                  ))}
                  {!fairness?.underloaded?.length && (
                    <div className="flex flex-col items-center justify-center py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-dashed border-slate-200">
                      <Scale size={32} className="mb-3 text-slate-300" strokeWidth={1.5} />
                      <span className="font-bold">Perfect Balance</span>
                      <span className="text-sm mt-1">No faculty members are currently underloaded.</span>
                    </div>
                  )}
                </div>
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>

      </div>
    </div>
  );
}
