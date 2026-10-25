# Demo Android → PostgreSQL → Web Admin

> Cập nhật 25/10/2026: [Docker Guide](../guide/DOCKER_GUIDE.md) là nguồn cấu hình hiện hành. Chạy `scripts/start-live-demo.ps1` sẽ khởi động cả hai web cùng backend/DB; không cần `-TouristWeb` hay container Web du khách riêng.

Demo local/cùng Wi-Fi, không cần thuê server. Audio tiếng Việt phát thật; địa điểm và kịch bản là nội dung minh họa do nhóm chuẩn bị.

## Khởi động

Cần Docker Desktop đang chạy. Tại thư mục gốc repository:

```powershell
# Emulator hoặc máy local:
.\scripts\start-live-demo.ps1
# Cho điện thoại/máy khác trên cùng Wi-Fi tin cậy:
.\scripts\start-live-demo.ps1 -Lan
```

Lần đầu script tạo `.env.demo.local` (Git bỏ qua) với mật khẩu ngẫu nhiên cho database và Admin. Không đưa file này vào release/commit. Có thể tự cấu hình bằng `demo.env.example`. Project Compose `hvp-live-demo` dùng volume PostgreSQL riêng, backend **8081**, web **3001**, không thay stack cũ ở 8080/3000.

Web Admin: <http://localhost:3001/analytics>. Tài khoản là `DEMO_ADMIN_USERNAME`, mật khẩu là `DEMO_ADMIN_PASSWORD` trong `.env.demo.local`. Credentials chỉ giữ trong bộ nhớ tab, reload cần đăng nhập lại. API `/api/admin/*` kiểm tra xác thực; APK không chứa mật khẩu Admin.

HTTP và Basic Auth chỉ dành cho demo local/mạng tin cậy. Không đưa cấu hình này trực tiếp lên Internet; production cần HTTPS và cơ chế xác thực/phân quyền chính thức.

## Kết nối Android

Build/cài APK debug theo [Android README](../../android/README.md). Debug cho phép HTTP local; release giữ mặc định HTTPS của Android. Mở tab **Nghe demo**:

* Emulator: `http://10.0.2.2:8081/` là server mặc định.
* Điện thoại: cùng Wi-Fi với máy chạy Docker; lấy IPv4 card Wi-Fi bằng `ipconfig`, nhập `http://<IPv4-máy-tính>:8081/`, nhấn **Kết nối server**. `localhost` trên điện thoại là điện thoại đó.
* Web trên máy khác: `http://<IPv4-máy-tính>:3001/analytics`.
* Giữ Docker chạy suốt buổi demo. Nếu Windows Firewall chặn, cấu hình quyền cổng 8081/3001 chỉ trên mạng Private theo quy định máy; script không tắt firewall.

Du khách không cần tài khoản. Android lấy catalog/SHORT guide APPROVED từ backend; server lỗi không tự chuyển sang mock. Tab Khám phá/Ngoại tuyến cũ vẫn là mô phỏng, có nhãn riêng.

## Kịch bản trình diễn

1. Đăng nhập Web Admin, xem tổng lượt phát. Database mới bắt đầu từ **0**, không seed lượt nghe.
2. Android chọn **Bảo tàng Demo**, đọc đoạn ngắn từ server. Mở màn hình/tải audio chưa tạo lượt.
3. Nhấn **Phát thuyết minh**. Khi MediaPlayer bắt đầu phát, app gửi sự kiện; chờ **Đã đồng bộ lượt nghe với server**.
4. Web tự cập nhật mỗi 5 giây khi đang hiển thị, hoặc nhấn **Làm mới số liệu**. Tổng và địa điểm/ngôn ngữ tương ứng tăng một.
5. Tạm dừng/tiếp tục không tăng lượt. Nghe hết rồi **Nghe lại** tạo phiên mới. Đổi kỳ báo cáo và xuất XLSX để kiểm tra số liệu DB.

Một lượt = **một phiên bắt đầu phát**, không phải nghe hết/người nghe duy nhất. Không thu tài khoản du khách, ID thiết bị hay GPS. Demo ghi online; chưa đồng bộ nội dung/playback offline. Lỗi gửi: app giữ sự kiện trong outbox, gửi lại cùng UUID để chống đếm trùng; không phát lại audio chỉ để gửi lượt nghe.

## Database và API

Flyway: V1 schema, V2 discovery, V2.1 fixture địa điểm (demo), V3 liên kết revision/sự kiện, V3.1 ba SHORT guides/audio metadata (demo). Không sửa migration đã chạy. Profile mặc định không mở API playback demo; giữ profile `demo` với volume đã có seed.

| API | Hành vi |
| --- | --- |
| `GET /api/locations` | Catalog APPROVED trong database |
| `GET /api/locations/{id}/guide?language=VI&scriptType=SHORT` | Revision APPROVED; fallback đang chọn → EN → VI, giữ script type |
| `GET /demo-audio/{name}.wav` | WAV thật, hỗ trợ HTTP Range |
| `POST /api/playbacks` | JSON `{eventId, contentId}`; server suy ra location/language và thời điểm nhận; retry cùng ID không tăng count |
| `GET /api/admin/analytics?from=YYYY-MM-DD&to=YYYY-MM-DD` | Basic Auth; mặc định 30 ngày; ngày inclusive theo Asia/Ho_Chi_Minh, tối đa 366 ngày |

SQL kiểm tra tại root:

```powershell
docker compose --env-file .env.demo.local -p hvp-live-demo exec postgres psql -U audio_guide -d audio_guide -c "SELECT event_type, language_code, count(*) FROM analytics_events GROUP BY event_type, language_code;"
docker compose --env-file .env.demo.local -p hvp-live-demo restart backend
```

Refresh sau restart: dữ liệu vẫn trong volume. Dừng bằng `docker compose --env-file .env.demo.local -p hvp-live-demo stop`. Không dùng `down -v` nếu muốn giữ lượt nghe.

## Audio và kiểm tra

WAV tại `backend/multi-audio-guide/src/main/resources/static/demo-audio/` được tổng hợp bằng eSpeak NG 1.52.0, giọng `vi`, tốc độ 155. Đây là giọng tổng hợp cơ bản, không phải bản thu du lịch chuyên nghiệp. Transcript lấy từ seed V3.1; tạo lại bằng `./backend/generate-demo-audio.ps1` (Docker), không dùng TTS trả phí.

Backend: `mvnw.cmd verify` với `TEST_DATABASE_URL/USERNAME/PASSWORD` trỏ đến DB PostGIS kiểm thử riêng; không trỏ vào database demo đang dùng.

Web: `npm run lint`, `npm run build`, `npm run test:e2e`. `live-analytics.spec.ts` cần backend 8081 và env `DEMO_ADMIN_PASSWORD`; test tạo sự kiện có thật. UI mock cũ: `/analytics?source=mock`.

Android: host tests không chứng minh audio native. `LivePlaybackTest` cần emulator/backend thật và instrumentation argument `demoAdminPassword`; kiểm tra MediaPlayer tiến thời gian, count tăng sau start/replay và không tăng khi pause/resume. Không nhúng credentials kiểm thử trong APK.

Runbook mô tả cách chạy; kết quả kiểm chứng và giới hạn từng lần được lưu tại Jira HVP-21/HVP-22.
