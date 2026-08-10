import { useState, useMemo, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { getBatches, downloadBatchPdf, downloadBatchExcel, downloadBatchSummaryExcel } from "@/api/allocationApi";
import { downloadInternalBatchPdf, downloadInternalBatchExcel, downloadInternalBatchSummaryExcel } from "@/api/internalApi";
import apiClient from "@/api/axios";
import { Download, Search, FileText, ClipboardList, CheckCircle2, RefreshCw, Calendar, Layers, Clock, Hash } from "lucide-react";
import { toast } from "sonner";

interface BatchHistoryItem {
  id: string;
  status: string;
  createdAt: string;
  examSession?: {
    id: string;
    seasonId?: string;
    date?: string;
    session?: string;
    examType?: string;
    name?: string;
  };
}

interface PastAllocationsCardProps {
  isInternal?: boolean;
}

export function PastAllocationsCard({ isInternal = false }: PastAllocationsCardProps) {
  const [selectedSeasonId, setSelectedSeasonId] = useState<string>("");
  const [selectedDate, setSelectedDate] = useState<string>("");
  const [selectedSessionTime, setSelectedSessionTime] = useState<string>("");
  const [selectedBatchId, setSelectedBatchId] = useState<string>("");
  const [downloadingType, setDownloadingType] = useState<string | null>(null);

  const { data: allBatches = [], isLoading, refetch } = useQuery<BatchHistoryItem[]>({
    queryKey: ["all-batches-history", isInternal],
    queryFn: async () => {
      const data = await getBatches();
      return (data as BatchHistoryItem[]).filter((b) => {
        if (!b.id || b.status === "FAILED") return false;
        const examType = b.examSession?.examType?.toUpperCase() || "";
        if (isInternal) {
          return examType.includes("INT") || examType.includes("INTERNAL") || examType.includes("MODEL");
        } else {
          return !examType.includes("INT") && !examType.includes("INTERNAL");
        }
      }).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    },
  });

  // Step 1: Unique Season IDs
  const seasonIdOptions = useMemo(() => {
    const set = new Set<string>();
    for (const b of allBatches) {
      const s = b.examSession?.seasonId || "ACADEMIC YEAR 2026-2027";
      set.add(s);
    }
    return Array.from(set).sort();
  }, [allBatches]);

  // Step 2: Unique Exam Dates for selected Season ID
  const dateOptions = useMemo(() => {
    if (!selectedSeasonId) return [];
    const set = new Set<string>();
    for (const b of allBatches) {
      const s = b.examSession?.seasonId || "ACADEMIC YEAR 2026-2027";
      if (s === selectedSeasonId && b.examSession?.date) {
        set.add(b.examSession.date);
      }
    }
    return Array.from(set).sort();
  }, [allBatches, selectedSeasonId]);

  // All batches matching selected Season ID + Exam Date
  const availableBatchesForDate = useMemo(() => {
    if (!selectedSeasonId || !selectedDate) return [];
    return allBatches.filter((b) => {
      const s = b.examSession?.seasonId || "ACADEMIC YEAR 2026-2027";
      const d = b.examSession?.date || "";
      return s === selectedSeasonId && d === selectedDate;
    });
  }, [allBatches, selectedSeasonId, selectedDate]);

  // De-duplicated unique Session Times for selected Season + Date (e.g. ["FN", "AN"])
  const sessionTimesForDate = useMemo(() => {
    const set = new Set<string>();
    for (const b of availableBatchesForDate) {
      set.add(b.examSession?.session?.toUpperCase() || "FN");
    }
    return Array.from(set).sort();
  }, [availableBatchesForDate]);

  // Filtered batch options matching active session time
  const batchOptionsForSession = useMemo(() => {
    if (!selectedSessionTime) return availableBatchesForDate;
    return availableBatchesForDate.filter((b) => {
      const sess = b.examSession?.session?.toUpperCase() || "FN";
      return sess === selectedSessionTime;
    });
  }, [availableBatchesForDate, selectedSessionTime]);

  // Auto-select first session time when Date is selected
  useEffect(() => {
    if (sessionTimesForDate.length > 0) {
      if (!selectedSessionTime || !sessionTimesForDate.includes(selectedSessionTime)) {
        setSelectedSessionTime(sessionTimesForDate[0]);
      }
    } else {
      setSelectedSessionTime("");
    }
  }, [sessionTimesForDate, selectedSessionTime]);

  // Auto-select latest batch ID when session or date changes
  useEffect(() => {
    if (batchOptionsForSession.length > 0) {
      const exists = batchOptionsForSession.some((b) => b.id === selectedBatchId);
      if (!exists) {
        setSelectedBatchId(batchOptionsForSession[0].id);
      }
    } else {
      setSelectedBatchId("");
    }
  }, [batchOptionsForSession, selectedBatchId]);

  // Active selected batch object
  const selectedBatch = useMemo(() => {
    if (!selectedBatchId) {
      return batchOptionsForSession[0] || availableBatchesForDate[0] || null;
    }
    return allBatches.find((b) => b.id === selectedBatchId) || batchOptionsForSession[0] || null;
  }, [allBatches, selectedBatchId, batchOptionsForSession, availableBatchesForDate]);

  const handleDownload = async (batchId: string, type: "pdf" | "excel" | "summary" | "duty") => {
    setDownloadingType(type);
    try {
      if (type === "pdf") {
        const { blob, filename } = isInternal 
          ? await downloadInternalBatchPdf(batchId)
          : await downloadBatchPdf(batchId);
        triggerFileSave(blob, filename);
      } else if (type === "excel") {
        const { blob, filename } = isInternal
          ? await downloadInternalBatchExcel(batchId)
          : await downloadBatchExcel(batchId);
        triggerFileSave(blob, filename);
      } else if (type === "summary") {
        const { blob, filename } = isInternal
          ? await downloadInternalBatchSummaryExcel(batchId)
          : await downloadBatchSummaryExcel(batchId);
        triggerFileSave(blob, filename);
      } else if (type === "duty") {
        const response = await apiClient.get(`/duties/batch/${batchId}/duty-schedule-excel`, { responseType: "blob" });
        let filename = `Duty_Schedule_${batchId.substring(0, 8)}.xlsx`;
        const contentDisposition = response.headers["content-disposition"];
        if (contentDisposition) {
          const match = contentDisposition.match(/filename="?([^";]+)"?/);
          if (match && match[1]) filename = match[1];
        }
        triggerFileSave(new Blob([response.data as BlobPart]), filename);
      }
      toast.success("Download started successfully!");
    } catch {
      toast.error("Failed to download file. Please try again.");
    } finally {
      setDownloadingType(null);
    }
  };

  const triggerFileSave = (blob: Blob, filename: string) => {
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  // Helper formatting for clean professional display
  const formatSeasonLabel = (str: string) => {
    if (!str) return "ACADEMIC SESSION";
    const s = str.trim().toUpperCase();
    if (s.startsWith("CIA")) {
      return s.replace("CIA", "CIA - ").replace(/\s+/g, " ");
    }
    return s;
  };

  const formatDisplayDate = (dStr: string) => {
    if (!dStr) return "";
    try {
      const parts = dStr.split("-");
      if (parts.length === 3) {
        return `${parts[2]}.${parts[1]}.${parts[0]}`; // Convert 2026-08-08 -> 08.08.2026
      }
    } catch {
      // fallback
    }
    return dStr;
  };

  return (
    <Card className="border border-slate-200 bg-white shadow-xs rounded-xl overflow-hidden">
      <CardHeader className="bg-slate-50/70 border-b border-slate-200/80 p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-emerald-100 text-emerald-700 font-bold border border-emerald-200/60 shadow-2xs">
              <Search className="h-4 w-4" />
            </div>
            <div>
              <CardTitle className="text-sm font-bold text-slate-900 tracking-tight">
                {isInternal ? "Internal Exam Download Center" : "Semester Exam Download Center"}
              </CardTitle>
              <CardDescription className="text-xs text-slate-500">
                Select Season ID, Exam Date & Batch ID to retrieve seating plans, Excel rosters, and duty schedules
              </CardDescription>
            </div>
          </div>
          <Button variant="outline" size="sm" onClick={() => refetch()} className="h-8 text-xs bg-white text-slate-700 border-slate-200 hover:bg-slate-50 gap-1.5 font-semibold">
            <RefreshCw className="h-3.5 w-3.5 text-slate-500" />
            Refresh
          </Button>
        </div>
      </CardHeader>

      <CardContent className="p-5 space-y-5">
        {isLoading ? (
          <div className="py-6 text-center text-xs text-slate-500 flex items-center justify-center gap-2">
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-400 border-t-slate-800" />
            Loading exam sessions...
          </div>
        ) : (
          <>
            {/* 3-Filter Controls: Season ID -> Exam Date -> Batch ID */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Step 1: Select Season ID */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                  <Layers className="h-3.5 w-3.5 text-emerald-600" />
                  1. Select Season ID
                </label>
                <Select
                  value={selectedSeasonId}
                  onValueChange={(val) => {
                    setSelectedSeasonId(val);
                    setSelectedDate("");
                    setSelectedSessionTime("");
                    setSelectedBatchId("");
                  }}
                >
                  <SelectTrigger className="h-9 text-xs bg-white border-slate-200 font-semibold text-slate-900">
                    <SelectValue placeholder="Choose Season ID..." />
                  </SelectTrigger>
                  <SelectContent>
                    {seasonIdOptions.map((s) => (
                      <SelectItem key={s} value={s} className="text-xs font-semibold">
                        {formatSeasonLabel(s)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Step 2: Select Date */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                  <Calendar className="h-3.5 w-3.5 text-emerald-600" />
                  2. Select Exam Date
                </label>
                <Select
                  value={selectedDate}
                  onValueChange={(val) => {
                    setSelectedDate(val);
                    setSelectedSessionTime("");
                    setSelectedBatchId("");
                  }}
                  disabled={!selectedSeasonId || dateOptions.length === 0}
                >
                  <SelectTrigger className="h-9 text-xs bg-white border-slate-200 font-semibold text-slate-900">
                    <SelectValue placeholder={!selectedSeasonId ? "Select Season ID first" : "Choose Exam Date..."} />
                  </SelectTrigger>
                  <SelectContent>
                    {dateOptions.map((d) => (
                      <SelectItem key={d} value={d} className="text-xs font-semibold">
                        {formatDisplayDate(d)} ({d})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Step 3: Select Batch ID */}
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                  <Hash className="h-3.5 w-3.5 text-emerald-600" />
                  3. Select Batch ID
                </label>
                <Select
                  value={selectedBatchId}
                  onValueChange={setSelectedBatchId}
                  disabled={!selectedDate || batchOptionsForSession.length === 0}
                >
                  <SelectTrigger className="h-9 text-xs bg-white border-slate-200 font-semibold text-slate-900">
                    <SelectValue placeholder={!selectedDate ? "Select Exam Date first" : "Choose Specific Batch..."} />
                  </SelectTrigger>
                  <SelectContent>
                    {batchOptionsForSession.map((b) => {
                      const createdTime = b.createdAt ? new Date(b.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : "";
                      const sessionName = b.examSession?.session || "FN";
                      return (
                        <SelectItem key={b.id} value={b.id} className="text-xs font-mono">
                          Batch #{b.id.substring(0, 8).toUpperCase()} ({sessionName} • {createdTime || 'Latest'})
                        </SelectItem>
                      );
                    })}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Targeted Download Hub for Selected Date & Batch */}
            {selectedBatch ? (
              <div className="p-4 rounded-xl border border-emerald-200/80 bg-emerald-50/30 space-y-3 animate-in fade-in-50 duration-300 shadow-2xs">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-emerald-200/60 pb-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                      <span className="text-xs font-bold text-slate-900 uppercase">
                        {formatSeasonLabel(selectedSeasonId)} • DATE: {formatDisplayDate(selectedDate)}
                      </span>
                      <Badge variant="outline" className="text-[10px] font-mono bg-white text-slate-700 border-slate-300">
                        Batch #{selectedBatch.id.substring(0, 8).toUpperCase()}
                      </Badge>
                    </div>

                    {/* Session Switcher (FN / AN) if both exist for date */}
                    <div className="flex items-center gap-2 pt-1">
                      <span className="text-[11px] font-semibold text-slate-500 flex items-center gap-1">
                        <Clock className="h-3 w-3 text-slate-400" /> Session:
                      </span>
                      {sessionTimesForDate.map((st) => {
                        const isSelected = selectedSessionTime === st;
                        return (
                          <Button
                            key={st}
                            size="sm"
                            variant={isSelected ? "default" : "outline"}
                            className={`h-6 text-[10px] px-2.5 font-bold ${
                              isSelected ? "bg-emerald-600 text-white hover:bg-emerald-700" : "bg-white text-slate-700 border-slate-300 hover:bg-slate-50"
                            }`}
                            onClick={() => {
                              setSelectedSessionTime(st);
                              const firstInSess = availableBatchesForDate.find((b) => (b.examSession?.session?.toUpperCase() || "FN") === st);
                              if (firstInSess) setSelectedBatchId(firstInSess.id);
                            }}
                          >
                            {st === "FN" ? "Forenoon (FN)" : st === "AN" ? "Afternoon (AN)" : st}
                          </Button>
                        );
                      })}
                    </div>
                  </div>

                  <Badge className="bg-emerald-100 text-emerald-800 border border-emerald-200 font-bold text-[10px] w-fit shrink-0">
                    ✓ ACTIVE BATCH SELECTED
                  </Badge>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={downloadingType === "pdf"}
                    onClick={() => handleDownload(selectedBatch.id, "pdf")}
                    className="h-9 text-xs gap-1.5 font-semibold text-slate-800 bg-white border-slate-200 hover:bg-slate-50 shadow-2xs"
                  >
                    {downloadingType === "pdf" ? (
                      <span className="h-3 w-3 animate-spin rounded-full border-2 border-slate-400 border-t-slate-800" />
                    ) : (
                      <FileText className="h-3.5 w-3.5 text-red-600" />
                    )}
                    PDF Report
                  </Button>

                  <Button
                    variant="outline"
                    size="sm"
                    disabled={downloadingType === "excel"}
                    onClick={() => handleDownload(selectedBatch.id, "excel")}
                    className="h-9 text-xs gap-1.5 font-semibold text-slate-800 bg-white border-slate-200 hover:bg-slate-50 shadow-2xs"
                  >
                    {downloadingType === "excel" ? (
                      <span className="h-3 w-3 animate-spin rounded-full border-2 border-slate-400 border-t-slate-800" />
                    ) : (
                      <Download className="h-3.5 w-3.5 text-emerald-600" />
                    )}
                    Seating Excel
                  </Button>

                  <Button
                    variant="outline"
                    size="sm"
                    disabled={downloadingType === "duty"}
                    onClick={() => handleDownload(selectedBatch.id, "duty")}
                    className="h-9 text-xs gap-1.5 font-semibold text-orange-800 bg-orange-50 border-orange-200 hover:bg-orange-100 shadow-2xs"
                  >
                    {downloadingType === "duty" ? (
                      <span className="h-3 w-3 animate-spin rounded-full border-2 border-orange-400 border-t-orange-800" />
                    ) : (
                      <ClipboardList className="h-3.5 w-3.5 text-orange-600" />
                    )}
                    Duty Schedule
                  </Button>

                  <Button
                    variant="outline"
                    size="sm"
                    disabled={downloadingType === "summary"}
                    onClick={() => handleDownload(selectedBatch.id, "summary")}
                    className="h-9 text-xs gap-1.5 font-semibold text-blue-800 bg-blue-50 border-blue-200 hover:bg-blue-100 shadow-2xs"
                  >
                    {downloadingType === "summary" ? (
                      <span className="h-3 w-3 animate-spin rounded-full border-2 border-blue-400 border-t-blue-800" />
                    ) : (
                      <Download className="h-3.5 w-3.5 text-blue-600" />
                    )}
                    Account Summary
                  </Button>
                </div>
              </div>
            ) : (
              <div className="py-4 text-center text-xs text-slate-500 border border-dashed border-slate-200 rounded-lg bg-slate-50/50 font-medium">
                Select Season ID and Exam Date above to reveal instant download buttons.
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
