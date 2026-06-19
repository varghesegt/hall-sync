import apiClient from './axios';

export interface ScriptDetail {
  sessionType: string;
  serialNumber: number;
  subjectCode: string;
  noOfScripts: number;
}

export interface ClaimRecord {
  id: number;
  batchId: string;
  serialNumber: number;
  examMonth: string;
  examSeason: string;
  valuationDate: string;
  governmentHoliday: boolean;
  sessionsAttended: string;
  boardName: string;
  postHeld: string;
  mobileNo: string;
  nameTitle: string;
  staffName: string;
  facultyType: string;
  designation: string;
  institutionName: string;
  institutionCode: string;
  issueRegPageNo: string;
  bankAccountNumber: string;
  ifscCode: string;
  bankName: string;
  branch: string;
  distanceKm: number;
  totalScripts: number;
  fnScripts: number;
  anScripts: number;
  scriptAmount: number;
  travellingAllowance: number;
  dearnessAllowance: number;
  totalAmount: number;
  amountInWords: string;
  maxScriptsValued: number;
  maxScriptsAmount: number;
  tenPercentAmount: number;
  overallScriptAmount: number;
  scriptDetails: ScriptDetail[];
  createdAt: string;
}

export interface UploadResponse {
  batchId: string;
  examSeason: string;
  totalRecords: number;
  examiners: number;
  assistantExaminers: number;
  chiefExaminers: number;
  totalAmount: number;
  warnings: string[];
  message: string;
}

export interface BatchSummary {
  batchId: string;
  totalRecords: number;
  totalAmount: number;
  examSeason: string;
  valuationDate: string;
  createdAt: string;
  examiners: number;
  assistantExaminers: number;
  chiefExaminers: number;
}

export interface CollegeDistance {
  id: number;
  institutionCode?: string | null;
  institutionName: string;
  place?: string | null;
  distanceKm: number;
}

// ==================== API FUNCTIONS ====================

export const claimsApi = {
  uploadExcel: async (
    file: File,
    examSeason?: string,
    valuationDate?: string,
    governmentHoliday?: boolean
  ): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    if (examSeason) formData.append('examSeason', examSeason);
    if (valuationDate) formData.append('valuationDate', valuationDate);
    if (governmentHoliday !== undefined) formData.append('governmentHoliday', governmentHoliday.toString());

    const { data } = await apiClient.post<UploadResponse>('/claims/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return data;
  },

  getBatchClaims: async (batchId: string): Promise<ClaimRecord[]> => {
    const { data } = await apiClient.get<ClaimRecord[]>(`/claims/batch/${batchId}`);
    return data;
  },

  getClaim: async (id: number): Promise<ClaimRecord> => {
    const { data } = await apiClient.get<ClaimRecord>(`/claims/${id}`);
    return data;
  },

  getAllBatches: async (): Promise<BatchSummary[]> => {
    const { data } = await apiClient.get<BatchSummary[]>('/claims/batches');
    return data;
  },

  getDownloadWordUrl: (batchId: string): string => {
    const token = localStorage.getItem('coe_auth') || localStorage.getItem('token');
    return `${apiClient.defaults.baseURL}/claims/batch/${batchId}/download/word${token ? `?token=${token}` : ''}`;
  },

  getDownloadExcel1Url: (batchId: string): string => {
    const token = localStorage.getItem('coe_auth') || localStorage.getItem('token');
    return `${apiClient.defaults.baseURL}/claims/batch/${batchId}/download/excel1${token ? `?token=${token}` : ''}`;
  },

  getDownloadExcel2Url: (batchId: string): string => {
    const token = localStorage.getItem('coe_auth') || localStorage.getItem('token');
    return `${apiClient.defaults.baseURL}/claims/batch/${batchId}/download/excel2${token ? `?token=${token}` : ''}`;
  },

  getSingleWordUrl: (id: number): string => {
    const token = localStorage.getItem('coe_auth') || localStorage.getItem('token');
    return `${apiClient.defaults.baseURL}/claims/${id}/download/word${token ? `?token=${token}` : ''}`;
  },

  deleteBatch: async (batchId: string): Promise<void> => {
    await apiClient.delete(`/claims/batch/${batchId}`);
  },

  getColleges: async (): Promise<CollegeDistance[]> => {
    const { data } = await apiClient.get<CollegeDistance[]>('/colleges');
    return data;
  },

  searchColleges: async (name: string): Promise<CollegeDistance[]> => {
    const { data } = await apiClient.get<CollegeDistance[]>(`/colleges/search?name=${encodeURIComponent(name)}`);
    return data;
  },

  addCollege: async (college: Omit<CollegeDistance, 'id'>): Promise<CollegeDistance> => {
    const { data } = await apiClient.post<CollegeDistance>('/colleges', college);
    return data;
  },

  updateCollege: async (id: number, college: Partial<CollegeDistance>): Promise<CollegeDistance> => {
    const { data } = await apiClient.put<CollegeDistance>(`/colleges/${id}`, college);
    return data;
  },

  deleteCollege: async (id: number): Promise<void> => {
    await apiClient.delete(`/colleges/${id}`);
  }
};
