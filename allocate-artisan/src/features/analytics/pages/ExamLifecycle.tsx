import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import * as allocationApi from '@/api/allocationApi';
import { analyticsApi } from '@/api/analyticsApi';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card, CardContent } from '@/components/ui/card';
import { BatchSelector } from '@/features/claims/components/BatchSelector';
import { Calendar, GitCommit, CheckCircle2, Circle, AlertCircle } from "lucide-react";

export default function ExamLifecycle() {
  const [selectedBatchId, setSelectedBatchId] = useState<string | null>(null);

  // batches are now fetched and handled inside BatchSelector

  const { data: lifecycle, isLoading } = useQuery({
    queryKey: ["lifecycle", selectedBatchId],
    queryFn: () => selectedBatchId ? analyticsApi.getLifecycle(selectedBatchId).then(res => res.data) : null,
    enabled: !!selectedBatchId,
  });

  return (
    <div className="p-6 space-y-6">
      <PageHeader
        title="Exam Lifecycle Workflow"
        subtitle="Track the end-to-end operational timeline of any exam batch."
        icon={GitCommit}
      />

      <div className="flex flex-col md:flex-row gap-6">
        <Card className="w-full md:w-1/3">
          <CardContent className="p-4 space-y-4">
            <h3 className="font-semibold text-slate-700">Select Exam Batch</h3>
            <BatchSelector 
              selectedBatchId={selectedBatchId} 
              onBatchSelect={setSelectedBatchId} 
              className="flex-col"
            />
          </CardContent>
        </Card>

        <div className="w-full md:w-2/3">
          {selectedBatchId && lifecycle ? (
            <Card>
              <CardContent className="p-8">
                <div className="space-y-8">
                  {/* Before Exam */}
                  <div className="relative">
                    <div className="absolute top-4 left-4 -bottom-12 w-0.5 bg-slate-200" />
                    <div className="flex items-start gap-4 relative z-10">
                      <div className="mt-1 bg-white">
                        {lifecycle.qpReady && lifecycle.facultyReady && lifecycle.hallReady 
                          ? <CheckCircle2 className="w-8 h-8 text-emerald-500" />
                          : <Circle className="w-8 h-8 text-blue-500 fill-blue-50" />}
                      </div>
                      <div className="flex-1">
                        <h3 className="text-lg font-bold text-slate-800 mb-2">1. Before Exam</h3>
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                          <StatusCard title="Question Papers" isDone={lifecycle.qpReady} desc="Packets Received" />
                          <StatusCard title="Faculty Allocation" isDone={lifecycle.facultyReady} desc="Duties Assigned" />
                          <StatusCard title="Hall Readiness" isDone={lifecycle.hallReady} desc="Seating Planned" />
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* During Exam */}
                  <div className="relative">
                    <div className="absolute top-4 left-4 -bottom-12 w-0.5 bg-slate-200" />
                    <div className="flex items-start gap-4 relative z-10">
                      <div className="mt-1 bg-white">
                        {lifecycle.attendanceComplete 
                          ? <CheckCircle2 className="w-8 h-8 text-emerald-500" />
                          : <Circle className="w-8 h-8 text-slate-300" />}
                      </div>
                      <div className="flex-1">
                        <h3 className="text-lg font-bold text-slate-800 mb-2">2. During Exam</h3>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          <StatusCard title="Attendance" isDone={lifecycle.attendanceComplete} desc="Logs Submitted" />
                          <div className={`p-4 rounded-xl border ${lifecycle.malpracticesLogged > 0 ? "bg-red-50 border-red-200" : "bg-slate-50 border-slate-100"}`}>
                            <p className="text-sm font-semibold text-slate-700">Malpractices</p>
                            <p className={`text-2xl font-bold mt-1 ${lifecycle.malpracticesLogged > 0 ? "text-red-600" : "text-slate-600"}`}>
                              {lifecycle.malpracticesLogged}
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* After Exam */}
                  <div className="relative">
                    <div className="flex items-start gap-4 relative z-10">
                      <div className="mt-1 bg-white">
                        {lifecycle.isArchived 
                          ? <CheckCircle2 className="w-8 h-8 text-emerald-500" />
                          : <Circle className="w-8 h-8 text-slate-300" />}
                      </div>
                      <div className="flex-1">
                        <h3 className="text-lg font-bold text-slate-800 mb-2">3. After Exam</h3>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                          <StatusCard title="Permanent Archive" isDone={lifecycle.isArchived} desc="Snapshots Saved" />
                          <StatusCard title="Compliance" isDone={lifecycle.complianceMet} desc="Accreditation Ready" />
                        </div>
                      </div>
                    </div>
                  </div>

                </div>
              </CardContent>
            </Card>
          ) : (
            <div className="h-full min-h-[400px] flex flex-col items-center justify-center text-slate-400 border-2 border-dashed rounded-xl border-slate-200 bg-slate-50/50">
              <Calendar className="w-12 h-12 mb-4 text-slate-300" />
              <p>Select a batch to view its lifecycle timeline.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function StatusCard({ title, isDone, desc }: { title: string; isDone: boolean; desc: string }) {
  return (
    <div className={`p-4 rounded-xl border transition-colors ${isDone ? "bg-emerald-50/50 border-emerald-200" : "bg-slate-50 border-slate-100"}`}>
      <div className="flex items-center gap-2 mb-2">
        {isDone ? <CheckCircle2 className="w-4 h-4 text-emerald-500" /> : <AlertCircle className="w-4 h-4 text-amber-500" />}
        <p className="text-sm font-semibold text-slate-700">{title}</p>
      </div>
      <p className="text-xs text-slate-500">{desc}</p>
    </div>
  );
}
