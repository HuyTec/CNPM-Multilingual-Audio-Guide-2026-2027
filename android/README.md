# TravelQ1 — Android 1.0.0

Ứng dụng khám phá địa điểm và nghe thuyết minh bằng Kotlin/Jetpack Compose. Package phát hành: `vn.hvp.travelvoice`; Android 8.0+ (minSdk 26). Bản phát hành đầu tiên dùng backend local theo cấu hình của dự án.

## Sử dụng

- Bản đồ là màn chính; tìm kiếm, danh sách, Home và nút về vị trí hiện tại. Chỉ xin quyền vị trí khi người dùng chọn chức năng cần vị trí.
- Chi tiết có ngôn ngữ thuyết minh và FULL/SHORT. Audio thu sẵn được ưu tiên; TTS đọc đúng nội dung đã duyệt, tạo WAV để tua theo thời gian thực.
- Hồ sơ là tùy chọn: khách dùng ngay, đăng nhập email qua OTP Gmail khi máy chủ được cấu hình. Không cần đăng nhập để đọc/nghe.
- Ngôn ngữ giao diện Tiếng Việt/English độc lập với 6 ngôn ngữ thuyết minh. Có sáng/tối/theo hệ thống, lưu lựa chọn trên thiết bị.
- Gói BASE có 7 địa điểm thật và 84 bài; dữ liệu công khai và gói offline cùng hợp đồng backend. Các điểm hư cấu cũ được thu hồi bằng ID có chữ ký.
- Cache guide 60 giây/32 mục và TTS tối đa 50 MB; gói offline được xác minh, cài nguyên tử và quản lý riêng. Mini-player tiếp tục khi đổi màn. Thông báo điều khiển xuất hiện khi nghe ở nền và được gỡ khi trở lại app.

## Backend local

Khởi động backend/PostGIS theo [hướng dẫn backend](../backend/README.md). Giữ backend tại `127.0.0.1:8080`; Android Emulator truy cập qua `http://10.0.2.2:8080/`. Điện thoại thật cần backend trong cùng mạng LAN và build với địa chỉ phù hợp; APK phát hành lần này dành cho cấu hình emulator nêu trên.

Cấu hình Gmail chỉ ở máy chủ theo [Tourist identity và OTP](../backend/TOURIST_AUTH_API.md). Khi chưa cấu hình tài khoản gửi, OTP trả thông báo chưa sẵn sàng và người dùng vẫn dùng được với tư cách khách. Chưa có bằng chứng giao thư Gmail thật trong lượt kiểm tra này.

## Google Maps

Đặt `MAPS_API_KEY` trong `secrets.properties` hoặc biến môi trường `ANDROID_MAPS_API_KEY`; không commit tệp bí mật. Bật Maps SDK for Android và giới hạn key theo package/chứng chỉ ký tương ứng:

- Release package: `vn.hvp.travelvoice`.
- Release SHA-1: `65:24:78:F2:96:AD:A0:8C:59:AD:F7:D9:31:3B:3C:69:6A:4D:20:D1`.
- Debug package: `vn.hvp.travelvoice.debug`, dùng chứng chỉ debug của máy phát triển.

Bản đồ nền cần mạng; không đưa Google tiles vào gói offline. Khi bản đồ không tải được, danh sách và bài đọc vẫn sử dụng được.

## Build và kiểm tra

JDK 21, Android SDK 36, Gradle Wrapper 8.13. AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.10.01. Trong PowerShell tại thư mục `android/`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=E:/Project/CongNghePhanMem_2026-2027/android/.java-tmp'
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest '-PbuildOutputDir=.verification-build' '-Pkotlin.incremental=false' '-Pkotlin.compiler.execution.strategy=in-process' --no-daemon --no-configuration-cache
```

Debug APK: `.verification-build/app/outputs/apk/debug/app-debug.apk`. Debug có package riêng và không ghi đè bản release.

## Ký và build release

Khóa ký của bản 1.0.0 nằm trong `.signing/` được Git ignore. **Sao lưu an toàn cả keystore và `release.properties` để ký các bản cập nhật sau; không đưa chúng lên GitHub hoặc đính kèm release.** Cấu hình file properties:

```properties
storeFile=.signing/travelq1-release.p12
storePassword=<local secret>
keyAlias=travelq1
keyPassword=<local secret>
```

Build bản dùng backend local đã được người dùng chọn:

```powershell
.\gradlew.bat :app:assembleRelease :app:lintRelease '-PbuildOutputDir=.verification-build' '-PtravelApiBaseUrl=http://10.0.2.2:8080/' '-PallowLocalBackend=true' '-Pkotlin.incremental=false' '-Pkotlin.compiler.execution.strategy=in-process' --no-daemon --no-configuration-cache
```

Output: `.verification-build/app/outputs/apk/release/app-release.apk`. Release không debuggable, dùng R8/resource shrinking và khóa ký riêng. HTTP chỉ được cho phép với các host loopback/emulator trong cấu hình release này; các host khác cần HTTPS. Khi chuyển sang server online, build lại với `-PtravelApiBaseUrl=https://your-api-origin/` và bỏ cờ local.

Hồ sơ thiết kế và bằng chứng kiểm tra: [HVP-22](https://trannhuthuy897.atlassian.net/browse/HVP-22). Prototype cũ chỉ còn là nguồn kiểm thử lịch sử, không phải entry point của ứng dụng phát hành. PRD do người dùng cập nhật riêng.
