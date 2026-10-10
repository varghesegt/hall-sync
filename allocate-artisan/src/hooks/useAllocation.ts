import { useState, useCallback } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  triggerAllocation,
  getLatestBatch,
  downloadBatchPdf,
  downloadBatchExcel,
  downloadBatchSummaryExcel,
  type AllocationStatus,
} from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";

export function useAllocation(sessionId: string | null, selectedRooms: string[] = []) {
  const [isTriggered, setIsTriggered] = useState(false);
  const [batchId, setBatchId] = useState<string | null>(null);

  const trigger = useMutation<{ batchId: string }, ApiError, void>({
    mutationFn: () => {
      if (!sessionId) throw new Error("No session ID");
      return triggerAllocation({ sessionId, selectedRooms });
    },
    onSuccess: (data) => {
      setBatchId(data.batchId);
      setIsTriggered(true);
    },
  });

  const polling = useQuery({
    queryKey: ["batch-status", sessionId],
    queryFn: () => getLatestBatch(sessionId!),
    enabled: !!sessionId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === "RUNNING") return 3000;
      return false;
    },
    retry: 2,
    retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 10000),
    staleTime: 5000,
  });

  const status: AllocationStatus = polling.data?.status ?? "NOT_STARTED";
  const resolvedBatchId = polling.data?.batchId ?? batchId;

  const downloadPdf = useCallback(async () => {
    if (!resolvedBatchId) return;
    const { blob, filename } = await downloadBatchPdf(resolvedBatchId);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, [resolvedBatchId]);

  const downloadExcel = useCallback(async () => {
    if (!resolvedBatchId) return;
    const { blob, filename } = await downloadBatchExcel(resolvedBatchId);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, [resolvedBatchId]);

  const downloadSummaryExcel = useCallback(async () => {
    if (!resolvedBatchId) return;
    const { blob, filename } = await downloadBatchSummaryExcel(resolvedBatchId);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, [resolvedBatchId]);

  return {
    trigger,
    status,
    batchId: resolvedBatchId,
    isPolling: isTriggered && (status === "NOT_STARTED" || status === "RUNNING"),
    pollingError: polling.error ? (polling.error as unknown as ApiError) : null,
    downloadPdf,
    downloadExcel,
    downloadSummaryExcel,
    isTriggered: isTriggered || status === "ACTIVE",
  };
}
