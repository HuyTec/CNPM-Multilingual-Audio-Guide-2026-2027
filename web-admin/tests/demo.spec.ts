import { test, expect } from "@playwright/test";
import ExcelJS from "exceljs";
import AxeBuilder from "@axe-core/playwright";

function sampleWav() {
  const b = Buffer.alloc(16044);
  b.write("RIFF");
  b.writeUInt32LE(b.length - 8, 4);
  b.write("WAVEfmt ", 8);
  b.writeUInt32LE(16, 16);
  b.writeUInt16LE(1, 20);
  b.writeUInt16LE(1, 22);
  b.writeUInt32LE(8000, 24);
  b.writeUInt32LE(16000, 28);
  b.writeUInt16LE(2, 32);
  b.writeUInt16LE(16, 34);
  b.write("data", 36);
  b.writeUInt32LE(16000, 40);
  return b;
}

test("UC-06 E2 focuses missing name; A1 saves incomplete draft", async ({
  page,
}) => {
  await page.goto("/locations/new");
  await page.getByRole("button", { name: "Gửi duyệt", exact: true }).click();
  await expect(page.getByLabel("Tên địa điểm")).toBeFocused();
  await expect(
    page.getByText("Nhập tên địa điểm.", { exact: true }),
  ).toBeVisible();
  await page.getByLabel("Tên địa điểm").fill("Địa điểm thử nghiệm");
  await page.getByRole("button", { name: "Lưu nháp", exact: true }).click();
  await expect(
    page.getByText("Đã lưu bản nháp trong trình duyệt."),
  ).toBeVisible();
  await page.reload();
  const stored = await page.evaluate(() =>
    JSON.parse(localStorage.getItem("hvp-demo-locations-v1")!),
  );
  expect(
    stored.find((x: { name: string }) => x.name === "Địa điểm thử nghiệm")
      .status,
  ).toBe("DRAFT");
});

test("UC-06 valid submission; E1 retry; BR4 and other language remain intact", async ({
  page,
}) => {
  await page.goto("/locations/loc-1/edit?scenario=upload-error");
  await expect(page.getByLabel("Tên địa điểm")).toHaveValue(
    "Bưu điện Trung tâm",
  );
  await page.getByLabel("Ngôn ngữ", { exact: true }).selectOption("en");
  await page
    .getByLabel("Nội dung thuyết minh")
    .fill("English text unchanged by retry");
  await page.locator("#audio-file").setInputFiles({
    name: "bad.txt",
    mimeType: "text/plain",
    buffer: Buffer.from("bad"),
  });
  await expect(page.getByText(/Định dạng không hợp lệ/)).toBeVisible();
  await page.locator("#audio-file").setInputFiles({
    name: "english.wav",
    mimeType: "audio/wav",
    buffer: sampleWav(),
  });
  await expect(page.getByText(/Upload giả lập thất bại/)).toBeVisible();
  await expect(page.getByLabel("Nội dung thuyết minh")).toHaveValue(
    "English text unchanged by retry",
  );
  await page.getByRole("button", { name: "Thử lại upload" }).click();
  await expect(page.locator("audio")).toHaveAttribute("src", /blob:/);
  await page.getByRole("button", { name: "Gửi duyệt", exact: true }).click();
  await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
  await expect(page.getByText(/Đã gửi duyệt giả lập/)).toBeVisible();
  const record = await page.evaluate(() =>
    JSON.parse(localStorage.getItem("hvp-demo-locations-v1")!).find(
      (x: { id: string }) => x.id === "loc-1",
    ),
  );
  expect(record.status).toBe("PENDING_REVIEW");
  expect(record.published.version).toBe("1-demo");
  expect(record.assets["vi:FULL"].text).toBe(
    record.published.assets["vi:FULL"].text,
  );
  expect(record.assets["en:FULL"].audioName).toBe("english.wav");
});

test("UC-06 unsaved navigation asks confirmation and Escape keeps form", async ({
  page,
}) => {
  await page.goto("/locations/new");
  await page.getByLabel("Tên địa điểm").fill("Chưa lưu");
  await page
    .getByRole("link", { name: "Thống kê & phản hồi", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(page.getByLabel("Tên địa điểm")).toHaveValue("Chưa lưu");
  await page
    .getByRole("link", { name: "Thống kê & phản hồi", exact: true })
    .click();
  await page.getByRole("button", { name: "Rời trang", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Thống kê & phản hồi", exact: true }),
  ).toBeVisible();
});

test("UC-07 URL filters/detail; strict feedback transitions and timestamp persist", async ({
  page,
}) => {
  await page.goto("/analytics");
  await expect(page).toHaveURL(/from=.*to=/);
  await page.getByLabel("Trạng thái", { exact: true }).selectOption("NEW");
  await expect(page).toHaveURL(/status=NEW/);
  await page.getByRole("button", { name: "Xem chi tiết" }).first().click();
  await expect(page).toHaveURL(/feedback=fb-/);
  await page.getByRole("button", { name: "Chuyển sang Đang xử lý" }).click();
  await expect(
    page.getByRole("dialog").getByText("Đang xử lý", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Đánh dấu Đã giải quyết" }).click();
  await expect(
    page.getByRole("dialog").getByText("Đã giải quyết", { exact: true }),
  ).toBeVisible();
  await expect(page.getByText(/Cập nhật: Chưa cập nhật/)).toHaveCount(0);
  await page.reload();
  await expect(
    page.getByRole("dialog").getByText("Đã giải quyết", { exact: true }),
  ).toBeVisible();
});

test("UC-07 A3 XLSX contains selected period and all period feedback; E2 retries", async ({
  page,
}) => {
  await page.goto("/analytics?scenario=export-error");
  await expect(
    page.getByRole("heading", { name: "Phản hồi từ du khách" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Xuất báo cáo .xlsx" }).click();
  await expect(page.getByText(/Không thể xuất báo cáo/)).toBeVisible();
  const wait = page.waitForEvent("download");
  await page.getByRole("button", { name: "Thử lại xuất báo cáo" }).click();
  const download = await wait;
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile((await download.path())!);
  expect(workbook.worksheets.map((x) => x.name)).toEqual([
    "Analytics",
    "Feedback",
  ]);
  expect(workbook.getWorksheet("Feedback")!.rowCount).toBeGreaterThan(11);
  expect(workbook.getWorksheet("Analytics")!.getCell("B2").value).toBe(
    new URL(page.url()).searchParams.get("from"),
  );
});

test("UC-07 A1 change period reloads metrics; empty and error states retry", async ({
  page,
}) => {
  await page.goto("/analytics");
  await expect(
    page.getByRole("heading", { name: "Phản hồi từ du khách" }),
  ).toBeVisible();
  await page.getByLabel("Từ ngày").fill("2000-01-01");
  await page.getByLabel("Đến ngày").fill("2000-01-31");
  await expect(page.getByText("Không có lượt phát trong kỳ này")).toBeVisible();
  await expect(page.getByText("Không có phản hồi phù hợp")).toBeVisible();
  await page.goto("/locations?scenario=error");
  await expect(
    page.getByText("Không thể tải dữ liệu demo. Hãy thử lại."),
  ).toBeVisible();
  await page.getByRole("button", { name: "Thử lại", exact: true }).click();
  await expect(
    page.getByText("Bưu điện Trung tâm", { exact: true }),
  ).toBeVisible();
});

test("Long data pagination and narrow viewport do not overflow page", async ({
  page,
}) => {
  await page.goto("/locations?scenario=long");
  await expect(page.locator("tbody tr")).toHaveCount(10);
  await page.getByRole("button", { name: "Sau", exact: true }).click();
  await expect(page).toHaveURL(/page=2/);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  await page.goto("/analytics?scenario=long");
  await expect(
    page
      .getByRole("region", { name: "Phản hồi du khách", exact: true })
      .locator("tbody tr"),
  ).toHaveCount(10);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "test-results/analytics-mobile.png",
    fullPage: true,
  });
});

test("Accessibility audit of screens and feedback dialog", async ({ page }) => {
  for (const url of ["/locations", "/locations/new", "/analytics"]) {
    await page.goto(url);
    await expect(
      page.getByText("Đang tải dữ liệu…", { exact: true }),
    ).toHaveCount(0);
    const result = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
      .analyze();
    expect(result.violations).toEqual([]);
  }
  await page.getByRole("button", { name: "Xem chi tiết" }).first().click();
  await expect(page.getByRole("dialog")).toBeVisible();
  const result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
    .analyze();
  expect(result.violations).toEqual([]);
});

test("Desktop screenshots and keyboard skip link", async ({ page }) => {
  for (const [url, name] of [
    ["/locations", "locations"],
    ["/locations/loc-1/edit", "form"],
    ["/analytics", "analytics"],
  ]) {
    await page.goto(url);
    await expect(
      page.getByText("Đang tải dữ liệu…", { exact: true }),
    ).toHaveCount(0);
    await page.screenshot({
      path: `test-results/${name}-desktop.png`,
      fullPage: true,
    });
  }
  await page.goto("/locations");
  await page.keyboard.press("Tab");
  await expect(
    page.getByRole("link", { name: "Bỏ qua đến nội dung chính" }),
  ).toBeFocused();
});
