import { createContext, useContext } from "react";
import type { Location } from "../mocks/data";

export type ContentLocation = Location & {
  editVersion?: number;
  updatedBy?: string;
};
export type ContentConfig = {
  categories: string[];
  languages: { code: string; label: string }[];
};
export const AdminSession = createContext<{
  authorization: string;
  setAuthorization: (value: string) => void;
  config?: ContentConfig;
  error?: string;
} | null>(null);
export function useAdminSession() {
  const value = useContext(AdminSession);
  if (!value) throw new Error("Admin session missing");
  return value;
}
/** Credentials stay in tab memory; every request is verified by the backend's existing filter. */
export async function adminRequest<T>(
  path: string,
  authorization: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Authorization", authorization);
  const response = await fetch(path, { ...init, headers, cache: "no-store" });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw Object.assign(
      new Error(
        response.status === 401
          ? "Tên đăng nhập hoặc mật khẩu chưa đúng."
          : response.status === 503
            ? "Backend chưa sẵn sàng hoặc chưa cấu hình tài khoản admin."
            : body.detail ||
              `Yêu cầu chưa thành công (${response.status}). Dữ liệu đang nhập được giữ nguyên.`,
      ),
      { status: response.status },
    );
  }
  return response.json() as Promise<T>;
}
export const languages = [
  { code: "vi", label: "Tiếng Việt" },
  { code: "en", label: "English" },
  { code: "fr", label: "Français" },
  { code: "ja", label: "日本語" },
  { code: "zh", label: "中文" },
  { code: "ru", label: "Русский" },
];
