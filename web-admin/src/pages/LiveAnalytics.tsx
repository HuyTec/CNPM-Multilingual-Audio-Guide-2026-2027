import { AdminAccess } from "../components/AdminAccess";
import { useAdminSession } from "../lib/admin-content";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { DataTable } from "../components/ui/DataTable";
import { EmptyState } from "../components/ui/EmptyState";
import { LoadingState } from "../components/ui/States";
import { useLiveAnalytics, type CountItem } from "../lib/live-analytics";
import { daysAgo, today, number, percent, dateTime } from "../lib/format";
import { exportReport } from "../lib/export";
import { feedbackForExport, feedbackQuery } from "../lib/feedback";
import { LiveFeedback } from "../components/LiveFeedback";

export default function LiveAnalytics() {
  const [params, setParams] = useSearchParams();
  const from = params.get("from") || daysAgo(29);
  const to = params.get("to") || today();
  const { authorization, setAuthorization } = useAdminSession();
  const [revision, setRevision] = useState(0);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState("");
  const result = useLiveAnalytics(authorization, from, to, revision);
  const data = result.data;
  useEffect(() => {
    if (!params.has("from") || !params.has("to")) {
      const next = new URLSearchParams(params);
      next.set("from", from);
      next.set("to", to);
      setParams(next, { replace: true });
    }
  }, [params, setParams, from, to]);
  function period(name: string, value: string) {
    const next = new URLSearchParams(params);
    next.set("from", from);
    next.set("to", to);
    next.set(name, value);
    next.delete("page");
    setParams(next);
  }
  async function download() {
    if (!data) return;
    setExporting(true);
    setExportError("");
    try {
      const feedback = await feedbackForExport(
        authorization,
        feedbackQuery(params, data.from, data.to),
      );
      await exportReport(
        data.from,
        data.to,
        [
          ["Tổng lượt phát", data.totalPlaybacks],
          ["Online", data.onlinePlaybacks],
          ["Offline", data.offlinePlaybacks],
          [
            "Tỷ lệ ngoại tuyến",
            percent(
              data.totalPlaybacks
                ? data.offlinePlaybacks / data.totalPlaybacks
                : 0,
            ),
          ],
          [
            "Tỷ lệ trực tuyến",
            percent(
              data.totalPlaybacks
                ? data.onlinePlaybacks / data.totalPlaybacks
                : 0,
            ),
          ],
          ...data.topLocations.map((x): [string, number] => [
            `Địa điểm: ${x.label}`,
            x.count,
          ]),
          ...data.languages.map((x): [string, number] => [
            `Ngôn ngữ: ${x.label}`,
            x.count,
          ]),
        ],
        feedback,
        "PostgreSQL — lượt bắt đầu phát và phản hồi theo bộ lọc đang chọn",
      );
    } catch {
      setExportError(
        "Xuất báo cáo thất bại. Dữ liệu vẫn được giữ; hãy thử lại.",
      );
    } finally {
      setExporting(false);
    }
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">DỮ LIỆU CHUNG WEB VÀ ANDROID</p>
          <h1>Thống kê lượt nghe</h1>
          <p className="muted">
            Theo dõi lượt phát, ngôn ngữ và phản hồi trong kỳ báo cáo.
          </p>
        </div>
      </div>
      {!authorization || result.unauthorized ? (
        <AdminAccess
          error={result.error}
          onAuthenticate={(value) => {
            setAuthorization(value);
            setRevision((x) => x + 1);
          }}
        />
      ) : (
        <>
          <section className="card period">
            <div>
              <h2>Kỳ báo cáo</h2>
              <p className="muted">
                Mặc định 30 ngày · giờ Việt Nam · tối đa 366 ngày.
              </p>
            </div>
            <div className="filters">
              <Field id="live-from" label="Từ ngày">
                <input
                  id="live-from"
                  name="from"
                  type="date"
                  autoComplete="off"
                  value={from}
                  onChange={(e) => period("from", e.target.value)}
                />
              </Field>
              <Field id="live-to" label="Đến ngày">
                <input
                  id="live-to"
                  name="to"
                  type="date"
                  autoComplete="off"
                  value={to}
                  onChange={(e) => period("to", e.target.value)}
                />
              </Field>
              <Button secondary onClick={() => setRevision((x) => x + 1)}>
                Làm mới số liệu
              </Button>
              <Button
                disabled={!data}
                busy={exporting}
                onClick={() => void download()}
              >
                {exporting ? "Đang xuất…" : "Xuất báo cáo .xlsx"}
              </Button>
            </div>
          </section>
          {result.error ? (
            <div className="notice error" role="status" aria-live="polite">
              <p>{result.error}</p>
              <Button secondary onClick={() => setRevision((x) => x + 1)}>
                Thử lại tải số liệu
              </Button>
            </div>
          ) : !data ? (
            <LoadingState />
          ) : (
            <>
              <p className="muted" role="status" aria-live="polite">
                Cập nhật: {dateTime(data.generatedAt)} · Một lượt = một phiên
                bắt đầu phát, không phải lượt nghe hết.
              </p>
              <div className="summary-grid">
                <Metric
                  label="Tổng lượt phát"
                  value={number(data.totalPlaybacks)}
                />
                <Metric
                  label="Địa điểm nổi bật"
                  value={data.topLocations[0]?.label || "Chưa có"}
                />
                <Metric
                  label="Ngôn ngữ được sử dụng"
                  value={number(data.languages.length)}
                />
                <Metric
                  label="Tỷ lệ ngoại tuyến"
                  value={percent(
                    data.totalPlaybacks
                      ? data.offlinePlaybacks / data.totalPlaybacks
                      : 0,
                  )}
                />
              </div>
              {!data.totalPlaybacks ? (
                <section className="card">
                  <EmptyState title="Chưa có lượt nghe trong kỳ">
                    <p>
                      Chọn địa điểm trên Web du khách hoặc Android rồi nhấn Nghe
                      thuyết minh. Trang chỉ hiển thị lượt nghe đã ghi vào
                      database.
                    </p>
                  </EmptyState>
                </section>
              ) : (
                <div className="analytics-grid">
                  <Distribution
                    title="Địa điểm nổi bật"
                    rows={data.topLocations}
                    total={data.totalPlaybacks}
                  />
                  <Distribution
                    title="Phân bố ngôn ngữ"
                    rows={data.languages}
                    total={data.totalPlaybacks}
                  />
                  <Distribution
                    title="Trực tuyến / ngoại tuyến"
                    rows={[
                      { label: "Trực tuyến", count: data.onlinePlaybacks },
                      { label: "Ngoại tuyến", count: data.offlinePlaybacks },
                    ]}
                    total={data.totalPlaybacks}
                  />
                </div>
              )}
            </>
          )}
          {exportError && (
            <div className="notice error" role="status" aria-live="polite">
              <p>{exportError}</p>
              <Button busy={exporting} onClick={() => void download()}>
                Thử lại xuất báo cáo
              </Button>
            </div>
          )}
          <LiveFeedback from={from} to={to} />
        </>
      )}
      <details className="card admin-demo-details">
        <summary>Thông tin bản demo</summary>
        <p>
          Địa điểm, bản nháp, gửi duyệt, lượt nghe và phản hồi dùng database
          chung. Phản hồi được tiếp nhận qua hệ thống bên ngoài; trang này quản
          lý trạng thái.
        </p>
        <Link to="/analytics?source=mock">
          Mở giao diện thống kê và phản hồi giả lập
        </Link>
      </details>
    </>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="card metric">
      <span className="metric-label">{label}</span>
      <strong
        className="metric-value"
        style={{ fontVariantNumeric: "tabular-nums", overflowWrap: "anywhere" }}
      >
        {value}
      </strong>
    </div>
  );
}

function Distribution({
  title,
  rows,
  total,
}: {
  title: string;
  rows: CountItem[];
  total: number;
}) {
  return (
    <section className="card">
      <div className="section-heading">
        <h2>{title}</h2>
      </div>
      <DataTable caption={title} headers={["Nhãn", "Lượt phát", "Tỷ lệ"]}>
        {rows.map((row) => (
          <tr key={row.label}>
            <th scope="row">{row.label}</th>
            <td>{number(row.count)}</td>
            <td>{percent(total ? row.count / total : 0)}</td>
          </tr>
        ))}
      </DataTable>
    </section>
  );
}
