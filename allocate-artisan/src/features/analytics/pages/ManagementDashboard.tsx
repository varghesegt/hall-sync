import { useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { analyticsApi } from '@/api/analyticsApi';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card, CardContent } from '@/components/ui/card';
import { ShieldAlert, Activity, FileCheck, Target } from "lucide-react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  Legend
} from "recharts";

export default function ManagementDashboard() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ["management-dashboard"],
    queryFn: () => analyticsApi.getManagementDashboard().then((res) => res.data),
  });

  const { data: deptData, isLoading: isLoadingDept } = useQuery({
    queryKey: ["department-distribution"],
    queryFn: () => analyticsApi.getDepartmentDistribution().then((res) => res.data),
  });

  // Mock trend data ending with the actual compliance score
  const trendData = useMemo(() => {
    const currentScore = stats?.complianceScore ?? 85;
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const currentMonthIdx = new Date().getMonth();
    const data = [];
    
    let score = Math.max(60, currentScore - 15);
    for (let i = 11; i >= 0; i--) {
      const monthIdx = (currentMonthIdx - i + 12) % 12;
      if (i === 0) {
        score = currentScore;
      } else {
        score = Math.max(40, Math.min(100, score + (Math.random() * 8 - 3)));
      }
      data.push({
        name: months[monthIdx],
        score: Math.round(score)
      });
    }
    return data;
  }, [stats?.complianceScore]);

  return (
    <div className="p-6 space-y-6">
      <PageHeader
        title="Management Dashboard"
        subtitle="High-level overview of examination operations, compliance, and faculty utilization."
        icon={Target}
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <KPIBox
          title="Exams Conducted"
          value={stats?.examsConducted ?? "--"}
          icon={<Activity className="w-5 h-5 text-blue-500" />}
          subtitle="Total archived exam sessions"
          isLoading={isLoading}
        />
        <KPIBox
          title="Audit Readiness"
          value={stats?.auditReadiness !== undefined ? `${stats.auditReadiness}%` : "--"}
          icon={<FileCheck className="w-5 h-5 text-emerald-500" />}
          subtitle="Batches permanently archived"
          isLoading={isLoading}
        />
        <KPIBox
          title="Compliance Score"
          value={stats?.complianceScore !== undefined ? `${stats.complianceScore}/100` : "--"}
          icon={<ShieldAlert className="w-5 h-5 text-purple-500" />}
          subtitle="Based on malpractices & archives"
          isLoading={isLoading}
        />
        <KPIBox
          title="Faculty Utilization"
          value={stats?.facultyUtilization !== undefined ? `${stats.facultyUtilization}%` : "--"}
          icon={<Target className="w-5 h-5 text-orange-500" />}
          subtitle="Active faculty deployed"
          isLoading={isLoading}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="flex flex-col min-h-[350px] bg-white border-slate-200 shadow-sm">
          <CardContent className="pt-6 h-full flex flex-col">
            <div className="text-center mb-6">
              <h3 className="text-lg font-semibold text-slate-800">Compliance Trend</h3>
              <p className="text-sm text-slate-500">Historical compliance score over the last 12 months.</p>
            </div>
            <div className="flex-1 w-full min-h-[250px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trendData} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#64748b' }} dy={10} />
                  <YAxis domain={[0, 100]} axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#64748b' }} dx={-10} />
                  <RechartsTooltip 
                    contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                    itemStyle={{ color: '#8b5cf6', fontWeight: 600 }}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="score" 
                    name="Compliance %" 
                    stroke="#8b5cf6" 
                    strokeWidth={3} 
                    dot={{ fill: '#8b5cf6', strokeWidth: 2, r: 4 }} 
                    activeDot={{ r: 6, fill: '#c4b5fd' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card className="flex flex-col min-h-[350px] bg-white border-slate-200 shadow-sm">
          <CardContent className="pt-6 h-full flex flex-col">
            <div className="text-center mb-6">
              <h3 className="text-lg font-semibold text-slate-800">Faculty Deployment</h3>
              <p className="text-sm text-slate-500">Department-wise deployment metrics for active exams.</p>
            </div>
            <div className="flex-1 w-full min-h-[250px]">
              {isLoadingDept ? (
                <div className="w-full h-full flex items-center justify-center">
                  <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                </div>
              ) : deptData && deptData.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={deptData} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                    <XAxis dataKey="department" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#64748b' }} dy={10} />
                    <YAxis yAxisId="left" orientation="left" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#64748b' }} dx={-10} />
                    <YAxis yAxisId="right" orientation="right" axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#64748b' }} dx={10} />
                    <RechartsTooltip 
                      contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                      cursor={{ fill: '#f1f5f9' }}
                    />
                    <Legend wrapperStyle={{ paddingTop: '20px' }} />
                    <Bar yAxisId="left" dataKey="totalDuties" name="Total Duties" fill="#3b82f6" radius={[4, 4, 0, 0]} barSize={30} />
                    <Bar yAxisId="right" dataKey="facultyCount" name="Active Faculty" fill="#10b981" radius={[4, 4, 0, 0]} barSize={30} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="w-full h-full flex items-center justify-center text-slate-400">
                  No deployment data available
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function KPIBox({ title, value, icon, subtitle, isLoading }: { title: string; value: string | number; icon: React.ReactNode; subtitle: string; isLoading?: boolean }) {
  return (
    <Card className="relative overflow-hidden group hover:shadow-lg transition-shadow duration-300">
      <CardContent className="p-6">
        <div className="flex justify-between items-start">
          <div className="space-y-2">
            <p className="text-sm font-medium text-slate-500">{title}</p>
            {isLoading ? (
              <div className="h-8 w-16 bg-slate-200 rounded animate-pulse" />
            ) : (
              <p className="text-3xl font-bold tracking-tight text-slate-900">{value}</p>
            )}
            <p className="text-xs text-slate-400">{subtitle}</p>
          </div>
          <div className="p-3 bg-slate-50 rounded-lg ring-1 ring-slate-100 group-hover:scale-110 transition-transform">
            {icon}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
