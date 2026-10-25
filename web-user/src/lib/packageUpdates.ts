import { useEffect, useSyncExternalStore } from "react";
import { installedPackages, type PackageManifest } from "./packages";
let snapshot: PackageManifest[] = [];
let etag = "";
let loading: Promise<void> | null = null;
const listeners = new Set<() => void>();
const subscribe = (callback: () => void) => {
  listeners.add(callback);
  return () => {
    listeners.delete(callback);
  };
};
export async function checkPackageUpdates({ force = false }: { force?: boolean } = {}) {
  if (loading) return loading;
  loading = (async () => {
    const response = await fetch("/api/content-packages", {
      headers: etag && !force ? { "If-None-Match": etag } : {},
      cache: "no-cache",
      signal: AbortSignal.timeout(15000),
    });
    if (response.status === 304) return;
    if (!response.ok) throw new Error("Chưa kiểm tra được gói mới.");
    const next = (await response.json()) as PackageManifest[];
    if (!Array.isArray(next) || next.some((m) => m.status !== "APPROVED"))
      throw new Error("Manifest không hợp lệ.");
    snapshot = next;
    etag = response.headers.get("ETag") || "";
    listeners.forEach((f) => f());
  })().finally(() => {
    loading = null;
  });
  return loading;
}
export function usePackageUpdates() {
  return useSyncExternalStore(subscribe, () => snapshot);
}
export function usePackagePolling() {
  useEffect(() => {
    const refresh = () => {
      if (document.visibilityState !== "hidden" && navigator.onLine)
        void checkPackageUpdates().catch(() => {});
    };
    refresh();
    const timer = setInterval(refresh, 30000);
    window.addEventListener("online", refresh);
    document.addEventListener("visibilitychange", refresh);
    return () => {
      clearInterval(timer);
      window.removeEventListener("online", refresh);
      document.removeEventListener("visibilitychange", refresh);
    };
  }, []);
}
export async function outdated(manifests: PackageManifest[]) {
  const installed = await installedPackages().catch(() => []);
  return manifests.filter(
    (m) =>
      !installed.some(
        (p) =>
          p.stored.scope === m.scope && p.stored.manifest.version === m.version,
      ),
  );
}
