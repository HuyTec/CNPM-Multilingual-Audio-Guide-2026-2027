import type { ReactNode } from "react";
import { FolderOpen } from "lucide-react";
export function EmptyState({
  title = "Chưa có dữ liệu",
  children,
}: {
  title?: string;
  children?: ReactNode;
}) {
  return (
    <div className="empty">
      <FolderOpen size={32} aria-hidden="true" />
      <h3>{title}</h3>
      {children}
    </div>
  );
}
