/**
 * Plain module-level singleton bridging `AuthProvider` (React state, localStorage) and
 * `api-client.ts`'s axios interceptors (which cannot use hooks). `AuthProvider` is the only
 * writer; the interceptors are the only readers.
 */

let currentAccessToken: string | null = null;
let unauthorizedHandler: (() => void) | null = null;

export function setAccessToken(token: string | null): void {
  currentAccessToken = token;
}

export function getAccessToken(): string | null {
  return currentAccessToken;
}

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

export function notifyUnauthorized(): void {
  unauthorizedHandler?.();
}
