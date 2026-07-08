import { useState, useEffect } from "react";
import { analyticsApi } from '@/api/analyticsApi';
import { useQuery } from "@tanstack/react-query";
import { 
  Users, 
  DoorOpen, 
  UserCheck, 
  AlertTriangle, 
  CalendarDays,
  Activity,
  Clock,
  PlayCircle,
  CheckCircle2,
  ChevronRight,
  Info,
  CheckCircle,
  AlertCircle
} from "lucide-react";
import { format, differenceInMinutes, parseISO } from "date-fns";
import { Progress } from '@/components/ui/progress';

export function CommandCenter() {
  const [targetDate, setTargetDate] = useState<string>(format(new Date(), "yyyy-MM-dd"));
  const [currentTime, setCurrentTime] = useState(new Date());

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 60000);
    return () => clearInterval(timer);
  }, []);

  const { data: stats, isLoading, refetch } = useQuery({
    queryKey: ["command-center-stats", targetDate],
    queryFn: async () => {
      const response = await analyticsApi.getCommandCenter(targetDate);
      return response.data;
    },
    refetchInterval: 30000, 
  });

  useEffect(() => {
    refetch();
  }, [targetDate, refetch]);

  const activeBatches = stats?.activeBatches || [];
  const liveEvents = stats?.liveEvents || [];

  return (
    <div className="space-y-6 animate-in fade-in zoom-in-95 duration-200 min-h-screen pb-12">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Exam Command Center</h1>
          <p className="text-slate-500 mt-1 flex items-center gap-2">
            <Activity className="w-4 h-4 text-emerald-500 animate-pulse" />
            Live monitoring dashboard
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="text-sm font-medium text-slate-700">Date:</label>
          <input
            type="date"
            className="rounded-md border-slate-300 shadow-sm focus:border-brand-500 focus:ring-brand-500 sm:text-sm"
            value={targetDate}
            onChange={(e) => setTargetDate(e.target.value)}
          />
        </div>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-slate-100 rounded-xl h-32"></div>
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
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Active Sessions Tracker */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-slate-800">Active Sessions</h2>
            <span className="text-sm text-slate-500 flex items-center gap-2">
              <CalendarDays className="w-4 h-4" /> {stats?.totalSessionsToday || 0} scheduled
            </span>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            {activeBatches.length === 0 ? (
              <div className="text-center py-12 text-slate-500">
                No exam sessions scheduled for {targetDate}.
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {activeBatches.map((batch: any) => {
                  const start = parseISO(batch.startTime);
                  const end = parseISO(batch.endTime);
                  const totalDuration = differenceInMinutes(end, start);
                  const elapsed = differenceInMinutes(currentTime, start);
                  
                  let progress = 0;
                  let statusText = "Upcoming";
                  let statusColor = "text-slate-500";
                  let StatusIcon = Clock;

                  if (elapsed >= totalDuration) {
                    progress = 100;
                    statusText = "Completed";
                    statusColor = "text-emerald-600";
                    StatusIcon = CheckCircle2;
                  } else if (elapsed > 0) {
                    progress = Math.min(100, Math.max(0, (elapsed / totalDuration) * 100));
                    statusText = "In Progress";
                    statusColor = "text-blue-600";
                    StatusIcon = PlayCircle;
                  }

                  return (
                    <div key={batch.batchId} className="p-5 hover:bg-slate-50 transition-colors">
                      <div className="flex justify-between items-start mb-3">
                        <div>
                          <div className="flex items-center gap-2">
                            <h3 className="font-semibold text-slate-900">{batch.examType} Exam</h3>
                            <span className={`text-xs font-medium px-2 py-0.5 rounded-full bg-slate-100 ${statusColor} flex items-center gap-1`}>
                              <StatusIcon className="w-3 h-3" /> {statusText}
                            </span>
                          </div>
                          <p className="text-sm text-slate-500 mt-1">
                            {batch.session} - {batch.name} • {batch.hallCount} Halls • {batch.studentCount} Students
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-sm font-medium text-slate-700">
                            {format(start, "h:mm a")} - {format(end, "h:mm a")}
                          </p>
                          <p className="text-xs text-slate-400 mt-1">
                            {progress > 0 && progress < 100 ? `${Math.floor(totalDuration - elapsed)} mins left` : ""}
                          </p>
                        </div>
                      </div>
                      
                      <div className="flex items-center gap-3">
                        <Progress value={progress} className="h-2 flex-1" />
                        <span className="text-xs font-medium text-slate-500 w-8">{Math.floor(progress)}%</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Live Event Feed */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-bold text-slate-800">Live Event Feed</h2>
            <div className="w-2 h-2 rounded-full bg-rose-500 animate-pulse"></div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 h-[400px] overflow-y-auto">
            {liveEvents.length === 0 ? (
              <div className="text-center py-12 text-slate-400 text-sm">
                Awaiting events...
              </div>
            ) : (
              <div className="space-y-6 relative before:absolute before:inset-0 before:ml-5 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-slate-300 before:to-transparent">
                {liveEvents.map((event: any, idx: number) => {
                  let EventIcon = Info;
                  let iconColor = "text-blue-500";
                  let bgColor = "bg-blue-50";

                  if (event.type === "SUCCESS") {
                    EventIcon = CheckCircle;
                    iconColor = "text-emerald-500";
                    bgColor = "bg-emerald-50";
                  } else if (event.type === "WARNING") {
                    EventIcon = AlertCircle;
                    iconColor = "text-rose-500";
                    bgColor = "bg-rose-50";
                  }

                  return (
                    <div key={event.id} className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
                      <div className={`flex items-center justify-center w-10 h-10 rounded-full border-4 border-white ${bgColor} ${iconColor} shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 shadow-sm z-10`}>
                        <EventIcon className="w-5 h-5" />
                      </div>
                      
                      <div className="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] p-4 rounded-xl border border-slate-100 bg-white shadow-sm">
                        <div className="flex items-center justify-between mb-1">
                          <span className="text-xs font-semibold text-slate-400">
                            {format(parseISO(event.time), "h:mm a")}
                          </span>
                        </div>
                        <p className="text-sm text-slate-700">{event.message}</p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}

function StatCard({ title, value, icon, bgColor, alert = false }: { title: string; value: number | string; icon: React.ReactNode; bgColor: string; alert?: boolean }) {
  return (
    <div className={`bg-white rounded-xl shadow-sm border ${alert ? 'border-rose-300 shadow-rose-100' : 'border-slate-200'} p-6 relative overflow-hidden group hover:shadow-md transition-shadow`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-slate-500 truncate mb-1">{title}</p>
          <p className={`text-3xl font-bold tracking-tight ${alert ? 'text-rose-600' : 'text-slate-900'}`}>{value}</p>
        </div>
        <div className={`p-3 rounded-xl ${bgColor} group-hover:scale-110 transition-transform`}>
          {icon}
        </div>
      </div>
      {alert && (
        <div className="absolute top-0 right-0 w-full h-1 bg-rose-500"></div>
      )}
    </div>
  );
}
