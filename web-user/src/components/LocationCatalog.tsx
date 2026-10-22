import { lazy, Suspense, useRef } from 'react';
import type { FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { languageName, supportedLanguages, tourCategoryId } from '../lib/guide';
import { useLocationCatalog } from '../lib/useLocationCatalog';

const TourMap = lazy(() => import('./TourMap'));

export function LocationCatalog() {
  const [params, setParams] = useSearchParams();
  const search = useRef<HTMLInputElement>(null);
  const listButton = useRef<HTMLButtonElement>(null);
  const keyword = params.get('keyword') || '';
  const category = params.get('categoryId') ?? tourCategoryId;
  const language = supportedLanguages.includes(params.get('language') || '') ? params.get('language')! : 'VI';
  const scriptType = params.get('scriptType') === 'SHORT' ? 'SHORT' : 'FULL';
  const listOpen = params.get('panel') === 'locations' || params.get('view') === 'list';
  const query = new URLSearchParams({ keyword, ...(category ? { categoryId: category } : {}) }).toString();
  const catalog = useLocationCatalog(query);
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
  function submit(event: FormEvent) {
    event.preventDefault(); change('keyword', search.current?.value.trim() || '');
  }
  return <section className="catalog map-catalog">
    <title>Bản đồ khám phá · TravelVoice</title>
    <h1 className="sr-only">Khám phá địa điểm thuyết minh</h1>
    <Suspense fallback={<p className="map-opening" role="status">Đang mở bản đồ…</p>}>
      <TourMap {...catalog} language={language} scriptType={scriptType} listOpen={listOpen} closeList={() => toggleList(false)}
        toolbar={<form className="map-toolbar" aria-label="Tìm kiếm địa điểm" onSubmit={submit}>
          <div className="map-search">
            <label htmlFor="map-search" className="sr-only">Tìm địa điểm</label>
            <input key={keyword} id="map-search" ref={search} name="keyword" type="search" defaultValue={keyword} autoComplete="off" placeholder="Tìm địa điểm, ví dụ Dinh Độc Lập…" maxLength={255} />
            <button type="submit" aria-label="Tìm kiếm" title="Tìm kiếm"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg></button>
          </div>
          <label className="map-language"><span className="sr-only">Ngôn ngữ</span><select name="language" value={language} onChange={event => change('language', event.target.value)}>
            {supportedLanguages.map(code => <option key={code} value={code}>{languageName(code)}</option>)}
          </select></label>
          <button ref={listButton} className="map-list-toggle secondary-button" type="button" aria-expanded={listOpen} aria-controls="map-location-list" onClick={() => toggleList(!listOpen)}>
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6h12M9 12h12M9 18h12M3 6h1M3 12h1M3 18h1"/></svg>Danh sách
          </button>
        </form>} />
    </Suspense>
    <details className="map-data-note"><summary>Thông tin bản đồ và dữ liệu demo</summary>
      <p>Nội dung và tọa độ demo đang được rà soát. Khoảng cách hiển thị là đường chim bay theo tọa độ hiện có, không phải quãng đường đi bộ.</p>
      <p>Khung ban đầu bao phủ khu vực Quận 1 và Quận 3. GPS chỉ được hỏi khi bạn bấm nút, không gửi lên backend. Bản đồ nền cần Internet; không tải tile vào gói offline.</p>
    </details>
  </section>;
}
