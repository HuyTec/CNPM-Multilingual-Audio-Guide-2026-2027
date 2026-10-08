import { useEffect, useState } from "react";
export function useDemoLoad(key: string, scenario: string) {
  const [loaded, setLoaded] = useState("");
  useEffect(() => {
    const timer = setTimeout(() => setLoaded(key), 350);
    return () => clearTimeout(timer);
  }, [key]);
  return loaded !== key ? "loading" : scenario === "error" ? "error" : "ready";
}
export function pageNumber(value: string | null, total: number) {
  return Math.min(
    Math.max(1, Math.ceil(total / 10)),
    Math.max(1, Math.floor(Number(value)) || 1),
  );
}
