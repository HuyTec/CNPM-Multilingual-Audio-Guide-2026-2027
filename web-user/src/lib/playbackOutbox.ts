import { installationId } from './visitor';
type Receipt = { eventId: string; contentId: string; occurredAt: string; offline: boolean; installationId?: string; platform?: 'WEB' };
type StoredReceipt = Receipt & { rejected: boolean; nextAttempt: number; leaseUntil: number };
type Core = { enqueue: (receipt: Receipt) => Promise<void>; listReceipts: () => Promise<StoredReceipt[]>;
  flushReceipts: (force?: boolean) => Promise<{ pending: number }> };
export type OutboxState = { pending: number; rejected: number; syncing: boolean; error: string };
let state: OutboxState = { pending: 0, rejected: 0, syncing: false, error: '' };
const listeners = new Set<() => void>();
const core = () => {
  const url = new URL('/playback-outbox.js', window.location.origin).href;
  return import(/* @vite-ignore */ url) as Promise<Core>;
};
const update = (patch: Partial<OutboxState>) => { state = { ...state, ...patch }; listeners.forEach(listener => listener()); };
export const outboxSnapshot = () => state;
export const subscribeOutbox = (listener: () => void) => { listeners.add(listener); return () => { listeners.delete(listener); }; };
let sending: Promise<void> | undefined;
let syncRequested = false;
let forceRequested = false;
async function registerSync() {
  const registration = await navigator.serviceWorker?.getRegistration();
  const sync = (registration as (ServiceWorkerRegistration & { sync?: { register: (tag: string) => Promise<void> } }) | undefined)?.sync;
  if (sync) await sync.register('travelvoice-playbacks');
}
export async function refreshOutbox() {
  const items = await (await core()).listReceipts();
  update({ pending: items.filter(item => !item.rejected).length, rejected: items.filter(item => item.rejected).length });
}
export function syncOutbox(force = false) {
  syncRequested = true; forceRequested ||= force;
  if (sending) return sending;
  sending = (async () => {
    update({ syncing: true });
    try {
      while (syncRequested) {
        syncRequested = false; const force = forceRequested; forceRequested = false;
        if (navigator.onLine) await (await core()).flushReceipts(force);
        await refreshOutbox();
        if (state.pending) await registerSync().catch(() => {});
      }
    } catch { update({ error: 'Không truy cập được bộ nhớ lượt nghe. Hãy cho phép lưu dữ liệu trang rồi thử lại.' }); }
    finally { update({ syncing: false }); sending = undefined; }
  })();
  return sending;
}
const unsaved = new Map<string, Receipt>();
let persisting: Promise<void> | undefined;
export async function recordPlayback(contentId: string, offline = !navigator.onLine) {
  const receipt: Receipt = { eventId: crypto.randomUUID(), contentId, occurredAt: new Date().toISOString(), offline };
  const id = installationId();
  if (id) { receipt.installationId = id; receipt.platform = 'WEB'; }
  unsaved.set(receipt.eventId, receipt);
  await persistUnsaved();
  if (unsaved.has(receipt.eventId) && !state.error) await persistUnsaved();
  await syncOutbox();
}
function persistUnsaved() {
  if (persisting) return persisting;
  persisting = (async () => {
    try {
      const storage = await core();
      for (const [id, receipt] of unsaved) { await storage.enqueue(receipt); unsaved.delete(id); }
      update({ error: '' }); await refreshOutbox();
      await registerSync().catch(() => {});
    } catch { update({ error: 'Chưa lưu được lượt nghe trên thiết bị. Lượt này có thể mất nếu đóng trang; hãy cho phép lưu dữ liệu và thử lại.' }); }
    finally { persisting = undefined; }
  })();
  return persisting;
}
export async function retryOutbox(force = false) { await persistUnsaved(); await syncOutbox(force); }
export function startOutbox() {
  const sync = () => { void retryOutbox(); };
  const reconnected = () => { void retryOutbox(true); };
  const visible = () => { if (!document.hidden) sync(); };
  const changed = () => { void refreshOutbox().catch(() => {}); };
  window.addEventListener('online', reconnected); document.addEventListener('visibilitychange', visible);
  navigator.serviceWorker?.addEventListener('message', changed);
  const timer = window.setInterval(() => { void retryOutbox(); }, 15_000);
  void retryOutbox();
  return () => { window.removeEventListener('online', reconnected); document.removeEventListener('visibilitychange', visible);
    navigator.serviceWorker?.removeEventListener('message', changed); clearInterval(timer); };
}
