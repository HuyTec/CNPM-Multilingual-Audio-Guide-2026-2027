import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import jsQR from "jsqr";
import { createHash } from "node:crypto";
test.skip(
  !process.env.PUBLICATION_API,
  "Requires an isolated real backend and seeded database accounts",
);
test("real DB: translate, draft, reviewer publication, reconnect polling, verified browser install", async ({
  page,
  browser,
  request,
}) => {
  const failures: string[] = [];
  page.on("pageerror", (e) => failures.push(e.message));
  async function login(target: typeof page, role: "editor" | "reviewer") {
    await target.goto("/locations");
    await target.getByLabel("Tên đăng nhập", { exact: true }).fill(role);
    await target
      .getByLabel("Mật khẩu", { exact: true })
      .fill(role === "editor" ? "EditorDemo!2026" : "ReviewerDemo!2026");
    await target
      .getByRole("button", { name: "Đăng nhập", exact: true })
      .click();
    await expect(
      target.getByRole("button", { name: "Đăng xuất quản trị" }),
    ).toBeVisible();
    await expect(
      target.getByRole("button", { name: "Đăng nhập", exact: true }),
    ).toHaveCount(0);
  }
  const before = (await (
    await request.get(`${process.env.PUBLICATION_API}/api/content-packages`)
  ).json()) as { scope: string; version: string }[];
  const userContext = await browser.newContext({
    baseURL: "http://127.0.0.1:15175",
  });
  const user = await userContext.newPage();
  await user.goto("http://127.0.0.1:15175/offline");
  await expect(
    user.getByRole("button", { name: "Tải gói", exact: true }).first(),
  ).toBeVisible();
  const editorName = `POI E2E ${Date.now()}`;
  await login(page, "editor");
  await page.getByRole("link", { name: "Thêm địa điểm", exact: true }).click();
  await page.getByLabel("Tên địa điểm").fill(editorName);
  await page.getByLabel("Danh mục", { exact: true }).selectOption({ index: 1 });
  await page.getByLabel("Vĩ độ", { exact: true }).fill("10.78");
  await page.getByLabel("Kinh độ", { exact: true }).fill("106.70");
  await page.getByLabel("Bán kính geofence (m)", { exact: true }).fill("80");
  await page
    .getByLabel("Nội dung thuyết minh", { exact: true })
    .fill("Nội dung nguồn được kiểm tra trước khi xuất bản.");
  await page.getByLabel("Chọn ảnh địa điểm", { exact: true }).setInputFiles({
    name: "poi-test.png",
    mimeType: "image/png",
    buffer: await page.screenshot(),
  });
  await expect(
    page.getByAltText("Ảnh đại diện địa điểm đang biên tập"),
  ).toBeVisible();
  await page
    .getByRole("radio", { name: "Dịch tự động từ nội dung nguồn", exact: true })
    .click();
  await page.getByRole("checkbox", { name: "English", exact: true }).check();
  await page.getByRole("button", { name: "Dịch tự động", exact: true }).click();
  await expect(page.getByText(/DEMO: kết quả giả lập/)).toBeVisible();
  await page.getByLabel("Ngôn ngữ", { exact: true }).selectOption("en");
  await expect(page.getByLabel("Tên địa điểm")).toHaveValue(
    `[DEMO en] ${editorName}`,
  );
  await page.getByLabel("Tên địa điểm").fill(`${editorName} English`);
  await page.getByRole("button", { name: "Lưu nháp", exact: true }).click();
  await expect(
    page.getByText("Đã lưu bản nháp vào database chung.", { exact: true }),
  ).toBeVisible();
  const id = page.url().match(/locations\/([^/]+)\/edit/)?.[1];
  expect(id).toBeTruthy();
  await page.getByRole("button", { name: "Gửi duyệt", exact: true }).click();
  await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
  await expect(page.getByText(/Đã gửi duyệt trong database/)).toBeVisible();
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  const reviewerContext = await browser.newContext({
    baseURL: "http://127.0.0.1:15174",
  });
  const reviewer = await reviewerContext.newPage();
  await login(reviewer, "reviewer");
  await reviewer
    .getByRole("link", { name: "Duyệt và xuất bản", exact: true })
    .click();
  await reviewer.getByRole("link", { name: editorName, exact: true }).click();
  await expect(
    reviewer.getByRole("heading", { name: "So sánh bản nội dung" }),
  ).toBeVisible();
  await reviewer
    .getByLabel("Nhận xét (bắt buộc khi từ chối)")
    .fill("Nội dung phù hợp.");
  reviewer.once("dialog", (dialog) => dialog.accept());
  await reviewer
    .getByRole("button", { name: "Chấp thuận và tạo gói", exact: true })
    .click();
  await expect
    .poll(
      async () => {
        const result = await request.get(
          `${process.env.PUBLICATION_API}/api/locations/${id}`,
        );
        return result.status();
      },
      { timeout: 30000 },
    )
    .toBe(200);
  const after = (await (
    await request.get(`${process.env.PUBLICATION_API}/api/content-packages`)
  ).json()) as { scope: string; version: string }[];
  const publishedPoi = await (await request.get(`${process.env.PUBLICATION_API}/api/locations/${id}`)).json() as { categoryId: string; imageUrl: string };
  expect(publishedPoi.imageUrl).toMatch(/^\/api\/content-images\//);
  const publishedImage = await request.get(`${process.env.PUBLICATION_API}${publishedPoi.imageUrl}`);
  expect(publishedImage.status()).toBe(200);
  expect(publishedImage.headers()["content-type"]).toBe("image/png");
  const expectedScope = publishedPoi.categoryId === "10000000-0000-0000-0000-000000000101" ? "BASE" : "OPTIONAL";
  const changed = after.find(
    (m) => m.scope === expectedScope && before.find((b) => b.scope === m.scope)?.version !== m.version,
  )!;
  expect(changed).toBeTruthy();
  const polled = user.waitForResponse(
    (r) => r.url().endsWith("/api/content-packages") && r.status() === 200,
  );
  await user.evaluate(() => window.dispatchEvent(new Event("online")));
  await polled;
  await expect(user.getByText(/Có nội dung mới/)).toBeVisible();
  const index = after.findIndex((m) => m.scope === changed.scope);
  await user
    .getByRole("button", { name: "Tải gói", exact: true })
    .nth(index)
    .click();
  await user.getByRole("button", { name: "Xác nhận tải", exact: true }).click();
  await expect(user.getByText(/Đã cài nội dung được xác minh/)).toBeVisible({
    timeout: 30000,
  });
  const installed = await user.evaluate(async () => {
    const packages = await import("/src/lib/packages.ts");
    return (await packages.installedPackages()).map((p) => ({
      scope: p.stored.scope,
      version: p.stored.manifest.version,
      locations: p.payload.locations,
    }));
  });
  const stored = installed.find((p) => p.scope === changed.scope)!;
  expect(stored.version).toBe(changed.version);
  expect(stored.locations.find((l) => l.locationId === id)?.names?.en).toBe(
    `${editorName} English`,
  );
  expect(
    (await new AxeBuilder({ page: reviewer }).analyze()).violations,
  ).toEqual([]);
  expect(failures).toEqual([]);
  await reviewerContext.close();
  await userContext.close();
});

test("Android promotion serves the current APK and its QR decodes to the same URL", async ({
  page,
  request,
}) => {
  const origin = process.env.PUBLIC_DEMO_URL || "http://127.0.0.1:15175";
  await page.goto(origin + "/");
  const promo = page.getByRole("region", {
    name: "Khám phá cùng TravelVoice trên Android",
  });
  const download = promo.getByRole("link", {
    name: "Tải ứng dụng Android",
    exact: true,
  });
  await expect(download).toBeVisible();
  const href = (await download.getAttribute("href"))!;
  const image = promo.getByRole("img", {
    name: "Mã QR tải ứng dụng TravelVoice cho Android",
  });
  await expect(image).toBeVisible();
  const pixels = await image.evaluate(async (img) => {
    const element = img as HTMLImageElement;
    await element.decode();
    const canvas = document.createElement("canvas");
    canvas.width = element.naturalWidth;
    canvas.height = element.naturalHeight;
    const ctx = canvas.getContext("2d")!;
    ctx.drawImage(element, 0, 0);
    const raw = ctx.getImageData(0, 0, canvas.width, canvas.height);
    return {
      pixels: Array.from(raw.data),
      width: raw.width,
      height: raw.height,
    };
  });
  expect(
    jsQR(new Uint8ClampedArray(pixels.pixels), pixels.width, pixels.height)
      ?.data,
  ).toBe(href);
  const metadata = (await (
    await request.get(origin + "/downloads/android-release.json")
  ).json()) as { sizeBytes: number; sha256: string };
  const response = await request.get(href);
  expect(response.status()).toBe(200);
  const apk = await response.body();
  expect(apk.byteLength).toBe(metadata.sizeBytes);
  expect(createHash("sha256").update(apk).digest("hex")).toBe(metadata.sha256);
  expect(apk.subarray(0, 2).toString()).toBe("PK");
  await promo.screenshot({
    path: "test-results/publication-live/android-promo-desktop.png",
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await promo.scrollIntoViewIfNeeded();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await promo.screenshot({
    path: "test-results/publication-live/android-promo-mobile.png",
  });
  expect(
    (await new AxeBuilder({ page }).include("#android-app").analyze())
      .violations,
  ).toEqual([]);
});
