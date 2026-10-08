# Cấu trúc phát triển Android

Ứng dụng hiện là bản demo giao diện cho UC-01 đến UC-05. Các màn Compose và dữ liệu mẫu ở `ui/` tiếp tục hoạt động trong khi phần dữ liệu và dịch vụ thật được triển khai dần. Tài liệu yêu cầu là `docs/vi/PRD_Report_vi.md` và `docs/en/PRD_Report.md` ở thư mục gốc.

```text
app/src/main/java/vn/hvp/travelvoice/
├── MainActivity.kt
├── ui/                       Màn Compose, điều hướng, theme, dữ liệu demo hiện có
├── domain/
│   ├── model/                Kiểu dữ liệu cho địa điểm, nội dung và gói nội dung
│   └── repository/           Hợp đồng đọc địa điểm, nội dung đã duyệt và phiên bản gói
├── data/
│   ├── bundled/              Dữ liệu cơ bản đóng gói cùng ứng dụng
│   ├── local/                Lưu trữ trên thiết bị
│   ├── remote/               Nguồn gói nội dung đã xuất bản
│   └── repository/           Triển khai các hợp đồng ở domain
└── platform/
    ├── audio/                Phát audio và TTS dự phòng (UC-03)
    ├── location/             Vị trí, geofence và thông báo (UC-01, UC-05)
    └── packages/             Tải, kiểm tra và cài gói ngoại tuyến (UC-04)
```

Các thư mục mới chỉ là vị trí đặt mã nguồn; chưa có API, cơ sở dữ liệu, quyền thiết bị, dịch vụ nền hoặc thư viện mới. Không chuyển dữ liệu `ui/mock/` sang nguồn thật bằng cách đổi tên: demo cần được thay thế theo từng luồng có kiểm chứng.

## Thứ tự triển khai

1. Chốt dữ liệu cơ bản đóng gói và định dạng package đã duyệt, gồm `version` và ánh xạ `(locationId, languageCode, scriptType)`.
2. Định nghĩa model và repository cho UC-01/UC-02, sau đó nối màn Khám phá và Chi tiết với dữ liệu cục bộ.
3. Thêm audio dựng sẵn và TTS dự phòng cho UC-03, giữ đúng nội dung/ngôn ngữ/độ dài đã chọn.
4. Thêm cập nhật gói có xác nhận, kiểm tra và bảo vệ dữ liệu cơ bản cho UC-04.
5. Thêm vị trí, geofence, quyền và thông báo cho UC-05; chọn thông báo chỉ mở Chi tiết.

Không dùng skill `kotlin-tooling-agp9-migration` để tự động đổi AGP. Dự án hiện là Android thuần với AGP 8.13.2; skill đó chủ yếu hướng dẫn di trú Kotlin Multiplatform lên AGP 9. Chỉ nâng phiên bản build khi có yêu cầu riêng và đã kiểm tra tương thích.
