import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { LoginForm } from "@/features/auth/login-form";
import { ApiError } from "@/services/api-client";

const pushMock = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock, replace: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

const loginMock = vi.fn();
vi.mock("@/contexts/auth-context", () => ({
  useAuth: () => ({ login: loginMock }),
}));

const loginStartMock = vi.fn();
const loginVerifyMock = vi.fn();
vi.mock("@/services/auth-service", () => ({
  loginStart: (...args: unknown[]) => loginStartMock(...args),
  loginVerify: (...args: unknown[]) => loginVerifyMock(...args),
}));

vi.mock("@/features/auth/otp-step", () => ({
  OtpStep: ({
    onVerify,
    onBack,
    destination,
  }: {
    onVerify: (code: string) => void | Promise<void>;
    onBack: () => void;
    destination: string;
  }) => (
    <div>
      <p>OTP sent to {destination}</p>
      <button onClick={() => onVerify("482913")}>Submit OTP</button>
      <button onClick={onBack}>Back</button>
    </div>
  ),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe("LoginForm", () => {
  it("takes the user from mobile entry through OTP verification to the dashboard", async () => {
    const user = userEvent.setup();
    loginStartMock.mockResolvedValueOnce({
      userId: "user-1",
      mobileNumber: "+919876543210",
      otpExpiresAt: "2026-07-12T10:05:00Z",
    });
    loginVerifyMock.mockResolvedValueOnce({
      userId: "user-1",
      accessToken: "access-token",
      accessTokenExpiresAt: "2026-07-12T10:15:00Z",
      refreshToken: "refresh-token",
      refreshTokenExpiresAt: "2026-08-11T10:00:00Z",
      tokenType: "Bearer",
    });

    render(<LoginForm />);

    const mobileInput = screen.getByLabelText("Mobile number");
    await user.clear(mobileInput);
    await user.type(mobileInput, "+919876543210");
    await user.click(screen.getByRole("button", { name: "Continue" }));

    expect(await screen.findByText("OTP sent to +919876543210")).toBeInTheDocument();
    expect(loginStartMock).toHaveBeenCalledWith({ mobileNumber: "+919876543210" });

    await user.click(screen.getByRole("button", { name: "Submit OTP" }));

    await waitFor(() => {
      expect(loginVerifyMock).toHaveBeenCalledWith({ userId: "user-1", code: "482913" });
    });
    expect(loginMock).toHaveBeenCalledWith(
      expect.objectContaining({ accessToken: "access-token" })
    );
    expect(pushMock).toHaveBeenCalledWith("/dashboard");
  });

  it("shows an inline field error when the mobile number is not registered", async () => {
    const user = userEvent.setup();
    loginStartMock.mockRejectedValueOnce(
      new ApiError({
        status: 404,
        title: "Not Found",
        detail: "no account is registered for this mobile number",
        code: "mobile-not-registered",
        type: "about:blank",
      })
    );

    render(<LoginForm />);

    const mobileInput = screen.getByLabelText("Mobile number");
    await user.clear(mobileInput);
    await user.type(mobileInput, "+919876543210");
    await user.click(screen.getByRole("button", { name: "Continue" }));

    expect(
      await screen.findByText("No account is registered for this mobile number.")
    ).toBeInTheDocument();
    expect(screen.queryByText(/OTP sent to/)).not.toBeInTheDocument();
  });

  it("returns to the mobile form when the OTP step goes back", async () => {
    const user = userEvent.setup();
    loginStartMock.mockResolvedValueOnce({
      userId: "user-1",
      mobileNumber: "+919876543210",
      otpExpiresAt: "2026-07-12T10:05:00Z",
    });

    render(<LoginForm />);

    const mobileInput = screen.getByLabelText("Mobile number");
    await user.clear(mobileInput);
    await user.type(mobileInput, "+919876543210");
    await user.click(screen.getByRole("button", { name: "Continue" }));

    expect(await screen.findByText("OTP sent to +919876543210")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Back" }));

    expect(screen.getByLabelText("Mobile number")).toBeInTheDocument();
  });
});
