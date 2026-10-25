# Android Kotlin — client chung và offline

App Kotlin/Compose dùng cùng locationId/contentId, public API và schema gói với Web user. Hợp đồng: [API_list.md](API_list.md); vận hành: [DOCKER_GUIDE.md](DOCKER_GUIDE.md).

## Entry point và các phần chính

| File | Trách nhiệm |
|---|---|
| [MainActivity.kt](../../android/app/src/main/java/vn/hvp/travelvoice/MainActivity.kt) | Activity, locale và theme. |
| [TravelQ1App.kt](../../android/app/src/main/java/vn/hvp/travelvoice/ui/navigation/TravelQ1App.kt) | Bản đồ/danh sách, chi tiết, Offline, cài đặt và Profile; giữ snapshot gói và vòng polling. |
| [SharedTourScreen.kt](../../android/app/src/main/java/vn/hvp/travelvoice/ui/screens/SharedTourScreen.kt) | SharedOfflineContent: xác nhận tải/xóa, tiến độ và mở địa điểm đã cài. Prototype SharedTourScreen không phải entry point hiện tại. |
| [PackageUpdates.kt](../../android/app/src/main/java/vn/hvp/travelvoice/ui/screens/PackageUpdates.kt) | Snapshot dùng chung cho polling, banner và kiểm tra thủ công; so sánh version/scope đã cài. |
| [TravelProfile.kt](../../android/app/src/main/java/vn/hvp/travelvoice/ui/screens/TravelProfile.kt) | Hồ sơ khách, lưu URL máy chủ hợp lệ, đồng bộ lượt nghe và mở Offline. |
| [DemoApi.kt](../../android/app/src/main/java/vn/hvp/travelvoice/data/remote/DemoApi.kt) | Đọc public API, tên đa ngôn ngữ và gửi receipt. |
| [SharedTourRepository.kt](../../android/app/src/main/java/vn/hvp/travelvoice/data/remote/SharedTourRepository.kt) | Catalog/guide online và fallback gói có chữ ký, tải/verify/cài AtomicFile. |
| [TouristIdentity.kt](../../android/app/src/main/java/vn/hvp/travelvoice/data/remote/TouristIdentity.kt) | UUID lượt cài đặt, heartbeat ANDROID; bộ lưu mã hóa cũ bảo vệ outbox. Không lấy email/hardware ID. |
| [PlaybackOutbox.kt](../../android/app/src/main/java/vn/hvp/travelvoice/data/remote/PlaybackOutbox.kt) | Receipt eventId/contentId/occurredAt/offline, ID khách, lưu trước khi gửi và retry cùng UUID. |
| [SharedNarrator.kt](../../android/app/src/main/java/vn/hvp/travelvoice/platform/audio/SharedNarrator.kt) | MediaPlayer ưu tiên, TTS nguyên văn dự phòng; focus, seek/rate và callback nghe thật. |
| [TravelPlaybackService.kt](../../android/app/src/main/java/vn/hvp/travelvoice/platform/audio/TravelPlaybackService.kt) | Sở hữu player khi đổi màn và điều khiển media khi nghe nền. |
| [PackagePlaybackGate.kt](../../android/app/src/main/java/vn/hvp/travelvoice/platform/audio/PackagePlaybackGate.kt) | Chờ preparing/playing/paused kết thúc hoặc dừng trước commit; chặn phát mới khi cài. |

DemoApp và các màn hình prototype cũ vẫn còn trong repo, không phải bằng chứng luồng release. REVIEWER/EDITOR thao tác trên Web Admin, không nằm trên APK.

## Nội dung và cập nhật gói

Tên POI lấy names theo ngôn ngữ đang chọn → vi → name. Guide giữ FULL/SHORT qua fallback Selected → EN → VI; audio phải khớp contentId. Mở POI/nhận banner không tự phát. Bản đồ nền cần mạng và Google Maps key hợp lệ; danh sách vẫn dùng được khi Maps lỗi. GPS/quyền vị trí và khảo sát geofence cần nghiệm thu riêng trên thiết bị.

TravelQ1App dùng repeatOnLifecycle(STARTED): kiểm manifest lúc mở/resume và mỗi 30 giây. ConnectivityManager kích hoạt kiểm tra khi có mạng và được gỡ khi dispose. Android hiện lấy toàn bộ manifest, chưa dùng ETag như Web. Manifest APPROVED của phiên bản mới chỉ có sau khi reviewer xuất bản và job READY; banner dẫn tới Offline, không tự tải.

Nút kiểm tra thủ công và polling cập nhật cùng PackageUpdates, không khóa ở snapshot cũ. Người dùng xác nhận rồi mới tải JSON/key/audio. Lỗi truyền tải được thử lại tối đa 3 lần; checksum/chữ ký hỏng không được bỏ qua. Sau verify, PackagePlaybackGate chờ dừng/kết thúc bài nghe trước khi thay gói. Kho gói có khóa đọc/ghi dùng chung để polling không đọc đúng lúc AtomicFile đang commit. AtomicFile giữ bản cũ khi lỗi hoặc hủy; BASE được bảo vệ, OPTIONAL có thể xóa. Sau cài catalog/guide cache làm mới, có danh sách mở địa điểm trong gói.

Đây là banner foreground, không có push khi app đóng. TTS offline phụ thuộc giọng đã cài của thiết bị. Ảnh POI mới chưa được đưa vào byte tài nguyên offline và Android chưa bổ sung UI ảnh upload trong lần sửa này.

## Profile, khách vãng lai và lượt nghe

Profile dùng chế độ khách như Web user, không gọi API OTP không có trong backend hiện hành. UUID ngẫu nhiên được giữ cho lượt cài đặt. Heartbeat ANDROID mỗi 60 giây khi foreground hoặc đang nghe qua media service ở nền; backend lưu first_seen_at/last_seen_at. Admin xem tổng lượt cài đặt đã mở và số hoạt động trong 5 phút, tách Android/Web.

Xóa dữ liệu/cài lại có thể tạo ID mới; không coi đây là số người hoặc máy vật lý duy nhất. Tải APK nhưng chưa mở app chưa được tính. Web dùng một ID theo hồ sơ trình duyệt, nhiều tab tính cùng ID; không lấy email, vị trí hay mã phần cứng cho heartbeat.

SharedNarrator chỉ tạo sự kiện khi âm thanh thực sự tiến triển. Pause/resume trong cùng phiên không tạo UUID mới. Outbox giữ thời điểm/offline và contentId revision đã nghe, gửi lại khi mở/resume và mỗi 60 giây. Backend chống đếm lặp theo eventId. Profile có nút đồng bộ thủ công; mất mạng không chặn đọc/nghe nội dung đã có.

## Build, APK và QR

[build.gradle.kts](../../android/app/build.gradle.kts): version 1.1.0, debug suffix -dev, package .debug. Dependency giữ theo repo. Google key đặt trong android/secrets.properties hoặc ANDROID_MAPS_API_KEY, không commit. Build trực tiếp mặc định emulator 10.0.2.2:8081.

Từ gốc repo, với Java 21 và Android SDK sẵn sàng:

```powershell
.\scripts\prepare-android-download.ps1 -ApiBaseUrl https://your-tunnel.trycloudflare.com/
docker compose --env-file .env.demo.local up -d --build web-user
```

Script tự đọc URL Cloudflare từ runtime local nếu không truyền ApiBaseUrl; không có URL thì fallback emulator. APK có thể đổi URL trong Profile khi tunnel thay đổi. Không dùng localhost của PC trên điện thoại. Script chép APK/version/size/hash vào web-user/public/downloads (gitignore), thêm query hash vào URL APK; QR và nút tải Web cùng lấy URL này theo origin hiện tại.

Unit/lint/build và nghiệm thu emulator/TalkBack/thiết bị thật là các bằng chứng riêng. Không dùng số test cũ trước lần nhập Android từ main để khẳng định bản 1.1.0 đã được kiểm; xem báo cáo thực thi của lần thay đổi hiện tại.

Host UI tests dùng OfflineTestApplication với máy chủ loopback không hoạt động để không gửi heartbeat vào demo. Số liệu demo có thể bao gồm các lượt cài đặt kiểm thử trước đó; không diễn giải là số khách thực tế duy nhất. Heartbeat cần mạng, nên thiết bị đang offline không thể cập nhật số hoạt động tức thời.
