import { useEffect, useState, type ReactNode } from "react";
import {
  AdminSession,
  adminRequest,
  useAdminSession,
  type ContentConfig,
} from "../lib/admin-content";
import { AdminAccess } from "./AdminAccess";
import { LoadingState } from "./ui/States";

export function AdminProvider({ children }: { children: ReactNode }) {
  const [authorization, setAuth] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState<{
    key: string;
    config?: ContentConfig;
    identity?: { username: string; role: "EDITOR" | "REVIEWER" };
    error?: string;
  }>({ key: "" });
  const key = `${authorization}:${attempt}`;
  useEffect(() => {
    if (!authorization) return;
    let active = true;
    const abort = new AbortController();
    const timer = setTimeout(() => abort.abort(), 12_000);
    void adminRequest<ContentConfig>(
      "/api/admin/content/config",
      authorization,
      { signal: abort.signal },
    )
      .then(async (config) => {
        const identity = await adminRequest<{
          username: string;
          role: "EDITOR" | "REVIEWER";
        }>("/api/admin/auth/me", authorization, { signal: abort.signal });
        if (
          !Array.isArray(config.categories) ||
          !Array.isArray(config.languages)
        )
          throw new Error("Cấu hình nội dung không hợp lệ.");
        if (active) setResult({ key, config, identity });
      })
      .catch((error) => {
        if (active)
          setResult({
            key,
            error:
              error instanceof Error
                ? error.message
                : "Chưa kết nối được backend.",
          });
      })
      .finally(() => clearTimeout(timer));
    return () => {
      active = false;
      clearTimeout(timer);
      abort.abort();
    };
  }, [authorization, key]);
  function connect(value: string) {
    if (!value && authorization)
      void adminRequest("/api/admin/auth/logout", authorization, {
        method: "POST",
      }).catch(() => {});
    setAuth(value);
    setAttempt((current) => current + 1);
  }
  const current = result.key === key ? result : undefined;
  return (
    <AdminSession.Provider
      value={{
        authorization,
        setAuthorization: connect,
        config: current?.config,
        identity: current?.identity,
        error: current?.error,
      }}
    >
      {children}
    </AdminSession.Provider>
  );
}
export function AdminContentGate({ children }: { children: ReactNode }) {
  const { authorization, setAuthorization, config, error } = useAdminSession();
  if (!authorization || error)
    return (
      <>
        <h1>Quản trị nội dung</h1>
        <AdminAccess error={error} onAuthenticate={setAuthorization} />
      </>
    );
  if (!config) return <LoadingState />;
  return <>{children}</>;
}
