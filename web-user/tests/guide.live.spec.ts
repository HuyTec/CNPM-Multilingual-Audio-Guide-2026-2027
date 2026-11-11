import { expect, test } from '@playwright/test';
import type { Guide } from '../src/lib/guide';
import AxeBuilder from '@axe-core/playwright';

// Opt in only with the real demo backend and its Flyway fixtures running.
// No page.route(), fallback data, intercepted responses or seeded playback events.
test.describe('Live Web → backend → PostgreSQL guide', () => {
  test.skip(process.env.LIVE_GUIDE_API !== '1', 'Set LIVE_GUIDE_API=1 with the real demo backend running');
  const location = '20000000-0000-0000-0000-000000000108';

  test('published Xá Lợi photo loads on mobile without overflow or autoplay', async ({ page, request }, info) => {
    const detail = await (await request.get(`/api/locations/${location}`)).json() as { imageUrl: string };
    expect(detail.imageUrl).toMatch(/^\/api\/content-images\//);
    const photoResponse = await request.get(detail.imageUrl);
    expect(photoResponse.status()).toBe(200);
    expect(photoResponse.headers()['content-type']).toBe('image/png');
    const receipts: string[] = [];
    page.on('request', request => { if (request.method() === 'POST' && request.url().endsWith('/api/playbacks')) receipts.push(request.url()); });
    for (const width of [320, 390, 430]) {
      await page.setViewportSize({ width, height: 844 });
      await page.goto(`/locations/${location}?language=VI&scriptType=FULL`);
      const photo = page.getByRole('img', { name: 'Chùa Xá Lợi', exact: true });
      await expect(photo).toHaveAttribute('src', detail.imageUrl);
      await expect.poll(() => photo.evaluate(image => (image as HTMLImageElement).naturalWidth)).toBe(800);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      await expect(page.getByRole('button', { name: 'Nghe thuyết minh', exact: true })).toBeVisible();
      expect((await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()).violations).toEqual([]);
      await page.screenshot({ path: info.outputPath(`xa-loi-mobile-${width}.png`), fullPage: true });
    }
    expect(receipts).toEqual([]);
  });

  test('actual MP3 starts only on click; position, pause/resume, speed and receipt work', async ({ page, request }) => {
    const receipts: string[] = [];
    page.on('request', request => { if (request.method() === 'POST' && request.url().endsWith('/api/playbacks')) receipts.push(request.postData()!); });
    await page.goto(`/locations/${location}?language=VI&scriptType=SHORT`);
    await expect(page.locator('.script-text')).toBeVisible();
    expect(receipts).toEqual([]);
    const response = page.waitForResponse(response => response.request().method() === 'POST' && response.url().endsWith('/api/playbacks'));
    await page.getByRole('button', { name: 'Nghe thuyết minh', exact: true }).click();
    await expect(page.locator('.guide-player [role="status"]').first()).toContainText('Đang phát · Audio thu sẵn');
    const recorded = await response;
    expect(recorded.ok()).toBe(true);
    expect((await recorded.json()).created).toBe(true);
    const repeated = await request.post('/api/playbacks', { data: JSON.parse(receipts[0]) });
    expect(repeated.ok()).toBe(true);
    expect((await repeated.json()).created).toBe(false);
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
    expect(JSON.parse(receipts[0]).contentId).toBe('32000000-0000-0000-0000-000000000002');
    await page.getByRole('button', { name: 'Dừng', exact: true }).click();
  });

  test('four stored variants, direct URL, switching, reload and history', async ({ page, request }, testInfo) => {
    const expected = new Map<string, Guide>();
    for (const [language, type, suffix] of [['VI', 'FULL', '001'], ['EN', 'FULL', '003'], ['EN', 'SHORT', '004'], ['VI', 'SHORT', '002']]) {
      const response = await request.get(`/api/locations/${location}/guide?language=${language}&scriptType=${type}`);
      expect(response.status()).toBe(200);
      const guide = await response.json() as Guide;
      expect(guide.contentId).toBe(`32000000-0000-0000-0000-000000000${suffix}`);
      expect(guide.locationId).toBe(location);
      expect(guide.languageCode).toBe(language);
      expect(guide.scriptType).toBe(type);
      expect(guide.scriptText.trim().length).toBeGreaterThan(0);
      if (language === 'VI') expect(guide.audioUrl).toMatch(/^\/api\/content-audio\/[0-9a-f-]{36}\.mp3$/);
      else expect(guide.audioUrl).toBeNull();
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

  test('real EN fallback, removed locations and unpublished location', async ({ page, request }) => {
    await page.goto(`/locations/${location}?language=DE&scriptType=SHORT`);
    await expect(page.locator('.reader .fallback-note')).toContainText('Chưa có bản');
    await expect(page.locator('.script-text')).toHaveAttribute('lang', 'en');
    await expect(page.getByLabel('Ngôn ngữ yêu cầu')).toHaveValue('DE');
    await page.getByRole('radio', { name: 'Đầy đủ' }).check();
    await expect(page.locator('.script-text')).toHaveAttribute('lang', 'en');
    await expect(page).toHaveURL(/language=DE&scriptType=FULL/);
    for (const suffix of ['001','002','003']) {
      const response = await request.get(`/api/locations/20000000-0000-0000-0000-000000000${suffix}/guide?language=VI&scriptType=SHORT`);
      expect(response.status()).toBe(404);
    }
    // A location with FULL but no SHORT must not silently substitute FULL.
    // That SQL branch is verified transactionally by GuideIntegrationTest.
    await page.goto('/locations/20000000-0000-0000-0000-000000000004');
    await expect(page.getByRole('heading', { name: 'Nội dung đang được cập nhật' })).toBeVisible();
    await expect(page.locator('.script-text')).toHaveCount(0);
    await expect(page.locator('audio, video')).toHaveCount(0);
  });
});
