import { expect, test } from '@playwright/test';

test.describe('Real signed package / production service worker', () => {
  test.skip(process.env.LIVE_GUIDE_API !== '1', 'Requires real backend and production preview');
  test('stored audio corruption is rejected before reporting a package verified', async ({ page }) => {
    await page.goto('/offline');
    await page.getByRole('button', { name: 'Kiểm tra cập nhật' }).click();
    await page.getByRole('button', { name: 'Tải gói', exact: true }).nth(1).click();
    await page.getByRole('button', { name: 'Xác nhận tải', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Gói bổ sung', exact: true })).toBeVisible();
    await page.evaluate(() => new Promise<void>((resolve, reject) => {
      const open = indexedDB.open('travelvoice-content-v1', 1);
      open.onerror = () => reject(open.error);
      open.onsuccess = () => {
        const db = open.result; const tx = db.transaction('packages', 'readwrite');
        const store = tx.objectStore('packages'); const read = store.get('OPTIONAL');
        read.onsuccess = () => {
          const stored = read.result;
          new Uint8Array(stored.assets[0].bytes)[0] ^= 1;
          store.put(stored);
        };
        tx.oncomplete = () => { db.close(); resolve(); };
        tx.onerror = () => { db.close(); reject(tx.error); };
      };
    }));
    await page.reload();
    await expect(page.getByRole('status')).toContainText('Chưa đọc/xác minh được bộ nhớ');
    await expect(page.getByRole('heading', { name: 'Gói bổ sung', exact: true })).toHaveCount(0);
  });
  test('confirm download, verify, protect base and reopen actual guide offline', async ({ page, context, request }) => {
    page.on('pageerror', error => console.log('Offline page error:', error.message));
    page.on('requestfailed', request => console.log('Offline request failed:', request.url(), request.failure()?.errorText));
    const available = await request.get('/api/content-packages'); expect(available.status()).toBe(200);
    const manifests = await available.json() as { scope: string; version: string }[];
    expect(manifests.map(item => item.scope)).toEqual(['BASE', 'OPTIONAL']);
    await page.goto('/offline');
    await page.evaluate(async () => { await navigator.serviceWorker.ready; });
    await expect.poll(() => page.evaluate(() => !!navigator.serviceWorker.controller)).toBe(true);
    await page.getByRole('button', { name: 'Kiểm tra cập nhật' }).click();
    await page.getByRole('button', { name: 'Tải gói', exact: true }).first().click();
    await expect(page.getByRole('heading', { name: 'Xác nhận tải nội dung?' })).toBeVisible();
    await page.getByRole('button', { name: 'Hủy', exact: true }).click();
    await expect(page.getByText('Chưa có gói đã xác minh.')).toBeVisible();
    await page.getByRole('button', { name: 'Tải gói', exact: true }).first().click();
    await page.getByRole('button', { name: 'Xác nhận tải', exact: true }).click();
    await expect(page.getByText('Gói cơ bản · được bảo vệ')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Xóa nội dung bổ sung' })).toHaveCount(0);
    console.log('Shell cache:', await page.evaluate(async () => {
      const names = await caches.keys();
      return Promise.all(names.map(async name => ({ name, keys: (await (await caches.open(name)).keys()).map(request => request.url) })));
    }));
    await context.setOffline(true);
    await page.goto('/locations/20000000-0000-0000-0000-000000000101?language=JA&scriptType=SHORT');
    await expect(page.locator('.script-text')).not.toBeEmpty();
    await expect(page.locator('.script-text')).toHaveAttribute('lang', 'ja');
    await expect(page.getByText('Đang đọc từ gói offline đã xác minh.', { exact: false })).toBeVisible();
    await page.reload();
    await expect(page.locator('.script-text')).toBeVisible();
    await page.getByRole('link', { name: 'Khám phá', exact: true }).click();
    await page.getByRole('button', { name: 'Danh sách', exact: true }).click();
    await expect(page.getByRole('list', { name: 'Danh sách địa điểm' }).getByRole('listitem')).toHaveCount(7);
    await context.setOffline(false);
  });

  test('tampered signed bytes rejected without installation; optional audio verified and removable', async ({ page }) => {
    await page.goto('/offline');
    await page.getByRole('button', { name: 'Kiểm tra cập nhật' }).click();
    await page.route('**/api/content-packages/BASE/base-*', async route => {
      const response = await route.fetch(); const bytes = Buffer.from(await response.body());
      bytes[bytes.length - 2] ^= 1;
      await route.fulfill({ response, body: bytes });
    });
    await page.getByRole('button', { name: 'Tải gói', exact: true }).first().click();
    await page.getByRole('button', { name: 'Xác nhận tải', exact: true }).click();
    await expect(page.getByRole('status')).toContainText('Checksum');
    await expect(page.getByText('Chưa có gói đã xác minh.')).toBeVisible();
    await page.unroute('**/api/content-packages/BASE/base-*');
    await page.getByRole('button', { name: 'Tải gói', exact: true }).nth(1).click();
    await page.getByRole('button', { name: 'Xác nhận tải', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Gói bổ sung', exact: true })).toBeVisible();
    page.once('dialog', dialog => dialog.accept());
    await page.getByRole('button', { name: 'Xóa nội dung bổ sung' }).click();
    await expect(page.getByText('Chưa có gói đã xác minh.')).toBeVisible();
  });
});
