const active = new Set<object>();
export function playbackActive(owner: object, busy: boolean) {
  if (busy) active.add(owner);
  else active.delete(owner);
}
export async function waitForPlayback(signal: AbortSignal) {
  while (active.size) {
    signal.throwIfAborted();
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  signal.throwIfAborted();
}
