import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useBlocker, useParams, useSearchParams } from "react-router-dom";
import { ArrowLeft, Upload, ShieldCheck } from "lucide-react";
import {
  categories,
  getLocations,
  languages,
  longLocations,
  saveLocation,
  type Asset,
  type Location,
} from "../mocks/data";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { AudioPlayer } from "../components/ui/AudioPlayer";
import { ConfirmDialog } from "../components/ui/ConfirmDialog";
import { StatusBadge } from "../components/ui/StatusBadge";
import { ErrorState, LoadingState } from "../components/ui/States";
import { useDemoLoad } from "../lib/demo";

function blankLocation(): Location {
  return {
    id: crypto.randomUUID(),
    name: "",
    category: "",
    latitude: "",
    longitude: "",
    radius: "",
    status: "DRAFT",
    packageVersion: "working-demo",
    assets: {},
  };
}
export default function LocationForm() {
  const { id } = useParams();
  const [params, setParams] = useSearchParams();
  const scenario = params.get("scenario") || "normal";
  const [form, setForm] = useState<Location>(
    () =>
      (scenario === "long" ? longLocations() : getLocations()).find(
        (x) => x.id === id,
      ) || blankLocation(),
  );
  const found =
    !id ||
    (scenario === "long" ? longLocations() : getLocations()).some(
      (x) => x.id === id,
    );
  const [dirty, setDirty] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const [uploadBusy, setUploadBusy] = useState(false);
  const [saveStatus, setSaveStatus] = useState<
    "DRAFT" | "PENDING_REVIEW" | null
  >(null);
  const [pendingFile, setPendingFile] = useState<{
    file: File;
    key: string;
    asset: Asset;
  } | null>(null);
  const urls = useRef<string[]>([]);
  const audioInput = useRef<HTMLInputElement>(null);
  const language = languages.some((x) => x.code === params.get("language"))
    ? params.get("language")!
    : "vi";
  const type = params.get("type") === "SHORT" ? "SHORT" : "FULL";
  const key = `${language}:${type}`;
  const asset: Asset = form.assets[key] || {
    locationId: form.id,
    languageCode: language,
    scriptType: type,
    packageVersion: form.packageVersion,
    text:
      scenario === "long"
        ? "Nội dung dài minh họa để kiểm tra bố cục. ".repeat(150)
        : "",
  };
  const load = useDemoLoad(`${id || "new"}:${scenario}`, scenario);
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      dirty && currentLocation.pathname !== nextLocation.pathname,
  );
  useEffect(() => {
    const guard = (event: BeforeUnloadEvent) => {
      if (dirty) {
        event.preventDefault();
        event.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", guard);
    return () => window.removeEventListener("beforeunload", guard);
  }, [dirty]);
  useEffect(
    () => () => {
      urls.current.forEach((url) => URL.revokeObjectURL(url));
    },
    [],
  );
  function change(field: keyof Location, value: string) {
    setForm((old) => ({ ...old, [field]: value }));
    setDirty(true);
    setMessage("");
    setErrors((old) => ({ ...old, [field]: "" }));
  }
  function updateAsset(next: Asset, target = key) {
    setForm((old) => ({ ...old, assets: { ...old.assets, [target]: next } }));
    setDirty(true);
    setMessage("");
  }
  function query(name: string, value: string) {
    const next = new URLSearchParams(params);
    next.set(name, value);
    setParams(next, { replace: true });
  }
  function validate() {
    const next: Record<string, string> = {};
    if (!form.name.trim()) next.name = "Nhập tên địa điểm.";
    if (!form.category) next.category = "Chọn danh mục.";
    for (const [field, min, max, label] of [
      ["latitude", -90, 90, "Vĩ độ"],
      ["longitude", -180, 180, "Kinh độ"],
    ] as const) {
      const value = Number(form[field].replace(",", "."));
      if (
        !form[field].trim() ||
        !Number.isFinite(value) ||
        value < min ||
        value > max
      )
        next[field] = `${label} phải nằm trong khoảng ${min} đến ${max}.`;
    }
    if (
      !form.radius.trim() ||
      !Number.isFinite(Number(form.radius)) ||
      Number(form.radius) <= 0
    )
      next.radius = "Nhập bán kính lớn hơn 0 mét.";
    setErrors(next);
    if (Object.keys(next).length) {
      document.getElementById(Object.keys(next)[0])?.focus();
      return false;
    }
    return true;
  }
  /** Validate the demo allowlist and browser decoding; replace only the chosen audio key. */
  async function upload(file: File, retry = false) {
    const target = retry ? pendingFile?.key || key : key;
    const current = retry ? pendingFile?.asset || asset : asset;
    setPendingFile({ file, key: target, asset: current });
    if (!/\.(mp3|wav|ogg)$/i.test(file.name)) {
      setUploadError(
        "Định dạng không hợp lệ. Demo hỗ trợ MP3, WAV, OGG; hãy chọn tệp khác.",
      );
      return;
    }
    setUploadBusy(true);
    setUploadError("");
    await new Promise((resolve) => setTimeout(resolve, 400));
    if (scenario === "upload-error" && !retry) {
      setUploadError(
        "Upload giả lập thất bại. Dữ liệu đã nhập được giữ nguyên; hãy thử lại.",
      );
      setUploadBusy(false);
      return;
    }
    const url = URL.createObjectURL(file);
    const decodable = await new Promise<boolean>((resolve) => {
      const preview = new Audio();
      const timer = setTimeout(() => finish(false), 5000);
      function finish(ok: boolean) {
        clearTimeout(timer);
        preview.onloadedmetadata = null;
        preview.onerror = null;
        preview.removeAttribute("src");
        preview.load();
        resolve(ok);
      }
      preview.onloadedmetadata = () => finish(true);
      preview.onerror = () => finish(false);
      preview.src = url;
    });
    if (!decodable) {
      URL.revokeObjectURL(url);
      setUploadError(
        "Không thể giải mã audio. Hãy chọn tệp MP3, WAV hoặc OGG hợp lệ; dữ liệu form được giữ nguyên.",
      );
      setUploadBusy(false);
      return;
    }
    urls.current.push(url);
    // Merge only audio metadata: text edited during an upload/retry must survive.
    setForm((old) => ({
      ...old,
      assets: {
        ...old.assets,
        [target]: {
          ...(old.assets[target] || current),
          audioName: file.name,
          audioUrl: url,
          audioStatus: "ready",
        },
      },
    }));
    setDirty(true);
    setMessage("");
    setUploadBusy(false);
    setPendingFile(null);
  }
  async function save(status: "DRAFT" | "PENDING_REVIEW") {
    setSaveStatus(null);
    setBusy(true);
    setMessage("");
    await new Promise((resolve) => setTimeout(resolve, 400));
    const next = { ...form, status, assets: { ...form.assets, [key]: asset } };
    try {
      saveLocation(next);
      setForm(next);
      setDirty(false);
      setMessage(
        status === "DRAFT"
          ? "Đã lưu bản nháp trong trình duyệt."
          : "Đã gửi duyệt giả lập. Bản đã xuất bản vẫn được giữ nguyên.",
      );
    } catch {
      setMessage(
        "Không thể lưu trong trình duyệt. Dữ liệu form được giữ nguyên; hãy thử lại.",
      );
    }
    setBusy(false);
  }
  function requestSave(status: "DRAFT" | "PENDING_REVIEW") {
    if (status === "PENDING_REVIEW" && !validate()) return;
    if (id) setSaveStatus(status);
    else void save(status);
  }
  function submit(event: FormEvent) {
    event.preventDefault();
    requestSave("PENDING_REVIEW");
  }
  if (load === "loading")
    return (
      <>
        <h1>{id ? "Chỉnh sửa địa điểm" : "Thêm địa điểm"}</h1>
        <LoadingState />
      </>
    );
  if (load === "error" || !found)
    return (
      <>
        <h1>Chỉnh sửa địa điểm</h1>
        {found ? (
          <ErrorState retry={() => query("scenario", "normal")} />
        ) : (
          <p>
            Không tìm thấy địa điểm. <Link to="/locations">Về danh sách</Link>
          </p>
        )}
      </>
    );
  return (
    <>
      <Link className="text-link back-link" to="/locations">
        <ArrowLeft size={16} aria-hidden="true" />
        Danh sách địa điểm
      </Link>
      <div className="page-heading">
        <div>
          <p className="eyebrow">BIÊN TẬP NỘI DUNG</p>
          <h1>{id ? "Chỉnh sửa địa điểm" : "Thêm địa điểm"}</h1>
          <p className="muted">
            Chuẩn bị nội dung, nghe thử audio và gửi để duyệt.
          </p>
        </div>
        <StatusBadge status={form.status} />
      </div>
      <div className="notice">
        <ShieldCheck size={20} aria-hidden="true" />
        <p>
          {form.published
            ? `Phiên bản ${form.published.version} đang chạy trên ứng dụng được giữ nguyên cho đến khi bản thay thế được duyệt và xuất bản.`
            : "Lưu nháp hoặc gửi duyệt không xuất bản nội dung lên ứng dụng du khách."}
        </p>
      </div>
      <form onSubmit={submit} noValidate>
        <div className="form-grid">
          <section className="card">
            <div className="section-heading">
              <div>
                <h2>Thông tin địa điểm</h2>
                <p className="muted">
                  Bắt buộc khi gửi duyệt; bản nháp có thể để trống.
                </p>
              </div>
            </div>
            <div className="field-grid">
              {(
                ["name", "category", "latitude", "longitude", "radius"] as const
              ).map((field) => (
                <Field
                  key={field}
                  id={field}
                  label={
                    {
                      name: "Tên địa điểm",
                      category: "Danh mục",
                      latitude: "Vĩ độ",
                      longitude: "Kinh độ",
                      radius: "Bán kính geofence (m)",
                    }[field]
                  }
                  error={errors[field]}
                >
                  {field === "category" ? (
                    <select
                      id={field}
                      name={field}
                      disabled={busy}
                      value={form[field]}
                      aria-invalid={!!errors[field]}
                      aria-describedby={`${field}-error`}
                      onChange={(e) => change(field, e.target.value)}
                    >
                      <option value="">Chọn danh mục</option>
                      {categories.map((x) => (
                        <option key={x}>{x}</option>
                      ))}
                    </select>
                  ) : (
                    <input
                      id={field}
                      name={field}
                      disabled={busy}
                      value={form[field]}
                      type={field === "radius" ? "number" : "text"}
                      inputMode={field === "name" ? "text" : "decimal"}
                      min={field === "radius" ? 1 : undefined}
                      autoComplete="off"
                      spellCheck={field === "name"}
                      placeholder={
                        {
                          name: "Bưu điện Trung tâm…",
                          latitude: "10.7769…",
                          longitude: "106.7009…",
                          radius: "80…",
                        }[field]
                      }
                      aria-invalid={!!errors[field]}
                      aria-describedby={`${field}-error`}
                      onChange={(e) => change(field, e.target.value)}
                    />
                  )}
                </Field>
              ))}
            </div>
            <p className="footnote">
              ID: {form.id} · Phiên bản làm việc: {form.packageVersion}
            </p>
          </section>
          <section className="card">
            <div className="section-heading">
              <div>
                <h2>Kịch bản & audio</h2>
                <p className="muted">
                  Mỗi ngôn ngữ và loại kịch bản có nội dung riêng.
                </p>
              </div>
            </div>
            <div className="filters">
              <Field id="language" label="Ngôn ngữ">
                <select
                  id="language"
                  name="language"
                  value={language}
                  disabled={uploadBusy || busy}
                  onChange={(e) => {
                    setUploadError("");
                    query("language", e.target.value);
                  }}
                >
                  {languages.map((x) => (
                    <option key={x.code} value={x.code}>
                      {x.label}
                    </option>
                  ))}
                </select>
              </Field>
              <Field id="script-type" label="Loại kịch bản">
                <select
                  id="script-type"
                  name="scriptType"
                  value={type}
                  disabled={uploadBusy || busy}
                  onChange={(e) => {
                    setUploadError("");
                    query("type", e.target.value);
                  }}
                >
                  <option value="FULL">FULL · Đầy đủ</option>
                  <option value="SHORT">SHORT · Rút gọn</option>
                </select>
              </Field>
            </div>
            <Field
              id="script"
              label="Nội dung thuyết minh"
              help="Nội dung demo; chưa phải dữ liệu được duyệt."
            >
              <textarea
                disabled={busy}
                id="script"
                name="scriptText"
                autoComplete="off"
                rows={8}
                placeholder="Giới thiệu địa điểm và nội dung thuyết minh…"
                value={asset.text}
                aria-describedby="script-help"
                onChange={(e) =>
                  updateAsset({ ...asset, text: e.target.value })
                }
              />
            </Field>
            <div
              className="upload-box"
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                e.preventDefault();
                if (!uploadBusy && !busy && e.dataTransfer.files[0])
                  void upload(e.dataTransfer.files[0]);
              }}
            >
              <Upload size={24} aria-hidden="true" />
              <strong>Kéo tệp audio vào đây hoặc chọn tệp</strong>
              <p className="muted">MP3, WAV, OGG · Cấu hình minh họa</p>
              <label htmlFor="audio-file">
                Chọn audio cho {language} / {type}
              </label>
              <input
                ref={audioInput}
                className="sr-only"
                tabIndex={-1}
                id="audio-file"
                type="file"
                name="audio"
                accept=".mp3,.wav,.ogg"
                disabled={uploadBusy || busy}
                aria-describedby="audio-error"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) void upload(file);
                  e.target.value = "";
                }}
              />
              <Button
                secondary
                disabled={uploadBusy || busy}
                onClick={() => audioInput.current?.click()}
              >
                Chọn tệp audio
              </Button>
              {uploadBusy && <p role="status">Đang tải audio…</p>}
              <p id="audio-error" className="field-error" aria-live="polite">
                {uploadError}
              </p>
              {uploadError &&
                pendingFile &&
                /\.(mp3|wav|ogg)$/i.test(pendingFile.file.name) && (
                  <Button
                    secondary
                    onClick={() => {
                      if (pendingFile) void upload(pendingFile.file, true);
                    }}
                  >
                    Thử lại upload
                  </Button>
                )}
            </div>
            {asset.audioName && (
              <p className="footnote">
                Tệp: {asset.audioName}
                {!asset.audioUrl &&
                  " · Chọn lại tệp để nghe thử sau khi tải lại trang."}
              </p>
            )}
            <AudioPlayer src={asset.audioUrl} transcript={asset.text} />
          </section>
        </div>
        <div className="form-footer">
          <span role="status" aria-live="polite">
            {message ||
              (dirty
                ? "Có thay đổi chưa lưu"
                : "Dữ liệu demo được lưu trong trình duyệt")}
          </span>
          <div className="actions">
            <Button
              secondary
              busy={busy || uploadBusy}
              onClick={() => requestSave("DRAFT")}
            >
              {uploadBusy ? "Đang tải audio…" : busy ? "Đang lưu…" : "Lưu nháp"}
            </Button>
            <Button type="submit" busy={busy || uploadBusy}>
              {uploadBusy
                ? "Đang tải audio…"
                : busy
                  ? "Đang gửi…"
                  : "Gửi duyệt"}
            </Button>
          </div>
        </div>
      </form>
      <ConfirmDialog
        open={saveStatus !== null}
        title="Xác nhận lưu thay đổi"
        onCancel={() => setSaveStatus(null)}
        onConfirm={() => {
          if (saveStatus) void save(saveStatus);
        }}
        confirmLabel="Lưu thay đổi"
      >
        <p>
          Bản đang sửa sẽ được lưu. Phiên bản đã xuất bản và audio của các ngôn
          ngữ khác được giữ nguyên.
        </p>
      </ConfirmDialog>
      <ConfirmDialog
        open={blocker.state === "blocked"}
        title="Rời trang khi chưa lưu?"
        onCancel={() => blocker.reset?.()}
        onConfirm={() => blocker.proceed?.()}
        confirmLabel="Rời trang"
      >
        <p>
          Thay đổi chưa lưu sẽ bị mất. Hãy ở lại và lưu nháp nếu cần giữ nội
          dung.
        </p>
      </ConfirmDialog>
    </>
  );
}
