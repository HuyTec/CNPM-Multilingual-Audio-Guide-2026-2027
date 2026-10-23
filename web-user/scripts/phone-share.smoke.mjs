import { chromium, devices, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const origin = (process.env.PUBLIC_WEB_URL || await readFile(new URL('../../docs/qr/web-user-demo.txt', import.meta.url), 'utf8')).trim();
if (new URL(origin).protocol !== 'https:') throw new Error('Expected public HTTPS URL');
const browser = await chromium.launch({ channel: 'chrome', headless: true });
try {
  for (const name of ['Pixel 7', 'iPhone 13']) {
    const { viewport, deviceScaleFactor, isMobile, hasTouch, userAgent } = devices[name];
    const context = await browser.newContext({ viewport, deviceScaleFactor, isMobile, hasTouch, userAgent,
      permissions: ['geolocation'], geolocation: { latitude: 10.777, longitude: 106.6953, accuracy: 5 } });
    const page = await context.newPage();
    const errors = []; const receipts = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('request', request => { if (request.url().endsWith('/api/playbacks')) receipts.push(request.url()); });
    await page.goto(origin, { waitUntil: 'domcontentloaded', timeout: 60_000 });
    await expect(page.locator('.guide-map-icon')).toHaveCount(7, { timeout: 30_000 });
    await page.getByRole('button', { name: 'Danh sách', exact: true }).click();
    const list = page.getByRole('list', { name: 'Danh sách địa điểm', exact: true });
    await expect(list.getByRole('listitem')).toHaveCount(7);
    expect(await page.evaluate(() => window.isSecureContext)).toBe(true);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.getByRole('button', { name: 'Đóng danh sách' }).click();
    await expect(list).toBeHidden();
    await page.getByRole('button', { name: 'Định vị vị trí hiện tại' }).click();
    await expect(page.locator('.user-location-icon')).toHaveCount(1);
    await page.screenshot({ path: fileURLToPath(new URL(`../../docs/qr/phone-${name.startsWith('Pixel') ? 'android' : 'iphone'}.png`, import.meta.url)) });
    await page.getByRole('button', { name: 'Danh sách', exact: true }).click();
    await expect(list).toBeVisible();
    await page.getByRole('combobox', { name: 'Ngôn ngữ' }).selectOption('EN');
    await list.getByRole('link').filter({ hasText: 'Dinh Độc Lập' }).click();
    await expect(page.locator('.script-text')).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('.script-text')).toHaveAttribute('lang', 'en');
    expect(receipts).toEqual([]);
    for (const blocked of ['/api/admin/analytics', '/actuator/health', '/.env']) {
      expect((await context.request.get(origin + blocked)).status()).toBe(404);
    }
    expect(errors).toEqual([]);
    console.log(`${name}: HTTPS, 7 places, guide, GPS emulation and private-route blocking passed`);
    await context.close();
  }
} finally { await browser.close(); }
