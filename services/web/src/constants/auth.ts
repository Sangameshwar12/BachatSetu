import type { PreferredLanguage } from "@/types/auth";

/** Matches the backend's `SignupStartRequest.mobileNumber` pattern exactly: `+91[6-9]\d{9}`. */
export const INDIAN_MOBILE_PATTERN = /^\+91[6-9]\d{9}$/;

/** Matches the backend's `ValidationPatterns.OTP_CODE`: exactly six digits. */
export const OTP_CODE_LENGTH = 6;
export const OTP_CODE_PATTERN = /^[0-9]{6}$/;

export const preferredLanguages: { value: PreferredLanguage; label: string }[] = [
  { value: "ENGLISH", label: "English" },
  { value: "HINDI", label: "हिन्दी (Hindi)" },
  { value: "MARATHI", label: "मराठी (Marathi)" },
];

/** Local storage key for the persisted session — read/written only by `auth-context.tsx`. */
export const SESSION_STORAGE_KEY = "bachatsetu.session";

/** Matches the backend's `PLATFORM_ADMIN` role exactly (see `AdminController`'s `@PreAuthorize`). */
export const PLATFORM_ADMIN_ROLE = "PLATFORM_ADMIN";
