import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests', testMatch: 'offline.live.spec.ts', outputDir: './test-results-offline', workers: 1,
  use: { baseURL: 'http://127.0.0.1:4175', browserName: 'chromium', headless: true,
    screenshot: 'only-on-failure', trace: 'retain-on-failure' },
  webServer: { command: 'npm.cmd run preview', url: 'http://127.0.0.1:4175', reuseExistingServer: true },
  reporter: 'list',
});
