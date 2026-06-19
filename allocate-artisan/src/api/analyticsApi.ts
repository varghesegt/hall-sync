import apiClient from "./axios";

export const analyticsApi = {
  getOverview: (from?: string, to?: string) =>
    apiClient.get("/analytics/overview", { params: { from, to } }),

  getWorkload: (from?: string, to?: string) =>
    apiClient.get("/analytics/workload", { params: { from, to } }),

  getDepartmentDistribution: (from?: string, to?: string) =>
    apiClient.get("/analytics/department-distribution", { params: { from, to } }),

  getFairnessIndex: (from?: string, to?: string) =>
    apiClient.get("/analytics/fairness-index", { params: { from, to } }),

  getMalpracticeTrends: (from?: string, to?: string) =>
    apiClient.get("/analytics/malpractice-trends", { params: { from, to } }),

  getExamHistory: (from?: string, to?: string) =>
    apiClient.get("/analytics/exam-history", { params: { from, to } }),

  getCommandCenter: (date?: string) =>
    apiClient.get("/analytics/command-center", { params: { date } }),

  exportRemuneration: (from?: string, to?: string) =>
    apiClient.get("/analytics/remuneration/export", { params: { from, to }, responseType: "blob" }),

  getManagementDashboard: () =>
    apiClient.get("/analytics/management-dashboard"),

  getLifecycle: (batchId: string) =>
    apiClient.get(`/analytics/lifecycle/${batchId}`),
};
