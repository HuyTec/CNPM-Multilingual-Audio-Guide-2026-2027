import { useEffect, useState } from 'react';
import { uuidPattern } from './guide';
import type { TourLocation } from './guide';
import { offlineLocations } from './packages';

type Page = { items: TourLocation[]; totalPages: number };
type Snapshot = { query: string; items: TourLocation[]; status: 'ready' | 'error'; offline: boolean; message?: string };

export function useLocationCatalog(query: string) {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const refresh = () => setAttempt(value => value + 1);
    window.addEventListener('travelvoice-content-changed', refresh);
    window.addEventListener('online', refresh);
    return () => {
      window.removeEventListener('travelvoice-content-changed', refresh);
      window.removeEventListener('online', refresh);
    };
  }, []);
  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), 30_000);
    async function load() {
      try {
        const items: TourLocation[] = [];
        const language=(new URLSearchParams(query).get("language")||"vi").toLowerCase();
        const ids = new Set<string>();
        let pages = 1;
        for (let page = 0; page < pages; page++) {
          const parameters = new URLSearchParams(query);
          parameters.set('page', String(page));
          parameters.set('size', '50');
          const response = await fetch(`/api/locations?${parameters}`, { signal: controller.signal, cache: 'no-store' });
          // A rejected query or withdrawn category must not resurrect stale offline results.
          if (response.status >= 400 && response.status < 500) {
            if (active) setSnapshot({ query, items: [], status: 'error', offline: false,
              message: response.status === 404 ? 'Danh mục không còn khả dụng. Hãy chọn danh mục khác hoặc tất cả địa điểm.' : 'Yêu cầu tìm kiếm chưa hợp lệ. Hãy kiểm tra từ khóa và danh mục.' });
            return;
          }
          if (!response.ok) throw new Error();
          const data = await response.json() as Page;
          if (!Array.isArray(data.items) || !Number.isInteger(data.totalPages) || data.totalPages < 0
            || data.items.some(item => !uuidPattern.test(item.locationId) || typeof item.name !== 'string' || ids.has(item.locationId))) throw new Error();
          data.items.forEach(item => ids.add(item.locationId));
          items.push(...data.items.map(item=>({...item,name:item.names?.[language]||item.names?.vi||item.name})));
          pages = data.totalPages;
        }
        if (active) setSnapshot({ query, items, status: 'ready', offline: false });
      } catch {
        if (!active) return;
        try {
          const requested=(new URLSearchParams(query).get("language")||"vi").toLowerCase();
          const local = (await offlineLocations()).map(item=>({...item,name:item.names?.[requested]||item.names?.vi||item.name}));
          if (!local.length) throw new Error();
          const parameters = new URLSearchParams(query);
          const keyword = (parameters.get('keyword') || '').toLocaleLowerCase('vi-VN');
          const category = parameters.get('categoryId');
          const items = local.filter(item => (!category || item.categoryId === category) && item.name.toLocaleLowerCase('vi-VN').includes(keyword))
            .sort((a, b) => a.name.localeCompare(b.name, 'vi-VN'));
          if (active) setSnapshot({ query, items, status: 'ready', offline: true });
        } catch { if (active) setSnapshot({ query, items: [], status: 'error', offline: false }); }
      } finally { window.clearTimeout(timer); }
    }
    void load();
    return () => { active = false; window.clearTimeout(timer); controller.abort(); };
  }, [query, attempt]);
  return {
    items: snapshot?.query === query ? snapshot.items : [],
    loading: snapshot?.query !== query,
    error: snapshot?.query === query && snapshot.status === 'error',
    errorMessage: snapshot?.query === query ? snapshot.message : undefined,
    offline: snapshot?.query === query && snapshot.offline,
    retry: () => { setSnapshot(null); setAttempt(value => value + 1); },
  };
}
