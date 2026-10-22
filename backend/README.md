# Backend — Spring Boot / PostgreSQL / PostGIS

Source: `multi-audio-guide/`, Java 21, Spring Boot 4.1.1, Spring JDBC và Flyway. Android/Web gọi HTTP; backend không dùng JPA/Redis/JWT trong snapshot này.

Hợp đồng HTTP, từng file, schema, luồng Android và finding review tập trung tại [Review Backend và Android](../docs/vi/BACKEND_ANDROID_CODE_REVIEW.md). Yêu cầu: [PRD EN](../docs/en/PRD_Report.md).

## Chạy bằng Docker Compose

Cần Docker Desktop đang chạy. Từ repository root, sao chép `.env.example` thành `.env` và đặt POSTGRES_PASSWORD local. Để Android dùng đủ playback, package và Admin workspace, thêm vào file env ignored:

```dotenv
SPRING_PROFILES_ACTIVE=demo
DEMO_ADMIN_USERNAME=admin
DEMO_ADMIN_PASSWORD=<mat_khau_admin_local>
```

```powershell
docker compose up -d --build backend postgres
docker compose ps
Invoke-RestMethod 'http://localhost:8080/actuator/health'
```

Backend mặc định bind loopback cổng 8080; DB `audio_guide` chỉ ở mạng Compose `postgres:5432`. Android Emulator gọi `http://10.0.2.2:8080/`. Web Admin có thể chạy thêm qua `docker compose up -d --build web-admin` (cổng 3000).

Profile default chỉ có catalog/guide và tourist identity, không có playback/package/Admin content controller. Profile demo thêm fixture và các controller đó. Chi tiết [bảng profile và API](../docs/vi/BACKEND_ANDROID_CODE_REVIEW.md#api). Không đổi profile trên volume đã có demo history để giả lập DB production sạch.

Thiết lập Docker, migration/backup/volume: [DOCKER_SETUP](../docs/vi/DOCKER_SETUP.md). Script legacy 8081/3001 được giải thích riêng trong [LIVE_DEMO_SETUP](../docs/vi/LIVE_DEMO_SETUP.md).

## Build và test trên Windows

Cài JDK 21; Maven Wrapper sẽ tải Maven 3.9.9 khi cần. Từ module backend:

```powershell
cd backend/multi-audio-guide
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
.\mvnw.cmd --version
.\mvnw.cmd -B -ntp verify
```

JAR tại `target/multi-audio-guide-0.0.1-SNAPSHOT.jar`. Nếu dùng `spring-boot:run` ngoài Docker, phải tự cung cấp SPRING_DATASOURCE_URL/USERNAME/PASSWORD tới PostgreSQL/PostGIS truy cập được từ máy host; tên `postgres` của Compose không phải hostname dùng ngoài mạng container.

Integration test chỉ chạy khi TEST_DATABASE_URL có giá trị jdbc:postgresql. Dùng **database kiểm thử riêng**, vì Flyway cài schema/fixture và test có UPDATE/DELETE trong transaction:

```powershell
$env:TEST_DATABASE_URL = 'jdbc:postgresql://localhost:5432/audio_guide_test'
$env:TEST_DATABASE_USERNAME = '<test user>'
$env:TEST_DATABASE_PASSWORD = '<test password>'
.\mvnw.cmd -B -ntp verify
```

Thiếu biến DB, các integration test bị skipped; controller/service test mock không chứng minh SQL/runtime. CI cấu hình PostGIS riêng tại [ci.yml](../.github/workflows/ci.yml); kết quả GitHub Actions cần xem từng run.

## Kiểm tra nội dung

Profile demo hiện có 7 địa điểm / 84 scripts / 6 ngôn ngữ, không có audio thu sẵn cho 7 điểm. Sáu địa điểm hư cấu trước đây được ARCHIVED ở V4.1; không dùng museum fixture cũ làm ví dụ API public hiện hành.

```powershell
Invoke-RestMethod 'http://localhost:8080/api/locations?size=50'
Invoke-RestMethod 'http://localhost:8080/api/locations/20000000-0000-0000-0000-000000000101/guide?language=VI&scriptType=FULL'
# Từ backend/multi-audio-guide:
node scripts/generate-tour-fixture.mjs --check
```

Không sửa migration đã chạy. `--check` chỉ đối chiếu source content với V3_3; script không ghi lại migration khi có cờ đó. Đọc [danh sách SQL và script](../docs/vi/BACKEND_ANDROID_CODE_REVIEW.md#database).

<a id="gmail-otp"></a>

## Gmail OTP

Tourist dùng được khi chưa login. OTP là tùy chọn; mật khẩu Gmail chỉ được cấu hình ở server, không đưa vào APK/source.

Trong file env Compose đã được ignore, thêm GMAIL_USERNAME là địa chỉ gửi và GMAIL_APP_PASSWORD là App Password. Có thể dùng SMTP_HOST/SMTP_PORT nếu cấu hình SMTP khác; Compose mặc định smtp.gmail.com:587, bắt buộc STARTTLS và authentication. TOURIST_MAIL_FROM lấy từ GMAIL_USERNAME. Không thay bằng mật khẩu đăng nhập Google thông thường.

Sau khi điền env, dùng đúng file env/project/volume đang vận hành để rebuild backend, ví dụ stack mặc định:

```powershell
docker compose --env-file .env up -d --build backend
```

Nếu dùng các file ignored `.env.demo.local` và `.env.admin.local` đã có, tiếp tục dùng đúng thứ tự các env-file cùng project/port của stack đó; không tạo stack khác rồi nhầm dữ liệu. Khi mailer/from chưa có hoặc gửi mail lỗi, OTP trả 503 và app vẫn dùng với tư cách khách. Automated auth tests mock mail transport; nhận Gmail thật cần kiểm tra riêng bằng tài khoản đã cấu hình.

API identity và quy tắc rate-limit/token/outbox: [review mục 3.5 và 4.5](../docs/vi/BACKEND_ANDROID_CODE_REVIEW.md#api). Admin Basic Auth dùng DEMO_ADMIN_* riêng; tourist bearer token không có quyền Admin.

## Giới hạn triển khai

Workspace chỉ lưu DRAFT/PENDING_REVIEW; chưa có approve/publish API. Package dùng khóa ký theo tiến trình và snapshot động. Feedback backend chưa có API; offline playback metadata cần bổ sung để báo cáo đúng ngày/online-offline. Các giới hạn và vị trí code được ghi trong [finding review](../docs/vi/BACKEND_ANDROID_CODE_REVIEW.md#findings).
