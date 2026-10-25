import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Link, Route, Routes } from 'react-router-dom';
import { LocationEntry } from './pages/LocationEntry';
import { GuidePage } from './pages/GuidePage';
import { OfflinePage } from './pages/OfflinePage';
import { PlaybackSyncStatus } from './components/PlaybackSyncStatus';
import { GeofenceNotices } from './components/GeofenceNotices';
import {PackageUpdateNotice} from './components/PackageUpdateNotice';
import './style.css';

if (import.meta.env.PROD && 'serviceWorker' in navigator) void navigator.serviceWorker.register(`/sw.js?build=${import.meta.env.APP_BUILD}`, { type: 'module', updateViaCache: 'none' }).catch(() => {});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <a className="skip-link" href="#main">Đến nội dung chính</a>
      <header className="site-header">
        <Link to="/" className="brand" translate="no">TravelVoice<span>Thuyết minh địa điểm</span></Link>
        <nav className="header-nav" aria-label="Điều hướng chính"><Link to="/">Khám phá</Link><Link to="/offline">Nội dung offline</Link></nav>
      </header>
      <main id="main" tabIndex={-1}>
        <Routes>
          <Route path="/" element={<LocationEntry />} />
          <Route path="/locations/:locationId" element={<GuidePage />} />
          <Route path="/offline" element={<OfflinePage />} />
          <Route path="*" element={<section className="entry"><title>Không tìm thấy trang · TravelVoice</title><h1>Không tìm thấy trang</h1><p>Hãy kiểm tra lại đường dẫn địa điểm.</p><Link to="/">Mở địa điểm khác</Link></section>} />
        </Routes>
        <PackageUpdateNotice />
        <PlaybackSyncStatus />
        <GeofenceNotices />
      </main>
      <footer className="site-footer"><span translate="no">TravelVoice</span><span>Mỗi địa điểm, một câu chuyện.</span></footer>
    </BrowserRouter>
  </StrictMode>,
);
