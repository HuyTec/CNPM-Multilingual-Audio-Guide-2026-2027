import { Link } from "react-router-dom";
import { languages, type Location } from "../../mocks/data";
import { coordinate, dateTime } from "../../lib/format";
import { StatusBadge } from "../ui/StatusBadge";
import { LanguageCoverage } from "./LanguageCoverage";

export function LocationRow({
  location,
  scenario,
}: {
  location: Location;
  scenario: string;
}) {
  const name = location.name || "Địa điểm chưa đặt tên";
  const href = `/locations/${location.id}/edit${scenario === "long" ? "?scenario=long" : ""}`;
  return (
    <tr className="location-row">
      <td className="location-title-cell">
        <Link
          className="location-row-link"
          to={href}
          aria-label={`Mở địa điểm ${name}`}
        >
          {name}
        </Link>
      </td>
      <td>{location.category || "Chưa chọn"}</td>
      <td>
        <div className="language-coverage">
          {languages.map((language) => (
            <LanguageCoverage
              key={language.code}
              code={language.code}
              language={language.label}
              assets={location.assets}
            />
          ))}
        </div>
      </td>
      <td className="location-coordinates numeric">
        <span className="sr-only">Lat (vĩ độ): </span>
        {coordinate(location.latitude)}
        <span aria-hidden="true"> / </span>
        <span className="sr-only">; Lng (kinh độ): </span>
        {coordinate(location.longitude)}
      </td>
      <td>
        <StatusBadge status={location.status} />
      </td>
      <td>
        <span className={location.published ? "published" : "muted"}>
          {location.published ? "Đang hiển thị" : "Chưa xuất bản"}
        </span>
      </td>
      <td className="location-updated numeric">
        {location.updatedAt &&
        Number.isFinite(Date.parse(location.updatedAt)) ? (
          <time dateTime={location.updatedAt}>
            {dateTime(location.updatedAt)}
          </time>
        ) : (
          <span className="muted">Chưa ghi nhận</span>
        )}
      </td>
      <td>
        <Link
          className="location-edit-link"
          to={href}
          aria-label={`Chỉnh sửa địa điểm ${name}`}
        >
          Chỉnh sửa
        </Link>
      </td>
    </tr>
  );
}
