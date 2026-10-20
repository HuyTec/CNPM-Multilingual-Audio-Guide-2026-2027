import { defineConfig } from '@playwright/test';

export default defineConfig({ testDir: './tests', testMatch: 'maps.live.spec.ts', workers: 1, timeout: 60_000,
  outputDir: './test-results-google-live',
  use: { baseURL: 'http://127.0.0.1:5175', browserName: 'chromium', headless: true,
    viewport: { width: 1280, height: 1000 }, trace: 'off', screenshot: 'off' },
  webServer: { command: 'npm.cmd run dev', url: 'http://127.0.0.1:5175', reuseExistingServer: true }, reporter: 'list' });
