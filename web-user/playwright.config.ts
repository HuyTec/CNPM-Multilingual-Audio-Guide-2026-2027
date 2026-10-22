import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  // Offline tests require the production preview and have their own configuration.
  testIgnore: ['**/maps.spec.ts', '**/offline.live.spec.ts'],
  timeout: 60_000,
  workers: 1,
  use: {
    baseURL: 'http://127.0.0.1:5175',
    browserName: 'chromium',
    channel: process.env.PLAYWRIGHT_CHANNEL,
    headless: true,
    viewport: { width: 1440, height: 1000 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'npm.cmd run dev',
    url: 'http://127.0.0.1:5175',
    reuseExistingServer: !process.env.CI,
  },
  reporter: 'list',
});
