import { useEffect, useRef, useState } from 'react';
import { packageSize, type PackageManifest, type PackagePreview } from '../lib/packages';

const number = new Intl.NumberFormat('vi-VN');
const size = new Intl.NumberFormat('vi-VN', { style: 'unit', unit: 'kilobyte', maximumFractionDigits: 1 });

export function PackageDownloadDialog({ manifest, installedVersion, onConfirm, onCancel }: {
  manifest: PackageManifest; installedVersion?: string; onConfirm: () => void; onCancel: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const cancel = useRef<HTMLButtonElement>(null);
  const [preview, setPreview] = useState<PackagePreview>();
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const element = dialog.current!;
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    element.showModal();
    cancel.current?.focus();
    return () => { element.close(); if (previous?.isConnected) previous.focus(); };
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    const timeout = window.setTimeout(() => controller.abort(), 8000);
    const query = installedVersion ? '?' + new URLSearchParams({ installedVersion }) : '';
    void fetch(`${manifest.downloadUrl}/preview${query}`, {
      signal: controller.signal,
    }).then(async response => {
      if (!response.ok) throw new Error('Package preview unavailable');
      const data: PackagePreview = await response.json();
      if (active) setPreview(data);
    }).catch(() => {
      if (active) setFailed(true);
    }).finally(() => window.clearTimeout(timeout));
    return () => {
      active = false;
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [manifest.downloadUrl, installedVersion, attempt]);
  return <dialog className="package-download-dialog" ref={dialog} aria-labelledby="download-title" aria-describedby="download-description" onCancel={event => { event.preventDefault(); onCancel(); }}>
    <p className="eyebrow">{manifest.isBase ? 'Gói cơ bản' : 'Gói bổ sung'}</p>
    <h2 id="download-title">Xác nhận tải nội dung?</h2>
    <p>Dung lượng tải {size.format(packageSize(manifest) / 1000)}.</p>
    <div aria-live="polite">
      {preview && <>
        <p>Gói chứa {number.format(preview.locations)} địa điểm, {number.format(preview.guides)} bài thuyết minh và {number.format(preview.audioFiles)} tệp audio.</p>
        {preview.changes && <p>So với bản đang lưu: {number.format(preview.changes.added)} địa điểm mới,
          {' '}{number.format(preview.changes.updated)} đã cập nhật, {number.format(preview.changes.removed)} đã rút khỏi gói.</p>}
        {installedVersion && !preview.changes && <p>Máy chủ không còn bản cũ để đối chiếu chi tiết.</p>}
      </>}
      {!preview && <p>{failed ? 'Chưa xem được chi tiết gói. Bạn vẫn có thể xác nhận tải.' : 'Đang lấy thông tin gói…'}</p>}
    </div>
    {failed && <button type="button" className="secondary-button" onClick={() => { setFailed(false); setAttempt(value => value + 1); }}>Thử tải thông tin lại</button>}
    <p id="download-description">Gói mới chỉ thay thế nội dung hiện có sau khi tải và xác minh thành công.</p>
    <div className="player-actions"><button ref={cancel} type="button" className="secondary-button" onClick={onCancel}>Hủy</button><button type="button" className="primary-button" disabled={!preview && !failed} onClick={onConfirm}>Xác nhận tải</button></div>
  </dialog>;
}
