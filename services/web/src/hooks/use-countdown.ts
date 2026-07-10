"use client";

import { useEffect, useState } from "react";

function computeRemaining(targetMs: number | null): number {
  return targetMs ? Math.max(0, targetMs - Date.now()) : 0;
}

/** Ticks down to zero once a second against a fixed target timestamp (ms epoch). */
export function useCountdown(targetMs: number | null): number {
  const [remainingMs, setRemainingMs] = useState(() => computeRemaining(targetMs));

  // Resets the displayed value the moment the target changes — adjusting state during render
  // (rather than in an effect) is React's own recommended pattern for derived state that must
  // reset when a prop changes; see https://react.dev/learn/you-might-not-need-an-effect.
  const [previousTargetMs, setPreviousTargetMs] = useState(targetMs);
  if (targetMs !== previousTargetMs) {
    setPreviousTargetMs(targetMs);
    setRemainingMs(computeRemaining(targetMs));
  }

  useEffect(() => {
    if (!targetMs) {
      return;
    }
    const interval = setInterval(() => {
      setRemainingMs(computeRemaining(targetMs));
    }, 1000);
    return () => clearInterval(interval);
  }, [targetMs]);

  return remainingMs;
}
