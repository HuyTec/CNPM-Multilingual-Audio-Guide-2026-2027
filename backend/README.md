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
