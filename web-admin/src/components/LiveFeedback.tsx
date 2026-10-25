import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { adminRequest, useAdminSession } from "../lib/admin-content";
import {
  feedbackQuery,
  type FeedbackItem,
  type FeedbackPage,
} from "../lib/feedback";
import { date, dateTime, number } from "../lib/format";
import { statusLabels } from "../lib/status";
import { Button } from "./ui/Button";
import { Field } from "./ui/Field";
import { DataTable } from "./ui/DataTable";
import { EmptyState } from "./ui/EmptyState";
import { ErrorState, LoadingState, Pagination } from "./ui/States";
import { StatusBadge } from "./ui/StatusBadge";
import { ConfirmDialog } from "./ui/ConfirmDialog";

export function LiveFeedback({ from, to }: { from: string; to: string }) {
  const [params, setParams] = useSearchParams();
  const { authorization } = useAdminSession();
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState<{
    key: string;
    data?: FeedbackPage;
    error?: string;
  }>({ key: "" });
  const [detail, setDetail] = useState<{
    key: string;
    data?: FeedbackItem;
    error?: string;
  }>({ key: "" });
  const [busy, setBusy] = useState(false);
  const [announcement, setAnnouncement] = useState("");
  const query = feedbackQuery(params, from, to);
  query.set("page", params.get("page") || "1");
  query.set("size", "10");
  const search = query.toString();
  const key = `${authorization}:${search}:${revision}`;
  const id = params.get("feedback") || "";
  const detailKey = `${authorization}:${id}`;
  useEffect(() => {
    const controller = new AbortController();
    void adminRequest<FeedbackPage>(
      `/api/admin/feedback?${search}`,
      authorization,
      { signal: controller.signal },
    )
      .then((data) => {
        if (!controller.signal.aborted) setResult({ key, data });
      })
      .catch((error: Error) => {
        if (!controller.signal.aborted)
          setResult({ key, error: error.message });
      });
    return () => controller.abort();
  }, [authorization, search, key]);
  useEffect(() => {
    if (!id) return;
    const controller = new AbortController();
    void adminRequest<FeedbackItem>(
      `/api/admin/feedback/${encodeURIComponent(id)}`,
      authorization,
      { signal: controller.signal },
    )
      .then((data) => {
        if (!controller.signal.aborted) setDetail({ key: detailKey, data });
      })
      .catch((error: Error) => {
        if (!controller.signal.aborted)
          setDetail({ key: detailKey, error: error.message });
      });
    return () => controller.abort();
  }, [authorization, id, detailKey, revision]);
  function filter(name: string, value: string) {
    const next = new URLSearchParams(params);
    if (value) next.set(name, value);
    else next.delete(name);
    if (name !== "page" && name !== "feedback") next.delete("page");
    setParams(next);
  }
  const current = result.key === key ? result : undefined;
  const selected = detail.key === detailKey ? detail.data : undefined;
  async function advance() {
    if (!selected) return;
    setBusy(true);
    setAnnouncement("");
    try {
      const data = await adminRequest<FeedbackItem>(
        `/api/admin/feedback/${selected.id}/status`,
        authorization,
        {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            status: selected.status === "NEW" ? "IN_REVIEW" : "RESOLVED",
          }),
        },
      );
      setDetail({ key: detailKey, data });
      setAnnouncement(`Đã chuyển phản hồi sang ${statusLabels[data.status]}.`);
      setRevision((value) => value + 1);
    } catch (error) {
      setAnnouncement(
        error instanceof Error
          ? error.message
          : "Chưa cập nhật được. Hãy thử lại.",
      );
    } finally {
      setBusy(false);
    }
  }
  const categories = Array.from(
    new Set([
      ...(current?.data?.categories || [
        "Nội dung",
        "Audio",
        "Ứng dụng",
        "Đề xuất",
        "Khác",
      ]),
      ...(params.get("category") ? [params.get("category")!] : []),
    ]),
  );
  return (
    <section className="card live-panel">
      <div className="section-heading">
        <div>
          <h2>Phản hồi từ du khách</h2>
          <p className="muted">Mới → Đang xử lý → Đã giải quyết</p>
        </div>
      </div>
      <div className="filters">
        <Field id="feedback-category" label="Danh mục">
          <select
            id="feedback-category"
            name="category"
            value={params.get("category") || ""}
            onChange={(event) => filter("category", event.target.value)}
          >
            <option value="">Tất cả danh mục</option>
            {categories.map((category) => (
              <option key={category}>{category}</option>
            ))}
          </select>
        </Field>
        <Field id="feedback-status" label="Trạng thái">
          <select
            id="feedback-status"
            name="status"
            value={params.get("status") || ""}
            onChange={(event) => filter("status", event.target.value)}
          >
            <option value="">Tất cả trạng thái</option>
            {["NEW", "IN_REVIEW", "RESOLVED"].map((status) => (
              <option key={status} value={status}>
                {statusLabels[status]}
              </option>
            ))}
          </select>
        </Field>
      </div>
      <p role="status" aria-live="polite">
        {!id && announcement}
      </p>
      {current?.error ? (
        <ErrorState
          message={current.error}
          retry={() => setRevision((value) => value + 1)}
        />
      ) : !current?.data ? (
        <LoadingState />
      ) : !current.data.items.length ? (
        <EmptyState title="Không có phản hồi phù hợp">
          <p>Thay đổi kỳ báo cáo hoặc bỏ bộ lọc để xem dữ liệu.</p>
        </EmptyState>
      ) : (
        <DataTable
          caption="Phản hồi du khách"
          headers={[
            "Nội dung",
            "Danh mục",
            "Đánh giá",
            "Trạng thái",
            "Ngày gửi",
            "Chi tiết",
          ]}
        >
          {current.data.items.map((item) => (
            <tr key={item.id}>
              <td className="feedback-content min-w-0">
                <strong>{item.location}</strong>
                <p className="line-clamp">{item.content}</p>
              </td>
              <td>{item.category}</td>
              <td className="numeric">
                {item.rating == null
                  ? "Chưa đánh giá"
                  : `${number(item.rating)} / 5`}
              </td>
              <td>
                <StatusBadge status={item.status} />
              </td>
              <td>{date(item.created)}</td>
              <td>
                <Button secondary onClick={() => filter("feedback", item.id)}>
                  Xem chi tiết
                </Button>
              </td>
            </tr>
          ))}
        </DataTable>
      )}
      {current?.data && current.data.total > 0 && (
        <Pagination
          page={current.data.page}
          total={current.data.total}
          onChange={(page) => filter("page", String(page))}
        />
      )}
      <ConfirmDialog
        open={!!id}
        title="Chi tiết phản hồi"
        onCancel={() => {
          if (!busy) filter("feedback", "");
        }}
      >
        {id && (
          <>
            <p role="status" aria-live="polite">
              {announcement}
            </p>
            {detail.key === detailKey && detail.error ? (
              <ErrorState
                message={detail.error}
                retry={() => setRevision((value) => value + 1)}
              />
            ) : !selected ? (
              <LoadingState />
            ) : (
              <>
                <StatusBadge status={selected.status} />
                <h3>{selected.location}</h3>
                <p>
                  {selected.category} ·{" "}
                  {selected.rating == null
                    ? "Chưa đánh giá"
                    : `${number(selected.rating)} / 5 sao`}
                </p>
                <p className="pre-wrap">{selected.content}</p>
                <p>Ngày gửi: {date(selected.created)}</p>
                <p>
                  Cập nhật:{" "}
                  {selected.updated
                    ? dateTime(selected.updated)
                    : "Chưa cập nhật"}
                </p>
                {selected.status !== "RESOLVED" ? (
                  <Button busy={busy} onClick={() => void advance()}>
                    {busy
                      ? "Đang cập nhật…"
                      : selected.status === "NEW"
                        ? "Chuyển sang Đang xử lý"
                        : "Đánh dấu Đã giải quyết"}
                  </Button>
                ) : (
                  <p>Phản hồi đã hoàn tất, không mở lại.</p>
                )}
              </>
            )}
          </>
        )}
      </ConfirmDialog>
    </section>
  );
}
