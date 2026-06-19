import { useQuery } from "@tanstack/react-query";
import { getStudentPreview, type StudentPreviewResponse } from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";

export function useStudentPreview(fileId: string | null) {
  return useQuery<StudentPreviewResponse, ApiError>({
    queryKey: ["student-preview", fileId],
    queryFn: () => getStudentPreview(fileId!),
    enabled: !!fileId,
    staleTime: Infinity,
    retry: 2,
  });
}
