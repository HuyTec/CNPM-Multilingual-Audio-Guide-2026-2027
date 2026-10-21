import { expect, test } from '@playwright/test';

const locations = [
  { locationId: '20000000-0000-0000-0000-000000000101', name: 'Dinh Độc Lập', latitude: 10.777, longitude: 106.6953 },
  { locationId: '20000000-0000-0000-0000-000000000102', name: 'Nhà hát Thành phố Hồ Chí Minh', latitude: 10.7766, longitude: 106.7031 },
  { locationId: '20000000-0000-0000-0000-000000000103', name: 'Nhà thờ Đức Bà', latitude: 10.7798, longitude: 106.699 },
  { locationId: '20000000-0000-0000-0000-000000000104', name: 'Trường Đại học Sài Gòn - Cơ sở 2', latitude: 10.787, longitude: 106.7058 },
  { locationId: '20000000-0000-0000-0000-000000000105', name: 'Bảo tàng Chứng tích Chiến tranh', latitude: 10.7795, longitude: 106.6919 },
  { locationId: '20000000-0000-0000-0000-000000000106', name: 'Bảo tàng Bến Nhà Rồng', latitude: 10.7682, longitude: 106.7069 },
  { locationId: '20000000-0000-0000-0000-000000000107', name: 'Tòa nhà Bitexco Financial Tower', latitude: 10.7716, longitude: 106.7044 },
].map(item => ({ ...item, categoryId: '10000000-0000-0000-0000-000000000101', categoryName: 'Tuyến Quận 1 (demo)', geofenceRadiusM: 100 }));

test.beforeEach(async ({ page }) => {
  await page.route('**/api/locations?**', async route => {
    const keyword = new URL(route.request().url()).searchParams.get('keyword')?.toLocaleLowerCase('vi-VN') || '';
    const items = locations.filter(item => item.name.toLocaleLowerCase('vi-VN').includes(keyword));
    await route.fulfill({ json: { items, totalElements: items.length, totalPages: items.length ? 1 : 0, page: 0 } });
  });
  await page.route('https://tile.openstreetmap.org/**', route => route.fulfill({
    status: 200,
    contentType: 'image/svg+xml',
    body: '<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"></svg>',
  }));
});

test('OSM map shows four tour markers with guide and OsmAnd links', async ({ page }) => {
  await page.goto('/?view=map&language=JA&scriptType=SHORT');
  await expect(page.getByRole('heading', { name: 'Bản đồ OpenStreetMap · Quận 1' })).toBeVisible();
  await expect(page.getByText(/Bản đồ đã sẵn sàng/)).toBeVisible();
  await expect(page.locator('.leaflet-marker-icon')).toHaveCount(4);
  await page.locator('.leaflet-marker-icon').first().click();
  const mapRegion = page.getByRole('region', { name: 'Bản đồ OpenStreetMap với địa điểm thuyết minh Quận 1' });
  await expect(mapRegion.getByText('Dinh Độc Lập', { exact: true })).toBeVisible();
  await expect(mapRegion.getByRole('link', { name: 'Mở bài thuyết minh' })).toHaveAttribute('href', /language=JA&scriptType=SHORT/);
  await expect(mapRegion.getByRole('link', { name: 'Mở vị trí trong OsmAnd ↗' }))
    .toHaveAttribute('href', 'https://osmand.net/map/?pin=10.777,106.6953#16/10.777/106.6953');
  await expect(page.locator('.leaflet-control-attribution')).toContainText('OpenStreetMap');
});

test('locations outside the District 1 tour stay selectable without an in-scope map', async ({ page }) => {
  await page.goto('/?view=map&keyword=Ch%E1%BB%A9ng%20t%C3%ADch');
  await expect(page.getByText('Không có điểm thuộc tuyến Quận 1', { exact: false })).toBeVisible();
  await expect(page.locator('.tour-map')).toHaveCount(0);
  await expect(page.getByRole('link', { name: /Bảo tàng Chứng tích Chiến tranh/ })).toBeVisible();
});
