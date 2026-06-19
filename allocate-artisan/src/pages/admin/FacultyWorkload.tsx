import { useState } from "react";
import { analyticsApi } from "../../api/analyticsApi";
import { useQuery } from "@tanstack/react-query";
import { BarChart3, TrendingUp, Users, Download, AlertTriangle, CheckCircle2, Loader2 } from "lucide-react";
import { toast } from "sonner";

interface WorkloadData {
  facultyId: string;
  dutyCount: number;
  facultyName: string;
  department: string;
}

interface DeptData {
  department: string;
  totalDuties: number;
  facultyCount: number;
  avgDuties: number;
}

interface FairnessData {
  fairnessScore: number;
  mean: number;
  stdDev: number;
  overloaded: { name: string; department: string; dutyCount: number }[];
  underloaded: { name: string; department: string; dutyCount: number }[];
}

export function FacultyWorkload() {
  const [dateRange, setDateRange] = useState({
    from: new Date(new Date().setMonth(new Date().getMonth() - 3)).toISOString().split('T')[0],
    to: new Date().toISOString().split('T')[0]
  });
  const [isExporting, setIsExporting] = useState(false);

  const { data: workload = [], isLoading } = useQuery<WorkloadData[]>({
    queryKey: ["faculty-workload", dateRange],
    queryFn: async () => {
      const response = await analyticsApi.getWorkload(dateRange.from, dateRange.to);
      return response.data;
    },
  });

  const { data: deptDistribution = [] } = useQuery<DeptData[]>({
    queryKey: ["dept-distribution", dateRange],
    queryFn: async () => {
      const response = await analyticsApi.getDepartmentDistribution(dateRange.from, dateRange.to);
      return response.data;
    },
  });

  const { data: fairness } = useQuery<FairnessData>({
    queryKey: ["fairness-index", dateRange],
    queryFn: async () => {
      const response = await analyticsApi.getFairnessIndex(dateRange.from, dateRange.to);
      return response.data;
    },
  });

  const totalDuties = workload.reduce((sum, item) => sum + item.dutyCount, 0);
  const averageDuties = workload.length > 0 ? (totalDuties / workload.length).toFixed(1) : "0";
  const maxDuties = workload.length > 0 ? Math.max(...workload.map(w => w.dutyCount)) : 0;
  const maxDeptDuties = deptDistribution.length > 0 ? Math.max(...deptDistribution.map(d => d.totalDuties)) : 0;

  const handleExport = async () => {
    setIsExporting(true);
    try {
      const response = await analyticsApi.exportRemuneration(dateRange.from, dateRange.to);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `Remuneration_Report_${dateRange.from}_to_${dateRange.to}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success("Report exported successfully!");
    } catch {
      toast.error("Failed to export report.");
    } finally {
      setIsExporting(false);
    }
  };

  const fairnessColor = (score: number) => {
    if (score >= 80) return "text-emerald-600";
    if (score >= 50) return "text-amber-600";
    return "text-rose-600";
  };

  const fairnessRingColor = (score: number) => {
    if (score >= 80) return "#10b981";
    if (score >= 50) return "#f59e0b";
    return "#ef4444";
  };

  return (
    <div className="space-y-6 animate-in fade-in zoom-in-95 duration-200">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight">Faculty Workload Analytics</h1>
          <p className="text-gray-500 mt-1">Monitor and balance invigilation duties</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-3 bg-white p-2 rounded-lg border border-gray-200 shadow-sm">
            <div className="flex items-center gap-2">
              <label className="text-sm text-gray-600">From:</label>
              <input
                type="date"
                className="text-sm border-gray-300 rounded focus:ring-brand-500 focus:border-brand-500"
                value={dateRange.from}
                onChange={e => setDateRange(prev => ({ ...prev, from: e.target.value }))}
              />
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm text-gray-600">To:</label>
              <input
                type="date"
                className="text-sm border-gray-300 rounded focus:ring-brand-500 focus:border-brand-500"
                value={dateRange.to}
                onChange={e => setDateRange(prev => ({ ...prev, to: e.target.value }))}
              />
            </div>
          </div>
          <button
            onClick={handleExport}
            disabled={isExporting}
            className="inline-flex items-center gap-2 rounded-lg bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:opacity-50 transition-colors"
          >
            {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
            Export
          </button>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex items-center gap-4">
          <div className="p-3 bg-indigo-50 rounded-lg">
            <Users className="w-6 h-6 text-indigo-600" />
          </div>
          <div>
            <p className="text-sm text-gray-500 font-medium">Active Faculty</p>
            <p className="text-2xl font-bold text-gray-900">{workload.length}</p>
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex items-center gap-4">
          <div className="p-3 bg-emerald-50 rounded-lg">
            <BarChart3 className="w-6 h-6 text-emerald-600" />
          </div>
          <div>
            <p className="text-sm text-gray-500 font-medium">Total Duties</p>
            <p className="text-2xl font-bold text-gray-900">{totalDuties}</p>
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex items-center gap-4">
          <div className="p-3 bg-amber-50 rounded-lg">
            <TrendingUp className="w-6 h-6 text-amber-600" />
          </div>
          <div>
            <p className="text-sm text-gray-500 font-medium">Avg Duties / Faculty</p>
            <p className="text-2xl font-bold text-gray-900">{averageDuties}</p>
            <p className="text-xs text-gray-500 mt-0.5">Max: {maxDuties}</p>
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex items-center gap-4">
          <div className="p-3 bg-blue-50 rounded-lg">
            <CheckCircle2 className={`w-6 h-6 ${fairnessColor(fairness?.fairnessScore ?? 100)}`} />
          </div>
          <div>
            <p className="text-sm text-gray-500 font-medium">Fairness Index</p>
            <p className={`text-2xl font-bold ${fairnessColor(fairness?.fairnessScore ?? 100)}`}>
              {fairness?.fairnessScore ?? 100}%
            </p>
            <p className="text-xs text-gray-500 mt-0.5">σ = {fairness?.stdDev ?? 0}</p>
          </div>
        </div>
      </div>

      {/* Department Distribution + Fairness side by side */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Department Distribution */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="p-5 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Department-wise Distribution</h2>
            <p className="text-xs text-gray-500 mt-0.5">Total duties assigned per department</p>
          </div>
          <div className="p-5 space-y-4">
            {deptDistribution.length === 0 ? (
              <p className="text-sm text-gray-500 text-center py-6">No department data found for this period.</p>
            ) : (
              deptDistribution.map((dept) => (
                <div key={dept.department} className="space-y-1.5">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium text-gray-900">{dept.department}</span>
                    <span className="text-sm text-gray-500">
                      {dept.totalDuties} duties • {dept.facultyCount} faculty • avg {dept.avgDuties}
                    </span>
                  </div>
                  <div className="w-full bg-gray-100 rounded-full h-3">
                    <div
                      className="h-3 rounded-full bg-gradient-to-r from-indigo-500 to-blue-500 transition-all duration-500"
                      style={{ width: `${maxDeptDuties > 0 ? (dept.totalDuties / maxDeptDuties) * 100 : 0}%` }}
                    />
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Fairness Index Panel */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="p-5 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Workload Fairness</h2>
            <p className="text-xs text-gray-500 mt-0.5">Faculty exceeding 1.5× or below 0.5× average</p>
          </div>
          <div className="p-5">
            {/* Visual gauge */}
            <div className="flex items-center justify-center mb-6">
              <div className="relative w-32 h-32">
                <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r="40" fill="none" stroke="#e5e7eb" strokeWidth="8" />
                  <circle
                    cx="50" cy="50" r="40" fill="none"
                    stroke={fairnessRingColor(fairness?.fairnessScore ?? 100)}
                    strokeWidth="8"
                    strokeDasharray={`${(fairness?.fairnessScore ?? 100) * 2.51} 251`}
                    strokeLinecap="round"
                  />
                </svg>
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className={`text-2xl font-bold ${fairnessColor(fairness?.fairnessScore ?? 100)}`}>
                    {fairness?.fairnessScore ?? 100}%
                  </span>
                </div>
              </div>
            </div>

            {/* Overloaded list */}
            {fairness?.overloaded && fairness.overloaded.length > 0 && (
              <div className="mb-4">
                <h4 className="text-sm font-semibold text-rose-600 flex items-center gap-1.5 mb-2">
                  <AlertTriangle className="w-4 h-4" /> Overloaded ({">"}1.5× avg)
                </h4>
                <div className="space-y-1.5">
                  {fairness.overloaded.map((f, i) => (
                    <div key={i} className="flex justify-between text-sm bg-rose-50 rounded-lg px-3 py-2">
                      <span className="font-medium text-gray-900">{f.name}</span>
                      <span className="text-rose-600 font-semibold">{f.dutyCount} duties</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Underloaded list */}
            {fairness?.underloaded && fairness.underloaded.length > 0 && (
              <div>
                <h4 className="text-sm font-semibold text-amber-600 flex items-center gap-1.5 mb-2">
                  <TrendingUp className="w-4 h-4" /> Underloaded ({"<"}0.5× avg)
                </h4>
                <div className="space-y-1.5">
                  {fairness.underloaded.map((f, i) => (
                    <div key={i} className="flex justify-between text-sm bg-amber-50 rounded-lg px-3 py-2">
                      <span className="font-medium text-gray-900">{f.name}</span>
                      <span className="text-amber-600 font-semibold">{f.dutyCount} duties</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {fairness && (fairness.overloaded?.length === 0 && fairness.underloaded?.length === 0) && (
              <div className="text-center text-sm text-emerald-600 py-4">
                <CheckCircle2 className="w-6 h-6 mx-auto mb-2" />
                All faculty are within normal workload range.
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Faculty table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Individual Workload Distribution</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Faculty Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Department</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Duties Count</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Load Indicator</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {isLoading ? (
                <tr>
                  <td colSpan={4} className="px-6 py-8 text-center text-gray-500">Loading workload data...</td>
                </tr>
              ) : workload.length === 0 ? (
                <tr>
                  <td colSpan={4} className="px-6 py-8 text-center text-gray-500">No duty data found for this period.</td>
                </tr>
              ) : (
                workload.map((item) => (
                  <tr key={item.facultyId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{item.facultyName}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{item.department}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">{item.dutyCount}</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="w-full bg-gray-200 rounded-full h-2.5 max-w-[200px]">
                        <div 
                          className={`h-2.5 rounded-full ${item.dutyCount > Number(averageDuties) * 1.5 ? 'bg-rose-500' : 'bg-emerald-500'}`} 
                          style={{ width: `${Math.min(100, (item.dutyCount / maxDuties) * 100)}%` }}
                        ></div>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
