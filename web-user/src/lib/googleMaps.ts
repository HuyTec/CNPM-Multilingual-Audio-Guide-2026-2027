import type { TourLocation } from './guide';

// Tourism demo viewport, not an authoritative administrative boundary.
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

type MapInstance = object;
type Marker = HTMLElement & { map: MapInstance | null };
export interface GoogleMaps {
  importLibrary?: (name: string) => Promise<Record<string, unknown>>;
  Map: new (container: HTMLElement, options: {
    center: { lat: number; lng: number }; zoom: number; minZoom: number; maxZoom: number;
    restriction: { latLngBounds: typeof districtOne.bounds; strictBounds: boolean };
    mapId: string; mapTypeControl: boolean; streetViewControl: boolean; fullscreenControl: boolean;
    clickableIcons: boolean; gestureHandling: string; keyboardShortcuts: boolean;
  }) => MapInstance;
  marker: { AdvancedMarkerElement: new (options: { map: MapInstance; position: { lat: number; lng: number }; title: string }) => Marker };
  event: { addListenerOnce: (map: MapInstance, event: string, callback: () => void) => object;
    removeListener: (listener: object) => void; clearInstanceListeners: (map: MapInstance) => void };
}
declare global {
  interface Window {
    google?: { maps: GoogleMaps };
    travelVoiceGoogleMapsReady?: () => void;
    gm_authFailure?: () => void;
  }
}
let pending: Promise<GoogleMaps> | undefined;

/** Load the official SDK once; timeout/network failures permit a later retry. */
export function loadGoogleMaps(key: string): Promise<GoogleMaps> {
  if (window.google?.maps?.marker) return Promise.resolve(window.google.maps);
  if (pending) return pending;
  pending = new Promise<GoogleMaps>((resolve, reject) => {
    const script = document.createElement('script');
    const timer = setTimeout(() => fail(), 15_000);
    function fail() {
      clearTimeout(timer); script.remove(); delete window.travelVoiceGoogleMapsReady;
      pending = undefined; reject(new Error('Google Maps unavailable'));
    }
    window.gm_authFailure = () => { window.dispatchEvent(new Event('google-maps-auth-failure')); fail(); };
    window.travelVoiceGoogleMapsReady = () => {
      void (async () => {
        const maps=window.google?.maps;
        if(!maps)throw new Error('Google Maps unavailable');
        if(maps.importLibrary) await Promise.all([maps.importLibrary('maps'),maps.importLibrary('marker')]);
        if(typeof maps.Map!=='function' || typeof maps.marker?.AdvancedMarkerElement!=='function')throw new Error('Google Maps constructors unavailable');
        clearTimeout(timer);delete window.travelVoiceGoogleMapsReady;resolve(maps);
      })().catch(fail);
    };
    script.async = true;
    script.src = `https://maps.googleapis.com/maps/api/js?${new URLSearchParams({ key, v: 'weekly', libraries: 'marker',
      loading: 'async', callback: 'travelVoiceGoogleMapsReady', language: 'vi', region: 'VN' })}`;
    script.onerror = fail;
    document.head.append(script);
  });
  return pending;
}
