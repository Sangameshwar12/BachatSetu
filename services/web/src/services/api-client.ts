import axios, { AxiosError } from "axios";

import { env } from "@/lib/env";
import { getAccessToken, notifyUnauthorized } from "@/lib/token-store";
import type { ApiProblemDetail } from "@/types/api";

/**
 * Shared HTTP client for every feature's `services/*.ts` module.
 *
 * Base URL, JSON defaults, a request interceptor that attaches the current access token (if
 * any — most endpoints in this codebase are either pre-auth like signup/OTP, or authenticated
 * like onboarding/storage), and a response interceptor that normalizes the backend's RFC 7807
 * problem-detail error body into a typed `ApiError` and reacts to 401s.
 *
 * There is no token-refresh retry here: the backend does not yet expose a refresh-token
 * endpoint (see Sprint FE-2 report, "Pending Backend APIs"). A 401 therefore clears the
 * session and hands off to `AuthProvider`'s unauthorized handler, which redirects to /login.
 */
export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 15_000,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly violations: { field: string; message: string }[];

  constructor(problem: ApiProblemDetail) {
    super(problem.detail || problem.title);
    this.name = "ApiError";
    this.status = problem.status;
    this.code = problem.code;
    this.violations = problem.violations ?? [];
  }
}

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiProblemDetail>) => {
    if (error.response?.status === 401) {
      notifyUnauthorized();
    }
    if (error.response?.data && typeof error.response.data === "object") {
      return Promise.reject(new ApiError(error.response.data));
    }
    return Promise.reject(error);
  }
);
