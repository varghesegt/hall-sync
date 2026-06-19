import apiClient from "./axios";

export interface MalpracticeCaseDto {
  id: string;
  caseType: string;
  description: string;
  severity: string;
  status: string;
  actionTaken?: string;
  reportedAt: string;
  resolvedAt?: string;
  hasEvidence: boolean;
  evidenceFilename?: string;
  studentName?: string;
  studentRegNo?: string;
  studentDepartment?: string;
  hallName?: string;
  reportedByName?: string;
}

export const malpracticeApi = {
  getAll: () => apiClient.get<MalpracticeCaseDto[]>("/malpractice"),

  getByBatch: (batchId: string) =>
    apiClient.get<MalpracticeCaseDto[]>(`/malpractice/batch/${batchId}`),

  getById: (id: string) =>
    apiClient.get<MalpracticeCaseDto>(`/malpractice/${id}`),

  create: (data: Record<string, string>) =>
    apiClient.post<MalpracticeCaseDto>("/malpractice", data),

  updateStatus: (id: string, status: string, actionTaken?: string) =>
    apiClient.put<MalpracticeCaseDto>(`/malpractice/${id}/status`, {
      status,
      actionTaken,
    }),

  uploadEvidence: (id: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient.post(`/malpractice/${id}/evidence`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  downloadEvidence: (id: string) =>
    apiClient.get(`/malpractice/${id}/evidence`, { responseType: "blob" }),

  getTrends: (from?: string, to?: string) =>
    apiClient.get("/malpractice/trends", { params: { from, to } }),
};
