import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { tourCategoryId, uuidPattern } from '../lib/guide';
import { offlineLocations } from '../lib/packages';
import { categoryDisplayName } from '../lib/catalogLabels';

type Category = { categoryId: string; name: string; locationCount: number };

export function CategoryFilter() {
  const [params, setParams] = useSearchParams();
  const selected = params.get('categoryId') || '';
  const [categories, setCategories] = useState<Category[]>([]);
  const [status, setStatus] = useState('Đang tải danh mục…');
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), 10_000);
    async function load() {
      try {
        const response = await fetch('/api/categories', { signal: controller.signal, cache: 'no-store' });
        if (!response.ok) throw new Error();
        const data = await response.json() as Category[];
        if (!Array.isArray(data) || data.some(item => !uuidPattern.test(item.categoryId) || typeof item.name !== 'string')) throw new Error();
        if (active) { setCategories(data); setStatus(''); }
      } catch {
        if (!active) return;
        try {
          const locations = await offlineLocations();
          if (!locations.length) throw new Error();
          const groups = new Map<string, Category>();
          for (const item of locations) {
            const group = groups.get(item.categoryId) || { categoryId: item.categoryId, name: item.categoryName, locationCount: 0 };
            group.locationCount++; groups.set(item.categoryId, group);
          }
          if (active) { setCategories([...groups.values()]); setStatus('Danh mục từ gói offline đã xác minh.'); }
        } catch { if (active) setStatus('Chưa tải được danh mục.'); }
      } finally { window.clearTimeout(timer); }
    }
    void load();
    return () => { active = false; controller.abort(); window.clearTimeout(timer); };
  }, [attempt]);
  return <div className="category-filter">
    <label htmlFor="discovery-category">Danh mục</label>
    <select id="discovery-category" name="categoryId" autoComplete="off" value={selected} onChange={event => {
      const next = new URLSearchParams(params); next.set('categoryId', event.target.value);
      next.delete('page'); next.delete('nearbyPage'); next.delete('nearbyExpanded'); next.delete('keyword'); next.delete('place'); setParams(next);
    }}>
      <option value="">Tất cả địa điểm</option>
      {selected && !categories.some(item => item.categoryId === selected) && <option value={selected}>{selected === tourCategoryId ? 'Tuyến tham quan hiện tại' : 'Danh mục theo đường dẫn'}</option>}
      {categories.map(item => <option key={item.categoryId} value={item.categoryId}>{categoryDisplayName(item.name)} ({new Intl.NumberFormat('vi-VN').format(item.locationCount)})</option>)}
    </select>
    <p className="player-hint" role="status">{status}</p>
    {status === 'Chưa tải được danh mục.' && <button className="secondary-button" onClick={() => { setStatus('Đang tải danh mục…'); setAttempt(value => value + 1); }}>Thử tải danh mục</button>}
  </div>;
}
