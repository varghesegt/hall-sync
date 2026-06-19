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
  getInternalBatchPreview,
} from "@/api/internalApi";
import type { AllocationStatus } from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";
import { StatusBadge } from "@/components/StatusBadge";
import { PreviewPanel } from "@/components/PreviewPanel";
import { AlertCircle, Download, Play, ShieldCheck, ShieldAlert } from "lucide-react";
import { getIntegritySummary, downloadIntegrityCertificate, type IntegritySummary } from "@/api/allocationApi";
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
    enabled: !!sessionId && isTriggered,
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
    if (!sessionId || trigger.isPending || isTriggered) return;
    trigger.mutate();
  };

  return (
    <Card className={cn(disabled && "opacity-60")}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base">4. Run Internal Allocation</CardTitle>
            <CardDescription>
              {!sessionId
                ? "Create session first"
                : selectedRooms.length === 0
                ? "Select rooms first"
                : `${selectedRooms.length} room${selectedRooms.length !== 1 ? "s" : ""} ready • 7×6 Grid`}
            </CardDescription>
          </div>
          {status !== "NOT_STARTED" && <StatusBadge status={status} />}
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {/* Trigger */}
        {!isTriggered && (
          <Button
            onClick={handleTrigger}
            disabled={disabled || trigger.isPending}
            className="w-full gap-2 bg-emerald-600 hover:bg-emerald-700"
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
        {status === "ACTIVE" && resolvedBatchId && (
          <>
            <div className="grid grid-cols-2 gap-2">
              <Button
                variant="outline"
                onClick={async () => { setIsDownloadingPdf(true); try { await downloadFile(downloadInternalBatchPdf); } finally { setIsDownloadingPdf(false); }}}
                disabled={isDownloadingPdf}
                className="gap-2"
              >
                {isDownloadingPdf ? (
                  <><span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" /> PDF…</>
                ) : (
                  <><Download className="h-4 w-4" /> PDF</>
                )}
              </Button>

              <Button
                variant="outline"
                onClick={async () => { setIsDownloadingExcel(true); try { await downloadFile(downloadInternalBatchExcel); } finally { setIsDownloadingExcel(false); }}}
                disabled={isDownloadingExcel}
                className="gap-2"
              >
                {isDownloadingExcel ? (
                  <><span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" /> Excel…</>
                ) : (
                  <><Download className="h-4 w-4" /> Excel</>
                )}
              </Button>
            </div>

            <Button
              variant="secondary"
              onClick={async () => { setIsDownloadingSummary(true); try { await downloadFile(downloadInternalBatchSummaryExcel); } finally { setIsDownloadingSummary(false); }}}
              disabled={isDownloadingSummary}
              className="w-full gap-2"
            >
              {isDownloadingSummary ? (
                <><span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" /> Wait Summary…</>
              ) : (
                <><Download className="h-4 w-4" /> Download Account Opening Summary (Excel)</>
              )}
            </Button>
          </>
        )}

        {/* Preview Panel */}
        <PreviewPanel 
          batchId={resolvedBatchId ?? null} 
          status={status} 
          onUpdate={refreshIntegrity}
        />

        {/* Integrity Certificate */}
        {status === "ACTIVE" && resolvedBatchId && (
          <div className="space-y-2">
            {integrity && (
              <div>
                <div className={cn(
                  "flex items-center gap-2 rounded-lg p-3 text-sm font-medium",
                  integrity.certified
                    ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                    : "bg-red-50 text-red-700 border border-red-200"
                )}>
                  {integrity.certified
                    ? <ShieldCheck className="h-5 w-5 text-emerald-600" />
                    : <ShieldAlert className="h-5 w-5 text-red-600" />}
                  <div className="flex-1">
                    <div className="font-bold text-xs uppercase tracking-wider">
                      {integrity.certified ? "Integrity Verified" : "Violations Detected"}
                    </div>
                    <div className="text-[11px] opacity-80 mt-0.5">
                      {integrity.integrityScore}% Subject Separation • {integrity.deptPurityScore}% Dept Purity • {integrity.totalAdjacencyChecks.toLocaleString()} checks
                    </div>
                  </div>
                </div>

                {/* Per-Hall Violation Details — only shown when integrity < 100% */}
                {!integrity.certified && integrity.hallViolations && integrity.hallViolations.length > 0 && (
                  <details className="mt-2 rounded-lg border border-red-200 bg-red-50/50 overflow-hidden">
                    <summary className="cursor-pointer px-3 py-2 text-xs font-bold text-red-700 uppercase tracking-wider hover:bg-red-100/50 transition-colors select-none">
                      ⚠ {integrity.hallViolations.length} Hall{integrity.hallViolations.length > 1 ? 's' : ''} with Issues — Click to expand
                    </summary>
                    <div className="px-3 pb-3 space-y-2 max-h-60 overflow-y-auto">
                      {integrity.hallViolations.map((hv, idx) => (
                        <div key={idx} className="rounded-md bg-white border border-red-100 p-2">
                          <div className="flex items-center justify-between">
                            <span className="font-bold text-xs text-red-800">{hv.hallName}</span>
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-red-100 text-red-700 font-semibold">
                              {hv.violations} violation{hv.violations > 1 ? 's' : ''} • {hv.studentCount} students
                            </span>
                          </div>
                          {hv.details.length > 0 && (
                            <ul className="mt-1.5 space-y-0.5">
                              {hv.details.slice(0, 5).map((detail, dIdx) => (
                                <li key={dIdx} className="text-[10px] text-red-600 font-mono leading-tight pl-2 border-l-2 border-red-200">
                                  {detail}
                                </li>
                              ))}
                              {hv.details.length > 5 && (
                                <li className="text-[10px] text-red-400 italic pl-2">
                                  + {hv.details.length - 5} more violations in this hall
                                </li>
                              )}
                            </ul>
                          )}
                        </div>
                      ))}
                      <p className="text-[10px] text-red-500 italic mt-1">
                        💡 Navigate to these halls using the preview, click Edit to swap students and fix violations.
                      </p>
                    </div>
                  </details>
                )}
              </div>
            )}
            <Button
              variant="outline"
              onClick={async () => { setIsDownloadingCert(true); try { await downloadFile(downloadIntegrityCertificate); } finally { setIsDownloadingCert(false); }}}
              disabled={isDownloadingCert}
              className={cn(
                "w-full gap-2 font-semibold",
                integrity?.certified
                  ? "border-emerald-300 text-emerald-700 hover:bg-emerald-50"
                  : ""
              )}
            >
              {isDownloadingCert ? (
                <>
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" />
                  Generating…
                </>
              ) : (
                <>
                  <ShieldCheck className="h-4 w-4" />
                  Download Integrity Certificate (PDF)
                </>
              )}
            </Button>
          </div>
        )}

        {resolvedBatchId && (
          <p className="text-xs text-muted-foreground">
            Batch ID: <span className="font-mono">{resolvedBatchId.slice(0, 12)}…</span>
          </p>
        )}
      </CardContent>
    </Card>
  );
}
