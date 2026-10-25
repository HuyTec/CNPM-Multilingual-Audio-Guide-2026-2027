import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { mkdtemp, mkdir, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { once } from 'node:events';
import { createPublicServer } from './public-server.mjs';

test('phone gateway serves only the built app and public backend routes', async () => {
  const root = await mkdtemp(join(tmpdir(), 'travelvoice-gateway-'));
  const seen = [];
  const backend = createServer(async (req, res) => {
    let body = ''; for await (const chunk of req) body += chunk;
    seen.push({ path: req.url, cookie: req.headers.cookie, auth: req.headers.authorization, range: req.headers.range, body });
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify({ path: req.url }));
  });
  backend.listen(0, '127.0.0.1'); await once(backend, 'listening');
  await mkdir(join(root, 'assets')); await writeFile(join(root, 'index.html'), '<h1>TravelVoice</h1>');
  await writeFile(join(root, 'assets/app.js'), 'console.log(1)');
  await writeFile(join(root, 'playback-outbox.js'), 'export const syncTag = "test";');
  await mkdir(join(root,'downloads'));
  await writeFile(join(root,'downloads/android-release.json'),'{"version":"test"}');
  await writeFile(join(root,'downloads/travelvoice.apk'),Buffer.from('PK-test-apk'));
  await writeFile(join(root,'downloads/private.txt'),'must not be served');
  const gateway = createPublicServer({ dist: root, backend: `http://127.0.0.1:${backend.address().port}` });
  gateway.listen(0, '127.0.0.1'); await once(gateway, 'listening');
  const origin = `http://127.0.0.1:${gateway.address().port}`;
  try {
    for (const path of ['/', '/offline', '/locations/20000000-0000-0000-0000-000000000101']) {
      const response = await fetch(origin + path); assert.equal(response.status, 200); assert.match(await response.text(), /TravelVoice/);
    }
    assert.equal((await fetch(origin + '/assets/app.js')).status, 200);
    assert.equal((await fetch(origin + '/playback-outbox.js')).status, 200);
    assert.equal((await fetch(origin+'/downloads/android-release.json')).status,200);
    const apk=await fetch(origin+'/downloads/travelvoice.apk');assert.equal(apk.status,200);
    assert.equal(apk.headers.get('content-type'),'application/vnd.android.package-archive');
    assert.match(apk.headers.get('content-disposition'),/attachment/);
    assert.equal(await apk.text(),'PK-test-apk');
    assert.equal((await fetch(origin+'/downloads/private.txt')).status,404);
    for (const path of ['/api/admin/analytics', '/api/admin/content/locations', '/actuator/health', '/.env', '/src/main.tsx', '/@vite/client', '/assets/%2e%2e/.env'])
      assert.equal((await fetch(origin + path)).status, 404, path);
    for (const path of ['/api/categories', '/api/locations?size=50', '/api/locations/20000000-0000-0000-0000-000000000101/guide?language=VI', '/api/content-packages/key', '/demo-audio/museum.wav'])
      assert.equal((await fetch(origin + path, { headers: { cookie: 'test=private', authorization: 'Bearer private', range: 'bytes=0-20' } })).status, 200);
    assert.ok(seen.every(item => !item.cookie && !item.auth)); assert.equal(seen[0].range, 'bytes=0-20');
    const receipt = { eventId: '30000000-0000-0000-0000-000000000001', contentId: '31000000-0000-0000-0000-000000000001' };
    assert.equal((await fetch(origin + '/api/playbacks', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(receipt) })).status, 200);
    const offline = { ...receipt, occurredAt: '2026-01-01T10:00:00Z', offline: true };
    assert.equal((await fetch(origin + '/api/playbacks', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(offline) })).status, 200);
    assert.deepEqual(JSON.parse(seen.at(-1).body), offline);
    assert.equal((await fetch(origin + '/api/playbacks', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' })).status, 400);
    assert.equal((await fetch(origin + '/api/locations', { method: 'DELETE' })).status, 405);
  } finally {
    gateway.closeAllConnections(); backend.closeAllConnections();
    await Promise.all([new Promise(resolve => gateway.close(resolve)), new Promise(resolve => backend.close(resolve))]);
    await rm(root, { recursive: true, force: true });
  }
});
