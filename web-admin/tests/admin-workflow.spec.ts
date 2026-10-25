import { test, expect } from "./admin-fixture";
import ExcelJS from "exceljs";
import AxeBuilder from "@axe-core/playwright";

test("entry modes preserve manual content, image upload failure retries and draft retains image", async ({
  page,
  adminData,
}) => {
  const png = Buffer.from(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aL1sAAAAASUVORK5CYII=",
    "base64",
  );
  const imageUrl =
    "/api/content-images/40000000-0000-0000-0000-000000000001.png";
  let failed = false;
  await page.route("**/api/admin/content/images", (route) => {
    if (!failed) {
      failed = true;
      return route.fulfill({ status: 500 });
    }
    return route.fulfill({ json: { imageUrl } });
  });
  await page.route("**/api/admin/content/images/*.png", (route) =>
    route.fulfill({ contentType: "image/png", body: png }),
  );
  await page.goto("/locations/new");
  await expect(
    page.getByRole("radio", { name: "Nhập thủ công", exact: true }),
  ).toBeChecked();
  await page.getByLabel("Tên địa điểm").fill("POI ảnh thử");
  await page
    .getByLabel("Nội dung thuyết minh", { exact: true })
    .fill("Nội dung để TTS đọc.");
  await page
    .getByRole("radio", { name: "Dịch tự động từ nội dung nguồn" })
    .click();
  await expect(page).toHaveURL(/mode=auto/);
  await expect(
    page.getByRole("button", { name: "Dịch tự động", exact: true }),
  ).toBeVisible();
  await page.getByRole("radio", { name: "Nhập thủ công", exact: true }).click();
  await expect(
    page.getByLabel("Nội dung thuyết minh", { exact: true }),
  ).toHaveValue("Nội dung để TTS đọc.");
  const file = { name: "poi.png", mimeType: "image/png", buffer: png };
  await page
    .getByLabel("Chọn ảnh địa điểm", { exact: true })
    .setInputFiles(file);
  await expect(page.getByText(/Không tải được ảnh. Kiểm tra/)).toBeVisible();
  await expect(page.getByLabel("Tên địa điểm")).toHaveValue("POI ảnh thử");
  await page
    .getByLabel("Chọn ảnh địa điểm", { exact: true })
    .setInputFiles(file);
  await expect(
    page.getByAltText("Ảnh đại diện địa điểm đang biên tập"),
  ).toBeVisible();
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  await page.getByRole("button", { name: "Lưu nháp", exact: true }).click();
  await expect(
    page.getByText("Đã lưu bản nháp vào database chung.", { exact: true }),
  ).toBeVisible();
  expect(
    adminData.locations.find((item) => item.name === "POI ảnh thử")?.imageUrl,
  ).toBe(imageUrl);
});

test("translation failure keeps source text and manual editing available", async ({
  page,
}) => {
  await page.route("**/api/admin/content/translate", (route) =>
    route.fulfill({
      status: 502,
      json: { detail: "Dịch tạm lỗi; hãy nhập thủ công." },
    }),
  );
  await page.goto("/locations/new");
  await page.getByLabel("Tên địa điểm").fill("Nguồn giữ nguyên");
  await page
    .getByLabel("Nội dung thuyết minh", { exact: true })
    .fill("Kịch bản không bị mất khi dịch lỗi.");
  await page
    .getByRole("radio", { name: "Dịch tự động từ nội dung nguồn", exact: true })
    .click();
  await page.getByRole("checkbox", { name: "English", exact: true }).check();
  await page.getByRole("button", { name: "Dịch tự động", exact: true }).click();
  await expect(
    page.getByText("Dịch tạm lỗi; hãy nhập thủ công.", { exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel("Tên địa điểm")).toHaveValue("Nguồn giữ nguyên");
  await expect(
    page.getByLabel("Nội dung thuyết minh", { exact: true }),
  ).toHaveValue("Kịch bản không bị mất khi dịch lỗi.");
  await page.getByLabel("Ngôn ngữ", { exact: true }).selectOption("en");
  await page.getByLabel("Tên địa điểm").fill("Manual English");
  await expect(page.getByLabel("Tên địa điểm")).toHaveValue("Manual English");
});

test("localized name selection preserves VI, rejection is read-only and can be resubmitted", async ({
  page,
  adminData,
}) => {
  Object.assign(adminData.locations[0], {
    names: { vi: "Bưu điện Trung tâm", en: "Post Office" },
    status: "REJECTED",
    reviewerComment: "Sửa tên tiếng Anh",
  });
  await page.goto("/locations/loc-1/edit");
  await expect(
    page.getByRole("heading", { name: "Nội dung bị từ chối" }),
  ).toBeVisible();
  await expect(
    page.getByText("Sửa tên tiếng Anh", { exact: true }),
  ).toBeVisible();
  await page.getByLabel("Ngôn ngữ", { exact: true }).selectOption("en");
  await expect(page.locator("#name")).toHaveValue("Post Office");
  await page.locator("#name").fill("Central Post Office");
  await page.getByLabel("Ngôn ngữ", { exact: true }).selectOption("vi");
  await expect(page.locator("#name")).toHaveValue("Bưu điện Trung tâm");
  await page.getByRole("button", { name: "Gửi duyệt", exact: true }).click();
  await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
  await expect(page.getByText(/Đã gửi duyệt trong database/)).toBeVisible();
  expect(adminData.locations[0].names?.en).toBe("Central Post Office");
  expect(adminData.locations[0].status).toBe("PENDING_REVIEW");
  expect(adminData.locations[0].published?.name).toBe("Bưu điện Trung tâm");
  await expect(
    page.getByRole("button", { name: /^(Phê duyệt|Xuất bản)$/ }),
  ).toHaveCount(0);
});

test("live feedback filters, pagination, URL detail, sequential states and final state", async ({
  page,
}) => {
  await page.goto("/analytics");
  await page.getByLabel("Danh mục", { exact: true }).selectOption("Audio");
  await page.getByLabel("Trạng thái", { exact: true }).selectOption("NEW");
  const region = page.getByRole("region", {
    name: "Phản hồi du khách",
    exact: true,
  });
  await expect(region.locator("tbody tr")).toHaveCount(10);
  await page.getByRole("button", { name: "Sau", exact: true }).click();
  await expect(page).toHaveURL(/page=2/);
  await region.getByRole("button", { name: "Xem chi tiết" }).first().click();
  await expect(page).toHaveURL(/feedback=/);
  await expect(
    page.getByRole("button", { name: "Đánh dấu Đã giải quyết" }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "Chuyển sang Đang xử lý" }).click();
  await page.getByRole("button", { name: "Đánh dấu Đã giải quyết" }).click();
  await expect(
    page.getByRole("dialog").getByText("Đã giải quyết", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Chuyển sang Đang xử lý" }),
  ).toHaveCount(0);
  await page.reload();
  await expect(
    page.getByRole("dialog").getByText("Đã giải quyết", { exact: true }),
  ).toBeVisible();
});

test("live XLSX lazily loads ExcelJS and exports all filtered rows; failure permits retry", async ({
  page,
  adminData,
}) => {
  adminData.exportFailures = 1;
  const excelRequests: string[] = [];
  page.on("request", (request) => {
    if (/exceljs/i.test(request.url())) excelRequests.push(request.url());
  });
  await page.goto("/analytics");
  await page.getByLabel("Danh mục", { exact: true }).selectOption("Audio");
  await page.getByLabel("Trạng thái", { exact: true }).selectOption("NEW");
  await expect(
    page
      .getByRole("region", { name: "Phản hồi du khách", exact: true })
      .locator("tbody tr"),
  ).toHaveCount(10);
  expect(excelRequests).toHaveLength(0);
  await page
    .getByRole("button", { name: "Xuất báo cáo .xlsx", exact: true })
    .click();
  await expect(page.getByText(/Xuất báo cáo thất bại/)).toBeVisible();
  const pending = page.waitForEvent("download");
  await page.getByRole("button", { name: "Thử lại xuất báo cáo" }).click();
  const download = await pending;
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile((await download.path())!);
  const sheet = workbook.getWorksheet("Feedback")!;
  expect(sheet.rowCount).toBe(23);
  for (let row = 2; row <= sheet.rowCount; row++) {
    expect(sheet.getCell(row, 3).value).toBe("Audio");
    expect(sheet.getCell(row, 6).value).toBe("NEW");
  }
  expect(excelRequests.length).toBeGreaterThan(0);
  await expect(page.getByLabel("Danh mục", { exact: true })).toHaveValue(
    "Audio",
  );
});

test("live feedback accessibility, null ratings, empty period and narrow layout", async ({
  page,
}) => {
  await page.goto("/analytics");
  await expect(page.getByText("Chưa đánh giá", { exact: true })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  expect(
    (
      await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
        .analyze()
    ).violations,
  ).toEqual([]);
  await page.getByLabel("Từ ngày", { exact: true }).fill("2000-01-01");
  await page.getByLabel("Đến ngày", { exact: true }).fill("2000-01-02");
  await expect(
    page.getByText("Không có phản hồi phù hợp", { exact: true }),
  ).toBeVisible();
});
