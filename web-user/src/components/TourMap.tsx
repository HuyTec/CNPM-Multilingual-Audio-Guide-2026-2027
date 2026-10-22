import { useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { useSearchParams } from 'react-router-dom';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { TourLocation } from '../lib/guide';
import { mapArea, hasMapCoordinates, osmAndLocationUrl } from '../lib/osmMap';
import { useBrowserLocation } from '../lib/useBrowserLocation';
import { LocationResults } from './LocationResults';
import { NearbyPlaces } from './NearbyPlaces';
import { CategoryFilter } from './CategoryFilter';

function popupFor(item: TourLocation, language: string, scriptType: string) {
  const content = document.createElement('div'); content.className = 'map-popup';
  const name = document.createElement('strong'); name.textContent = item.name; content.append(name);
  const guide = document.createElement('a');
  guide.href = `/locations/${item.locationId}?${new URLSearchParams({ language, scriptType })}`;
  guide.textContent = 'Mở bài thuyết minh'; content.append(guide);
  const osmand = document.createElement('a'); osmand.href = osmAndLocationUrl(item);
  osmand.target = '_blank'; osmand.rel = 'noreferrer'; osmand.textContent = 'Mở vị trí trong OsmAnd ↗'; content.append(osmand);
  return content;
}

export default function TourMap({ items, language, scriptType, toolbar, listOpen, closeList, loading, error, errorMessage, offline, retry }: {
  items: TourLocation[]; language: string; scriptType: string; toolbar: ReactNode;
  listOpen: boolean; closeList: () => void; loading: boolean; error: boolean; errorMessage?: string; offline: boolean; retry: () => void;
}) {
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const closeButton = useRef<HTMLButtonElement>(null);
  const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading');
  const [attempt, setAttempt] = useState(0);
  const [params, setParams] = useSearchParams();
  const rawRadius = Number(params.get('radius') ?? 100);
  const radius = Number.isFinite(rawRadius) ? Math.min(500, Math.max(50, Math.round(rawRadius))) : 100;
  const gps = useBrowserLocation();
  const stops = useMemo(() => items.filter(hasMapCoordinates), [items]);

  useEffect(() => {
    const host = container.current;
    if (!host) return;
    let active = true;
    const map = L.map(host, { center: [mapArea.center.lat, mapArea.center.lng], zoom: 13, minZoom: 2, maxZoom: 19,
      zoomControl: false, scrollWheelZoom: true, keyboard: true,
      fadeAnimation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
      zoomAnimation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches });
    mapRef.current = map;
    L.control.zoom({ position: 'bottomright', zoomInTitle: 'Phóng to', zoomOutTitle: 'Thu nhỏ' }).addTo(map);
    map.fitBounds([[mapArea.bounds.south, mapArea.bounds.west], [mapArea.bounds.north, mapArea.bounds.east]], { padding: [24, 24], maxZoom: 14, animate: false });
    const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>' });
    const timer = window.setTimeout(() => { if (active) setState('error'); }, 15_000);
    tiles.once('tileload', () => { if (active) { window.clearTimeout(timer); setState('ready'); } });
    tiles.addTo(map);
    const observer = new ResizeObserver(() => map.invalidateSize({ animate: false }));
    observer.observe(host);
    return () => { active = false; window.clearTimeout(timer); observer.disconnect(); mapRef.current = null; map.remove(); };
  }, [attempt]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    const group = L.layerGroup().addTo(map);
    stops.forEach((item, index) => {
      const icon = L.divIcon({ className: 'guide-map-icon',
        html: `<span class="map-marker" aria-hidden="true">${new Intl.NumberFormat('vi-VN', { minimumIntegerDigits: 2 }).format(index + 1)}</span>`,
        iconSize: [36, 36], iconAnchor: [18, 18], popupAnchor: [0, -18] });
      L.marker([item.latitude, item.longitude], { title: item.name, alt: `Mở thông tin ${item.name}`, icon, keyboard: true, riseOnHover: true })
        .addTo(group).bindPopup(popupFor(item, language, scriptType));
    });
    return () => { group.remove(); };
  }, [stops, language, scriptType, attempt]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !gps.position) return;
    const point: L.LatLngExpression = [gps.position.latitude, gps.position.longitude];
    const marker = L.marker(point, { title: 'Vị trí hiện tại của bạn', alt: 'Vị trí hiện tại của bạn', keyboard: true,
      icon: L.divIcon({ className: 'user-location-icon', html: '<span aria-hidden="true"></span>', iconSize: [22, 22], iconAnchor: [11, 11] }) }).addTo(map);
    map.setView(point, 18, { animate: false });
    return () => { marker.remove(); };
  }, [gps.position, attempt]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !gps.position) return;
    const circle = L.circle([gps.position.latitude, gps.position.longitude], { radius, color: '#08766d', weight: 2, fillOpacity: 0.1, interactive: false }).addTo(map);
    return () => { circle.remove(); };
  }, [gps.position, radius, attempt]);

  useEffect(() => { if (listOpen) closeButton.current?.focus(); }, [listOpen]);

  return <section className="map-section" aria-label="Bản đồ địa điểm">
    <h2 className="sr-only">Bản đồ OpenStreetMap · Quận 1 &amp; Quận 3</h2>
    <div className="map-workspace">
      <div key={attempt} ref={container} className="tour-map" role="region" aria-label="Bản đồ OpenStreetMap bao phủ Quận 1 và Quận 3" />
      {toolbar}
      <aside id="map-location-list" className="location-drawer" hidden={!listOpen} aria-labelledby="list-heading" onKeyDown={event => { if (event.key === 'Escape') closeList(); }}>
        <div className="drawer-heading"><h2 id="list-heading">Danh sách địa điểm</h2><button ref={closeButton} className="secondary-button" onClick={closeList} aria-label="Đóng danh sách">✕</button></div>
        <CategoryFilter />
        <p className="result-count" role="status">{loading ? 'Đang tải địa điểm…' : `${new Intl.NumberFormat('vi-VN').format(items.length)} địa điểm`}</p>
        {error ? <div role="alert"><p>{errorMessage || 'Chưa tải được địa điểm. Hãy kiểm tra kết nối rồi thử lại.'}</p><button className="secondary-button" onClick={retry}>Thử lại</button></div>
          : !loading && !items.length ? <p role="status">Chưa tìm thấy địa điểm. Hãy thử từ khóa khác.</p>
            : <LocationResults items={items} language={language} scriptType={scriptType} label="Danh sách địa điểm" page={Number(params.get('page')) || 0}
              onPage={page => { const next = new URLSearchParams(params); next.set('page', String(page)); setParams(next); }} />}
        {!loading && !error && !items.length && <p>Thử từ khóa khác hoặc chọn một danh mục khác ở trên.</p>}
      </aside>
      <button className="map-location-button" onClick={gps.locate} disabled={gps.status === 'loading'} aria-label={gps.status === 'loading' ? 'Đang xác định vị trí hiện tại' : 'Định vị vị trí hiện tại'} title="Định vị vị trí hiện tại">
        {gps.status === 'loading' ? <span aria-hidden="true">…</span> : <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2"/><path d="M12 2v3m0 14v3M2 12h3m14 0h3"/></svg>}
      </button>
    </div>
    <div className="map-status" role="status" aria-live="polite">
      {state === 'loading' ? 'Đang tải bản đồ…' : state === 'ready' ? 'Bản đồ đã sẵn sàng. Chọn ghim để mở bài thuyết minh.' : 'Chưa tải được bản đồ nền. Hãy kiểm tra Internet; bạn vẫn có thể dùng danh sách và tìm quanh GPS.'}
      {gps.status === 'success' && ' Đã hiển thị vị trí của bạn trên bản đồ.'}
      {loading ? ' Đang tải địa điểm…' : !error && !items.length ? ' Chưa tìm thấy địa điểm. Hãy thử từ khóa khác.' : ''}
      {offline && ' Danh sách từ gói offline đã xác minh.'}
    </div>
    {state === 'error' && <button className="secondary-button" onClick={() => { setState('loading'); setAttempt(value => value + 1); }}>Tải lại bản đồ</button>}
    <NearbyPlaces items={items} language={language} scriptType={scriptType} radius={radius}
      onRadius={value => { const next = new URLSearchParams(params); next.set('radius', String(value)); next.delete('nearbyPage'); setParams(next, { replace: true }); }}
      position={gps.position} gpsStatus={gps.status} gpsError={gps.error} locate={gps.locate} loading={loading} error={error} errorMessage={errorMessage} retry={retry} />
  </section>;
}
