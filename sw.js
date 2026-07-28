const CACHE = "farmdesk-v2";
const FILES = [
  "./farmdesk.html",
  "./manifest.json",
  "./icon192.png",
  "./icon512.png",
  // Vendored map library — cached so the map/draw/Walk mode work offline
  "./leaflet/leaflet.js",
  "./leaflet/leaflet.css",
  "./leaflet/leaflet.draw.js",
  "./leaflet/leaflet.draw.css"
];

self.addEventListener("install", e => {
  e.waitUntil(
    // Cache core files; ignore any that 404 so one missing file can't break install
    caches.open(CACHE).then(c => Promise.all(
      FILES.map(f => c.add(f).catch(err => console.log("SW: skip " + f, err)))
    ))
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
  e.respondWith(
    caches.match(e.request).then(cached => cached || fetch(e.request))
  );
});
