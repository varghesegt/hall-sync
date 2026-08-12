import { useState, useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  getInternalStudentPreview,
} from "@/api/internalApi";
import { ExcelUploadCard } from '@/features/claims/components/ExcelUploadCard';
import { InternalSessionCard } from "@/features/allocations/components/InternalSessionCard";
import { InternalAllocationCard } from "@/features/allocations/components/InternalAllocationCard";
import { PastAllocationsCard } from "@/features/allocations/components/PastAllocationsCard";
import { RoomSelector } from "@/features/allocations/components/RoomSelector";
import { StepIndicator } from "@/components/StepIndicator";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ChevronDown, ChevronUp, Search } from "lucide-react";

export default function InternalDashboard() {
  const [fileId, setFileId] = useState<string | null>(() => sessionStorage.getItem("hall_sync_internal_file_id"));
  const [sessionId, setSessionId] = useState<string | null>(() => sessionStorage.getItem("hall_sync_internal_session_id"));
  const [totalStudents, setTotalStudents] = useState<number | null>(null);
  const [selectedRooms, setSelectedRooms] = useState<string[]>([]);

  useEffect(() => {
    if (fileId) sessionStorage.setItem("hall_sync_internal_file_id", fileId);
  }, [fileId]);

  useEffect(() => {
    if (sessionId) sessionStorage.setItem("hall_sync_internal_session_id", sessionId);
  }, [sessionId]);

  const steps = [
    { label: "Upload", completed: !!fileId, active: !fileId },
    { label: "Session", completed: !!sessionId, active: !!fileId && !sessionId },
    { label: "Rooms", completed: selectedRooms.length > 0, active: !!sessionId },
    { label: "Allocate", completed: false, active: !!sessionId && selectedRooms.length > 0 },
  ];

  return (
    <div className="min-h-screen bg-slate-50/50 mesh-gradient selection:bg-primary/10">

      <main className="mx-auto max-w-4xl space-y-6 px-6 py-10 animate-in fade-in slide-in-from-bottom-5 duration-1000">
        <div className="mb-2">
          <StepIndicator steps={steps} />
        </div>

        <div className="grid gap-6">
          <div className="animate-in fade-in slide-in-from-bottom-4 duration-700 delay-100">
            <ExcelUploadCard
              fileId={fileId}
              onSuccess={(fid) => {
                setFileId(fid);
                sessionStorage.setItem("hall_sync_internal_file_id", fid);
              }}
              onReset={() => {
                setFileId(null);
                setSessionId(null);
                sessionStorage.removeItem("hall_sync_internal_file_id");
                sessionStorage.removeItem("hall_sync_internal_session_id");
              }}
            />
          </div>

          {fileId && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-700 delay-200">
              <InternalStudentPreview fileId={fileId} onParsedCount={(count) => setTotalStudents(count)} />
            </div>
          )}

          <div className="animate-in fade-in slide-in-from-bottom-4 duration-700 delay-300">
            <InternalSessionCard
              fileId={fileId}
              sessionId={sessionId}
              onSuccess={(sid, count) => {
                setSessionId(sid);
                setTotalStudents(count);
                sessionStorage.setItem("hall_sync_internal_session_id", sid);
              }}
            />
          </div>

          {sessionId && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-700">
              <RoomSelector
                onSelectionChange={setSelectedRooms}
                totalStudents={totalStudents}
                isInternal={true}
              />
            </div>
          )}

          <div className="animate-in fade-in slide-in-from-bottom-4 duration-700 delay-400">
            <InternalAllocationCard sessionId={sessionId} selectedRooms={selectedRooms} />
          </div>

          {/* Dedicated Internal Allocation History & Downloads */}
          <div className="animate-in fade-in slide-in-from-bottom-4 duration-700 delay-500 pt-4">
            <PastAllocationsCard isInternal={true} />
          </div>
        </div>
      </main>

      <footer className="w-full mt-20 border-t border-slate-200/80 bg-white/50 backdrop-blur-md">
        <div className="mx-auto max-w-6xl px-8 py-10 flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex flex-col items-center md:items-start gap-1">
            <h3 className="text-sm font-black tracking-tighter text-slate-900">
              Hall<span className="text-emerald-600">Sync</span>
            </h3>
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">
              Internal Examination Management
            </p>
          </div>
          <div className="flex flex-col items-center md:items-end gap-1">
            <div className="flex items-center gap-2 text-[11px] font-bold text-slate-500">
              <span>DEVELOPED BY</span>
              <span className="text-slate-900 font-black uppercase tracking-tight underline decoration-emerald-500/30 decoration-2 underline-offset-4">
                Varghese G T
              </span>
            </div>
            <p className="text-[9px] font-black text-slate-400 uppercase tracking-[0.2em]">
              Department of Mechanical Engineering
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}

// Internal student preview component that uses internal API
function InternalStudentPreview({ fileId, onParsedCount }: { fileId: string; onParsedCount?: (count: number) => void }) {
  const { data } = useQuery({
    queryKey: ["internal-student-preview", fileId],
    queryFn: () => getInternalStudentPreview(fileId),
    enabled: !!fileId,
  });

  useEffect(() => {
    if (data?.totalStudents) {
      onParsedCount?.(data.totalStudents);
    }
  }, [data, onParsedCount]);

  const [search, setSearch] = useState("");
  const [deptFilter, setDeptFilter] = useState<string | null>(null);
  const [expanded, setExpanded] = useState(true);
  const [visibleCount, setVisibleCount] = useState(20);

  const departments = useMemo(() => {
    if (!data?.students) return [];
    return [...new Set(data.students.map((s) => s.department))].sort();
  }, [data]);

  const filtered = useMemo(() => {
    if (!data?.students) return [];
    let list = [...data.students];
    
    // Sort department wise
    list.sort((a, b) => {
      const deptCmp = a.department.localeCompare(b.department);
      if (deptCmp !== 0) return deptCmp;
      return a.registerNumber.localeCompare(b.registerNumber);
    });

    if (deptFilter) list = list.filter((s) => s.department === deptFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (s) =>
          s.registerNumber.toLowerCase().includes(q) ||
          s.name.toLowerCase().includes(q) ||
          s.department.toLowerCase().includes(q) ||
          (s.subjectName && s.subjectName.toLowerCase().includes(q)) ||
          (s.subjectCode && s.subjectCode.toLowerCase().includes(q))
      );
    }
    return list;
  }, [data, search, deptFilter]);

  if (!data?.students || data.students.length === 0) return null;

  return (
    <div className="rounded-xl border border-slate-200/80 bg-white/80 backdrop-blur-md shadow-xs overflow-hidden">
      <div className="flex items-center justify-between p-4 bg-slate-50/50 border-b border-slate-100">
        <div className="flex items-center gap-3">
          <span className="font-bold text-sm text-slate-900">Parsed Students Roster</span>
          <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
            {data.totalStudents} Students
          </span>
        </div>
        <Button variant="ghost" size="sm" onClick={() => setExpanded(!expanded)} className="h-8 w-8 p-0 text-slate-500">
          {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </Button>
      </div>

      {expanded && (
        <div className="p-4 space-y-4">
          <div className="flex flex-col sm:flex-row items-center gap-3 justify-between">
            <div className="relative w-full sm:w-72">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <Input
                placeholder="Search reg no, name, dept..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9 h-9 text-xs"
              />
            </div>
            <div className="flex flex-wrap gap-1.5 w-full sm:w-auto">
              <Button
                variant={deptFilter === null ? "default" : "outline"}
                size="sm"
                onClick={() => setDeptFilter(null)}
                className="h-7 text-[11px] px-2.5"
              >
                All ({data.totalStudents})
              </Button>
              {departments.map((dept) => {
                const count = data.students.filter((s) => s.department === dept).length;
                return (
                  <Button
                    key={dept}
                    variant={deptFilter === dept ? "default" : "outline"}
                    size="sm"
                    onClick={() => setDeptFilter(deptFilter === dept ? null : dept)}
                    className="h-7 text-[11px] px-2.5"
                  >
                    {dept} ({count})
                  </Button>
                );
              })}
            </div>
          </div>

          <div className="border border-slate-100 rounded-lg overflow-x-auto max-h-80 overflow-y-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead className="sticky top-0 bg-slate-100/90 backdrop-blur-xs text-slate-600 font-semibold text-[11px] uppercase tracking-wider">
                <tr>
                  <th className="py-2.5 px-3 border-b border-slate-200">#</th>
                  <th className="py-2.5 px-3 border-b border-slate-200">Register No</th>
                  <th className="py-2.5 px-3 border-b border-slate-200">Student Name</th>
                  <th className="py-2.5 px-3 border-b border-slate-200">Dept</th>
                  <th className="py-2.5 px-3 border-b border-slate-200">Subject</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700 font-medium">
                {filtered.slice(0, visibleCount).map((s, idx) => (
                  <tr key={s.registerNumber + idx} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-2 px-3 text-slate-400 text-[10px]">{idx + 1}</td>
                    <td className="py-2 px-3 font-mono font-bold text-slate-900">{s.registerNumber}</td>
                    <td className="py-2 px-3">{s.name}</td>
                    <td className="py-2 px-3 font-semibold text-slate-600">{s.department}</td>
                    <td className="py-2 px-3 text-slate-500">{s.subjectCode || "—"} {s.subjectName ? `(${s.subjectName})` : ""}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {filtered.length > visibleCount && (
            <div className="text-center pt-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setVisibleCount((prev) => prev + 50)}
                className="text-xs text-slate-600"
              >
                Show More ({filtered.length - visibleCount} remaining)
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
