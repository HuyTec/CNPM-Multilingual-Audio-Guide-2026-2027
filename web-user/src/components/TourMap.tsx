import { useEffect, useEffectEvent, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { useSearchParams } from 'react-router-dom';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { TourLocation } from '../lib/guide';
import { mapArea, hasMapCoordinates } from '../lib/osmMap';
import { attachMapTiles } from '../lib/mapTiles';
import type { MapTileState } from '../lib/mapTiles';
import { locationPhoto } from '../lib/locationPhotos';
import { useBrowserLocation } from '../lib/useBrowserLocation';
import { LocationResults } from './LocationResults';
import { NearbyPlaces } from './NearbyPlaces';
import { CategoryFilter } from './CategoryFilter';

function popupFor(item: TourLocation, language: string, scriptType: string) {
  const content = document.createElement('div'); content.className = 'map-popup';
  const photo = locationPhoto(item.locationId, item.imageUrl);
  if (photo) {
    const image = document.createElement('img'); image.src = photo.src; image.alt = item.name;
    image.width = photo.width; image.height = photo.height; content.append(image);
  }
  const name = document.createElement('strong'); name.textContent = item.name; content.append(name);
  const badge = document.createElement('span'); badge.className = 'map-selected-label'; badge.textContent = 'Đang chọn'; content.append(badge);
  const guide = document.createElement('a');
  guide.href = `/locations/${item.locationId}?${new URLSearchParams({ language, scriptType })}`;
  guide.className = 'primary-button'; guide.textContent = 'Mở bài thuyết minh'; content.append(guide);
  return content;
}

export default function TourMap({ items, language, scriptType, toolbar, listOpen, closeList, loading, error, errorMessage, offline, retry, focusRequest, selectedId: selected, noSearchMatch }: {
  items: TourLocation[]; language: string; scriptType: string; toolbar: ReactNode;
  listOpen: boolean; closeList: () => void; loading: boolean; error: boolean; errorMessage?: string; offline: boolean; retry: () => void;
  focusRequest: number;
  selectedId: string | null; noSearchMatch: boolean;
}) {
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const closeButton = useRef<HTMLButtonElement>(null);
  const [state, setState] = useState<MapTileState>('loading');
  const [attempt, setAttempt] = useState(0);
  const [params, setParams] = useSearchParams();
  const rawRadius = Number(params.get('radius') ?? 100);
  const radius = Number.isFinite(rawRadius) ? Math.min(500, Math.max(50, Math.round(rawRadius))) : 100;
  const gps = useBrowserLocation();
  const stops = useMemo(() => items.filter(hasMapCoordinates), [items]);
  const markers = useRef(new Map<string, L.Marker>());
  const previousSelection = useRef({ selected, focusRequest });
  function selectPlace(item: TourLocation) {
    // Selecting the same item again should also return from a GPS/manual pan.
    const marker = markers.current.get(item.locationId);
    if (selected === item.locationId && marker && mapRef.current) {
      mapRef.current.setView(marker.getLatLng(), 16, { animate: false }); marker.openPopup();
      marker.getPopup()?.getElement()?.querySelector<HTMLAnchorElement>('.map-popup a')?.focus({ preventScroll: true });
    }
    const next = new URLSearchParams(params); next.set('place', item.locationId); next.delete('panel'); next.set('view', 'map');
    setParams(next);
  }
  const markerSelected = useEffectEvent((item: TourLocation) => selectPlace(item));

  useEffect(() => {
    const host = container.current;
    if (!host) return;
    let active = true;
    const map = L.map(host, { center: [mapArea.center.lat, mapArea.center.lng], zoom: 13, minZoom: 2, maxZoom: 19,
      zoomControl: false, scrollWheelZoom: true, keyboard: true,
      fadeAnimation: false,
      zoomAnimation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches });
    mapRef.current = map;
    map.attributionControl.setPosition('bottomleft');
    L.control.zoom({ position: 'bottomright', zoomInTitle: 'Phóng to', zoomOutTitle: 'Thu nhỏ' }).addTo(map);
    map.fitBounds([[mapArea.bounds.south, mapArea.bounds.west], [mapArea.bounds.north, mapArea.bounds.east]], { padding: [24, 24], maxZoom: 14, animate: false });
    const removeTiles = attachMapTiles(map, value => { if (active) setState(value); });
    const observer = new ResizeObserver(() => map.invalidateSize({ animate: false }));
    observer.observe(host);
    return () => { active = false; removeTiles(); observer.disconnect(); mapRef.current = null; map.remove(); };
  }, [attempt]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    const group = L.layerGroup().addTo(map);
    markers.current.clear();
    stops.forEach((item, index) => {
      const icon = L.divIcon({ className: 'guide-map-icon',
        html: `<span class="map-marker" aria-hidden="true">${new Intl.NumberFormat('vi-VN', { minimumIntegerDigits: 2 }).format(index + 1)}</span>`,
        iconSize: [36, 36], iconAnchor: [18, 18], popupAnchor: [0, -18] });
      const marker = L.marker([item.latitude, item.longitude], { title: item.name, alt: `Mở thông tin ${item.name}`, icon, keyboard: true, riseOnHover: true })
        .addTo(group).bindPopup(popupFor(item, language, scriptType), { maxWidth: 260, minWidth: 200,
          autoPanPaddingTopLeft: [16, 144], autoPanPaddingBottomRight: [16, 64] });
      markers.current.set(item.locationId, marker);
      marker.on('click', () => markerSelected(item));
      marker.on('popupopen', () => {
        const close = marker.getPopup()?.getElement()?.querySelector<HTMLAnchorElement>('.leaflet-popup-close-button');
        close?.setAttribute('aria-label', 'Đóng thông tin địa điểm'); close?.setAttribute('title', 'Đóng thông tin địa điểm');
      });
    });
    return () => { group.remove(); };
  }, [stops, language, scriptType, attempt]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !stops.length) return;
    map.fitBounds(L.latLngBounds(stops.map(item => [item.latitude, item.longitude] as [number, number])), {
      paddingTopLeft: [48, 144], paddingBottomRight: [64, 48], maxZoom: 15, animate: false,
    });
  }, [stops, attempt]);

  useEffect(() => {
    const map = mapRef.current;
    const previous = previousSelection.current;
    previousSelection.current = { selected, focusRequest };
    markers.current.forEach((marker, id) => {
      const active = id === selected;
      const element = marker.getElement();
      element?.classList.toggle('is-selected', active);
      element?.setAttribute('aria-pressed', String(active));
      const item = stops.find(stop => stop.locationId === id);
      if (item) element?.setAttribute('aria-label', `${item.name}${active ? ' · Đang chọn' : ''}`);
      marker.setZIndexOffset(active ? 1000 : 0);
    });
    const marker = selected ? markers.current.get(selected) : undefined;
    if (!map) return;
    if (!marker) {
      map.closePopup();
      if (stops.length && (previous.selected || previous.focusRequest !== focusRequest)) {
        map.fitBounds(L.latLngBounds(stops.map(item => [item.latitude, item.longitude] as [number, number])), {
          paddingTopLeft: [48, 144], paddingBottomRight: [64, 48], maxZoom: 15, animate: false,
        });
      }
      return;
    }
    map.setView(marker.getLatLng(), 16, { animate: false });
    marker.openPopup();
    marker.getPopup()?.getElement()?.querySelector<HTMLAnchorElement>('.map-popup a')?.focus({ preventScroll: true });
  }, [selected, stops, language, scriptType, attempt, focusRequest]);

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
              onSelect={selectPlace}
              onPage={page => { const next = new URLSearchParams(params); next.set('page', String(page)); setParams(next); }} />}
        {!loading && !error && !items.length && <p>Thử từ khóa khác hoặc chọn một danh mục khác ở trên.</p>}
      </aside>
      <button className="map-location-button" onClick={gps.locate} disabled={gps.status === 'loading'} aria-label={gps.status === 'loading' ? 'Đang xác định vị trí hiện tại' : 'Định vị vị trí hiện tại'} title="Định vị vị trí hiện tại">
        {gps.status === 'loading' ? <span aria-hidden="true">…</span> : <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2"/><path d="M12 2v3m0 14v3M2 12h3m14 0h3"/></svg>}
      </button>
    </div>
    <div className="map-status" role="status" aria-live="polite">
      {state === 'loading' ? 'Đang tải bản đồ…' : state === 'ready' ? 'Bản đồ đã sẵn sàng. Chọn ghim để mở bài thuyết minh.'
        : state === 'fallback' ? 'Bản đồ đã sẵn sàng với nguồn nền dự phòng OSM France. Chọn ghim để mở bài thuyết minh.'
          : 'Chưa tải được bản đồ nền. Hãy kiểm tra Internet; bạn vẫn có thể chọn địa điểm trong Danh sách để xem ảnh và mở bài thuyết minh.'}
      {gps.status === 'success' && ' Đã hiển thị vị trí của bạn trên bản đồ.'}
      {noSearchMatch && ' Chưa tìm thấy địa điểm phù hợp. Hãy thử tên khác; các địa điểm trong danh mục vẫn hiển thị.'}
      {loading ? ' Đang tải địa điểm…' : !error && !items.length ? ' Chưa tìm thấy địa điểm. Hãy thử từ khóa khác.' : ''}
      {offline && ' Danh sách từ gói offline đã xác minh.'}
    </div>
    {state === 'error' && <button className="secondary-button" onClick={() => { setState('loading'); setAttempt(value => value + 1); }}>Tải lại bản đồ</button>}
    <NearbyPlaces items={items} language={language} scriptType={scriptType} radius={radius}
      onRadius={value => { const next = new URLSearchParams(params); next.set('radius', String(value)); next.delete('nearbyPage'); setParams(next, { replace: true }); }}
      position={gps.position} gpsStatus={gps.status} gpsError={gps.error} refining={gps.refining} locate={gps.locate} loading={loading} error={error} errorMessage={errorMessage} retry={retry} />
  </section>;
}
