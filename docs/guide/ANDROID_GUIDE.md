# Android Kotlin — client chung và offline

Hợp đồng API: [API_list.md](API_list.md); backend/schema: [BACKEND_GUIDE.md](BACKEND_GUIDE.md). App Kotlin/Jetpack Compose dùng cùng locationId/contentId và schema gói với Web.

## Cấu trúc thực tế

| File | Trách nhiệm |
|---|---|
| [MainActivity.kt](../../android/app/src/main/java/vn/hvp/travelvoice/MainActivity.kt) | Activity entry. |
| [DemoApp.kt](../../android/app/src/main/java/vn/hvp/travelvoice/ui/navigation/DemoApp.kt) | Navigation hiện có; không viết lại app. |
| [SharedTourScreen.kt](../../android/app/src/main/java/vn/hvp/travelvoice/ui/screens/SharedTourScreen.kt) | Client chung: danh sách/chi tiết, ngôn ngữ FULL/SHORT, map, player và offline. REVIEWER/EDITOR không nằm trên APK. |
| [DemoApi.kt](../../android/app/src/main/java/vn/hvp/travelvoice/data/remote/DemoApi.kt) | HTTP public, parse ServerLocation/ServerGuide, tên đa ngôn ngữ, normalized base URL và playback. |
| [SharedTourRepository.kt](../../android/app/src/main/java/vn/hvp/travelvoice/data/remote/SharedTourRepository.kt) | Server-first locations/guide; verified offline fallback; available dùng ETag; download kiểm chữ ký/hash/audio và AtomicFile. |
| [PackageUpdateNotice.kt](../../android/app/src/main/java/vn/hvp/travelvoice/ui/screens/PackageUpdateNotice.kt) | Poll ở Lifecycle.STARTED, lúc mở/resume/có mạng và 30s; thông báo scope/dung lượng; confirm tải; không push khi app đóng. |
| [SharedNarrator.kt](../../android/app/src/main/java/vn/hvp/travelvoice/platform/audio/SharedNarrator.kt) | MediaPlayer ưu tiên, TTS nguyên văn dự phòng; audio focus, lifecycle pause, seek/rate; callback nghe thật. |
| [PackagePlaybackGate.kt](../../android/app/src/main/java/vn/hvp/travelvoice/platform/audio/PackagePlaybackGate.kt) | Chờ owner preparing/playing/paused hoàn tất trước AtomicFile commit; owner được bỏ khi close. |
| [PlaybackOutbox.kt](../../android/app/src/main/java/vn/hvp/travelvoice/data/remote/PlaybackOutbox.kt) | Lưu/gửi lại receipt eventId/contentId; contentId giữ revision nghe, không suy ra từ tên mới. |
| [MapPreview.kt](../../android/app/src/main/java/vn/hvp/travelvoice/ui/components/MapPreview.kt), [MapSession.kt](../../android/app/src/main/java/vn/hvp/travelvoice/platform/maps/MapSession.kt) | Google Maps adapter, marker và lifecycle; cần key/package/signature hợp lệ. |
| [platform](../../android/app/src/main/java/vn/hvp/travelvoice/platform) | Adapter GPS/geofence/notification/audio của nền tảng. |

Tên lớp DemoApi/DemoApp còn từ lịch sử, không chứng minh dữ liệu là mock. Các màn hình ExploreScreen, DetailScreen, PlayerScreen, OfflineScreen và LiveDemoScreen cũ vẫn tồn tại; không xem mọi màn hình đó là luồng chung đã nối DB. SharedTourScreen là màn hình được cập nhật trong phạm vi này.

## Nội dung, GPS và audio

Catalog lấy names theo language → vi → name cho list/map. Guide giữ cùng FULL/SHORT qua fallback Selected → EN → VI, audio phải khớp contentId. Không dịch ở phía phát. GPS/quyền vị trí và geofence là adapter riêng; gói chỉ có metadata geofence khi đủ khảo sát/cooldown. Nhập tọa độ POI chưa phải bằng chứng đã khảo sát hoặc đã kiểm GPS trên thiết bị. Bản đồ nền cần mạng và Google Maps key hợp lệ.

Player chỉ bắt đầu do thao tác người dùng; mở POI hoặc nhận thông báo không tự phát. Thiếu audio mới thử giọng TTS đúng ngôn ngữ, không tự thay nội dung. Receipt chỉ tạo khi phát bắt đầu thật; server lưu content_id/location_id. Historical revision APPROVED vẫn được nhận cho lượt nghe offline sau khi POI có bản mới.

## Gói và polling

Repository nhớ ETag/manifest trong phiên; 304 dùng manifest trước. PackageUpdateNotice đăng ký ConnectivityManager callback rồi gỡ khi dispose; repeatOnLifecycle ngừng vòng polling khi background. Nút kiểm tra thủ công vẫn còn. Tải và xác minh trong Dispatchers.IO, progress về Main; client giữ gói cũ khi download/cancel/error. Gói gồm raw JSON/key/manifest và audio lưu Base64 trong AtomicFile theo scope. Android có BASE bundled; OPTIONAL có thể xóa, BASE được bảo vệ. Sau khi tải xong phải đợi player kết thúc/stop/close rồi mới finishWrite. Polling không tự ép tải và không bảo đảm thông báo khi app đã đóng.

## Build, chạy, APK

[build.gradle.kts](../../android/app/build.gradle.kts) ghim dependency theo repo; applicationId demo và version đọc từ file này. Google key ở android/secrets.properties hoặc ANDROID_MAPS_API_KEY, không commit. Emulator dùng 10.0.2.2 trỏ máy host; chọn cổng 8081 nếu backend live-demo hoặc 8080 nếu Compose mặc định. Điện thoại thật phải dùng IP LAN/HTTPS truy cập được, không dùng localhost của PC.

```powershell
cd android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug '-PdemoBuildDir=.verification-build'
# Từ gốc repo, chuẩn bị APK lên Web User sau build
cd ..
.\scripts\prepare-android-download.ps1 -SkipBuild
```

Output .verification-build/app chứa APK debug, test-results và reports; không commit binary. Script Web chép APK hiện có, version/size/hash vào output downloads; cần build lại Web để phát hành output. Trong lần sửa này assembleDebug/testDebugUnitTest/lintDebug PASS: 11 unit tests, lint 0 lỗi, 13 warnings và 2 hints. Chưa thao tác polling, GPS, audio, scan QR/cài APK trên thiết bị thật. Không dùng kết quả compiler/host tests để thay nghiệm thu thiết bị.
