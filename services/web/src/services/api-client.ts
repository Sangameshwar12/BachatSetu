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
 * There is no refresh-and-retry logic here: `AuthProvider` already refreshes the access token
 * proactively, shortly before it expires, so a request should rarely hit this client with an
 * already-expired token. A 401 that does slip through (clock skew, out-of-band revocation) is a
 * fallback signal only — it clears the session and hands off to `AuthProvider`'s unauthorized
 * handler, which redirects to /login, rather than attempting an inline refresh-and-retry.
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
