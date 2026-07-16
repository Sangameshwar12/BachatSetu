import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * True for paths safe to `router.push`/`router.replace` after login — a single leading `/`
 * only, never `//` or `/\` (both browser-interpreted as protocol-relative, i.e. an open
 * redirect off-site). Used to validate a `redirect` query param before honoring it.
 */
export function isSafeRedirectPath(path: string | null | undefined): path is string {
  return typeof path === "string" && /^\/(?!\/|\\)/.test(path)
}
