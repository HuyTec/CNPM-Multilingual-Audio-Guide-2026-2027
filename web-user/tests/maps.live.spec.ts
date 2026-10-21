import { expect, test } from '@playwright/test';

test('live OpenStreetMap tiles render with tour markers and OsmAnd handoff', async ({ page }, info) => {
  test.skip(process.env.LIVE_OSM_MAPS !== '1', 'Requires live access to the OSM tile service');
  await page.goto('/?view=map');
  await expect(page.getByRole('heading', { name: 'Bản đồ OpenStreetMap · Quận 1' })).toBeVisible();
  await expect(page.getByText(/Bản đồ đã sẵn sàng/)).toBeVisible({ timeout: 30_000 });
  await expect(page.locator('.leaflet-marker-icon')).toHaveCount(4);
  await page.locator('.tour-map').screenshot({ path: info.outputPath('openstreetmap-map.png') });
  await page.locator('.leaflet-marker-icon').first().click();
  await expect(page.getByRole('link', { name: 'Mở vị trí trong OsmAnd ↗' })).toBeVisible();
});
