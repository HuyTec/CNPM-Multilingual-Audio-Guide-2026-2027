import { useEffect, useRef, useState, type FormEvent } from "react";
import { adminRequest } from "../lib/admin-content";
import { Button } from "./ui/Button";
import { Field } from "./ui/Field";

export function AdminAccess({
  error,
  onAuthenticate,
}: {
  error?: string;
  onAuthenticate: (value: string) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState("");
  const password = useRef<HTMLInputElement>(null);
  useEffect(() => {
    if (error) password.current?.focus();
  }, [error]);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setBusy(true);
    setFailure("");
    try {
      const session = await adminRequest<{ token: string }>(
        "/api/admin/auth/login",
        "",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            username: String(data.get("username") || ""),
            password: String(data.get("password") || ""),
          }),
        },
      );
      onAuthenticate(`Bearer ${session.token}`);
    } catch (error) {
      setFailure(
        error instanceof Error ? error.message : "Chưa đăng nhập được.",
      );
      password.current?.focus();
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="card live-panel admin-login">
      <p className="login-kicker">HVP · KHÔNG GIAN QUẢN TRỊ</p>
      <h2>Đăng nhập quản trị</h2>
      <p>
        Đăng nhập bằng tài khoản được cấp để biên tập hoặc duyệt nội dung thuyết
        minh.
      </p>
      <form onSubmit={submit}>
        <Field id="content-admin-user" label="Tên đăng nhập">
          <input
            id="content-admin-user"
            name="username"
            autoComplete="username"
            defaultValue="editor"
            required
            spellCheck={false}
          />
        </Field>
        <Field
          id="content-admin-password"
          label="Mật khẩu"
          error={failure || error}
        >
          <input
            ref={password}
            id="content-admin-password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            aria-invalid={!!(failure || error)}
            aria-describedby={
              failure || error ? "content-admin-password-error" : undefined
            }
          />
        </Field>
        <Button type="submit" busy={busy}>
          Đăng nhập
        </Button>
      </form>
      <div className="login-help">
        <strong>Lần đầu sử dụng?</strong>
        <p>
          Biên tập viên tạo nội dung và gửi duyệt. Người duyệt kiểm tra rồi xuất
          bản gói cho du khách.
        </p>
        <a href="/help">Xem hướng dẫn thao tác</a>
      </div>
    </section>
  );
}
