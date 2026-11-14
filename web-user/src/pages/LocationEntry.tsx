import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { uuidPattern } from '../lib/guide';
import { LocationCatalog } from '../components/LocationCatalog';

export function LocationEntry() {
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const input = useRef<HTMLInputElement>(null);
  function openLocation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      const url = new URL(input.current!.value.trim());
      const match = url.pathname.match(/^\/locations\/([^/]+)\/?$/);
      if (!['http:', 'https:'].includes(url.protocol) || !match || !uuidPattern.test(match[1])) throw new Error();
      navigate(`/locations/${match[1].toLowerCase()}${url.search}`);
    } catch {
      setError('Đường dẫn chưa hợp lệ. Hãy sao chép đầy đủ liên kết địa điểm rồi thử lại.');
      input.current?.focus();
    }
  }
  return <>
    <LocationCatalog />
    <details className="link-entry"><summary>Bạn có đường dẫn địa điểm?</summary>
    <p>Dán liên kết hoặc quét mã QR để mở thẳng bài thuyết minh.</p>
    <form onSubmit={openLocation} noValidate>
      <label htmlFor="location-link">Đường dẫn địa điểm</label>
      <input ref={input} id="location-link" name="locationLink" type="url" autoComplete="off" spellCheck={false}
        placeholder="https://travelvoice.example/locations/…" aria-invalid={!!error} aria-describedby="link-error" />
      <p id="link-error" className="field-error" aria-live="polite">{error}</p>
      <button type="submit" className="primary-button">Mở thuyết minh <span aria-hidden="true">→</span></button>
    </form>
    </details>
  </>;
}
