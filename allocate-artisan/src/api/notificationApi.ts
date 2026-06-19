import apiClient from "./axios";

export const notificationApi = {
  sendDutyEmails: (batchId: string) =>
    apiClient.post(`/notifications/duty-emails/${batchId}`),

  sendExamBroadcast: (batchId: string, message: string) =>
    apiClient.post(`/notifications/exam-broadcast/${batchId}`, { message }),

  getLog: () =>
    apiClient.get("/notifications/log"),

  getStats: (batchId: string) =>
    apiClient.get(`/notifications/stats/${batchId}`),
};
