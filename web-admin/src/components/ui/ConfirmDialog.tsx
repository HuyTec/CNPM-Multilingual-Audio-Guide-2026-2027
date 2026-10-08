import { useEffect, useId, useRef, type ReactNode } from "react";
import { Button } from "./Button";
export function ConfirmDialog({
  open,
  title,
  children,
  onCancel,
  onConfirm,
  confirmLabel = "Xác nhận",
}: {
  open: boolean;
  title: string;
  children: ReactNode;
  onCancel: () => void;
  onConfirm?: () => void;
  confirmLabel?: string;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const headingId = useId();
  useEffect(() => {
    const dialog = ref.current;
    const previous = document.activeElement as HTMLElement | null;
    if (open) dialog?.showModal();
    else dialog?.close();
    return () => {
      dialog?.close();
      if (open) previous?.focus();
    };
  }, [open]);
  return (
    <dialog
      ref={ref}
      aria-labelledby={headingId}
      onCancel={(event) => {
        event.preventDefault();
        onCancel();
      }}
    >
      <div className="dialog-body">
        <h2 id={headingId}>{title}</h2>
        {children}
        <div className="actions">
          <Button secondary onClick={onCancel}>
            {onConfirm ? "Hủy" : "Đóng"}
          </Button>
          {onConfirm && <Button onClick={onConfirm}>{confirmLabel}</Button>}
        </div>
      </div>
    </dialog>
  );
}
