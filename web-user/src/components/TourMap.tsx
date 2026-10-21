import { useEffect, useMemo, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { TourLocation } from '../lib/guide';
import { districtOne, isDistrictOneStop, osmAndLocationUrl } from '../lib/osmMap';

function popupFor(item: TourLocation, language: string, scriptType: string) {
  const content = document.createElement('div');
  content.className = 'map-popup';

  const name = document.createElement('strong');
  name.textContent = item.name;
  content.append(name);

  const guide = document.createElement('a');
  guide.href = `/locations/${item.locationId}?${new URLSearchParams({ language, scriptType })}`;
  guide.textContent = 'Mở bài thuyết minh';
  content.append(guide);

  const osmand = document.createElement('a');
  osmand.href = osmAndLocationUrl(item);
  osmand.target = '_blank';
  osmand.rel = 'noreferrer';
  osmand.textContent = 'Mở vị trí trong OsmAnd ↗';
  content.append(osmand);
  return content;
}

export default function TourMap({ items, language, scriptType }: { items: TourLocation[]; language: string; scriptType: string }) {
  const container = useRef<HTMLDivElement>(null);
  const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading');
  const [attempt, setAttempt] = useState(0);
  const stops = useMemo(() => items.filter(isDistrictOneStop), [items]);

  useEffect(() => {
    const host = container.current;
    if (!host || !stops.length) return;

    let active = true;
    let hasTiles = false;
    const mapBounds = L.latLngBounds(
      [districtOne.bounds.south, districtOne.bounds.west],
      [districtOne.bounds.north, districtOne.bounds.east],
    );
    const map = L.map(host, {
      center: [districtOne.center.lat, districtOne.center.lng],
      zoom: 15,
      minZoom: 14,
      maxZoom: 19,
      maxBounds: mapBounds,
      maxBoundsViscosity: 1,
      scrollWheelZoom: false,
      keyboard: true,
    });
    const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
    });
    const timer = window.setTimeout(() => { if (active && !hasTiles) setState('error'); }, 15_000);
    tiles.once('tileload', () => {
      hasTiles = true;
      if (active) { window.clearTimeout(timer); setState('ready'); }
    });
    tiles.addTo(map);

    const iconFor = (number: number) => L.divIcon({
      className: 'guide-map-icon',
      html: `<span class="map-marker" aria-hidden="true">${new Intl.NumberFormat('vi-VN', { minimumIntegerDigits: 2 }).format(number)}</span>`,
      iconSize: [36, 36],
      iconAnchor: [18, 18],
      popupAnchor: [0, -18],
    });
    stops.forEach((item, index) => {
      L.marker([item.latitude, item.longitude], {
        title: item.name,
        alt: `Mở thông tin ${item.name}`,
        icon: iconFor(index + 1),
        keyboard: true,
        riseOnHover: true,
      }).addTo(map).bindPopup(popupFor(item, language, scriptType));
    });

    return () => {
      active = false;
      window.clearTimeout(timer);
      map.remove();
    };
  }, [language, scriptType, stops, attempt]);

  return <section className="map-section" aria-label="Bản đồ địa điểm">
    <h2>Bản đồ OpenStreetMap · Quận 1</h2>
    {!stops.length ? <p role="status">Không có điểm thuộc tuyến Quận 1 trong kết quả này. Bạn vẫn có thể chọn bài bên dưới.</p>
      : <><div key={attempt} ref={container} className="tour-map" role="region" aria-label="Bản đồ OpenStreetMap với địa điểm thuyết minh Quận 1" />
        {state === 'loading' && <p role="status">Đang tải bản đồ…</p>}
        {state === 'ready' && <p role="status">Bản đồ đã sẵn sàng. Chọn một ghim để mở bài thuyết minh hoặc vị trí trong OsmAnd.</p>}
        {state === 'error' && <div role="alert" className="map-error"><p>Chưa tải được bản đồ nền. Hãy kiểm tra kết nối Internet.</p>
          <button className="secondary-button" onClick={() => { setState('loading'); setAttempt(value => value + 1); }}>Tải lại bản đồ</button></div>}</>}
    <p className="player-hint">Khung bản đồ giới hạn quanh tuyến demo Quận 1; đây không phải đường biên hành chính đã kiểm chứng.</p>
    <p className="player-hint">Bản đồ nền cần Internet. Dữ liệu bản đồ © OpenStreetMap contributors; không tải tile nền vào gói offline.</p>
  </section>;
}
