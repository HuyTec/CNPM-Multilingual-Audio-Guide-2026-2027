import { Link } from 'react-router-dom';
import type { TourLocation } from '../lib/guide';

export type LocatedPlace = TourLocation & { distance?: number };
const number = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });

export function LocationResults({ items, language, scriptType, label, page, onPage }: {
  items: LocatedPlace[]; language: string; scriptType: string; label: string;
  page: number; onPage: (page: number) => void;
}) {
  const pages = Math.ceil(items.length / 50);
  const current = Math.min(Math.max(0, Math.floor(page)), Math.max(0, pages - 1));
  return <>
    <ol className="map-place-list" aria-label={label}>
      {items.slice(current * 50, (current + 1) * 50).map((item, index) => <li key={item.locationId}>
        <Link title={item.name} to={`/locations/${item.locationId}?${new URLSearchParams({ language, scriptType })}`}>
          <span className="place-index" aria-hidden="true">{number.format(current * 50 + index + 1).padStart(2, '0')}</span>
          <span className="place-copy"><h3>{item.name}</h3><span>{item.categoryName}</span></span>
          {item.distance !== undefined ? <span className="place-distance">{number.format(item.distance)} m</span> : <span aria-hidden="true">↗</span>}
        </Link>
      </li>)}
    </ol>
    {pages > 1 && <nav className="place-pagination" aria-label={`Phân trang ${label.toLocaleLowerCase('vi-VN')}`}>
      <button className="secondary-button" disabled={current === 0} onClick={() => onPage(current - 1)}>Trước</button>
      <span>Trang {number.format(current + 1)} / {number.format(pages)}</span>
      <button className="secondary-button" disabled={current + 1 === pages} onClick={() => onPage(current + 1)}>Sau</button>
    </nav>}
  </>;
}
