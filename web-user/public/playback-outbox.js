// Shared by the page and module service worker; one IndexedDB lease per receipt.
export const syncTag = 'travelvoice-playbacks';
async function transaction(mode, work) {
  const db = await new Promise((resolve, reject) => {
    const request = indexedDB.open('travelvoice-playbacks-v1', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('events', { keyPath: 'eventId' });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
    request.onblocked = () => reject(new Error('Playback storage blocked'));
  });
  return new Promise((resolve, reject) => {
    const tx = db.transaction('events', mode);
    let result;
    work(tx.objectStore('events'), value => { result = value; });
    tx.oncomplete = () => { db.close(); resolve(result); };
    tx.onabort = tx.onerror = () => { db.close(); reject(tx.error || new Error('Playback storage failed')); };
  });
}
export async function enqueue(receipt) {
  await transaction('readwrite', store => store.add({ ...receipt, attempts: 0, nextAttempt: 0, leaseUntil: 0, rejected: false }));
}
export async function listReceipts() {
  return transaction('readonly', (store, result) => { store.getAll().onsuccess = event => result(event.target.result); });
}
async function claim(force, excluded) {
  return transaction('readwrite', (store, result) => {
    store.getAll().onsuccess = event => {
      const now = Date.now();
      const item = event.target.result.find(item => !excluded.has(item.eventId) && !item.rejected && item.leaseUntil <= now && (force || item.nextAttempt <= now));
      if (!item) return;
      item.leaseUntil = now + 30_000; item.lease = crypto.randomUUID();
      store.put(item); result(item);
    };
  });
}
async function settle(item, action, status) {
  await transaction('readwrite', store => {
    store.get(item.eventId).onsuccess = event => {
      const current = event.target.result;
      if (!current || current.lease !== item.lease) return;
      if (action === 'delete') { store.delete(item.eventId); return; }
      current.attempts++; current.leaseUntil = 0; current.rejected = action === 'reject'; current.lastStatus = status;
      current.nextAttempt = Date.now() + Math.min(300_000, 5_000 * 2 ** Math.min(current.attempts - 1, 6));
      store.put(current);
    };
  });
}
/** Remove only acknowledged receipts. Lost responses reuse the original eventId. */
export async function flushReceipts(force = false) {
  const count = (await listReceipts()).length;
  const attempted = new Set();
  for (let index = 0; index < count; index++) {
    const item = await claim(force, attempted);
    if (!item) break;
    attempted.add(item.eventId);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 10_000);
    const { eventId, contentId, occurredAt, offline, installationId, platform } = item;
    try {
      const response = await fetch('/api/playbacks', { method: 'POST', signal: controller.signal,
        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ eventId, contentId, occurredAt, offline, installationId, platform }) });
      if (!response.ok) {
        const terminal = response.status >= 400 && response.status < 500 && ![408, 425, 429].includes(response.status);
        await settle(item, terminal ? 'reject' : 'retry', response.status);
      } else {
        const receipt = await response.json();
        if (receipt.eventId !== eventId || typeof receipt.created !== 'boolean') throw new Error('Invalid receipt');
        await settle(item, 'delete');
      }
    } catch { await settle(item, 'retry', 0); }
    finally { clearTimeout(timer); }
  }
  return { pending: (await listReceipts()).filter(item => !item.rejected).length };
}
