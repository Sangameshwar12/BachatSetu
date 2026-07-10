/**
 * Client-side-only cache of profile details the user has already told us, once, at signup or
 * onboarding time. This exists because the backend has no `GET /api/v1/users/me` endpoint (see
 * Sprint FE-3 report) — there is no way to re-fetch a member's name, email, or address after the
 * fact. Signup and onboarding write to this cache immediately after each succeeds; the Profile
 * page reads it. It is best-effort and per-device: a fresh browser/session with a restored
 * session token will not have it.
 */

const STORAGE_KEY = "bachatsetu.profileCache";

export interface CachedProfile {
  givenName?: string;
  familyName?: string;
  email?: string;
  preferredLanguage?: string;
  city?: string;
  state?: string;
  notificationsEnabled?: boolean;
  hasPhoto?: boolean;
}

export function getCachedProfile(): CachedProfile | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as CachedProfile) : null;
  } catch {
    return null;
  }
}

export function mergeCachedProfile(patch: CachedProfile): void {
  const existing = getCachedProfile() ?? {};
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...existing, ...patch }));
}
