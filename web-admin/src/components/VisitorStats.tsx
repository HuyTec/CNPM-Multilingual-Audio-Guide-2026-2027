import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { number, dateTime } from '../lib/format';
import { Button } from './ui/Button';
type Snapshot = { generatedAt: string; activeWindowMinutes: number; platforms: {
  platform: string; totalInstallations: number; activeInstallations: number;
}[] };
const windows = [5, 10, 15, 20, 25, 30, 60];

export function VisitorStats({ authorization }: { authorization: string }) {
  const [result, setResult] = useState<{ key: string; data?: Snapshot; error?: string }>({ key: '' });
  const [attempt, setAttempt] = useState(0);
  const [params, setParams] = useSearchParams();
  const requestedMinutes = Number(params.get('activeMinutes'));
  const minutes = windows.includes(requestedMinutes) ? requestedMinutes : 5;
  const requestKey = `${authorization}:${minutes}:${attempt}`;
  useEffect(() => {
    const controller = new AbortController();
    let busy = false;
    const refresh = async () => {
      if (busy || document.hidden || !authorization) return;
      busy = true;
      try {
        const response = await fetch(`/api/admin/visitors?minutes=${minutes}`, { headers: { Authorization: authorization },
          signal: controller.signal, cache: 'no-store', credentials: 'same-origin' });
        if (!response.ok) throw new Error('Chưa tải được số khách. Hãy thử lại.');
        const data = await response.json() as Snapshot;
        if (!controller.signal.aborted) setResult({ key: requestKey, data });
      } catch {
        if (!controller.signal.aborted) setResult({ key: requestKey, error: 'Chưa tải được số khách. Hãy thử lại.' });
      } finally { busy = false; }
    };
    void refresh();
    const timer = setInterval(() => void refresh(), 15_000);
    document.addEventListener('visibilitychange', refresh);
    return () => { controller.abort(); clearInterval(timer); document.removeEventListener('visibilitychange', refresh); };
  }, [authorization, attempt, minutes, requestKey]);
  const data = result.key === requestKey ? result.data : undefined;
  const web = data?.platforms.find(item => item.platform === 'WEB');
  const error = result.key === requestKey ? result.error : undefined;
  return <section className="card visitor-stats-card" aria-labelledby="visitor-stats-title">
    <div className="visitor-stats-heading">
      <div>
        <h2 id="visitor-stats-title">Hoạt động trên Web du khách</h2>
        <p className="muted">Đếm theo hồ sơ trình duyệt. Khoảng hoạt động chỉ áp dụng cho chỉ số gần đây.</p>
      </div>
      <div className="field">
        <label htmlFor="visitor-minutes">Khoảng hoạt động</label>
        <select id="visitor-minutes" name="activeMinutes" value={minutes} onChange={(event) => {
          const value = event.target.value;
          setParams((current) => {
            const next = new URLSearchParams(current);
            next.set('activeMinutes', value);
            return next;
          }, { preventScrollReset: true });
        }}>
          {windows.map((window) => <option key={window} value={window}>{number(window)} phút</option>)}
        </select>
      </div>
    </div>
    {error ? <div className="notice error"><p role="status">{error}</p><Button secondary onClick={() => setAttempt(value => value + 1)}>Thử lại</Button></div>
      : !data ? <p role="status">Đang tải hoạt động trong {number(minutes)} phút…</p>
      : <>
        <dl className="visitor-metrics">
          <div><dt>Hoạt động trong {number(data.activeWindowMinutes)} phút</dt><dd>{number(web?.activeInstallations ?? 0)}</dd></div>
          <div><dt>Đã từng truy cập</dt><dd>{number(web?.totalInstallations ?? 0)}</dd></div>
        </dl>
        <p className="muted report-updated">Cập nhật {dateTime(data.generatedAt)}</p>
      </>}
  </section>;
}
