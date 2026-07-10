"use client";

import { useSyncExternalStore } from "react";

const subscribe = () => () => {};

/**
 * True only once the client has hydrated. Using `useSyncExternalStore` (rather than a
 * `useState` + `useEffect` pair) avoids a synchronous `setState` inside an effect body while
 * still deferring to a second render pass after hydration, which is what every "client-only"
 * rendering guard in this codebase needs (theme-dependent icons, cached-locally-only data, etc).
 */
export function useHasMounted(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => true,
    () => false
  );
}
