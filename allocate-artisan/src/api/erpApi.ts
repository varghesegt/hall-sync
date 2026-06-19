import apiClient from "./axios";

export const erpApi = {
  exportToCamu: (batchId: string) =>
    apiClient.get(`/integrations/erp/export/camu/${batchId}`, { responseType: 'blob' }),

  exportToICloudEms: (batchId: string) =>
    apiClient.get(`/integrations/erp/export/icloudems/${batchId}`, { responseType: 'blob' }),

  importExcel: (file: File, sessionId: string) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("sessionId", sessionId);
    return apiClient.post("/integrations/import/excel", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  importCamu: (file: File, sessionId: string) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("sessionId", sessionId);
    return apiClient.post("/integrations/import/camu", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  importICloudEms: (file: File, sessionId: string) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("sessionId", sessionId);
    return apiClient.post("/integrations/import/icloudems", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  downloadTemplate: (type: string) =>
    apiClient.get(`/integrations/import/templates/${type}`, { responseType: "blob" }),
};
