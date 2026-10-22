# TravelVoice · Web du khách

React + Vite + TypeScript + Tailwind; cùng locationId, guide và gói nội dung với Android.
Hợp đồng: [GUIDE_API.md](../backend/GUIDE_API.md) và
[CONTENT_PACKAGE_API.md](../backend/CONTENT_PACKAGE_API.md).

```powershell
cd web-user
npm ci
npm run dev
```

Mở http://127.0.0.1:5175/ để chọn địa điểm. Proxy đọc `BACKEND_PORT` từ `.env` ở thư mục gốc
(nếu chưa đặt thì dùng 8080). Có thể ghi đè bằng `API_PROXY_TARGET` trong `web-user/.env.local`.
Backend dùng profile demo, DB đã áp dụng V3_3. Môi trường được kiểm tra ngày 22/10/2026 dùng cổng 8081.
Danh sách mặc định là bảy điểm từ tài liệu nhóm cung cấp; vẫn có lựa chọn tất cả
địa điểm cũ. Nội dung chưa thẩm định; tọa độ chỉ minh họa.

- URL /locations/:locationId?language=VI&scriptType=FULL giữ lựa chọn khi reload/back.
  Sáu ngôn ngữ VI/EN/FR/JA/ZH/RU; server quyết định APPROVED và fallback Selected → EN → VI,
  giữ FULL/SHORT. 404 không dùng bài offline cũ để thay nội dung đã ngừng công khai.
- Audio đúng bài được ưu tiên; lỗi/thiếu audio dùng TTS đúng effective language và
  nguyên văn scriptText. Chỉ phát khi bấm, có pause/resume, seek và tốc độ. TTS cần
  voice tương ứng trên trình duyệt/thiết bị; voice mạng không bảo đảm hoạt động offline.
  Lượt nghe chỉ gửi khi media/TTS báo bắt đầu. Lỗi gửi có nút thử lại bằng cùng eventId, tránh đếm trùng.
  Web hiện chưa có outbox lưu bền; lượt gửi lỗi chưa được giữ sau khi rời trang.
- /offline: kiểm tra → xác nhận tải → xác minh chữ ký/checksum JSON và từng audio →
  cài đặt nguyên tử. BASE được bảo vệ; OPTIONAL có thể xóa sau xác nhận. Bản cũ giữ
  nguyên khi lỗi/hủy. Chỉ thông báo sẵn sàng offline khi gói đã được xác minh.
- Mở lại ứng dụng offline cần production build/preview và service worker đã cài
  trong lần online trước. Dev server không cung cấp app shell offline. HTTPS cần
  cho triển khai ngoài localhost; chưa deploy.

## OpenStreetMap, OsmAnd và vùng Quận 1–3

Web User hiển thị nền OpenStreetMap trong ứng dụng, không cần Google Maps API key.
Trang chủ mở bản đồ bao phủ Quận 1 và Quận 3; bấm một ghim để mở bài thuyết minh hoặc mở tọa độ
đó trong OsmAnd. Liên kết OsmAnd dùng URL pin tương thích với ứng dụng di động và bản đồ web.

Ô tìm kiếm ở trên trái, ngôn ngữ bên cạnh; nút **Danh sách** mở bảng địa điểm. Từ khóa,
ngôn ngữ, bảng đang mở, trang danh sách và bán kính được giữ trong URL.

Nút **Định vị vị trí hiện tại** ở góc dưới phải hỏi quyền vị trí khi bạn bấm; nếu được cấp, bản đồ đặt ghim GPS hiện tại
trên thiết bị này. Tọa độ không được gửi lên backend. Trình duyệt cần HTTPS hoặc localhost.
Khung ban đầu là vùng demo Quận 1–3, **không phải ranh giới hành chính đã kiểm chứng**. Bảy ghim
thuyết minh hiện tại gồm Dinh Độc Lập, Nhà hát Thành phố, Nhà thờ Đức Bà, Trường Đại học Sài Gòn – Cơ sở 2,
Bảo tàng Chứng tích Chiến tranh, Bảo tàng Hồ Chí Minh (Bến Nhà Rồng) và Bitexco. Nền bản
đồ cần Internet; không tải tile vào gói offline. Bản đồ dùng tile OSM tiêu chuẩn và hiển thị
ghi công OpenStreetMap contributors. Khi triển khai có lưu lượng lớn, cần dùng nhà cung cấp
tile phù hợp hoặc tự host; không dùng tile server cộng đồng để tải trước hay dùng offline.

Android tiếp tục dùng Google Maps SDK với key riêng theo package/SHA-1 trong android/README.md.

### Tìm quanh GPS

Bấm **Lấy vị trí hiện tại**, cấp quyền rồi kéo **Phạm vi quét** dưới bản đồ từ 50–500 m
(mặc định 100 m, mỗi bước 1 m; hỗ trợ phím mũi tên, Home/End). Vòng tròn thể hiện bán kính;
danh sách phía dưới chỉ giữ các địa điểm trong phạm vi và xếp từ gần nhất đến xa dần.
Ban đầu hiển thị 5 điểm gần nhất; bấm **Xem tất cả địa điểm trong phạm vi** để mở rộng.
Ứng dụng quét toàn bộ các trang kết quả tìm kiếm hiện tại, tính đường chim bay trên trình duyệt;
không gửi GPS lên API hay lưu tọa độ vào URL. Danh sách dài phân trang 50 mục.

GPS là vị trí lấy một lần, bấm **Cập nhật vị trí** khi di chuyển. Trình duyệt báo độ chính xác;
nếu sai số lớn hơn bán kính, kết quả có thể sai lệch. Tọa độ demo chưa kiểm chứng,
không thay thế dữ liệu định vị đã khảo sát. Thiếu quyền GPS vẫn tìm kiếm thủ công được.

### Kiểm tra kết nối backend

Tại môi trường local hiện tại, `docker compose ps` hiển thị backend khỏe tại `127.0.0.1:8081`.
Web User gọi cùng origin `/api` qua Vite; không cần mở CORS hoặc nhúng mật khẩu backend vào mã frontend.

```powershell
Invoke-RestMethod 'http://127.0.0.1:8081/actuator/health'
Invoke-RestMethod 'http://127.0.0.1:5175/api/locations?size=50&categoryId=10000000-0000-0000-0000-000000000101'
Invoke-RestMethod 'http://127.0.0.1:5175/api/categories'
```

Nếu đổi cổng backend, sửa `BACKEND_PORT` tại `.env` gốc hoặc `API_PROXY_TARGET`, rồi khởi động lại Vite.
API phân trang chỉ chấp nhận `size` từ 1 đến 50; web tải tuần tự mọi trang để quét đủ địa điểm.
Trong **Danh sách**, chọn **Danh mục** để duyệt nhóm hoặc tất cả địa điểm; mặc định vẫn là tuyến 7 điểm.

Gói cơ bản cần xác nhận tải và xác minh trước khi dùng offline. Production build xuất
`asset-manifest.json`; service worker lưu cả JS/CSS tải chậm để danh sách vẫn mở được sau khi mất mạng.
Audio đã lưu được kiểm tra checksum trước khi báo gói đã xác minh. Test offline phải chạy bằng cấu hình
`playwright.offline.config.ts` với production preview; cấu hình test mặc định không chạy nhóm này.

UC-05 chưa được triển khai trên Web User: nút GPS hiện chỉ lấy vị trí một lần. Chưa có theo dõi
geofence hoặc thông báo nền. Việc triển khai cần tọa độ/tên địa điểm đã kiểm chứng, cấu hình cooldown
và xác minh hỗ trợ trình duyệt theo PRD; không coi dữ liệu demo là bằng chứng geofence hoạt động.

## Kiểm tra

```powershell
npm run lint
npm run build
npm run test:e2e
$env:LIVE_GUIDE_API='1'
npm run test:e2e -- guide.live.spec.ts tour.live.spec.ts
npm run test:e2e -- --config playwright.offline.config.ts
npm run test:e2e -- --config playwright.maps.config.ts
Remove-Item Env:LIVE_GUIDE_API
```

Nếu chưa cài Chromium của Playwright nhưng máy đã có Google Chrome, đặt
`$env:PLAYWRIGHT_CHANNEL='chrome'` trước khi chạy cấu hình `playwright.maps.config.ts`.
Test này dùng GPS, API và tile giả lập; không đọc vị trí thật của thiết bị.

Guide/playback UI tests giả lập API hoặc media adapter, không chứng minh DB/voice thật.
Guide/tour live tests dùng backend/PostgreSQL thật. Offline live dùng production preview
cổng 4175 và gói thật; ca lỗi chủ động sửa response để kiểm tra checksum. Map tests kiểm
tra marker, popup, giới hạn khu vực và URL OsmAnd; chúng có thể giả lập tile để không phụ
thuộc mạng bên ngoài.

Triển khai cần reverse proxy /api và /demo-audio, SPA fallback index.html, HTTPS và tile
provider phù hợp lưu lượng. Chưa có bằng chứng CI/CD, thiết bị thật hoặc đủ sáu voice TTS.
