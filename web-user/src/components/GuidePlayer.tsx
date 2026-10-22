import { useEffect, useState, useSyncExternalStore } from 'react';
import type { Guide } from '../lib/guide';
import { GuidePlayback } from '../lib/playback';
import { usePlaybackReceipt } from '../lib/usePlaybackReceipt';

const number = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });
export function GuidePlayer({ guide }: { guide: Guide }) {
  const receipt = usePlaybackReceipt();
  const [controller] = useState(() => new GuidePlayback(guide, () => {
    // A playback receipt is sent only after the real media/TTS start callback.
    receipt.record(guide.contentId);
  }));
  const state = useSyncExternalStore(controller.subscribe, controller.snapshot);
  useEffect(() => {
    const visibility = () => { if (document.hidden) controller.pause(); };
    document.addEventListener('visibilitychange', visibility);
    window.addEventListener('offline', controller.networkLost);
    return () => {
      document.removeEventListener('visibilitychange', visibility);
      window.removeEventListener('offline', controller.networkLost); controller.stop();
    };
  }, [controller]);
  const isTts = state.source === 'tts';
  const active = ['playing', 'preparing'].includes(state.status);
  const status = { idle: 'Sẵn sàng nghe', preparing: 'Đang chuẩn bị…', playing: 'Đang phát', paused: 'Đã tạm dừng', ended: 'Đã nghe hết bài', error: 'Chưa thể phát' }[state.status];
  const max = isTts ? Math.max(0, state.total - 1) : state.total;
  return <section className="guide-player" aria-label="Nghe thuyết minh">
    <h2>Nghe câu chuyện</h2>
    <p role="status">{status}{state.source ? ` · ${isTts ? 'Giọng đọc văn bản (TTS)' : 'Audio thu sẵn'}` : ''}</p>
    {state.message && <p className={state.status === 'error' ? 'field-error' : 'player-hint'} role={state.status === 'error' ? 'alert' : undefined}>{state.message}</p>}
    <div className="player-actions">
      <button className="primary-button" onClick={active ? controller.pause : controller.play}>
        {active ? 'Tạm dừng' : state.status === 'paused' ? 'Tiếp tục' : state.status === 'error' ? 'Thử lại' : state.status === 'ended' ? 'Nghe lại' : 'Nghe thuyết minh'}
      </button>
      <button className="secondary-button" onClick={controller.stop} disabled={state.status === 'idle'}>Dừng</button>
      <label><span id="playback-rate-label">Tốc độ</span>
        <select aria-labelledby="playback-rate-label" name="playbackRate" value={state.rate} onChange={event => controller.setRate(Number(event.target.value))}>
          {[0.75, 1, 1.25, 1.5].map(rate => <option key={rate} value={rate}>{new Intl.NumberFormat('vi-VN').format(rate)}×</option>)}
        </select>
      </label>
    </div>
    {state.total > 0 && <div className="playback-position">
      <label htmlFor="playback-position">{isTts ? `Đoạn ${number.format(state.position + 1)} / ${number.format(state.total)}`
        : `${number.format(state.position)} / ${number.format(state.total)} giây`}</label>
      <input id="playback-position" name="position" type="range" min="0" max={max} step="1" value={Math.min(state.position, max)}
        aria-valuetext={isTts ? `Đoạn ${number.format(state.position + 1)}` : `${number.format(state.position)} giây`}
        onChange={event => controller.seek(Number(event.target.value))} />
      <div className="player-actions">
        <button className="secondary-button" onClick={() => controller.seek(state.position - (isTts ? 1 : 10))}>{isTts ? 'Đoạn trước' : 'Lùi 10 giây'}</button>
        <button className="secondary-button" onClick={() => controller.seek(state.position + (isTts ? 1 : 10))}>{isTts ? 'Đoạn sau' : 'Tiến 10 giây'}</button>
      </div>
      {isTts && <p className="player-hint">{state.voice}. Vị trí theo đoạn văn; tạm dừng/đổi tốc độ có thể đọc lại từ gần từ cuối cùng trình duyệt báo.</p>}
    </div>}
    {receipt.failed && <div><p className="player-hint" role="status">Lượt nghe chưa gửi được đến máy chủ. Bạn vẫn có thể tiếp tục nghe.</p>
      <button className="secondary-button" disabled={receipt.retrying} onClick={() => void receipt.retry()}>{receipt.retrying ? 'Đang gửi lại…' : 'Gửi lại lượt nghe'}</button></div>}
  </section>;
}
