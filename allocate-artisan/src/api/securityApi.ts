import apiClient from "./axios";

export interface QuestionPaperTracker {
  id: string;
  examSession: any;
  subjectCode: string;
  totalReceived: number;
  storedLocation: string;
  distributedTime: string | null;
  returnedTime: string | null;
  status: string;
  remarks: string | null;
  createdAt: string;
  updatedAt: string;
}

export const securityApi = {
  getTrackersForSession: (sessionId: string) =>
    apiClient.get<QuestionPaperTracker[]>(`/security/qp/session/${sessionId}`),

  receiveQP: (data: Partial<QuestionPaperTracker>) =>
    apiClient.post<QuestionPaperTracker>("/security/qp/receive", data),

  updateStatus: (id: string, status: string, remarks?: string) =>
    apiClient.put<QuestionPaperTracker>(`/security/qp/${id}/status`, null, {
      params: { status, remarks },
    }),
};
