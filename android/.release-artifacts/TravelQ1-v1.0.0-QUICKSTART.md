# TravelQ1 1.0.0 — chạy local

1. Cài APK trên Android Emulator (Android 8.0 trở lên). Package release mới `vn.hvp.travelvoice` cài riêng với các bản demo/debug trước đây.
2. Checkout tag `v1.0.0` của repository, chuẩn bị cấu hình PostgreSQL và Admin theo `.env.example`/hướng dẫn backend. Giữ các file env local ngoài Git.
3. Chạy backend với profile hiện có và port 8080. Trên checkout đang có `.env.demo.local` và `.env.admin.local`, chạy ở repository root:

```powershell
$env:BACKEND_PORT = '8080'
$env:BACKEND_BIND_ADDRESS = '127.0.0.1'
docker compose --env-file .env.demo.local --env-file .env.admin.local up -d --build backend
```

Emulator truy cập máy host qua `http://10.0.2.2:8080/`; Android không chạy PostgreSQL/Spring Boot bên trong APK. Điện thoại thật cần bản build trỏ đến IP LAN/HTTPS phù hợp.

OTP email: đặt `GMAIL_USERNAME` và `GMAIL_APP_PASSWORD` trong env local của backend rồi khởi động lại backend. Không đưa tài khoản/mật khẩu gửi thư vào app. Khi chưa cấu hình, tiếp tục sử dụng với tư cách khách. Chi tiết tại `backend/TOURIST_AUTH_API.md`.

Google Maps: bật Maps SDK for Android cho key và cấp quyền package `vn.hvp.travelvoice`, SHA-1 `65:24:78:F2:96:AD:A0:8C:59:AD:F7:D9:31:3B:3C:69:6A:4D:20:D1`. Map cần mạng; text/gói offline vẫn dùng được khi map không tải.

Chọn ghim → Chi tiết → ngôn ngữ/FULL–SHORT → Nghe thuyết minh. Lần đầu TTS cần thời gian tạo âm thanh và giọng tương ứng; lần nghe sau dùng cache. Ngôn ngữ giao diện nằm trong Cài đặt và không đổi ngôn ngữ thuyết minh.
