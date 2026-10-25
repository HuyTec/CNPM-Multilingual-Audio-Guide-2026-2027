import { createServer, request as httpRequest } from 'node:http';
import { request as httpsRequest } from 'node:https';
import { createReadStream } from 'node:fs';
import { realpath, stat } from 'node:fs/promises';
import { dirname, extname, resolve, sep } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const uuid = '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}';
const locationRoute = new RegExp(`^/api/locations(?:/${uuid}(?:/guide)?)?$`);
const audioRoute = new RegExp(`^/api/content-audio/${uuid}\\.(?:mp3|wav|ogg)$`);
const imageRoute = new RegExp(`^/api/content-images/${uuid}\\.png$`);
const guidePage = new RegExp(`^/locations/${uuid}/?$`);
const packageRoute = /^\/api\/content-packages(?:\/key|\/(?:BASE|OPTIONAL)\/(?:base|optional)-[a-f0-9]{64})?$/;
const mime = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8', '.svg': 'image/svg+xml', '.png': 'image/png', '.webp': 'image/webp', '.woff2': 'font/woff2' };

/** Publish the production tourist app and its public APIs, never the dev server or admin API. */
export function createPublicServer({ dist, backend }) {
  const target = new URL(backend);
  if (!['http:', 'https:'].includes(target.protocol)) throw new Error('Invalid backend protocol');
  const transport = target.protocol === 'https:' ? httpsRequest : httpRequest;
  const reply = (response, status, message) => {
    if (!response.headersSent) response.writeHead(status, { 'Content-Type': 'text/plain; charset=utf-8', 'Cache-Control': 'no-store' });
    response.end(message);
  };
  return createServer(async (req, res) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    res.setHeader('X-Frame-Options', 'SAMEORIGIN');
    let url;
    try { url = new URL(req.url || '/', 'http://localhost'); }
    catch { reply(res, 400, 'Invalid URL'); return; }
    const path = url.pathname;
    const read = req.method === 'GET' || req.method === 'HEAD';
    const publicApi = locationRoute.test(path) || imageRoute.test(path) || audioRoute.test(path) || packageRoute.test(path)
      || path === '/api/categories' || /^\/demo-audio\/[a-z-]+\.wav$/.test(path);
    const playback = path === '/api/playbacks' && req.method === 'POST';
    if (publicApi || playback) {
      if (!read && !playback) { reply(res, 405, 'Method not allowed'); return; }
      let body;
      if (playback) {
        if (!/^application\/json(?:\s*;|$)/i.test(req.headers['content-type'] || '')) { reply(res, 415, 'JSON required'); return; }
        try {
          const chunks = []; let length = 0;
          for await (const chunk of req) {
            length += chunk.length;
            if (length > 2048) { reply(res, 413, 'Request too large'); return; }
            chunks.push(chunk);
          }
          const data = JSON.parse(Buffer.concat(chunks).toString('utf8'));
          const validId = new RegExp(`^${uuid}$`);
          if (!data || typeof data.eventId !== 'string' || typeof data.contentId !== 'string'
            || !validId.test(data.eventId) || !validId.test(data.contentId)) throw new Error();
          if (data.offline !== undefined && typeof data.offline !== 'boolean') throw new Error();
          if (data.occurredAt !== undefined && (typeof data.occurredAt !== 'string' || !Number.isFinite(Date.parse(data.occurredAt)))) throw new Error();
          body = JSON.stringify({ eventId: data.eventId, contentId: data.contentId, offline: data.offline, occurredAt: data.occurredAt });
        } catch { reply(res, 400, 'Invalid playback receipt'); return; }
      }
      const headers = { accept: req.headers.accept || '*/*' };
      for (const name of ['range', 'if-range', 'if-none-match', 'if-modified-since']) if (req.headers[name]) headers[name] = req.headers[name];
      if (body) { headers['content-type'] = 'application/json'; headers['content-length'] = Buffer.byteLength(body); }
      const upstream = transport(new URL(path + url.search, target.origin), { method: req.method, headers }, response => {
        const output = { 'cache-control': 'no-store' };
        for (const name of ['content-type', 'content-length', 'content-range', 'accept-ranges', 'etag', 'last-modified'])
          if (response.headers[name]) output[name] = response.headers[name];
        res.writeHead(response.statusCode || 502, output);
        response.on('error', () => res.destroy()); response.pipe(res);
      });
      upstream.setTimeout(30_000, () => upstream.destroy(new Error('Backend timeout')));
      upstream.on('error', () => reply(res, 502, 'Chưa kết nối được backend. Hãy thử lại.'));
      res.on('close', () => { if (!res.writableEnded) upstream.destroy(); });
      upstream.end(body);
      return;
    }
    if (!read) { reply(res, 405, 'Method not allowed'); return; }
    let file;
    if (path === '/' || path === '/index.html' || path === '/offline' || guidePage.test(path)) file = 'index.html';
    else if (['/sw.js', '/playback-outbox.js', '/asset-manifest.json'].includes(path)) file = path.slice(1);
    else if (/^\/assets\/[a-zA-Z0-9_.-]+$/.test(path)) file = path.slice(1);
    else if (['/downloads/android-release.json', '/downloads/travelvoice.apk'].includes(path)) file = path.slice(1);
    else { reply(res, 404, 'Not found'); return; }
    try {
      const root = await realpath(dist);
      const absolute = await realpath(resolve(root, file));
      if (!absolute.startsWith(root + sep)) { reply(res, 404, 'Not found'); return; }
      const metadata = await stat(absolute);
      if (!metadata.isFile()) throw new Error();
      res.writeHead(200, { 'Content-Type': file.endsWith('.apk') ? 'application/vnd.android.package-archive' : mime[extname(absolute)] || 'application/octet-stream',
        ...(file.endsWith('.apk') ? { 'Content-Disposition': 'attachment; filename="travelvoice.apk"' } : {}),
        'Content-Length': metadata.size, 'Cache-Control': file.startsWith('assets/') ? 'public, max-age=31536000, immutable' : 'no-cache' });
      if (req.method === 'HEAD') res.end();
      else createReadStream(absolute).on('error', () => res.destroy()).pipe(res);
    } catch { reply(res, 404, 'Not found'); }
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const project = resolve(dirname(fileURLToPath(import.meta.url)), '..');
  // The VPS image has no build tools or local .env files. Local sharing retains Vite env support.
  let backend = process.env.API_PROXY_TARGET;
  if (!backend) {
    const { loadEnv } = await import('vite');
    const env = loadEnv('production', project, 'API_PROXY_TARGET');
    const root = loadEnv('production', resolve(project, '..'), 'BACKEND_');
    backend = env.API_PROXY_TARGET || `http://127.0.0.1:${root.BACKEND_PORT || '8080'}`;
  }
  const port = Number(process.env.PUBLIC_WEB_PORT || 4176);
  const host = process.env.PUBLIC_WEB_HOST || '127.0.0.1';
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid PUBLIC_WEB_PORT');
  if (!['127.0.0.1', '0.0.0.0'].includes(host)) throw new Error('Invalid PUBLIC_WEB_HOST');
  await stat(resolve(project, 'dist/index.html'));
  const server = createPublicServer({ dist: resolve(project, 'dist'), backend });
  server.listen(port, host, () => console.log(`TravelVoice public gateway: http://${host}:${port}`));
  server.on('error', error => { console.error(error.message); process.exitCode = 1; });
}
