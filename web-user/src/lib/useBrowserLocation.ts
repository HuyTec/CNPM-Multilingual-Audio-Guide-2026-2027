import { useCallback, useEffect, useRef, useState } from 'react';
import type { DevicePosition } from './osmMap';

export function useBrowserLocation() {
  const [position, setPosition] = useState<DevicePosition | null>(null);
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [error, setError] = useState('');
  const request = useRef(0);
  useEffect(() => () => { request.current += 1; }, []);

  const locate = useCallback(() => {
    if (!navigator.geolocation || !window.isSecureContext) {
      setStatus('error');
      setError('Không lấy được vị trí. Hãy mở trang bằng HTTPS hoặc localhost trên trình duyệt hỗ trợ vị trí.');
      return;
    }
    const id = ++request.current;
    setStatus('loading');
    setError('');
    navigator.geolocation.getCurrentPosition(result => {
      if (id !== request.current) return;
      setPosition({ latitude: result.coords.latitude, longitude: result.coords.longitude, accuracy: result.coords.accuracy });
      setStatus('success');
    }, failure => {
      if (id !== request.current) return;
      setError(failure.code === 1
        ? 'Bạn chưa cấp quyền vị trí. Hãy cho phép Vị trí trong cài đặt trang rồi thử lại. Bạn vẫn có thể tìm địa điểm thủ công.'
        : failure.code === 2
          ? 'Thiết bị chưa xác định được vị trí. Hãy bật dịch vụ vị trí rồi thử lại.'
          : 'Lấy vị trí quá lâu. Hãy thử lại.');
      setStatus('error');
    }, { enableHighAccuracy: true, timeout: 15_000, maximumAge: 0 });
  }, []);

  return { position, status, error, locate };
}
