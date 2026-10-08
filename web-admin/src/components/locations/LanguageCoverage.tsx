import { useId, useState } from "react";
import { Check, Minus, VolumeX, TriangleAlert, FileText } from "lucide-react";
import type { Asset } from "../../mocks/data";

/** Summarize FULL/SHORT assets for display only; audio availability never gates submission. */
function coverage(assets: Record<string, Asset>, code: string) {
  const variants = ["FULL", "SHORT"].map((type) => assets[`${code}:${type}`]);
  const texts = variants.filter((asset) => asset?.text.trim()).length;
  if (!texts)
    return {
      state: "absent",
      label: "Chưa có",
      detail: "Chưa có kịch bản FULL hoặc SHORT.",
      Icon: Minus,
    };
  if (variants.some((asset) => asset?.audioStatus === "error"))
    return {
      state: "error",
      label: "Audio lỗi",
      detail:
        "Audio không thể giải mã. Mở chỉnh sửa để thay tệp; kịch bản được giữ nguyên.",
      Icon: TriangleAlert,
    };
  if (texts < 2)
    return {
      state: "partial",
      label: "Thiếu kịch bản",
      detail: "Chưa đủ hai kịch bản FULL và SHORT.",
      Icon: FileText,
    };
  const audio = variants.filter(
    (asset) => asset?.audioName && asset.audioStatus !== "error",
  ).length;
  if (audio < 2)
    return {
      state: "missing-audio",
      label: "Thiếu audio",
      detail: `Đã có FULL và SHORT; ${audio === 0 ? "chưa có audio" : "còn thiếu một audio"}. Audio tùy chọn, không chặn gửi duyệt.`,
      Icon: VolumeX,
    };
  return {
    state: "complete",
    label: "Đủ kịch bản và audio",
    detail: "Đã có kịch bản FULL, SHORT và audio tương ứng.",
    Icon: Check,
  };
}
export function LanguageCoverage({
  code,
  language,
  assets,
}: {
  code: string;
  language: string;
  assets: Record<string, Asset>;
}) {
  const id = useId();
  const [dismissed, setDismissed] = useState(false);
  const { state, label, detail, Icon } = coverage(assets, code);
  const description = `${language}: ${label}. ${detail}`;
  return (
    <span
      className={`language-chip-wrap ${dismissed ? "tooltip-dismissed" : ""}`}
      onMouseEnter={() => setDismissed(false)}
    >
      <span
        className={`language-chip language-${state}`}
        role="img"
        tabIndex={0}
        aria-label={`${language}: ${label}`}
        aria-describedby={id}
        title={description}
        onFocus={() => setDismissed(false)}
        onKeyDown={(e) => {
          if (e.key === "Escape") setDismissed(true);
        }}
      >
        <Icon size={13} aria-hidden="true" />
        <span translate="no">{code.toUpperCase()}</span>
      </span>
      <span className="language-tooltip" id={id} role="tooltip">
        {description}
      </span>
    </span>
  );
}
