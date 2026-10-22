import { useState } from 'react';

type Receipt = { eventId: string; contentId: string };

/** Keep the original eventId on retry so a lost response cannot count a play twice. */
export function usePlaybackReceipt() {
  const [failed, setFailed] = useState<Receipt[]>([]);
  const [retrying, setRetrying] = useState(false);
  async function send(receipt: Receipt) {
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), 10_000);
    try {
      const response = await fetch('/api/playbacks', { method: 'POST', signal: controller.signal,
        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(receipt) });
      if (!response.ok) throw new Error();
      setFailed(items => items.filter(item => item.eventId !== receipt.eventId));
    } catch {
      setFailed(items => items.some(item => item.eventId === receipt.eventId) ? items : [...items, receipt]);
    } finally { window.clearTimeout(timer); }
  }
  return {
    failed: failed.length > 0,
    retrying,
    record: (contentId: string) => { void send({ eventId: crypto.randomUUID(), contentId }); },
    retry: async () => { setRetrying(true); await Promise.all(failed.map(send)); setRetrying(false); },
  };
}
