import { lazy, Suspense, useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { languageName, supportedLanguages, tourCategoryId, uuidPattern } from '../lib/guide';
import type { TourLocation } from '../lib/guide';
import { offlineLocations } from '../lib/packages';

const TourMap = lazy(() => import('./TourMap'));
type Result = { items: TourLocation[]; totalElements: number; totalPages: number; page: number };
function Results({ query, language, scriptType, view }: { query: string; language: string; scriptType: string; view: string }) {
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [offline, setOffline] = useState(false);
  useEffect(() => {
    let active = true; const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 12_000);
    void fetch(`/api/locations?${query}`, { signal: controller.signal, cache: 'no-store' }).then(async response => {
      if (!response.ok) throw new Error();
      const data = await response.json() as Result;
      if (!Array.isArray(data.items) || !Number.isFinite(data.totalElements)
        || data.items.some(item => !uuidPattern.test(item.locationId) || typeof item.name !== 'string')) throw new Error();
      if (active) { setResult(data); setError(false); }
    }).catch(async () => {
      try {
        const local = await offlineLocations();
        if (!local.length) throw new Error();
        const parameters = new URLSearchParams(query);
        const category = parameters.get('categoryId'); const keyword = parameters.get('keyword')!.toLocaleLowerCase('vi-VN');
        const matches = local.filter(item => (!category || item.categoryId === category) && item.name.toLocaleLowerCase('vi-VN').includes(keyword))
          .sort((a, b) => a.name.localeCompare(b.name, 'vi-VN'));
        const page = Number(parameters.get('page'));
        if (active) { setResult({ items: matches.slice(page * 12, (page + 1) * 12), totalElements: matches.length, totalPages: Math.ceil(matches.length / 12), page }); setOffline(true); setError(false); }
      } catch { if (active) setError(true); }
    }).finally(() => clearTimeout(timer));
    return () => { active = false; clearTimeout(timer); controller.abort(); };
  }, [query, attempt]);
  if (error) return <div className="catalog-message" role="alert"><h2>Chưa tải được địa điểm</h2>
    <p>Hãy kiểm tra kết nối rồi thử lại. Dữ liệu sẽ được lấy từ máy chủ.</p>
    <button className="primary-button" onClick={() => setAttempt(value => value + 1)}>Thử lại</button></div>;
  if (!result) return <p className="catalog-message" role="status">Đang tải địa điểm…</p>;
  if (!result.items.length) return <div className="catalog-message" role="status"><h2>Chưa tìm thấy địa điểm</h2><p>Hãy thử từ khóa khác hoặc chọn tất cả địa điểm.</p></div>;
  return <>
    {offline && <p className="fallback-note" role="status">Danh sách từ gói offline đã xác minh.</p>}
    <p role="status" className="result-count">{new Intl.NumberFormat('vi-VN').format(result.totalElements)} địa điểm</p>
    {view === 'map' && <Suspense fallback={<p role="status">Đang mở bản đồ…</p>}><TourMap items={result.items} language={language} scriptType={scriptType} /></Suspense>}
    <ol className="location-list" aria-label="Danh sách địa điểm">
      {result.items.map((item, index) => <li key={item.locationId}><Link className="location-card" to={`/locations/${item.locationId}?language=${language}&scriptType=${scriptType}`}>
        <span className="location-number" aria-hidden="true">{new Intl.NumberFormat('vi-VN', { minimumIntegerDigits: 2 }).format(index + 1 + result.page * 12)}</span>
        <span className="location-card-copy"><span className="location-category">{item.categoryName}</span><h2>{item.name}</h2><span>Đọc và nghe thuyết minh <span aria-hidden="true">→</span></span></span>
      </Link></li>)}
    </ol>
  </>;
}

export function LocationCatalog() {
  const [params, setParams] = useSearchParams();
  const search = useRef<HTMLInputElement>(null);
  const keyword = params.get('keyword') || '';
  const category = params.get('categoryId') ?? tourCategoryId;
  const language = supportedLanguages.includes(params.get('language') || '') ? params.get('language')! : 'VI';
  const scriptType = params.get('scriptType') === 'SHORT' ? 'SHORT' : 'FULL';
  const view = params.get('view') === 'map' ? 'map' : 'list';
  const page = Math.max(0, Number(params.get('page')) || 0);
  const query = new URLSearchParams({ keyword, page: String(page), size: '12', ...(category ? { categoryId: category } : {}) }).toString();
  function change(name: string, value: string) {
    const next = new URLSearchParams(params); next.set(name, value);
    if (name !== 'page') next.delete('page'); setParams(next);
  }
  function submit(event: FormEvent) { event.preventDefault(); change('keyword', search.current?.value.trim() || ''); }
  return <section className="catalog">
    <title>Khám phá Quận 1 · TravelVoice</title>
    <div className="catalog-intro"><p className="eyebrow">Quận 1 & những điểm đến lân cận</p><h1>Chạm một điểm đến.<br />Nghe một câu chuyện.</h1>
      <p>Chọn nơi bạn muốn khám phá, ngôn ngữ của bạn và bắt đầu lắng nghe.</p></div>
    <p className="demo-notice">Bộ nội dung demo do nhóm cung cấp, đang được rà soát. Tọa độ là mốc minh họa, chưa dùng để xác nhận vị trí hoặc khoảng cách thực.</p>
    <form className="catalog-controls" onSubmit={submit}>
      <label>Tìm địa điểm<input key={keyword} ref={search} name="keyword" type="search" defaultValue={keyword} autoComplete="off" placeholder="Dinh Độc Lập…" maxLength={255} /></label>
      <button className="primary-button" type="submit">Tìm kiếm</button>
      <label><span id="catalog-language-label">Ngôn ngữ</span><select aria-labelledby="catalog-language-label" name="language" value={language} onChange={event => change('language', event.target.value)}>{supportedLanguages.map(code => <option key={code} value={code}>{languageName(code)}</option>)}</select></label>
      <label><span id="catalog-category-label">Bộ địa điểm</span><select aria-labelledby="catalog-category-label" name="categoryId" value={category} onChange={event => change('categoryId', event.target.value)}><option value={tourCategoryId}>Tuyến tham quan mới</option><option value="">Tất cả địa điểm</option></select></label>
    </form>
    <div className="catalog-view" aria-label="Cách xem địa điểm">
      <button className="secondary-button" aria-pressed={view === 'list'} onClick={() => change('view', 'list')}>Danh sách</button>
      <button className="secondary-button" aria-pressed={view === 'map'} onClick={() => change('view', 'map')}>Bản đồ</button>
    </div>
    <Results key={`${query}:${language}:${scriptType}:${view}`} query={query} language={language} scriptType={scriptType} view={view} />
  </section>;
}
