import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test.skip(
  !process.env.ADMIN_DEPLOYED_PASSWORD,
  "Requires explicitly configured local deployed admin credentials",
);
for (const port of [3000, 3001]) {
  test(`deployed Admin ${port} exposes manual/automatic entry, images and optional audio`, async ({
    page,
  }) => {
    await page.goto(`http://127.0.0.1:${port}/locations/new`);
    await page.getByLabel("Tên đăng nhập", { exact: true }).fill("admin");
    await page
      .getByLabel("Mật khẩu", { exact: true })
      .fill(process.env.ADMIN_DEPLOYED_PASSWORD!);
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
    await expect(
      page.getByRole("heading", { name: "Thêm địa điểm", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("radio", { name: "Nhập thủ công", exact: true }),
    ).toBeChecked();
    await expect(
      page.getByRole("button", { name: "Chọn ảnh", exact: true }),
    ).toBeVisible();
    await page
      .getByRole("radio", {
        name: "Dịch tự động từ nội dung nguồn",
        exact: true,
      })
      .click();
    await expect(
      page.getByRole("button", { name: "Dịch tự động", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText(/Provider demo chỉ tạo kết quả giả lập/),
    ).toBeVisible();
    expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.screenshot({
      path: `test-results/deployed-admin-${port}-desktop.png`,
      fullPage: true,
    });
    await page.setViewportSize({ width: 390, height: 844 });
    await expect
      .poll(() =>
        page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      )
      .toBe(true);
    await page.screenshot({
      path: `test-results/deployed-admin-${port}.png`,
      fullPage: true,
    });
  });
}
