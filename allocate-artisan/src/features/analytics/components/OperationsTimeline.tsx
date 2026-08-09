import React from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Clock, CheckCircle2, ArrowRight, ArrowUpRight, Activity, Archive, AlertCircle, CalendarClock } from "lucide-react";
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getBatches } from '@/api/allocationApi';
import { formatDistanceToNow, parseISO } from 'date-fns';

export function OperationsTimeline() {
  const navigate = useNavigate();

  const { data: batches } = useQuery({
    queryKey: ['batches-all'],
    queryFn: () => getBatches()
  });

  const allBatches = (batches as any[]) || [];
  
  const pendingTasks = allBatches.filter((b: any) => 
    b.status === "NOT_STARTED" || b.status === "RUNNING"
  ).sort((a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  const completedTasks = allBatches.filter((b: any) => 
    b.status === "COMPLETED" || b.status === "ARCHIVED"
  ).sort((a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  const recentOperations = [...allBatches]
    .sort((a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);

  const getStatusConfig = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return { color: 'text-emerald-600 bg-emerald-50 border-emerald-100', icon: CheckCircle2, text: 'Completed' };
      case 'ARCHIVED':
        return { color: 'text-slate-500 bg-slate-50 border-slate-200', icon: Archive, text: 'Archived' };
      case 'RUNNING':
        return { color: 'text-indigo-600 bg-indigo-50 border-indigo-100', icon: Activity, text: 'In Progress' };
      default:
        return { color: 'text-amber-600 bg-amber-50 border-amber-100', icon: Clock, text: 'Pending' };
    }
  };

  const handleContinue = (batch: any) => {
    if (batch.status === "NOT_STARTED" || batch.status === "RUNNING") {
      navigate(`/dashboard/duty`);
    } else {
      navigate(`/dashboard/history`);
    }
  };

  return (
    <div className="grid grid-cols-1 xl:grid-cols-3 gap-8 mt-8">
      
      {/* Reverse Chronological Timeline (Last Operations) */}
      <Card className="xl:col-span-2 shadow-sm border border-slate-200 bg-white rounded-xl overflow-hidden">
        <CardHeader className="border-b border-slate-100 bg-slate-50/50 px-8 py-5">
          <div className="flex justify-between items-center">
            <div>
              <CardTitle className="text-lg font-bold text-slate-800 flex items-center gap-2">
                <CalendarClock className="text-indigo-600" size={20} /> 
                Recent Activity & Operations
              </CardTitle>
              <CardDescription className="text-sm font-medium mt-1">
                Your last actions in reverse chronological order
              </CardDescription>
            </div>
            <Badge variant="outline" className="bg-white px-3 py-1 text-slate-700 font-bold border-slate-200 shadow-sm">
              {recentOperations.length} recent
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="divide-y divide-slate-50">
            {recentOperations.length === 0 ? (
              <div className="p-12 text-center text-slate-400 font-medium">No recent operations found.</div>
            ) : (
              recentOperations.map((batch, idx) => {
                const config = getStatusConfig(batch.status);
                const Icon = config.icon;
                
                return (
                  <div key={batch.id || idx} className="p-5 hover:bg-slate-50 transition-colors flex items-center justify-between group">
                    <div className="flex items-center gap-4">
                      <div className={`p-2.5 rounded-lg border ${config.color} shadow-sm group-hover:scale-105 transition-transform`}>
                        <Icon size={18} strokeWidth={2.5} />
                      </div>
                      <div>
                        <h4 className="font-semibold text-slate-800 text-base">
                          {batch.session?.examName || 'Exam Session'}
                        </h4>
                        <div className="flex items-center gap-3 mt-1 text-sm font-medium text-slate-500">
                          <span className="flex items-center gap-1">
                            <Clock size={14} /> 
                            {batch.createdAt ? formatDistanceToNow(parseISO(batch.createdAt), { addSuffix: true }) : 'Recently'}
                          </span>
                          <span className="w-1 h-1 rounded-full bg-slate-300"></span>
                          <span>{batch.session?.examDate || 'No date'}</span>
                        </div>
                      </div>
                    </div>
                    <Button 
                      variant="ghost" 
                      size="sm"
                      className="text-indigo-600 font-semibold hover:bg-indigo-50 rounded-lg"
                      onClick={() => handleContinue(batch)}
                    >
                      {batch.status === 'NOT_STARTED' || batch.status === 'RUNNING' ? 'Continue' : 'View'}
                      <ArrowRight size={16} className="ml-1.5" />
                    </Button>
                  </div>
                );
              })
            )}
          </div>
        </CardContent>
      </Card>

      {/* Task Breakdown (Pending vs Completed) */}
      <div className="flex flex-col gap-6">
        
        {/* Pending Works */}
        <Card className="shadow-sm border border-slate-200 bg-white rounded-xl overflow-hidden relative flex-1">
          <CardHeader className="px-6 py-4 border-b border-slate-100 bg-slate-50/50">
            <CardTitle className="text-base font-bold text-slate-800 flex justify-between items-center">
              Pending Works
              <span className="bg-indigo-600 text-white text-xs px-2.5 py-1 rounded-md">{pendingTasks.length}</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="divide-y divide-slate-100 max-h-[220px] overflow-y-auto">
              {pendingTasks.length === 0 ? (
                <div className="p-6 text-center text-slate-500 font-medium text-sm">All caught up! No pending works.</div>
              ) : (
                pendingTasks.slice(0, 3).map((task, idx) => (
                  <div key={idx} className="p-4 flex justify-between items-center hover:bg-slate-50 cursor-pointer transition-colors" onClick={() => handleContinue(task)}>
                    <div>
                      <p className="font-bold text-slate-800 text-sm">{task.session?.examName || 'Draft Allocation'} - {task.session?.examDate || ''}</p>
                      <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                        <AlertCircle size={12} className="text-amber-500" /> 
                        {task.status === "NOT_STARTED" ? "Seating Uploaded. Next: Run Staff Allocation" : "Allocation is currently running."}
                      </p>
                    </div>
                    <ArrowUpRight size={16} className="text-indigo-400 shrink-0 ml-2" />
                  </div>
                ))
              )}
            </div>
          </CardContent>
        </Card>

        {/* Completed Works */}
        <Card className="shadow-sm border border-slate-200 bg-white rounded-xl overflow-hidden relative flex-1">
          <CardHeader className="px-6 py-4 border-b border-slate-100 bg-slate-50/50">
            <CardTitle className="text-base font-bold text-slate-800 flex justify-between items-center">
              Completed Tasks
              <span className="bg-emerald-600 text-white text-xs px-2.5 py-1 rounded-md">{completedTasks.length}</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="divide-y divide-slate-100 max-h-[220px] overflow-y-auto">
              {completedTasks.length === 0 ? (
                <div className="p-6 text-center text-slate-500 font-medium text-sm">No completed tasks yet.</div>
              ) : (
                completedTasks.slice(0, 3).map((task, idx) => (
                  <div key={idx} className="p-4 flex justify-between items-center hover:bg-slate-50 cursor-pointer transition-colors" onClick={() => handleContinue(task)}>
                    <div>
                      <p className="font-bold text-slate-800 text-sm">{task.session?.examName || 'Completed Batch'}</p>
                      <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                        <CheckCircle2 size={12} className="text-emerald-500" /> Finalized & Verified
                      </p>
                    </div>
                    <ArrowUpRight size={16} className="text-emerald-400" />
                  </div>
                ))
              )}
            </div>
          </CardContent>
        </Card>

      </div>
    </div>
  );
}
