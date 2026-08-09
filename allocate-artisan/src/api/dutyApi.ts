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

export interface FacultyDto {
  id: string;
  name: string;
  employeeId: string;
  department: string;
  designation?: string;
  isActive: boolean;
  isAvailable: boolean;
}

export const dutyApi = {
  allocate: (batchId: string, facultyIds?: string[]) =>
    apiClient.post(`/duties/allocate/${batchId}`, { facultyIds }),

  clearAllocation: (batchId: string) =>
    apiClient.delete(`/duties/batch/${batchId}`),

  getAllFaculty: () =>
    apiClient.get<FacultyDto[]>("/faculty"),

  getByBatch: (batchId: string) =>
    apiClient.get<DutyDto[]>(`/duties/batch/${batchId}`),

  markAttendance: (dutyId: string, present: boolean) =>
    apiClient.put(`/duties/${dutyId}/attendance`, { present }),

  downloadExcel: (batchId: string) =>
    apiClient.get(`/duties/batch/${batchId}/excel`, { responseType: "blob" }),

  downloadWord: (batchId: string) =>
    apiClient.get(`/duties/batch/${batchId}/word`, { responseType: "blob" }),

  swapDuty: (dutyId: string, newFacultyId: string) =>
    apiClient.put(`/duties/${dutyId}/swap/${newFacultyId}`),

  downloadDutySchedule: (batchId: string) =>
    apiClient.get(`/duties/batch/${batchId}/duty-schedule-excel`, { responseType: "blob" }),
};
