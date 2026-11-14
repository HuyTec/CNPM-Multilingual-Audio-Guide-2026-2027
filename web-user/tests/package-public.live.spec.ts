import { test, expect } from '@playwright/test';

test.skip(!process.env.PUBLIC_PACKAGE_URL, 'Requires an explicitly supplied public demo URL with base content');
test('public base content downloads into the browser and its narration opens offline', async ({ page, context }, info) => {
  test.setTimeout(180000);
  const origin = process.env.PUBLIC_PACKAGE_URL!;
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`${origin}/offline`);
  await page.evaluate(async () => { await navigator.serviceWorker.ready; });
  await expect.poll(() => page.evaluate(() => !!navigator.serviceWorker.controller)).toBe(true);
  await page.getByRole('button', { name: 'Kiểm tra cập nhật', exact: true }).click();
  const card = page.locator('.package-list').first().getByRole('listitem').filter({ has: page.getByRole('heading', { name: 'Nội dung có thể tải ngoại tuyến', exact: true }) });
  await card.getByRole('button', { name: 'Tải gói', exact: true }).click();
  await expect(page.getByRole('dialog', { name: 'Xác nhận tải nội dung?' })).toBeVisible();
  await page.screenshot({ path: info.outputPath('confirm-download-mobile.png'), fullPage: true });
  await page.getByRole('button', { name: 'Xác nhận tải', exact: true }).click();
  await expect(page.getByText(/Đã cài nội dung được xác minh/)).toBeVisible({ timeout: 90000 });
  await page.getByText(/Xem \d+ địa điểm trong gói/).click();
  await page.screenshot({ path: info.outputPath('base-content-installed.png'), fullPage: true });
  await page.locator('.installed-places').getByRole('link').first().click();
  const narration = page.locator('.script-text');
  await expect(narration).toHaveText(/\S/);
  const script = await narration.innerText();
  await context.setOffline(true);
  await page.reload();
  await expect(narration).toHaveText(script);
  await expect(page.getByText(/Đang đọc từ gói offline đã xác minh/)).toBeVisible();
  await page.screenshot({ path: info.outputPath('base-reader-offline.png'), fullPage: true });
});
