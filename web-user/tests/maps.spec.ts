import { expect, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

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
  await page.route('**/api/categories', route => route.fulfill({ json: [
    { categoryId: locations[0].categoryId, name: 'Tuyến Quận 1 (demo)', locationCount: 7 },
    { categoryId: '10000000-0000-0000-0000-000000000102', name: 'Danh mục rỗng', locationCount: 0 },
  ] }));
  await page.route('**/api/locations?**', async route => {
    expect(Number(new URL(route.request().url()).searchParams.get('size'))).toBeLessThanOrEqual(50);
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

test('category browsing retains URL state and offers recovery for an empty category', async ({ page }) => {
  await page.route('**/api/locations?**', route => {
    const empty = new URL(route.request().url()).searchParams.get('categoryId')?.endsWith('102');
    return route.fulfill({ json: { items: empty ? [] : locations, totalPages: empty ? 0 : 1 } });
  });
  await page.goto('/?panel=locations&language=JA&radius=500');
  const category = page.getByRole('combobox', { name: 'Danh mục', exact: true });
  await expect(category.getByRole('option', { name: 'Danh mục rỗng (0)' })).toHaveCount(1);
  await category.selectOption('10000000-0000-0000-0000-000000000102');
  await expect(page.locator('.location-drawer')).toContainText('Chưa tìm thấy địa điểm');
  await expect(page).toHaveURL(/categoryId=/);
  await expect(page.getByRole('combobox', { name: 'Ngôn ngữ' })).toHaveValue('JA');
  await expect(page.getByRole('slider')).toHaveValue('500');
  await category.selectOption('');
  await expect(page.locator('.guide-map-icon')).toHaveCount(7);
});

test('nearby shortlist starts with five nearest and expands without removing map markers', async ({ page, context }) => {
  const close = locations.map((item, index) => ({ ...item, latitude: 10.777 + index * 0.00005, longitude: 106.6953 }));
  await page.route('**/api/locations?**', route => route.fulfill({ json: { items: close.reverse(), totalPages: 1 } }));
  await context.grantPermissions(['geolocation']);
  await context.setGeolocation({ latitude: 10.777, longitude: 106.6953, accuracy: 5 });
  await page.goto('/');
  await page.getByRole('button', { name: 'Lấy vị trí hiện tại', exact: true }).click();
  const list = page.getByRole('list', { name: 'Địa điểm gần bạn', exact: true });
  await expect(list.getByRole('listitem')).toHaveCount(5);
  await page.getByRole('button', { name: 'Xem tất cả địa điểm trong phạm vi' }).click();
  await expect(list.getByRole('listitem')).toHaveCount(7);
  await expect(page.locator('.guide-map-icon')).toHaveCount(7);
});

test('OSM map shows all seven tour markers with guide and OsmAnd links', async ({ page }) => {
  await page.goto('/?view=map&language=JA&scriptType=SHORT');
  await expect(page.getByRole('heading', { name: 'Bản đồ OpenStreetMap · Quận 1 & Quận 3' })).toBeVisible();
  await expect(page.getByText(/Bản đồ đã sẵn sàng/)).toBeVisible();
  await expect(page.locator('.leaflet-marker-icon')).toHaveCount(7);
  await page.locator('.leaflet-marker-icon').first().click();
  const mapRegion = page.getByRole('region', { name: 'Bản đồ OpenStreetMap bao phủ Quận 1 và Quận 3' });
  await expect(mapRegion.getByText('Dinh Độc Lập', { exact: true })).toBeVisible();
  await expect(mapRegion.getByRole('link', { name: 'Mở bài thuyết minh' })).toHaveAttribute('href', /language=JA&scriptType=SHORT/);
  await expect(mapRegion.getByRole('link', { name: 'Mở vị trí trong OsmAnd ↗' }))
    .toHaveAttribute('href', 'https://osmand.net/map/?pin=10.777,106.6953#16/10.777/106.6953');
  await expect(page.locator('.leaflet-control-attribution')).toContainText('OpenStreetMap');
  for (const name of ['Bảo tàng Chứng tích Chiến tranh', 'Trường Đại học Sài Gòn - Cơ sở 2', 'Bảo tàng Bến Nhà Rồng']) {
    const matchingMarker = page.locator('.guide-map-icon').and(page.getByTitle(name, { exact: true }));
    await matchingMarker.click();
    await expect(mapRegion.getByText(name, { exact: true })).toBeVisible();
  }
});

test('GPS is requested only after clicking and displays a local position marker', async ({ page, context }) => {
  await context.grantPermissions(['geolocation']);
  await context.setGeolocation({ latitude: 10.7823, longitude: 106.6842 });
  await page.addInitScript(() => {
    const original = navigator.geolocation.getCurrentPosition.bind(navigator.geolocation);
    Reflect.set(window, 'gpsRequests', 0);
    navigator.geolocation.getCurrentPosition = (...args: Parameters<Geolocation['getCurrentPosition']>) => {
      Reflect.set(window, 'gpsRequests', Number(Reflect.get(window, 'gpsRequests')) + 1);
      original(...args);
    };
  });
  await page.goto('/?view=map');
  await expect(page.locator('.guide-map-icon')).toHaveCount(7);
  expect(await page.evaluate(() => Reflect.get(window, 'gpsRequests'))).toBe(0);
  await page.getByRole('button', { name: 'Định vị vị trí hiện tại' }).click();
  await expect(page.getByText('Đã hiển thị vị trí của bạn trên bản đồ.')).toBeVisible();
  await expect(page.locator('.user-location-icon')).toBeVisible();
  expect(await page.evaluate(() => Reflect.get(window, 'gpsRequests'))).toBe(1);
});

test('search filters both map and drawer without hiding the map', async ({ page }) => {
  await page.goto('/?view=map&keyword=Ch%E1%BB%A9ng%20t%C3%ADch');
  await expect(page.locator('.guide-map-icon')).toHaveCount(1);
  await page.getByRole('button', { name: 'Danh sách', exact: true }).click();
  await expect(page.getByRole('link', { name: /Bảo tàng Chứng tích Chiến tranh/ })).toBeVisible();
  await page.getByRole('button', { name: 'Đóng danh sách' }).press('Escape');
  await expect(page.getByRole('button', { name: 'Danh sách', exact: true })).toBeFocused();
  await page.getByRole('searchbox').fill('Không tồn tại');
  await page.getByRole('button', { name: 'Tìm kiếm', exact: true }).click();
  await expect(page.locator('.guide-map-icon')).toHaveCount(0);
  await expect(page.locator('.tour-map')).toBeVisible();
  await expect(page.locator('.map-status')).toContainText('Chưa tìm thấy địa điểm.');
});

test('nearby results include later API pages, sort by distance and respond to keyboard radius changes', async ({ page, context }) => {
  const sample = [
    { ...locations[0], name: 'Điểm xa', latitude: 10.779, longitude: 106.6953 },
    { ...locations[1], name: 'Điểm khoảng 78 mét', latitude: 10.7777, longitude: 106.6953 },
    { ...locations[2], name: 'Điểm khoảng 22 mét', latitude: 10.7772, longitude: 106.6953 },
  ];
  const requests: string[] = [];
  await page.route('**/api/locations?**', route => {
    requests.push(route.request().url());
    const pageNumber = Number(new URL(route.request().url()).searchParams.get('page'));
    return route.fulfill({ json: { items: pageNumber === 0 ? sample.slice(0, 2) : sample.slice(2), totalPages: 2 } });
  });
  await context.grantPermissions(['geolocation']);
  await context.setGeolocation({ latitude: 10.777, longitude: 106.6953, accuracy: 5 });
  await page.goto('/');
  await expect(page.locator('.user-location-icon')).toHaveCount(0);
  await page.getByRole('button', { name: 'Định vị vị trí hiện tại' }).click();
  const nearby = page.getByRole('list', { name: 'Địa điểm gần bạn', exact: true });
  await expect(nearby.getByRole('listitem')).toHaveCount(2);
  await expect(nearby.getByRole('heading')).toHaveText(['Điểm khoảng 22 mét', 'Điểm khoảng 78 mét']);
  const slider = page.getByRole('slider', { name: /Phạm vi quét/ });
  await slider.focus(); await slider.press('Home');
  await expect(slider).toHaveValue('50');
  await expect(nearby.getByRole('listitem')).toHaveCount(1);
  await expect(page).toHaveURL(/radius=50/);
  await slider.press('ArrowRight'); await expect(slider).toHaveValue('51');
  await slider.press('End'); await expect(slider).toHaveValue('500');
  await expect(nearby.getByRole('listitem')).toHaveCount(3);
  await expect(nearby.getByRole('heading')).toHaveText(['Điểm khoảng 22 mét', 'Điểm khoảng 78 mét', 'Điểm xa']);
  await page.getByRole('combobox', { name: 'Ngôn ngữ' }).selectOption('JA');
  await page.getByRole('button', { name: 'Danh sách', exact: true }).click();
  await page.getByRole('button', { name: 'Đóng danh sách' }).click();
  await expect(page.locator('.user-location-icon')).toHaveCount(1);
  await expect(nearby.getByRole('link').first()).toHaveAttribute('href', /language=JA/);
  expect(requests.every(url => !/latitude|longitude|accuracy/.test(url))).toBe(true);
  await context.setGeolocation({ latitude: 10.8, longitude: 106.72, accuracy: 600 });
  await page.getByRole('button', { name: 'Cập nhật vị trí', exact: true }).click();
  await expect(page.getByText(/Chưa có địa điểm trong phạm vi 500 m/)).toBeVisible();
  await expect(page.getByText(/Độ chính xác GPS lớn hơn/)).toBeVisible();
});

test('denied GPS keeps manual discovery usable', async ({ page, context }) => {
  await context.clearPermissions();
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'geolocation', { value: { getCurrentPosition: (_success: unknown, failure: (error: { code: number }) => void) => failure({ code: 1 }) } });
  });
  await page.goto('/');
  await page.getByRole('button', { name: 'Lấy vị trí hiện tại', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Bạn chưa cấp quyền vị trí');
  await expect(page.locator('.user-location-icon')).toHaveCount(0);
  await page.getByRole('button', { name: 'Danh sách', exact: true }).click();
  await expect(page.getByRole('list', { name: 'Danh sách địa điểm', exact: true }).getByRole('listitem')).toHaveCount(7);
});

test('backend error can be retried while map and GPS controls remain available', async ({ page }) => {
  await page.route('**/api/locations?**', route => route.fulfill({ status: 503 }));
  await page.goto('/');
  await expect(page.getByRole('alert')).toContainText('Chưa tải được địa điểm');
  await expect(page.getByRole('button', { name: 'Định vị vị trí hiện tại' })).toBeEnabled();
  await page.unroute('**/api/locations?**');
  await page.route('**/api/locations?**', route => route.fulfill({ json: { items: locations, totalPages: 1 } }));
  await page.getByRole('button', { name: 'Thử tải lại địa điểm' }).click();
  await expect(page.locator('.guide-map-icon')).toHaveCount(7);
});

test('tile failure retains list access and reload recovers the map', async ({ page }) => {
  await page.route('https://tile.openstreetmap.org/**', route => route.fulfill({ status: 503 }));
  await page.clock.install();
  await page.goto('/');
  await expect(page.locator('.guide-map-icon')).toHaveCount(7);
  await expect(page.locator('.map-status')).toContainText('Đang tải bản đồ…');
  await page.clock.fastForward(16_000);
  await expect(page.getByRole('button', { name: 'Tải lại bản đồ' })).toBeVisible();
  await page.getByRole('button', { name: 'Danh sách', exact: true }).click();
  await expect(page.getByRole('list', { name: 'Danh sách địa điểm', exact: true }).getByRole('listitem')).toHaveCount(7);
  await page.route('https://tile.openstreetmap.org/**', route => route.fulfill({ contentType: 'image/svg+xml', body: '<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256"></svg>' }));
  await page.getByRole('button', { name: 'Tải lại bản đồ' }).click();
  await expect(page.locator('.map-status')).toContainText('Bản đồ đã sẵn sàng');
});

test('responsive map, long names, keyboard drawer and accessibility', async ({ page }, info) => {
  const longName = 'Bảo tàng có tên rất dài để kiểm tra bố cục và khả năng xuống dòng '.repeat(8);
  await page.route('**/api/locations?**', route => route.fulfill({ json: { items: [{ ...locations[0], name: longName }], totalPages: 1 } }));
  await page.goto('/');
  for (const width of [320, 768, 1024, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    await page.getByRole('button', { name: 'Danh sách', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Đóng danh sách' })).toBeFocused();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    await page.screenshot({ path: info.outputPath(`map-${width}.png`), fullPage: true });
    await page.getByRole('button', { name: 'Đóng danh sách' }).press('Escape');
  }
  const audit = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
  expect(audit.violations).toEqual([]);
});
