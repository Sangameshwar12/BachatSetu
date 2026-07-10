import { apiClient } from "@/services/api-client";
import type {
  OtpInvalidateRequest,
  OtpRequestRequest,
  OtpRequestResponse,
  OtpResendRequest,
  OtpVerifyRequest,
  OtpVerifyResponse,
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
