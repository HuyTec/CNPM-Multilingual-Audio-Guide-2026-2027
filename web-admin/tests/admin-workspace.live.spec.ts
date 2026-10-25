import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test.skip(!process.env.ADMIN_WORKSPACE_PASSWORD, 'Requires local deployed Admin credentials');
test('deployed editor and reviewer pages remain accessible and readable at desktop/mobile sizes', async ({ page }, info) => {
  test.setTimeout(180000);
  await page.goto('http://127.0.0.1:3001/locations');
  await expect(page.getByRole('heading', { name: 'Đăng nhập quản trị' })).toBeVisible();
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  await page.screenshot({ path: info.outputPath('login-desktop.png'), fullPage: true });
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('admin');
  await page.getByLabel('Mật khẩu', { exact: true }).fill(process.env.ADMIN_WORKSPACE_PASSWORD!);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  const nav = page.getByRole('navigation', { name: 'Điều hướng chính' });
  for (const [link, heading] of [['Địa điểm', 'Địa điểm'], ['Thống kê & phản hồi', 'Thống kê lượt nghe'], ['Hướng dẫn sử dụng', 'Hướng dẫn sử dụng']]) {
    await nav.getByRole('link', { name: link, exact: true }).click();
    await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
    await expect(page.getByText('Đang tải dữ liệu…', { exact: true })).toHaveCount(0);
    expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
    await page.screenshot({ path: info.outputPath(`${link}-desktop.png`), fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
    await page.screenshot({ path: info.outputPath(`${link}-mobile.png`), fullPage: true });
    await page.setViewportSize({ width: 1440, height: 1000 });
  }
  await page.getByRole('button', { name: 'Đăng xuất quản trị', exact: true }).click();
  await nav.getByRole('link', { name: 'Địa điểm', exact: true }).click();
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('reviewer');
  await page.getByLabel('Mật khẩu', { exact: true }).fill(process.env.REVIEWER_WORKSPACE_PASSWORD || 'ReviewerDemo!2026');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await nav.getByRole('link', { name: 'Duyệt và xuất bản', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Tác vụ tạo gói', exact: true })).toBeVisible();
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  await page.screenshot({ path: info.outputPath('review-desktop.png'), fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  await page.screenshot({ path: info.outputPath('review-mobile.png'), fullPage: true });
});
