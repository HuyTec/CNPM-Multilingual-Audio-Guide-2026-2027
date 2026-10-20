import { useEffect, useRef, useState } from 'react';
import type { TourLocation } from '../lib/guide';
import { districtOne, isDistrictOneStop, loadGoogleMaps } from '../lib/googleMaps';

export default function TourMap({ items, language, scriptType }: { items: TourLocation[]; language: string; scriptType: string }) {
  const container = useRef<HTMLDivElement>(null);
  const [state, setState] = useState<'loading' | 'ready' | 'error' | 'denied'>('loading');
  const key = import.meta.env.VITE_GOOGLE_MAPS_API_KEY?.trim();
  const stopCount = items.filter(isDistrictOneStop).length;
  useEffect(() => {
    if (!key || !stopCount) return;
    let active = true;
    let dispose = () => {};
    const timer = setTimeout(() => { if (active) setState('error'); }, 20_000);
    const authFailure = () => { if (active) { clearTimeout(timer); setState('denied'); } };
    window.addEventListener('google-maps-auth-failure', authFailure);
    void loadGoogleMaps(key).then(maps => {
      if (!active || !container.current) return;
      const host = container.current;
      const map = new maps.Map(host, {
        center: districtOne.center, zoom: 15, minZoom: 14, maxZoom: 19,
        restriction: { latLngBounds: districtOne.bounds, strictBounds: true },
        mapId: import.meta.env.VITE_GOOGLE_MAPS_MAP_ID || 'DEMO_MAP_ID',
        mapTypeControl: false, streetViewControl: false, fullscreenControl: false,
        clickableIcons: false, gestureHandling: 'cooperative', keyboardShortcuts: true,
      });
      const ready = maps.event.addListenerOnce(map, 'tilesloaded', () => {
        if (active) { clearTimeout(timer); setState(current=>current==='denied'?current:'ready'); }
      });
      const markers = items.filter(isDistrictOneStop).map(item => {
        const link = document.createElement('a'); link.className = 'map-marker';
        link.href = `/locations/${item.locationId}?${new URLSearchParams({ language, scriptType })}`;
        link.setAttribute('aria-label', `Mở ${item.name}`); link.textContent = '●';
        const marker = new maps.marker.AdvancedMarkerElement({ map,
          position: { lat: item.latitude, lng: item.longitude }, title: item.name });
        marker.append(link);
        return marker;
      });
      // Keep a fixed camera instead of fitting neighbouring-district fixtures.
      dispose = () => {
        maps.event.removeListener(ready);
        markers.forEach(marker => { marker.map = null; marker.remove(); });
        maps.event.clearInstanceListeners(map); host.replaceChildren();
      };
    }).catch((error: unknown) => {
      if (active) {
        const detail=error instanceof Error?error.message:'Unknown error';
        console.warn('TravelVoice map setup:',detail.replace(/AIza[\w-]+/g,'[redacted]').replace(/https?:\/\/\S+/g,'[URL]'));
        clearTimeout(timer); setState(current=>current==='denied'?current:'error');
      }
    });
    return () => { active = false; clearTimeout(timer); window.removeEventListener('google-maps-auth-failure', authFailure); dispose(); };
  }, [items, key, language, scriptType, stopCount]);
  return <section className="map-section" aria-label="Bản đồ địa điểm">
    <h2>Bản đồ Google Maps · Quận 1</h2>
    {!stopCount ? <p role="status">Không có điểm thuộc tuyến Quận 1 trong kết quả này. Bạn vẫn có thể chọn bài bên dưới.</p>
      : !key ? <p role="status">Bản đồ Google Maps chưa được cấu hình. Bạn vẫn có thể chọn địa điểm trong danh sách bên dưới.</p>
      : <><div ref={container} className="tour-map" role="region" aria-label="Google Maps, vùng tham quan Quận 1" />
        {state === 'loading' && <p role="status">Đang tải Google Maps…</p>}
        {state === 'ready' && <p role="status">Bản đồ đã sẵn sàng.</p>}
        {state === 'error' && <p role="alert">Google Maps chưa tải được. Hãy kiểm tra kết nối hoặc tiếp tục chọn địa điểm bên dưới.</p>}</>}
    {state === 'denied' && <p role="alert">Google Maps chưa được cấp quyền cho ứng dụng này. Bạn vẫn có thể chọn địa điểm trong danh sách bên dưới.</p>}
    <p className="player-hint">Khung kéo và thu phóng cố định quanh Quận 1; chỉ ghim các điểm thuộc tuyến Quận 1. Đây là vùng demo hình chữ nhật, chưa phải đường biên hành chính.</p>
    <p className="player-hint">Bản đồ cần mạng; không tải bản đồ nền vào gói offline. Danh sách bên dưới là cách chọn địa điểm bằng bàn phím.</p>
  </section>;
}
