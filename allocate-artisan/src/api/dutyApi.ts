import apiClient from "./axios";

export interface DutyDto {
  id: string;
  facultyName: string;
  facultyId: string;
  employeeId: string;
  facultyDepartment: string;
  hallId: string;
  hallName: string;
  dutyType: string;
  shift: string;
  dutyDate: string;
  isPresent: boolean | null;
}

export const dutyApi = {
  allocate: (batchId: string) =>
    apiClient.post(`/duties/allocate/${batchId}`),

  getByBatch: (batchId: string) =>
    apiClient.get<DutyDto[]>(`/duties/batch/${batchId}`),

  markAttendance: (dutyId: string, present: boolean) =>
    apiClient.put(`/duties/${dutyId}/attendance`, { present }),

  downloadExcel: (batchId: string) =>
    apiClient.get(`/duties/batch/${batchId}/excel`, { responseType: "blob" }),
};
