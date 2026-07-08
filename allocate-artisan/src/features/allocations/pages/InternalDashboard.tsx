import { useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  uploadInternalExcel,
  getInternalStudentPreview,
  createInternalSession,
  triggerInternalAllocation,
  getInternalLatestBatch,
  downloadInternalBatchPdf,
  downloadInternalBatchExcel,
  downloadInternalBatchSummaryExcel,
} from "@/api/internalApi";
import type { UploadResponse, StudentPreviewResponse, SessionResponse, AllocationStatus } from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";
import { ExcelUploadCard } from '@/features/claims/components/ExcelUploadCard';
import { StudentPreviewTable } from "@/features/allocations/components/StudentPreviewTable";
import { InternalSessionCard } from "@/features/allocations/components/InternalSessionCard";
import { InternalAllocationCard } from "@/features/allocations/components/InternalAllocationCard";
import { BatchSelector } from '@/features/claims/components/BatchSelector';
import { RoomSelector } from "@/features/allocations/components/RoomSelector";
import { StepIndicator } from "@/components/StepIndicator";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { LogOut, ShieldCheck, ArrowLeft, LayoutGrid, Users, ChevronDown, ChevronUp, Search, AlertCircle } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

export default function InternalDashboard() {
  const [fileId, setFileId] = useState<string | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [totalStudents, setTotalStudents] = useState<number | null>(null);
  const [selectedRooms, setSelectedRooms] = useState<string[]>([]);
  const navigate = useNavigate();

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
            <ExcelUploadCard fileId={fileId} onSuccess={setFileId} />
          </div>

          {fileId && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-700 delay-200">
              <InternalStudentPreview fileId={fileId} />
            </div>
          )}

          <div className="animate-in fade-in slide-in-from-bottom-4 duration-700 delay-300">
            <InternalSessionCard
              fileId={fileId}
              sessionId={sessionId}
              onSuccess={(sid, count) => {
                setSessionId(sid);
                setTotalStudents(count);
              }}
            />
          </div>

          {sessionId && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-700">
              <RoomSelector onSelectionChange={setSelectedRooms} />
            </div>
          )}

          <div className="animate-in fade-in slide-in-from-bottom-4 duration-700 delay-400">
            <InternalAllocationCard sessionId={sessionId} selectedRooms={selectedRooms} />
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
function InternalStudentPreview({ fileId }: { fileId: string }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ["internal-student-preview", fileId],
    queryFn: () => getInternalStudentPreview(fileId),
    enabled: !!fileId,
  });

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

  const visible = filtered.slice(0, visibleCount);

  if (!fileId) return null;

  return (
    <div className="rounded-xl border bg-white shadow-sm overflow-hidden transition-all duration-300">
      <div 
        className="border-b px-5 py-4 cursor-pointer select-none hover:bg-slate-50 flex items-center justify-between"
        onClick={() => setExpanded(p => !p)}
      >
        <div>
          <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <Users className="h-4 w-4 text-emerald-600" />
            Student Roster Preview
            {data && (
              <Badge variant="secondary" className="ml-2 font-mono text-xs bg-emerald-100 text-emerald-700 hover:bg-emerald-200 border-none">
                {data.totalStudents} parsed
              </Badge>
            )}
          </h3>
          <p className="text-xs text-slate-500 mt-1">
            {isLoading 
              ? "Loading students from uploaded Excel file..." 
              : data 
              ? `${departments.length} departments detected` 
              : "Preview uploaded students"}
          </p>
        </div>
        {expanded ? <ChevronUp className="h-5 w-5 text-slate-400" /> : <ChevronDown className="h-5 w-5 text-slate-400" />}
      </div>

      {expanded && (
        <div className="p-5 space-y-4 bg-slate-50/30">
          {isLoading && (
            <div className="flex items-center justify-center p-8 gap-3 text-sm text-slate-500">
              <span className="h-5 w-5 animate-spin rounded-full border-2 border-slate-300 border-t-emerald-600" />
              Processing Excel Data...
            </div>
          )}

          {error && (
            <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <span>{error.message || "Failed to load student preview."}</span>
            </div>
          )}

          {data && data.totalStudents > 0 && (
            <>
              {/* Department Filters */}
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => { setDeptFilter(null); setVisibleCount(20); }}
                  className={cn(
                    "rounded-full border px-3 py-1 text-xs font-bold transition-all shadow-sm",
                    !deptFilter
                      ? "border-emerald-500 bg-emerald-500 text-white shadow-emerald-500/20"
                      : "border-slate-200 bg-white text-slate-600 hover:bg-slate-100 hover:border-slate-300"
                  )}
                >
                  All Depts
                </button>
                {departments.map((dept) => (
                  <button
                    key={dept}
                    onClick={() => { setDeptFilter(deptFilter === dept ? null : dept); setVisibleCount(20); }}
                    className={cn(
                      "rounded-full border px-3 py-1 text-xs font-bold transition-all shadow-sm",
                      deptFilter === dept
                        ? "border-emerald-500 bg-emerald-500 text-white shadow-emerald-500/20"
                        : "border-slate-200 bg-white text-slate-600 hover:bg-slate-100 hover:border-slate-300"
                    )}
                  >
                    {dept}
                  </button>
                ))}
              </div>

              {/* Search */}
              <div className="relative">
                <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                <Input
                  placeholder="Search by register number, name, department, or subject code..."
                  value={search}
                  onChange={(e) => {
                    setSearch(e.target.value);
                    setVisibleCount(20);
                  }}
                  className="pl-9 h-10 bg-white border-slate-200 shadow-sm rounded-xl text-sm"
                />
              </div>

              {/* Table Wrapper */}
              <div className="rounded-xl border border-slate-200 bg-white overflow-hidden shadow-sm">
                <div className="max-h-[350px] overflow-auto">
                  <table className="w-full text-xs">
                    <thead className="bg-slate-50 sticky top-0 z-10 shadow-sm">
                      <tr>
                        <th className="px-4 py-3 text-left font-bold text-slate-700 w-12 border-b border-slate-200">#</th>
                        <th className="px-4 py-3 text-left font-bold text-slate-700 border-b border-slate-200">Reg No</th>
                        <th className="px-4 py-3 text-left font-bold text-slate-700 border-b border-slate-200">Name</th>
                        <th className="px-4 py-3 text-left font-bold text-slate-700 border-b border-slate-200">Department</th>
                        <th className="px-4 py-3 text-left font-bold text-slate-700 border-b border-slate-200">Subject Code</th>
                      </tr>
                    </thead>
                    <tbody>
                      {visible.length === 0 ? (
                        <tr>
                          <td colSpan={5} className="text-center text-slate-500 py-10 italic">
                            No students match your filter.
                          </td>
                        </tr>
                      ) : (
                        visible.map((s, i) => (
                          <tr key={i} className="border-b border-slate-50 hover:bg-slate-50/80 transition-colors">
                            <td className="px-4 py-2.5 text-slate-400 font-mono text-[10px]">
                              {i + 1}
                            </td>
                            <td className="px-4 py-2.5 font-bold font-mono text-slate-700">
                              {s.registerNumber}
                            </td>
                            <td className="px-4 py-2.5 text-slate-700 font-medium">
                              {s.name.replace(/^\d+[\s.]+\s*/, "")}
                            </td>
                            <td className="px-4 py-2.5">
                              <Badge variant="outline" className="bg-slate-100 text-slate-700 font-bold border-none">
                                {s.department}
                              </Badge>
                            </td>
                            <td className="px-4 py-2.5 font-mono text-emerald-700 font-bold">
                              {s.subjectCode || s.subjectName || <span className="text-slate-400 italic font-sans font-normal">Not Provided</span>}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
                
                {/* Footer / Load More */}
                <div className="bg-slate-50 border-t border-slate-100 p-3 flex items-center justify-between">
                  <span className="text-xs text-slate-500 font-medium ml-2">
                    Showing {visible.length} of {filtered.length} students
                  </span>
                  {visibleCount < filtered.length && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setVisibleCount((c) => c + 50)}
                      className="text-xs h-8 rounded-lg bg-white shadow-sm border-slate-200 hover:bg-slate-100"
                    >
                      Load More Students
                    </Button>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
