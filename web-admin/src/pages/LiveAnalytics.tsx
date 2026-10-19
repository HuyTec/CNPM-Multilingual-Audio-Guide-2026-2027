import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { DataTable } from "../components/ui/DataTable";
import { EmptyState } from "../components/ui/EmptyState";
import { LoadingState } from "../components/ui/States";
import {
  basicCredentials,
  useLiveAnalytics,
  type CountItem,
} from "../lib/live-analytics";
import { daysAgo, today, number, percent, dateTime } from "../lib/format";
import { exportReport } from "../lib/export";

export default function LiveAnalytics() {
  const [params, setParams] = useSearchParams();
  const from = params.get("from") || daysAgo(29);
  const to = params.get("to") || today();
  const [authorization, setAuthorization] = useState("");
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
    setParams(next);
  }
  async function download() {
    if (!data) return;
    setExporting(true);
    setExportError("");
    try {
      await exportReport(
        data.from,
        data.to,
        [
          ["Tổng lượt phát", data.totalPlaybacks],
          ["Online", data.onlinePlaybacks],
          ["Offline", data.offlinePlaybacks],
          ...data.topLocations.map((x): [string, number] => [
            `Địa điểm: ${x.label}`,
            x.count,
          ]),
          ...data.languages.map((x): [string, number] => [
            `Ngôn ngữ: ${x.label}`,
            x.count,
          ]),
        ],
        [],
        "PostgreSQL — lượt bắt đầu phát từ Android; chưa tích hợp phản hồi",
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
          <p className="eyebrow">DỮ LIỆU TỪ ANDROID</p>
          <h1>Thống kê lượt nghe</h1>
          <p className="muted">
            Số liệu PostgreSQL thật · cập nhật mỗi 5 giây khi trang đang mở.
          </p>
        </div>
        {authorization && (
          <Button secondary onClick={() => setAuthorization("")}>
            Đăng xuất thống kê
          </Button>
        )}
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
                {exporting ? "Đang xuất…" : "Xuất lượt nghe .xlsx"}
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
                      Mở tab Nghe demo trên Android, chọn địa điểm rồi nhấn Phát
                      thuyết minh. Không có lượt nghe được tạo sẵn.
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
        </>
      )}
      <section className="card live-panel">
        <h2>Phạm vi bản demo</h2>
        <p>
          Luồng lượt nghe đã nối database. Quản lý địa điểm và phản hồi vẫn là
          giao diện mẫu, chưa đồng bộ với database này.
        </p>
        <Link to="/analytics?source=mock">
          Mở giao diện thống kê và phản hồi giả lập
        </Link>
      </section>
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

function AdminAccess({
  error,
  onAuthenticate,
}: {
  error?: string;
  onAuthenticate: (value: string) => void;
}) {
  const passwordInput = useRef<HTMLInputElement>(null);
  useEffect(() => {
    if (error) passwordInput.current?.focus();
  }, [error]);
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    onAuthenticate(
      basicCredentials(
        String(form.get("username") || ""),
        String(form.get("password") || ""),
      ),
    );
  }
  return (
    <section className="card live-panel">
      <h2>Đăng nhập thống kê demo</h2>
      <p>
        Dùng tài khoản quản trị được cấu hình trên server. Phiên chỉ giữ trong
        bộ nhớ của tab trình duyệt.
      </p>
      <form onSubmit={submit}>
        <Field id="admin-user" label="Tên đăng nhập">
          <input
            id="admin-user"
            name="username"
            autoComplete="username"
            defaultValue="admin"
            required
            spellCheck={false}
          />
        </Field>
        <Field id="admin-password" label="Mật khẩu" error={error}>
          <input
            ref={passwordInput}
            id="admin-password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            aria-invalid={!!error}
            aria-describedby={error ? "admin-password-error" : undefined}
          />
        </Field>
        <Button type="submit">Xem số liệu database</Button>
      </form>
    </section>
  );
}
