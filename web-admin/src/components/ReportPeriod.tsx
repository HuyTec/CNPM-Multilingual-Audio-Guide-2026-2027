import { useRef, useState, type FormEvent } from "react";
import { Button } from "./ui/Button";
import { Field } from "./ui/Field";
import { reportPeriodError } from "../lib/format";

export function ReportPeriod({
  from, to, loading, exporting, canExport, onApply, onExport,
}: {
  from: string;
  to: string;
  loading: boolean;
  exporting: boolean;
  canExport: boolean;
  onApply: (from: string, to: string) => void;
  onExport: () => void;
}) {
  const [start, setStart] = useState(from);
  const [end, setEnd] = useState(to);
  const [error, setError] = useState(() => reportPeriodError(from, to));
  const endInput = useRef<HTMLInputElement>(null);
  const changed = start !== from || end !== to;

  function apply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const issue = reportPeriodError(start, end);
    setError(issue);
    if (issue) endInput.current?.focus();
    else onApply(start, end);
  }

  return (
    <section className="card report-period" aria-labelledby="report-period-title">
      <div>
        <h2 id="report-period-title">Kỳ báo cáo</h2>
        <p className="muted">Mặc định 30 ngày · giờ Việt Nam · tối đa 366 ngày</p>
      </div>
      <form onSubmit={apply}>
        <div className="report-controls">
          <Field id="live-from" label="Từ ngày">
            <input id="live-from" name="from" type="date" required autoComplete="off"
              value={start} onChange={(event) => { setStart(event.target.value); setError(""); }} />
          </Field>
          <Field id="live-to" label="Đến ngày" error={error}>
            <input ref={endInput} id="live-to" name="to" type="date" required autoComplete="off"
              aria-invalid={!!error} aria-describedby={error ? "live-to-error" : undefined}
              value={end} onChange={(event) => { setEnd(event.target.value); setError(""); }} />
          </Field>
          <div className="report-actions">
            <Button type="submit" secondary busy={loading} disabled={exporting}>
              {loading ? "Đang tải…" : changed ? "Áp dụng kỳ" : "Làm mới số liệu"}
            </Button>
            <Button busy={exporting} disabled={!canExport || changed} onClick={onExport}>
              {exporting ? "Đang xuất…" : "Xuất báo cáo .xlsx"}
            </Button>
          </div>
        </div>
      </form>
    </section>
  );
}
