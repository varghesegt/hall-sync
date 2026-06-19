import { useState, useEffect } from "react";
import { analyticsApi } from "../../api/analyticsApi";
import { useQuery } from "@tanstack/react-query";
import { 
  Users, 
  DoorOpen, 
  UserCheck, 
  AlertTriangle, 
  CalendarDays,
  Activity
} from "lucide-react";
import { format } from "date-fns";

export function CommandCenter() {
  const [targetDate, setTargetDate] = useState<string>(format(new Date(), "yyyy-MM-dd"));

  const { data: stats, isLoading, refetch } = useQuery({
    queryKey: ["command-center-stats", targetDate],
    queryFn: async () => {
      const response = await analyticsApi.getCommandCenter(targetDate);
      return response.data;
    },
    refetchInterval: 30000, // Auto refresh every 30s
  });

  // Force refresh
  useEffect(() => {
    refetch();
  }, [targetDate, refetch]);

  return (
    <div className="space-y-6 animate-in fade-in zoom-in-95 duration-200">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight">Exam Command Center</h1>
          <p className="text-gray-500 mt-1 flex items-center gap-2">
            <Activity className="w-4 h-4 text-emerald-500 animate-pulse" />
            Live monitoring dashboard
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="text-sm font-medium text-gray-700">Date:</label>
          <input
            type="date"
            className="rounded-md border-gray-300 shadow-sm focus:border-brand-500 focus:ring-brand-500 sm:text-sm"
            value={targetDate}
            onChange={(e) => setTargetDate(e.target.value)}
          />
        </div>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-gray-100 rounded-xl h-32"></div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard 
            title="Active Halls" 
            value={stats?.activeHalls || 0} 
            icon={<DoorOpen className="w-6 h-6 text-blue-600" />} 
            bgColor="bg-blue-50"
          />
          <StatCard 
            title="Expected Students" 
            value={stats?.totalExpectedStudents || 0} 
            icon={<Users className="w-6 h-6 text-indigo-600" />} 
            bgColor="bg-indigo-50"
          />
          <StatCard 
            title="Faculty Allocated" 
            value={stats?.totalFacultyAllocated || 0} 
            icon={<UserCheck className="w-6 h-6 text-emerald-600" />} 
            bgColor="bg-emerald-50"
          />
          <StatCard 
            title="Pending Malpractices" 
            value={stats?.pendingMalpractices || 0} 
            icon={<AlertTriangle className="w-6 h-6 text-rose-600" />} 
            bgColor="bg-rose-50"
            alert={stats?.pendingMalpractices > 0}
          />
          <StatCard 
            title="Sessions Today" 
            value={stats?.totalSessionsToday || 0} 
            icon={<CalendarDays className="w-6 h-6 text-amber-600" />} 
            bgColor="bg-amber-50"
          />
        </div>
      )}

      {/* Further widgets could go here like live timeline or notifications */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Live Updates</h2>
        {stats?.totalSessionsToday === 0 ? (
          <div className="text-center py-12 text-gray-500">
            No exam sessions scheduled for {targetDate}.
          </div>
        ) : (
          <div className="text-sm text-gray-600">
            Monitoring {stats?.totalSessionsToday} session(s) across {stats?.activeHalls} hall(s).
            System will auto-refresh metrics every 30 seconds.
          </div>
        )}
      </div>

    </div>
  );
}

function StatCard({ title, value, icon, bgColor, alert = false }: { title: string; value: number | string; icon: React.ReactNode; bgColor: string; alert?: boolean }) {
  return (
    <div className={`bg-white rounded-xl shadow-sm border ${alert ? 'border-rose-300 shadow-rose-100' : 'border-gray-200'} p-6 relative overflow-hidden`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500 truncate mb-1">{title}</p>
          <p className={`text-3xl font-bold ${alert ? 'text-rose-600' : 'text-gray-900'}`}>{value}</p>
        </div>
        <div className={`p-3 rounded-xl ${bgColor}`}>
          {icon}
        </div>
      </div>
      {alert && (
        <div className="absolute top-0 right-0 w-full h-1 bg-rose-500"></div>
      )}
    </div>
  );
}
