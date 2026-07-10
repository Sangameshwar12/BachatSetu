/** Centralizes access to build-time environment configuration so it's never read ad hoc. */
export const env = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
} as const;
