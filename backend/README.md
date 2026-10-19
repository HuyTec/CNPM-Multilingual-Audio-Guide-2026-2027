# Backend

Spring Boot API cho nội dung hướng dẫn, quy trình duyệt và quản trị. Mã nguồn ở `multi-audio-guide/`, dùng Java 21, Spring Boot 4.1.1 và Maven Wrapper 3.3.4 (Maven 3.9.9).

## Cài đặt và chạy trên Windows

Cài JDK 21 (nếu máy có `winget`):

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
java -version
javac -version
```

Mở lại PowerShell sau khi cài; `java` và `javac` phải hiển thị phiên bản 21. Đặt `JAVA_HOME` trỏ tới JDK 21 nếu máy có nhiều phiên bản Java.

Mở PowerShell tại thư mục gốc repository, rồi chạy:

```powershell
cd .\backend\multi-audio-guide
.\mvnw.cmd --version
.\mvnw.cmd test
.\mvnw.cmd package
```

`test` tải dependency và chạy các test hiện có; `package` tạo JAR trong `target/`. Không cần cài Maven toàn hệ thống: Maven Wrapper tự tải Maven 3.9.9 vào cache người dùng trong lần đầu. Lần chạy đầu cần kết nối Maven Central.

## Chạy backend với PostgreSQL

Dùng [Docker Compose](../docs/vi/DOCKER_SETUP.md) tại thư mục gốc: sao chép `.env.example` thành `.env`, đặt mật khẩu local, rồi chạy `docker compose up -d --build`. Backend dùng JDBC tới `postgres:5432/audio_guide`; PostgreSQL không công khai cổng ra máy host. Xem thêm [quy ước dùng chung](../GUILD.md).

Nếu chạy `spring-boot:run` ngoài Docker, phải có database truy cập được từ máy host và tự đặt `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` phù hợp. Tên host `postgres` chỉ dùng trong mạng Compose.

## Flyway và schema demo

Flyway tự chạy migration khi backend khởi động, dùng cùng datasource PostgreSQL. Migration đầu tiên là [`V1__demo.sql`](multi-audio-guide/src/main/resources/db/migration/V1__demo.sql); tên có **hai dấu gạch dưới** giữa phiên bản và mô tả.

Migration tạo 9 bảng theo trang **Database** của [`SE.drawio (1).xml`](../docs/en/SE.drawio%20%281%29.xml): `categories`, `languages`, `locations`, `guide_content_revisions`, `audio_assets`, `content_packages`, `content_package_items`, `feedbacks`, `analytics_events`. Đây là schema demo vật lý trong sơ đồ, chưa phải mô hình PRD hoàn chỉnh; không chèn dữ liệu mẫu. Thông tin địa điểm và script text cho phép thiếu khi lưu DRAFT; các trường bắt buộc phải được kiểm tra khi gửi duyệt.

`locations.coordinates` dùng PostGIS `GEOGRAPHY(Point,4326)` và GiST index. Compose dùng PostgreSQL 17 kèm PostGIS (`postgis/postgis:17-3.6-alpine`); nếu dùng PostgreSQL ngoài Docker, cần cài PostGIS và cấp quyền tạo extension cho tài khoản chạy migration, hoặc nhờ quản trị viên bật extension trước.

Sau khi chuẩn bị `.env` theo hướng dẫn ở trên, chạy từ thư mục gốc:

```powershell
docker compose up -d --build
docker compose logs --tail 100 backend
docker compose exec postgres psql -U audio_guide -d audio_guide -c 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;'
```

Kết quả mong đợi: phiên bản `1`, mô tả `demo`, `success = t`. Không sửa migration sau khi đã áp dụng; tạo `V2__...sql` cho lần thay đổi tiếp theo. Với DB đã có bảng ứng dụng nhưng chưa có lịch sử Flyway, cần đối chiếu schema trước khi baseline; không tự bật `baseline-on-migrate` để bỏ qua V1.
