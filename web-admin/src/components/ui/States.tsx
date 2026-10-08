import { Button } from "./Button";
import { number } from "../../lib/format";
export function LoadingState() {
  return (
    <div className="empty" role="status">
      Đang tải dữ liệu…
    </div>
  );
}
export function ErrorState({ retry }: { retry: () => void }) {
  return (
    <div className="notice error" role="status">
      <p>Không thể tải dữ liệu demo. Hãy thử lại.</p>
      <Button secondary onClick={retry}>
        Thử lại
      </Button>
    </div>
  );
}
export function Pagination({
  page,
  total,
  onChange,
}: {
  page: number;
  total: number;
  onChange: (page: number) => void;
}) {
  const pages = Math.max(1, Math.ceil(total / 10));
  return (
    <nav aria-label="Phân trang" className="pagination">
      <span>
        {number(total)} bản ghi · Trang {number(page)} / {number(pages)}
      </span>
      <Button secondary disabled={page <= 1} onClick={() => onChange(page - 1)}>
        Trước
      </Button>
      <Button
        secondary
        disabled={page >= pages}
        onClick={() => onChange(page + 1)}
      >
        Sau
      </Button>
    </nav>
  );
}
