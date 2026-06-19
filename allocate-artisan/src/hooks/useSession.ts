import { useMutation } from "@tanstack/react-query";
import { createSession, type SessionRequest, type SessionResponse } from "@/api/allocationApi";
import type { ApiError } from "@/api/axios";

export function useSession() {
  return useMutation<SessionResponse, ApiError, SessionRequest>({
    mutationFn: createSession,
  });
}
