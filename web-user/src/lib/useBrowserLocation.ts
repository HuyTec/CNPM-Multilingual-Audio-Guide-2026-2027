import { useCallback, useEffect, useRef, useState } from 'react';
import type { DevicePosition } from './osmMap';

const denied = 'Trình duyệt chưa được cấp quyền vị trí. Hãy cho phép Vị trí và Vị trí chính xác cho Safari/Chrome trong cài đặt điện thoại và cài đặt trang. Bạn vẫn có thể tìm địa điểm thủ công.';

export function useBrowserLocation() {
  const [position, setPosition] = useState<DevicePosition | null>(null);
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [error, setError] = useState('');
  const [refining, setRefining] = useState(false);
  const request = useRef(0);

  const acquire = useCallback((allowRecentPosition: boolean) => {
    if (!navigator.geolocation || !window.isSecureContext) {
      setStatus('error');
      setError('Không lấy được vị trí. Hãy mở trang HTTPS bằng Safari/Chrome, thay vì trình duyệt bên trong ứng dụng nhắn tin.');
      return;
    }
    const id = ++request.current;
    let initial: DevicePosition | null = null;
    setStatus('loading'); setError(''); setRefining(false);
    const accept = (result: GeolocationPosition) => {
      const point = { latitude: result.coords.latitude, longitude: result.coords.longitude, accuracy: result.coords.accuracy };
      initial = point; setPosition(point); setStatus('success'); setError('');
    };
    const fail = (failure: GeolocationPositionError) => {
      if (id !== request.current) return;
      setRefining(false);
      if (initial && failure.code !== 1) {
        // A failed precise fix must not discard a usable approximate fix.
        setStatus('success'); return;
      }
      setError(failure.code === 1 ? denied : failure.code === 2
        ? 'Điện thoại chưa cung cấp được vị trí. Hãy bật dịch vụ vị trí, kết nối Wi-Fi/4G và thử ở nơi thoáng.'
        : 'Chưa lấy được vị trí sau khi thử lại. Hãy mở liên kết trực tiếp trong Safari/Chrome, bật Vị trí chính xác và bấm Cập nhật vị trí.');
      setStatus('error');
    };
    const precise = () => {
      if (id !== request.current) return;
      setRefining(true);
      navigator.geolocation.getCurrentPosition(result => {
        if (id !== request.current) return;
        if (!initial || result.coords.accuracy <= initial.accuracy) accept(result);
        setRefining(false);
      }, fail, { enableHighAccuracy: true, timeout: 45_000, maximumAge: 0 });
    };
    navigator.geolocation.getCurrentPosition(result => {
      if (id !== request.current) return;
      accept(result);
      if (result.coords.accuracy > 25) precise();
    }, failure => {
      if (id !== request.current) return;
      if (failure.code === 1) fail(failure); else precise();
    }, { enableHighAccuracy: false, timeout: 12_000, maximumAge: allowRecentPosition ? 30_000 : 0 });
  }, []);
  const locate = useCallback(() => acquire(false), [acquire]);

  useEffect(() => {
    let active = true;
    // The user requested automatic location on entry. Permission still belongs to the browser.
    void Promise.resolve().then(async () => {
      if (!active) return;
      try {
        if (navigator.permissions?.query) {
          const permission = await navigator.permissions.query({ name: 'geolocation' });
          if (!active) return;
          if (permission.state === 'denied') { setStatus('error'); setError(denied); return; }
        }
      } catch { /* Some Safari versions do not expose geolocation through Permissions API. */ }
      if (active) acquire(true);
    });
    return () => { active = false; request.current += 1; };
  }, [acquire]);

  return { position, status, error, refining, locate };
}
