import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  outdated,
  usePackagePolling,
  usePackageUpdates,
} from "../lib/packageUpdates";
import type { PackageManifest } from "../lib/packages";
export function PackageUpdateNotice() {
  usePackagePolling();
  const available = usePackageUpdates();
  const [updates, setUpdates] = useState<PackageManifest[]>([]);
  useEffect(() => {
    let active = true;
    const refresh = () => {
      void outdated(available).then((next) => {
        if (active) setUpdates(next);
      });
    };
    refresh();
    window.addEventListener("travelvoice-content-changed", refresh);
    return () => {
      active = false;
      window.removeEventListener("travelvoice-content-changed", refresh);
    };
  }, [available]);
  if (!updates.length) return null;
  const bytes = updates.reduce(
    (sum, m) =>
      sum + m.sizeBytes + m.assets.reduce((sum, a) => sum + a.sizeBytes, 0),
    0,
  );
  return (
    <aside className="fallback-note" aria-live="polite">
      Có nội dung mới ·{" "}
      {new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(
        bytes / 1024,
      )}{" "}
      KB. <Link to="/offline">Tải về nếu cần</Link>
    </aside>
  );
}
