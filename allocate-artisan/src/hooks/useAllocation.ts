import { useState, useCallback } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  triggerAllocation,
  getLatestBatch,
  downloadBatchPdf,
  downloadBatchExcel,
  downloadBatchSummaryExcel,
  type AllocationStatus,
} from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";
import { triggerFileDownload } from "@/lib/downloadHelper";

export function useAllocation(sessionId: string | null, selectedRooms: string[] = []) {
  const queryClient = useQueryClient();
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
      queryClient.invalidateQueries({ queryKey: ["batch-status", sessionId] });
      queryClient.refetchQueries({ queryKey: ["batch-status", sessionId] });
    },
  });

  const polling = useQuery({
    queryKey: ["batch-status", sessionId],
    queryFn: () => getLatestBatch(sessionId!),
    enabled: !!sessionId,
    refetchInterval: (query) => {
      if (query.state.error) return false;
      const currentStatus = query.state.data?.status;
      if (currentStatus === "ACTIVE" || currentStatus === "FAILED") return false;
      if (isTriggered || currentStatus === "RUNNING") return 2000;
      return false;
    },
    retry: 2,
    retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 10000),
    staleTime: 0,
  });

  const status: AllocationStatus = polling.data?.status ?? "NOT_STARTED";
  const resolvedBatchId = polling.data?.batchId ?? batchId;

  const downloadPdf = useCallback(async () => {
    if (!resolvedBatchId) return;
    const { blob, filename } = await downloadBatchPdf(resolvedBatchId);
    await triggerFileDownload(blob, filename);
  }, [resolvedBatchId]);

  const downloadExcel = useCallback(async () => {
    if (!resolvedBatchId) return;
    const { blob, filename } = await downloadBatchExcel(resolvedBatchId);
    await triggerFileDownload(blob, filename);
  }, [resolvedBatchId]);

  const downloadSummaryExcel = useCallback(async () => {
    if (!resolvedBatchId) return;
    const { blob, filename } = await downloadBatchSummaryExcel(resolvedBatchId);
    await triggerFileDownload(blob, filename);
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
