import { useEffect, useRef, type FormEvent } from "react";
import { basicCredentials } from "../lib/live-analytics";
import { Button } from "./ui/Button";
import { Field } from "./ui/Field";

export function AdminAccess({
  error,
  onAuthenticate,
}: {
  error?: string;
  onAuthenticate: (value: string) => void;
}) {
  const password = useRef<HTMLInputElement>(null);
  useEffect(() => {
    if (error) password.current?.focus();
  }, [error]);
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    onAuthenticate(
      basicCredentials(
        String(data.get("username") || ""),
        String(data.get("password") || ""),
      ),
    );
  }
  return (
    <section className="card live-panel">
      <h2>Đăng nhập quản trị demo</h2>
      <p>
        Tài khoản được kiểm tra tại backend. Phiên chỉ giữ trong bộ nhớ của tab.
      </p>
      <form onSubmit={submit}>
        <Field id="content-admin-user" label="Tên đăng nhập">
          <input
            id="content-admin-user"
            name="username"
            autoComplete="username"
            defaultValue="admin"
            required
            spellCheck={false}
          />
        </Field>
        <Field id="content-admin-password" label="Mật khẩu" error={error}>
          <input
            ref={password}
            id="content-admin-password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            aria-invalid={!!error}
            aria-describedby={
              error ? "content-admin-password-error" : undefined
            }
          />
        </Field>
        <Button type="submit">Đăng nhập</Button>
      </form>
    </section>
  );
}
