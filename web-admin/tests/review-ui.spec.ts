import { test, expect } from './admin-fixture';
import AxeBuilder from '@axe-core/playwright';

test('review workspace shows selected content, publication states and responsive help', async ({ page, adminData }) => {
  const candidate = { ...adminData.locations[0], status: 'PENDING_REVIEW', editVersion: 1 };
  await page.route('**/api/admin/auth/me', route => route.fulfill({ json: { username: 'reviewer', role: 'REVIEWER' } }));
  await page.route('**/api/admin/review/locations', route => route.fulfill({ json: [candidate] }));
  await page.route('**/api/admin/review/jobs', route => route.fulfill({ json: [
    { id: 'test-job', locationId: candidate.id, status: 'READY', createdAt: new Date().toISOString() },
  ] }));
  await page.goto('/reviews');
  await expect(page.getByRole('heading', { name: 'Duyệt và xuất bản', exact: true })).toBeVisible();
  await page.getByRole('link', { name: new RegExp(candidate.name) }).click();
  await expect(page.getByRole('heading', { name: 'So sánh bản nội dung' })).toBeVisible();
  await expect(page.getByText('Sẵn sàng', { exact: true })).toBeVisible();
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  await page.getByRole('navigation', { name: 'Điều hướng chính' }).getByRole('link', { name: 'Hướng dẫn sử dụng' }).click();
  await expect(page.getByRole('heading', { name: 'Hướng dẫn sử dụng', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: '4. Kiểm tra nội dung trên Web du khách' })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
