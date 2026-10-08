# TravelVoice — Android UI Demo (HVP-22)

> Cấu trúc để phát triển các luồng Android thật: [docs/development-structure.md](docs/development-structure.md). Các thư mục `domain/`, `data/` và `platform/` hiện là khung trống; ứng dụng vẫn chạy bằng dữ liệu mẫu.

Jetpack Compose / Material 3 prototype cho du khách, đối chiếu UC-01–UC-05 của [PRD](../docs/en/PRD_Report.md). `MainActivity` → `DemoApp`. Không có backend hay dịch vụ thiết bị thật.

## Chạy demo

Mở `android/` bằng Android Studio, Gradle Sync rồi chạy `app` trên thiết bị/emulator API 26+. Dùng JDK 17+, Android SDK 36 và Build Tools 35.0.0.

```powershell
cd E:\Project\CongNghePhanMem_2026-2027\android
# Sửa theo JDK/SDK thực tế; local.properties không commit.
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:ANDROID_HOME = 'E:\Project\CongNghePhanMem_2026-2027\android\.sdk'
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
# Workaround Windows nếu JDK báo Unable to establish loopback connection:
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=E:/Project/CongNghePhanMem_2026-2027 -Djava.net.preferIPv4Stack=true'
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

## Trình diễn

- Khám phá: tên/đường, danh mục, bán kính 50–500 m, bản đồ minh họa, marker và bottom sheet.
- Chọn địa điểm → VI/EN/JA và Đầy đủ/Tóm tắt → Nghe thuyết minh → player giả lập → Back giữ lựa chọn.
- Bộ chọn tình huống cuối Khám phá: thiếu quyền GPS vẫn tìm thủ công; banner mở chi tiết, không tự phát; cooldown/ID lỗi.
- Chi tiết: thử thiếu bản dịch EN → VI, đang tải/chưa có nội dung; phản hồi là dialog mẫu.
- Ngoại tuyến: kiểm tra → xác nhận → tải/kiểm tra giả → phiên bản mới. Thử thiếu dung lượng/mất mạng/gói lỗi trước khi cập nhật thành công. Xóa gói bổ sung cần xác nhận; nội dung cơ bản luôn được giữ.

Restart app để bắt đầu lại fixture. `rememberSaveable` giữ UI state nhỏ, không có lưu package/tệp/database. Khoảng cách và thuyết minh là mẫu, không phải dữ liệu du lịch đã xuất bản.

## Thiết kế và kiểm tra

Tham khảo ảnh người dùng tại `docs/ui_requirments/`: xanh biển chỉ dẫn, nền giấy sáng, chữ đậm; vector placeholder tạo tại chỗ. Không thêm tài khoản/hồ sơ ngoài PRD. Android dùng font sans hệ thống; Arial của web-admin giữ nguyên, không đóng gói tệp Arial có bản quyền từ Windows.

Mỗi màn có Compose Preview; Offline thêm dark/font lớn. UI tests tương tác trên Compose; host tests dùng Robolectric, không xác nhận dịch vụ thật. Kết quả tại [biên bản review](../docs/ui-demo/ui-review.md).

Xem [hướng dẫn tổng](../docs/ui-demo/README.md), [yêu cầu](../docs/ui-demo/ui-requirements.md), [kiến trúc](../docs/ui-demo/ui-architecture.md).
