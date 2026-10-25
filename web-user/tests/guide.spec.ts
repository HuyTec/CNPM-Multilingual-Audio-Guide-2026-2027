import { test, expect } from '@playwright/test';
import type { Page, Route } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

// Synthetic HTTP fixtures test the browser contract, NOT backend/DB integration.
const location = '20000000-0000-0000-0000-000000000001';
const path = `/locations/${location}`;
const content = (language = 'VI', type = 'FULL', text = `Bài đọc ${language} ${type}`) => ({
  contentId: '30000000-0000-0000-0000-000000000004',
  locationId: location, locationName: 'Bảo tàng Demo', languageCode: language,
  scriptType: type, scriptText: text, audioUrl: null,
});
async function respond(route: Route, language?: string, type?: string, text?: string) {
  const query = new URL(route.request().url()).searchParams;
  await route.fulfill({ json: content(language ?? query.get('language')!, type ?? query.get('scriptType')!, text) });
}
async function mockApi(page: Page) {
  await page.route('**/api/**', route => respond(route));
}

test.describe('Guide browser contract — synthetic API', () => {
  test('a forbidden guide response never reads stale offline storage', async ({ page }) => {
    await page.addInitScript(() => {
      Reflect.set(window, 'offlineReads', 0);
      const original = indexedDB.open.bind(indexedDB);
      indexedDB.open = (...args: Parameters<IDBFactory['open']>) => {
        if (args[0] === 'travelvoice-content-v1') Reflect.set(window, 'offlineReads', Number(Reflect.get(window, 'offlineReads')) + 1);
        return original(...args);
      };
    });
    await page.route('**/api/**', route => route.fulfill({ status: 403 }));
    await page.goto(path);
    await expect(page.getByRole('heading', { name: 'Chưa tải được thuyết minh' })).toBeVisible();
    expect(await page.evaluate(() => Reflect.get(window, 'offlineReads'))).toBe(0);
    await expect(page.locator('.script-text')).toHaveCount(0);
  });
  test('direct link defaults to FULL, supports all four variants, refresh and history', async ({ page }) => {
    const requests: string[] = [];
    await page.route('**/api/**', async route => {
      requests.push(route.request().url());
      await respond(route);
    });
    await page.goto(path);
    await expect(page.locator('.script-text')).toHaveText('Bài đọc VI FULL');
    await page.getByLabel('Ngôn ngữ yêu cầu').selectOption('EN');
    await expect(page.locator('.script-text')).toHaveText('Bài đọc EN FULL');
    await page.getByRole('radio', { name: 'Ngắn gọn' }).check();
    await expect(page.locator('.script-text')).toHaveText('Bài đọc EN SHORT');
    await page.getByLabel('Ngôn ngữ yêu cầu').selectOption('VI');
    await expect(page.locator('.script-text')).toHaveText('Bài đọc VI SHORT');
    await expect(page).toHaveURL(/language=VI&scriptType=SHORT/);
    await page.reload();
    await expect(page.locator('.script-text')).toHaveText('Bài đọc VI SHORT');
    await page.goBack();
    await expect(page.locator('.script-text')).toHaveText('Bài đọc EN SHORT');
    await page.goForward();
    await expect(page.locator('.script-text')).toHaveText('Bài đọc VI SHORT');
    expect(requests.every(url => new URL(url).pathname === `/api/locations/${location}/guide`)).toBeTruthy();
    await expect(page.locator('audio, video')).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Nghe thuyết minh' })).toBeVisible();
  });

  for (const effective of ['EN', 'VI']) {
    test(`fallback ${effective} is visible, retains requested JA and SHORT`, async ({ page }) => {
      const requests: string[] = [];
      await page.route('**/api/**', async route => {
        requests.push(route.request().url());
        await respond(route, effective);
      });
      await page.goto(`${path}?language=JA&scriptType=SHORT`);
      await expect(page.locator('.script-text')).toHaveText(`Bài đọc ${effective} SHORT`);
      await expect(page.locator('.fallback-note')).toContainText('Chưa có bản');
      await expect(page.getByLabel('Ngôn ngữ yêu cầu')).toHaveValue('JA');
      await expect(page.locator('.script-text')).toHaveAttribute('lang', effective.toLowerCase());
      await page.getByRole('radio', { name: 'Đầy đủ' }).check();
      await expect(page.locator('.script-text')).toHaveText(`Bài đọc ${effective} FULL`);
      expect(requests.every(url => new URL(url).searchParams.get('language') === 'JA')).toBeTruthy();
    });
  }

  test('late old response cannot overwrite new selection; loading clears old text', async ({ page }) => {
    let release: () => void = () => {};
    let finishOld: () => void = () => {};
    let delayed = false;
    const barrier = new Promise<void>(resolve => { release = resolve; });
    const oldFinished = new Promise<void>(resolve => { finishOld = resolve; });
    await page.route('**/api/**', async route => {
      if (new URL(route.request().url()).searchParams.get('language') === 'EN') {
        delayed = true;
        await barrier;
      }
      await respond(route).catch(() => {}); // Old request may have been aborted by cleanup.
      if (new URL(route.request().url()).searchParams.get('language') === 'EN') finishOld();
    });
    await page.goto(path);
    await expect(page.locator('.script-text')).toHaveText('Bài đọc VI FULL');
    await page.getByLabel('Ngôn ngữ yêu cầu').selectOption('EN');
    await expect.poll(() => delayed).toBe(true);
    await expect(page.getByRole('status')).toHaveText('Đang tải bài thuyết minh…');
    await expect(page).toHaveTitle('Đang tải thuyết minh · TravelVoice');
    await expect(page.locator('.script-text')).toHaveCount(0);
    await page.getByLabel('Ngôn ngữ yêu cầu').selectOption('VI');
    await expect(page.locator('.script-text')).toHaveText('Bài đọc VI FULL');
    release();
    await oldFinished;
    await expect(page.locator('.script-text')).toHaveText('Bài đọc VI FULL');
  });

  test('404 clears old text, keeps options and permits another location', async ({ page }) => {
    const requests: string[] = [];
    await page.route('**/api/**', async route => {
      requests.push(new URL(route.request().url()).pathname);
      if (new URL(route.request().url()).searchParams.get('scriptType') === 'SHORT') {
        await route.fulfill({ status: 404, json: { detail: 'Nội dung đang cập nhật' } });
      } else await respond(route);
    });
    await page.goto(path);
    await expect(page.locator('.script-text')).toBeVisible();
    await page.getByRole('radio', { name: 'Ngắn gọn' }).check();
    await expect(page.getByRole('heading', { name: 'Nội dung đang được cập nhật' })).toBeVisible();
    await expect(page).toHaveTitle('Nội dung đang được cập nhật · TravelVoice');
    expect((await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze()).violations).toEqual([]);
    await expect(page.locator('.script-text')).toHaveCount(0);
    await expect(page.getByRole('radio', { name: 'Ngắn gọn' })).toBeChecked();
    expect(requests.every(url => url.endsWith('/guide'))).toBeTruthy();
    await page.getByRole('link', { name: /Mở địa điểm khác/ }).first().click();
    await page.getByText('Bạn có đường dẫn địa điểm?').click();
    await expect(page.getByLabel('Đường dẫn địa điểm')).toBeVisible();
  });

  for (const error of ['503', 'network']) {
    test(`${error} shows retry, preserves URL and does not substitute fixture text`, async ({ page }) => {
      let failed = true;
      await page.route('**/api/**', async route => {
        if (!failed) return respond(route);
        if (error === 'network') await route.abort('failed');
        else await route.fulfill({ status: 503, json: { detail: 'Database unavailable' } });
      });
      await page.goto(`${path}?language=EN&scriptType=SHORT`);
      await expect(page.getByRole('alert')).toContainText('Chưa tải được');
      await expect(page).toHaveTitle('Chưa tải được thuyết minh · TravelVoice');
      await expect(page.locator('.script-text')).toHaveCount(0);
      failed = false;
      await page.getByRole('button', { name: 'Thử lại' }).click();
      await expect(page.locator('.script-text')).toHaveText('Bài đọc EN SHORT');
      await expect(page).toHaveURL(/language=EN&scriptType=SHORT/);
    });
  }

  for (const invalid of ['not-an-id', `${location}?language=bad!`, `${location}?scriptType=OTHER`]) {
    test(`invalid link ${invalid} makes no API call`, async ({ page }) => {
      let calls = 0;
      await page.route('**/api/**', async route => { calls++; await respond(route); });
      await page.goto(`/locations/${invalid}`);
      await expect(page.getByRole('heading', { name: 'Đường dẫn chưa hợp lệ' })).toBeVisible();
      expect(calls).toBe(0);
    });
  }

  for (const malformed of ['empty', 'mode', 'location', 'language']) {
    test(`rejects successful but mismatched response: ${malformed}`, async ({ page }) => {
      await page.route('**/api/**', route => route.fulfill({ json: {
        ...content(),
        ...(malformed === 'empty' ? { scriptText: ' \t\n ' } : {}),
        ...(malformed === 'mode' ? { scriptType: 'SHORT' } : {}),
        ...(malformed === 'location' ? { locationId: '20000000-0000-0000-0000-000000000002' } : {}),
        ...(malformed === 'language' ? { languageCode: 'FR' } : {}),
      } }));
      await page.goto(path);
      await expect(page.getByRole('alert')).toContainText('Chưa tải được');
      await expect(page.locator('.script-text')).toHaveCount(0);
    });
  }

  test('home accepts location link, rejects unrelated URL and has keyboard entry', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await page.keyboard.press('Tab');
    await expect(page.getByRole('link', { name: 'Đến nội dung chính' })).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.locator('main')).toBeFocused();
    await page.getByText('Bạn có đường dẫn địa điểm?').click();
    await page.getByLabel('Đường dẫn địa điểm').fill('https://example.com/unrelated');
    await page.getByRole('button', { name: 'Mở thuyết minh' }).click();
    await expect(page.getByLabel('Đường dẫn địa điểm')).toBeFocused();
    await expect(page.locator('#link-error')).toContainText('Đường dẫn chưa hợp lệ');
    await page.getByLabel('Đường dẫn địa điểm').fill(`https://example.com${path}?language=EN&scriptType=SHORT`);
    await page.keyboard.press('Enter');
    await expect(page.locator('.script-text')).toHaveText('Bài đọc EN SHORT');
  });

  test('keyboard selection keeps focus while URL and text update', async ({ page }) => {
    await mockApi(page);
    await page.goto(path);
    await expect(page.locator('.script-text')).toHaveText('Bài đọc VI FULL');
    const language = page.getByLabel('Ngôn ngữ yêu cầu');
    await language.focus();
    await language.selectOption('EN');
    await expect(page.locator('.script-text')).toHaveText('Bài đọc EN FULL');
    await expect(language).toBeFocused();
    await page.getByRole('radio', { name: 'Đầy đủ' }).focus();
    await page.keyboard.press('ArrowRight');
    await expect(page.locator('.script-text')).toHaveText('Bài đọc EN SHORT');
    await expect(page.getByRole('radio', { name: 'Ngắn gọn' })).toBeFocused();
    await expect(page.getByRole('radio', { name: 'Ngắn gọn' })).toBeChecked();
    await page.goBack();
    await expect(page.locator('.script-text')).toHaveText('Bài đọc EN FULL');
    await expect(page.getByRole('radio', { name: 'Đầy đủ' })).toBeChecked();
  });

  test('long text is exact, safely rendered, responsive and axe clean', async ({ page }, testInfo) => {
    const text = '<script>alert("never")</script>\n' + 'Câu chuyện địa điểm. '.repeat(120) + 'X'.repeat(300);
    const errors: string[] = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.route('**/api/**', route => respond(route, 'VI', 'FULL', text));
    await page.goto(path);
    await expect(page.locator('.script-text')).toHaveText(text);
    await expect(page).toHaveTitle('Bảo tàng Demo · TravelVoice');
    await expect(page.locator('.script-text script')).toHaveCount(0);
    for (const width of [320, 768, 1024, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
      expect(results.violations).toEqual([]);
      await page.screenshot({ path: testInfo.outputPath(`reader-${width}.png`), fullPage: true });
    }
    expect(errors).toEqual([]);
  });
});
