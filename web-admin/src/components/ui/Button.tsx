import type { ButtonHTMLAttributes } from "react";
import { LoaderCircle } from "lucide-react";
export function Button({
  busy,
  secondary,
  children,
  className = "",
  disabled,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  busy?: boolean;
  secondary?: boolean;
}) {
  return (
    <button
      type="button"
      {...props}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      className={`button ${secondary ? "secondary" : ""} ${className}`}
    >
      {busy && (
        <span className="spinner" aria-hidden="true">
          <LoaderCircle size={16} />
        </span>
      )}
      {children}
    </button>
  );
}
