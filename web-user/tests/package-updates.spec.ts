import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('manual checks keep receiving polling updates and download confirmation stays visible', async ({ page }) => {
  let version = 'web-first';
  await page.route('**/api/content-packages?client=web', route => route.fulfill({ json: [{
    schemaVersion: 1, scope: 'WEB', version, status: 'APPROVED', isBase: true,
    sizeBytes: 1024, sha256: 'a'.repeat(64), signature: '', keyId: '', downloadUrl: `/api/content-packages/WEB/${version}`, assets: [],
  }], headers: { ETag: `"${version}"` } }));
  await page.route('**/api/content-packages/WEB/*/preview', route => route.fulfill({
    json: { locations: 1, guides: 1, audioFiles: 0, changes: null },
  }));
  await page.setViewportSize({ width: 390, height: 700 });
  await page.goto('/offline');
  await page.getByRole('button', { name: 'Kiểm tra cập nhật' }).click();
  await expect(page.getByText('web-first', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Tải gói', exact: true })).toHaveCount(1);
  version = 'web-new-point-a';
  await page.evaluate(() => window.dispatchEvent(new Event('online')));
  await expect(page.getByText('web-new-point-a', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Tải gói', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Xác nhận tải nội dung?' });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole('button', { name: 'Hủy', exact: true })).toBeFocused();
  const box = await dialog.boundingBox();
  expect(box!.y).toBeGreaterThanOrEqual(0);
  expect(box!.y + box!.height).toBeLessThanOrEqual(700);
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  await page.keyboard.press('Escape');
  await expect(dialog).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Tải gói', exact: true })).toBeFocused();
});

test('unavailable preview still allows confirmation and retries preserve integrity checks', async ({ page }) => {
  let attempts = 0;
  const version = `web-${'a'.repeat(64)}`;
  await page.route('**/api/content-packages?client=web', route => route.fulfill({ json: [{ schemaVersion: 1, scope: 'WEB', version, status: 'APPROVED', isBase: true, sizeBytes: 1024, sha256: 'a'.repeat(64), signature: '', keyId: '', downloadUrl: `/api/content-packages/WEB/${version}`, assets: [] }] }));
  await page.route(`**/api/content-packages/WEB/${version}/preview`, route => route.fulfill({ status: 404 }));
  await page.route('**/api/content-packages/key', route => route.fulfill({ json: { spki: '' } }));
  await page.route(`**/api/content-packages/WEB/${version}`, route => {
    attempts++;
    return attempts < 3 ? route.abort('failed') : route.fulfill({ body: '{}', contentType: 'application/json' });
  });
  await page.goto('/offline');
  await page.getByRole('button', { name: 'Tải gói', exact: true }).click();
  await expect(page.getByRole('dialog')).toContainText('Bạn vẫn có thể xác nhận tải.');
  expect(attempts).toBe(0);
  await page.getByRole('button', { name: 'Xác nhận tải', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Checksum của gói không khớp.');
  expect(attempts).toBe(3);
  await expect(page.getByText('Chưa có gói đã xác minh.', { exact: true })).toBeVisible();
});
