# TravelVoice — Android UI Demo (HVP-22)

**Demo database/audio thật:** tab mặc định **Nghe demo** tải địa điểm, SHORT guide và WAV tiếng Việt từ backend, dùng MediaPlayer thật và đồng bộ lượt bắt đầu phát vào PostgreSQL. Web Admin xem thống kê từ cùng DB. Xem [hướng dẫn local/cùng Wi-Fi](../docs/vi/LIVE_DEMO_SETUP.md). Mô tả prototype bên dưới áp dụng cho Khám phá/Ngoại tuyến.

> Cấu trúc phát triển được lưu tại [Jira HVP-22](https://trannhuthuy897.atlassian.net/browse/HVP-22). Bản đồ trực tuyến đã được tích hợp ở `platform/maps/`, dùng `domain/model/MapPoint`; địa điểm, GPS, nội dung, audio và gói ngoại tuyến vẫn là dữ liệu/tình huống demo.

Jetpack Compose / Material 3 cho du khách, đối chiếu UC-01–UC-05 của [PRD](../docs/en/PRD_Report.md). `MainActivity` → `DemoApp`. Tab Bản đồ dùng MapLibre Native OpenGL 13.3.1 và raster OpenStreetMap qua HTTPS; không cần API key. GPS và các màn nghe/offline cũ còn mô phỏng; tab Nghe demo gọi backend và phát audio thật.

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

## Bản đồ OpenStreetMap / MapLibre

Chạy app, chọn tab **Bản đồ** trong Khám phá. Có thể kéo/chụm để pan/zoom hoặc dùng **Thu nhỏ**, **Phóng to**, **Xem tất cả**. Chọn marker mở sheet; **Chọn địa điểm này** mở Chi tiết theo cùng ID, không tự phát thuyết minh. Các nút tên địa điểm dưới bản đồ là phương án lựa chọn cho bàn phím/TalkBack.

`ui/mock/DemoData.mapLocations` bổ sung tọa độ minh họa cho sáu fixture hiện có; chưa phải tọa độ/nội dung đã duyệt và chưa lấy từ API `/api/locations`. Khi chưa có mạng hoặc tải lỗi, chọn **Về danh sách** để tiếp tục; có **Thử lại** khi lỗi tải. Không xin quyền vị trí cho pan/zoom hay chọn thủ công.

Style ở `app/src/main/assets/maps/osm.json`. MapView được quản lý theo lifecycle và hủy khi rời composition; camera lưu bằng state nhỏ của Compose. Chỉ thêm quyền `INTERNET` và `ACCESS_NETWORK_STATE`. MapLibre mặc định OpenGL cho tương thích thiết bị; không đổi AGP/Kotlin/Compose đã ghim.

Theo [OSM Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/), cấu hình giữ attribution hiển thị, gửi User-Agent nhận diện TravelVoiceDemo, giữ cache HTTP của SDK và tắt prefetch zoom. Không có chức năng tải bản đồ offline/bulk. Có thể thay nguồn raster trong file style khi chọn provider khác.

Host test chọn marker dùng renderer Compose được inject, chỉ chứng minh luồng ID/navigation. `MapFlowTest` trong `androidTest` kiểm tra pan/zoom/marker native khi tile tải được; nếu OSM trả lỗi mạng, test kiểm tra thông báo lỗi → về danh sách → mở đúng chi tiết, đồng thời ghi rõ nhánh online chưa được kiểm chứng trong logcat. Cần thiết bị/emulator có Internet và truy cập được máy chủ tile để xác nhận nhánh online. Instrumentation ghim Espresso 3.7.0 vì [bản ổn định này sửa lỗi truy cập InputManager](https://developer.android.com/jetpack/androidx/releases/test#espresso_3_7_0) trên Android mới. Build/host test không thay thế kết quả chạy native hoặc việc kiểm tra TalkBack. Các ảnh map schematic cũ trong `docs/ui-demo/previews` là tham khảo lịch sử.
