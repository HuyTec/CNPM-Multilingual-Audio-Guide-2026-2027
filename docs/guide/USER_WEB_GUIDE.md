# Web du khách — nội dung, offline và ứng dụng Android

Hợp đồng API/JSON: [API_list.md](API_list.md). Gói/DB: [BACKEND_GUIDE.md](BACKEND_GUIDE.md). Website không yêu cầu đăng nhập du khách.

## Cấu trúc và trang

| File | Vai trò |
|---|---|
| [main.tsx](../../web-user/src/main.tsx) | BrowserRouter: khám phá /, chi tiết /locations/:locationId, /offline; shell, polling notice, geofence, playback sync. |
| [LocationEntry.tsx](../../web-user/src/pages/LocationEntry.tsx), [LocationCatalog.tsx](../../web-user/src/components/LocationCatalog.tsx) | Khám phá/tìm tên, chọn ngôn ngữ/loại, danh sách/bản đồ, mở link POI hoặc QR địa điểm. |
| [useLocationCatalog.ts](../../web-user/src/lib/useLocationCatalog.ts), [TourMap.tsx](../../web-user/src/components/TourMap.tsx) | Nạp toàn bộ trang API theo batch 50, chọn tên theo ngôn ngữ → vi → name; map và list dùng cùng dữ liệu. Lỗi mạng mới fallback gói đã xác minh; 4xx không phục hồi nội dung lỗi từ cache. |
| [GuidePage.tsx](../../web-user/src/pages/GuidePage.tsx), [useGuide.ts](../../web-user/src/lib/useGuide.ts) | Guide theo locationId/language/FULL/SHORT, giữ fallback đúng loại; báo thiếu/lỗi; fallback offline có kiểm chứng. |
| [GuidePlayer.tsx](../../web-user/src/components/GuidePlayer.tsx), [playback.ts](../../web-user/src/lib/playback.ts) | Audio đúng bài trước, thiếu/lỗi mới TTS nguyên văn và đúng ngôn ngữ; pause/resume/stop/seek/rate; chỉ người dùng bấm mới phát. |
| [playbackOutbox.ts](../../web-user/src/lib/playbackOutbox.ts), [usePlaybackReceipt.ts](../../web-user/src/lib/usePlaybackReceipt.ts) | Sau callback phát thật tạo eventId/contentId, lưu outbox IndexedDB, gửi lại; refresh 15s là đồng bộ lượt nghe, không phải polling gói. |
| [OfflinePage.tsx](../../web-user/src/pages/OfflinePage.tsx), [packages.ts](../../web-user/src/lib/packages.ts) | Kiểm tra/tải/xóa OPTIONAL có xác nhận; verify JSON/audio, staging trong bộ nhớ, commit IndexedDB nguyên tử. BASE không được xóa bằng ứng dụng. |
| [packageUpdates.ts](../../web-user/src/lib/packageUpdates.ts), [PackageUpdateNotice.tsx](../../web-user/src/components/PackageUpdateNotice.tsx) | Manifest polling khi mount, online, visibility và mỗi 30s khi tab hiện/có mạng; If-None-Match, 304; notice có dung lượng và đường tới trang tải. |
| [playbackGate.ts](../../web-user/src/lib/playbackGate.ts) | Giữ owner khi preparing/playing/paused; chờ đến stop/ended/unmount trước transaction thay gói. Cho hủy trong lúc chờ. |
| [useGeofencing.ts](../../web-user/src/lib/useGeofencing.ts), [GeofenceNotices.tsx](../../web-user/src/components/GeofenceNotices.tsx) | Tự bật theo ý người dùng, dữ liệu khảo sát/cooldown trong gói; không tự phát audio khi nhận banner. |
| [AndroidAppPromo.tsx](../../web-user/src/components/AndroidAppPromo.tsx) | Khối giới thiệu Android dưới khám phá: loading/error/retry/ready; nút APK + QR cùng URL, version/dung lượng; tạo QR tại client, không gọi dịch vụ QR bên ngoài. |
| [public-server.mjs](../../web-user/scripts/public-server.mjs) | Gateway static/public API allowlist; không forward API quản trị; phục vụ file APK từ dist sau build. |

## Offline và cập nhật

Ảnh POI mới lấy từ `imageUrl` của API địa điểm sau khi reviewer xuất bản; dùng trong danh sách, bản đồ và trang bài đọc qua [PublishedLocationPhoto.tsx](../../web-user/src/components/PublishedLocationPhoto.tsx). Gateway chỉ mở route ảnh UUID `.png`, không mở ảnh nháp hay API Admin. **Giới hạn:** ảnh tự tải lên hiện phục vụ online, chưa có byte ảnh trong manifest tài nguyên offline; khi dùng danh sách offline, bỏ URL ảnh mạng để tránh ảnh hỏng. Ảnh tham chiếu đóng sẵn của các POI cũ vẫn có trong app shell. Android chưa bổ sung UI hiển thị ảnh mới.

Polling chỉ lấy manifest; phát hiện version khác mới hiện thông báo trong ứng dụng, không ép tải. Download lấy public key, raw JSON và audio; kiểm size, SHA-256, keyId và RSA trên đúng bytes. Verify schema/APPROVED/scope/location/guide/audio liên kết trước commit. IndexedDB travelvoice-content-v1 lưu manifest, raw bytes, key, audio và installedAt; đọc lại vẫn verify. Thiếu dung lượng, bị hủy hoặc dữ liệu hỏng không ghi đè gói cũ. Cài xong phát travelvoice-content-changed để cập nhật notice/geofence. Web cần online lần đầu để chuẩn bị app shell/service worker và dữ liệu; TTS offline tùy voice máy. Không có push khi đóng tab; không bảo đảm polling nền khi browser bị treo.

Lượt nghe dùng contentId của revision đã phát và timestamp/offline. Server giữ revision cũ; thay tên POI không làm mất thống kê locationId. Chỉ mở trang/đọc script chưa tính lượt nghe. Tên trong gói có names; guide.locationName lấy tên phù hợp effective language. Bản đồ nền cần mạng; POI/geofence chỉ đáng tin khi đã khảo sát.

## APK và QR Android

Chạy [prepare-android-download.ps1](../../scripts/prepare-android-download.ps1) từ gốc repo để build Android và chép APK + metadata vào web-user/public/downloads (output được gitignore). Không commit APK/cache. Dùng -SkipBuild chỉ khi đã có APK mới trong output .verification-build. Script lấy versionName từ output-metadata.json, tính size/SHA-256. Sau đó mới build Web để dist có tệp tải.

Component đọc /downloads/android-release.json. Mặc định release.url là /downloads/travelvoice.apk, QR được tạo thành URL tuyệt đối theo origin đang mở; VITE_ANDROID_APK_URL có thể override khi đưa APK lên hosting riêng. Chỉ nhận http/https không có userinfo. Khi chưa chuẩn bị output, hiển thị chưa sẵn sàng + retry, không hiện QR giả. Mã không chứa credential. Điện thoại cần URL website HTTPS công khai hoặc địa chỉ LAN truy cập được; QR localhost trên PC không giúp điện thoại truy cập PC. Đây là APK debug demo, chưa phải phát hành Play Store hay APK production.

## Chạy và kiểm thử

```powershell
# Từ gốc repo; JAVA_HOME/Android SDK phải sẵn sàng
.\scripts\prepare-android-download.ps1
cd web-user
npm.cmd ci
# .env.local: API_PROXY_TARGET=http://127.0.0.1:8081
npm.cmd run dev
npm.cmd run build
npm.cmd run lint
npm.cmd run test:gateway
```

Vite dev mặc định 5175; preview 4175. npm run serve:public phục vụ dist và proxy public API; xem cấu hình trong script trước khi đổi cổng/backend. Không dùng việc dev-server mở được để kết luận service worker offline production đã sẵn sàng. Kiểm thử live chung với Web Admin được dẫn ở [ADMIN_GUIDE.md](ADMIN_GUIDE.md). Tài liệu này không xác nhận đã nghe audio/scan QR trên điện thoại thật trong lần sửa này.

## Chia sẻ cổng Docker ra internet

Đã thử ngày 25/10/2026 trên cùng cổng Web Docker: ngrok 3.39.13 dừng ERR_NGROK_4018 vì chưa có tài khoản/authtoken; LocalTunnel cấp URL nhưng Web/API trả 408; Cloudflare 2026.10.3 dùng HTTP/2 trả 200 cho Web, API và metadata APK, không cần tài khoản/token. Vì vậy chọn Cloudflare Quick Tunnel cho demo trên máy này. Đây là quan sát lần thử, không phải kết luận LocalTunnel luôn lỗi ở mọi mạng. Tham khảo [ngrok](https://ngrok.com/docs/start), [LocalTunnel](https://github.com/localtunnel/localtunnel), [Cloudflare Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

```powershell
# Từ gốc repo, đã chuẩn bị APK như phần trên
.\scripts\start-live-demo.ps1 -TouristWeb
.\scripts\share-docker-web.ps1
# Dừng đúng tunnel script tạo; không xóa database/container
.\scripts\share-docker-web.ps1 -Stop
```

[share-docker-web.ps1](../../scripts/share-docker-web.ps1) tải cloudflared chính thức đã ghim phiên bản/SHA-256 nếu thiếu, chạy tiến trình ẩn và lưu PID/URL/log trong .local-tools. Dùng -Port để trỏ cổng Docker khác; -Restart tạo tunnel mới và có thể đổi URL. Giữ máy, Docker và tunnel hoạt động. URL ngẫu nhiên là tạm thời, không có cam kết uptime. QR Android lấy origin của URL đang mở nên không cần sửa mã QR bằng tay khi đổi tunnel.

Lần kiểm thử hiện hành dùng mạng Docker riêng và cổng 14176, backend/DB demo riêng; không thay stack cũ cổng 8080/3000 hoặc 8081/3001. Khi dùng preview này, gọi script với -Port 14176. Gateway chỉ phục vụ public routes cùng hai file downloads được allowlist; Admin/Actuator không được đưa ra internet. Chuẩn bị lại APK trước Docker build, nếu không khối Android báo chưa sẵn sàng.

Trên điện thoại thật, sau khi cài APK demo, mở **Kết nối máy chủ** trong ứng dụng và dùng chính URL HTTPS của Web làm địa chỉ đồng bộ. Không dùng 10.0.2.2 (chỉ dành cho emulator). Gateway public hỗ trợ các API du khách mà Android dùng; không cung cấp quyền quản trị.
