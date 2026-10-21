# TravelVoice · Web du khách

React + Vite + TypeScript + Tailwind; cùng locationId, guide và gói nội dung với Android.
Hợp đồng: [GUIDE_API.md](../backend/GUIDE_API.md) và
[CONTENT_PACKAGE_API.md](../backend/CONTENT_PACKAGE_API.md).

```powershell
cd web-user
npm ci
npm run dev
```

Mở http://127.0.0.1:5175/ để chọn địa điểm. Backend mặc định cổng 8080,
profile demo, DB đã áp dụng V3_3. API_PROXY_TARGET đổi được trong .env.local.
Danh sách mặc định là bảy điểm từ tài liệu nhóm cung cấp; vẫn có lựa chọn tất cả
địa điểm cũ. Nội dung chưa thẩm định; tọa độ chỉ minh họa.

- URL /locations/:locationId?language=VI&scriptType=FULL giữ lựa chọn khi reload/back.
  Sáu ngôn ngữ VI/EN/FR/JA/ZH/RU; server quyết định APPROVED và fallback Selected → EN → VI,
  giữ FULL/SHORT. 404 không dùng bài offline cũ để thay nội dung đã ngừng công khai.
- Audio đúng bài được ưu tiên; lỗi/thiếu audio dùng TTS đúng effective language và
  nguyên văn scriptText. Chỉ phát khi bấm, có pause/resume, seek và tốc độ. TTS cần
  voice tương ứng trên trình duyệt/thiết bị; voice mạng không bảo đảm hoạt động offline.
  Lượt nghe chỉ gửi khi media/TTS báo bắt đầu; Web hiện chưa có outbox gửi lại khi mất mạng.
- /offline: kiểm tra → xác nhận tải → xác minh chữ ký/checksum JSON và từng audio →
  cài đặt nguyên tử. BASE được bảo vệ; OPTIONAL có thể xóa sau xác nhận. Bản cũ giữ
  nguyên khi lỗi/hủy. Chỉ thông báo sẵn sàng offline khi gói đã được xác minh.
- Mở lại ứng dụng offline cần production build/preview và service worker đã cài
  trong lần online trước. Dev server không cung cấp app shell offline. HTTPS cần
  cho triển khai ngoài localhost; chưa deploy.

## OpenStreetMap, OsmAnd và vùng Quận 1

Web User hiển thị nền OpenStreetMap trong ứng dụng, không cần Google Maps API key.
Chọn **Bản đồ** để xem tuyến Quận 1; bấm một ghim để mở bài thuyết minh hoặc mở tọa độ
đó trong OsmAnd. Liên kết OsmAnd dùng URL pin tương thích với ứng dụng di động và bản đồ web.

Camera giới hạn trong vùng demo Quận 1, zoom 14–19. Chỉ ghim Dinh Độc Lập, Nhà hát Thành
phố, Nhà thờ Đức Bà và Bitexco; ba điểm lân cận vẫn mở được từ danh sách bên dưới. Khung
giới hạn là hình chữ nhật demo, **không phải ranh giới hành chính đã kiểm chứng**. Nền bản
đồ cần Internet; không tải tile vào gói offline. Bản đồ dùng tile OSM tiêu chuẩn và hiển thị
ghi công OpenStreetMap contributors. Khi triển khai có lưu lượng lớn, cần dùng nhà cung cấp
tile phù hợp hoặc tự host; không dùng tile server cộng đồng để tải trước hay dùng offline.

Android tiếp tục dùng Google Maps SDK với key riêng theo package/SHA-1 trong android/README.md.

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

Guide/playback UI tests giả lập API hoặc media adapter, không chứng minh DB/voice thật.
Guide/tour live tests dùng backend/PostgreSQL thật. Offline live dùng production preview
cổng 4175 và gói thật; ca lỗi chủ động sửa response để kiểm tra checksum. Map tests kiểm
tra marker, popup, giới hạn khu vực và URL OsmAnd; chúng có thể giả lập tile để không phụ
thuộc mạng bên ngoài.

Triển khai cần reverse proxy /api và /demo-audio, SPA fallback index.html, HTTPS và tile
provider phù hợp lưu lượng. Chưa có bằng chứng CI/CD, thiết bị thật hoặc đủ sáu voice TTS.
