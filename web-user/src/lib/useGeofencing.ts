import { useEffect, useState } from 'react';
import { supportedLanguages } from './guide';
import { deliverSystemNotification, geofenceEntries, geofenceMessage, localGeofences, releaseNotification, reserveNotification, usableGeofencePosition } from './geofence';

export function useGeofencing(language: string) {
  const [enabled, setEnabled] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [status, setStatus] = useState('Chưa bật gợi ý theo vị trí.');
  const [banners, setBanners] = useState<NonNullable<ReturnType<typeof geofenceMessage>>[]>([]);
  useEffect(() => {
    if (!enabled) return;
    let active = true; let blocked = false; let watch: number | undefined; let lastFix = 0;
    let permission: PermissionStatus | undefined;
    let freshAfter = Date.now();
    const inside = new Set<string>();
    let serial = Promise.resolve();
    const visibility = () => {
      inside.clear(); lastFix = 0; freshAfter = Date.now();
      setBanners([]);
      if (blocked) return;
      setStatus(document.hidden ? 'Trang đang ẩn: theo dõi nền chưa được xác minh; trình duyệt có thể dừng GPS.' : 'Đang chờ vị trí mới…');
    };
    const refresh = () => { setAttempt(value => value + 1); };
    const revoke = () => {
      blocked = true; inside.clear(); setBanners([]); lastFix = 0;
      if (watch !== undefined) navigator.geolocation.clearWatch(watch);
      setStatus('Quyền vị trí chưa được cấp hoặc đã thu hồi. Hãy cho phép vị trí rồi thử lại; vẫn có thể tìm thủ công.');
    };
    async function start() {
      setBanners([]); setStatus('Đang xác minh dữ liệu geofence trên thiết bị…');
      try {
        const fences = await localGeofences();
        if (!active) return;
        if (!supportedLanguages.includes(language)) { setStatus('Ngôn ngữ hiện tại chưa có gợi ý địa điểm. Hãy chọn một trong sáu ngôn ngữ được hỗ trợ.'); return; }
        if (!fences.length) { setStatus('Chưa có dữ liệu geofence đã khảo sát và cấu hình cooldown. Hãy tải gói đã xác minh khi được cung cấp; vẫn có thể tìm địa điểm thủ công.'); return; }
        if (!window.isSecureContext || !navigator.geolocation) { setStatus('Trình duyệt chưa hỗ trợ vị trí trong phiên này. Hãy mở HTTPS; vẫn có thể khám phá thủ công.'); return; }
        try {
          permission = await navigator.permissions?.query({ name: 'geolocation' });
          if (!active) return;
          if (permission?.state === 'denied') { revoke(); return; }
          if (permission) permission.onchange = () => { if (active && permission?.state === 'denied') revoke(); };
        } catch { /* Geolocation errors remain authoritative on browsers without Permissions API. */ }
        setStatus('Đang chờ quyền vị trí và GPS mới…');
        watch = navigator.geolocation.watchPosition(position => {
          serial = serial.then(async () => {
            if (!active || blocked) return;
            if (!usableGeofencePosition(position) || position.timestamp < freshAfter) { setStatus('GPS đã cũ hoặc chưa hợp lệ. Đang chờ vị trí mới…'); return; }
            lastFix = Date.now();
            const entries = geofenceEntries(fences, position, inside);
            setStatus(document.hidden ? 'Trang đang ẩn: chưa bảo đảm theo dõi nền liên tục.' : 'Đang theo dõi trong phiên mở. GPS có thể bỏ lỡ ranh giới khi sai số lớn.');
            for (const fence of entries) {
              const message = geofenceMessage(fence, language);
              if (!message || !active || blocked) continue;
              const reservation = Date.now();
              if (!await reserveNotification(fence.location.locationId, fence.cooldownMs, reservation)) continue;
              if (!active || blocked || position.timestamp < freshAfter) { await releaseNotification(fence.location.locationId, reservation); return; }
              try {
                if (!document.hidden) setBanners(current => [...current.filter(item => item.locationId !== message.locationId), message]);
                else if (!await deliverSystemNotification(message)) { await releaseNotification(fence.location.locationId, reservation); inside.delete(fence.location.locationId); }
              } catch { await releaseNotification(fence.location.locationId, reservation); inside.delete(fence.location.locationId); setStatus('Chưa gửi được thông báo. Mở lại trang để nhận gợi ý trong ứng dụng.'); }
            }
          }).catch(() => { if (active) { blocked = true; if (watch !== undefined) navigator.geolocation.clearWatch(watch);
            setStatus('Không lưu được cooldown. Đã dừng thông báo để tránh lặp; hãy cho phép lưu dữ liệu rồi thử lại.'); } });
        }, error => {
          if (!active) return;
          inside.clear(); setBanners([]); lastFix = 0;
          setStatus(error.code === 1 ? 'Quyền vị trí chưa được cấp hoặc đã thu hồi. Hãy cho phép vị trí rồi thử lại; vẫn có thể tìm thủ công.' : 'GPS chưa khả dụng. Đang chờ vị trí mới; vẫn có thể tìm thủ công.');
          if (error.code === 1) revoke();
        }, { enableHighAccuracy: true, maximumAge: 0, timeout: 20_000 });
      } catch { if (active) setStatus('Chưa đọc/xác minh được dữ liệu geofence. Hãy kiểm tra gói offline rồi thử lại.'); }
    }
    void start();
    const timer = window.setInterval(() => { if (active && lastFix && Date.now() - lastFix > 30_000) {
      lastFix = 0; setStatus('Chưa có GPS mới: theo dõi đang chờ cập nhật.');
    } }, 5_000);
    document.addEventListener('visibilitychange', visibility);
    window.addEventListener('travelvoice-content-changed', refresh);
    return () => { active = false; if (watch !== undefined) navigator.geolocation.clearWatch(watch); clearInterval(timer);
      if (permission) permission.onchange = null;
      document.removeEventListener('visibilitychange', visibility); window.removeEventListener('travelvoice-content-changed', refresh); };
  }, [enabled, language, attempt]);
  return { enabled, status: enabled ? status : 'Chưa bật gợi ý theo vị trí.', banners: enabled ? banners.filter(item => item.language === language) : [],
    toggle: () => { setEnabled(value => !value); setBanners([]); }, dismiss: (id: string) => setBanners(current => current.filter(item => item.locationId !== id)), retry: () => setAttempt(value => value + 1) };
}
