import { defineConfig } from '@playwright/test';
import base from './playwright.config';

export default defineConfig({ ...base, testMatch: ['**/maps.spec.ts', '**/gps.spec.ts'], testIgnore: [], outputDir: './test-results-maps',
  use: { ...base.use, channel: process.env.PLAYWRIGHT_CHANNEL, baseURL: 'http://127.0.0.1:5176' },
  webServer: { command: 'npm.cmd run dev -- --port 5176', url: 'http://127.0.0.1:5176', reuseExistingServer: false, timeout: 30_000,
  },
});
