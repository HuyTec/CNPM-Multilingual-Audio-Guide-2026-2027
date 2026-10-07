import { EmptyState } from "./EmptyState";
import { useState } from "react";
export function AudioPlayer({
  src,
  transcript,
}: {
  src?: string;
  transcript: string;
}) {
  const [failedSource, setFailedSource] = useState("");
  return src ? (
    <div>
      <audio
        controls
        preload="metadata"
        src={src}
        aria-label="Nghe thử audio thuyết minh"
        onError={() => setFailedSource(src)}
      />
      <p className="field-error" aria-live="polite">
        {failedSource === src
          ? "Không thể giải mã audio này. Hãy chọn tệp khác; nội dung form được giữ nguyên."
          : ""}
      </p>
      <details>
        <summary>Kịch bản đối chiếu audio</summary>
        <p className="pre-wrap">
          {transcript || "Chưa có kịch bản đối chiếu."}
        </p>
      </details>
    </div>
  ) : (
    <EmptyState title="Chưa có audio nghe thử">
      <p>Chọn tệp audio để nghe thử. Nội dung văn bản vẫn có thể lưu nháp.</p>
    </EmptyState>
  );
}
