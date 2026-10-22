# Runbook stack demo riêng — legacy 8081/3001

Script này vẫn chạy được một stack hạ tầng riêng; **giao diện Android 1.0.0 hiện dùng TravelQ1App và backend 8080 mặc định**. Các tab Nghe demo/Khám phá prototype không còn là entry point phát hành. Để chạy app hiện tại, ưu tiên [backend README](../../backend/README.md) và [Android README](../../android/README.md).

## Khởi động stack legacy

Cần Docker Desktop. Từ repository root:

```powershell
.\scripts\start-live-demo.ps1
# Chỉ khi chủ động cho phép máy khác trong LAN:
.\scripts\start-live-demo.ps1 -Lan
```

Script tạo `.env.demo.local` ignored với mật khẩu ngẫu nhiên nếu chưa có, profile demo, backend8081/Admin3001. Project `hvp-live-demo` có volume riêng. Không chia sẻ/commit file env; không nhầm số liệu với stack 8080/3000.

Web Admin ở `http://localhost:3001/analytics`. Username/password đọc ở máy local từ DEMO_ADMIN_USERNAME/DEMO_ADMIN_PASSWORD trong file env; không chép credentials vào APK hoặc báo cáo. API Admin kiểm Basic Auth; HTTP chỉ dành môi trường local phù hợp, triển khai công khai cần HTTPS và auth phù hợp.

## Kết nối Android hiện tại

APK mặc định 8080 không tự chuyển sang stack 8081. Nếu muốn kiểm thử stack này, build debug theo Android README với `-PtravelApiBaseUrl=http://10.0.2.2:8081/`. Điện thoại thật cần origin LAN/HTTPS phù hợp và cấu hình network của build. Không có ô đổi server trong shell release hiện tại.

Profile demo hiện tới V4.2: catalog công khai có 7 địa điểm / 84 scripts; 6 fixture hư cấu cũ đã ARCHIVED. Vì vậy không dùng kịch bản chọn Bảo tàng Demo của bản APK cũ. Guide hiện có thể dùng TTS vì 7 điểm chưa có audio thu sẵn. Native voice/Gmail/Maps cần kiểm tra thực tế riêng.

API và flow mới ở [review backend/Android](BACKEND_ANDROID_CODE_REVIEW.md#api). Các device test cũ đang bám prototype, xem finding P1-05; không dùng chúng làm bằng chứng app hiện tại đã kiểm chứng audio/map.

## Kiểm tra và dừng

```powershell
Invoke-RestMethod 'http://localhost:8081/actuator/health'
Invoke-RestMethod 'http://localhost:8081/api/locations?size=50'
docker compose --env-file .env.demo.local -p hvp-live-demo logs --tail 100 backend postgres
docker compose --env-file .env.demo.local -p hvp-live-demo stop
```

`stop` giữ dữ liệu. Không dùng `down -v` nếu cần giữ lượt nghe và nội dung local. Sau khi sửa backend, chạy lại script để build/up; dùng `-NoBuild` chỉ khi muốn tái sử dụng image.

## Fixture audio cũ

Ba WAV dưới `backend/multi-audio-guide/src/main/resources/static/demo-audio/` được script [generate-demo-audio.ps1](../../backend/generate-demo-audio.ps1) tổng hợp từ V3_1 bằng eSpeak. Static file còn phục vụ dù location cũ bị archive; đó không phải audio của 7 điểm mới. Không tạo lại file âm thanh chỉ để chạy review.
