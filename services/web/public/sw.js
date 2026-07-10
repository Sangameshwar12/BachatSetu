// Minimal offline-fallback service worker: no asset caching or offline-first strategy, just a
// safety net so a lost connection during navigation shows the app's own offline page instead of
// the browser's default error screen. Everything else is always fetched from the network.
const OFFLINE_URL = "/offline";
const CACHE_NAME = "bachatsetu-offline-v1";

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.add(OFFLINE_URL)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener("fetch", (event) => {
  if (event.request.mode !== "navigate") {
    return;
  }
  event.respondWith(
    fetch(event.request).catch(() => caches.match(OFFLINE_URL))
  );
});
