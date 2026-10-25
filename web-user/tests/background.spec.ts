import { expect, test } from '@playwright/test';

test('production service worker drains IndexedDB on a sync event and opens exact notification location', async ({ page, context }) => {
  const requests: { eventId: string; contentId: string }[] = [];
  await context.route('**/api/playbacks', route => {
    const data = route.request().postDataJSON(); requests.push(data);
    return route.fulfill({ json: { eventId: data.eventId, created: true } });
  });
  await page.goto('/offline');
  await page.evaluate(() => navigator.serviceWorker.ready.then(() => undefined));
  await expect.poll(() => page.evaluate(() => !!navigator.serviceWorker.controller)).toBe(true);
  const cdp = await context.newCDPSession(page);
  let registrationId = '';
  cdp.on('ServiceWorker.workerRegistrationUpdated', ({ registrations }) => {
    registrationId = registrations.find((item: { scopeURL: string }) => item.scopeURL === 'http://127.0.0.1:4175/')?.registrationId || registrationId;
  });
  await cdp.send('ServiceWorker.enable');
  await expect.poll(() => registrationId).not.toBe('');
  // Block the page's online sender. This row must be consumed by the actual worker.
  await page.evaluate(async () => {
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: false });
    const core = await import('/playback-outbox.js' as string);
    await core.enqueue({ eventId: '41000000-0000-0000-0000-000000000001', contentId: '31000000-0000-0000-0000-000000000001', occurredAt: new Date().toISOString(), offline: true });
  });
  await cdp.send('ServiceWorker.dispatchSyncEvent', { origin: 'http://127.0.0.1:4175', registrationId, tag: 'travelvoice-playbacks', lastChance: false });
  await expect.poll(() => requests.length).toBe(1);
  await expect.poll(() => page.evaluate(async () => (await (await import('/playback-outbox.js' as string)).listReceipts()).length)).toBe(0);
  // Dispatch through the installed worker's real notificationclick listener without OS UI.
  const worker = context.serviceWorkers()[0];
  await worker.evaluate(async () => {
    const data = { locationId: '20000000-0000-0000-0000-000000000101', language: 'EN' };
    let completion: Promise<unknown> | undefined;
    const event = new Event('notificationclick');
    Object.assign(event, { notification: { data, close() {} }, waitUntil: (promise: Promise<unknown>) => { completion = promise; } });
    self.dispatchEvent(event); await completion;
  });
  await expect(page).toHaveURL('/locations/20000000-0000-0000-0000-000000000101?language=EN&scriptType=FULL');
  expect(requests).toHaveLength(1);
});
