import L from 'leaflet';

export type MapTileState = 'loading' | 'ready' | 'fallback' | 'error';

// Only request visible tiles. Do not prefetch or add these to the offline package.
const sources = [
  { url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>' },
  { url: 'https://a.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>, style <a href="https://www.hotosm.org/">HOT</a>, hosting <a href="https://www.openstreetmap.fr/">OSM France</a>' },
];

export function attachMapTiles(map: L.Map, update: (state: MapTileState) => void) {
  let disposed = false;
  let layer: L.TileLayer;
  let timer: number;
  let source = 0;
  let failures = 0;
  let transition: number | undefined;
  function fail() {
    if (disposed || transition !== undefined) return;
    window.clearTimeout(timer);
    // Wait until Leaflet finishes dispatching zoom/viewreset. remove() must run
    // before off(): Leaflet's own remove handler detaches map event listeners.
    transition = window.setTimeout(() => {
      if (disposed) return;
      layer.remove(); layer.off();
      transition = undefined;
      if (source + 1 < sources.length) {
        source++; start();
      } else update('error');
    }, 0);
  }
  function start() {
    failures = 0;
    layer = L.tileLayer(sources[source].url, { maxZoom: 19, referrerPolicy: 'strict-origin-when-cross-origin',
      attribution: sources[source].attribution });
    const pending = () => {
      window.clearTimeout(timer); failures = 0;
      timer = window.setTimeout(fail, 10_000);
    };
    layer.on('loading', pending);
    layer.on('tileerror', () => { if (++failures >= 3) fail(); });
    layer.on('load', () => {
      window.clearTimeout(timer);
      if (failures) fail(); else if (!disposed) update(source ? 'fallback' : 'ready');
    });
    pending(); layer.addTo(map);
  }
  start();
  return () => { disposed = true; window.clearTimeout(timer); window.clearTimeout(transition); layer.remove(); layer.off(); };
}
