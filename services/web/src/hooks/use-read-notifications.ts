"use client";

import { useCallback, useEffect, useState } from "react";

const STORAGE_KEY = "bachatsetu.readNotifications";

/**
 * Client-side-only "read" tracking. The Notification domain model has no `READ` status — only
 * `QUEUED → SENDING → SENT → DELIVERED`/`FAILED` — and no REST endpoint to mark a notification
 * read. This is therefore a local, per-device convenience, not synced to the backend.
 */
export function useReadNotifications() {
  const [readIds, setReadIds] = useState<Set<string>>(new Set());

  // One-time hydration from localStorage on mount — a genuine side-effectful read (with error
  // handling) rather than a pure snapshot, so this is a deliberate, justified exception.
  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setReadIds(new Set(JSON.parse(raw) as string[]));
      }
    } catch {
      // Ignore corrupt local storage; treat everything as unread.
    }
  }, []);

  const markRead = useCallback((notificationId: string) => {
    setReadIds((previous) => {
      if (previous.has(notificationId)) {
        return previous;
      }
      const next = new Set(previous);
      next.add(notificationId);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(next)));
      return next;
    });
  }, []);

  const isRead = useCallback((notificationId: string) => readIds.has(notificationId), [readIds]);

  return { isRead, markRead };
}
