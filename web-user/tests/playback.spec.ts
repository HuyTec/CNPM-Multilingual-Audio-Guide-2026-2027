import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';

const location = '20000000-0000-0000-0000-000000000101';
const text = 'Câu đầu giữ nguyên văn. Câu tiếp theo để kiểm tra chuyển đoạn. '.repeat(10);
async function setup(page: Page, options: { language?: string; missingVoice?: boolean; lateVoice?: boolean; brokenAudio?: boolean; audio?: boolean } = {}) {
  await page.addInitScript(({ options }) => {
    const data = { spoken: [] as { text: string; lang: string; rate: number }[], cancels: 0, voices: [] as { lang: string; name: string; localService: boolean; default: boolean }[] };
    Object.assign(window, { testSpeech: data });
    class Utterance {
      text: string; lang = ''; rate = 1; voice = null;
      onstart?: () => void; onend?: () => void; onerror?: () => void; onboundary?: (event: { charIndex: number }) => void;
      constructor(value: string) { this.text = value; }
    }
    class Synthesis extends EventTarget {
      current?: Utterance;
      getVoices() { return data.voices; }
      speak(value: Utterance) {
        this.current = value; data.spoken.push({ text: value.text, lang: value.lang, rate: value.rate });
        setTimeout(() => value.onstart?.(), 20);
      }
      cancel() { data.cancels++; const previous = this.current; this.current = undefined; setTimeout(() => previous?.onend?.(), 20); }
    }
    const synth = new Synthesis();
    if (!options.missingVoice && !options.lateVoice) data.voices = [{ lang: `${options.language || 'VI'}-VN`, name: 'Test matching voice', localService: true, default: true }];
    Object.defineProperty(window, 'speechSynthesis', { configurable: true, value: synth });
    Object.defineProperty(window, 'SpeechSynthesisUtterance', { configurable: true, value: Utterance });
    if (options.lateVoice) Object.assign(window, { supplyVoice: () => {
      data.voices = [{ lang: `${options.language || 'VI'}-VN`, name: 'Late matching voice', localService: true, default: true }];
      synth.dispatchEvent(new Event('voiceschanged'));
    } });
    if (options.brokenAudio) Object.defineProperty(window, 'Audio', { configurable: true, value: class {
      playbackRate = 1; src = ''; onplaying = null; onpause = null; onended = null; onerror = null; ontimeupdate = null; onloadedmetadata = null;
      play() { return Promise.reject(new Error('decode failure')); } pause() {} load() {} removeAttribute() {}
    } });
  }, { options });
  const receipts: string[] = [];
  await page.route('**/api/**', async route => {
    if (route.request().method() === 'POST') { receipts.push(route.request().postData()!); return route.fulfill({ json: { eventId: route.request().postDataJSON().eventId, created: true } }); }
    const query = new URL(route.request().url()).searchParams;
    await route.fulfill({ json: { contentId: '31000000-0000-0000-0000-000000000001', locationId: location,
      locationName: 'Dinh Độc Lập', languageCode: options.language || query.get('language'), scriptType: query.get('scriptType'),
      scriptText: text, audioUrl: options.audio ? '/demo-audio/museum.wav' : null } });
  });
  return receipts;
}
async function speechData(page: Page) {
  return page.evaluate(() => (window as unknown as { testSpeech: { spoken: { text: string; lang: string; rate: number }[]; cancels: number } }).testSpeech);
}

test('failed playback receipt retries the same eventId without restarting media', async ({ page }) => {
  await setup(page);
  const receipts: string[] = [];
  await page.route('**/api/playbacks', route => {
    receipts.push(route.request().postData()!);
    return receipts.length === 1 ? route.fulfill({ status: 503 }) : route.fulfill({ json: { eventId: route.request().postDataJSON().eventId, created: false } });
  });
  await page.goto(`/locations/${location}`);
  await page.getByRole('button', { name: 'Nghe thuyết minh', exact: true }).click();
  await page.getByRole('button', { name: 'Gửi lại lượt nghe' }).click();
  await expect(page.getByRole('button', { name: 'Gửi lại lượt nghe' })).toHaveCount(0);
  expect(receipts).toHaveLength(2);
  expect(receipts[1]).toBe(receipts[0]);
  expect((await speechData(page)).spoken).toHaveLength(1);
});
test('TTS starts only on click, preserves exact text chunks, pause/resume/seek/rate do work and count once', async ({ page }) => {
  const receipts = await setup(page);
  await page.goto(`/locations/${location}?language=VI&scriptType=FULL`);
  await expect(page.locator('.script-text')).toHaveText(text);
  expect((await speechData(page)).spoken).toHaveLength(0);
  await page.getByRole('button', { name: 'Nghe thuyết minh', exact: true }).click();
  await expect(page.locator('.guide-player')).toContainText('Đang phát');
  expect((await speechData(page)).spoken[0].text).toBe(text.slice(0, (await speechData(page)).spoken[0].text.length));
  await page.getByRole('button', { name: 'Tạm dừng', exact: true }).click();
  await expect(page.locator('.guide-player')).toContainText('Đã tạm dừng');
  await page.getByRole('button', { name: 'Tiếp tục', exact: true }).click();
  await expect(page.locator('.guide-player')).toContainText('Đang phát');
  await page.getByRole('button', { name: 'Đoạn sau' }).click();
  await expect(page.locator('.playback-position > label')).toContainText('Đoạn 2');
  await page.getByLabel('Tốc độ').selectOption('1.5');
  await expect.poll(async () => (await speechData(page)).spoken.at(-1)?.rate).toBe(1.5);
  expect(receipts).toHaveLength(1);
  await page.getByLabel('Ngôn ngữ yêu cầu').selectOption('EN');
  await expect(page.locator('.script-text')).toBeVisible();
  await expect(page.locator('.guide-player')).toContainText('Sẵn sàng nghe');
});
for (const language of ['VI', 'EN', 'FR', 'JA', 'ZH', 'RU']) {
  test(`TTS selects only matching ${language} voice`, async ({ page }) => {
    await setup(page, { language });
    await page.goto(`/locations/${location}?language=${language}`);
    await page.getByRole('button', { name: 'Nghe thuyết minh', exact: true }).click();
    await expect(page.locator('.guide-player')).toContainText('Đang phát');
    expect((await speechData(page)).spoken[0].lang.toUpperCase()).toBe(`${language}-VN`);
  });
}
test('missing voice errors without speaking/counting and text remains available', async ({ page }) => {
  const receipts = await setup(page, { missingVoice: true });
  await page.goto(`/locations/${location}`);
  await page.getByRole('button', { name: 'Nghe thuyết minh', exact: true }).click();
  await expect(page.locator('.guide-player [role="alert"]')).toContainText('chưa có giọng', { timeout: 7000 });
  await expect(page.locator('.script-text')).toHaveText(text);
  expect((await speechData(page)).spoken).toHaveLength(0); expect(receipts).toHaveLength(0);
});
test('voiceschanged handles a late voice; failed audio falls back to TTS', async ({ page }) => {
  await setup(page, { audio: true, brokenAudio: true, lateVoice: true });
  await page.goto(`/locations/${location}`);
  await page.getByRole('button', { name: 'Nghe thuyết minh', exact: true }).click();
  await expect(page.locator('.guide-player')).toContainText('Đang chuẩn bị');
  await page.evaluate(() => (window as unknown as { supplyVoice: () => void }).supplyVoice());
  await expect(page.locator('.guide-player')).toContainText('Đang phát');
  await expect(page.locator('.guide-player')).toContainText('TTS');
});
