import apiClient from './axios';

export interface LabClaimRecord {
  id?: number;
  batchId?: string;
  examSeason?: string;
  examDate?: string;
  session?: string;
  department?: string;
  semester?: string;
  subjectCode?: string;
  subjectName?: string;
  batchNumber?: string;
  registeredCount?: number;
  presentCount?: number;
  staffName: string;
  nameTitle?: string;
  staffRole: string; // INTERNAL_EXAMINER, EXTERNAL_EXAMINER, SKILLED_ASSISTANT, LAB_ATTENDER
  designation?: string;
  institutionName?: string;
  institutionCode?: string;
  mobileNo?: string;
  bankAccountNumber?: string;
  ifscCode?: string;
  bankName?: string;
  branch?: string;
  distanceKm?: number;
  remunerationAmount?: number;
  taAmount?: number;
  daAmount?: number;
  totalAmount?: number;
  status?: string;
  createdAt?: string;
}

export interface LabUploadResponse {
  batchId?: string;
  count?: number;
  message?: string;
  warnings?: string[];
  records?: LabClaimRecord[];
  error?: string;
}

const API_BASE = '/lab-claims';

export const labClaimsApi = {
  uploadExcel: async (file: File, examSeason?: string, examDate?: string, subjectCode?: string): Promise<LabUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    if (examSeason) formData.append('examSeason', examSeason);
    if (examDate) formData.append('examDate', examDate);
    if (subjectCode) formData.append('subjectCode', subjectCode);

    const res = await apiClient.post(`${API_BASE}/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    return res.data;
  },

  getClaims: async (role?: string): Promise<LabClaimRecord[]> => {
    const params = role ? { role } : {};
    const res = await apiClient.get(API_BASE, { params });
    return res.data;
  },

  createClaim: async (claim: LabClaimRecord): Promise<LabClaimRecord> => {
    const res = await apiClient.post(API_BASE, claim);
    return res.data;
  },

  deleteClaim: async (id: number): Promise<void> => {
    await apiClient.delete(`${API_BASE}/${id}`);
  },

  deleteAllClaims: async (): Promise<{ message: string }> => {
    const res = await apiClient.delete(`${API_BASE}/all`);
    return res.data;
  },

  downloadPdf: async (id: number): Promise<Blob> => {
    const res = await apiClient.get(`${API_BASE}/${id}/pdf`, {
      responseType: 'blob'
    });
    return res.data;
  },

  downloadWord: async (id: number): Promise<Blob> => {
    const res = await apiClient.get(`${API_BASE}/${id}/word`, {
      responseType: 'blob'
    });
    return res.data;
  },

  downloadSessionWord: async (examDate: string, subjectCode: string): Promise<Blob> => {
    const res = await apiClient.get(`${API_BASE}/download-session-word`, {
      params: { examDate, subjectCode },
      responseType: 'blob'
    });
    return res.data;
  },

  downloadSampleTemplate: async (): Promise<Blob> => {
    const res = await apiClient.get(`${API_BASE}/sample-template`, {
      responseType: 'blob'
    });
    return res.data;
  }
};
