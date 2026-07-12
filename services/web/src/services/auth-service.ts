import { apiClient } from "@/services/api-client";
import type {
  LoginStartRequest,
  LoginStartResponse,
  LoginVerifyRequest,
  LoginVerifyResponse,
  LogoutRequest,
  OtpInvalidateRequest,
  OtpRequestRequest,
  OtpRequestResponse,
  OtpResendRequest,
  OtpVerifyRequest,
  OtpVerifyResponse,
  RefreshTokenRequest,
  RefreshTokenResponse,
  SignupStartRequest,
  SignupStartResponse,
  SignupVerifyRequest,
  SignupVerifyResponse,
} from "@/types/auth";

/** `POST /api/v1/auth/signup` — creates the account and dispatches a REGISTRATION OTP. */
export async function signupStart(payload: SignupStartRequest): Promise<SignupStartResponse> {
  const { data } = await apiClient.post<SignupStartResponse>("/api/v1/auth/signup", payload);
  return data;
}

/** `POST /api/v1/auth/signup/verify` — activates the account and issues the first token pair. */
export async function signupVerify(payload: SignupVerifyRequest): Promise<SignupVerifyResponse> {
  const { data } = await apiClient.post<SignupVerifyResponse>(
    "/api/v1/auth/signup/verify",
    payload
  );
  return data;
}

/** `POST /api/v1/auth/otp/request` — requests a new OTP challenge for an existing userId. */
export async function requestOtp(payload: OtpRequestRequest): Promise<OtpRequestResponse> {
  const { data } = await apiClient.post<OtpRequestResponse>("/api/v1/auth/otp/request", payload);
  return data;
}

/** `POST /api/v1/auth/otp/verify` — verifies a six-digit code. Does not itself issue tokens. */
export async function verifyOtp(payload: OtpVerifyRequest): Promise<OtpVerifyResponse> {
  const { data } = await apiClient.post<OtpVerifyResponse>("/api/v1/auth/otp/verify", payload);
  return data;
}

/** `POST /api/v1/auth/otp/resend` — invalidates the active code and dispatches a replacement. */
export async function resendOtp(payload: OtpResendRequest): Promise<OtpRequestResponse> {
  const { data } = await apiClient.post<OtpRequestResponse>("/api/v1/auth/otp/resend", payload);
  return data;
}

/** `POST /api/v1/auth/otp/invalidate` — immediately invalidates the active OTP challenge. */
export async function invalidateOtp(payload: OtpInvalidateRequest): Promise<OtpRequestResponse> {
  const { data } = await apiClient.post<OtpRequestResponse>(
    "/api/v1/auth/otp/invalidate",
    payload
  );
  return data;
}

/** `POST /api/v1/auth/login/start` — looks up the account by mobile number and dispatches a SIGN_IN OTP. */
export async function loginStart(payload: LoginStartRequest): Promise<LoginStartResponse> {
  const { data } = await apiClient.post<LoginStartResponse>("/api/v1/auth/login/start", payload);
  return data;
}

/** `POST /api/v1/auth/login/verify` — verifies the sign-in OTP and issues the caller's tokens. */
export async function loginVerify(payload: LoginVerifyRequest): Promise<LoginVerifyResponse> {
  const { data } = await apiClient.post<LoginVerifyResponse>("/api/v1/auth/login/verify", payload);
  return data;
}

/** `POST /api/v1/auth/token/refresh` — rotates a refresh token for a new access/refresh token pair. */
export async function refreshAccessToken(payload: RefreshTokenRequest): Promise<RefreshTokenResponse> {
  const { data } = await apiClient.post<RefreshTokenResponse>("/api/v1/auth/token/refresh", payload);
  return data;
}

/** `POST /api/v1/auth/logout` — revokes a refresh token, ending its session. */
export async function logout(payload: LogoutRequest): Promise<void> {
  await apiClient.post("/api/v1/auth/logout", payload);
}
