import apiClient from "./axios";

export const accreditationApi = {
  getNaacReport: (from?: string, to?: string) =>
    apiClient.get("/reports/naac", {
      params: { from, to },
      responseType: "blob",
    }),

  getNbaReport: (from?: string, to?: string) =>
    apiClient.get("/reports/nba", {
      params: { from, to },
      responseType: "blob",
    }),

  getAuditReport: (from?: string, to?: string) =>
    apiClient.get("/reports/audit", {
      params: { from, to },
      responseType: "blob",
    }),

  exportAuditorReport: (batchId: string) =>
    apiClient.get(`/accreditation/auditor-report/${batchId}`, {
      responseType: "blob",
    }),

  downloadEvidencePack: (archiveId: string) =>
    apiClient.get(`/reports/evidence-pack/${archiveId}`, {
      responseType: "blob",
    }),
};
