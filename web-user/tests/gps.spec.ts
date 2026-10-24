import { expect, test } from '@playwright/test';

test.beforeEach(async ({ page, context }) => {
  await context.grantPermissions(['geolocation']);
  await page.route('**/api/categories', route => route.fulfill({ json: [] }));
  await page.route('**/api/locations?**', route => route.fulfill({ json: { items: [], totalPages: 0 } }));
  await page.route('https://tile.openstreetmap.org/**', route => route.fulfill({ contentType: 'image/svg+xml', body: '<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"></svg>' }));
});

test('coarse fix appears after My Location and survives precise GPS timeout', async ({ page }) => {
  await page.addInitScript(() => {
    const calls: PositionOptions[] = [];
    Reflect.set(window, 'geoCalls', calls);
    Object.defineProperty(navigator, 'geolocation', { value: {
      getCurrentPosition: (ok: (value: unknown) => void, fail: (value: { code: number }) => void, options: PositionOptions) => {
        calls.push(options);
        if (options.enableHighAccuracy) fail({ code: 3 });
        else ok({ coords: { latitude: 10.777, longitude: 106.6953, accuracy: 180 } });
      },
    } });
  });
  await page.goto('/');
  await page.getByRole('button', { name: 'Định vị vị trí hiện tại' }).click();
  await expect(page.locator('.user-location-icon')).toHaveCount(1);
  await expect(page.getByText(/Độ chính xác GPS lớn hơn/)).toBeVisible();
  await expect(page.getByText('Chưa lấy được vị trí sau khi thử lại.', { exact: false })).toHaveCount(0);
  const calls = await page.evaluate(() => Reflect.get(window, 'geoCalls')) as PositionOptions[];
  expect(calls).toHaveLength(2);
  expect(calls[0]).toMatchObject({ enableHighAccuracy: false, maximumAge: 0 });
  expect(calls[1]).toMatchObject({ enableHighAccuracy: true, timeout: 45_000 });
});

test('a slow first fix retries with longer precise GPS rather than failing after fifteen seconds', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'geolocation', { value: {
      getCurrentPosition: (ok: (value: unknown) => void, fail: (value: { code: number }) => void, options: PositionOptions) => {
        if (!options.enableHighAccuracy) fail({ code: 3 });
        else ok({ coords: { latitude: 10.777, longitude: 106.6953, accuracy: 8 } });
      },
    } });
  });
  await page.goto('/');
  await page.getByRole('button', { name: 'Định vị vị trí hiện tại' }).click();
  await expect(page.locator('.user-location-icon')).toHaveCount(1);
  await expect(page.locator('.gps-accuracy')).toContainText('±8 m');
});

test('a denied permission does not loop permission requests or block manual search', async ({ page }) => {
  await page.addInitScript(() => {
    Reflect.set(window, 'geoCalls', 0);
    Object.defineProperty(navigator.permissions, 'query', { value: () => Promise.resolve({ state: 'denied' }) });
    Object.defineProperty(navigator, 'geolocation', { value: { getCurrentPosition: (_ok: unknown, fail: (value: { code: number }) => void) => { Reflect.set(window, 'geoCalls', Number(Reflect.get(window, 'geoCalls')) + 1); fail({ code: 1 }); } } });
  });
  await page.goto('/');
  await page.getByRole('button', { name: 'Định vị vị trí hiện tại' }).click();
  await expect(page.getByRole('alert')).toContainText('Trình duyệt chưa được cấp quyền vị trí');
  expect(await page.evaluate(() => Reflect.get(window, 'geoCalls'))).toBe(1);
  await expect(page.getByRole('combobox', { name: 'Tìm địa điểm' })).toBeEnabled();
});

test('missing Permissions API still allows browser geolocation and both timeouts show actionable help', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'permissions', { value: undefined });
    Object.defineProperty(navigator, 'geolocation', { value: { getCurrentPosition: (_ok: unknown, fail: (value: { code: number }) => void) => fail({ code: 3 }) } });
  });
  await page.goto('/');
  await page.getByRole('button', { name: 'Định vị vị trí hiện tại' }).click();
  await expect(page.getByRole('alert')).toContainText('Safari/Chrome');
  await expect(page.locator('.user-location-icon')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Định vị vị trí hiện tại' })).toBeEnabled();
});
