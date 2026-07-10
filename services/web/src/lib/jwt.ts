import type { AccessTokenClaims } from "@/types/auth";

/**
 * Decodes (never verifies) a JWT access token's payload for display and client-side expiry
 * checks. The backend signs with HS512 using a server-only secret, so the browser has no way
 * to verify the signature — this is purely a UX convenience, never an authorization decision.
 * Claim names (`mobile_number`, `tenant_id`, `roles`, `permissions`) match
 * `infrastructure.auth.adapter.JwtProviderAdapter` exactly.
 */
export function decodeAccessToken(token: string): AccessTokenClaims | null {
  const segments = token.split(".");
  if (segments.length !== 3) {
    return null;
  }

  try {
    const payload = JSON.parse(base64UrlDecode(segments[1])) as Record<string, unknown>;
    if (typeof payload.sub !== "string" || typeof payload.mobile_number !== "string") {
      return null;
    }

    return {
      userId: payload.sub,
      mobileNumber: payload.mobile_number,
      tenantId: typeof payload.tenant_id === "string" ? payload.tenant_id : "",
      roles: Array.isArray(payload.roles) ? (payload.roles as string[]) : [],
      permissions: Array.isArray(payload.permissions) ? (payload.permissions as string[]) : [],
      issuedAt: typeof payload.iat === "number" ? payload.iat * 1000 : 0,
      expiresAt: typeof payload.exp === "number" ? payload.exp * 1000 : 0,
    };
  } catch {
    return null;
  }
}

export function isExpired(expiresAtMs: number): boolean {
  return Date.now() >= expiresAtMs;
}

function base64UrlDecode(segment: string): string {
  const normalized = segment.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
  return decodeURIComponent(
    atob(padded)
      .split("")
      .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, "0")}`)
      .join("")
  );
}
