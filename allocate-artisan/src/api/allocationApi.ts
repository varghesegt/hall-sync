import apiClient from "./axios";

export interface UploadResponse {
  fileId: string;
  fileName: string;
  totalStudents?: number;
}

export interface StudentPreview {
  registerNumber: string;
  name: string;
  department: string;
  className: string;
  subjectName: string;
  subjectCode: string;
}

export interface StudentPreviewResponse {
  fileId: string;
  totalStudents: number;
  students: StudentPreview[];
}

export interface SessionRequest {
  fileId: string;
  examName: string;
  examDate: string;
  seasonId: string;
  session: string;
}

export interface SessionResponse {
  examSessionId: string;
  totalStudents: number;
  status: string;
}

export type AllocationStatus = "NOT_STARTED" | "RUNNING" | "ACTIVE" | "FAILED";

export interface BatchResponse {
  batchId: string;
  status: AllocationStatus;
  message?: string;
  createdAt?: string;
}

export const uploadPdf = async (file: File): Promise<UploadResponse> => {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient.post<UploadResponse>("/uploads/pdf", formData, {
    timeout: 30000,
  });
  return data;
};

export const getStudentPreview = async (fileId: string): Promise<StudentPreviewResponse> => {
  const { data } = await apiClient.get<StudentPreviewResponse>(`/uploads/${fileId}/preview`);
  return data;
};

export const createSession = async (payload: SessionRequest): Promise<SessionResponse> => {
  const { data } = await apiClient.post<SessionResponse>("/exam-sessions", payload);
  return data;
};

export const triggerAllocation = async (params: { sessionId: string; selectedRooms: string[] }): Promise<{ batchId: string }> => {
  const { data } = await apiClient.post<{ batchId: string }>(
    `/exam-sessions/${params.sessionId}/allocations`,
    { requestedBy: "system-admin", selectedRooms: params.selectedRooms }
  );
  return data;
};

export const getLatestBatch = async (sessionId: string): Promise<BatchResponse> => {
  const { data } = await apiClient.get<BatchResponse>(
    `/exam-sessions/${sessionId}/batches/latest`
  );
  return data;
};

export const getBatches = async (): Promise<unknown[]> => {
  const { data } = await apiClient.get<unknown[]>("/batches");
  return data;
};

export const getSessions = async (): Promise<unknown[]> => {
  try {
    const { data } = await apiClient.get<unknown[]>("/batches");
    const sessions = data
      .filter((b: any) => b.session)
      .map((b: any) => b.session);
    
    // De-duplicate sessions by ID
    const uniqueSessions = Array.from(new Map(sessions.map(s => [s.id, s])).values());
    return uniqueSessions;
  } catch {
    return [];
  }
};

export const downloadBatchPdf = async (batchId: string): Promise<{blob: Blob, filename: string}> => {
  const response = await apiClient.get<Blob>(`/allocation-batches/${batchId}/pdf`, {
    responseType: "blob",
    headers: {
      Accept: "application/pdf",
    },
    timeout: 120000,
  });
  
  let filename = `allocation-${batchId}.pdf`;
  const contentDisposition = response.headers["content-disposition"];
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) filename = match[1];
  }
  
  return { blob: response.data, filename };
};

export const downloadBatchExcel = async (batchId: string): Promise<{blob: Blob, filename: string}> => {
  const response = await apiClient.get<Blob>(`/allocation-batches/${batchId}/excel`, {
    responseType: "blob",
    headers: {
      Accept: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    },
    timeout: 120000,
  });
  
  let filename = `allocation-${batchId}.xlsx`;
  const contentDisposition = response.headers["content-disposition"];
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) filename = match[1];
  }
  
  return { blob: response.data, filename };
};

export const downloadBatchSummaryExcel = async (batchId: string): Promise<{blob: Blob, filename: string}> => {
  const response = await apiClient.get<Blob>(`/allocation-batches/${batchId}/summary-excel`, {
    responseType: "blob",
    headers: {
      Accept: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    },
    timeout: 30000,
  });
  
  let filename = `summary-${batchId}.xlsx`;
  const contentDisposition = response.headers["content-disposition"];
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) filename = match[1];
  }
  
  return { blob: response.data, filename };
};

export interface PreviewSeat {
  hallId: string;
  hallName: string;
  seatRow: number;
  seatCol: string;
  registerNumber: string;
  studentName: string;
  department: string;
  subjectCode: string;
  semester: string;
  regulation: string;
  riskScore: number;
}

export const getBatchPreview = async (batchId: string): Promise<PreviewSeat[]> => {
  const { data } = await apiClient.get<PreviewSeat[]>(`/allocation-batches/${batchId}/preview`);
  return data;
};

// ==================== VISUAL OVERRIDE API (NEW) ====================

export interface SwapSeatsRequest {
  hallIdA: string;
  seatRowA: number;
  seatColA: string;
  hallIdB: string;
  seatRowB: number;
  seatColB: string;
}

export const swapSeats = async (batchId: string, request: SwapSeatsRequest): Promise<PreviewSeat[]> => {
  const { data } = await apiClient.put<PreviewSeat[]>(`/allocation-batches/${batchId}/swap`, request);
  return data;
};

export interface UpdateRegisterRequest {
  hallId: string;
  seatRow: number;
  seatCol: string;
  newRegisterNumber: string;
}

export const updateRegisterNumber = async (batchId: string, request: UpdateRegisterRequest): Promise<PreviewSeat[]> => {
  const { data } = await apiClient.put<PreviewSeat[]>(`/allocation-batches/${batchId}/update-register`, request);
  return data;
};

// ==================== INTEGRITY CERTIFICATE API (NEW) ====================

export interface HallViolation {
  hallName: string;
  violations: number;
  studentCount: number;
  details: string[];
}

export interface IntegritySummary {
  certified: boolean;
  integrityScore: string;
  deptPurityScore: string;
  totalStudents: number;
  totalHalls: number;
  totalAdjacencyChecks: number;
  totalViolations: number;
  hallViolations?: HallViolation[];
}

export const getIntegritySummary = async (batchId: string): Promise<IntegritySummary> => {
  const { data } = await apiClient.get<IntegritySummary>(`/allocation-batches/${batchId}/integrity`);
  return data;
};

export const downloadIntegrityCertificate = async (batchId: string): Promise<{blob: Blob, filename: string}> => {
  const response = await apiClient.get<Blob>(`/allocation-batches/${batchId}/integrity-certificate`, {
    responseType: "blob",
    headers: { Accept: "application/pdf" },
    timeout: 30000,
  });

  let filename = `integrity-certificate-${batchId}.pdf`;
  const contentDisposition = response.headers["content-disposition"];
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^"]+)"?/);
    if (match && match[1]) filename = match[1];
  }

  return { blob: response.data, filename };
};
