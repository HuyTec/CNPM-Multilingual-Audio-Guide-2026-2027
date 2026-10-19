import { useEffect, useState } from "react";

export type CountItem = { label: string; count: number };
export type Analytics = {
  from: string;
  to: string;
  timeZone: string;
  generatedAt: string;
  totalPlaybacks: number;
  onlinePlaybacks: number;
  offlinePlaybacks: number;
  topLocations: CountItem[];
  languages: CountItem[];
};

export function basicCredentials(username: string, password: string) {
  return `Basic ${btoa(String.fromCharCode(...new TextEncoder().encode(`${username}:${password}`)))}`;
}

export function useLiveAnalytics(
  authorization: string,
  from: string,
  to: string,
  revision: number,
) {
  const [result, setResult] = useState<{
    key: string;
    data?: Analytics;
    error?: string;
    unauthorized?: boolean;
  }>({ key: "" });
  const key = `${authorization}:${from}:${to}:${revision}`;
  useEffect(() => {
    if (!authorization) return;
    const controller = new AbortController();
    let fetching = false;
    let rejected = false;
    async function refresh() {
      if (
        fetching ||
        rejected ||
        controller.signal.aborted ||
        document.visibilityState === "hidden"
      )
        return;
      fetching = true;
      try {
        const response = await fetch(
          `/api/admin/analytics?${new URLSearchParams({ from, to })}`,
          {
            headers: { Authorization: authorization },
            signal: controller.signal,
            cache: "no-store",
          },
        );
        if (!response.ok) {
          const body = await response.json().catch(() => ({}));
          throw Object.assign(
            new Error(
              response.status === 401
                ? "Tên đăng nhập hoặc mật khẩu chưa đúng."
                : body.detail ||
                    "Không thể tải số liệu. Kiểm tra backend rồi thử lại.",
            ),
            { unauthorized: response.status === 401 },
          );
        }
        const data = (await response.json()) as Analytics;
        if (!controller.signal.aborted) setResult({ key, data });
      } catch (error) {
        if (!controller.signal.aborted) {
          const failure = error as Error & { unauthorized?: boolean };
          rejected = !!failure.unauthorized;
          setResult({
            key,
            error: failure.message,
            unauthorized: failure.unauthorized,
          });
        }
      } finally {
        fetching = false;
      }
    }
    void refresh();
    const timer = window.setInterval(() => void refresh(), 5000);
    document.addEventListener("visibilitychange", refresh);
    return () => {
      controller.abort();
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", refresh);
    };
  }, [authorization, from, to, revision, key]);
  return result.key === key ? result : { key };
}
