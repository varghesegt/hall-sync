import axios from "axios";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api/v1",
  timeout: 75000, // 75s allows Render Free Tier cold-start spin-up (~45-60s)
  withCredentials: true,
  headers: {
    Accept: "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  const tenant = localStorage.getItem("tenant") || "krce";
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (!config.headers["X-Tenant-ID"]) {
    config.headers["X-Tenant-ID"] = tenant;
  }
  return config;
});

export interface ApiError {
  status: number;
  message: string;
  details?: Record<string, string[]>;
}

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const apiError: ApiError = {
      status: 0,
      message: "An unexpected error occurred. Please try again.",
    };

    if (error.response) {
      const { status, data } = error.response;
      apiError.status = status;

      switch (status) {
        case 401:
          apiError.message = "Session expired. Please sign in again.";
          // Emit unauthorized event to trigger logout gracefully
          window.dispatchEvent(new Event("hallsync:unauthorized"));
          break;
        case 422:
          apiError.message = data?.message || "Validation failed. Please check your inputs.";
          if (Array.isArray(data?.details)) {
              const formatted: Record<string, string[]> = {};
              for (const err of data.details) {
                  if (typeof err === "string") {
                      if (!formatted["Global"]) formatted["Global"] = [];
                      formatted["Global"].push(err);
                  } else if (err && err.field && err.message) {
                      if (!formatted[err.field]) formatted[err.field] = [];
                      formatted[err.field].push(err.message);
                  }
              }
              apiError.details = formatted;
          } else {
              apiError.details = data?.errors || data?.details;
          }
          break;
        case 423:
          apiError.message = data?.message || "Allocation is already running. Please wait.";
          break;
        case 429:
          apiError.message = data?.message || "Too many requests. Please wait before retrying.";
          break;
        case 503:
          apiError.message = data?.message || "System is busy or warming up. Please retry shortly.";
          break;
        case 500:
          apiError.message = data?.message || "A server error occurred. Please contact support if this persists.";
          break;
        default:
          apiError.message = data?.message || `Request failed (${status}).`;
      }
    } else if (error.code === "ECONNABORTED" || error.message?.toLowerCase().includes("timeout")) {
      apiError.message = "The server is taking longer than usual to respond. It may be waking up from sleep mode (free tier cold start). Please retry in a few moments.";
    } else if (error.request) {
      // No response received -> Server might be down or network dropped
      apiError.message = "Network error. Backend server may be waking up or unreachable.";
      window.dispatchEvent(new Event("hallsync:network_offline"));
    }

    return Promise.reject(apiError);
  }
);

export default apiClient;
