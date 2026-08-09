import { useState, useEffect, useCallback } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAllocation } from "@/hooks/useAllocation";
import { StatusBadge } from "@/components/StatusBadge";
import { PreviewPanel } from "@/components/PreviewPanel";
import { AlertCircle, Download, Play, ShieldCheck, ShieldAlert } from "lucide-react";
import { getIntegritySummary, downloadIntegrityCertificate, type IntegritySummary } from "@/api/allocationApi";
import { cn } from "@/lib/utils";

interface AllocationCardProps {
  sessionId: string | null;
  selectedRooms?: string[];
}

export function AllocationCard({ sessionId, selectedRooms = [] }: AllocationCardProps) {
  const allocation = useAllocation(sessionId, selectedRooms);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [isDownloadingExcel, setIsDownloadingExcel] = useState(false);
  const [isDownloadingSummary, setIsDownloadingSummary] = useState(false);
  const [isDownloadingCert, setIsDownloadingCert] = useState(false);
  const [integrity, setIntegrity] = useState<IntegritySummary | null>(null);

  const refreshIntegrity = useCallback(() => {
    if (allocation.status === "ACTIVE" && allocation.batchId) {
      getIntegritySummary(allocation.batchId)
        .then(setIntegrity)
        .catch(() => setIntegrity(null));
    }
  }, [allocation.status, allocation.batchId]);

  // Auto-fetch integrity summary when allocation completes
  useEffect(() => {
    refreshIntegrity();
  }, [refreshIntegrity]);

  const disabled = !sessionId || selectedRooms.length === 0;

  const handleTrigger = () => {
    if (!sessionId || allocation.trigger.isPending || allocation.isTriggered) return;
    allocation.trigger.mutate();
  };

  const handleDownloadPdf = async () => {
    setIsDownloadingPdf(true);
    try {
      await allocation.downloadPdf();
    } catch (error) {
      console.error("Failed to download PDF:", error);
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  const handleDownloadExcel = async () => {
    setIsDownloadingExcel(true);
    try {
      await allocation.downloadExcel();
    } catch (error) {
      console.error("Failed to download Excel:", error);
    } finally {
      setIsDownloadingExcel(false);
    }
  };

  const handleDownloadSummary = async () => {
    setIsDownloadingSummary(true);
    try {
      await allocation.downloadSummaryExcel();
    } catch (error) {
      console.error("Failed to download Summary Excel:", error);
    } finally {
      setIsDownloadingSummary(false);
    }
  };

  const handleDownloadCertificate = async () => {
    if (!allocation.batchId) return;
    setIsDownloadingCert(true);
    try {
      const { blob, filename } = await downloadIntegrityCertificate(allocation.batchId);
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
    } finally {
      setIsDownloadingCert(false);
    }
  };

  return (
    <Card className={cn("transition-all duration-500 overflow-hidden glass-panel", disabled && "opacity-50 grayscale-[0.2]")}>
      <CardHeader className="bg-slate-50/50 border-b border-slate-100/50 pb-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
              <Play className="w-5 h-5" />
            </div>
            <div>
              <CardTitle className="text-lg font-black tracking-tight text-slate-900">4. Allocation Engine</CardTitle>
              <CardDescription className="text-xs font-semibold uppercase tracking-wider mt-1 text-slate-500">
                {!sessionId
                  ? "Awaiting session initialization..."
                  : selectedRooms.length === 0
                  ? "Awaiting physical environment selection..."
                  : `${selectedRooms.length} room${selectedRooms.length !== 1 ? "s" : ""} queued for processing`}
              </CardDescription>
            </div>
          </div>
          {allocation.status !== "NOT_STARTED" && <StatusBadge status={allocation.status} />}
        </div>
      </CardHeader>
      <CardContent className="space-y-4 pt-6">
        {/* Trigger */}
        {!allocation.isTriggered && (
          <Button
            onClick={handleTrigger}
            disabled={disabled || allocation.trigger.isPending}
            className="w-full gap-2"
          >
            {allocation.trigger.isPending ? (
              <>
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-primary-foreground/30 border-t-primary-foreground" />
                Starting…
              </>
            ) : (
              <>
                <Play className="h-4 w-4" />
                Run Allocation
              </>
            )}
          </Button>
        )}

        {/* Trigger error */}
        {allocation.trigger.isError && (
          <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <span>{allocation.trigger.error.message}</span>
          </div>
        )}

        {/* Polling state */}
        {allocation.isPolling && (
          <div className="flex items-center gap-2 rounded-md border bg-muted/50 p-3 text-sm text-muted-foreground">
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-muted-foreground" />
            Allocation in progress… Polling for updates.
          </div>
        )}

        {/* Polling error */}
        {allocation.pollingError && (
          <div className="flex items-start gap-2 rounded-md border border-warning/30 bg-warning/5 p-3 text-sm text-warning">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <span>Polling error: {allocation.pollingError.message}</span>
          </div>
        )}

        {/* Failed */}
        {allocation.status === "FAILED" && (
          <div className="flex items-start gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <span>Allocation failed. Please create a new session and try again.</span>
          </div>
        )}

        {/* Preview Panel */}
        <PreviewPanel 
          batchId={allocation.batchId ?? null} 
          status={allocation.status} 
          onUpdate={refreshIntegrity}
        />

        {/* Downloads */}
        {allocation.status === "ACTIVE" && allocation.batchId && (
          <div className="grid grid-cols-2 gap-2">
            <Button
              variant="outline"
              onClick={handleDownloadPdf}
              disabled={isDownloadingPdf}
              className="gap-2"
            >
              {isDownloadingPdf ? (
                <>
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" />
                  PDF…
                </>
              ) : (
                <>
                  <Download className="h-4 w-4" />
                  PDF
                </>
              )}
            </Button>

            <Button
              variant="outline"
              onClick={handleDownloadExcel}
              disabled={isDownloadingExcel}
              className="gap-2"
            >
              {isDownloadingExcel ? (
                <>
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" />
                  Excel…
                </>
              ) : (
                <>
                  <Download className="h-4 w-4" />
                  Excel
                </>
              )}
            </Button>
          </div>
        )}

        {allocation.status === "ACTIVE" && allocation.batchId && (
          <Button
            variant="secondary"
            onClick={handleDownloadSummary}
            disabled={isDownloadingSummary}
            className="w-full gap-2"
          >
            {isDownloadingSummary ? (
              <>
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-muted-foreground border-t-foreground" />
                Wait Summary…
              </>
            ) : (
              <>
                <Download className="h-4 w-4" />
                Download Account Opening Summary (Excel)
              </>
            )}
          </Button>
        )}

        {/* Integrity Certificate */}
        {allocation.status === "ACTIVE" && allocation.batchId && (
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
                      <div className="mt-3 p-3 bg-red-100/50 rounded-md border border-red-200">
                        <h4 className="text-xs font-bold text-red-800 mb-2 flex items-center gap-1.5">
                          <AlertCircle className="w-3.5 h-3.5" />
                          Recommendations for 100% Integrity
                        </h4>
                        <ul className="text-[10px] text-red-700 space-y-1.5 list-disc pl-4">
                          <li>Use the <strong>Preview Panel</strong> below to locate the flagged halls.</li>
                          <li>Click the <strong>Edit</strong> button on a hall to manually swap students.</li>
                          <li>Ensure no adjacent seats (horizontal or diagonal) share the same subject code.</li>
                          <li>Avoid placing students from the same department next to each other where possible.</li>
                        </ul>
                      </div>
                    </div>
                  </details>
                )}
              </div>
            )}
            <Button
              variant="outline"
              onClick={handleDownloadCertificate}
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

        {allocation.batchId && (
          <p className="text-xs text-muted-foreground">
            Batch ID: <span className="font-mono">{allocation.batchId.slice(0, 12)}…</span>
          </p>
        )}
      </CardContent>
    </Card>
  );
}
