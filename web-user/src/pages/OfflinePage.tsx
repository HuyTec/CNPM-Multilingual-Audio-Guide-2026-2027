import {checkPackageUpdates, usePackageUpdates} from '../lib/packageUpdates';
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { PackageDownloadDialog } from '../components/PackageDownloadDialog';
import { deleteOptionalPackage, downloadPackage, packageSize, readInstalledPackages } from '../lib/packages';
import type { PackageManifest } from '../lib/packages';

export function OfflinePage() {
  const available = usePackageUpdates();
  const [storage, setStorage] = useState<Awaited<ReturnType<typeof readInstalledPackages>>>();
  const installed = storage?.packages ?? [];
  const [message, setMessage] = useState('Đang kiểm tra nội dung đã lưu…');
  const [busy, setBusy] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [shellReady, setShellReady] = useState(false);
  const [online, setOnline] = useState(navigator.onLine);
  const [progress, setProgress] = useState<{ bytes: number; total: number }>();
  const [retry, setRetry] = useState<PackageManifest | null>(null);
  const [pending, setPending] = useState<PackageManifest | null>(null);
  const transfer = useRef<AbortController | null>(null);
  const baseReady = installed.some(item => item.payload.isBase);
  useEffect(() => {
    let active = true;
    const refresh = () => {
      void readInstalledPackages().then(value => {
        if (!active) return;
        setStorage(value);
        if (value.invalidScopes.length) setMessage('Chưa đọc/xác minh được bộ nhớ của một số gói. Hãy tải lại gói lỗi khi có mạng.');
        else setMessage(value.packages.length ? 'Nội dung đã lưu đã qua xác minh.' : 'Chưa có nội dung offline. Hãy kết nối mạng để chuẩn bị lần đầu.');
      }).catch(() => {
        if (active) setMessage('Chưa đọc/xác minh được bộ nhớ. Hãy kiểm tra quyền lưu trữ rồi tải lại khi có mạng.');
      });
    };
    const control = () => {
      const worker = navigator.serviceWorker?.controller;
      if (!worker) { setShellReady(false); return; }
      const build = new URL(worker.scriptURL).searchParams.get('build');
      void caches.match('/index.html', { cacheName: 'travelvoice-shell-' + build }).then(response => {
        if (active) setShellReady(Boolean(response));
      }).catch(() => { if (active) setShellReady(false); });
    };
    const network = () => {
      setOnline(navigator.onLine);
      if (!navigator.onLine) transfer.current?.abort(new DOMException('Mất kết nối', 'NetworkError'));
    };
    refresh();
    control();
    navigator.serviceWorker?.addEventListener('controllerchange', control);
    window.addEventListener('online', network);
    window.addEventListener('offline', network);
    document.addEventListener('visibilitychange', control);
    return () => {
      active = false;
      transfer.current?.abort();
      navigator.serviceWorker?.removeEventListener('controllerchange', control);
      window.removeEventListener('online', network);
      window.removeEventListener('offline', network);
      document.removeEventListener('visibilitychange', control);
    };
  }, []);
  async function check() {
    setBusy(true); setMessage('Đang kiểm tra phiên bản trên máy chủ…');
    try {
      const latest = await checkPackageUpdates({ force: true });
      if (!latest.length) setMessage('Máy chủ chưa có gói đã phê duyệt để tải.');
      else if (baseReady && latest.every(manifest =>
        installed.some(item => item.stored.scope === manifest.scope && item.stored.manifest.version === manifest.version))) {
        setMessage('Nội dung đã cài đang ở phiên bản mới nhất.');
      } else setMessage('Có gói để tải hoặc cập nhật. Chỉ tải sau khi bạn xác nhận.');
    } catch { setMessage('Không kiểm tra được cập nhật. Nội dung đã cài vẫn giữ nguyên.'); }
    finally { setBusy(false); }
  }
  async function install(manifest: PackageManifest) {
    setPending(null); setBusy(true); setDownloading(true); setRetry(null);
    const controller = new AbortController();
    transfer.current = controller;
    const total = packageSize(manifest);
    setProgress({ bytes: 0, total });
    try {
      await downloadPackage(manifest, controller.signal, (message, bytes) => {
        setMessage(message);
        if (bytes !== undefined) setProgress({ bytes, total });
      });
      setMessage('Đã cài nội dung được xác minh.');
      try { setStorage(await readInstalledPackages()); }
      catch { setMessage('Đã cài gói nhưng chưa đọc lại được bộ nhớ. Hãy mở lại trang để kiểm tra.'); }
    } catch (error) {
      if (controller.signal.reason?.name === 'NetworkError') {
        setMessage('Đã mất kết nối. Dữ liệu tải dở đã được bỏ, phiên bản cũ vẫn được giữ.');
        setRetry(manifest);
      } else if (controller.signal.aborted) setMessage('Đã hủy. Nội dung cũ vẫn nguyên vẹn.');
      else {
        setMessage(error instanceof Error ? error.message : 'Không cài được gói.');
        setRetry(manifest);
      }
    } finally { transfer.current = null; setBusy(false); setDownloading(false); setProgress(undefined); }
  }
  async function remove() {
    if (!window.confirm('Xóa gói bổ sung? Nội dung cơ bản vẫn được giữ.')) return;
    setBusy(true);
    try { await deleteOptionalPackage('OPTIONAL'); setStorage(await readInstalledPackages()); setMessage('Đã xóa gói bổ sung. Nội dung cơ bản được giữ.'); }
    catch { setMessage('Chưa xóa được. Hãy thử lại.'); }
    finally { setBusy(false); }
  }
  return <section className="offline-page">
    <title>Nội dung offline · TravelVoice</title><h1>Nội dung offline</h1>
    <p>Lưu bài thuyết minh trong trình duyệt này để đọc khi mất mạng.</p>
    {storage && !baseReady && <p className="fallback-note">Hãy kết nối mạng và chọn “Tải gói” để lưu nội dung lần đầu.</p>}
    {baseReady && shellReady && <p className="fallback-note"><strong>Sẵn sàng ngoại tuyến.</strong> Mở lại cùng địa chỉ bằng trình duyệt này để dùng bài đã lưu. Bản đồ nền cần mạng.</p>}
    {!shellReady && <p className="fallback-note">Web chưa sẵn sàng mở lại khi mất mạng. Hãy cho phép lưu trữ và tải lại trang khi có mạng.</p>}
    {!online && <p>Đang mất kết nối. Bạn có thể mở bài đã lưu bên dưới.</p>}
    <p role="status">{message}</p>
    {progress && <div className="package-progress">
      <label htmlFor="package-progress">Tiến độ tải {new Intl.NumberFormat('vi-VN', { style: 'percent' }).format(progress.bytes / progress.total)}</label>
      <progress id="package-progress" value={progress.bytes} max={progress.total} />
    </div>}
    <div className="player-actions"><button type="button" className="primary-button" disabled={busy || !online} onClick={() => void check()}>Kiểm tra cập nhật</button>
      {downloading && <button type="button" className="secondary-button" onClick={() => transfer.current?.abort()}>Hủy tải</button>}
      {retry && <button type="button" className="secondary-button" disabled={busy || !online} onClick={() => setPending(available.find(item => item.scope === retry.scope) ?? retry)}>Thử tải lại</button>}
    </div>
    <ul className="package-list">{available.map(manifest => {
      const current = installed.find(item => item.stored.scope === manifest.scope);
      const same = current?.stored.manifest.version === manifest.version;
      const size = packageSize(manifest);
      return <li key={manifest.scope}><h2>Nội dung có thể tải ngoại tuyến</h2>
        <p>{new Intl.NumberFormat('vi-VN', { style: 'unit', unit: 'kilobyte', maximumFractionDigits: 1 }).format(size / 1000)} · {same ? 'Đã mới nhất' : current ? 'Có bản cập nhật' : 'Chưa tải'}</p>
        <p className="version-text">Phiên bản: <span translate="no">{manifest.version}</span></p>
        {!same && <button type="button" className="secondary-button" disabled={busy || !online} onClick={() => setPending(manifest)}>{current ? 'Cập nhật gói' : 'Tải gói'}</button>}
      </li>;
    })}</ul>
    <h2>Đang lưu trên thiết bị</h2>
    {storage?.invalidScopes.includes('OPTIONAL') && <button type="button" className="secondary-button" disabled={busy} onClick={() => void remove()}>Xóa gói bổ sung bị lỗi</button>}
    {installed.length === 0 ? <p>{storage ? 'Chưa có gói đã xác minh.' : 'Chưa có thông tin về nội dung đã lưu.'}</p> : <ul className="package-list">{installed.map(({ stored, payload }) => <li key={stored.scope}>
      <h3>{stored.scope === 'WEB' ? 'Gói thuyết minh ngoại tuyến' : stored.manifest.isBase ? 'Gói cơ bản · được bảo vệ' : 'Nội dung tải thêm trước đây'}</h3>
      <p className="version-text">Phiên bản đã cài: <span translate="no">{stored.manifest.version}</span></p>
      <p>{new Intl.NumberFormat('vi-VN').format(payload.locations.length)} địa điểm · {new Intl.NumberFormat('vi-VN').format(payload.guides.length)} bài · đã cài {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(stored.installedAt))}</p>
      <p>{shellReady ? 'Có thể đọc nội dung đã tải khi offline.' : 'Đã lưu text; ứng dụng chưa xác nhận mở lại được offline.'}</p>
      <details className="installed-places"><summary>Xem {new Intl.NumberFormat('vi-VN').format(payload.locations.length)} địa điểm trong gói</summary>
        <ul>{payload.locations.map(place => <li key={place.locationId}><Link to={`/locations/${place.locationId}?language=VI&scriptType=FULL`}>{place.names?.vi || place.name}</Link></li>)}</ul>
      </details>
      {!stored.manifest.isBase && <button type="button" className="secondary-button" disabled={busy} onClick={() => void remove()}>Xóa nội dung bổ sung</button>}
    </li>)}</ul>}
    {pending && <PackageDownloadDialog manifest={pending}
      installedVersion={installed.find(item => item.stored.scope === pending.scope)?.stored.manifest.version}
      onConfirm={() => void install(pending)} onCancel={() => setPending(null)} />}
  </section>;
}
