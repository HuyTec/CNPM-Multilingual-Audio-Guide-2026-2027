import { Link, useLocation, useSearchParams } from 'react-router-dom';
import { languageOptionName, supportedLanguages } from '../lib/guide';

export function SiteHeader() {
  const { pathname } = useLocation();
  const [params, setParams] = useSearchParams();
  const language = supportedLanguages.includes(params.get('language') || '') ? params.get('language')! : 'VI';
  return <header className="site-header">
    <Link to="/" className="brand" translate="no">TravelVoice<span>Thuyết minh địa điểm</span></Link>
    <nav className="header-nav" aria-label="Điều hướng chính">
      <Link to="/" aria-current={pathname === '/' ? 'page' : undefined}>Khám phá</Link>
      {pathname === '/' && <label className="header-language">
        <span className="sr-only">Ngôn ngữ</span>
        <select name="language" autoComplete="off" lang="en" translate="no" value={language} onChange={event => {
          const next = new URLSearchParams(params);
          next.set('language', event.target.value); next.delete('page'); next.delete('nearbyPage'); setParams(next);
        }}>
          {supportedLanguages.map(code => <option key={code} value={code}>{languageOptionName(code)}</option>)}
        </select>
      </label>}
      <Link to="/offline" aria-current={pathname === '/offline' ? 'page' : undefined}>Nội dung offline</Link>
    </nav>
  </header>;
}
