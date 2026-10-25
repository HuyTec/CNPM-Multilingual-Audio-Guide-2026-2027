import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests",
  testMatch: "publication.live.spec.ts",
  timeout: 120000,
  workers: 1,
  outputDir: "test-results/publication-live",
  use: {
    baseURL: "http://127.0.0.1:15174",
    headless: true,
    trace: "retain-on-failure",
  },
  webServer: [
    {
      command:
        "node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 15174 --strictPort",
      url: "http://127.0.0.1:15174",
      reuseExistingServer: true,
      env: {
        API_PROXY_TARGET:
          process.env.PUBLICATION_API || "http://127.0.0.1:18081",
      },
    },
    {
      command:
        "node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 15175 --strictPort",
      cwd: "../web-user",
      url: "http://127.0.0.1:15175",
      reuseExistingServer: true,
      env: {
        API_PROXY_TARGET:
          process.env.PUBLICATION_API || "http://127.0.0.1:18081",
      },
    },
  ],
});
