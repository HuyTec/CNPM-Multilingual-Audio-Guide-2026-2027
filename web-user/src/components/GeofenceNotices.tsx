import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useGeofencing } from '../lib/useGeofencing';

export function GeofenceNotices() {
  const [params] = useSearchParams();
  const language = (params.get('language') || 'VI').toUpperCase();
  const geofence = useGeofencing(language);
  const [notificationStatus, setNotificationStatus] = useState('');
  async function requestNotifications() {
    try {
      if (!('Notification' in window) || !('serviceWorker' in navigator)) { setNotificationStatus('Trình duyệt chưa hỗ trợ thông báo hệ thống. Gợi ý trong trang vẫn dùng được.'); return; }
      const permission = await Notification.requestPermission();
      setNotificationStatus(permission === 'granted' ? 'Đã cho phép thông báo hệ thống. Theo dõi GPS nền vẫn tùy trình duyệt và chưa được xác minh.' : 'Chưa được cấp quyền thông báo hệ thống. Gợi ý trong trang vẫn dùng được.');
    } catch { setNotificationStatus('Chưa bật được thông báo hệ thống. Hãy mở HTTPS và kiểm tra quyền; gợi ý trong trang vẫn dùng được.'); }
  }
  return <aside className="geofence-notices" aria-label="Gợi ý địa điểm theo vị trí">
    <div className="player-actions">
      <button type="button" className="secondary-button" aria-pressed={geofence.enabled} onClick={geofence.toggle}>{geofence.enabled ? 'Tắt gợi ý theo vị trí' : 'Bật gợi ý theo vị trí'}</button>
      {geofence.enabled && <><button type="button" className="secondary-button" onClick={geofence.retry}>Kiểm tra lại geofence</button>
        <button type="button" className="secondary-button" onClick={() => void requestNotifications()}>Cho phép thông báo hệ thống</button></>}
    </div>
    <p role={geofence.enabled ? 'status' : undefined} aria-live="polite">{geofence.status}</p>
    {geofence.enabled && <p className="player-hint">Đóng trang, khóa màn hình hoặc chuyển ứng dụng có thể dừng GPS. Chưa bảo đảm theo dõi nền trên thiết bị này.</p>}
    <p aria-live="polite">{notificationStatus}</p>
    <div aria-live="polite">{geofence.banners.map(banner => <div key={banner.locationId} className="geofence-banner" lang={banner.language.toLowerCase()}>
      <p><strong>{banner.title}</strong><br />{banner.body}</p>
      <div className="player-actions"><Link to={banner.url} onClick={() => geofence.dismiss(banner.locationId)}>{banner.open}</Link>
        <button type="button" className="secondary-button" onClick={() => geofence.dismiss(banner.locationId)}>{banner.dismiss}</button></div>
    </div>)}</div>
  </aside>;
}
