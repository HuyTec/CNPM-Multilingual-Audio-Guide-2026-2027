import { useEffect, useSyncExternalStore } from 'react';
import { outboxSnapshot, retryOutbox, startOutbox, subscribeOutbox } from '../lib/playbackOutbox';

export function PlaybackSyncStatus() {
  const state = useSyncExternalStore(subscribeOutbox, outboxSnapshot);
  useEffect(startOutbox, []);
  return <aside className="playback-sync" aria-label="Đồng bộ lượt nghe" aria-live="polite">
    {state.error && <p className="field-error">{state.error}</p>}
    {state.pending > 0 && <p>{new Intl.NumberFormat('vi-VN').format(state.pending)} lượt nghe đã lưu trên thiết bị, {state.syncing ? 'đang đồng bộ…' : 'sẽ tự gửi lại khi có mạng.'}</p>}
    {state.rejected > 0 && <p className="field-error">{new Intl.NumberFormat('vi-VN').format(state.rejected)} lượt nghe bị máy chủ từ chối. Dữ liệu vẫn được giữ trên thiết bị để kiểm tra.</p>}
    {(state.pending > 0 || !!state.error) && <button className="secondary-button" disabled={state.syncing} onClick={() => void retryOutbox()}>{state.syncing ? 'Đang đồng bộ…' : 'Thử đồng bộ lượt nghe'}</button>}
  </aside>;
}
