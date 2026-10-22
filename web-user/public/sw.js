const SHELL = 'travelvoice-shell-' + new URL(self.location.href).searchParams.get('build');
self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(SHELL);
    const response = await fetch('/index.html', { cache: 'reload' });
    if (!response.ok) throw new Error('Shell unavailable');
    const html = await response.clone().text();
    const assets = [...html.matchAll(/(?:src|href)=["]([^"]+[.](?:js|css))["]/g)].map(match => match[1]);
    await cache.put('/index.html', response);
    await cache.addAll(assets);
    await self.skipWaiting();
  })());
});
self.addEventListener('activate', event => { event.waitUntil(self.clients.claim()); });
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin || event.request.method !== 'GET' || url.pathname.startsWith('/api/') || url.pathname.startsWith('/demo-audio/')) return;
  if (event.request.mode === 'navigate') {
    event.respondWith(fetch(event.request).catch(async () => (await caches.open(SHELL)).match('/index.html')));
  } else if (url.pathname.startsWith('/assets/')) {
    event.respondWith((async () => {
      // Immutable same-origin build assets are identical despite Vite's Vary: Origin.
      const cache = await caches.open(SHELL); const cached = await cache.match(event.request, { ignoreVary: true });
      if (cached) return cached;
      const response = await fetch(event.request); if (response.ok) await cache.put(event.request, response.clone()); return response;
    })());
  }
});
