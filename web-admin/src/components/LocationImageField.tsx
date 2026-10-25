import { useEffect, useRef, useState } from "react";
import { Button } from "./ui/Button";

type Props = {
  locationId: string;
  imageUrl?: string;
  authorization: string;
  onChange: (url: string) => void;
  onBusyChange: (busy: boolean) => void;
};

export function LocationImageField({
  locationId,
  imageUrl,
  authorization,
  onChange,
  onBusyChange,
}: Props) {
  const input = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    if (!imageUrl) return;
    const abort = new AbortController();
    let objectUrl = "";
    void fetch(
      imageUrl.replace("/api/content-images/", "/api/admin/content/images/"),
      {
        headers: { Authorization: authorization },
        signal: abort.signal,
      },
    )
      .then(async (response) => {
        if (!response.ok)
          throw new Error("Không tải được ảnh xem trước. Hãy thử lại.");
        const blob = await response.blob();
        if (abort.signal.aborted) return;
        objectUrl = URL.createObjectURL(blob);
        setPreview(objectUrl);
      })
      .catch((error) => {
        if (!abort.signal.aborted) setError(error.message);
      })
      .finally(() => {
        if (!abort.signal.aborted) setLoading(false);
      });
    return () => {
      abort.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [imageUrl, authorization, retry]);
  async function upload(file: File) {
    setError("");
    if (!/\.(jpe?g|png)$/i.test(file.name) || file.size > 5 * 1024 * 1024) {
      setError(
        "Chọn ảnh JPEG/PNG tối đa 5 MB. Nội dung đang nhập được giữ nguyên.",
      );
      return;
    }
    onBusyChange(true);
    try {
      const body = new FormData();
      body.set("locationId", locationId);
      body.set("file", file);
      const response = await fetch("/api/admin/content/images", {
        method: "POST",
        headers: { Authorization: authorization },
        body,
      });
      if (!response.ok)
        throw new Error(
          "Không tải được ảnh. Kiểm tra định dạng, kích thước và kết nối rồi chọn lại.",
        );
      const result = (await response.json()) as { imageUrl: string };
      onChange(result.imageUrl);
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : "Tải ảnh lỗi. Hãy chọn lại để thử lại.",
      );
    } finally {
      onBusyChange(false);
    }
  }
  return (
    <section className="card" aria-labelledby="poi-image-heading">
      <h2 id="poi-image-heading">Ảnh địa điểm</h2>
      <p>
        Ảnh đại diện dùng chung cho các ngôn ngữ. Chọn JPEG/PNG tối đa 5 MB, mỗi
        chiều tối đa 4096 px và tối đa 12 triệu điểm ảnh.
      </p>
      {!imageUrl && <p>Chưa có ảnh. Bạn vẫn có thể lưu bản nháp.</p>}
      {imageUrl && loading && <p role="status">Đang tải ảnh xem trước…</p>}
      {imageUrl && preview && (
        <img
          src={preview}
          alt="Ảnh đại diện địa điểm đang biên tập"
          width={640}
          height={360}
          style={{ maxWidth: "100%", height: "auto", objectFit: "contain" }}
        />
      )}
      <label htmlFor="poi-image">Chọn ảnh địa điểm</label>
      <input
        ref={input}
        id="poi-image"
        name="locationImage"
        type="file"
        accept="image/jpeg,image/png"
        aria-describedby="poi-image-error"
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.target.value = "";
          if (file) void upload(file);
        }}
      />
      <p id="poi-image-error" className="field-error" aria-live="polite">
        {error}
      </p>
      {imageUrl && error && (
        <Button
          type="button"
          secondary
          onClick={() => {
            setLoading(true);
            setError("");
            setRetry((value) => value + 1);
          }}
        >
          Thử lại xem ảnh
        </Button>
      )}
      <p className="muted">
        Ảnh chỉ công khai khi nội dung được duyệt và xuất bản. Thay ảnh không
        làm đổi bản đang công khai.
      </p>
    </section>
  );
}
