import { supportedLanguages, uuidPattern } from './guide';
import type { TourLocation } from './guide';
import { distanceMeters, hasMapCoordinates } from './osmMap';
import { installedPackages } from './packages';

export type GeofencingMetadata = { surveyedAt: string; cooldownSeconds: number; localizedNames: Record<string, Record<string, string>> };
export type Geofence = { location: TourLocation; names: Record<string, string>; cooldownMs: number };
export function parseGeofences(metadata: GeofencingMetadata | undefined, locations: TourLocation[]): Geofence[] {
  if (!metadata) return [];
  if (!Number.isFinite(Date.parse(metadata.surveyedAt)) || Date.parse(metadata.surveyedAt) > Date.now()
    || !Number.isSafeInteger(metadata.cooldownSeconds) || metadata.cooldownSeconds <= 0
    || metadata.cooldownSeconds > 2_147_483 || !metadata.localizedNames || typeof metadata.localizedNames !== 'object') throw new Error('Dữ liệu khảo sát/cooldown geofence không hợp lệ.');
  return locations.filter(location => Object.hasOwn(metadata.localizedNames, location.locationId)).map(location => {
    const names = metadata.localizedNames[location.locationId];
    if (!uuidPattern.test(location.locationId) || !hasMapCoordinates(location)
      || !Number.isFinite(location.geofenceRadiusM) || location.geofenceRadiusM <= 0
      || !names || supportedLanguages.some(language => typeof names[language] !== 'string' || !names[language].trim()))
      throw new Error('Gói geofence thiếu tọa độ, bán kính hoặc tên địa điểm đã khảo sát cho sáu ngôn ngữ.');
    return { location, names, cooldownMs: metadata.cooldownSeconds * 1000 };
  });
}
export async function localGeofences() {
  const packages = await installedPackages();
  const unique = new Map<string, Geofence>();
  // Prefer the most recently installed signed package if scopes overlap.
  packages.sort((a, b) => a.stored.installedAt.localeCompare(b.stored.installedAt));
  for (const item of packages) for (const fence of parseGeofences(item.payload.geofencing, item.payload.locations)) unique.set(fence.location.locationId, fence);
  return [...unique.values()];
}
/** Accuracy must fit wholly inside/outside the circle. Boundary jitter is not a crossing. */
export function usableGeofencePosition(position: GeolocationPosition, now = Date.now()) {
  const { latitude, longitude, accuracy } = position.coords;
  return [latitude, longitude, accuracy, position.timestamp].every(Number.isFinite)
    && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180 && accuracy >= 0
    && now - position.timestamp <= 15_000 && position.timestamp <= now + 1000;
}
export function geofenceEntries(fences: Geofence[], position: GeolocationPosition, inside: Set<string>, now = Date.now()) {
  const { latitude, longitude, accuracy } = position.coords;
  if (!usableGeofencePosition(position, now)) return [];
  const entries: Geofence[] = [];
  for (const fence of fences) {
    const id = fence.location.locationId;
    const distance = distanceMeters({ latitude, longitude }, fence.location);
    if (distance - accuracy > fence.location.geofenceRadiusM) inside.delete(id);
    else if (distance + accuracy <= fence.location.geofenceRadiusM && !inside.has(id)) { inside.add(id); entries.push(fence); }
  }
  return entries;
}
async function cooldownTransaction<T>(work: (store: IDBObjectStore, result: (value: T) => void) => void): Promise<T> {
  const db = await new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open('travelvoice-geofence-v1', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('cooldowns');
    request.onsuccess = () => resolve(request.result); request.onerror = () => reject(request.error);
  });
  return new Promise((resolve, reject) => {
    const tx = db.transaction('cooldowns', 'readwrite'); let value: T;
    work(tx.objectStore('cooldowns'), result => { value = result; });
    tx.oncomplete = () => { db.close(); resolve(value); };
    tx.onerror = tx.onabort = () => { db.close(); reject(tx.error); };
  });
}
/** Atomic reservation across tabs. Persist before delivery to prevent duplicate alerts. */
export function reserveNotification(id: string, cooldownMs: number, now = Date.now()) {
  return cooldownTransaction<boolean>((store, result) => {
    store.get(id).onsuccess = event => {
      const previous = (event.target as IDBRequest<number | undefined>).result;
      if (previous !== undefined && now - previous < cooldownMs) { result(false); return; }
      store.put(now, id); result(true);
    };
  });
}
export function releaseNotification(id: string, reservation: number) {
  return cooldownTransaction<void>(store => {
    store.get(id).onsuccess = event => { if ((event.target as IDBRequest).result === reservation) store.delete(id); };
  });
}
const messages: Record<string, { title: string; body: (name: string) => string; open: string; dismiss: string }> = {
  VI: { title: 'Địa điểm gần bạn', body: name => `Bạn đang ở gần ${name}. Mở để đọc và chọn nghe thuyết minh.`, open: 'Mở thuyết minh', dismiss: 'Đóng gợi ý' },
  EN: { title: 'A place nearby', body: name => `You are near ${name}. Open the guide to read or choose to listen.`, open: 'Open guide', dismiss: 'Dismiss' },
  FR: { title: 'Un lieu à proximité', body: name => `Vous êtes près de ${name}. Ouvrez le guide pour lire ou choisir de l’écouter.`, open: 'Ouvrir le guide', dismiss: 'Fermer' },
  JA: { title: '近くのスポット', body: name => `${name}の近くにいます。ガイドを開いて、読むか音声再生を選べます。`, open: 'ガイドを開く', dismiss: '閉じる' },
  ZH: { title: '附近的景点', body: name => `您正在${name}附近。打开导览以阅读或选择收听。`, open: '打开导览', dismiss: '关闭' },
  RU: { title: 'Место поблизости', body: name => `Вы рядом с ${name}. Откройте гид, чтобы прочитать или включить аудио.`, open: 'Открыть гид', dismiss: 'Закрыть' },
};
export function geofenceMessage(fence: Geofence, language: string) {
  const copy = messages[language]; const name = fence.names[language];
  if (!copy || !name) return undefined;
  return { ...copy, body: copy.body(name), locationId: fence.location.locationId, language,
    url: `/locations/${fence.location.locationId}?language=${language}&scriptType=FULL` };
}
export async function deliverSystemNotification(message: NonNullable<ReturnType<typeof geofenceMessage>>) {
  if (!('Notification' in window) || Notification.permission !== 'granted') return false;
  const registration = await navigator.serviceWorker?.getRegistration();
  if (!registration?.active || !registration.showNotification) return false;
  await registration.showNotification(message.title, { body: message.body, lang: message.language.toLowerCase(),
    tag: `travelvoice-${message.locationId}`, data: { locationId: message.locationId, language: message.language } });
  return true;
}
