import { expect, test } from '@playwright/test';

const contentId = '31000000-0000-0000-0000-000000000001';
test('durable queue survives navigation/reload and auto-syncs on reconnection using one eventId', async ({ page }) => {
  const received: { eventId: string; contentId: string; occurredAt: string; offline: boolean }[] = [];
  let fail = true;
  await page.route('**/api/playbacks', route => {
    const receipt = route.request().postDataJSON(); received.push(receipt);
    return fail ? route.fulfill({ status: 503 }) : route.fulfill({ json: { eventId: receipt.eventId, created: false } });
  });
  await page.goto('/offline');
  // Simulate a real media-start callback while the client reports offline.
  await page.evaluate(async contentId => {
    Object.defineProperty(navigator, 'onLine', { configurable: true, get: () => false });
    const outbox = await import('/src/lib/playbackOutbox.ts' as string);
    await outbox.recordPlayback(contentId);
  }, contentId);
  await expect(page.getByLabel('Đồng bộ lượt nghe')).toContainText('1 lượt nghe đã lưu');
  expect(received).toHaveLength(0);
  await page.reload();
  await expect.poll(() => received.length).toBe(1);
  await expect(page.getByLabel('Đồng bộ lượt nghe')).toContainText('1 lượt nghe đã lưu');
  fail = false;
  await page.evaluate(() => window.dispatchEvent(new Event('online')));
  await expect.poll(() => received.length).toBe(2);
  expect(received[1]).toEqual(received[0]); expect(received[0].offline).toBe(true);
  expect(Number.isFinite(Date.parse(received[0].occurredAt))).toBe(true);
  await expect(page.getByLabel('Đồng bộ lượt nghe')).not.toContainText('lượt nghe đã lưu');
});
test('atomic leases prevent concurrent tab sends; failed acknowledgments remain durable', async ({ page, context }) => {
  let count = 0;
  await context.route('**/api/playbacks', async route => {
    count++; await new Promise(resolve => setTimeout(resolve, 100));
    await route.fulfill({ json: { eventId: 'wrong-ack', created: true } });
  });
  await page.goto('/offline');
  await page.evaluate(async contentId => {
    const core = await import('/playback-outbox.js' as string);
    await core.enqueue({ eventId: crypto.randomUUID(), contentId, occurredAt: new Date().toISOString(), offline: true });
  }, contentId);
  const other = await context.newPage(); await other.goto('/offline');
  await Promise.all([page, other].map(tab => tab.evaluate(async () => {
    const core = await import('/playback-outbox.js' as string); await core.flushReceipts();
  })));
  expect(count).toBe(1);
  const stored = await page.evaluate(async () => (await import('/playback-outbox.js' as string)).listReceipts());
  expect(stored).toHaveLength(1); expect(stored[0].attempts).toBe(1);
});
test('permanent rejection retained without infinite retries', async ({ page }) => {
  let count = 0;
  await page.route('**/api/playbacks', route => { count++; return route.fulfill({ status: 404 }); });
  await page.goto('/offline');
  await page.evaluate(async contentId => (await import('/src/lib/playbackOutbox.ts' as string)).recordPlayback(contentId), contentId);
  await expect(page.getByLabel('Đồng bộ lượt nghe')).toContainText('bị máy chủ từ chối');
  await page.reload();
  await expect(page.getByLabel('Đồng bộ lượt nghe')).toContainText('bị máy chủ từ chối');
  expect(count).toBe(1);
});
test('storage failure is visible and keeps the same unsaved receipt for manual recovery', async ({ page }) => {
  const requests: { eventId: string }[] = [];
  await page.route('**/api/playbacks', route => {
    const data = route.request().postDataJSON(); requests.push(data);
    return route.fulfill({ json: { eventId: data.eventId, created: true } });
  });
  await page.goto('/offline');
  await page.evaluate(async contentId => {
    const original = indexedDB.open.bind(indexedDB);
    indexedDB.open = () => { throw new Error('Storage blocked'); };
    await (await import('/src/lib/playbackOutbox.ts' as string)).recordPlayback(contentId);
    indexedDB.open = original;
  }, contentId);
  await expect(page.getByLabel('Đồng bộ lượt nghe')).toContainText('bộ nhớ');
  expect(requests).toHaveLength(0);
  await page.getByRole('button', { name: 'Thử đồng bộ lượt nghe', exact: true }).click();
  await expect.poll(() => requests.length).toBe(1);
  await expect(page.getByLabel('Đồng bộ lượt nghe')).not.toContainText('bộ nhớ');
});
