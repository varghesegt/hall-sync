import { useState, useEffect, useCallback } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  triggerInternalAllocation,
  getInternalLatestBatch,
  downloadInternalBatchPdf,
  downloadInternalBatchExcel,
  downloadInternalBatchSummaryExcel,
} from "@/api/internalApi";
import type { AllocationStatus } from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";
import { StatusBadge } from "@/components/StatusBadge";
import { PreviewPanel } from "@/components/PreviewPanel";
import { IntegrityAuditBanner } from "@/components/IntegrityAuditBanner";
import { AlertCircle, Download, Play, ClipboardList, ShieldCheck } from "lucide-react";
import { getIntegritySummary, type IntegritySummary } from "@/api/allocationApi";
import { cn } from "@/lib/utils";

interface InternalAllocationCardProps {
  sessionId: string | null;
  selectedRooms?: string[];
}

export function InternalAllocationCard({ sessionId, selectedRooms = [] }: InternalAllocationCardProps) {
  const [isTriggered, setIsTriggered] = useState(false);
  const [batchId, setBatchId] = useState<string | null>(null);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [isDownloadingExcel, setIsDownloadingExcel] = useState(false);
  const [isDownloadingSummary, setIsDownloadingSummary] = useState(false);
  const [isDownloadingSchedule, setIsDownloadingSchedule] = useState(false);
  const [isDownloadingCert, setIsDownloadingCert] = useState(false);
  const [integrity, setIntegrity] = useState<IntegritySummary | null>(null);

  const trigger = useMutation<{ batchId: string }, ApiError, void>({
    mutationFn: () => {
      if (!sessionId) throw new Error("No session ID");
      return triggerInternalAllocation({ sessionId, selectedRooms });
    },
    onSuccess: (data) => {
      setBatchId(data.batchId);
      setIsTriggered(true);
    },
  });

  const polling = useQuery({
    queryKey: ["internal-batch-status", sessionId],
    queryFn: () => getInternalLatestBatch(sessionId!),
    enabled: !!sessionId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === "ACTIVE" || status === "FAILED") return false;
      return 3000;
    },
    retry: 3,
    staleTime: 0,
  });

  const status: AllocationStatus = polling.data?.status ?? "NOT_STARTED";
  const resolvedBatchId = polling.data?.batchId ?? batchId;
  const disabled = !sessionId || selectedRooms.length === 0;
  const isPolling = isTriggered && (status === "NOT_STARTED" || status === "RUNNING");
  const hasActiveBatch = status === "ACTIVE" && !!resolvedBatchId;

  const refreshIntegrity = useCallback(() => {
    if (resolvedBatchId) {
      getIntegritySummary(resolvedBatchId)
        .then(setIntegrity)
        .catch(() => setIntegrity(null));
    }
  }, [resolvedBatchId]);

  useEffect(() => {
    if (status === "ACTIVE") refreshIntegrity();
  }, [status, refreshIntegrity]);

  const handleDownloadCertificate = async () => {
    if (!resolvedBatchId) return;
    setIsDownloadingCert(true);
    try {
      const { downloadIntegrityCertificate } = await import("@/api/allocationApi");
      const { blob, filename } = await downloadIntegrityCertificate(resolvedBatchId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Failed to download Integrity Certificate:", error);
      alert("Failed to download Integrity Certificate. Please try again.");
    } finally {
      setIsDownloadingCert(false);
    }
  };

  const downloadFile = useCallback(async (downloadFn: (id: string) => Promise<{blob: Blob, filename: string}>) => {
    if (!resolvedBatchId) return;
    const { blob, filename } = await downloadFn(resolvedBatchId);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, [resolvedBatchId]);

  const handleTrigger = () => {
    if (!sessionId || trigger.isPending || isTriggered || status === "ACTIVE") return;
    trigger.mutate();
  };

  const hasActiveBatch = status === "ACTIVE" && !!resolvedBatchId;

  return (
    <Card className={cn("transition-all duration-500 overflow-hidden glass-panel", disabled && !hasActiveBatch && "opacity-60")}>
      <CardHeader className="bg-slate-50/50 border-b border-slate-100/50 pb-5">
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base font-bold text-slate-900">4. Run Internal Allocation</CardTitle>
            <CardDescription>
              {!sessionId
                ? "Create session first"
                : selectedRooms.length === 0 && !hasActiveBatch
                ? "Select rooms first"
                : `${selectedRooms.length || 'Ready'} rooms • 7×6 Grid`}
            </CardDescription>
          </div>
          {status !== "NOT_STARTED" && <StatusBadge status={status} />}
        </div>
      </CardHeader>
      <CardContent className="space-y-3 pt-6">
        {/* Trigger */}
        {(!isTriggered && status !== "ACTIVE") && (
          <Button
            onClick={handleTrigger}
            disabled={disabled || trigger.isPending}
            className="w-full gap-2 bg-emerald-600 hover:bg-emerald-700 font-semibold"
          >
            {trigger.isPending ? (
              <>
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-primary-foreground/30 border-t-primary-foreground" />
                Starting…
              </>
            ) : (
              <>
                <Play className="h-4 w-4" />
                Run Internal Allocation
              </>
            )}
          </Button>
        )}

        {/* Trigger error */}
        {trigger.isError && (
          <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <span>{trigger.error.message}</span>
          </div>
        )}

        {/* Polling state */}
        {isPolling && (
          <div className="flex items-center gap-2 rounded-md border bg-muted/50 p-3 text-sm text-muted-foreground">
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-muted-foreground" />
            Internal allocation in progress… Polling for updates.
          </div>
        )}

        {/* Failed */}
        {status === "FAILED" && (
          <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <span>Internal allocation failed. Please create a new session and try again.</span>
          </div>
        )}

        {/* Downloads */}
        {hasActiveBatch && (
          <div className="space-y-3 pt-2">
            {/* Integrity Audit Banner */}
            <IntegrityAuditBanner
              integrity={integrity}
              onDownloadCertificate={handleDownloadCertificate}
              isDownloadingCert={isDownloadingCert}
            />

            <div className="grid grid-cols-2 gap-2">
              <Button
                variant="outline"
                onClick={async () => { setIsDownloadingPdf(true); try { await downloadFile(downloadInternalBatchPdf); } finally { setIsDownloadingPdf(false); }}}
                disabled={isDownloadingPdf}
                className="gap-2 font-semibold text-slate-800"
              >
                {isDownloadingPdf ? (
                  <><span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" /> PDF…</>
                ) : (
                  <><Download className="h-4 w-4 text-red-500" /> Download PDF</>
                )}
              </Button>

              <Button
                variant="outline"
                onClick={async () => { setIsDownloadingExcel(true); try { await downloadFile(downloadInternalBatchExcel); } finally { setIsDownloadingExcel(false); }}}
                disabled={isDownloadingExcel}
                className="gap-2 font-semibold text-slate-800"
              >
                {isDownloadingExcel ? (
                  <><span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" /> Excel…</>
                ) : (
                  <><Download className="h-4 w-4 text-emerald-600" /> Download Excel</>
                )}
              </Button>
            </div>

            <Button
              variant="secondary"
              onClick={async () => { setIsDownloadingSummary(true); try { await downloadFile(downloadInternalBatchSummaryExcel); } finally { setIsDownloadingSummary(false); }}}
              disabled={isDownloadingSummary}
              className="w-full gap-2 font-semibold text-slate-800"
            >
              {isDownloadingSummary ? (
                <><span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" /> Wait Summary…</>
              ) : (
                <><Download className="h-4 w-4 text-blue-600" /> Download Account Opening Summary (Excel)</>
              )}
            </Button>

            <Button
              variant="secondary"
              onClick={async () => {
                if (!resolvedBatchId) return;
                setIsDownloadingSchedule(true);
                try {
                  const { default: apiClient } = await import("@/api/axios");
                  const response = await apiClient.get(`/duties/batch/${resolvedBatchId}/duty-schedule-excel`, { responseType: "blob" });
                  const contentDisposition = response.headers["content-disposition"];
                  let filename = `Internal_Duty_Schedule.xlsx`;
                  if (contentDisposition) {
                    const match = contentDisposition.match(/filename="?([^";]+)"?/);
                    if (match && match[1]) filename = match[1];
                  }
                  const url = URL.createObjectURL(new Blob([response.data as BlobPart]));
                  const a = document.createElement("a");
                  a.href = url;
                  a.download = filename;
                  document.body.appendChild(a);
                  a.click();
                  document.body.removeChild(a);
                  URL.revokeObjectURL(url);
                } catch {
                  alert("Failed to download Duty Schedule. Please try again.");
                } finally {
                  setIsDownloadingSchedule(false);
                }
              }}
              disabled={isDownloadingSchedule}
              className="w-full gap-2 font-semibold border-orange-300 bg-orange-50 text-orange-700 hover:bg-orange-100"
            >
              {isDownloadingSchedule ? (
                <><span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" /> Generating…</>
              ) : (
                <><ClipboardList className="h-4 w-4 text-orange-600" /> Download Invigilation Duty Schedule (Excel)</>
              )}
            </Button>
          </div>
        )}

        {/* Preview Panel */}
        <PreviewPanel 
          batchId={resolvedBatchId ?? null} 
          status={status} 
          onUpdate={refreshIntegrity}
        />
      </CardContent>
    </Card>
  );
}
