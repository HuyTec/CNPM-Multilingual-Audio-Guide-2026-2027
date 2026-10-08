import type { ReactNode } from "react";
export function Field({
  id,
  label,
  error,
  help,
  children,
}: {
  id: string;
  label: string;
  error?: string;
  help?: string;
  children: ReactNode;
}) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
      {help && <small id={`${id}-help`}>{help}</small>}
      <span id={`${id}-error`} className="field-error" aria-live="polite">
        {error}
      </span>
    </div>
  );
}
