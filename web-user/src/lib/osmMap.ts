import type { TourLocation } from './guide';

// Demo viewport around the District 1 tour, not an official administrative boundary.
export const districtOne = {
  center: { lat: 10.777, lng: 106.701 },
  bounds: { north: 10.800, south: 10.757, west: 106.685, east: 106.714 },
};

const districtStops = new Set(['101', '102', '103', '107'].map(suffix => `20000000-0000-0000-0000-000000000${suffix}`));

export function isDistrictOneStop(item: TourLocation) {
  return districtStops.has(item.locationId) && Number.isFinite(item.latitude) && Number.isFinite(item.longitude)
    && item.latitude >= districtOne.bounds.south && item.latitude <= districtOne.bounds.north
    && item.longitude >= districtOne.bounds.west && item.longitude <= districtOne.bounds.east;
}

export function osmAndLocationUrl(item: Pick<TourLocation, 'latitude' | 'longitude'>) {
  const { latitude, longitude } = item;
  return `https://osmand.net/map/?pin=${latitude},${longitude}#16/${latitude}/${longitude}`;
}
