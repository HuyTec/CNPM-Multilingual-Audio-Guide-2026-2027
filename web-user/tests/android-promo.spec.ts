import { expect, test } from '@playwright/test';

test('Android QR and download use the public release URL on localhost', async ({ page }) => {
  const publicUrl = 'https://travelvoice.trycloudflare.com/downloads/travelvoice.apk?v=abc123';
  await page.route('**/downloads/android-release.json', (route) =>
    route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        url: '/downloads/travelvoice.apk?v=abc123',
        publicUrl,
        version: '1.1.0-dev',
        sizeBytes: 1024,
        sha256: 'a'.repeat(64),
      }),
    }),
  );

  await page.goto('/');
  await expect(page.getByRole('img', { name: 'Mã QR tải ứng dụng TravelVoice cho Android' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Tải ứng dụng Android' })).toHaveAttribute('href', publicUrl);
  await expect(page.locator('.android-qr-host')).toHaveText('travelvoice.trycloudflare.com');
});
