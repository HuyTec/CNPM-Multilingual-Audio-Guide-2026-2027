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
    await page.goto('/locations/20000000-0000-0000-0000-000000000101?language=VI&scriptType=SHORT');
    await expect(page.getByRole('img', { name: 'Dinh Độc Lập', exact: true })).toBeVisible();
    expect(await page.locator('.guide-photo').evaluate(image => (image as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
    await page.getByRole('button', { name: 'Nghe thuyết minh', exact: true }).click();
    await expect(page.locator('.guide-player')).toContainText('Đang phát · Audio thu sẵn');
    await page.getByRole('button', { name: 'Tạm dừng', exact: true }).click();
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

  test('real cached WAV creates durable offline receipt and syncs after reload/reconnection', async ({ page, context, request }) => {
    await page.goto('/offline');
    await page.evaluate(async () => { await navigator.serviceWorker.ready; });
    await expect.poll(() => page.evaluate(() => !!navigator.serviceWorker.controller)).toBe(true);
    await page.getByRole('button', { name: 'Kiểm tra cập nhật' }).click();
    await page.getByRole('button', { name: 'Tải gói', exact: true }).nth(1).click();
    await page.getByRole('button', { name: 'Xác nhận tải', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Gói bổ sung', exact: true })).toBeVisible();
    await context.setOffline(true);
    await page.goto('/locations/20000000-0000-0000-0000-000000000001?language=VI&scriptType=SHORT');
    await expect(page.getByText('Đang đọc từ gói offline đã xác minh.', { exact: false })).toBeVisible();
    await page.getByRole('button', { name: 'Nghe thuyết minh', exact: true }).click();
    await expect(page.locator('.guide-player')).toContainText('Đang phát · Audio thu sẵn');
    await expect(page.getByLabel('Đồng bộ lượt nghe')).toContainText('1 lượt nghe đã lưu');
    const queued = await page.evaluate(async () => (await (await import('/playback-outbox.js' as string)).listReceipts())[0]);
    expect(queued.offline).toBe(true);
    expect(queued.contentId).toBe('30000000-0000-0000-0000-000000000001');
    await page.reload();
    await expect(page.getByLabel('Đồng bộ lượt nghe')).toContainText('1 lượt nghe đã lưu');
    await context.setOffline(false);
    await expect.poll(() => page.evaluate(async () => (await (await import('/playback-outbox.js' as string)).listReceipts()).length)).toBe(0);
    const duplicate = await request.post('/api/playbacks', { data: { eventId: queued.eventId, contentId: queued.contentId, occurredAt: queued.occurredAt, offline: queued.offline } });
    expect(duplicate.ok()).toBe(true); expect((await duplicate.json()).created).toBe(false);
    await expect(page.locator('.guide-player')).toContainText('Sẵn sàng nghe');
  });

  test.describe('Intercepted transfer verification', () => {
    // Chromium may bypass request interception for fetches delegated to a controlling worker.
    // The separate reopen test above exercises the actual production worker.
    test.use({ serviceWorkers: 'block' });
    test('tampered signed bytes rejected without installation; optional audio verified and removable', async ({ page, context }) => {
      await page.goto('/offline');
      await page.getByRole('button', { name: 'Kiểm tra cập nhật' }).click();
      let tampered = 0;
      const packageUrl = (url: URL) => url.pathname.startsWith('/api/content-packages/BASE/');
      await context.route(packageUrl, async route => {
        const response = await route.fetch(); const bytes = Buffer.from(await response.body());
        bytes[bytes.length - 2] ^= 1;
        tampered++;
        await route.fulfill({ response, body: bytes });
      });
      await page.getByRole('button', { name: 'Tải gói', exact: true }).first().click();
      await page.getByRole('button', { name: 'Xác nhận tải', exact: true }).click();
      await expect.poll(() => tampered).toBe(1);
      await expect(page.getByRole('status')).toContainText('Checksum');
      await expect(page.getByText('Chưa có gói đã xác minh.')).toBeVisible();
      await context.unroute(packageUrl);
      await page.getByRole('button', { name: 'Tải gói', exact: true }).nth(1).click();
      await page.getByRole('button', { name: 'Xác nhận tải', exact: true }).click();
      await expect(page.getByRole('heading', { name: 'Gói bổ sung', exact: true })).toBeVisible();
      page.once('dialog', dialog => dialog.accept());
      await page.getByRole('button', { name: 'Xóa nội dung bổ sung' }).click();
      await expect(page.getByText('Chưa có gói đã xác minh.')).toBeVisible();
    });
  });
});
