import { useState, useEffect, useCallback } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useAllocation } from "@/hooks/useAllocation";
import { StatusBadge } from "@/components/StatusBadge";
import { PreviewPanel } from "@/components/PreviewPanel";
import { IntegrityAuditBanner } from "@/components/IntegrityAuditBanner";
import { AlertCircle, Download, Play, ClipboardList, ShieldCheck } from "lucide-react";
import { getIntegritySummary, downloadIntegrityCertificate, type IntegritySummary } from "@/api/allocationApi";
import apiClient from "@/api/axios";
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
  const [isDownloadingSchedule, setIsDownloadingSchedule] = useState(false);
  const [isDownloadingCert, setIsDownloadingCert] = useState(false);
  const [integrity, setIntegrity] = useState<IntegritySummary | null>(null);

  const refreshIntegrity = useCallback(() => {
    if (allocation.status === "ACTIVE" && allocation.batchId) {
      getIntegritySummary(allocation.batchId)
        .then(setIntegrity)
        .catch(() => setIntegrity(null));
    }
  }, [allocation.status, allocation.batchId]);

  useEffect(() => {
    refreshIntegrity();
  }, [refreshIntegrity]);

  const disabled = !sessionId || selectedRooms.length === 0;

  const handleTrigger = () => {
    if (!sessionId || allocation.trigger.isPending || allocation.isTriggered || allocation.status === "ACTIVE") return;
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

  const handleDownloadSchedule = async () => {
    if (!allocation.batchId) return;
    setIsDownloadingSchedule(true);
    try {
      const response = await apiClient.get(`/duties/batch/${allocation.batchId}/duty-schedule-excel`, { responseType: "blob", timeout: 120000 });
      const contentDisposition = response.headers["content-disposition"];
      let filename = `Invigilation_Duty_Schedule.xlsx`;
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

  const hasActiveBatch = allocation.status === "ACTIVE" && !!allocation.batchId;

  return (
    <Card className={cn("transition-all duration-500 overflow-hidden glass-panel", disabled && !hasActiveBatch && "opacity-50 grayscale-[0.2]")}>
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
                  : selectedRooms.length === 0 && !hasActiveBatch
                  ? "Awaiting physical environment selection..."
                  : `${selectedRooms.length || 'Ready'} rooms queued for processing`}
              </CardDescription>
            </div>
          </div>
          {allocation.status !== "NOT_STARTED" && <StatusBadge status={allocation.status} />}
        </div>
      </CardHeader>
      <CardContent className="space-y-4 pt-6">
        {/* Trigger */}
        {(!allocation.isTriggered && allocation.status !== "ACTIVE") && (
          <Button
            onClick={handleTrigger}
            disabled={disabled || allocation.trigger.isPending}
            className="w-full gap-2 font-semibold"
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
                onClick={handleDownloadPdf}
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
                onClick={handleDownloadExcel}
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
              onClick={handleDownloadSummary}
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
              onClick={handleDownloadSchedule}
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
          batchId={allocation.batchId ?? null} 
          status={allocation.status} 
          onUpdate={refreshIntegrity}
        />
      </CardContent>
    </Card>
  );
}
