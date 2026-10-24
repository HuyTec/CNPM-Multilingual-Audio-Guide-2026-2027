import type { TourLocation } from './guide';

// Approximate demo viewport including Districts 1 and 3; not an official boundary.
export const mapArea = {
  center: { lat: 10.781, lng: 106.692 },
  bounds: { north: 10.805, south: 10.755, west: 106.665, east: 106.715 },
};

export function hasMapCoordinates(item: TourLocation) {
  return Number.isFinite(item.latitude) && Number.isFinite(item.longitude)
    && Math.abs(item.latitude) <= 90 && Math.abs(item.longitude) <= 180;
}

export type DevicePosition = { latitude: number; longitude: number; accuracy: number };

/** Great-circle distance in metres, independent of road routes. */
export function distanceMeters(from: Pick<DevicePosition, 'latitude' | 'longitude'>, to: TourLocation) {
  const radians = Math.PI / 180;
  const latitudeDelta = (to.latitude - from.latitude) * radians;
  const longitudeDelta = (to.longitude - from.longitude) * radians;
  const a = Math.sin(latitudeDelta / 2) ** 2 + Math.cos(from.latitude * radians)
    * Math.cos(to.latitude * radians) * Math.sin(longitudeDelta / 2) ** 2;
  return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
}
