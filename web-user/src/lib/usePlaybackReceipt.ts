import { useSyncExternalStore } from 'react';
import { outboxSnapshot, recordPlayback, retryOutbox, subscribeOutbox } from './playbackOutbox';

/** Keep the original eventId on retry so a lost response cannot count a play twice. */
export function usePlaybackReceipt() {
  const state = useSyncExternalStore(subscribeOutbox, outboxSnapshot);
  return {
    failed: state.pending > 0 || !!state.error,
    retrying: state.syncing,
    record: (contentId: string, offline?: boolean) => { void recordPlayback(contentId, offline); },
    retry: retryOutbox,
  };
}
