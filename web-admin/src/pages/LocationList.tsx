import { Link, useSearchParams } from "react-router-dom";
import { Plus, Check, VolumeX, FileText, Minus, TriangleAlert } from "lucide-react";
import { getLocations, longLocations } from "../mocks/data";
import { DataTable } from "../components/ui/DataTable";
import { EmptyState } from "../components/ui/EmptyState";
import { ErrorState, LoadingState, Pagination } from "../components/ui/States";
import { LocationRow } from "../components/locations/LocationRow";
import { statusLabels } from "../lib/status";
import { pageNumber, useDemoLoad } from "../lib/demo";
import { number } from "../lib/format";

export default function LocationList() {
  const [params, setParams] = useSearchParams();
  const scenario = params.get("scenario") || "normal";
  const load = useDemoLoad(scenario, scenario);
  const source =
    scenario === "empty"
      ? []
      : scenario === "long"
        ? longLocations()
        : getLocations();
  const q = params.get("q") || "";
  const status = params.get("status") || "";
  const rows = source.filter(
    (x) =>
      x.name.toLocaleLowerCase("vi").includes(q.toLocaleLowerCase("vi")) &&
      (!status || x.status === status),
  );
  const page = pageNumber(params.get("page"), rows.length);
  function filter(key: string, value: string) {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    next.delete("page");
    setParams(next, { replace: key === "q" });
  }
  const stats = [
    { label: "Tất cả địa điểm", status: "", count: source.length },
    {
      label: "Đã duyệt",
      status: "APPROVED",
      count: source.filter((x) => x.status === "APPROVED").length,
    },
    {
      label: "Chờ duyệt",
      status: "PENDING_REVIEW",
      count: source.filter((x) => x.status === "PENDING_REVIEW").length,
    },
    {
      label: "Bản nháp",
      status: "DRAFT",
      count: source.filter((x) => x.status === "DRAFT").length,
    },
  ];
  return (
    <div className="location-list">
      <div className="page-heading">
        <div>
          <h1>Địa điểm</h1>
          <p className="muted">
            Biên tập kịch bản, kiểm tra audio và chuẩn bị nội dung gửi duyệt.
          </p>
        </div>
        <Link to="/locations/new" className="button">
          <Plus size={18} aria-hidden="true" />
          Thêm địa điểm
        </Link>
      </div>
      <div
        className="location-stats"
        role="group"
        aria-label="Lọc theo trạng thái nội dung"
      >
        {stats.map((stat) => (
          <button
            type="button"
            className="location-stat"
            key={stat.label}
            aria-pressed={status === stat.status}
            onClick={() => filter("status", stat.status)}
          >
            <span>{stat.label}</span>
            <strong>{number(stat.count)}</strong>
          </button>
        ))}
      </div>
      <section className="card location-content">
        <div className="section-heading">
          <h2>Danh sách địa điểm</h2>
          <span className="location-count" role="status" aria-live="polite">
            {number(rows.length)} địa điểm
          </span>
        </div>
        <div className="filters location-filters">
          <div>
            <label htmlFor="search">Tìm địa điểm</label>
            <input
              id="search"
              name="search"
              autoComplete="off"
              className="location-filter-control"
              type="search"
              placeholder="Bưu điện Trung tâm…"
              value={q}
              onChange={(e) => filter("q", e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="location-status">Trạng thái nội dung</label>
            <select
              id="location-status"
              name="status"
              className="location-filter-control"
              value={status}
              onChange={(e) => filter("status", e.target.value)}
            >
              <option value="">Tất cả trạng thái</option>
              {["DRAFT", "PENDING_REVIEW", "APPROVED"].map((value) => (
                <option key={value} value={value}>
                  {statusLabels[value]}
                </option>
              ))}
            </select>
          </div>
        </div>
        {load === "loading" ? (
          <LoadingState />
        ) : load === "error" ? (
          <ErrorState retry={() => filter("scenario", "normal")} />
        ) : !rows.length ? (
          <EmptyState title="Không tìm thấy địa điểm">
            <p>Thử bỏ bộ lọc hoặc thêm địa điểm mới.</p>
          </EmptyState>
        ) : (
          <>
            <DataTable
              caption="Danh sách địa điểm"
              headers={[
                "Địa điểm",
                "Danh mục",
                "Ngôn ngữ",
                "Tọa độ GPS",
                "Trạng thái",
                "Trên ứng dụng",
                "Cập nhật",
                "Thao tác",
              ]}
            >
              {rows.slice((page - 1) * 10, page * 10).map((location) => (
                <LocationRow
                  key={location.id}
                  location={location}
                  scenario={scenario}
                />
              ))}
            </DataTable>
            <Pagination
              page={page}
              total={rows.length}
              onChange={(n) => {
                const next = new URLSearchParams(params);
                next.set("page", String(n));
                setParams(next);
              }}
            />
          </>
        )}
      </section>
      <div className="location-legend" aria-label="Ý nghĩa trạng thái ngôn ngữ">
        <span>
          <Check size={14} aria-hidden="true" /> Đủ kịch bản & audio
        </span>
        <span>
          <VolumeX size={14} aria-hidden="true" /> Thiếu audio
        </span>
        <span>
          <FileText size={14} aria-hidden="true" /> Thiếu kịch bản
        </span>
        <span>
          <Minus size={14} aria-hidden="true" /> Chưa có
        </span>
        <span>
          <TriangleAlert size={14} aria-hidden="true" /> Audio lỗi
        </span>
      </div>
      <p className="footnote">
        Audio là tùy chọn. Bản đã xuất bản vẫn hiển thị trên ứng dụng trong thời
        gian chỉnh sửa và chờ duyệt.
      </p>
    </div>
  );
}
