import { lazy, Suspense, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { languageName, supportedLanguages } from '../lib/guide';
import { useLocationCatalog } from '../lib/useLocationCatalog';
import type { TourLocation } from '../lib/guide';
import { normalizePlaceName } from '../lib/locationPhotos';
import { LocationSearch } from './LocationSearch';

const TourMap = lazy(() => import('./TourMap'));

export function LocationCatalog() {
  const [params, setParams] = useSearchParams();
  const listButton = useRef<HTMLButtonElement>(null);
  const keyword = params.get('keyword') || '';
  const category = params.get('categoryId') || '';
  const language = supportedLanguages.includes(params.get('language') || '') ? params.get('language')! : 'VI';
  const scriptType = params.get('scriptType') === 'SHORT' ? 'SHORT' : 'FULL';
  const listOpen = params.get('panel') === 'locations' || params.get('view') === 'list';
  const query = new URLSearchParams(category ? { categoryId: category, language } : { language }).toString();
  const catalog = useLocationCatalog(query);
  const [focusRequest, setFocusRequest] = useState(0);
  const matches = useMemo(() => catalog.items.filter(item => normalizePlaceName(item.name).includes(normalizePlaceName(keyword))), [catalog.items, keyword]);
  const requestedPlace = params.get('place');
  const selectedId = requestedPlace ? catalog.items.find(item => item.locationId === requestedPlace)?.locationId ?? null
    : keyword.trim() ? matches[0]?.locationId ?? null : null;
  const noSearchMatch = !!keyword.trim() && !catalog.loading && !catalog.error && !matches.length;
  function change(name: string, value: string) {
    const next = new URLSearchParams(params);
    next.set(name, value);
    next.delete('page'); next.delete('nearbyPage'); setParams(next);
  }
  function toggleList(open: boolean) {
    const next = new URLSearchParams(params); next.set('view', 'map');
    if (open) next.set('panel', 'locations'); else next.delete('panel');
    setParams(next);
    if (!open) listButton.current?.focus();
  }
  function submit(value: string, place?: TourLocation) {
    // Clearing an active selection resets through the URL change; do not refocus
    // its old popup while React Router is applying that navigation.
    if (value || !selectedId) setFocusRequest(count => count + 1);
    const next = new URLSearchParams(params);
    if (value) next.set('keyword', value); else next.delete('keyword');
    if (place) next.set('place', place.locationId); else next.delete('place');
    next.delete('panel'); next.delete('page'); next.delete('nearbyPage'); setParams(next);
  }
  return <section className="catalog map-catalog">
    <title>Bản đồ khám phá · TravelVoice</title>
    <h1 className="sr-only">Khám phá địa điểm thuyết minh</h1>
    <Suspense fallback={<p className="map-opening" role="status">Đang mở bản đồ…</p>}>
      <TourMap {...catalog} selectedId={selectedId} noSearchMatch={noSearchMatch} focusRequest={focusRequest} language={language} scriptType={scriptType} listOpen={listOpen} closeList={() => toggleList(false)}
        toolbar={<div className="map-toolbar" role="search" aria-label="Tìm kiếm địa điểm">
          <LocationSearch key={category} keyword={keyword} items={catalog.items} loading={catalog.loading} error={catalog.error} onSearch={submit} onClear={() => submit('')} />
          <label className="map-language"><span className="sr-only">Ngôn ngữ</span><select name="language" value={language} onChange={event => change('language', event.target.value)}>
            {supportedLanguages.map(code => <option key={code} value={code}>{languageName(code)}</option>)}
          </select></label>
          <button ref={listButton} className="map-list-toggle secondary-button" type="button" aria-expanded={listOpen} aria-controls="map-location-list" onClick={() => toggleList(!listOpen)}>
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6h12M9 12h12M9 18h12M3 6h1M3 12h1M3 18h1"/></svg>Danh sách
          </button>
        </div>} />
    </Suspense>
  </section>;
}
