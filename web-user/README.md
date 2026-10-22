# TravelVoice · Web du khách

React + Vite + TypeScript + Tailwind; cùng locationId, guide và gói nội dung với Android.
Hợp đồng guide và package: [API backend, mục 3.1 và 3.3](../docs/vi/BACKEND_ANDROID_CODE_REVIEW.md#api).

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

## Google Maps và vùng Quận 1

Sao chép .env.example thành .env.local, tự điền VITE_GOOGLE_MAPS_API_KEY; khởi động
lại Vite. Bật Maps JavaScript API và billing theo cấu hình Google Cloud của nhóm.
Giới hạn key theo website/referrer localhost và domain triển khai, chỉ cho API cần dùng.
Không commit key. Có thể đặt VITE_GOOGLE_MAPS_MAP_ID riêng; mặc định DEMO_MAP_ID.

Bản đồ tải SDK chính thức khi chọn Bản đồ, khóa camera quanh Quận 1 bằng strictBounds
và zoom 14–19. Không fit toàn bộ fixture để kéo sang quận khác. Chỉ ghim Dinh Độc Lập,
Nhà hát Thành phố, Nhà thờ Đức Bà, Bitexco; ba điểm lân cận vẫn đọc/nghe trong danh sách.
Vùng giới hạn là hình chữ nhật demo, **không phải polygon ranh giới hành chính**;
phần nền giáp ranh có thể xuất hiện. Chưa có dữ liệu đường biên được kiểm chứng để cắt sát.
Không tải tile Google vào gói offline. Thiếu key/lỗi SDK vẫn dùng được danh sách.
Android cũng dùng Google Maps SDK, cùng bốn ID và vùng camera; key Android cấu hình
riêng theo package/SHA-1 trong android/README.md. Sau cập nhật restriction, lần kiểm tra
SDK Web thật vẫn nhận lỗi xác thực từ Google; nhóm đang kiểm tra API/billing.
Danh sách và đọc bài vẫn hoạt động khi bản đồ lỗi.

Nguồn cấu hình: https://developers.google.com/maps/documentation/javascript/examples/control-bounds-restriction
và https://developers.google.com/maps/documentation/javascript/get-api-key.

## Kiểm tra

```powershell
npm run lint
npm run build
npm run test:e2e
$env:LIVE_GUIDE_API='1'
npm run test:e2e -- guide.live.spec.ts tour.live.spec.ts
npm run test:e2e -- --config playwright.offline.config.ts
npm run test:e2e -- --config playwright.maps.config.ts
$env:LIVE_GOOGLE_MAPS='1'
npm run test:e2e -- --config playwright.google-live.config.ts
Remove-Item Env:LIVE_GOOGLE_MAPS
Remove-Item Env:LIVE_GUIDE_API
```

Guide/playback UI tests giả lập API hoặc media adapter, không chứng minh DB/voice thật.
Guide/tour live tests dùng backend/PostgreSQL thật. Offline live dùng production preview
cổng 4175 và gói thật; ca lỗi chủ động sửa response để kiểm tra checksum. Google adapter
config dùng key giả riêng và SDK giả lập để kiểm tra giới hạn/lỗi, **không chứng minh
Google tiles hoặc credentials thật**. Kết quả đã chạy và giới hạn ghi trong SESSION_REPORT.

Triển khai cần reverse proxy /api và /demo-audio, SPA fallback index.html, HTTPS và
key Google giới hạn đúng domain. Chưa có bằng chứng CI/CD, thiết bị thật hoặc đủ sáu voice TTS.
