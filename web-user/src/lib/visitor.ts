/** One anonymous browser-profile identifier, shared by tabs. No email or hardware ID. */
export function installationId(): string | undefined {
  try {
    const key = 'travelvoice-visitor-v1';
    const saved = localStorage.getItem(key);
    if (saved && /^[a-f0-9-]{36}$/i.test(saved)) return saved;
    const value = crypto.randomUUID();
    localStorage.setItem(key, value);
    return value;
  } catch { return undefined; }
}

export function startVisitorPresence() {
  let sending = false;
  const heartbeat = async () => {
    if (sending || document.hidden || !navigator.onLine) return;
    const id = installationId();
    if (!id) return;
    sending = true;
    try {
      await fetch('/api/visitors/heartbeat', { method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ installationId: id, platform: 'WEB' }), signal: AbortSignal.timeout(10_000) });
    } catch { /* Content remains usable offline; presence never blocks playback. */ }
    finally { sending = false; }
  };
  const refresh = () => { void heartbeat(); };
  refresh();
  const timer = setInterval(refresh, 60_000);
  window.addEventListener('online', refresh);
  document.addEventListener('visibilitychange', refresh);
  return () => { clearInterval(timer); window.removeEventListener('online', refresh); document.removeEventListener('visibilitychange', refresh); };
}
