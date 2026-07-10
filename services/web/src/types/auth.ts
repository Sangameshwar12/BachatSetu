/**
 * Mirrors `services/backend`'s actual auth/onboarding REST DTOs exactly (see
 * `auth.interfaces.rest.dto.*`, `user.interfaces.rest.dto.*`). No field here was invented —
 * every shape traces back to a real backend record type.
 */

export type OtpPurpose = "REGISTRATION" | "SIGN_IN" | "PASSWORD_RESET" | "MOBILE_CHANGE";

export type PreferredLanguage = "ENGLISH" | "HINDI" | "MARATHI";

export interface SignupStartRequest {
  givenName: string;
  familyName?: string;
  mobileNumber: string;
  email?: string;
  preferredLanguage: PreferredLanguage;
  acceptedTerms: boolean;
}

export interface SignupStartResponse {
  userId: string;
  mobileNumber: string;
  otpExpiresAt: string;
}

export interface SignupVerifyRequest {
  userId: string;
  code: string;
}

export interface SignupVerifyResponse {
  userId: string;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  tokenType: string;
}

export interface OtpRequestRequest {
  userId: string;
  purpose: OtpPurpose;
}

export interface OtpRequestResponse {
  verificationId: string;
  purpose: OtpPurpose;
  status: string;
  expiresAt: string;
  resendCount: number;
}

export interface OtpVerifyRequest {
  userId: string;
  purpose: OtpPurpose;
  code: string;
}

export interface OtpVerifyResponse {
  verificationId: string;
  status: string;
  verified: boolean;
  verificationAttempts: number;
}

export interface OtpResendRequest {
  userId: string;
  purpose: OtpPurpose;
}

export interface OtpInvalidateRequest {
  userId: string;
  purpose: OtpPurpose;
}

export interface CompleteOnboardingRequest {
  city?: string;
  state?: string;
  photoFileId?: string;
  notificationsEnabled: boolean;
}

export interface OnboardingCompletedResponse {
  userId: string;
  city: string | null;
  state: string | null;
  photoFileId: string | null;
  notificationsEnabled: boolean;
}

export interface UploadFileResponse {
  fileId: string;
  provider: string;
  path: string;
}

/** Decoded, display-only claims from the access token — never used for authorization decisions. */
export interface AccessTokenClaims {
  userId: string;
  mobileNumber: string;
  tenantId: string;
  roles: string[];
  permissions: string[];
  issuedAt: number;
  expiresAt: number;
}

/** Persisted session shape — the full token pair plus the decoded identity for display. */
export interface AuthSession {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  userId: string;
  mobileNumber: string;
  /**
   * Decoded from the access token for client-side UX gating only (hiding nav items, redirecting
   * away from a role-restricted page before it renders). The backend independently enforces every
   * role check server-side — this is never the actual authorization boundary.
   */
  roles: string[];
}
