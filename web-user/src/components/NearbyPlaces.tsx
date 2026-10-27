import { useEffect, useMemo, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { TourLocation } from '../lib/guide';
import { distanceMeters, hasMapCoordinates } from '../lib/osmMap';
import type { DevicePosition } from '../lib/osmMap';
import { LocationResults } from './LocationResults';

const number = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

export function NearbyPlaces({ items, language, scriptType, radius, onRadius, position, gpsStatus, gpsError, refining, locate, loading, error, errorMessage, retry, onClose }: {
  items: TourLocation[]; language: string; scriptType: string; radius: number; onRadius: (radius: number) => void;
  position: DevicePosition | null; gpsStatus: string; gpsError: string; refining: boolean; locate: () => void;
  loading: boolean; error: boolean; errorMessage?: string; retry: () => void; onClose: () => void;
}) {
  const closeButton = useRef<HTMLButtonElement>(null);
  useEffect(() => { closeButton.current?.focus({ preventScroll: true }); }, []);
  const [params, setParams] = useSearchParams();
  const expanded = params.get('nearbyExpanded') === 'true';
  const nearby = useMemo(() => position ? items.filter(hasMapCoordinates)
    .map(item => ({ ...item, distance: distanceMeters(position, item) }))
    .filter(item => item.distance <= radius)
    .sort((a, b) => a.distance - b.distance || a.name.localeCompare(b.name, 'vi-VN')) : [], [items, position, radius]);
  return <section id="nearby-panel" className="nearby-section" aria-labelledby="nearby-heading" onKeyDown={event => { if (event.key === 'Escape') onClose(); }}>
    <div className="nearby-title"><div><p className="eyebrow">Khám phá quanh bạn</p><h2 id="nearby-heading">Tìm địa điểm lân cận</h2></div>
      <button ref={closeButton} type="button" className="secondary-button nearby-close" aria-label="Đóng tìm kiếm lân cận" onClick={onClose}>✕</button>
    </div>
    <p className="player-hint">Cho phép truy cập vị trí để tìm các địa điểm gần bạn. Bạn vẫn có thể tìm theo tên, bản đồ hoặc Danh sách.</p>
    <div className="scan-control"><label htmlFor="scan-radius">Phạm vi quét <output htmlFor="scan-radius">{number.format(radius)} m</output></label>
      <input id="scan-radius" name="radius" type="range" min="100" max="500" step="1" value={radius} onChange={event => onRadius(Number(event.target.value))} aria-valuetext={`${number.format(radius)} mét`} aria-describedby="scan-help" />
      <div className="scan-limits" aria-hidden="true"><span>100 m</span><span>500 m</span></div>
    </div>
    <p id="scan-help" className="player-hint">Kéo hoặc dùng phím mũi tên để đổi bán kính. Kết quả xếp theo khoảng cách đường chim bay, từ gần đến xa.</p>
    <button type="button" className="secondary-button nearby-refresh" onClick={locate} disabled={gpsStatus === 'loading'}>{gpsStatus === 'loading' ? 'Đang lấy vị trí…' : position ? 'Cập nhật vị trí' : 'Lấy vị trí hiện tại'}</button>
    {refining && <p role="status">Đang thử lấy GPS chính xác hơn…{position && ' Bạn có thể dùng vị trí gần đúng trong lúc chờ.'}</p>}
    {gpsError && <p className="field-error" role="alert">{gpsError}{position && ' Đang giữ kết quả theo vị trí lấy thành công lần trước.'}</p>}
    {gpsError && <details className="location-settings"><summary>Hướng dẫn mở cài đặt vị trí</summary><p>Mở cài đặt trang trong Safari/Chrome, cho phép Vị trí và bật dịch vụ vị trí của điện thoại. Sau đó quay lại và bấm “Lấy vị trí hiện tại”. Trình duyệt không cho phép website tự mở cài đặt hệ thống.</p></details>}
    {position && <p className="gps-accuracy">Độ chính xác vị trí: khoảng ±{number.format(position.accuracy)} m.{position.accuracy > radius && ' Độ chính xác GPS lớn hơn phạm vi quét; kết quả có thể sai lệch. Hãy thử cập nhật vị trí ở nơi thoáng.'}</p>}
    <div role="status" aria-live="polite">
      {gpsStatus === 'loading' ? <p>Đang xác định vị trí hiện tại…</p>
        : !position ? <p>Bấm “Lấy vị trí hiện tại” và cho phép truy cập vị trí để tìm địa điểm gần bạn.</p>
          : loading ? <p>Đang tải địa điểm để quét…</p>
            : !error && <p>{nearby.length ? `Tìm thấy ${number.format(nearby.length)} địa điểm trong phạm vi ${number.format(radius)} m.` : `Chưa có địa điểm trong phạm vi ${number.format(radius)} m. Hãy mở rộng phạm vi hoặc thử tìm kiếm thủ công.`}</p>}
    </div>
    {error && <div role="alert"><p>{errorMessage || 'Chưa tải được địa điểm. Hãy kiểm tra kết nối rồi thử lại.'}</p><button className="secondary-button" onClick={retry}>Thử tải lại địa điểm</button></div>}
    {position && !loading && !error && gpsStatus !== 'loading' && <>
      <LocationResults items={expanded ? nearby : nearby.slice(0, 5)} language={language} scriptType={scriptType} label="Địa điểm gần bạn" page={expanded ? Number(params.get('nearbyPage')) || 0 : 0}
        onPage={page => { const next = new URLSearchParams(params); next.set('nearbyPage', String(page)); setParams(next); }} />
      {nearby.length > 5 && <button className="secondary-button" aria-expanded={expanded} onClick={() => {
        const next = new URLSearchParams(params); next.set('nearbyExpanded', String(!expanded)); next.delete('nearbyPage'); setParams(next);
      }}>{expanded ? 'Thu gọn còn 5 điểm gần nhất' : 'Xem tất cả địa điểm trong phạm vi'}</button>}
    </>}
  </section>;
}
