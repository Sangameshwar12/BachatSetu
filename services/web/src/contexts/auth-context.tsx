"use client";

import { useRouter } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { toast } from "sonner";

import { SESSION_STORAGE_KEY } from "@/constants/auth";
import { decodeAccessToken, isExpired } from "@/lib/jwt";
import { setAccessToken, setUnauthorizedHandler } from "@/lib/token-store";
import { logout as logoutRequest, refreshAccessToken } from "@/services/auth-service";
import type { AuthSession, TokenIssuedResponse } from "@/types/auth";

/** How long before the access token actually expires to trigger a silent refresh. */
const REFRESH_MARGIN_MS = 60_000;

interface AuthContextValue {
  session: AuthSession | null;
  /** True until the initial localStorage hydration check has run once, client-side. */
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (tokens: TokenIssuedResponse) => void;
  logout: () => void;
  /** Client-side UX gating only — see the `roles` field doc on `AuthSession`. */
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function toSession(tokens: TokenIssuedResponse): AuthSession | null {
  const claims = decodeAccessToken(tokens.accessToken);
  if (!claims) {
    return null;
  }
  return {
    accessToken: tokens.accessToken,
    accessTokenExpiresAt: tokens.accessTokenExpiresAt,
    refreshToken: tokens.refreshToken,
    refreshTokenExpiresAt: tokens.refreshTokenExpiresAt,
    userId: claims.userId,
    mobileNumber: claims.mobileNumber,
    roles: claims.roles,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  // Read by callbacks that must not be recreated every time the session changes (the
  // unauthorized-handler effect below registers/unregisters on every render otherwise).
  const sessionRef = useRef<AuthSession | null>(null);
  useEffect(() => {
    sessionRef.current = session;
  }, [session]);

  const applySession = useCallback((next: AuthSession) => {
    setSession(next);
    setAccessToken(next.accessToken);
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(next));
  }, []);

  const clearSession = useCallback(() => {
    setSession(null);
    setAccessToken(null);
    localStorage.removeItem(SESSION_STORAGE_KEY);
  }, []);

  const logout = useCallback(() => {
    const current = sessionRef.current;
    clearSession();
    if (current) {
      // Best-effort: the session is already cleared locally regardless of the server's
      // response, so a stale/already-revoked refresh token here is not an error the user
      // needs to see.
      logoutRequest({ refreshToken: current.refreshToken }).catch(() => {});
    }
  }, [clearSession]);

  const login = useCallback(
    (tokens: TokenIssuedResponse) => {
      const next = toSession(tokens);
      if (!next) {
        return;
      }
      applySession(next);
    },
    [applySession]
  );

  const forceLogoutToLogin = useCallback(
    (message: string) => {
      clearSession();
      toast.error(message);
      router.replace("/login");
    },
    [clearSession, router]
  );

  // One-time session hydration from localStorage on mount — a genuine side-effectful read (with
  // expiry validation, a possible network refresh, and a module-level token-store sync) rather
  // than a pure snapshot, so this is a deliberate, justified exception.
  useEffect(() => {
    let cancelled = false;

    async function hydrate() {
      const raw = localStorage.getItem(SESSION_STORAGE_KEY);
      if (!raw) {
        setIsLoading(false);
        return;
      }
      let parsed: AuthSession;
      try {
        parsed = JSON.parse(raw) as AuthSession;
      } catch {
        localStorage.removeItem(SESSION_STORAGE_KEY);
        setIsLoading(false);
        return;
      }
      // Older persisted sessions (pre-role-gating) never stored `roles`.
      const hydrated: AuthSession = { ...parsed, roles: parsed.roles ?? [] };

      if (!isExpired(new Date(hydrated.accessTokenExpiresAt).getTime())) {
        if (!cancelled) {
          setSession(hydrated);
          setAccessToken(hydrated.accessToken);
          setIsLoading(false);
        }
        return;
      }

      // The access token has expired, but the browser may have just reloaded mid-session —
      // the refresh token is likely still valid, so attempt a silent refresh before giving up.
      if (isExpired(new Date(hydrated.refreshTokenExpiresAt).getTime())) {
        localStorage.removeItem(SESSION_STORAGE_KEY);
        if (!cancelled) {
          setIsLoading(false);
        }
        return;
      }

      try {
        const tokens = await refreshAccessToken({ refreshToken: hydrated.refreshToken });
        const next = toSession(tokens);
        if (!next) {
          throw new Error("malformed refresh response");
        }
        if (!cancelled) {
          applySession(next);
        }
      } catch {
        localStorage.removeItem(SESSION_STORAGE_KEY);
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void hydrate();
    return () => {
      cancelled = true;
    };
    // Runs once on mount only — applySession is stable (empty deps).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Reactive fallback: any 401 the API client sees (clock skew, a token invalidated
  // out-of-band, ...) still ends the session immediately, independent of the proactive timer
  // below.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      forceLogoutToLogin("Your session has expired — please log in again.");
    });
    return () => setUnauthorizedHandler(null);
  }, [forceLogoutToLogin]);

  // Proactive silent refresh: reschedules itself every time `session` changes (including right
  // after a refresh installs a session with a new expiry), so the access token is renewed
  // shortly before it would actually expire and the user is never interrupted while the
  // refresh token remains valid.
  useEffect(() => {
    if (!session) {
      return;
    }
    const expiresAt = new Date(session.accessTokenExpiresAt).getTime();
    const delay = Math.max(expiresAt - Date.now() - REFRESH_MARGIN_MS, 0);

    const timer = setTimeout(() => {
      void (async () => {
        try {
          const tokens = await refreshAccessToken({ refreshToken: session.refreshToken });
          const next = toSession(tokens);
          if (!next) {
            throw new Error("malformed refresh response");
          }
          applySession(next);
        } catch {
          forceLogoutToLogin("Your session has expired — please log in again.");
        }
      })();
    }, delay);

    return () => clearTimeout(timer);
  }, [session, applySession, forceLogoutToLogin]);

  const hasRole = useCallback(
    (role: string) => session?.roles.includes(role) ?? false,
    [session]
  );

  const value = useMemo<AuthContextValue>(
    () => ({ session, isLoading, isAuthenticated: session !== null, login, logout, hasRole }),
    [session, isLoading, login, logout, hasRole]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
