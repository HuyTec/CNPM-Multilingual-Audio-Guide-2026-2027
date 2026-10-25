import { useEffect, useState } from "react";
type Release = {
  url: string;
  publicUrl?: string;
  version: string;
  sizeBytes: number;
  sha256: string;
};
type State =
  | { status: "loading" }
  | { status: "error" }
  | { status: "ready"; release: Release; qr: string; url: string };
/** QR and download button always share one URL; QR generation never contacts an external service. */
export function AndroidAppPromo() {
  const [state, setState] = useState<State>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    const abort = new AbortController();
    async function load() {
      try {
        const response = await fetch("/downloads/android-release.json", {
          cache: "no-cache",
          signal: abort.signal,
        });
        if (!response.ok) throw new Error("No release");
        const release = (await response.json()) as Release;
        if (
          !release.version ||
          !Number.isSafeInteger(release.sizeBytes) ||
          release.sizeBytes <= 0 ||
          !/^[a-f0-9]{64}$/.test(release.sha256)
        )
          throw new Error("Invalid release");
        const url = new URL(
          import.meta.env.VITE_ANDROID_APK_URL || release.publicUrl || release.url,
          window.location.origin,
        );
        if (
          !["http:", "https:"].includes(url.protocol) ||
          url.username ||
          url.password
        )
          throw new Error("Invalid download URL");
        const { default: makeQr } = await import("qrcode-generator");
        const qr = makeQr(0, "M");
        qr.addData(url.href);
        qr.make();
        if (active)
          setState({
            status: "ready",
            release,
            qr: qr.createDataURL(4, 16),
            url: url.href,
          });
      } catch {
        if (active) setState({ status: "error" });
      }
    }
    void load();
    return () => {
      active = false;
      abort.abort();
    };
  }, [attempt]);
  return (
    <section
      id="android-app"
      className="android-promo"
      aria-labelledby="android-app-title"
    >
      <div className="android-promo-copy">
        <p className="eyebrow">Mang câu chuyện theo bạn</p>
        <h2 id="android-app-title">Khám phá cùng TravelVoice trên Android</h2>
        <p>
          Nghe thuyết minh nhiều ngôn ngữ, lưu nội dung để khám phá khi không có
          mạng và kiểm tra những địa điểm mới ngay trong ứng dụng.
        </p>
        {state.status === "loading" && (
          <p role="status">Đang kiểm tra bản Android…</p>
        )}
        {state.status === "error" && (
          <div>
            <p role="status">
              Bản tải Android chưa sẵn sàng. Bạn vẫn có thể khám phá và nghe
              trên website.
            </p>
            <button
              className="secondary-button"
              onClick={() => {
                setState({ status: "loading" });
                setAttempt((n) => n + 1);
              }}
            >
              Kiểm tra lại bản Android
            </button>
          </div>
        )}
        {state.status === "ready" && (
          <>
            <p className="android-release">
              Bản demo {state.release.version} ·{" "}
              {new Intl.NumberFormat("vi-VN", {
                maximumFractionDigits: 1,
              }).format(state.release.sizeBytes / 1024 / 1024)}{" "}
              MB
            </p>
            <a
              className="primary-button android-download"
              href={state.url}
              download="travelvoice.apk"
            >
              Tải ứng dụng Android
            </a>
            <p className="android-install-note">
              Tải tệp APK rồi mở để cài đặt. Android có thể yêu cầu cho phép cài
              ứng dụng từ trình duyệt này.
            </p>
          </>
        )}
      </div>
      {state.status === "ready" && (
        <figure className="android-promo-qr">
            <img
            src={state.qr}
            width="208"
            height="208"
            loading="lazy"
            alt="Mã QR tải ứng dụng TravelVoice cho Android"
          />
          <figcaption>Quét để tải trên điện thoại Android</figcaption>
          <p className="android-qr-host">{new URL(state.url).host}</p>
          <a className="android-qr-save" href={state.qr} download="travelvoice-android-qr.gif">Lưu mã QR</a>
        </figure>
      )}
    </section>
  );
}
