import { test, expect } from "@playwright/test";
import type { APIRequestContext, Page } from "@playwright/test";
import { mkdirSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import AxeBuilder from "@axe-core/playwright";

// Real Docker Web Admin -> authenticated backend -> existing PostgreSQL.
// No route interception, localStorage fixtures, or seeded playback events.
test.skip(
  process.env.LIVE_CONTENT_API !== "1",
  "Requires local demo backend/admin credentials",
);
const owned: string[] = [];
function recordOwned(id: string) {
  owned.push(id);
  mkdirSync("test-results-content", { recursive: true });
  writeFileSync("test-results-content/owned-ids.json", JSON.stringify(owned));
}
async function auth(request: APIRequestContext) {
  const result=await request.post("/api/admin/auth/login",{data:{username:process.env.DEMO_EDITOR_USERNAME||"editor",password:process.env.DEMO_EDITOR_PASSWORD||"EditorDemo!2026"}});
  expect(result.ok()).toBeTruthy();return `Bearer ${(await result.json()).token}`;
}
async function login(page: Page) {
  await expect(page.locator('input[name="password"]')).toBeVisible();
  await page
    .locator('input[name="username"]')
    .fill(process.env.DEMO_EDITOR_USERNAME || "editor");
  // An uncontrolled password field is populated without putting the value in action logs.
  await page.locator('input[name="password"]').evaluate((input, password) => {
    (input as HTMLInputElement).value = password;
  }, process.env.DEMO_EDITOR_PASSWORD || "EditorDemo!2026");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
}
test("real catalog and all twelve stored variants match the Tourist API", async ({
  page,
  request,
}) => {
  await page.goto("/locations");
  await login(page);
  await page
    .getByRole("link", { name: "Mở địa điểm Dinh Độc Lập", exact: true })
    .click();
  const id = "20000000-0000-0000-0000-000000000101";
  for (const language of ["vi", "en", "fr", "ja", "zh", "ru"])
    for (const type of ["FULL", "SHORT"]) {
      await page.locator("#language").selectOption(language);
      await page.locator("#script-type").selectOption(type);
      const response = await request.get(
        `/api/locations/${id}/guide?language=${language.toUpperCase()}&scriptType=${type}`,
      );
      expect(response.status()).toBe(200);
      const guide = await response.json();
      await expect(page.locator("#script")).toHaveValue(guide.scriptText);
    }
  const result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
    .analyze();
  expect(result.violations).toEqual([]);
});
test("save incomplete draft, upload actual WAV, submit and reopen persisted server data", async ({
  page,
  request,
}) => {
  await page.goto("/locations/new");
  await login(page);
  const name = `Codex UI integration ${Date.now()}`;
  await page.locator("#name").fill(name);
  const savedResponse = page.waitForResponse(
    (response) =>
      response.request().method() === "PUT" &&
      response.url().includes("/api/admin/content/locations/"),
  );
  await page.getByRole("button", { name: "Lưu nháp", exact: true }).click();
  const response = await savedResponse;
  expect(response.status()).toBe(200);
  const saved = await response.json();
  recordOwned(saved.id);
  await expect(page).toHaveURL(new RegExp(`/locations/${saved.id}/edit$`));
  await expect(page.locator("#name")).toHaveValue(name);
  await page.locator("#category").selectOption({ index: 1 });
  await page.locator("#latitude").fill("10.777");
  await page.locator("#longitude").fill("106.701");
  await page.locator("#radius").fill("100");
  await page.locator("#language").selectOption("fr");
  await page.locator("#script-type").selectOption("SHORT");
  const script =
    "Texte de test sauvegardé dans la base commune, en attente de validation.";
  await page.locator("#script").fill(script);
  const upload = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/api/admin/content/audio"),
  );
  await page
    .locator("#audio-file")
    .setInputFiles(
      resolve(
        "../backend/multi-audio-guide/src/main/resources/static/demo-audio/museum.wav",
      ),
    );
  expect((await upload).status()).toBe(200);
  await expect(page.locator("audio")).toHaveAttribute("src", /^blob:/);
  await page.getByRole("button", { name: "Gửi duyệt", exact: true }).click();
  const pendingResponse = page.waitForResponse(
    (response) =>
      response.request().method() === "PUT" &&
      response.url().endsWith(saved.id),
  );
  await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
  expect((await pendingResponse).status()).toBe(200);
  await expect(page.locator(".form-footer [role=status]")).toContainText(
    "Đã gửi duyệt trong database",
  );
  const storedResponse = await request.get(
    `/api/admin/content/locations/${saved.id}`,
    { headers: { Authorization: await auth(request) } },
  );
  expect(storedResponse.status()).toBe(200);
  const stored = await storedResponse.json();
  expect(stored.status).toBe("PENDING_REVIEW");
  expect(stored.editVersion).toBe(2);
  expect(stored.assets["fr:SHORT"].text).toBe(script);
  expect(stored.assets["fr:SHORT"].audioUrl).toMatch(/^\/api\/content-audio\//);
  expect(stored.assets["vi:FULL"].audioUrl).toBeUndefined();
  const publicResponse = await request.get(
    `/api/locations/${saved.id}/guide?language=FR&scriptType=SHORT`,
  );
  expect(publicResponse.status()).toBe(404);
  await page.reload();
  await login(page);
  await expect(page.locator("#script")).toHaveValue(script);
  await expect(page.locator("audio")).toHaveAttribute("src", /^blob:/);
});
test("shared admin session reads actual playback statistics", async ({
  page,
  request,
}) => {
  await page.goto("/locations");
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Danh sách địa điểm", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("link", { name: "Thống kê & phản hồi", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Thống kê lượt nghe", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".metric-value").first()).toBeVisible();
  const params = new URL(page.url()).searchParams;
  const response = await request.get(
    `/api/admin/analytics?from=${params.get("from")}&to=${params.get("to")}`,
    { headers: { Authorization: await auth(request) } },
  );
  expect(response.status()).toBe(200);
  const data = await response.json();
  await expect(page.locator(".metric-value").first()).toHaveText(
    new Intl.NumberFormat("vi-VN").format(data.totalPlaybacks),
  );
});
