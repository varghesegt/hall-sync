import apiClient from "./axios";

export interface ExamArchiveDto {
  id: string;
  examType: string;
  examDate: string;
  session: string;
  totalStudents: number;
  totalHalls: number;
  totalInvigilators: number;
  totalAbsentees: number;
  totalMalpractice: number;
  archivedAt: string;
  archivedBy?: string;
  hasSeatingPlan: boolean;
  hasAttendance: boolean;
  hasDutySheet: boolean;
}

export const archiveApi = {
  archiveBatch: (batchId: string, archivedBy: string) =>
    apiClient.post<ExamArchiveDto>(`/archives/batch/${batchId}`, { archivedBy }),

  getAll: (from?: string, to?: string) =>
    apiClient.get<ExamArchiveDto[]>("/archives", { params: { from, to } }),

  getById: (id: string) => apiClient.get<ExamArchiveDto>(`/archives/${id}`),

  downloadSnapshot: (id: string, type: "seating" | "attendance" | "duty") =>
    apiClient.get(`/archives/${id}/download/${type}`, { responseType: "blob" }),
};
