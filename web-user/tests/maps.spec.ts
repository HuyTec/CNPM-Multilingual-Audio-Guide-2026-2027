import { expect, test } from '@playwright/test';

// Only the Google adapter is simulated. These checks do not prove Google tiles/API credentials.
const sdk = `
window.google={maps:{Map:class{constructor(host,options){this.host=host;window.mapOptions=options}},
 marker:{AdvancedMarkerElement:class extends HTMLElement{constructor(options){super();options.map.host.append(this)} }},
 event:{addListenerOnce(map,event,cb){setTimeout(cb,0);return{}},removeListener(){},clearInstanceListeners(){}}}};
customElements.define('test-google-marker',window.google.maps.marker.AdvancedMarkerElement);
window.travelVoiceGoogleMapsReady();`;

test('fixed camera, strict bounds, four in-scope markers and shared guide links', async ({ page }) => {
  await page.route('https://maps.googleapis.com/maps/api/js?**', route => route.fulfill({ contentType: 'application/javascript', body: sdk }));
  await page.goto('/?view=map&language=JA&scriptType=SHORT');
  await expect(page.getByText('Bản đồ đã sẵn sàng.', { exact: true })).toBeVisible();
  await expect(page.locator('.map-marker')).toHaveCount(4);
  const options = await page.evaluate(() => (window as unknown as { mapOptions: unknown }).mapOptions);
  expect(options).toMatchObject({ center: { lat: 10.777, lng: 106.701 }, zoom: 15, minZoom: 14, maxZoom: 19,
    restriction: { strictBounds: true, latLngBounds: { north: 10.800, south: 10.757, west: 106.685, east: 106.714 } } });
  await expect(page.locator('.map-marker[aria-label*="Chứng tích"]')).toHaveCount(0);
  await expect(page.getByRole('list', { name: 'Danh sách địa điểm' }).getByRole('listitem')).toHaveCount(7);
  await page.getByRole('link', { name: 'Mở Dinh Độc Lập', exact: true }).click();
  await expect(page).toHaveURL(/000101\?language=JA&scriptType=SHORT$/);
  await expect(page.locator('.script-text')).toHaveAttribute('lang', 'ja');
});

test('SDK network failure keeps the real location list usable', async ({ page }) => {
  await page.route('https://maps.googleapis.com/maps/api/js?**', route => route.abort());
  await page.goto('/?view=map');
  await expect(page.getByRole('alert')).toContainText('Google Maps chưa tải được');
  await page.getByRole('list', { name: 'Danh sách địa điểm' }).getByRole('link', { name: /Dinh Độc Lập/ }).click();
  await expect(page.locator('.script-text')).toBeVisible();
});

test('authorization failure is never reported as a ready map', async ({ page }) => {
  await page.route('https://maps.googleapis.com/maps/api/js?**', route => route.fulfill({ contentType: 'application/javascript', body: 'window.gm_authFailure();' }));
  await page.goto('/?view=map');
  await expect(page.getByRole('alert')).toContainText('Google Maps chưa được cấp quyền');
  await expect(page.getByText('Bản đồ đã sẵn sàng.', { exact: true })).toHaveCount(0);
});

test('out-of-scope search has no map markers or SDK request', async ({ page }) => {
  let sdkRequests = 0;
  await page.route('https://maps.googleapis.com/maps/api/js?**', route => { sdkRequests++; return route.abort(); });
  await page.goto('/?view=map&keyword=Chứng%20tích');
  await expect(page.getByText('Không có điểm thuộc tuyến Quận 1', { exact: false })).toBeVisible();
  expect(sdkRequests).toBe(0);
});
