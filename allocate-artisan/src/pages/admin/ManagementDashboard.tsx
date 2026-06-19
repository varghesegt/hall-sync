import { useQuery } from "@tanstack/react-query";
import { analyticsApi } from "../../api/analyticsApi";
import { PageHeader } from "../../components/ui/PageHeader";
import { Card, CardContent } from "../../components/ui/card";
import { ShieldAlert, Activity, FileCheck, Target } from "lucide-react";

export default function ManagementDashboard() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ["management-dashboard"],
    queryFn: () => analyticsApi.getManagementDashboard().then((res) => res.data),
  });

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
        <Card className="flex flex-col items-center justify-center min-h-[300px] bg-slate-50 border-dashed">
          <CardContent className="text-center">
            <h3 className="text-lg font-semibold text-slate-700 mb-2">Compliance Trend</h3>
            <p className="text-sm text-slate-500 mb-4">Visual chart will be rendered here showing compliance score over the last 12 months.</p>
            <div className="w-16 h-16 rounded-full bg-slate-200 animate-pulse mx-auto" />
          </CardContent>
        </Card>

        <Card className="flex flex-col items-center justify-center min-h-[300px] bg-slate-50 border-dashed">
          <CardContent className="text-center">
            <h3 className="text-lg font-semibold text-slate-700 mb-2">Faculty Deployment</h3>
            <p className="text-sm text-slate-500 mb-4">Visual chart showing department-wise deployment metrics for active exams.</p>
            <div className="w-full max-w-[200px] h-32 bg-slate-200 animate-pulse mx-auto rounded-md" />
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
