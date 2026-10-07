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
.\mvnw.cmd spring-boot:run
```

`test` tải dependency và chạy test; `package` tạo JAR trong `target/`; `spring-boot:run` chạy ứng dụng. Không cần cài Maven toàn hệ thống: Maven Wrapper tự tải Maven 3.9.9 vào cache người dùng trong lần đầu. Lần chạy đầu cần kết nối Maven Central.
