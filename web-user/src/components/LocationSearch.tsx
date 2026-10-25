import { useRef, useState } from 'react';
import type { TourLocation } from '../lib/guide';
import { normalizePlaceName } from '../lib/locationPhotos';

export function LocationSearch({ keyword, items, loading, error, onSearch, onClear }: {
  keyword: string; items: TourLocation[]; loading: boolean; error: boolean;
  onSearch: (keyword: string, place?: TourLocation) => void;
  onClear: () => void;
}) {
  const input = useRef<HTMLInputElement>(null);
  const [edit, setEdit] = useState({ keyword, value: keyword });
  // URL navigation resets the draft without remounting the focused input.
  if (edit.keyword !== keyword) setEdit({ keyword, value: keyword });
  const draft = edit.keyword === keyword ? edit.value : keyword;
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const needle = normalizePlaceName(draft);
  const matches = needle ? items.filter(item => normalizePlaceName(item.name).includes(needle))
    .sort((a, b) => Number(normalizePlaceName(b.name).startsWith(needle)) - Number(normalizePlaceName(a.name).startsWith(needle))
      || a.name.localeCompare(b.name, 'vi-VN')).slice(0, 8) : [];
  const expanded = open && !!needle;
  function choose(place?: TourLocation) {
    setOpen(false); setActive(-1);
    const best = place ?? matches.find(item => normalizePlaceName(item.name) === needle) ?? matches[0];
    if (place) setEdit({ keyword, value: place.name });
    onSearch(place?.name ?? draft.trim(), best);
  }
  return <div className="map-search" onBlur={event => { if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false); }}>
    <label htmlFor="map-search" className="sr-only">Tìm địa điểm</label>
    <input ref={input} id="map-search" name="keyword" type="search" role="combobox" value={draft} autoComplete="off"
      aria-autocomplete="list" aria-controls="place-suggestions" aria-expanded={expanded}
      aria-activedescendant={expanded && active >= 0 && matches[active] ? `place-option-${active}` : undefined}
      placeholder="Tìm địa điểm, ví dụ Dinh Độc Lập…" maxLength={255}
      onFocus={() => setOpen(true)} onChange={event => {
        const value = event.target.value; setEdit({ keyword, value }); setActive(-1); setOpen(!!value.trim());
        if (!value.trim()) onClear();
      }}
      onKeyDown={event => {
        if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
          event.preventDefault(); setOpen(true);
          setActive(value => !matches.length ? -1 : value < 0 ? (event.key === 'ArrowDown' ? 0 : matches.length - 1)
            : (value + (event.key === 'ArrowDown' ? 1 : matches.length - 1)) % matches.length);
        } else if (event.key === 'Escape') { event.preventDefault(); setOpen(false); setActive(-1); }
        else if (event.key === 'Enter') { event.preventDefault(); choose(expanded && active >= 0 ? matches[active] : undefined); }
      }} />
    {!!draft && <button type="button" className="map-search-clear" aria-label="Xóa tìm kiếm" title="Xóa tìm kiếm" onClick={() => {
      setEdit({ keyword, value: '' }); setOpen(false); setActive(-1); onClear(); input.current?.focus();
    }}><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7 7 10 10M17 7 7 17"/></svg></button>}
    <button type="button" onClick={() => choose()} aria-label="Tìm kiếm" title="Tìm kiếm"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg></button>
    <div className="place-suggestions" hidden={!expanded}>
      <ul id="place-suggestions" role="listbox" aria-label="Gợi ý địa điểm">
        {matches.map((place, index) => <li key={place.locationId} role="presentation">
          <button id={`place-option-${index}`} type="button" role="option" tabIndex={-1} aria-selected={index === active}
            onMouseDown={event => event.preventDefault()} onClick={() => choose(place)}>{place.name}</button>
        </li>)}
      </ul>
      <p role="status" aria-live="polite">{loading ? 'Đang tải gợi ý…' : error ? 'Chưa tải được gợi ý. Hãy thử tải lại địa điểm.'
        : matches.length ? `${new Intl.NumberFormat('vi-VN').format(matches.length)} gợi ý. Dùng phím mũi tên và Enter để chọn.` : 'Không có địa điểm phù hợp. Hãy thử tên khác.'}</p>
    </div>
  </div>;
}
