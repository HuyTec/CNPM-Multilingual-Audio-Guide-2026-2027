import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import jsQR from 'jsqr';
import { createHash } from 'node:crypto';

test.skip(!process.env.SYNC_LIVE, 'Explicit real-stack check; never edits Admin content');
const origin = process.env.PUBLIC_DEMO_URL || 'http://127.0.0.1:14176';
const admin = process.env.SYNC_ADMIN_URL || 'http://127.0.0.1:3001';

test('browser installation is shared by tabs and Admin displays platform totals', async ({ page, context, request }) => {
  const login = await request.post(admin + '/api/admin/auth/login', { data: {
    username: process.env.SYNC_ADMIN_USERNAME || 'editor', password: process.env.SYNC_ADMIN_PASSWORD || 'EditorDemo!2026',
  } });
  expect(login.ok()).toBeTruthy();
  const session = await login.json();
  try {
    const beat = page.waitForResponse(r => r.url().endsWith('/api/visitors/heartbeat') && r.request().method() === 'POST');
    await page.goto(origin);
    expect((await beat).status()).toBe(204);
    const id = await page.evaluate(() => localStorage.getItem('travelvoice-visitor-v1'));
    expect(id).toMatch(/^[a-f0-9-]{36}$/i);
    const second = await context.newPage();
    const secondBeat = second.waitForResponse(r => r.url().endsWith('/api/visitors/heartbeat') && r.request().method() === 'POST');
    await second.goto(origin);
    const body = (await secondBeat).request().postDataJSON();
    expect(body).toEqual({ installationId: id, platform: 'WEB' });
    await second.close();
    const stats = await request.get(admin + '/api/admin/visitors', { headers: { Authorization: 'Bearer ' + session.token } });
    expect(stats.status()).toBe(200);
    const snapshot = await stats.json();
    expect(snapshot.activeWindowMinutes).toBe(5);
    expect(snapshot.platforms.find((p: { platform: string }) => p.platform === 'WEB').activeInstallations).toBeGreaterThan(0);
    await page.goto(admin + '/analytics');
    await page.getByLabel('Tên đăng nhập').fill(process.env.SYNC_ADMIN_USERNAME || 'editor');
    await page.getByLabel('Mật khẩu').fill(process.env.SYNC_ADMIN_PASSWORD || 'EditorDemo!2026');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    const region = page.getByRole('region', { name: 'Khách vãng lai dùng ứng dụng' });
    await expect(region.getByRole('heading', { name: 'Android', exact: true })).toBeVisible();
    await expect(region.getByRole('heading', { name: 'Web du khách', exact: true })).toBeVisible();
    await page.setViewportSize({ width: 390, height: 844 });
    await region.scrollIntoViewIfNeeded();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
    expect((await new AxeBuilder({ page }).include('[aria-labelledby="visitor-stats-title"]').analyze()).violations).toEqual([]);
    await region.screenshot({ path: 'test-results/visitor-sync/admin-visitors-mobile.png' });
  } finally {
    await request.post(admin + '/api/admin/auth/logout', { headers: { Authorization: 'Bearer ' + session.token } });
  }
});

test('new Android QR points to the exact APK published with current metadata', async ({ page, request }) => {
  test.setTimeout(180_000);
  await page.goto(origin);
  const promo = page.getByRole('region', { name: 'Khám phá cùng TravelVoice trên Android' });
  const download = promo.getByRole('link', { name: 'Tải ứng dụng Android', exact: true });
  await expect(download).toBeVisible();
  const href = (await download.getAttribute('href'))!;
  const qr = promo.getByRole('img', { name: 'Mã QR tải ứng dụng TravelVoice cho Android' });
  const raw = await qr.evaluate(async image => {
    const img = image as HTMLImageElement;
    await img.decode();
    const canvas = document.createElement('canvas'); canvas.width = img.naturalWidth; canvas.height = img.naturalHeight;
    const ctx = canvas.getContext('2d')!; ctx.drawImage(img, 0, 0);
    return { pixels: Array.from(ctx.getImageData(0, 0, canvas.width, canvas.height).data), width: canvas.width, height: canvas.height };
  });
  expect(jsQR(new Uint8ClampedArray(raw.pixels), raw.width, raw.height)?.data).toBe(href);
  const metadata = await (await request.get(origin + '/downloads/android-release.json')).json();
  expect(metadata.version).toBe('1.1.1-dev');
  expect(href).toBe(new URL(metadata.url, origin).href);
  expect(href).toContain(metadata.sha256.slice(0, 12));
  const response = await request.get(href);
  expect(response.status()).toBe(200);
  const apk = await response.body();
  expect(apk.byteLength).toBe(metadata.sizeBytes);
  expect(createHash('sha256').update(apk).digest('hex')).toBe(metadata.sha256);
  await promo.screenshot({ path: 'test-results/visitor-sync/android-new-qr.png' });
});
