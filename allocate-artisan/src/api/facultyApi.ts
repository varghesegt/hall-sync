import apiClient from "./axios";

export interface FacultyDto {
  id: string;
  name: string;
  employeeId: string;
  department: string;
  designation?: string;
  phone?: string;
  email?: string;
  isActive: boolean;
  collegeName?: string;
  isInternal?: boolean;
  isAvailable?: boolean;
}

export const facultyApi = {
  getAll: () => apiClient.get<FacultyDto[]>("/faculty"),

  getById: (id: string) => apiClient.get<FacultyDto>(`/faculty/${id}`),

  create: (data: Partial<FacultyDto>) => apiClient.post<FacultyDto>("/faculty", data),

  update: (id: string, data: Partial<FacultyDto>) =>
    apiClient.put<FacultyDto>(`/faculty/${id}`, data),

  delete: (id: string) => apiClient.delete(`/faculty/${id}`),

  bulkUpload: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiClient.post("/faculty/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  downloadTemplate: () =>
    apiClient.get("/faculty/template", { responseType: "blob" }),
};
