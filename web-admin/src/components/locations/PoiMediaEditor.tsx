import { useEffect, useRef, useState } from "react";
import { ImagePlus, Images, Trash2, Upload } from "lucide-react";
import { Button } from "../ui/Button";
import { ConfirmDialog } from "../ui/ConfirmDialog";
import "./poi-media.css";

type Photo = { id: string; name: string; url: string; size: number };
const PAGE_SIZE = 12;
const MAX_BYTES = 10 * 1024 * 1024;

/** Local UX preview only. Bytes and URLs never enter the existing content API payload. */
export function PoiMediaEditor({
  disabled,
  onDirtyChange,
}: {
  disabled: boolean;
  onDirtyChange: (dirty: boolean) => void;
}) {
  const [cover, setCover] = useState<Photo | null>(null);
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);
  const [page, setPage] = useState(0);
  const [removing, setRemoving] = useState<Photo | null>(null);
  const [preview, setPreview] = useState<Photo | null>(null);
  const coverInput = useRef<HTMLInputElement>(null);
  const galleryInput = useRef<HTMLInputElement>(null);
  const urls = useRef(new Set<string>());
  const alive = useRef(true);
  const processing = useRef(false);
  useEffect(() => {
    alive.current = true;
    const owned = urls.current;
    return () => {
      alive.current = false;
      owned.forEach(URL.revokeObjectURL);
      owned.clear();
    };
  }, []);
  async function add(files: File[], asCover: boolean) {
    if (processing.current || disabled || files.length === 0) return;
    processing.current = true;
    setBusy(true);
    setErrors([]);
    const accepted: Photo[] = [];
    const failures: string[] = [];
    for (const file of asCover ? files.slice(0, 1) : files) {
      try {
        if (file.size > MAX_BYTES)
          throw new Error("vượt quá 10 MB; chọn ảnh nhỏ hơn.");
        const header = new Uint8Array(await file.slice(0, 12).arrayBuffer());
        const png = [137, 80, 78, 71, 13, 10, 26, 10].every(
          (byte, index) => header[index] === byte,
        );
        const jpeg =
          header[0] === 255 && header[1] === 216 && header[2] === 255;
        const ascii = (start: number, end: number) =>
          String.fromCharCode(...header.slice(start, end));
        const webp = ascii(0, 4) === "RIFF" && ascii(8, 12) === "WEBP";
        if (!png && !jpeg && !webp)
          throw new Error("không phải PNG, JPEG hoặc WebP hợp lệ.");
        const bitmap = await createImageBitmap(file);
        bitmap.close();
        if (!alive.current) break;
        const url = URL.createObjectURL(file);
        urls.current.add(url);
        accepted.push({
          id: crypto.randomUUID(),
          name: file.name,
          url,
          size: file.size,
        });
      } catch (error) {
        failures.push(
          `${file.name}: ${error instanceof Error ? error.message : "chưa đọc được ảnh. Hãy chọn lại tệp."}`,
        );
      }
    }
    processing.current = false;
    if (!alive.current) return;
    if (accepted.length > 0) {
      if (asCover)
        setCover((previous) => {
          if (previous) {
            URL.revokeObjectURL(previous.url);
            urls.current.delete(previous.url);
          }
          return accepted[0];
        });
      else {
        setPhotos((previous) => [...previous, ...accepted]);
        setPage(Math.floor(photos.length / PAGE_SIZE));
      }
      onDirtyChange(true);
    }
    setErrors(failures);
    setBusy(false);
  }
  function remove() {
    if (!removing) return;
    if (cover?.id === removing.id) setCover(null);
    else
      setPhotos((previous) =>
        previous.filter((photo) => photo.id !== removing.id),
      );
    URL.revokeObjectURL(removing.url);
    urls.current.delete(removing.url);
    setRemoving(null);
    onDirtyChange(true);
  }
  const totalPages = Math.max(1, Math.ceil(photos.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages - 1);
  function photoTile(photo: Photo, isCover = false) {
    return (
      <figure className="poi-photo" key={photo.id}>
        <button
          type="button"
          className="poi-photo-preview"
          onClick={() => setPreview(photo)}
          aria-label={`Xem ảnh ${photo.name}`}
        >
          <img
            src={photo.url}
            alt={photo.name}
            width={320}
            height={200}
            loading="lazy"
          />
        </button>
        <figcaption>
          <span title={photo.name}>{photo.name}</span>
          <small>
            {new Intl.NumberFormat("vi-VN", {
              style: "unit",
              unit: photo.size >= 1024 * 1024 ? "megabyte" : "kilobyte",
              maximumFractionDigits: 1,
            }).format(
              photo.size / (photo.size >= 1024 * 1024 ? 1024 * 1024 : 1024),
            )}
          </small>
        </figcaption>
        <button
          type="button"
          className="poi-photo-remove"
          disabled={disabled || busy}
          onClick={() => setRemoving(photo)}
          aria-label={`Xóa ${isCover ? "ảnh đại diện" : "ảnh"} ${photo.name}`}
        >
          <Trash2 size={16} aria-hidden="true" />
        </button>
      </figure>
    );
  }
  return (
    <section className="card poi-media" aria-labelledby="poi-media-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">HÌNH ẢNH ĐỊA ĐIỂM</p>
          <h2 id="poi-media-title">Cho du khách một cái nhìn trước</h2>
          <p className="muted">
            Ảnh đại diện cho danh sách; album kể thêm về không gian và những góc
            nhìn khác.
          </p>
        </div>
        <Images aria-hidden="true" size={24} />
      </div>
      <p className="poi-media-preview-note" role="note">
        Bản xem trước UX: ảnh chỉ giữ trong phiên biên tập này. Lưu nháp và Gửi
        duyệt hiện chưa lưu ảnh lên máy chủ.
      </p>
      <div className="poi-media-layout">
        <div className="poi-cover">
          <h3>Ảnh đại diện</h3>
          <p className="muted">Một ảnh riêng cho thẻ địa điểm.</p>
          {cover ? (
            photoTile(cover, true)
          ) : (
            <div className="poi-photo-empty">
              <ImagePlus size={28} aria-hidden="true" />
              <span>Chưa có ảnh đại diện</span>
            </div>
          )}
          <input
            ref={coverInput}
            className="sr-only"
            tabIndex={-1}
            id="poi-cover-file"
            name="poi-cover-file"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            disabled={disabled || busy}
            onChange={(event) => {
              void add(Array.from(event.target.files || []), true);
              event.target.value = "";
            }}
            aria-label="Chọn ảnh đại diện địa điểm"
          />
          <Button
            secondary
            disabled={disabled || busy}
            onClick={() => coverInput.current?.click()}
          >
            {cover ? "Thay ảnh đại diện" : "Chọn ảnh đại diện"}
          </Button>
        </div>
        <div className="poi-gallery">
          <h3>
            Album địa điểm{" "}
            <span className="poi-photo-count">
              {new Intl.NumberFormat("vi-VN").format(photos.length)} ảnh
            </span>
          </h3>
          <p className="muted">
            Thêm nhiều ảnh; album độc lập với ảnh đại diện.
          </p>
          <div
            className="poi-photo-drop"
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => {
              event.preventDefault();
              void add(Array.from(event.dataTransfer.files), false);
            }}
          >
            <Upload size={24} aria-hidden="true" />
            <p>Kéo ảnh vào đây hoặc chọn từ thiết bị</p>
            <p className="muted">PNG, JPEG, WebP · tối đa 10 MB mỗi ảnh</p>
            <input
              ref={galleryInput}
              className="sr-only"
              tabIndex={-1}
              id="poi-gallery-files"
              name="poi-gallery-files"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              multiple
              disabled={disabled || busy}
              onChange={(event) => {
                void add(Array.from(event.target.files || []), false);
                event.target.value = "";
              }}
              aria-label="Chọn nhiều ảnh cho album địa điểm"
            />
            <Button
              secondary
              busy={busy}
              disabled={disabled}
              onClick={() => galleryInput.current?.click()}
            >
              {busy ? "Đang kiểm tra ảnh…" : "Thêm ảnh vào album"}
            </Button>
          </div>
          <div role="status" aria-live="polite" className="poi-media-status">
            {!busy && photos.length > 0
              ? `${new Intl.NumberFormat("vi-VN").format(photos.length)} ảnh trong album xem trước.`
              : busy
                ? "Đang đọc và kiểm tra ảnh…"
                : "Chưa có ảnh trong album."}
          </div>
          {errors.length > 0 && (
            <div className="field-error" role="alert">
              <ul>
                {errors.map((error, index) => (
                  <li key={index}>{error}</li>
                ))}
              </ul>
              <Button
                secondary
                disabled={disabled || busy}
                onClick={() => galleryInput.current?.click()}
              >
                Chọn lại ảnh
              </Button>
            </div>
          )}
          <div className="poi-photo-grid">
            {photos
              .slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE)
              .map((photo) => photoTile(photo))}
          </div>
          {totalPages > 1 && (
            <nav aria-label="Phân trang album" className="actions">
              <Button
                secondary
                disabled={currentPage === 0}
                onClick={() => setPage(currentPage - 1)}
              >
                Trang trước
              </Button>
              <span>
                Trang {new Intl.NumberFormat("vi-VN").format(currentPage + 1)} /{" "}
                {new Intl.NumberFormat("vi-VN").format(totalPages)}
              </span>
              <Button
                secondary
                disabled={currentPage + 1 === totalPages}
                onClick={() => setPage(currentPage + 1)}
              >
                Trang sau
              </Button>
            </nav>
          )}
        </div>
      </div>
      <ConfirmDialog
        open={removing !== null}
        title="Xóa ảnh khỏi bản xem trước?"
        onCancel={() => setRemoving(null)}
        onConfirm={remove}
        confirmLabel="Xóa ảnh"
      >
        <p>{removing?.name}</p>
        <p>Các ảnh khác và nội dung đã xuất bản được giữ nguyên.</p>
      </ConfirmDialog>
      <ConfirmDialog
        open={preview !== null}
        title="Xem ảnh địa điểm"
        onCancel={() => setPreview(null)}
      >
        {preview && (
          <img
            className="poi-lightbox"
            src={preview.url}
            alt={preview.name}
            width={1200}
            height={800}
          />
        )}
      </ConfirmDialog>
    </section>
  );
}
