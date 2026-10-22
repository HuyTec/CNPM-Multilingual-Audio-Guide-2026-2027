import { expect, test } from '@playwright/test';
import type { Guide } from '../src/lib/guide';

// Opt in only with the real demo backend and its Flyway fixtures running.
// No page.route(), fallback data, intercepted responses or seeded playback events.
test.describe('Live Web → backend → PostgreSQL guide', () => {
  test.skip(process.env.LIVE_GUIDE_API !== '1', 'Set LIVE_GUIDE_API=1 with the real demo backend running');
  const location = '20000000-0000-0000-0000-000000000001';

  test('actual WAV starts only on click; position, pause/resume, speed and receipt work', async ({ page }) => {
    const receipts: string[] = [];
    page.on('request', request => { if (request.method() === 'POST' && request.url().endsWith('/api/playbacks')) receipts.push(request.postData()!); });
    await page.goto(`/locations/${location}?language=VI&scriptType=SHORT`);
    await expect(page.locator('.script-text')).toBeVisible();
    expect(receipts).toEqual([]);
    const response = page.waitForResponse(response => response.request().method() === 'POST' && response.url().endsWith('/api/playbacks'));
    await page.getByRole('button', { name: 'Nghe thuyết minh', exact: true }).click();
    await expect(page.locator('.guide-player [role="status"]').first()).toContainText('Đang phát · Audio thu sẵn');
    expect((await response).ok()).toBe(true);
    const slider = page.locator('#playback-position');
    await expect.poll(async () => Number(await slider.inputValue())).toBeGreaterThan(0);
    await page.getByRole('button', { name: 'Tạm dừng', exact: true }).click();
    await expect(page.locator('.guide-player [role="status"]').first()).toContainText('Đã tạm dừng');
    await page.getByLabel('Tốc độ', { exact: true }).selectOption('1.25');
    await slider.fill('2');
    await expect.poll(async () => Number(await slider.inputValue())).toBeCloseTo(2, 0);
    await page.getByRole('button', { name: 'Tiếp tục', exact: true }).click();
    await expect(page.locator('.guide-player [role="status"]').first()).toContainText('Đang phát · Audio thu sẵn');
    expect(receipts).toHaveLength(1);
    expect(JSON.parse(receipts[0]).contentId).toBe('30000000-0000-0000-0000-000000000001');
    await page.getByRole('button', { name: 'Dừng', exact: true }).click();
  });

  test('four stored variants, direct URL, switching, reload and history', async ({ page, request }, testInfo) => {
    const expected = new Map<string, Guide>();
    for (const [language, type, suffix] of [['VI', 'FULL', '004'], ['EN', 'FULL', '005'], ['EN', 'SHORT', '006'], ['VI', 'SHORT', '001']]) {
      const response = await request.get(`/api/locations/${location}/guide?language=${language}&scriptType=${type}`);
      expect(response.status()).toBe(200);
      const guide = await response.json() as Guide;
      expect(guide.contentId).toBe(`30000000-0000-0000-0000-000000000${suffix}`);
      expect(guide.locationId).toBe(location);
      expect(guide.languageCode).toBe(language);
      expect(guide.scriptType).toBe(type);
      expect(guide.scriptText.trim().length).toBeGreaterThan(0);
      expect(guide.audioUrl).toBe(suffix === '001' ? '/demo-audio/museum.wav' : null);
      expected.set(`${language}:${type}`, guide);
    }
    expect(new Set([...expected.values()].map(guide => guide.scriptText)).size).toBe(4);
    const playbackRequests: string[] = [];
    page.on('request', req => { if (/\/api\/playbacks|\/demo-audio\//.test(req.url())) playbackRequests.push(req.url()); });
    await page.goto(`/locations/${location}`);
    await expect(page.locator('.script-text')).toHaveText(expected.get('VI:FULL')!.scriptText);
    await page.getByLabel('Ngôn ngữ yêu cầu').selectOption('EN');
    await expect(page.locator('.script-text')).toHaveText(expected.get('EN:FULL')!.scriptText);
    await page.getByRole('radio', { name: 'Ngắn gọn' }).check();
    await expect(page.locator('.script-text')).toHaveText(expected.get('EN:SHORT')!.scriptText);
    await page.getByLabel('Ngôn ngữ yêu cầu').selectOption('VI');
    await expect(page.locator('.script-text')).toHaveText(expected.get('VI:SHORT')!.scriptText);
    await page.reload();
    await expect(page.locator('.script-text')).toHaveText(expected.get('VI:SHORT')!.scriptText);
    await page.goBack();
    await expect(page.locator('.script-text')).toHaveText(expected.get('EN:SHORT')!.scriptText);
    await page.goForward();
    await expect(page.locator('.script-text')).toHaveText(expected.get('VI:SHORT')!.scriptText);
    expect(playbackRequests).toEqual([]);
    await page.screenshot({ path: testInfo.outputPath('live-reader-desktop.png'), fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    await page.screenshot({ path: testInfo.outputPath('live-reader-mobile.png'), fullPage: true });
  });

  test('real EN/VI fallback, missing FULL and unpublished location', async ({ page, request }) => {
    await page.goto(`/locations/${location}?language=JA&scriptType=SHORT`);
    await expect(page.locator('.fallback-note')).toContainText('Chưa có bản');
    await expect(page.locator('.script-text')).toHaveAttribute('lang', 'en');
    await expect(page.getByLabel('Ngôn ngữ yêu cầu')).toHaveValue('JA');
    await page.getByRole('radio', { name: 'Đầy đủ' }).check();
    await expect(page.locator('.script-text')).toHaveAttribute('lang', 'en');
    await expect(page).toHaveURL(/language=JA&scriptType=FULL/);
    const viOnly = '20000000-0000-0000-0000-000000000002';
    const response = await request.get(`/api/locations/${viOnly}/guide?language=JA&scriptType=SHORT`);
    expect(response.status()).toBe(200);
    const guide = await response.json() as Guide;
    expect(guide.languageCode).toBe('VI');
    expect(guide.scriptType).toBe('SHORT');
    expect(guide.contentId).toBe('30000000-0000-0000-0000-000000000002');
    await page.goto(`/locations/${viOnly}?language=JA&scriptType=SHORT`);
    await expect(page.locator('.script-text')).toHaveText(guide.scriptText);
    await expect(page.locator('.fallback-note')).toContainText('Tiếng Việt');
    await page.getByRole('radio', { name: 'Đầy đủ' }).check();
    await expect(page.getByRole('heading', { name: 'Nội dung đang được cập nhật' })).toBeVisible();
    await expect(page.locator('.script-text')).toHaveCount(0);
    // A location with FULL but no SHORT must not silently substitute FULL.
    // That SQL branch is verified transactionally by GuideIntegrationTest.
    await page.goto('/locations/20000000-0000-0000-0000-000000000004');
    await expect(page.getByRole('heading', { name: 'Nội dung đang được cập nhật' })).toBeVisible();
    await expect(page.locator('.script-text')).toHaveCount(0);
    await expect(page.locator('audio, video')).toHaveCount(0);
  });
});
