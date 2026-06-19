import { useState, useEffect } from "react";
import { analyticsApi } from "@/api/analyticsApi";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import { BarChart, Activity, Users, FileWarning, FileText } from "lucide-react";

export default function AnalyticsDashboard() {
  const [overview, setOverview] = useState<any>(null);
  const [workload, setWorkload] = useState<any[]>([]);
  const [trends, setTrends] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [overviewRes, workloadRes, trendsRes] = await Promise.all([
          analyticsApi.getOverview(),
          analyticsApi.getWorkload(),
          analyticsApi.getMalpracticeTrends(),
        ]);
        setOverview(overviewRes.data);
        setWorkload(workloadRes.data);
        setTrends(trendsRes.data);
      } catch (error) {
        toast.error("Failed to load analytics data");
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="container mx-auto py-8 px-4 max-w-6xl flex justify-center items-center min-h-[50vh]">
        <div className="text-slate-500 animate-pulse">Loading Analytics...</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8 px-4 max-w-6xl animate-in fade-in duration-500">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900 flex items-center gap-2">
          <Activity className="text-primary" /> Analytics Dashboard
        </h1>
        <p className="text-slate-500 mt-1">
          High-level insights into exam operations, faculty workload, and malpractice trends.
        </p>
      </div>

      {/* Top Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <Card className="shadow-sm border-slate-200">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Total Faculty</CardTitle>
            <Users size={16} className="text-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-slate-900">{overview?.totalFaculty || 0}</div>
            <p className="text-xs text-slate-400 mt-1">Active invigilators</p>
          </CardContent>
        </Card>
        <Card className="shadow-sm border-slate-200">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Exams Conducted</CardTitle>
            <BarChart size={16} className="text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-slate-900">{overview?.totalArchives || 0}</div>
            <p className="text-xs text-slate-400 mt-1">In the last 3 months</p>
          </CardContent>
        </Card>
        <Card className="shadow-sm border-slate-200">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-slate-500">Malpractice Cases</CardTitle>
            <FileWarning size={16} className="text-destructive" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-slate-900">
              {String(Object.values(trends?.byType || {}).reduce((a: any, b: any) => a + Number(b), 0))}
            </div>
            <p className="text-xs text-slate-400 mt-1">Reported issues</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Workload Section */}
        <Card className="shadow-sm border-slate-200">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-lg">Top Faculty Workload</CardTitle>
            <Button 
              size="sm" 
              variant="outline" 
              className="text-primary border-primary/20 hover:bg-primary/5"
              onClick={() => {
                const url = `${import.meta.env.VITE_API_BASE_URL || "/api/v1"}/analytics/remuneration/export`;
                window.open(url, "_blank");
              }}
            >
              <FileText className="mr-2 h-4 w-4" />
              Remuneration Sheet
            </Button>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {workload.slice(0, 5).map((w, i) => (
                <div key={w.facultyId} className="flex items-center">
                  <div className="w-8 text-slate-400 font-mono text-sm">{i + 1}.</div>
                  <div className="flex-1">
                    <div className="font-medium text-sm text-slate-900">{w.facultyName}</div>
                    <div className="text-xs text-slate-500">{w.department}</div>
                  </div>
                  <div className="text-sm font-bold text-slate-700 bg-slate-100 px-3 py-1 rounded-full">
                    {w.dutyCount} duties
                  </div>
                </div>
              ))}
              {workload.length === 0 && (
                <div className="text-center text-sm text-slate-500 py-4">No workload data available.</div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Malpractice Trends Section */}
        <Card className="shadow-sm border-slate-200">
          <CardHeader>
            <CardTitle className="text-lg">Malpractice Breakdown</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-6">
              <div>
                <h4 className="text-sm font-medium text-slate-500 mb-3">By Type</h4>
                <div className="space-y-2">
                  {Object.entries(trends?.byType || {}).map(([type, count]) => (
                    <div key={type} className="flex justify-between items-center text-sm">
                      <span className="text-slate-700">{type}</span>
                      <span className="font-medium">{count as number}</span>
                    </div>
                  ))}
                  {Object.keys(trends?.byType || {}).length === 0 && (
                    <div className="text-sm text-slate-400 italic">No data</div>
                  )}
                </div>
              </div>
              <div className="border-t pt-4">
                <h4 className="text-sm font-medium text-slate-500 mb-3">By Severity</h4>
                <div className="flex gap-4">
                  {Object.entries(trends?.bySeverity || {}).map(([sev, count]) => (
                    <div key={sev} className="flex-1 bg-slate-50 rounded-lg p-3 text-center border">
                      <div className="text-2xl font-bold text-slate-800">{count as number}</div>
                      <div className="text-xs text-slate-500 uppercase">{sev}</div>
                    </div>
                  ))}
                  {Object.keys(trends?.bySeverity || {}).length === 0 && (
                    <div className="text-sm text-slate-400 italic">No data</div>
                  )}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
