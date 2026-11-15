import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test.skip(!process.env.SYNC_LIVE, 'Explicit real-stack check; never edits Admin content');
const origin = process.env.PUBLIC_DEMO_URL || 'http://127.0.0.1:14176';
const admin = process.env.SYNC_ADMIN_URL || 'http://127.0.0.1:3001';

test('browser installation is shared by tabs and Admin reports Web presence', async ({ page, context, request }) => {
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
    const region = page.getByRole('region', { name: 'Hoạt động trên Web du khách' });
    await expect(region.getByText('Hoạt động trong 5 phút')).toBeVisible();
    await page.setViewportSize({ width: 390, height: 844 });
    await region.scrollIntoViewIfNeeded();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
    expect((await new AxeBuilder({ page }).include('[aria-labelledby="visitor-stats-title"]').analyze()).violations).toEqual([]);
    await region.screenshot({ path: 'test-results/visitor-sync/admin-visitors-mobile.png' });
  } finally {
    await request.post(admin + '/api/admin/auth/logout', { headers: { Authorization: 'Bearer ' + session.token } });
  }
});
