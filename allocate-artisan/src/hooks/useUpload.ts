import { useMutation } from "@tanstack/react-query";
import { uploadPdf, type UploadResponse } from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";

export function useUpload() {
  return useMutation<UploadResponse, ApiError, File>({
    mutationFn: uploadPdf,
  });
}
