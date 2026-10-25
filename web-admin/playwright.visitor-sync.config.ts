import { defineConfig } from '@playwright/test';
export default defineConfig({ testDir: './tests', testMatch: 'visitor-sync.live.spec.ts', workers: 1,
  timeout: 60_000, use: { headless: true, viewport: { width: 1440, height: 1000 }, trace: 'retain-on-failure' }, reporter: 'list' });
