import { useLayoutEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { languageName, languageOptionName, languagePattern, supportedLanguages, uuidPattern } from '../lib/guide';
import { GuidePlayer } from '../components/GuidePlayer';
import type { Selection } from '../lib/guide';
import { useGuide } from '../lib/useGuide';
import { PublishedLocationPhoto } from '../components/PublishedLocationPhoto';
import { PlaceCredits } from '../components/PlaceCredits';

function GuideReader({ selection, retry }: { selection: Selection; retry: () => void }) {
  const state = useGuide(selection);
  if (state.status === 'loading') return <section className="reader loading" aria-busy="true" aria-label="Đang tải thuyết minh">
    <title>Đang tải thuyết minh · TravelVoice</title>
    <h1>Thuyết minh địa điểm</h1>
    <p role="status">Đang tải bài thuyết minh…</p>
    <div className="skeleton" aria-hidden="true"><span /><span /><span /><span /></div>
  </section>;
  if (state.status === 'unavailable') return <section className="reader message" role="status">
    <title>Nội dung đang được cập nhật · TravelVoice</title>
    <h1>Nội dung đang được cập nhật</h1>
    <p>Nội dung cho địa điểm này hiện đang được cập nhật. Bạn có thể chọn ngôn ngữ hoặc độ dài khác, hoặc mở một địa điểm khác.</p>
    <Link to="/">Mở địa điểm khác <span aria-hidden="true">→</span></Link>
  </section>;
  if (state.status === 'error') return <section className="reader message" role="alert">
    <title>Chưa tải được thuyết minh · TravelVoice</title>
    <h1>Chưa tải được thuyết minh</h1><p>{state.message}</p>
    <button className="primary-button" onClick={retry}>Thử lại</button>
  </section>;
  const { guide } = state;
  const fallback = guide.languageCode !== selection.language;
  return <article className="reader" aria-labelledby="location-name">
    <title>{`${guide.locationName} · TravelVoice`}</title>
    <p className="eyebrow">Câu chuyện nơi bạn đến</p>
    <h1 id="location-name">{guide.locationName}</h1>
    <p className="reading-meta">{languageName(guide.languageCode)}<span aria-hidden="true"> / </span>{guide.scriptType === 'FULL' ? 'Bài đầy đủ' : 'Bài ngắn gọn'}</p>
    <PublishedLocationPhoto id={guide.locationId} name={guide.locationName} />
    {state.offline && <p className="fallback-note" role="status">Đang đọc từ gói offline đã xác minh. Giọng đọc offline còn tùy thiết bị.</p>}
    <p role="status" className={fallback ? 'fallback-note' : 'sr-only'}>{fallback
      ? `Chưa có bản ${languageName(selection.language)} cho lựa chọn này. Đang hiển thị bản ${languageName(guide.languageCode)}; giữ nguyên ${guide.scriptType === 'FULL' ? 'bài đầy đủ' : 'bài ngắn gọn'}.`
      : 'Đã tải bài thuyết minh.'}</p>
    <GuidePlayer key={guide.contentId} guide={guide} offline={state.offline} />
    <div className="script-text" lang={guide.languageCode.toLowerCase()}>{guide.scriptText}</div>
    <PlaceCredits locationId={guide.locationId} />
    <p className="reader-end" aria-hidden="true">— Hết bài thuyết minh —</p>
  </article>;
}

export function GuidePage() {
  const { locationId = '' } = useParams();
  const [params, setParams] = useSearchParams();
  const [attempt, setAttempt] = useState(0);
  const language = (params.get('language') || 'VI').trim().toUpperCase();
  const scriptType = params.get('scriptType') || 'FULL';
  const id = locationId.toLowerCase();
  const languageControl = useRef<HTMLSelectElement>(null);
  const modeControls = useRef<HTMLFieldSetElement>(null);
  // Sync URL navigation into native controls without remounting or losing focus.
  useLayoutEffect(() => {
    if (languageControl.current) languageControl.current.value = language;
    modeControls.current?.querySelectorAll('input').forEach(input => {
      input.checked = input.value === scriptType;
    });
  }, [language, scriptType]);
  if (!uuidPattern.test(id) || !languagePattern.test(language) || !['FULL', 'SHORT'].includes(scriptType)) {
    return <section className="entry" role="alert"><h1>Đường dẫn chưa hợp lệ</h1>
      <title>Đường dẫn chưa hợp lệ · TravelVoice</title>
      <p>Hãy kiểm tra lại liên kết địa điểm, ngôn ngữ và độ dài bài thuyết minh.</p><Link to="/">Mở địa điểm khác</Link></section>;
  }
  const selection: Selection = { locationId: id, language, scriptType: scriptType as Selection['scriptType'] };
  function change(name: string, value: string) {
    const next = new URLSearchParams(params);
    next.set('language', language);
    next.set('scriptType', scriptType);
    next.set(name, value);
    setParams(next);
  }
  const languages = [...supportedLanguages, ...(!supportedLanguages.includes(language) ? [language] : [])];
  return <div className="guide-layout">
    <nav className="back-link" aria-label="Điều hướng địa điểm"><Link to={`/?language=${language}&scriptType=${scriptType}`}>← Khám phá địa điểm khác</Link></nav>
    <aside className="reading-options" aria-labelledby="options-heading">
      <p className="eyebrow">Theo cách của bạn</p><p className="options-title" id="options-heading">Chọn bài đọc</p>
      <label htmlFor="guide-language">Ngôn ngữ yêu cầu</label>
      <select ref={languageControl} id="guide-language" name="language" autoComplete="off" defaultValue={language} onChange={event => change('language', event.target.value)}>
        {languages.map(code => <option key={code} value={code} lang="en" translate="no">{languageOptionName(code)}{!supportedLanguages.includes(code) ? ' (theo liên kết)' : ''}</option>)}
      </select>
      <fieldset ref={modeControls}><legend>Độ dài thuyết minh</legend>
        {(['FULL', 'SHORT'] as const).map(mode => <label className="mode-option" key={mode}>
          <input type="radio" name="scriptType" value={mode} defaultChecked={scriptType === mode} onChange={() => change('scriptType', mode)} />
          <span>{mode === 'FULL' ? 'Đầy đủ' : 'Ngắn gọn'}<small>{mode === 'FULL' ? 'Khám phá trọn câu chuyện' : 'Nắm những nét chính'}</small></span>
        </label>)}
      </fieldset>
      <p className="options-hint">Đổi lựa chọn để đọc bài tương ứng. Ngôn ngữ thực tế được ghi ngay dưới tên địa điểm.</p>
    </aside>
    <GuideReader key={`${id}:${language}:${scriptType}:${attempt}`} selection={selection} retry={() => setAttempt(value => value + 1)} />
  </div>;
}
