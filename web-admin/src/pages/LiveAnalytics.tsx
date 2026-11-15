import { AdminAccess } from "../components/AdminAccess";
import { useAdminSession } from "../lib/admin-content";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { ReportPeriod } from "../components/ReportPeriod";
import { DataTable } from "../components/ui/DataTable";
import { EmptyState } from "../components/ui/EmptyState";
import { LoadingState } from "../components/ui/States";
import { useLiveAnalytics, type CountItem } from "../lib/live-analytics";
import { daysAgo, today, number, percent, dateTime, reportPeriodError } from "../lib/format";
import { exportReport } from "../lib/export";
import { feedbackForExport, feedbackQuery } from "../lib/feedback";
import { LiveFeedback } from "../components/LiveFeedback";
import { VisitorStats } from "../components/VisitorStats";

export default function LiveAnalytics() {
  const [params, setParams] = useSearchParams();
  const from = params.get("from") || daysAgo(29);
  const to = params.get("to") || today();
  const { authorization, setAuthorization } = useAdminSession();
  const [revision, setRevision] = useState(0);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState("");
  const invalidPeriod = reportPeriodError(from, to);
  const result = useLiveAnalytics(authorization, from, to, revision, !invalidPeriod);
  const data = result.data;
  useEffect(() => {
    if (!params.has("from") || !params.has("to")) {
      const next = new URLSearchParams(params);
      next.set("from", from);
      next.set("to", to);
      setParams(next, { replace: true });
    }
  }, [params, setParams, from, to]);
  function period(start: string, end: string) {
    if (start === from && end === to) {
      setRevision((x) => x + 1);
      return;
    }
    const next = new URLSearchParams(params);
    next.set("from", start);
    next.set("to", end);
    next.delete("page");
    next.delete("feedback");
    setExportError("");
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
        "Web du khách · lượt bắt đầu phát và phản hồi theo bộ lọc đang chọn",
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
          <h1>Thống kê và phản hồi</h1>
          <p className="muted">
            Theo dõi hoạt động trên Web du khách và xử lý phản hồi theo kỳ báo cáo.
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
          <ReportPeriod key={`${from}:${to}`} from={from} to={to}
            loading={!invalidPeriod && !data && !result.error}
            exporting={exporting} canExport={!!data} onApply={period}
            onExport={() => void download()} />
          <VisitorStats authorization={authorization} />
          {invalidPeriod ? null : result.error ? (
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
              <p className="muted report-updated">
                Cập nhật {dateTime(data.generatedAt)} · Tính theo lượt bắt đầu phát
              </p>
              <div className="summary-grid">
                <Metric
                  label="Tổng lượt phát"
                  value={number(data.totalPlaybacks)}
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
                      Hãy đổi kỳ báo cáo hoặc kiểm tra lượt phát trên Web du khách.
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
          {!invalidPeriod && <LiveFeedback from={from} to={to} />}
        </>
      )}
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
