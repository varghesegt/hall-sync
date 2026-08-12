import { useState, useEffect } from "react";
import { UploadCard } from '@/features/claims/components/UploadCard';
import { StudentPreviewTable } from "@/features/allocations/components/StudentPreviewTable";
import { SessionCard } from "@/features/allocations/components/SessionCard";
import { AllocationCard } from "@/features/allocations/components/AllocationCard";
import { PastAllocationsCard } from "@/features/allocations/components/PastAllocationsCard";
import { RoomSelector } from "@/features/allocations/components/RoomSelector";
import { StepIndicator } from "@/components/StepIndicator";

export default function Dashboard() {
  const [fileId, setFileId] = useState<string | null>(() => sessionStorage.getItem("hall_sync_semester_file_id"));
  const [sessionId, setSessionId] = useState<string | null>(() => sessionStorage.getItem("hall_sync_semester_session_id"));
  const [totalStudents, setTotalStudents] = useState<number | null>(null);
  const [selectedRooms, setSelectedRooms] = useState<string[]>([]);

  useEffect(() => {
    if (fileId) sessionStorage.setItem("hall_sync_semester_file_id", fileId);
  }, [fileId]);

  useEffect(() => {
    if (sessionId) sessionStorage.setItem("hall_sync_semester_session_id", sessionId);
  }, [sessionId]);

  const steps = [
    { label: "Upload", completed: !!fileId, active: !fileId },
    { label: "Session", completed: !!sessionId, active: !!fileId && !sessionId },
    { label: "Rooms", completed: selectedRooms.length > 0, active: !!sessionId },
    { label: "Allocate", completed: false, active: !!sessionId && selectedRooms.length > 0 },
  ];

  return (
    <div className="w-full h-full min-h-screen">
      <main className="mx-auto max-w-5xl space-y-8 animate-in fade-in slide-in-from-bottom-8 duration-1000">
        <div className="mb-4">
          <StepIndicator steps={steps} />
        </div>

        <div className="grid gap-8">
          <div className="animate-in fade-in slide-in-from-bottom-6 duration-1000 delay-100">
            <UploadCard
              fileId={fileId}
              onSuccess={(fid) => {
                if (!fid) {
                  setFileId(null);
                  setSessionId(null);
                  sessionStorage.removeItem("hall_sync_semester_file_id");
                  sessionStorage.removeItem("hall_sync_semester_session_id");
                } else {
                  setFileId(fid);
                  sessionStorage.setItem("hall_sync_semester_file_id", fid);
                }
              }}
            />
          </div>

          <div className="animate-in fade-in slide-in-from-bottom-6 duration-1000 delay-200">
            <StudentPreviewTable fileId={fileId} />
          </div>

          <div className="animate-in fade-in slide-in-from-bottom-6 duration-1000 delay-300">
            <SessionCard
              fileId={fileId}
              sessionId={sessionId}
              onSuccess={(sid, count) => {
                setSessionId(sid);
                setTotalStudents(count);
                sessionStorage.setItem("hall_sync_semester_session_id", sid);
              }}
            />
          </div>

          {sessionId && (
            <div className="animate-in fade-in slide-in-from-bottom-6 duration-1000 delay-400">
              <RoomSelector
                onSelectionChange={setSelectedRooms}
                totalStudents={totalStudents}
                isInternal={false}
              />
            </div>
          )}

          <div className="animate-in fade-in slide-in-from-bottom-6 duration-1000 delay-500">
            <AllocationCard sessionId={sessionId} selectedRooms={selectedRooms} />
          </div>

          {/* Dedicated Semester Exam Allocation History & Downloads */}
          <div className="animate-in fade-in slide-in-from-bottom-6 duration-1000 delay-600 pt-4">
            <PastAllocationsCard isInternal={false} />
          </div>
        </div>
      </main>

      <footer className="w-full mt-32 border-t border-white/30 bg-white/20 backdrop-blur-3xl shadow-[0_-10px_40px_-15px_rgba(0,0,0,0.05)]">
        <div className="mx-auto max-w-6xl px-8 py-12 flex flex-col md:flex-row items-center justify-between gap-8">
          <div className="flex flex-col items-center md:items-start gap-1">
            <h3 className="text-xl font-black tracking-tighter text-slate-900 drop-shadow-sm">
              Hall<span className="text-primary bg-clip-text text-transparent bg-gradient-to-r from-primary to-purple-600">Sync</span>
            </h3>
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-[0.2em] mt-1">
              Examination Management System
            </p>
          </div>

          <div className="flex flex-col items-center md:items-end gap-1.5">
            <div className="flex items-center gap-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest">
              <span>Developed By</span>
              <span className="text-slate-900 font-black tracking-tight bg-white/50 px-3 py-1 rounded-full shadow-sm border border-white/60">
                Varghese G T
              </span>
            </div>
            <p className="text-[9px] font-black text-slate-400 uppercase tracking-[0.25em]">
              Department of Mechanical Engineering
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
