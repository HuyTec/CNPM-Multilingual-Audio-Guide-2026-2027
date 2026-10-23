import { flushReceipts, syncTag } from './playback-outbox.js';
const SHELL = 'travelvoice-shell-' + new URL(self.location.href).searchParams.get('build');
self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(SHELL);
    const response = await fetch('/index.html', { cache: 'reload' });
    if (!response.ok) throw new Error('Shell unavailable');
    const html = await response.clone().text();
    const assets = [...html.matchAll(/(?:src|href)=["]([^"]+[.](?:js|css))["]/g)].map(match => match[1]);
    // Include lazy map/reader dependencies before claiming that the shell is available offline.
    const manifestResponse = await fetch('/asset-manifest.json', { cache: 'reload' });
    if (!manifestResponse.ok) throw new Error('Build manifest unavailable');
    const manifest = await manifestResponse.json();
    for (const entry of Object.values(manifest)) {
      for (const file of [entry.file, ...(entry.css || []), ...(entry.assets || [])]) {
        if (typeof file === 'string' && file.startsWith('assets/')) assets.push('/' + file);
      }
    }
    await cache.put('/index.html', response);
    await cache.addAll([...new Set([...assets, '/playback-outbox.js'])]);
    await self.skipWaiting();
  })());
});
self.addEventListener('activate', event => { event.waitUntil((async () => {
  await self.clients.claim();
  for (const key of await caches.keys()) if (key.startsWith('travelvoice-shell-') && key !== SHELL) await caches.delete(key);
})()); });
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin || event.request.method !== 'GET' || url.pathname.startsWith('/api/') || url.pathname.startsWith('/demo-audio/')) return;
  if (event.request.mode === 'navigate') {
    event.respondWith(fetch(event.request).catch(async () => (await caches.open(SHELL)).match('/index.html')));
  } else if (url.pathname.startsWith('/assets/') || url.pathname === '/playback-outbox.js') {
    event.respondWith((async () => {
      // Immutable same-origin build assets are identical despite Vite's Vary: Origin.
      const cache = await caches.open(SHELL); const cached = await cache.match(event.request, { ignoreVary: true });
      if (cached) return cached;
      const response = await fetch(event.request); if (response.ok) await cache.put(event.request, response.clone()); return response;
    })());
  }
});
self.addEventListener('sync', event => {
  if (event.tag !== syncTag) return;
  event.waitUntil((async () => {
    const result = await flushReceipts(true);
    for (const client of await self.clients.matchAll()) client.postMessage({ type: 'PLAYBACK_OUTBOX_CHANGED' });
    if (result.pending) throw new Error('Playback delivery pending');
  })());
});
self.addEventListener('notificationclick', event => {
  event.notification.close();
  const { locationId, language } = event.notification.data || {};
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(locationId || '')
    || !['VI', 'EN', 'FR', 'JA', 'ZH', 'RU'].includes(language)) return;
  const url = new URL(`/locations/${locationId}?language=${language}&scriptType=FULL`, self.location.origin).href;
  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    const client = windows.find(client => new URL(client.url).origin === self.location.origin);
    if (client) { await client.navigate(url); try { await client.focus(); } catch { /* Navigation still works when browser policy prevents focus. */ } }
    else await self.clients.openWindow(url);
  })());
});
