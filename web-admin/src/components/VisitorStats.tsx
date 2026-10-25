import { useEffect, useState } from 'react';
import { number, dateTime } from '../lib/format';
import { Button } from './ui/Button';
type Snapshot = { generatedAt: string; activeWindowMinutes: number; platforms: {
  platform: string; totalInstallations: number; activeInstallations: number;
}[] };

export function VisitorStats({ authorization }: { authorization: string }) {
  const [result, setResult] = useState<{ key: string; data?: Snapshot; error?: string }>({ key: '' });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let busy = false;
    const refresh = async () => {
      if (busy || document.hidden || !authorization) return;
      busy = true;
      try {
        const response = await fetch('/api/admin/visitors', { headers: { Authorization: authorization },
          signal: controller.signal, cache: 'no-store' });
        if (!response.ok) throw new Error('Chưa tải được số khách. Hãy thử lại.');
        const data = await response.json() as Snapshot;
        if (!controller.signal.aborted) setResult({ key: authorization, data });
      } catch {
        if (!controller.signal.aborted) setResult({ key: authorization, error: 'Chưa tải được số khách. Hãy thử lại.' });
      } finally { busy = false; }
    };
    void refresh();
    const timer = setInterval(() => void refresh(), 15_000);
    document.addEventListener('visibilitychange', refresh);
    return () => { controller.abort(); clearInterval(timer); document.removeEventListener('visibilitychange', refresh); };
  }, [authorization, attempt]);
  const data = result.key === authorization ? result.data : undefined;
  return <section className="card visitor-stats-card" aria-labelledby="visitor-stats-title">
    <h2 id="visitor-stats-title">Khách vãng lai dùng ứng dụng</h2>
    <p className="muted">Tổng lượt cài đặt Android/hồ sơ trình duyệt đã mở từ khi bật thống kê; không phải lượt tải APK hay số người duy nhất. Số liệu này độc lập kỳ báo cáo lượt nghe.</p>
    {result.key === authorization && result.error ? <div role="status"><p>{result.error}</p><Button secondary onClick={() => setAttempt(value => value + 1)}>Thử lại thống kê khách</Button></div>
      : !data ? <p role="status">Đang tải số khách…</p>
      : <><div className="visitor-platform-grid">{data.platforms.map(item => <div className="visitor-platform-card" key={item.platform}>
        <h3>{item.platform === 'ANDROID' ? 'Android' : 'Web du khách'}</h3>
        <p className="visitor-total" style={{ fontVariantNumeric: 'tabular-nums' }}>Đã sử dụng <strong>{number(item.totalInstallations)}</strong></p>
        <p style={{ fontVariantNumeric: 'tabular-nums' }}>Hoạt động trong {number(data.activeWindowMinutes)} phút: <strong>{number(item.activeInstallations)}</strong></p>
      </div>)}</div><p className="muted">Cập nhật: {dateTime(data.generatedAt)}. Nhiều tab cùng trình duyệt tính một; xóa dữ liệu hoặc cài lại có thể tạo lượt cài đặt mới.</p></>}
  </section>;
}
