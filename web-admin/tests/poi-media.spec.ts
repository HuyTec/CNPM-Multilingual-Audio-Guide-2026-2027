import { test, expect, type Page } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
test.setTimeout(90_000);

// UX tests: only the existing config endpoint is stubbed. No image upload API is called.
async function openEditor(page: Page) {
  await page.route("**/api/admin/content/config", (route) =>
    route.fulfill({
      json: {
        categories: ["Lịch sử"],
        languages: [{ code: "vi", label: "Tiếng Việt" }],
      },
    }),
  );
  await page.goto("/locations/new");
  await page.locator('input[name="username"]').fill("ux-preview");
  await page.locator('input[name="password"]').fill("test-only");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Cho du khách một cái nhìn trước" }),
  ).toBeVisible();
}
async function png(page: Page) {
  const data = await page.evaluate(() => {
    const canvas = document.createElement("canvas");
    canvas.width = 120;
    canvas.height = 80;
    const context = canvas.getContext("2d")!;
    context.fillStyle = "#2d6653";
    context.fillRect(0, 0, 120, 80);
    return canvas.toDataURL("image/png").split(",")[1];
  });
  return Buffer.from(data, "base64");
}
test("cover and multiple gallery photos stay independent; deletion confirms and rejected files preserve previews", async ({
  page,
}) => {
  await openEditor(page);
  const buffer = await png(page);
  await page
    .locator("#poi-cover-file")
    .setInputFiles({ name: "dai-dien.png", mimeType: "image/png", buffer });
  await page.locator("#poi-gallery-files").setInputFiles([
    { name: "goc-nhin-1.png", mimeType: "image/png", buffer },
    { name: "goc-nhin-2.png", mimeType: "image/png", buffer },
  ]);
  await expect(page.locator(".poi-photo-grid .poi-photo")).toHaveCount(2);
  await page
    .locator(".poi-media")
    .screenshot({ path: test.info().outputPath("album-preview.png") });
  await page
    .locator("#poi-gallery-files")
    .setInputFiles({
      name: "tep-gia.png",
      mimeType: "image/png",
      buffer: Buffer.from("not an image"),
    });
  await expect(page.getByRole("alert")).toContainText(
    "không phải PNG, JPEG hoặc WebP hợp lệ",
  );
  await expect(page.locator(".poi-photo-grid .poi-photo")).toHaveCount(2);
  await page
    .locator("#poi-gallery-files")
    .setInputFiles({
      name: "qua-lon.png",
      mimeType: "image/png",
      buffer: Buffer.alloc(10 * 1024 * 1024 + 1),
    });
  await expect(page.getByRole("alert")).toContainText("vượt quá 10 MB");
  await page
    .getByRole("button", { name: "Xóa ảnh goc-nhin-1.png", exact: true })
    .click();
  await page.getByRole("button", { name: "Hủy", exact: true }).click();
  await expect(page.locator(".poi-photo-grid .poi-photo")).toHaveCount(2);
  await page
    .getByRole("button", { name: "Xóa ảnh goc-nhin-1.png", exact: true })
    .click();
  await page.getByRole("button", { name: "Xóa ảnh", exact: true }).click();
  await expect(page.locator(".poi-photo-grid .poi-photo")).toHaveCount(1);
  await expect(page.locator(".poi-cover img")).toHaveAttribute(
    "alt",
    "dai-dien.png",
  );
  await page
    .getByRole("button", { name: "Xem ảnh goc-nhin-2.png", exact: true })
    .click();
  await expect(
    page.getByRole("dialog", { name: "Xem ảnh địa điểm" }),
  ).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(
    page.getByRole("button", { name: "Xem ảnh goc-nhin-2.png", exact: true }),
  ).toBeFocused();
  const accessibility = await new AxeBuilder({ page })
    .include(".poi-media")
    .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
    .analyze();
  expect(accessibility.violations).toEqual([]);
});
test("gallery handles narrow layouts and warns before leaving a photo preview", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await openEditor(page);
  await page
    .locator("#poi-gallery-files")
    .setInputFiles({
      name: "anh.png",
      mimeType: "image/png",
      buffer: await png(page),
    });
  await expect(page.locator(".poi-photo-grid .poi-photo")).toHaveCount(1);
  const overflow = await page
    .locator(".poi-media")
    .evaluate((element) => element.scrollWidth > element.clientWidth);
  expect(overflow).toBe(false);
  await page
    .getByRole("link", { name: "Danh sách địa điểm", exact: true })
    .click();
  await expect(
    page.getByRole("dialog", { name: "Rời trang khi chưa lưu?" }),
  ).toBeVisible();
  await expect(page.getByRole("dialog")).toContainText(
    "Ảnh xem trước chỉ giữ trong phiên này",
  );
});
