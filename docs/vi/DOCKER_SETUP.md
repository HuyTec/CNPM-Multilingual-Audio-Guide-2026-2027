# Thiết lập Docker trên Windows

Docker Compose đóng gói Spring Boot backend, React web-admin và PostgreSQL 17 kèm PostGIS (`postgis/postgis:17-3.6-alpine`). Backend được build bằng Java 21/Maven; web-admin được build bằng Node.js 24 và phục vụ qua Nginx. Backend kết nối database `audio_guide` tại `postgres:5432` qua JDBC; Flyway tự chạy `V1__demo.sql` khi khởi động để tạo schema demo.

```text
Trình duyệt ── localhost:3000 ──> Nginx (web-admin)
                                    └── /api/* ──> backend:8080
PowerShell ── localhost:8080 ──> Spring Boot /actuator/health
                                    └── postgres:5432 ──> volume postgres_data
```

## 1. Cài Docker Desktop và WSL 2

Máy Windows cần bật ảo hóa trong BIOS/UEFI, có ít nhất 8 GB RAM và WSL 2 phiên bản 2.1.5 trở lên. Mở PowerShell với quyền Administrator để cài hoặc cập nhật WSL, rồi khởi động lại máy nếu Windows yêu cầu:

```powershell
wsl --install
wsl --update
wsl --version
```

Tải và cài [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/). Nếu máy có `winget`, có thể dùng:

```powershell
winget install --id Docker.DockerDesktop -e
```

Mở Docker Desktop từ Start menu. Trong **Settings → General**, bật **Use WSL 2 based engine**. Đợi Docker Desktop báo engine đã chạy, mở PowerShell mới và kiểm tra:

```powershell
docker version
docker compose version
```

Nếu PowerShell chưa nhận lệnh `docker` sau khi cài, mở lại terminal. Với bản cài theo người dùng, CLI thường ở `%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin`; thêm thư mục đó vào `PATH` của người dùng nếu cần. `docker version` phải hiển thị cả **Client** lẫn **Server**; chỉ có Client nghĩa là engine chưa chạy.

## 2. Chạy ứng dụng

Mở PowerShell tại thư mục gốc repository:

Sao chép `.env.example` thành `.env` nếu chưa có file, rồi thay mật khẩu mẫu bằng mật khẩu local của mình. File `.env` đã được Git bỏ qua. Nếu trước đó đã đặt `$env:POSTGRES_PASSWORD`, biến này được ưu tiên hơn `.env`; dùng cùng mật khẩu đã khởi tạo database. Đổi giá trị cấu hình không tự đổi mật khẩu trong database đã tồn tại.

```powershell
docker compose -f compose.yaml config --quiet
docker compose -f compose.yaml up -d --build
docker compose -f compose.yaml ps
```

Lần build đầu cần mạng để tải base image, npm packages và Maven dependencies. Compose đợi PostgreSQL khỏe trước khi chạy backend, rồi đợi backend khỏe trước khi chạy web-admin. Kiểm tra:

```powershell
(Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/actuator/health).Content
(Invoke-WebRequest -UseBasicParsing http://127.0.0.1:3000/).StatusCode
```

Backend trả JSON có `"status":"UP"` và `components.db.status` bằng `UP`; web-admin trả mã `200` và mở tại [http://127.0.0.1:3000/](http://127.0.0.1:3000/). Hai cổng được giới hạn trên loopback của máy; PostgreSQL chỉ mở trong mạng Compose. Web-admin chuyển tiếp đường dẫn `/api/` tới backend, nhưng hiện chưa có API nghiệp vụ để kiểm tra luồng này.

## 3. Xem log, cập nhật và dừng

```powershell
docker compose -f compose.yaml logs -f backend
docker compose -f compose.yaml logs -f web-admin
docker compose -f compose.yaml up -d --build
docker compose -f compose.yaml down
```

Nhấn `Ctrl+C` để thoát chế độ theo dõi log; các container vẫn chạy. Sau khi sửa mã nguồn, dùng `up -d --build` để tạo lại image. `down` dừng và xóa các container cùng network, nhưng giữ dữ liệu PostgreSQL trong volume `postgres_data`. `down -v` xóa cả volume và dữ liệu; chỉ dùng khi chủ động muốn xóa database local.

## 4. Quy ước database, migration và backup

- Mỗi máy có volume/database local riêng; Git chia sẻ cấu hình và migration. `.env` chứa POSTGRES_PASSWORD, không commit mật khẩu hoặc backup. Đổi mật khẩu trong `.env` không tự đổi mật khẩu của volume đã khởi tạo.
- Flyway tự chạy từ `backend/multi-audio-guide/src/main/resources/db/migration/`; profile `demo` thêm `db/demo/`. Tên migration `V<version>__<description>.sql` có hai dấu gạch dưới. Thống nhất số phiên bản trước merge.
- Không sửa migration đã chạy trên DB dùng chung; tạo migration mới. CRUD dữ liệu qua ứng dụng không cần một migration cho mỗi thao tác. Không tự bật baseline để bỏ qua schema chưa kiểm tra.
- Kiểm tra `/actuator/health` và database UP; PostGIS cần thiết cho schema tọa độ. Không trộn stack mặc định 8080/3000 với project legacy `hvp-live-demo` 8081/3001 và volume riêng.
- Volume không thay thế backup: sao lưu DB quan trọng bằng `pg_dump` trước migration rủi ro và kiểm tra phục hồi trên DB riêng. Không đưa dump chứa dữ liệu/secret lên Git.

Tra cứu schema và thứ tự migration trong [review backend/Android](BACKEND_ANDROID_CODE_REVIEW.md#database).

Nếu báo không kết nối được Docker daemon, mở Docker Desktop và kiểm tra `docker version`. Nếu tải dependency báo `Unknown host` hoặc `Connection reset`, kiểm tra DNS/mạng, proxy trong Docker Desktop rồi build lại. Nếu cổng 3000 hoặc 8080 đã bị chiếm, giải phóng cổng hoặc đổi số cổng bên trái dấu `:` trong `compose.yaml`.

Docker Compose này phục vụ phát triển và demo trên một máy. Việc triển khai lên VM, TLS, secret, database production và rollback được theo dõi riêng trong [hướng dẫn CI/CD](./CI_CD_SETUP.md).
