import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const locationId = '20000000-0000-0000-0000-000000000101';
const names = { VI: 'Địa điểm khảo sát thử', EN: 'Survey fixture', FR: 'Lieu de test', JA: 'テスト地点', ZH: '测试地点', RU: 'Тестовое место' };
async function setup(page: Page, metadata = true, localizedNames = names) {
  await page.addInitScript(() => {
    let callback: PositionCallback | undefined; let fail: PositionErrorCallback | undefined;
    let active = false;
    Object.defineProperty(navigator, 'geolocation', { value: {
      getCurrentPosition() {},
      watchPosition(success: PositionCallback, error: PositionErrorCallback) { callback = success; fail = error; active = true; return 1; },
      clearWatch() { active = false; },
    } });
    Object.assign(window, {
      move: (lat: number, accuracy = 5, timestamp = Date.now()) => { if (active) callback?.({ timestamp, coords: { latitude: lat, longitude: 106.7, accuracy } } as GeolocationPosition); },
      deny: () => fail?.({ code: 1 } as GeolocationPositionError),
    });
  });
  await page.route('**/api/**', route => {
    if (route.request().url().includes('/guide')) return route.fulfill({ json: {
      locationId, contentId: '31000000-0000-0000-0000-000000000001', locationName: 'Survey fixture',
      languageCode: 'EN', scriptType: 'FULL', scriptText: 'Read the approved test fixture.', audioUrl: null,
    } });
    return route.fulfill({ json: { items: [], totalPages: 0 } });
  });
  await page.goto('/offline');
  await page.evaluate(async ({ locationId, names, metadata }) => {
    const pair = await crypto.subtle.generateKey({ name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' }, true, ['sign', 'verify']);
    const digest = async (bytes: ArrayBuffer) => Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)), byte => byte.toString(16).padStart(2, '0')).join('');
    const encode = (bytes: ArrayBuffer) => btoa(String.fromCharCode(...new Uint8Array(bytes)));
    const payload = { schemaVersion: 1, status: 'APPROVED', scope: 'BASE', isBase: true,
      locations: [{ locationId, name: names.VI, categoryId: 'test', categoryName: 'Test', latitude: 10.78, longitude: 106.7, geofenceRadiusM: 100 }],
      guides: [], languages: Object.keys(names), assets: [],
      ...(metadata ? { geofencing: { surveyedAt: '2026-01-01T00:00:00Z', cooldownSeconds: 3600, localizedNames: { [locationId]: names } } } : {}),
    };
    const bytes = new TextEncoder().encode(JSON.stringify(payload)).buffer;
    const key = await crypto.subtle.exportKey('spki', pair.publicKey);
    const sha256 = await digest(bytes);
    const manifest = { schemaVersion: 1, scope: 'BASE', version: `base-${sha256}`, status: 'APPROVED', isBase: true, sizeBytes: bytes.byteLength,
      sha256, signature: encode(await crypto.subtle.sign('RSASSA-PKCS1-v1_5', pair.privateKey, bytes)), keyId: await digest(key), assets: [] };
    await new Promise<void>((resolve, reject) => {
      const request = indexedDB.open('travelvoice-content-v1', 1);
      request.onupgradeneeded = () => request.result.createObjectStore('packages', { keyPath: 'scope' });
      request.onsuccess = () => { const db = request.result; const tx = db.transaction('packages', 'readwrite');
        tx.objectStore('packages').put({ scope: 'BASE', manifest, bytes, key: encode(key), assets: [], installedAt: new Date().toISOString() });
        tx.oncomplete = () => { db.close(); resolve(); }; tx.onerror = () => reject(tx.error);
      };
    });
  }, { locationId, names: localizedNames, metadata });
}
async function move(page: Page, latitude: number, accuracy = 5, timestamp = Date.now()) {
  await page.evaluate(({ latitude, accuracy, timestamp }) => (window as unknown as { move: (lat: number, accuracy: number, timestamp: number) => void }).move(latitude, accuracy, timestamp), { latitude, accuracy, timestamp });
}
for (const language of Object.keys(names)) test(`geofence ${language} banner uses current locale and never autoplays`, async ({ page }) => {
  await setup(page); await page.goto(`/offline?language=${language}`);
  await page.getByRole('button', { name: 'Bật gợi ý theo vị trí', exact: true }).click();
  await expect(page.getByRole('status')).toContainText(['Đang chờ quyền vị trí']);
  await move(page, 10.79); await move(page, 10.78);
  const banner = page.locator('.geofence-banner');
  await expect(banner).toContainText(names[language as keyof typeof names]);
  await expect(banner).toHaveAttribute('lang', language.toLowerCase());
  await expect(banner.getByRole('link')).toHaveAttribute('href', `/locations/${locationId}?language=${language}&scriptType=FULL`);
  if (language === 'EN') {
    await banner.getByRole('link').click();
    await expect(page.locator('.guide-player')).toContainText('Sẵn sàng nghe');
    expect((await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()).violations).toEqual([]);
  }
});
test('cooldown persists across exit/reentry and reload; stale and uncertain GPS do not notify', async ({ page }) => {
  await setup(page);
  await page.getByRole('button', { name: 'Bật gợi ý theo vị trí', exact: true }).click();
  await expect(page.getByRole('status')).toContainText(['Đang chờ quyền vị trí']);
  await move(page, 10.78, 5, Date.now() - 60_000); await expect(page.locator('.geofence-banner')).toHaveCount(0);
  await move(page, 10.78, 200); await expect(page.locator('.geofence-banner')).toHaveCount(0);
  await move(page, 10.78); await expect(page.locator('.geofence-banner')).toBeVisible();
  await page.getByRole('button', { name: 'Đóng gợi ý', exact: true }).click();
  await move(page, 10.79); await move(page, 10.78); await expect(page.locator('.geofence-banner')).toHaveCount(0);
  await page.reload(); await page.getByRole('button', { name: 'Bật gợi ý theo vị trí', exact: true }).click();
  await expect(page.getByRole('status')).toContainText(['Đang chờ quyền vị trí']);
  await move(page, 10.78); await expect(page.locator('.geofence-banner')).toHaveCount(0);
});
test('unsigned survey claim absent from signed demo package cannot activate geofencing', async ({ page }) => {
  await setup(page, false); await page.getByRole('button', { name: 'Bật gợi ý theo vị trí', exact: true }).click();
  await expect(page.getByRole('status')).toContainText(['Chưa có dữ liệu geofence đã khảo sát']);
  await move(page, 10.78); await expect(page.locator('.geofence-banner')).toHaveCount(0);
});
test('permission revoked clears banner; manual navigation remains available', async ({ page }) => {
  await setup(page); await page.getByRole('button', { name: 'Bật gợi ý theo vị trí', exact: true }).click();
  await expect(page.getByRole('status')).toContainText(['Đang chờ quyền vị trí']);
  await page.evaluate(() => (window as unknown as { deny: () => void }).deny());
  await expect(page.getByRole('status')).toContainText(['Quyền vị trí chưa được cấp']);
  await move(page, 10.78); await expect(page.locator('.geofence-banner')).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Khám phá', exact: true })).toBeEnabled();
});
test('cooldown reservation is atomic across callers and expires at configured interval', async ({ page }) => {
  await setup(page);
  const result = await page.evaluate(async locationId => {
    const { reserveNotification } = await import('/src/lib/geofence.ts' as string);
    const now = Date.now();
    const concurrent = await Promise.all([reserveNotification(locationId, 60_000, now), reserveNotification(locationId, 60_000, now)]);
    return { concurrent, before: await reserveNotification(locationId, 60_000, now + 59_999), after: await reserveNotification(locationId, 60_000, now + 60_000) };
  }, locationId);
  expect(result.concurrent.filter(Boolean)).toHaveLength(1); expect(result.before).toBe(false); expect(result.after).toBe(true);
});
test('hidden page reports monitoring limit and requires a new fix on return', async ({ page }) => {
  await setup(page); await page.getByRole('button', { name: 'Bật gợi ý theo vị trí', exact: true }).click();
  await expect(page.getByRole('status')).toContainText(['Đang chờ quyền vị trí']);
  await page.evaluate(() => { Object.defineProperty(document, 'hidden', { configurable: true, value: true }); document.dispatchEvent(new Event('visibilitychange')); });
  await expect(page.getByRole('status')).toContainText(['Trang đang ẩn']);
  await page.evaluate(() => { Object.defineProperty(document, 'hidden', { configurable: true, value: false }); document.dispatchEvent(new Event('visibilitychange')); });
  await expect(page.getByRole('status')).toContainText(['Đang chờ vị trí mới']);
});
test('long localized banner stays readable at supported widths and keyboard opens its guide', async ({ page }, testInfo) => {
  await setup(page, true, { ...names, VI: 'Tên địa điểm khảo sát thử rất dài '.repeat(30) });
  await page.getByRole('button', { name: 'Bật gợi ý theo vị trí', exact: true }).click();
  await expect(page.getByRole('status')).toContainText(['Đang chờ quyền vị trí']);
  await move(page, 10.78); await expect(page.locator('.geofence-banner')).toBeVisible();
  for (const width of [320, 768, 1024, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.locator('.geofence-notices').screenshot({ path: testInfo.outputPath(`geofence-${width}.png`) });
  }
  const link = page.locator('.geofence-banner a'); await link.focus(); await expect(link).toBeFocused();
  await page.keyboard.press('Enter'); await expect(page).toHaveURL(new RegExp(`/locations/${locationId}`));
});
test('fresh hidden callback uses system adapter with localized message and exact locationId', async ({ page }) => {
  await setup(page); await page.goto('/offline?language=EN');
  await page.evaluate(() => {
    const notices: unknown[] = []; Reflect.set(window, 'testNotices', notices);
    Object.defineProperty(window, 'Notification', { configurable: true, value: { permission: 'granted' } });
    navigator.serviceWorker.getRegistration = async () => ({ active: {}, showNotification: async (title: string, options: NotificationOptions) => { notices.push({ title, ...options }); } } as unknown as ServiceWorkerRegistration);
  });
  await page.getByRole('button', { name: 'Bật gợi ý theo vị trí', exact: true }).click();
  await expect(page.getByRole('status')).toContainText(['Đang chờ quyền vị trí']);
  await page.evaluate(() => { Object.defineProperty(document, 'hidden', { configurable: true, value: true }); document.dispatchEvent(new Event('visibilitychange')); });
  await move(page, 10.78);
  await expect.poll(() => page.evaluate(() => Reflect.get(window, 'testNotices'))).toEqual([{
    title: 'A place nearby', body: `You are near ${names.EN}. Open the guide to read or choose to listen.`, lang: 'en',
    tag: `travelvoice-${locationId}`, data: { locationId, language: 'EN' },
  }]);
  await expect(page.locator('.geofence-banner')).toHaveCount(0); await expect(page).toHaveURL(/offline/);
});
test('denied system notification still permits foreground banner', async ({ page }) => {
  await setup(page);
  await page.evaluate(() => Object.defineProperty(window, 'Notification', { configurable: true, value: { permission: 'denied', requestPermission: async () => 'denied' } }));
  await page.getByRole('button', { name: 'Bật gợi ý theo vị trí', exact: true }).click();
  await expect(page.getByRole('status')).toContainText(['Đang chờ quyền vị trí']);
  await page.getByRole('button', { name: 'Cho phép thông báo hệ thống', exact: true }).click();
  await expect(page.getByText('Chưa được cấp quyền thông báo hệ thống. Gợi ý trong trang vẫn dùng được.')).toBeVisible();
  await move(page, 10.78); await expect(page.locator('.geofence-banner')).toBeVisible();
});
