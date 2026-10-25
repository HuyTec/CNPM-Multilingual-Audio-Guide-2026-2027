import { useEffect, useRef } from 'react';
import type { PackageManifest } from '../lib/packages';

export function PackageDownloadDialog({ manifest, onConfirm, onCancel }: {
  manifest: PackageManifest; onConfirm: () => void; onCancel: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const cancel = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    const element = dialog.current!;
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    element.showModal();
    cancel.current?.focus();
    return () => { element.close(); if (previous?.isConnected) previous.focus(); };
  }, []);
  const bytes = manifest.sizeBytes + manifest.assets.reduce((sum, asset) => sum + asset.sizeBytes, 0);
  return <dialog className="package-download-dialog" ref={dialog} aria-labelledby="download-title" aria-describedby="download-description" onCancel={event => { event.preventDefault(); onCancel(); }}>
    <p className="eyebrow">{manifest.isBase ? 'Gói cơ bản' : 'Gói bổ sung'}</p>
    <h2 id="download-title">Xác nhận tải nội dung?</h2>
    <p>Dung lượng: {new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(bytes / 1024)} KB.</p>
    <p id="download-description">Gói mới chỉ thay thế nội dung hiện có sau khi tải và xác minh thành công.</p>
    <div className="player-actions"><button ref={cancel} className="secondary-button" onClick={onCancel}>Hủy</button><button className="primary-button" onClick={onConfirm}>Xác nhận tải</button></div>
  </dialog>;
}
