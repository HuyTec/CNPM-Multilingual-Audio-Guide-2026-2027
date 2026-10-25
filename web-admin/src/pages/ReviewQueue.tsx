import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  adminRequest,
  useAdminSession,
  type ContentLocation,
} from "../lib/admin-content";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
type Job = {
  id: string;
  locationId: string;
  status: string;
  errorLog?: string;
  createdAt: string;
};
export default function ReviewQueue() {
  const { authorization, identity } = useAdminSession();
  const [params, setParams] = useSearchParams();
  const [data, setData] = useState<{
    queue: ContentLocation[];
    jobs: Job[];
  } | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    if (identity?.role !== "REVIEWER") return;
    let active = true;
    const abort = new AbortController();
    async function load() {
      try {
        const [queue, jobs] = await Promise.all([
          adminRequest<ContentLocation[]>(
            "/api/admin/review/locations",
            authorization,
            { signal: abort.signal },
          ),
          adminRequest<Job[]>("/api/admin/review/jobs", authorization, {
            signal: abort.signal,
          }),
        ]);
        if (active) {
          setData({ queue, jobs });
          setError("");
        }
      } catch (e) {
        if (active)
          setError(e instanceof Error ? e.message : "Không tải được hàng đợi.");
      }
    }
    void load();
    const timer = setInterval(() => void load(), 5000);
    return () => {
      active = false;
      abort.abort();
      clearInterval(timer);
    };
  }, [authorization, identity?.role, revision]);
  const selected = data?.queue.find((p) => p.id === params.get("location"));
  const totalPages = Math.max(1, Math.ceil((data?.queue.length || 0) / 20));
  const page = Math.max(
    1,
    Math.min(totalPages, Number(params.get("page")) || 1),
  );
  const visible = data?.queue.slice((page - 1) * 20, page * 20);
  function turnPage(next: number) {
    const query = new URLSearchParams(params);
    query.set("page", String(next));
    setParams(query);
  }
  async function decide(action: "approve" | "reject") {
    if (!selected) return;
    if (action === "reject" && !comment.trim()) {
      setMessage("Nhập nhận xét trước khi từ chối.");
      document.getElementById("review-comment")?.focus();
      return;
    }
    if (
      action === "approve" &&
      !window.confirm("Chấp thuận và tạo gói công khai cho bản này?")
    )
      return;
    setBusy(true);
    try {
      await adminRequest(
        `/api/admin/review/locations/${selected.id}/${action}`,
        authorization,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ editVersion: selected.editVersion, comment }),
        },
      );
      setMessage(
        action === "approve"
          ? "Đã chấp thuận. Tác vụ tạo gói được lưu trong DB; chỉ công bố sau khi xác minh."
          : "Đã từ chối và gửi nhận xét cho EDITOR.",
      );
      setComment("");
      setRevision((n) => n + 1);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "Chưa lưu được quyết định.");
    } finally {
      setBusy(false);
    }
  }
  async function retry(id: string) {
    setBusy(true);
    try {
      await adminRequest(`/api/admin/review/jobs/${id}/retry`, authorization, {
        method: "POST",
      });
      setRevision((n) => n + 1);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : "Chưa thử lại được.");
    } finally {
      setBusy(false);
    }
  }
  if (identity?.role !== "REVIEWER")
    return (
      <>
        <h1>Duyệt và xuất bản</h1>
        <p>Chỉ REVIEWER được truy cập.</p>
      </>
    );
  return (
    <>
      <h1>Duyệt và xuất bản</h1>
      <p role="status" aria-live="polite">
        {message}
      </p>
      {error && (
        <div role="alert">
          {error}
          <Button onClick={() => setRevision((n) => n + 1)}>Thử lại</Button>
        </div>
      )}
      {!data && !error && <p role="status">Đang tải…</p>}
      {data?.queue.length === 0 && <p>Không có bản chờ duyệt.</p>}
      <section className="card">
        <h2>Hàng đợi</h2>
        {visible?.map((p) => (
          <p key={p.id}>
            <Link
              to={`?page=${page}&location=${p.id}`}
              onClick={() => setComment("")}
            >
              {p.name || "POI chưa có tên"}
            </Link>{" "}
            · {p.status}
          </p>
        ))}
        {totalPages > 1 && (
          <nav aria-label="Phân trang bản chờ duyệt">
            <Button disabled={page === 1} onClick={() => turnPage(page - 1)}>
              Trang trước
            </Button>
            <span>
              {" "}
              Trang {new Intl.NumberFormat("vi-VN").format(page)} /{" "}
              {new Intl.NumberFormat("vi-VN").format(totalPages)}{" "}
            </span>
            <Button
              disabled={page === totalPages}
              onClick={() => turnPage(page + 1)}
            >
              Trang sau
            </Button>
          </nav>
        )}
      </section>
      {selected && (
        <section className="card">
          <h2>So sánh bản nội dung</h2>
          <p>Bản công khai: {selected.published?.name || "Chưa xuất bản"}</p>
          <p>
            Bản đề xuất: {selected.name} · {selected.category} ·{" "}
            {selected.latitude}, {selected.longitude} · {selected.radius} m
          </p>
          <p>
            Danh mục/vị trí đang công khai:{" "}
            {selected.published?.category || "Chưa có"} ·{" "}
            {selected.published?.latitude}, {selected.published?.longitude} ·{" "}
            {selected.published?.radius} m
          </p>
          {Object.entries(selected.names || {}).map(([l, n]) => (
            <p key={l}>
              {l}: {selected.published?.names?.[l] || "Chưa có"} → {n} ·{" "}
              {selected.fieldOrigins?.[`name:${l}`] || "MANUAL"}
            </p>
          ))}
          {Object.entries(selected.assets).map(([key, a]) => (
            <details key={key}>
              <summary>{key}</summary>
              <h3>Đang công khai</h3>
              <p className="pre-wrap">
                {selected.published?.assets[key]?.text || "Chưa có"}
              </p>
              <h3>Đề xuất</h3>
              <p className="pre-wrap">{a.text || "Chưa có"}</p>
              <p>Audio: {a.audioName || a.audioUrl || "Không có"}</p>
            </details>
          ))}
          <Link to={`/locations/${selected.id}/edit`}>
            Xem đầy đủ bản biên tập
          </Link>
          <Field id="review-comment" label="Nhận xét (bắt buộc khi từ chối)">
            <textarea
              id="review-comment"
              name="reviewComment"
              autoComplete="off"
              maxLength={4000}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          </Field>
          {selected.status === "PENDING_REVIEW" && (
            <div>
              <Button busy={busy} onClick={() => void decide("approve")}>
                Chấp thuận và tạo gói
              </Button>
              <Button busy={busy} onClick={() => void decide("reject")}>
                Từ chối
              </Button>
            </div>
          )}
        </section>
      )}
      <section className="card">
        <h2>Tác vụ tạo gói</h2>
        {data?.jobs.length === 0 && <p>Chưa có tác vụ.</p>}
        {data?.jobs.map((j) => (
          <div key={j.id}>
            <p>
              {new Intl.DateTimeFormat("vi-VN", {
                dateStyle: "short",
                timeStyle: "short",
              }).format(new Date(j.createdAt))}{" "}
              · {j.status}
            </p>
            {j.errorLog && <p>{j.errorLog}</p>}
            {j.status === "FAILED" && (
              <Button busy={busy} onClick={() => void retry(j.id)}>
                Thử tạo lại gói
              </Button>
            )}
          </div>
        ))}
      </section>
      <Button onClick={() => setParams({})}>Bỏ chọn</Button>
    </>
  );
}
