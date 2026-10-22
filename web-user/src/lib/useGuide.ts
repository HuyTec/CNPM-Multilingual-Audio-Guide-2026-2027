import { useEffect, useState } from 'react';
import { parseGuide } from './guide';
import type { Guide, Selection } from './guide';
import { offlineGuide } from './packages';

type GuideState = { status: 'loading' } | { status: 'ready'; guide: Guide; offline: boolean }
  | { status: 'unavailable' } | { status: 'error'; message: string };

/** The reader is keyed by selection; abort plus active guard rejects late responses. */
export function useGuide({ locationId, language, scriptType }: Selection): GuideState {
  const [state, setState] = useState<GuideState>({ status: 'loading' });
  useEffect(() => {
    let active = true;
    let localAudio: string | undefined;
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), 15_000);
    const query = new URLSearchParams({ language, scriptType });
    async function load() {
      let allowOffline = true;
      try {
        const response = await fetch(`/api/locations/${locationId}/guide?${query}`, {
          signal: controller.signal, cache: 'no-store', headers: { Accept: 'application/json' },
        });
        if (response.status === 404) {
          if (active) setState({ status: 'unavailable' });
          return;
        }
        if (!response.ok) throw new Error('Request failed');
        allowOffline = false;
        const guide = parseGuide(await response.json(), { locationId, language, scriptType });
        if (active) setState({ status: 'ready', guide, offline: false });
      } catch {
        if (active && allowOffline) {
          try {
            const guide = await offlineGuide({ locationId, language, scriptType });
            if (guide?.audioUrl?.startsWith('blob:')) localAudio = guide.audioUrl;
            if (active && guide) { setState({ status: 'ready', guide, offline: true }); return; }
            if (!active && localAudio) URL.revokeObjectURL(localAudio);
          } catch { /* Unverified/missing local data must never be presented as ready. */ }
        }
        if (active) setState({ status: 'error', message: controller.signal.aborted
          ? 'Kết nối quá lâu. Hãy kiểm tra mạng và thử lại.'
          : 'Chưa thể tải bài thuyết minh. Hãy kiểm tra kết nối và thử lại.' });
      } finally {
        window.clearTimeout(timer);
      }
    }
    void load();
    return () => { active = false; window.clearTimeout(timer); controller.abort(); if (localAudio) URL.revokeObjectURL(localAudio); };
  }, [locationId, language, scriptType]);
  return state;
}
