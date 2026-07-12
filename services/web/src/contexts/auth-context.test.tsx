import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { SESSION_STORAGE_KEY } from "@/constants/auth";
import { AuthProvider, useAuth } from "@/contexts/auth-context";
import { getAccessToken } from "@/lib/token-store";

const replaceMock = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: replaceMock, push: vi.fn() }),
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

const loginStartMock = vi.fn();
const loginVerifyMock = vi.fn();
const refreshAccessTokenMock = vi.fn();
const logoutMock = vi.fn();
vi.mock("@/services/auth-service", () => ({
  loginStart: (...args: unknown[]) => loginStartMock(...args),
  loginVerify: (...args: unknown[]) => loginVerifyMock(...args),
  refreshAccessToken: (...args: unknown[]) => refreshAccessTokenMock(...args),
  logout: (...args: unknown[]) => logoutMock(...args),
}));

function base64UrlEncode(value: string): string {
  return Buffer.from(value, "utf-8")
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function makeAccessToken(overrides: { userId?: string; expSeconds?: number } = {}): string {
  const nowSeconds = Math.floor(Date.now() / 1000);
  const payload = {
    sub: overrides.userId ?? "user-1",
    mobile_number: "+919876543210",
    tenant_id: "tenant-1",
    roles: ["GROUP_MEMBER"],
    permissions: [],
    iat: nowSeconds,
    exp: overrides.expSeconds ?? nowSeconds + 900,
  };
  return `header.${base64UrlEncode(JSON.stringify(payload))}.signature`;
}

function tokenPair(overrides: { accessExpSeconds?: number; refreshExpSeconds?: number } = {}) {
  const nowMs = Date.now();
  const accessExp = overrides.accessExpSeconds ?? Math.floor(nowMs / 1000) + 900;
  const refreshExp = overrides.refreshExpSeconds ?? Math.floor(nowMs / 1000) + 2_592_000;
  return {
    userId: "user-1",
    accessToken: makeAccessToken({ expSeconds: accessExp }),
    accessTokenExpiresAt: new Date(accessExp * 1000).toISOString(),
    refreshToken: "refresh-token-value",
    refreshTokenExpiresAt: new Date(refreshExp * 1000).toISOString(),
    tokenType: "Bearer",
  };
}

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>;
}

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
});

afterEach(() => {
  vi.useRealTimers();
});

describe("AuthProvider login/logout", () => {
  it("stores the session and access token after login", async () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => {
      result.current.login(tokenPair());
    });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.session?.userId).toBe("user-1");
    expect(getAccessToken()).not.toBeNull();
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).not.toBeNull();
  });

  it("revokes the refresh token on the server and clears local state on logout", async () => {
    logoutMock.mockResolvedValueOnce(undefined);
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    act(() => {
      result.current.login(tokenPair());
    });
    act(() => {
      result.current.logout();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(getAccessToken()).toBeNull();
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
    expect(logoutMock).toHaveBeenCalledWith({ refreshToken: "refresh-token-value" });
  });
});

describe("AuthProvider hydration (browser refresh)", () => {
  it("restores the session from localStorage when the access token is still valid", async () => {
    const pair = tokenPair();
    localStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        accessToken: pair.accessToken,
        accessTokenExpiresAt: pair.accessTokenExpiresAt,
        refreshToken: pair.refreshToken,
        refreshTokenExpiresAt: pair.refreshTokenExpiresAt,
        userId: "user-1",
        mobileNumber: "+919876543210",
        roles: ["GROUP_MEMBER"],
      })
    );

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.isAuthenticated).toBe(true);
    expect(refreshAccessTokenMock).not.toHaveBeenCalled();
  });

  it("silently refreshes when the access token expired but the refresh token is still valid", async () => {
    const expiredAccess = makeAccessToken({ expSeconds: Math.floor(Date.now() / 1000) - 60 });
    localStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        accessToken: expiredAccess,
        accessTokenExpiresAt: new Date(Date.now() - 60_000).toISOString(),
        refreshToken: "stale-refresh-token",
        refreshTokenExpiresAt: new Date(Date.now() + 2_592_000_000).toISOString(),
        userId: "user-1",
        mobileNumber: "+919876543210",
        roles: ["GROUP_MEMBER"],
      })
    );
    refreshAccessTokenMock.mockResolvedValueOnce(tokenPair());

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(refreshAccessTokenMock).toHaveBeenCalledWith({ refreshToken: "stale-refresh-token" });
    expect(result.current.isAuthenticated).toBe(true);
  });

  it("clears the session when both the access and refresh tokens have expired", async () => {
    const expiredAccess = makeAccessToken({ expSeconds: Math.floor(Date.now() / 1000) - 60 });
    localStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        accessToken: expiredAccess,
        accessTokenExpiresAt: new Date(Date.now() - 60_000).toISOString(),
        refreshToken: "long-dead-refresh-token",
        refreshTokenExpiresAt: new Date(Date.now() - 1_000).toISOString(),
        userId: "user-1",
        mobileNumber: "+919876543210",
        roles: ["GROUP_MEMBER"],
      })
    );

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(refreshAccessTokenMock).not.toHaveBeenCalled();
    expect(result.current.isAuthenticated).toBe(false);
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it("clears the session when the silent refresh call itself fails", async () => {
    const expiredAccess = makeAccessToken({ expSeconds: Math.floor(Date.now() / 1000) - 60 });
    localStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        accessToken: expiredAccess,
        accessTokenExpiresAt: new Date(Date.now() - 60_000).toISOString(),
        refreshToken: "revoked-refresh-token",
        refreshTokenExpiresAt: new Date(Date.now() + 2_592_000_000).toISOString(),
        userId: "user-1",
        mobileNumber: "+919876543210",
        roles: ["GROUP_MEMBER"],
      })
    );
    refreshAccessTokenMock.mockRejectedValueOnce(new Error("refresh token revoked"));

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.isAuthenticated).toBe(false);
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });
});

describe("AuthProvider silent refresh scheduling", () => {
  it("refreshes the access token shortly before it expires and reschedules", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    // Expires in 90s — the 60s refresh margin means the timer should fire in ~30s.
    const nowSeconds = Math.floor(Date.now() / 1000);
    act(() => {
      result.current.login(tokenPair({ accessExpSeconds: nowSeconds + 90 }));
    });
    refreshAccessTokenMock.mockResolvedValueOnce(tokenPair({ accessExpSeconds: nowSeconds + 990 }));

    await act(async () => {
      await vi.advanceTimersByTimeAsync(31_000);
    });

    expect(refreshAccessTokenMock).toHaveBeenCalledWith({ refreshToken: "refresh-token-value" });
    expect(result.current.isAuthenticated).toBe(true);
  });

  it("logs out and redirects to /login when the scheduled refresh fails", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    const nowSeconds = Math.floor(Date.now() / 1000);
    act(() => {
      result.current.login(tokenPair({ accessExpSeconds: nowSeconds + 90 }));
    });
    refreshAccessTokenMock.mockRejectedValueOnce(new Error("refresh token revoked"));

    await act(async () => {
      await vi.advanceTimersByTimeAsync(31_000);
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(replaceMock).toHaveBeenCalledWith("/login");
  });
});
