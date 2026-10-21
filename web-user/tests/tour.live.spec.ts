import { expect, test } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test.describe('District tour — live data', () => {
  test.skip(process.env.LIVE_GUIDE_API !== '1', 'Requires real backend');
  test('catalog opens exact selected location and all six languages / two modes', async ({ page, request }, info) => {
    await page.goto('/');
    await expect(page.getByRole('list', { name: 'Danh sách địa điểm' }).getByRole('listitem')).toHaveCount(7);
    await page.locator('select[name="language"]').selectOption('FR');
    await page.getByRole('link', { name: /Dinh Độc Lập/ }).click();
    await expect(page.getByRole('heading', { name: 'Dinh Độc Lập', exact: true })).toBeVisible();
    for (const language of ['VI', 'EN', 'FR', 'JA', 'ZH', 'RU']) for (const type of ['FULL', 'SHORT']) {
      await page.getByLabel('Ngôn ngữ yêu cầu').selectOption(language);
      await page.getByRole('radio', { name: type === 'FULL' ? 'Đầy đủ' : 'Ngắn gọn' }).check();
      const response = await request.get(`/api/locations/20000000-0000-0000-0000-000000000101/guide?language=${language}&scriptType=${type}`);
      expect(response.status()).toBe(200);
      const guide = await response.json() as { scriptText: string };
      await expect(page.locator('.script-text')).toHaveText(guide.scriptText);
      await expect(page.locator('.script-text')).toHaveAttribute('lang', language.toLowerCase());
    }
    await page.screenshot({ path: info.outputPath('six-language-reader.png'), fullPage: true });
    const axe = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
    expect(axe.violations).toEqual([]);
  });
});
