import {checkPackageUpdates, usePackageUpdates} from '../lib/packageUpdates';
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { PackageDownloadDialog } from '../components/PackageDownloadDialog';
import { deleteOptionalPackage, downloadPackage, installedPackages } from '../lib/packages';
import type { PackageManifest } from '../lib/packages';

export function OfflinePage() {
  const available=usePackageUpdates();
  const [installed, setInstalled] = useState<Awaited<ReturnType<typeof installedPackages>>>([]);
  const [message, setMessage] = useState('Đang kiểm tra nội dung đã lưu…');
  const [busy, setBusy] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [shellReady, setShellReady] = useState(!!navigator.serviceWorker?.controller);
  const [pending, setPending] = useState<PackageManifest | null>(null);
  const transfer = useRef<AbortController | null>(null);
  const baseReady = installed.some(item => item.stored.scope === 'BASE' && item.payload.isBase);
  useEffect(() => {
    void installedPackages().then(value => { setInstalled(value); setMessage(value.length ? 'Nội dung đã lưu đã qua xác minh.' : 'Chưa có nội dung offline. Hãy kết nối mạng để chuẩn bị lần đầu.'); })
      .catch(() => setMessage('Chưa đọc/xác minh được bộ nhớ. Hãy tải lại gói khi có mạng.'));
    const control = () => setShellReady(!!navigator.serviceWorker.controller);
    navigator.serviceWorker?.addEventListener('controllerchange', control);
    return () => { transfer.current?.abort(); navigator.serviceWorker?.removeEventListener('controllerchange', control); };
  }, []);
  async function check() {
    setBusy(true); setMessage('Đang kiểm tra phiên bản trên máy chủ…');
    try {
      await checkPackageUpdates({ force: true });
      setMessage('Đã kiểm tra. Chỉ tải sau khi bạn xác nhận.');
    } catch { setMessage('Không kiểm tra được cập nhật. Nội dung đã cài vẫn giữ nguyên.'); }
    finally { setBusy(false); }
  }
  async function install(manifest: PackageManifest) {
    setPending(null); setBusy(true); setDownloading(true); const controller = new AbortController(); transfer.current = controller;
    try {
      await downloadPackage(manifest, controller.signal, setMessage);
      setInstalled(await installedPackages());
      setMessage('Đã cài nội dung được xác minh. Nghe offline còn cần audio đã tải hoặc giọng đọc offline phù hợp.');
    } catch (error) { setMessage(controller.signal.aborted ? 'Đã hủy. Nội dung cũ vẫn nguyên vẹn.' : error instanceof Error ? error.message : 'Không cài được gói.'); }
    finally { transfer.current = null; setBusy(false); setDownloading(false); }
  }
  async function remove() {
    if (!window.confirm('Xóa gói bổ sung? Nội dung cơ bản vẫn được giữ.')) return;
    try { await deleteOptionalPackage('OPTIONAL'); setInstalled(await installedPackages()); setMessage('Đã xóa gói bổ sung. Nội dung cơ bản được giữ.'); }
    catch { setMessage('Chưa xóa được. Hãy thử lại.'); }
  }
  return <section className="offline-page">
    <title>Nội dung offline · TravelVoice</title><p className="eyebrow">Mang câu chuyện theo bạn</p><h1>Nội dung offline</h1>
    <p>Web và Android dùng cùng phiên bản gói, mã địa điểm và bài thuyết minh. Bản đồ nền cần mạng.</p>
    <p>Gói được lưu trong trình duyệt để dùng offline, không xuất hiện trong thư mục Downloads. Sau khi cài, mở danh sách địa điểm trong gói để đọc bài.</p>
    {!baseReady && <p className="fallback-note">Chưa có nội dung cơ bản đã xác minh. Hãy kiểm tra cập nhật và xác nhận tải gói cơ bản khi có mạng trước khi dùng offline.</p>}
    {!shellReady && <p className="fallback-note">Chưa xác nhận ứng dụng sẵn sàng mở lại khi mất mạng. Hãy dùng bản production/preview, cho phép lưu trữ và tải lại trang khi có mạng.</p>}
    <p role="status">{message}</p>
    <div className="player-actions"><button className="primary-button" disabled={busy} onClick={() => void check()}>Kiểm tra cập nhật</button>
      {downloading && <button className="secondary-button" onClick={() => transfer.current?.abort()}>Hủy tải</button>}</div>
    <ul className="package-list">{available.map(manifest => {
      const current = installed.find(item => item.stored.scope === manifest.scope);
      const same = current?.stored.manifest.version === manifest.version;
      const size = manifest.sizeBytes + manifest.assets.reduce((sum, item) => sum + item.sizeBytes, 0);
      return <li key={manifest.scope}><h2>{manifest.isBase ? 'Nội dung cơ bản' : 'Nội dung bổ sung'}</h2>
        <p>{new Intl.NumberFormat('vi-VN', { style: 'unit', unit: 'kilobyte', maximumFractionDigits: 1 }).format(size / 1024)} · {same ? 'Đã mới nhất' : current ? 'Có bản cập nhật' : 'Chưa tải'}</p>
        <p className="version-text">Phiên bản: <span translate="no">{manifest.version}</span></p>
        {!same && <button className="secondary-button" disabled={busy} onClick={() => setPending(manifest)}>{current ? 'Cập nhật gói' : 'Tải gói'}</button>}
      </li>;
    })}</ul>
    <h2>Đang lưu trên thiết bị</h2>
    {installed.length === 0 ? <p>Chưa có gói đã xác minh.</p> : <ul className="package-list">{installed.map(({ stored, payload }) => <li key={stored.scope}>
      <h3>{stored.manifest.isBase ? 'Gói cơ bản · được bảo vệ' : 'Gói bổ sung'}</h3>
      <p>{new Intl.NumberFormat('vi-VN').format(payload.locations.length)} địa điểm · {new Intl.NumberFormat('vi-VN').format(payload.guides.length)} bài · đã cài {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(stored.installedAt))}</p>
      <p>{shellReady ? 'Có thể đọc nội dung đã tải khi offline.' : 'Đã lưu text; ứng dụng chưa xác nhận mở lại được offline.'}</p>
      <details className="installed-places"><summary>Xem {new Intl.NumberFormat('vi-VN').format(payload.locations.length)} địa điểm trong gói</summary>
        <ul>{payload.locations.map(place => <li key={place.locationId}><Link to={`/locations/${place.locationId}?language=VI&scriptType=FULL`}>{place.names?.vi || place.name}</Link></li>)}</ul>
      </details>
      {!stored.manifest.isBase && <button className="secondary-button" disabled={busy} onClick={() => void remove()}>Xóa nội dung bổ sung</button>}
    </li>)}</ul>}
    {pending && <PackageDownloadDialog manifest={pending} onConfirm={() => void install(pending)} onCancel={() => setPending(null)} />}
  </section>;
}
