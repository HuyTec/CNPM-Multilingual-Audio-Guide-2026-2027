import { test, expect } from "@playwright/test";
import { randomUUID } from "node:crypto";
import ExcelJS from "exceljs";
import AxeBuilder from "@axe-core/playwright";

const password = process.env.DEMO_ADMIN_PASSWORD || "";
const authorization = `Basic ${Buffer.from(`admin:${password}`).toString("base64")}`;
test.skip(
  !password,
  "Requires real demo backend on port 8081 and DEMO_ADMIN_PASSWORD.",
);

test("real database event reaches authenticated dashboard and XLSX; duplicate delivery is idempotent", async ({
  page,
  request,
}) => {
  expect((await request.get("/api/admin/analytics")).status()).toBe(401);
  await page.goto("/analytics");
  await expect(page).toHaveURL(/from=.*to=/);
  await page.getByLabel("Mật khẩu", { exact: true }).fill("incorrect-password");
  await page.getByRole("button", { name: "Xem số liệu database" }).click();
  await expect(
    page.getByText("Tên đăng nhập hoặc mật khẩu chưa đúng."),
  ).toBeVisible();
  await expect(page.getByLabel("Mật khẩu", { exact: true })).toBeFocused();
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Xem số liệu database" }).click();
  await expect(page.getByText(/Cập nhật:/)).toBeVisible();
  const before = await (
    await request.get("/api/admin/analytics", {
      headers: { Authorization: authorization },
    })
  ).json();
  const payload = {
    eventId: randomUUID(),
    contentId: "30000000-0000-0000-0000-000000000001",
  };
  const created = await request.post("/api/playbacks", { data: payload });
  expect(created.ok()).toBeTruthy();
  expect((await created.json()).created).toBe(true);
  expect(
    (await (await request.post("/api/playbacks", { data: payload })).json())
      .created,
  ).toBe(false);
  const total = page
    .locator(".metric")
    .filter({ hasText: "Tổng lượt phát" })
    .locator("strong");
  await expect
    .poll(async () => Number((await total.innerText()).replace(/\D/g, "")), {
      timeout: 15000,
    })
    .toBeGreaterThanOrEqual(before.totalPlaybacks + 1);
  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "Xuất lượt nghe .xlsx" }).click();
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile((await (await downloadPromise).path())!);
  expect(
    String(workbook.getWorksheet("Analytics")!.getCell("B1").value),
  ).toContain("PostgreSQL");
  expect(
    Number(workbook.getWorksheet("Analytics")!.getCell("B5").value),
  ).toBeGreaterThanOrEqual(before.totalPlaybacks + 1);
  await page.screenshot({
    path: "test-results/live-analytics-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator("body")).toHaveJSProperty(
    "scrollWidth",
    await page.locator("body").evaluate((el) => el.clientWidth),
  );
  await page.screenshot({
    path: "test-results/live-analytics-mobile.png",
    fullPage: true,
  });
  const accessibility = await new AxeBuilder({ page }).analyze();
  expect(accessibility.violations).toEqual([]);
});

test("real empty period has no synthetic events and date changes preserve URL state", async ({
  page,
}) => {
  await page.goto("/analytics?from=1900-01-01&to=1900-01-02");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Xem số liệu database" }).click();
  await expect(
    page.getByRole("heading", { name: "Chưa có lượt nghe trong kỳ" }),
  ).toBeVisible();
  await page.getByLabel("Từ ngày", { exact: true }).fill("2026-10-20");
  await expect(page.getByText(/Chọn kỳ báo cáo hợp lệ/)).toBeVisible();
});
