import {waitForPlayback} from './playbackGate';
import type { Guide, Selection, TourLocation } from './guide';
import { parseGuide, uuidPattern } from './guide';
import type { GeofencingMetadata } from './geofence';

export type PackageAsset = { url: string; sizeBytes: number; sha256: string };
export type PackageManifest = {
  schemaVersion: number; scope: 'BASE' | 'OPTIONAL'; version: string; status: string; isBase: boolean;
  sizeBytes: number; sha256: string; signature: string; keyId: string; downloadUrl: string; assets: PackageAsset[];
};
type Payload = { schemaVersion: number; status: string; scope: string; isBase: boolean;
  locations: TourLocation[]; guides: Guide[]; languages: string[]; assets: PackageAsset[]; geofencing?: GeofencingMetadata };
type StoredPackage = { scope: string; manifest: PackageManifest; bytes: ArrayBuffer; key: string;
  assets: { url: string; bytes: ArrayBuffer }[]; installedAt: string };
const base64 = (value: string) => Uint8Array.from(atob(value), char => char.charCodeAt(0));
async function digest(bytes: BufferSource) {
  return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)), byte => byte.toString(16).padStart(2, '0')).join('');
}
async function database() {
  return new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open('travelvoice-content-v1', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('packages', { keyPath: 'scope' });
    request.onerror = () => reject(new Error('Trình duyệt không cho phép lưu nội dung.'));
    request.onsuccess = () => resolve(request.result);
  });
}
async function allStored(): Promise<StoredPackage[]> {
  const db = await database();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction('packages', 'readonly');
    const request = transaction.objectStore('packages').getAll();
    request.onsuccess = () => resolve(request.result as StoredPackage[]);
    request.onerror = () => reject(request.error);
    transaction.oncomplete = () => db.close();
  });
}
/** Verify signed raw bytes again before use. Never trust a partially installed/edited JSON object. */
export async function verifyPackage(stored: Pick<StoredPackage, 'manifest' | 'bytes' | 'key'>): Promise<Payload> {
  const { manifest, bytes } = stored;
  if (bytes.byteLength !== manifest.sizeBytes || await digest(bytes) !== manifest.sha256) throw new Error('Checksum của gói không khớp.');
  const spki = base64(stored.key);
  if (await digest(spki) !== manifest.keyId) throw new Error('Khóa xác minh không khớp.');
  const key = await crypto.subtle.importKey('spki', spki, { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }, false, ['verify']);
  if (!await crypto.subtle.verify('RSASSA-PKCS1-v1_5', key, base64(manifest.signature), bytes)) throw new Error('Chữ ký gói không hợp lệ.');
  const payload = JSON.parse(new TextDecoder().decode(bytes)) as Payload;
  if (payload.schemaVersion !== 1 || payload.status !== 'APPROVED' || payload.scope !== manifest.scope
    || payload.isBase !== manifest.isBase || payload.isBase !== (payload.scope === 'BASE')
    || manifest.version !== `${manifest.scope.toLowerCase()}-${manifest.sha256}`
    || !Array.isArray(payload.locations) || !Array.isArray(payload.guides) || !Array.isArray(payload.assets)
    || payload.locations.some(item => !uuidPattern.test(item.locationId) || typeof item.name !== 'string')) throw new Error('Cấu trúc gói không hợp lệ.');
  for (const guide of payload.guides) {
    parseGuide(guide, { locationId: guide.locationId, language: guide.languageCode, scriptType: guide.scriptType });
    if (!payload.locations.some(item => item.locationId === guide.locationId)) throw new Error('Bài không thuộc gói.');
    if (guide.audioUrl && !payload.assets.some(asset => asset.url === guide.audioUrl)) throw new Error('Gói thiếu audio đã khai báo.');
  }
  return payload;
}
export async function installedPackages() {
  const packages = await allStored();
  return Promise.all(packages.map(async stored => {
    const payload = await verifyPackage(stored);
    if (!Array.isArray(stored.assets) || stored.assets.length !== payload.assets.length) throw new Error('Gói đã lưu thiếu audio. Hãy tải lại khi có mạng.');
    for (const expected of payload.assets) {
      const asset = stored.assets.find(item => item.url === expected.url);
      if (!asset || asset.bytes.byteLength !== expected.sizeBytes || await digest(asset.bytes) !== expected.sha256)
        throw new Error('Audio đã lưu không còn toàn vẹn. Hãy tải lại gói khi có mạng.');
    }
    return { stored, payload };
  }));
}
/** Retry only transport failures; signature/checksum validation still runs once on the complete bytes. */
async function fetchPackageBytes(url: string, signal: AbortSignal, progress: (message: string) => void): Promise<ArrayBuffer> {
  for (let attempt = 0; attempt < 3; attempt++) {
    signal.throwIfAborted();
    try {
      const response = await fetch(url, { signal: AbortSignal.any([signal, AbortSignal.timeout(45_000)]), cache: 'no-store' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return await response.arrayBuffer();
    } catch {
      signal.throwIfAborted();
      if (attempt === 2) throw new Error('Kết nối tải gói bị gián đoạn sau 3 lần thử. Kiểm tra mạng rồi tải lại; nội dung cũ vẫn được giữ.');
      progress(`Kết nối bị gián đoạn. Đang thử tải lại (${attempt + 2}/3)…`);
      await new Promise(resolve => setTimeout(resolve, 500));
    }
  }
  throw new Error('Chưa tải được nội dung.');
}
export async function downloadPackage(manifest: PackageManifest, signal: AbortSignal, progress: (message: string) => void) {
  if (!crypto.subtle || !navigator.storage?.estimate) throw new Error('Môi trường lưu trữ/xác minh chưa được hỗ trợ.');
  const size = manifest.sizeBytes + manifest.assets.reduce((total, asset) => total + asset.sizeBytes, 0);
  if (!Number.isSafeInteger(size) || size <= 0 || size > 150 * 1024 * 1024) throw new Error('Kích thước gói không hợp lệ.');
  const estimate = await navigator.storage.estimate();
  if (estimate.quota !== undefined && estimate.usage !== undefined && estimate.quota - estimate.usage < size * 1.2) throw new Error('Không đủ dung lượng. Phiên bản đã cài vẫn được giữ.');
  if (manifest.downloadUrl !== `/api/content-packages/${manifest.scope}/${manifest.version}`) throw new Error('Đường dẫn gói không hợp lệ.');
  progress('Đang tải và xác minh nội dung…');
  const [keyBytes, bytes] = await Promise.all([fetchPackageBytes('/api/content-packages/key', signal, progress), fetchPackageBytes(manifest.downloadUrl, signal, progress)]);
  const keyData = JSON.parse(new TextDecoder().decode(keyBytes)) as { spki: string };
  const payload = await verifyPackage({ manifest, bytes, key: keyData.spki });
  const assets: StoredPackage['assets'] = [];
  for (const [index, asset] of payload.assets.entries()) {
    if (!/^(?:\/demo-audio\/[a-z-]+\.wav|\/api\/content-audio\/[0-9a-f-]{36}\.(?:mp3|wav|ogg))$/.test(asset.url)) throw new Error('Nguồn audio trong gói không hợp lệ.');
    progress(`Đang tải audio ${new Intl.NumberFormat('vi-VN').format(index + 1)} / ${new Intl.NumberFormat('vi-VN').format(payload.assets.length)}…`);
    const bytes = await fetchPackageBytes(asset.url, signal, progress);
    if (bytes.byteLength !== asset.sizeBytes || await digest(bytes) !== asset.sha256) throw new Error('Audio không qua kiểm tra toàn vẹn.');
    assets.push({ url: asset.url, bytes });
  }
  signal.throwIfAborted();
  progress('Đang chờ kết thúc lượt nghe trước khi cài gói…');
  await waitForPlayback(signal);
  progress('Đang cài đặt gói đã xác minh…');
  const db = await database();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction('packages', 'readwrite');
    tx.objectStore('packages').put({ scope: manifest.scope, manifest, bytes, key: keyData.spki, assets, installedAt: new Date().toISOString() } satisfies StoredPackage);
    const abort = () => { try { tx.abort(); } catch { /* Already committed. */ } };
    signal.addEventListener('abort', abort, { once: true });
    tx.oncomplete = () => { signal.removeEventListener('abort', abort); db.close(); window.dispatchEvent(new Event('travelvoice-content-changed')); resolve(); };
    tx.onabort = tx.onerror = () => { signal.removeEventListener('abort', abort); db.close(); reject(new Error('Chưa cài được gói; phiên bản cũ vẫn giữ nguyên.')); };
  });
}
export async function deleteOptionalPackage(scope: string) {
  if (scope !== 'OPTIONAL') throw new Error('Nội dung cơ bản được bảo vệ.');
  const db = await database();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction('packages', 'readwrite'); tx.objectStore('packages').delete(scope);
    tx.oncomplete = () => { db.close(); window.dispatchEvent(new Event('travelvoice-content-changed')); resolve(); }; tx.onerror = () => { db.close(); reject(tx.error); };
  });
}
export async function offlineGuide(selection: Selection): Promise<Guide | undefined> {
  const packages = await installedPackages();
  const guides = packages.flatMap(item => item.payload.guides).filter(guide => guide.locationId === selection.locationId && guide.scriptType === selection.scriptType);
  for (const language of [...new Set([selection.language, 'EN', 'VI'])]) {
    const guide = guides.find(item => item.languageCode === language);
    if (!guide) continue;
    const stored = packages.find(item => item.payload.guides.some(value => value.contentId === guide.contentId))!.stored;
    const asset = stored.assets.find(item => item.url === guide.audioUrl);
    if (asset) {
      const expected = packages.flatMap(item => item.payload.assets).find(item => item.url === asset.url);
      if (!expected || await digest(asset.bytes) !== expected.sha256) throw new Error('Audio đã lưu không còn toàn vẹn.');
    }
    const mime=asset?.url.endsWith('.mp3')?'audio/mpeg':asset?.url.endsWith('.ogg')?'audio/ogg':'audio/wav';
    return { ...guide, audioUrl: asset ? URL.createObjectURL(new Blob([asset.bytes], { type: mime })) : null };
  }
}
export async function offlineLocations() {
  // Custom images are currently served online; do not render broken network images offline.
  return (await installedPackages()).flatMap(item => item.payload.locations).map(item => ({ ...item, imageUrl: undefined }));
}
