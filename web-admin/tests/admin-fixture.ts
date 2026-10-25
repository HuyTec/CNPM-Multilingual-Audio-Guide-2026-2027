import { test as base, expect, type Page } from "@playwright/test";
import { getLocations, type Location } from "../src/mocks/data";
import { daysAgo, today } from "../src/lib/format";
import type { FeedbackItem } from "../src/lib/feedback";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

type Data = {
  locations: Location[];
  feedback: FeedbackItem[];
  exportFailures: number;
  listError: boolean;
};
/** Browser tests mock the HTTP boundary explicitly, never claim PostgreSQL integration. */
export const test = base.extend<{ adminData: Data }>({
  adminData: async ({}, use) => {
    await use({
      locations: getLocations(),
      exportFailures: 0,
      listError: true,
      feedback: Array.from({ length: 25 }, (_, n) => ({
        id: `feedback-${n}`,
        locationId: null,
        location: "Địa điểm kiểm thử",
        category: n < 22 ? "Audio" : "Nội dung",
        content: n === 0 ? "Nội dung dài ".repeat(250) : `Phản hồi ${n}`,
        rating: n === 1 ? null : 4,
        status: "NEW",
        created: today(),
        createdAt: new Date().toISOString(),
        updated: "2020-01-01T00:00:00Z",
      })),
    });
  },
  page: async ({ page, adminData }, use) => {
    let uploadFailed = false;
    await page.route("**/api/admin/**", async (route) => {
      const request = route.request(),
        url = new URL(request.url());
      const json = (data: unknown, status = 200) =>
        route.fulfill({ status, json: data });
      if (url.pathname === "/api/admin/auth/login") return json({token:"fixture-session",username:"editor",role:"EDITOR"});
      if (url.pathname === "/api/admin/auth/me") return json({username:"editor",role:"EDITOR"});
      if (url.pathname === "/api/admin/auth/logout") return json({username:"editor",role:"EDITOR"});
      if (url.pathname === "/api/admin/content/config")
        return json({
          categories: ["Văn hóa", "Lịch sử", "Ẩm thực"],
          languages: [
            { code: "vi", label: "Tiếng Việt" },
            { code: "en", label: "English" },
            { code: "fr", label: "Français" },
          ],
        });
      if (url.pathname === "/api/admin/content/audio") {
        if (page.url().includes("upload-error") && !uploadFailed) {
          uploadFailed = true;
          return json({ detail: "Upload giả lập thất bại" }, 500);
        }
        const body = request.postDataBuffer()?.toString() || "";
        const name = /filename="([^"]+)"/.exec(body)?.[1] || "test.wav";
        return json({
          audioUrl:
            "/api/content-audio/10000000-0000-0000-0000-000000000001.wav",
          audioName: name,
          audioStatus: "ready",
        });
      }
      if (url.pathname.startsWith("/api/admin/content/audio/"))
        return route.fulfill({
          contentType: "audio/wav",
          body: readFileSync(resolve("public/audio/preview.wav")),
        });
      if (url.pathname === "/api/admin/content/locations") {
        if (page.url().includes("scenario=error") && adminData.listError)
          return json({}, 503);
        const rows = page.url().includes("scenario=long")
          ? Array.from({ length: 25 }, (_, n) => ({
              ...adminData.locations[n % 6],
              id: `long-${n}`,
              name: `${adminData.locations[n % 6].name} ${n}`,
            }))
          : adminData.locations;
        return json(rows);
      }
      if (url.pathname.startsWith("/api/admin/content/locations/")) {
        const id = url.pathname.split("/").pop()!;
        const old = adminData.locations.find((row) => row.id === id);
        if (request.method() === "PUT") {
          const body = request.postDataJSON();
          const next = {
            ...body,
            editVersion: (old as Location & { editVersion?: number })
              ?.editVersion
              ? (old as Location & { editVersion: number }).editVersion + 1
              : 1,
            updatedAt: new Date().toISOString(),
            published: old?.published,
          };
          const index = adminData.locations.findIndex((row) => row.id === id);
          if (index < 0) adminData.locations.unshift(next);
          else adminData.locations[index] = next;
          return json(next);
        }
        return old ? json(old) : json({}, 404);
      }
      if (url.pathname === "/api/admin/analytics")
        return json({
          from: url.searchParams.get("from") || daysAgo(29),
          to: url.searchParams.get("to") || today(),
          timeZone: "Asia/Ho_Chi_Minh",
          generatedAt: new Date().toISOString(),
          totalPlaybacks: 7,
          onlinePlaybacks: 6,
          offlinePlaybacks: 1,
          topLocations: [{ label: "Địa điểm kiểm thử", count: 7 }],
          languages: [{ label: "VI", count: 7 }],
        });
      if (url.pathname.startsWith("/api/admin/feedback")) {
        const category = url.searchParams.get("category"),
          status = url.searchParams.get("status");
        const rows = adminData.feedback.filter(
          (row) =>
            (!category || row.category === category) &&
            (!status || row.status === status) &&
            (!url.searchParams.get("from") ||
              row.created >= url.searchParams.get("from")!) &&
            (!url.searchParams.get("to") ||
              row.created <= url.searchParams.get("to")!),
        );
        if (url.pathname.endsWith("/export")) {
          if (adminData.exportFailures > 0) {
            adminData.exportFailures--;
            return json({ detail: "Export integration fixture failure" }, 503);
          }
          return json(rows);
        }
        if (url.pathname === "/api/admin/feedback") {
          const pageNumber = Number(url.searchParams.get("page") || 1);
          return json({
            items: rows.slice((pageNumber - 1) * 10, pageNumber * 10),
            total: rows.length,
            page: pageNumber,
            size: 10,
            totalPages: Math.ceil(rows.length / 10),
            categories: ["Audio", "Nội dung"],
          });
        }
        const parts = url.pathname.split("/"),
          id = parts[4],
          row = adminData.feedback.find((item) => item.id === id);
        if (!row) return json({}, 404);
        if (request.method() === "PATCH") {
          row.status = request.postDataJSON().status;
          row.updated = new Date().toISOString();
        }
        return json(row);
      }
      return json({}, 404);
    });
    // The real app deliberately keeps auth only in tab memory. Log in after each full navigation/reload.
    const login = async () => {
      const button = page.getByRole("button", {
        name: "Đăng nhập",
        exact: true,
      });
      if (await button.isVisible()) {
        await page
          .getByLabel("Mật khẩu", { exact: true })
          .fill("browser-fixture-only");
        await button.click();
        await expect(button).toHaveCount(0);
        await expect(
          page.getByText("Đang tải dữ liệu…", { exact: true }),
        ).toHaveCount(0);
      }
    };
    const goto = page.goto.bind(page),
      reload = page.reload.bind(page);
    page.goto = (async (...args: Parameters<Page["goto"]>) => {
      const response = await goto(...args);
      await page.locator("h1").waitFor();
      await login();
      return response;
    }) as Page["goto"];
    page.reload = (async (...args: Parameters<Page["reload"]>) => {
      const response = await reload(...args);
      await page.locator("h1").waitFor();
      await login();
      return response;
    }) as Page["reload"];
    await use(page);
  },
});
export { expect };
