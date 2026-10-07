import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Download, Headphones, MapPin, Languages, WifiOff } from "lucide-react";
import {
  advanceFeedback,
  feedbackCategories,
  getFeedback,
  playbackEvents,
} from "../mocks/data";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { StatusBadge } from "../components/ui/StatusBadge";
import { statusLabels } from "../lib/status";
import { DataTable } from "../components/ui/DataTable";
import { EmptyState } from "../components/ui/EmptyState";
import { ConfirmDialog } from "../components/ui/ConfirmDialog";
import { LoadingState, ErrorState, Pagination } from "../components/ui/States";
import { date, dateTime, daysAgo, number, percent, today } from "../lib/format";
import { pageNumber, useDemoLoad } from "../lib/demo";
import { exportReport } from "../lib/export";

export default function AnalyticsFeedback() {
  const [params, setParams] = useSearchParams();
  const from = params.get("from") || daysAgo(29);
  const to = params.get("to") || today();
  const scenario = params.get("scenario") || "normal";
  const category = params.get("category") || "";
  const status = params.get("status") || "";
  const [feedback, setFeedback] = useState(getFeedback);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState("");
  const [announcement, setAnnouncement] = useState("");
  const [updating, setUpdating] = useState(false);
  const validDates =
    /^\d{4}-\d{2}-\d{2}$/.test(from) &&
    /^\d{4}-\d{2}-\d{2}$/.test(to) &&
    Number.isFinite(Date.parse(from)) &&
    Number.isFinite(Date.parse(to)) &&
    from <= to;
  const load = useDemoLoad(`${from}:${to}:${scenario}`, scenario);
  useEffect(() => {
    if (!params.has("from") || !params.has("to")) {
      const next = new URLSearchParams(params);
      next.set("from", from);
      next.set("to", to);
      setParams(next, { replace: true });
    }
  }, [params, setParams, from, to]);
  function query(name: string, value: string) {
    const next = new URLSearchParams(params);
    if (value) next.set(name, value);
    else next.delete(name);
    if (name !== "page" && name !== "feedback") next.delete("page");
    setParams(next);
  }
  const periodFeedback =
    scenario === "empty" || !validDates
      ? []
      : feedback
          .filter((x) => x.created >= from && x.created <= to)
          .map((x) =>
            scenario === "long" ? { ...x, content: x.content.repeat(20) } : x,
          );
  const rows = periodFeedback.filter(
    (x) =>
      (!category || category === x.category) &&
      (!status || status === x.status),
  );
  const page = pageNumber(params.get("page"), rows.length);
  const detail = periodFeedback.find((x) => x.id === params.get("feedback"));
  const events =
    scenario === "empty" || !validDates
      ? []
      : playbackEvents().filter((x) => x.date >= from && x.date <= to);
  const total = events.reduce((sum, x) => sum + x.count, 0);
  const offline = events
    .filter((x) => x.offline)
    .reduce((sum, x) => sum + x.count, 0);
  const aggregate = (field: "location" | "language") =>
    Object.entries(
      events.reduce<Record<string, number>>(
        (acc, x) => ({ ...acc, [x[field]]: (acc[x[field]] || 0) + x.count }),
        {},
      ),
    ).sort((a, b) => b[1] - a[1]);
  const locations = aggregate("location");
  const langs = aggregate("language");
  async function download(retry = false) {
    setExporting(true);
    setExportError("");
    await new Promise((resolve) => setTimeout(resolve, 300));
    try {
      if (scenario === "export-error" && !retry) throw new Error("demo");
      await exportReport(
        from,
        to,
        [
          ["Tổng lượt phát", total],
          ["Offline", offline],
          ["Online", total - offline],
          ["Tỷ lệ ngoại tuyến", percent(total ? offline / total : 0)],
          ["Tỷ lệ trực tuyến", percent(total ? (total - offline) / total : 0)],
          ...locations.map(([k, v]): [string, number] => [`Địa điểm: ${k}`, v]),
          ...langs.map(([k, v]): [string, number] => [`Ngôn ngữ: ${k}`, v]),
        ],
        periodFeedback,
      );
      setAnnouncement("Đã xuất báo cáo XLSX cho kỳ đang chọn.");
    } catch {
      setExportError(
        "Không thể xuất báo cáo. Bộ lọc và dữ liệu vẫn được giữ nguyên; hãy thử lại.",
      );
    } finally {
      setExporting(false);
    }
  }
  async function update() {
    if (!detail) return;
    setUpdating(true);
    await new Promise((resolve) => setTimeout(resolve, 250));
    try {
      const next = advanceFeedback(detail.id);
      setFeedback(next);
      setAnnouncement(
        `Phản hồi ${detail.id}: ${statusLabels[next.find((x) => x.id === detail.id)!.status]}. Đã lưu thời gian cập nhật.`,
      );
    } catch {
      setAnnouncement(
        "Không thể lưu trạng thái. Hãy thử lại; dữ liệu được giữ nguyên.",
      );
    }
    setUpdating(false);
  }
  function distribution(title: string, data: [string, number][]) {
    return (
      <section className="card">
        <div className="section-heading">
          <h2>{title}</h2>
        </div>
        {!data.length ? (
          <EmptyState />
        ) : (
          <>
            <div className="bar-list" aria-hidden="true">
              {data.map(([label, value]) => (
                <div key={label}>
                  <div className="bar-label">
                    <span>{label}</span>
                    <strong>{number(value)}</strong>
                  </div>
                  <div className="bar-track">
                    <div
                      style={{
                        width: `${(value / Math.max(...data.map((x) => x[1]))) * 100}%`,
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
            <details>
              <summary>
                Xem bảng số liệu {title.toLocaleLowerCase("vi")}
              </summary>
              <DataTable
                caption={title}
                headers={["Nhãn", "Lượt phát", "Tỷ lệ"]}
              >
                {data.map(([label, value]) => (
                  <tr key={label}>
                    <th scope="row">{label}</th>
                    <td>{number(value)}</td>
                    <td>{percent(total ? value / total : 0)}</td>
                  </tr>
                ))}
              </DataTable>
            </details>
          </>
        )}
      </section>
    );
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">HIỆU QUẢ & TRẢI NGHIỆM</p>
          <h1>Thống kê & phản hồi</h1>
          <p className="muted">
            Theo dõi lượt nghe và cải thiện trải nghiệm du khách.
          </p>
        </div>
        <Button
          busy={exporting}
          disabled={!validDates || load !== "ready"}
          onClick={() => void download()}
        >
          <Download size={17} aria-hidden="true" />
          {exporting ? "Đang xuất…" : "Xuất báo cáo .xlsx"}
        </Button>
      </div>
      <section className="card period">
        <div>
          <h2>Kỳ báo cáo</h2>
          <p className="muted">Mặc định 30 ngày gần nhất · giờ Việt Nam</p>
        </div>
        <div className="filters">
          <Field
            id="from"
            label="Từ ngày"
            error={
              !validDates
                ? "Chọn khoảng ngày hợp lệ, ngày bắt đầu không sau ngày kết thúc."
                : undefined
            }
          >
            <input
              id="from"
              name="from"
              type="date"
              autoComplete="off"
              value={from}
              aria-invalid={!validDates}
              aria-describedby="from-error"
              onChange={(e) => query("from", e.target.value)}
            />
          </Field>
          <Field id="to" label="Đến ngày">
            <input
              id="to"
              name="to"
              type="date"
              autoComplete="off"
              value={to}
              onChange={(e) => query("to", e.target.value)}
            />
          </Field>
        </div>
      </section>
      <p className="sr-only" role="status" aria-live="polite">
        {announcement}
      </p>
      {exportError && (
        <div className="notice error" role="status">
          <p>{exportError}</p>
          <Button
            secondary
            busy={exporting}
            onClick={() => void download(true)}
          >
            Thử lại xuất báo cáo
          </Button>
        </div>
      )}
      {load === "loading" ? (
        <LoadingState />
      ) : load === "error" ? (
        <ErrorState retry={() => query("scenario", "normal")} />
      ) : (
        <>
          {!total ? (
            <section className="card">
              <EmptyState title="Không có lượt phát trong kỳ này">
                <p>Hãy chọn khoảng ngày khác để xem thống kê.</p>
              </EmptyState>
            </section>
          ) : (
            <>
              <div className="summary-grid">
                {[
                  {
                    label: "Tổng lượt phát",
                    value: number(total),
                    hint: "Trong kỳ báo cáo",
                    icon: Headphones,
                  },
                  {
                    label: "Địa điểm nổi bật",
                    value: locations[0]?.[0] || "—",
                    hint: `${number(locations[0]?.[1] || 0)} lượt phát`,
                    icon: MapPin,
                  },
                  {
                    label: "Ngôn ngữ được sử dụng",
                    value: number(langs.length),
                    hint: langs.map((x) => x[0]).join(" · "),
                    icon: Languages,
                  },
                  {
                    label: "Tỷ lệ ngoại tuyến",
                    value: percent(offline / total),
                    hint: `Trực tuyến ${percent(1 - offline / total)}`,
                    icon: WifiOff,
                  },
                ].map((x) => (
                  <div className="card metric" key={x.label}>
                    <div className="metric-label">
                      <span>{x.label}</span>
                      <x.icon size={18} aria-hidden="true" />
                    </div>
                    <strong className="metric-value">{x.value}</strong>
                    <small>{x.hint}</small>
                  </div>
                ))}
              </div>
              <div className="analytics-grid">
                {distribution("Địa điểm nổi bật", locations)}
                {distribution("Phân bố ngôn ngữ", langs)}
                {distribution("Ngoại tuyến / trực tuyến", [
                  ["Ngoại tuyến", offline],
                  ["Trực tuyến", total - offline],
                ])}
              </div>
            </>
          )}
          <section className="card">
            <div className="section-heading">
              <div>
                <h2>Phản hồi từ du khách</h2>
                <p className="muted">Tiếp nhận → Đang xử lý → Đã giải quyết</p>
              </div>
              <span className="pill">{number(rows.length)} phản hồi</span>
            </div>
            <div className="filters">
              <Field id="feedback-category" label="Danh mục">
                <select
                  id="feedback-category"
                  name="category"
                  value={category}
                  onChange={(e) => query("category", e.target.value)}
                >
                  <option value="">Tất cả danh mục</option>
                  {feedbackCategories.map((x) => (
                    <option key={x}>{x}</option>
                  ))}
                </select>
              </Field>
              <Field id="feedback-status" label="Trạng thái">
                <select
                  id="feedback-status"
                  name="status"
                  value={status}
                  onChange={(e) => query("status", e.target.value)}
                >
                  <option value="">Tất cả trạng thái</option>
                  {["NEW", "IN_REVIEW", "RESOLVED"].map((x) => (
                    <option key={x} value={x}>
                      {statusLabels[x]}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
            {!rows.length ? (
              <EmptyState title="Không có phản hồi phù hợp">
                <p>Thay đổi kỳ báo cáo hoặc bỏ bộ lọc để xem dữ liệu.</p>
              </EmptyState>
            ) : (
              <>
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
                  {rows.slice((page - 1) * 10, page * 10).map((x) => (
                    <tr key={x.id}>
                      <td className="feedback-content">
                        <strong>{x.location}</strong>
                        <p className="line-clamp">{x.content}</p>
                      </td>
                      <td>{x.category}</td>
                      <td>
                        <span
                          className="rating"
                          aria-label={`${number(x.rating)} trên 5 sao`}
                        >
                          {number(x.rating)} / 5{" "}
                          <span aria-hidden="true">★</span>
                        </span>
                      </td>
                      <td>
                        <StatusBadge status={x.status} />
                      </td>
                      <td>{date(x.created)}</td>
                      <td>
                        <Button
                          secondary
                          onClick={() => query("feedback", x.id)}
                        >
                          Xem chi tiết
                        </Button>
                      </td>
                    </tr>
                  ))}
                </DataTable>
                <Pagination
                  page={page}
                  total={rows.length}
                  onChange={(n) => query("page", String(n))}
                />
              </>
            )}
          </section>
        </>
      )}
      <ConfirmDialog
        open={!!detail}
        title="Chi tiết phản hồi"
        onCancel={() => query("feedback", "")}
      >
        {detail && (
          <>
            <div className="detail-meta">
              <StatusBadge status={detail.status} />
              <span>
                {detail.category} · {number(detail.rating)} / 5 sao
              </span>
            </div>
            <h3>{detail.location}</h3>
            <p className="pre-wrap">{detail.content}</p>
            <p className="muted">Ngày gửi: {date(detail.created)}</p>
            <p className="muted">
              Cập nhật:{" "}
              {detail.updated ? dateTime(detail.updated) : "Chưa cập nhật"}
            </p>
            {detail.status !== "RESOLVED" ? (
              <Button busy={updating} onClick={() => void update()}>
                {updating
                  ? "Đang cập nhật…"
                  : detail.status === "NEW"
                    ? "Chuyển sang Đang xử lý"
                    : "Đánh dấu Đã giải quyết"}
              </Button>
            ) : (
              <p>Phản hồi đã hoàn tất. MVP không mở lại phản hồi.</p>
            )}
          </>
        )}
      </ConfirmDialog>
    </>
  );
}
