const CACHE = "goatdesk-v1";
const FILES = ["goatdesk.html", "manifest.json"];

self.addEventListener("install", e => {
  e.waitUntil(
    caches.open(CACHE).then(c => c.addAll(FILES))
  );
  self.skipWaiting();
});

self.addEventListener("activate", e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener("fetch", e => {
  // Only cache GET requests for local files — never cache API/proxy calls
  if (e.request.method !== "GET") return;
  if (e.request.url.includes("/proxy/")) return;
  if (e.request.url.includes("farmos.net")) return;

  e.respondWith(
    caches.match(e.request).then(cached => {
      return cached || fetch(e.request).then(res => {
        return caches.open(CACHE).then(c => {
          c.put(e.request, res.clone());
          return res;
        });
      }).catch(() => cached);
    })
  );
});
