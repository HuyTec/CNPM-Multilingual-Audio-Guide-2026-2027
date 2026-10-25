import { useEffect, useRef, useState, type FormEvent } from "react";
import {
  Link,
  useBlocker,
  useNavigate,
  useLocation,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { ArrowLeft, Upload, ShieldCheck } from "lucide-react";
import { type Asset, type Location } from "../mocks/data";
import {
  adminRequest,
  useAdminSession,
  type ContentLocation,
} from "../lib/admin-content";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { AudioPlayer } from "../components/ui/AudioPlayer";
import { ConfirmDialog } from "../components/ui/ConfirmDialog";
import { StatusBadge } from "../components/ui/StatusBadge";
import { ErrorState, LoadingState } from "../components/ui/States";

function blankLocation(): ContentLocation {
  return {
    id: crypto.randomUUID(),
    name: "",
    names: {},
    category: "",
    latitude: "",
    longitude: "",
    radius: "",
    status: "DRAFT",
    packageVersion: "Chưa gán phiên bản làm việc",
    editVersion: 0,
    assets: {},
  };
}
export default function LocationForm() {
  const route = useLocation();
  const { id } = useParams();
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const { authorization, config, identity } = useAdminSession();
  const categories = config!.categories,
    languages = config!.languages;
  const [form, setForm] = useState<ContentLocation>(blankLocation);
  const [found, setFound] = useState(true);
  const [load, setLoad] = useState(id ? "loading" : "ready");
  const [revision, setRevision] = useState(0);
  const [previews, setPreviews] = useState<Record<string, string>>({});
  const [dirty, setDirty] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState(
    (route.state as { savedMessage?: string } | null)?.savedMessage || "",
  );
  const [busy, setBusy] = useState(false);
  const [translating, setTranslating] = useState(false);
  const [targets, setTargets] = useState<string[]>([]);
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
    text: "",
  };
  useEffect(() => {
    if (!id) return;
    let active = true;
    const abort = new AbortController();
    void adminRequest<ContentLocation>(
      `/api/admin/content/locations/${id}`,
      authorization,
      { signal: abort.signal },
    )
      .then((data) => {
        if (data.id !== id || !data.assets)
          throw new Error("Dữ liệu địa điểm không hợp lệ.");
        if (active) {
          setForm(data);
          setFound(true);
          setLoad("ready");
        }
      })
      .catch((error) => {
        if (active) {
          setMessage(error.message);
          if (error.status === 404) setFound(false);
          setLoad("error");
        }
      });
    return () => {
      active = false;
      abort.abort();
    };
  }, [id, authorization, revision]);
  useEffect(() => {
    const path = asset.audioUrl;
    if (!path?.startsWith("/api/content-audio/") || previews[path]) return;
    let active = true;
    void fetch(
      path.replace("/api/content-audio/", "/api/admin/content/audio/"),
      { headers: { Authorization: authorization }, cache: "no-store" },
    )
      .then((response) => {
        if (!response.ok) throw new Error("Chưa tải được audio xem trước.");
        return response.blob();
      })
      .then((blob) => {
        if (active) {
          const url = URL.createObjectURL(blob);
          urls.current.push(url);
          setPreviews((old) => ({ ...old, [path]: url }));
        }
      })
      .catch((error) => {
        if (active) setUploadError(error.message);
      });
    return () => {
      active = false;
    };
  }, [asset.audioUrl, authorization, previews]);
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      dirty && currentLocation.pathname !== nextLocation.pathname,
  );
  // Register the latest navigation guard before redirecting a newly saved draft.
  useEffect(() => {
    if (!id && form.editVersion && !dirty)
      void navigate(`/locations/${form.id}/edit?${params.toString()}`, {
        replace: true,
        state: { savedMessage: message },
      });
  }, [id, form.editVersion, form.id, dirty, navigate, params, message]);
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
    setForm((old) => {
      if (field !== "name") return { ...old, [field]: value };
      const names = { ...(old.names || { vi: old.name }), [language]: value };
      const name =
        names.vi?.trim() ||
        names.en?.trim() ||
        Object.keys(names)
          .sort()
          .map((key) => names[key].trim())
          .find(Boolean) ||
        "";
      const originKey = `name:${language}`;
      const origin = old.fieldOrigins?.[originKey];
      return {
        ...old,
        name,
        names,
        fieldOrigins: {
          ...old.fieldOrigins,
          [originKey]:
            origin && origin !== "MANUAL" ? "EDITED_AFTER_TRANSLATE" : "MANUAL",
        },
      };
    });
    setDirty(true);
    setMessage("");
    setErrors((old) => ({ ...old, [field]: "" }));
  }
  function updateAsset(next: Asset, target = key) {
    setForm((old) => {
      const originKey = `script:${target}`;
      const origin = old.fieldOrigins?.[originKey];
      return {
        ...old,
        assets: { ...old.assets, [target]: next },
        fieldOrigins: {
          ...old.fieldOrigins,
          [originKey]:
            origin && origin !== "MANUAL" ? "EDITED_AFTER_TRANSLATE" : "MANUAL",
        },
      };
    });
    setDirty(true);
    setMessage("");
  }
  async function translate() {
    if (!targets.length || !form.names?.[language]?.trim()) {
      setMessage("Nhập tên nguồn và chọn ít nhất một ngôn ngữ đích.");
      return;
    }
    if (
      targets.some(
        (l) =>
          form.names?.[l] ||
          form.assets[`${l}:FULL`]?.text ||
          form.assets[`${l}:SHORT`]?.text,
      ) &&
      !window.confirm(
        "Dịch sẽ thay nội dung các ngôn ngữ đích đã chọn. Tiếp tục?",
      )
    )
      return;
    setTranslating(true);
    setMessage("");
    try {
      const result = await adminRequest<{
        provider: string;
        simulated: boolean;
        variants: {
          language: string;
          name: string;
          full: string;
          shortText: string;
        }[];
      }>("/api/admin/content/translate", authorization, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          sourceLanguage: language,
          targetLanguages: targets,
          name: form.names?.[language],
          full: form.assets[`${language}:FULL`]?.text || "",
          shortText: form.assets[`${language}:SHORT`]?.text || "",
        }),
      });
      setForm((old) => {
        const names = { ...old.names },
          assets = { ...old.assets },
          fieldOrigins = { ...old.fieldOrigins };
        for (const variant of result.variants) {
          names[variant.language] = variant.name;
          fieldOrigins[`name:${variant.language}`] = "AUTO_TRANSLATED";
          for (const type of ["FULL", "SHORT"] as const) {
            const key = `${variant.language}:${type}`;
            assets[key] = {
              ...assets[key],
              locationId: old.id,
              languageCode: variant.language,
              scriptType: type,
              packageVersion: old.packageVersion,
              text: type === "FULL" ? variant.full : variant.shortText,
            };
            fieldOrigins[`script:${key}`] = "AUTO_TRANSLATED";
          }
        }
        return { ...old, names, assets, fieldOrigins };
      });
      setDirty(true);
      setMessage(
        result.simulated
          ? "DEMO: kết quả giả lập, chưa phải bản dịch thật. Hãy kiểm tra và sửa trước khi gửi duyệt."
          : "Đã dịch. Hãy kiểm tra tên và kịch bản trước khi gửi duyệt; audio vẫn upload thủ công.",
      );
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Dịch lỗi; dữ liệu được giữ nguyên.",
      );
    } finally {
      setTranslating(false);
    }
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
      !Number.isInteger(Number(form.radius)) ||
      Number(form.radius) <= 0
    )
      next.radius = "Nhập bán kính là số nguyên lớn hơn 0 mét.";
    setErrors(next);
    if (Object.keys(next).length) {
      document.getElementById(Object.keys(next)[0])?.focus();
      return false;
    }
    return true;
  }
  /** Validate decoding, upload to the authenticated backend, and replace one audio variant. */
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
    let uploaded: Pick<Asset, "audioUrl" | "audioName" | "audioStatus">;
    try {
      const payload = new FormData();
      payload.set("locationId", form.id);
      payload.set("language", current.languageCode.toUpperCase());
      payload.set("scriptType", current.scriptType);
      payload.set("file", file);
      uploaded = await adminRequest("/api/admin/content/audio", authorization, {
        method: "POST",
        body: payload,
      });
      if (!uploaded.audioUrl)
        throw new Error("Server chưa trả về tệp audio đã lưu.");
      urls.current.push(url);
      setPreviews((old) => ({ ...old, [uploaded.audioUrl!]: url }));
    } catch (error) {
      URL.revokeObjectURL(url);
      setUploadError(
        error instanceof Error
          ? error.message
          : "Upload chưa thành công; dữ liệu vẫn được giữ.",
      );
      setUploadBusy(false);
      return;
    }
    // Merge only audio metadata: text edited during an upload/retry must survive.
    setForm((old) => ({
      ...old,
      assets: {
        ...old.assets,
        [target]: {
          ...(old.assets[target] || current),
          ...uploaded,
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
    const next = { ...form, status, assets: { ...form.assets, [key]: asset } };
    try {
      const saved = await adminRequest<ContentLocation>(
        `/api/admin/content/locations/${form.id}`,
        authorization,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(next),
        },
      );
      setForm(saved);
      setDirty(false);
      setMessage(
        status === "DRAFT"
          ? "Đã lưu bản nháp vào database chung."
          : "Đã gửi duyệt trong database. Bản đã xuất bản vẫn được giữ nguyên.",
      );
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : "Không thể lưu vào database. Dữ liệu form được giữ nguyên; hãy thử lại.",
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
          <ErrorState
            message={message}
            retry={() => {
              setLoad("loading");
              setRevision((value) => value + 1);
            }}
          />
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
            ? "Bản APPROVED đang chạy trên ứng dụng được giữ nguyên cho đến khi bản thay thế được duyệt và xuất bản."
            : "Lưu nháp hoặc gửi duyệt không xuất bản nội dung lên ứng dụng du khách."}
        </p>
      </div>
      {(form.status === "REJECTED" || form.reviewerComment) && (
        <section className="notice" aria-label="Kết quả xét duyệt">
          <div className="min-w-0">
            <h2>
              {form.status === "REJECTED"
                ? "Nội dung bị từ chối"
                : "Nhận xét của người duyệt"}
            </h2>
            <p className="pre-wrap">
              {form.reviewerComment || "Chưa có nhận xét từ người duyệt."}
            </p>
            <p>
              Sửa nội dung theo nhận xét, sau đó lưu nháp hoặc gửi duyệt lại.
              REVIEWER sẽ duyệt và xuất bản trong hệ thống.
            </p>
          </div>
        </section>
      )}
      {identity?.role === "REVIEWER" && (
        <p>
          Bạn đang xem với vai trò REVIEWER.{" "}
          <Link to="/reviews">Mở hàng đợi duyệt</Link>.
        </p>
      )}
      <form onSubmit={submit} noValidate>
        <fieldset
          disabled={
            busy ||
            uploadBusy ||
            translating ||
            identity?.role === "REVIEWER" ||
            (form.status === "APPROVED" && !!form.editVersion)
          }
          className="editor-fields"
        >
          <section className="card" aria-labelledby="translation-heading">
            <h2 id="translation-heading">Nhập thủ công hoặc dịch tự động</h2>
            <p>
              Ngôn ngữ đang chọn là nguồn. Bạn có thể nhập mọi ô bằng tay; dịch
              chỉ tạo văn bản.
            </p>
            <p>
              Nguồn tên: {form.fieldOrigins?.[`name:${language}`] || "MANUAL"} ·
              Kịch bản: {form.fieldOrigins?.[`script:${key}`] || "MANUAL"}
            </p>
            <fieldset>
              <legend>Ngôn ngữ đích</legend>
              {languages
                .filter((l) => l.code !== language)
                .map((l) => (
                  <label key={l.code}>
                    <input
                      type="checkbox"
                      name="translationTargets"
                      value={l.code}
                      checked={targets.includes(l.code)}
                      onChange={(e) =>
                        setTargets((old) =>
                          e.target.checked
                            ? [...old, l.code]
                            : old.filter((x) => x !== l.code),
                        )
                      }
                    />{" "}
                    {l.label}{" "}
                  </label>
                ))}
            </fieldset>
            <Button
              type="button"
              busy={translating}
              onClick={() => void translate()}
            >
              Dịch tự động
            </Button>
          </section>
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
                  [
                    "name",
                    "category",
                    "latitude",
                    "longitude",
                    "radius",
                  ] as const
                ).map((field) => (
                  <Field
                    key={field}
                    id={field}
                    label={
                      {
                        name: `Tên địa điểm · ${languages.find((item) => item.code === language)?.label || language}`,
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
                        value={
                          field === "name"
                            ? (form.names?.[language] ??
                              (language === "vi" ? form.name : ""))
                            : form[field]
                        }
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
                help="Bản biên tập chỉ công khai sau khi được duyệt và xuất bản."
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
                <p className="muted">MP3, WAV, OGG · Tối đa 20 MB mỗi tệp</p>
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
              <AudioPlayer
                src={
                  asset.audioUrl?.startsWith("/api/content-audio/")
                    ? previews[asset.audioUrl]
                    : asset.audioUrl
                }
                transcript={asset.text}
              />
            </section>
          </div>
          <div className="form-footer">
            <span role="status" aria-live="polite">
              {message ||
                (dirty
                  ? "Có thay đổi chưa lưu"
                  : "Đang biên tập dữ liệu từ database chung")}
            </span>
            <div className="actions">
              <Button
                secondary
                busy={busy || uploadBusy}
                onClick={() => requestSave("DRAFT")}
              >
                {uploadBusy
                  ? "Đang tải audio…"
                  : busy
                    ? "Đang lưu…"
                    : "Lưu nháp"}
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
        </fieldset>
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
