import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { TourLocation } from '../lib/guide';
import { distanceMeters, hasMapCoordinates } from '../lib/osmMap';
import type { DevicePosition } from '../lib/osmMap';
import { LocationResults } from './LocationResults';

const number = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

export function NearbyPlaces({ items, language, scriptType, radius, onRadius, position, gpsStatus, gpsError, locate, loading, error, errorMessage, retry }: {
  items: TourLocation[]; language: string; scriptType: string; radius: number; onRadius: (radius: number) => void;
  position: DevicePosition | null; gpsStatus: string; gpsError: string; locate: () => void;
  loading: boolean; error: boolean; errorMessage?: string; retry: () => void;
}) {
  const [params, setParams] = useSearchParams();
  const expanded = params.get('nearbyExpanded') === 'true';
  const nearby = useMemo(() => position ? items.filter(hasMapCoordinates)
    .map(item => ({ ...item, distance: distanceMeters(position, item) }))
    .filter(item => item.distance <= radius)
    .sort((a, b) => a.distance - b.distance || a.name.localeCompare(b.name, 'vi-VN')) : [], [items, position, radius]);
  return <section className="nearby-section" aria-labelledby="nearby-heading">
    <div className="nearby-title"><div><p className="eyebrow">Khám phá quanh bạn</p><h2 id="nearby-heading">Địa điểm gần vị trí của bạn</h2></div>
      <button className="secondary-button" onClick={locate} disabled={gpsStatus === 'loading'}>{gpsStatus === 'loading' ? 'Đang lấy vị trí…' : position ? 'Cập nhật vị trí' : 'Lấy vị trí hiện tại'}</button>
    </div>
    <div className="scan-control"><label htmlFor="scan-radius">Phạm vi quét <output htmlFor="scan-radius">{number.format(radius)} m</output></label>
      <input id="scan-radius" name="radius" type="range" min="50" max="500" step="1" value={radius} onChange={event => onRadius(Number(event.target.value))} aria-valuetext={`${number.format(radius)} mét`} aria-describedby="scan-help" />
      <div className="scan-limits" aria-hidden="true"><span>50 m</span><span>500 m</span></div>
    </div>
    <p id="scan-help" className="player-hint">Kéo hoặc dùng phím mũi tên để đổi phạm vi. Khoảng cách đường chim bay được sắp từ gần nhất đến xa dần trong kết quả tìm kiếm.</p>
    {gpsError && <p className="field-error" role="alert">{gpsError}{position && ' Đang giữ kết quả theo vị trí lấy thành công lần trước.'}</p>}
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
