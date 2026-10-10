import apiClient from "./axios";
import type {
  UploadResponse,
  StudentPreviewResponse,
  SessionRequest,
  SessionResponse,
  BatchResponse,
  PreviewSeat,
  IntegritySummary,
} from "./allocationApi";

// ==================== INTERNAL EXAM API ====================
// All endpoints under /api/v1/internal/*

const INTERNAL_BASE = "/internal";

export const uploadInternalExcel = async (file: File): Promise<UploadResponse> => {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient.post<UploadResponse>(`${INTERNAL_BASE}/uploads/excel`, formData, {
    timeout: 30000,
  });
  return data;
};

export const getInternalStudentPreview = async (fileId: string): Promise<StudentPreviewResponse> => {
  const { data } = await apiClient.get<StudentPreviewResponse>(`${INTERNAL_BASE}/uploads/${fileId}/preview`);
  return data;
};

export const createInternalSession = async (payload: SessionRequest): Promise<SessionResponse> => {
  const { data } = await apiClient.post<SessionResponse>(`${INTERNAL_BASE}/exam-sessions`, payload);
  return data;
};

export const triggerInternalAllocation = async (params: { sessionId: string; selectedRooms: string[] }): Promise<{ batchId: string }> => {
  const { data } = await apiClient.post<{ batchId: string }>(
    `${INTERNAL_BASE}/exam-sessions/${params.sessionId}/allocations`,
    { requestedBy: "system-admin", selectedRooms: params.selectedRooms }
  );
  return data;
};

export const getInternalLatestBatch = async (sessionId: string): Promise<BatchResponse> => {
  const { data } = await apiClient.get<BatchResponse>(
    `${INTERNAL_BASE}/exam-sessions/${sessionId}/batches/latest`
  );
  return data;
};

export const downloadInternalBatchPdf = async (batchId: string): Promise<{blob: Blob, filename: string}> => {
  const response = await apiClient.get<Blob>(`${INTERNAL_BASE}/allocation-batches/${batchId}/pdf`, {
    responseType: "blob",
    headers: { Accept: "application/pdf" },
    timeout: 120000,
  });
  let filename = `internal-allocation-${batchId}.pdf`;
  const contentDisposition = response.headers["content-disposition"];
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) filename = match[1];
  }
  return { blob: response.data, filename };
};

export const downloadInternalBatchExcel = async (batchId: string): Promise<{blob: Blob, filename: string}> => {
  const response = await apiClient.get<Blob>(`${INTERNAL_BASE}/allocation-batches/${batchId}/excel`, {
    responseType: "blob",
    headers: { Accept: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
    timeout: 120000,
  });
  let filename = `internal-allocation-${batchId}.xlsx`;
  const contentDisposition = response.headers["content-disposition"];
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) filename = match[1];
  }
  return { blob: response.data, filename };
};

export const downloadInternalBatchSummaryExcel = async (batchId: string): Promise<{blob: Blob, filename: string}> => {
  const response = await apiClient.get<Blob>(`${INTERNAL_BASE}/allocation-batches/${batchId}/summary-excel`, {
    responseType: "blob",
    headers: { Accept: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
    timeout: 120000,
  });
  let filename = `internal-summary-${batchId}.xlsx`;
  const contentDisposition = response.headers["content-disposition"];
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) filename = match[1];
  }
  return { blob: response.data, filename };
};

export const getInternalBatchPreview = async (batchId: string): Promise<PreviewSeat[]> => {
  const { data } = await apiClient.get<PreviewSeat[]>(`${INTERNAL_BASE}/allocation-batches/${batchId}/preview`);
  return data;
};
