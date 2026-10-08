import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

test("LocationList readiness, timestamps, accessible tooltips and row links", async ({
  page,
}) => {
  await page.goto("/locations");
  await expect(page.locator("tbody tr")).toHaveCount(6);
  await expect(
    page.getByText("Bản demo · UC-06 & UC-07", { exact: true }),
  ).toHaveCount(0);
  await expect(page.locator("main")).not.toContainText(/loc-\d|1-demo/);
  await expect(
    page.getByRole("columnheader", { name: "Ngôn ngữ" }),
  ).toBeVisible();
  await expect(
    page.getByRole("columnheader", { name: "Cập nhật" }),
  ).toBeVisible();
  const first = page.locator("tbody tr").first();
  await expect(
    first.getByRole("img", {
      name: "Tiếng Việt: Đủ kịch bản và audio",
      exact: true,
    }),
  ).toBeVisible();
  const missing = first.getByRole("img", {
    name: "English: Thiếu audio",
    exact: true,
  });
  await missing.focus();
  await expect(
    first.getByRole("tooltip").filter({ hasText: "English:" }),
  ).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(
    first.getByRole("tooltip").filter({ hasText: "English:" }),
  ).not.toBeVisible();
  await first
    .getByRole("link", {
      name: "Mở địa điểm Bưu điện Trung tâm",
      exact: true,
    })
    .focus();
  await missing.hover();
  const tooltip = first.getByRole("tooltip").filter({ hasText: "English:" });
  await tooltip.hover();
  await expect(tooltip).toBeVisible();
  await expect(
    first.getByRole("img", { name: "Français: Chưa có", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("img", { name: "English: Audio lỗi", exact: true }),
  ).toBeVisible();
  expect(
    await page
      .locator("time")
      .evaluateAll(
        (nodes) => new Set(nodes.map((n) => n.getAttribute("datetime"))).size,
      ),
  ).toBe(6);
  const coords = first.locator(".location-coordinates");
  await expect(coords).toContainText("Lat (vĩ độ):");
  await expect(coords).toContainText("Lng (kinh độ):");
  expect(await coords.evaluate((el) => getComputedStyle(el).whiteSpace)).toBe(
    "nowrap",
  );
  expect(
    await coords.evaluate((el) => getComputedStyle(el).fontVariantNumeric),
  ).toBe("tabular-nums");
  const edit = first.getByRole("link", {
    name: "Chỉnh sửa địa điểm Bưu điện Trung tâm",
    exact: true,
  });
  await expect(edit).toHaveAttribute("href", "/locations/loc-1/edit");
  await expect(edit.locator("svg")).toHaveCount(0);
  // A click away from the visible name still reaches the native Link's stretched area.
  const bounds = await coords.boundingBox();
  if (!bounds) throw new Error("Coordinate cell is not visible");
  await page.mouse.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
  await expect(page).toHaveURL(/\/locations\/loc-1\/edit/);
  await expect(page.getByLabel("Tên địa điểm")).toHaveValue(
    "Bưu điện Trung tâm",
  );
});

test("Status statistics filter using URL, keep search, reset pagination, support Back", async ({
  page,
}) => {
  await page.goto("/locations?scenario=long&q=Trung&page=2");
  const drafts = page.getByRole("button", { name: /^Bản nháp/ });
  await drafts.focus();
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(/status=DRAFT/);
  const params = new URL(page.url()).searchParams;
  expect(params.get("q")).toBe("Trung");
  expect(params.has("page")).toBe(false);
  await expect(drafts).toHaveAttribute("aria-pressed", "true");
  await page.goBack();
  await expect(page).toHaveURL(/page=2/);
  await page.goto("/locations");
  await page.getByRole("button", { name: /^Chờ duyệt/ }).click();
  await expect(page.locator("tbody tr")).toHaveCount(2);
  await expect(page.getByLabel("Trạng thái nội dung", { exact: true })).toHaveValue(
    "PENDING_REVIEW",
  );
  await page.getByRole("button", { name: /^Tất cả địa điểm/ }).click();
  await expect(page.locator("tbody tr")).toHaveCount(6);
  expect(new URL(page.url()).searchParams.has("status")).toBe(false);
});

test("LocationList accessibility, minimum helper type and responsive finish gate", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/locations");
  await expect(page.locator("tbody tr")).toHaveCount(6);
  const result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
    .analyze();
  expect(result.violations).toEqual([]);
  const smallText = await page
    .locator(
      ".location-list small,.location-list .footnote,.location-list th,.location-list .badge,.demo-controls small,.brand-sub,.nav-label",
    )
    .evaluateAll((nodes) =>
      nodes
        .filter((n) => parseFloat(getComputedStyle(n).fontSize) < 12)
        .map((n) => n.className),
    );
  expect(smallText).toEqual([]);
  for (const width of [320, 768, 1024, 1440]) {
    await page.setViewportSize({ width, height: 1000 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBe(true);
    await page.screenshot({
      path: `test-results/location-list-${width}.png`,
      fullPage: true,
    });
  }
  expect(errors).toEqual([]);
});
