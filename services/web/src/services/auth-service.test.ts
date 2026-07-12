import { describe, expect, it, vi } from "vitest";

import { apiClient } from "@/services/api-client";
import { loginStart, loginVerify, logout, refreshAccessToken } from "@/services/auth-service";

vi.mock("@/services/api-client", () => ({
  apiClient: { post: vi.fn() },
}));

const postMock = vi.mocked(apiClient.post);

describe("auth-service login/session endpoints", () => {
  it("posts to /api/v1/auth/login/start with the mobile number", async () => {
    postMock.mockResolvedValueOnce({
      data: { userId: "u1", mobileNumber: "+919876543210", otpExpiresAt: "2026-07-12T10:05:00Z" },
    });

    const result = await loginStart({ mobileNumber: "+919876543210" });

    expect(postMock).toHaveBeenCalledWith("/api/v1/auth/login/start", {
      mobileNumber: "+919876543210",
    });
    expect(result.userId).toBe("u1");
  });

  it("posts to /api/v1/auth/login/verify with the userId and code", async () => {
    postMock.mockResolvedValueOnce({
      data: {
        userId: "u1",
        accessToken: "access-token",
        accessTokenExpiresAt: "2026-07-12T10:15:00Z",
        refreshToken: "refresh-token",
        refreshTokenExpiresAt: "2026-08-11T10:00:00Z",
        tokenType: "Bearer",
      },
    });

    const result = await loginVerify({ userId: "u1", code: "482913" });

    expect(postMock).toHaveBeenCalledWith("/api/v1/auth/login/verify", {
      userId: "u1",
      code: "482913",
    });
    expect(result.accessToken).toBe("access-token");
  });

  it("posts to /api/v1/auth/token/refresh with the refresh token", async () => {
    postMock.mockResolvedValueOnce({
      data: {
        userId: "u1",
        accessToken: "new-access-token",
        accessTokenExpiresAt: "2026-07-12T11:15:00Z",
        refreshToken: "new-refresh-token",
        refreshTokenExpiresAt: "2026-08-11T11:00:00Z",
        tokenType: "Bearer",
      },
    });

    const result = await refreshAccessToken({ refreshToken: "old-refresh-token" });

    expect(postMock).toHaveBeenCalledWith("/api/v1/auth/token/refresh", {
      refreshToken: "old-refresh-token",
    });
    expect(result.accessToken).toBe("new-access-token");
  });

  it("posts to /api/v1/auth/logout with the refresh token", async () => {
    postMock.mockResolvedValueOnce({ data: undefined });

    await logout({ refreshToken: "refresh-token" });

    expect(postMock).toHaveBeenCalledWith("/api/v1/auth/logout", { refreshToken: "refresh-token" });
  });
});
