"use client";

import { useRouter } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { SESSION_STORAGE_KEY } from "@/constants/auth";
import { decodeAccessToken, isExpired } from "@/lib/jwt";
import { setAccessToken, setUnauthorizedHandler } from "@/lib/token-store";
import type { AuthSession, SignupVerifyResponse } from "@/types/auth";

interface AuthContextValue {
  session: AuthSession | null;
  /** True until the initial localStorage hydration check has run once, client-side. */
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (tokens: SignupVerifyResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function toSession(tokens: SignupVerifyResponse): AuthSession | null {
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
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  const logout = useCallback(() => {
    setSession(null);
    setAccessToken(null);
    localStorage.removeItem(SESSION_STORAGE_KEY);
  }, []);

  const login = useCallback((tokens: SignupVerifyResponse) => {
    const next = toSession(tokens);
    if (!next) {
      return;
    }
    setSession(next);
    setAccessToken(next.accessToken);
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(next));
  }, []);

  // One-time session hydration from localStorage on mount — a genuine side-effectful read (with
  // expiry validation and a module-level token-store sync) rather than a pure snapshot, so this
  // is a deliberate, justified exception.
  useEffect(() => {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY);
    if (raw) {
      try {
        const parsed = JSON.parse(raw) as AuthSession;
        if (!isExpired(new Date(parsed.accessTokenExpiresAt).getTime())) {
          // eslint-disable-next-line react-hooks/set-state-in-effect
          setSession(parsed);
          setAccessToken(parsed.accessToken);
        } else {
          localStorage.removeItem(SESSION_STORAGE_KEY);
        }
      } catch {
        localStorage.removeItem(SESSION_STORAGE_KEY);
      }
    }
    setIsLoading(false);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout();
      router.replace("/login");
    });
    return () => setUnauthorizedHandler(null);
  }, [logout, router]);

  const value = useMemo<AuthContextValue>(
    () => ({ session, isLoading, isAuthenticated: session !== null, login, logout }),
    [session, isLoading, login, logout]
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
