# TravelQ1 — Android

## Bản xem trước UX ngày 21/10/2026

Màn mặc định mới: `MainActivity → DemoApp → TravelQ1App`. Bản đồ Google là nội dung chính; tìm kiếm mở từ biểu tượng, chọn ghim mở box giới thiệu và đọc tiếp trong sheet. Nút Home bên phải tên app xóa lựa chọn để tìm nơi khác. Nút định vị ở góc dưới phải phần bản đồ đưa camera về GPS và hiện ghim xanh/vùng sai số; thao tác này không tự bật bộ lọc Gần tôi. Mini-player giữ phiên nghe khi đổi màn; Ngoại tuyến nằm trong Cài đặt. Ngôn ngữ thuyết minh và tốc độ được lưu trên thiết bị.

Chỉ xin quyền vị trí sau khi bấm **Gần tôi** và xác nhận lời giải thích. Lấy vị trí một lần, có timeout và hủy khi ra nền; không có geofence trong mốc UX này. Khi đang nghe và chuyển app/nhấn Home, media foreground service hiển thị thông báo điều khiển; mở lại app gỡ thông báo. Force-stop không được xem là thao tác chuyển nền. TTS vẫn chuyển theo câu, chưa có timeline thời gian chính xác hoặc phụ đề timestamp.

Địa chỉ API không còn trong UI mặc định. Emulator mặc định dùng `10.0.2.2:8080`; build cho thiết bị LAN bằng `-PtravelApiBaseUrl=http://<IP-LAN>:8080/` (cấu hình build, không phải secret). Dữ liệu/ảnh published, API và PRD không đổi trong mốc UX. Android hiển thị placeholder ảnh; album trong form Web Admin là bản xem trước trong bộ nhớ, chưa có lưu ảnh lên server/gói offline.

Phần dưới ghi nhận luồng APK 0.2 trước khi thay shell UX; các màn prototype vẫn dùng riêng cho kiểm thử legacy.

**APK 0.2-shared-content (versionCode 2):** màn chính đọc danh sách và bài từ cùng backend/PostgreSQL với Web du khách và Web Admin. Bảy điểm demo, VI/EN/FR/JA/ZH/RU × FULL/SHORT; mặc định VI/FULL. `Làm mới nội dung` gọi lại máy chủ. Khi mạng lỗi, nguồn gói đã xác minh được ghi rõ; không dùng dữ liệu giả để báo kết nối thành công.

Audio ưu tiên tệp đúng bài, thiếu/lỗi mới dùng TTS đúng ngôn ngữ hiệu lực; không autoplay. BASE/OPTIONAL dùng cùng hợp đồng [gói nội dung](../backend/CONTENT_PACKAGE_API.md), xác nhận tải, checksum/chữ ký, cài nguyên tử và bảo vệ BASE. Lượt nghe gửi khi thực sự bắt đầu, có outbox. Voice TTS phụ thuộc thiết bị; chưa xác nhận đủ sáu voice thật. GPS/geofence tự động chưa triển khai.

Jetpack Compose / Material 3, `MainActivity` → `DemoApp` → `SharedTourScreen`. Bản biên tập chưa APPROVED trong Web Admin không thay thế nội dung công khai. Luồng prototype cũ chỉ còn cho kiểm thử legacy, không phải màn chính của APK mới.

## Kết nối và Google Maps

Emulator dùng `http://10.0.2.2:8080/`; điện thoại thật nhận địa chỉ LAN qua property build `travelApiBaseUrl` ở trên. Cần cài APK mới thay bản demo cũ.

Sao chép `secrets.properties.example` thành `secrets.properties`, đặt `MAPS_API_KEY` của Android (hoặc biến môi trường `ANDROID_MAPS_API_KEY`); không commit tệp. SDK Google Maps 20.1.0, bật Maps SDK for Android/billing. Restriction Android apps: package **vn.hvp.travelvoice.demo**, SHA-1 debug hiện tại **09:34:78:C0:96:63:FF:90:DD:B1:C0:E8:EB:C2:7C:39:17:C1:A4:42**. Máy/keystore khác hoặc bản release phải lấy fingerprint tương ứng. Key Web và key Android cấu hình riêng.

Map dùng cùng ID và khung Quận 1 như Web, zoom 14–19; ghim 101/102/103/107, các điểm lân cận vẫn có trong danh sách. Vùng camera là hình chữ nhật demo, chưa phải đường biên hành chính. Không tải Google tiles vào gói offline. Thiếu quyền/xác thực Google không làm mất danh sách/text.

## Chạy demo

Mở `android/` bằng Android Studio, Gradle Sync rồi chạy `app` trên thiết bị/emulator API 26+. Dùng JDK 17+, Android SDK 36 và Build Tools 35.0.0.

```powershell
cd E:\Project\CongNghePhanMem_2026-2027\android
# Sửa theo JDK/SDK thực tế; local.properties không commit.
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:ANDROID_HOME = 'E:\Project\CongNghePhanMem_2026-2027\android\.sdk'
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
# Workaround Windows nếu JDK báo Unable to establish loopback connection:
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=E:/Project/CongNghePhanMem_2026-2027/android/.java-tmp'
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest --no-daemon --no-configuration-cache
# Cần thiết bị/emulator kết nối cho instrumentation:
.\gradlew.bat :app:connectedDebugAndroidTest
```

Nếu Android Studio đang build cùng checkout, tách output xác minh và chạy toàn bộ kiểm tra trong một lượt:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug '-PdemoBuildDir=.verification-build' '-Pkotlin.incremental=false' '-Pkotlin.compiler.execution.strategy=in-process' --no-daemon --no-configuration-cache
```

Output của lượt này nằm trong `.verification-build/app/`, gồm `outputs/apk/debug/app-debug.apk`, `reports/tests/testDebugUnitTest/` và `outputs/ui-demo/*.png` (nếu host renderer khả dụng).

APK: `app/build/outputs/apk/debug/app-debug.apk`. Không commit APK/SDK/cache. Wrapper ghim Gradle 8.13 với SHA-256; AGP 8.13.2, Kotlin/Compose compiler 2.2.21, Compose BOM 2025.10.01, Activity 1.11.0, Navigation Compose 2.9.5. Đây là phiên bản ghim cho demo, không phải tuyên bố dùng mọi thư viện mới nhất. [AGP compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [Compose BOM](https://developer.android.com/develop/ui/compose/bom).

## Prototype lịch sử (không phải luồng chính của APK 0.2)

- Khám phá: tên/đường, danh mục, bán kính mô phỏng, bản đồ trực tuyến có pan/zoom, marker và bottom sheet. Bán kính UI cũ còn 50–500 m; PRD hiện hành là 100–500 m và GPS vẫn chưa tích hợp.
- Chọn địa điểm → VI/EN/JA và Đầy đủ/Tóm tắt → Nghe thuyết minh → player giả lập → Back giữ lựa chọn.
- Bộ chọn tình huống cuối Khám phá: thiếu quyền GPS vẫn tìm thủ công; banner mở chi tiết, không tự phát; cooldown/ID lỗi.
- Chi tiết: thử thiếu bản dịch EN → VI, đang tải/chưa có nội dung; phản hồi là dialog mẫu.
- Ngoại tuyến: kiểm tra → xác nhận → tải/kiểm tra giả → phiên bản mới. Thử thiếu dung lượng/mất mạng/gói lỗi trước khi cập nhật thành công. Xóa gói bổ sung cần xác nhận; nội dung cơ bản luôn được giữ.

Restart app để bắt đầu lại fixture. `rememberSaveable` giữ UI state nhỏ, không có lưu package/tệp/database. Khoảng cách và thuyết minh là mẫu, không phải dữ liệu du lịch đã xuất bản.

## Thiết kế và kiểm tra

Tham khảo ảnh người dùng tại `docs/ui_requirments/`: xanh biển chỉ dẫn, nền giấy sáng, chữ đậm; vector placeholder tạo tại chỗ. Không thêm tài khoản/hồ sơ ngoài PRD. Android dùng font sans hệ thống; Arial của web-admin giữ nguyên, không đóng gói tệp Arial có bản quyền từ Windows.

Mỗi màn có Compose Preview; Offline thêm dark/font lớn. UI tests tương tác trên Compose; host tests dùng Robolectric, không xác nhận dịch vụ thật. Thiết kế, yêu cầu demo, kiến trúc và biên bản review được lưu tại [Jira HVP-22](https://trannhuthuy897.atlassian.net/browse/HVP-22).

Ảnh render tham khảo được giữ tại [docs/ui-demo/previews](../docs/ui-demo/previews/). Backlog UC-01 hiện hành ở [Jira HVP-1](https://trannhuthuy897.atlassian.net/browse/HVP-1), các câu hỏi PRD ở [Jira HVP-7](https://trannhuthuy897.atlassian.net/browse/HVP-7).

## Kiểm thử dữ liệu chung và bản đồ

`SharedTourFlowTest` yêu cầu backend thật: xác nhận nguồn máy chủ, sáu lựa chọn ngôn ngữ, FR/SHORT và BASE được bảo vệ. `MapFlowTest` yêu cầu Google SDK tải bản đồ thật, kiểm tra giới hạn camera và mở bài cùng ID. Lỗi Google không được tính là bản đồ đạt. Build/unit test không thay thế runtime hoặc TalkBack. Các ảnh schematic cũ trong `docs/ui-demo/previews` chỉ là tham khảo lịch sử.
