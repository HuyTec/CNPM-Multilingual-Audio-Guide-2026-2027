# Thiết lập Docker trên Windows

Docker Compose hiện đóng gói hai thành phần đã có trong repository: Spring Boot backend và React web-admin. Backend được build bằng Java 21/Maven; web-admin được build bằng Node.js 24 và phục vụ qua Nginx. Chưa có PostgreSQL trong Compose vì backend hiện chưa cấu hình kết nối cơ sở dữ liệu.

```text
Trình duyệt ── localhost:3000 ──> Nginx (web-admin)
                                    └── /api/* ──> backend:8080
PowerShell ── localhost:8080 ──> Spring Boot /actuator/health
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

```powershell
docker compose -f compose.yaml config --quiet
docker compose -f compose.yaml up -d --build
docker compose -f compose.yaml ps
```

Lần build đầu cần mạng để tải base image, npm packages và Maven dependencies. Compose đợi backend khỏe rồi mới chạy web-admin. Kiểm tra:

```powershell
(Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/actuator/health).Content
(Invoke-WebRequest -UseBasicParsing http://127.0.0.1:3000/).StatusCode
```

Backend trả JSON có `"status":"UP"`; web-admin trả mã `200` và mở tại [http://127.0.0.1:3000/](http://127.0.0.1:3000/). Hai cổng được giới hạn trên loopback của máy. Web-admin chuyển tiếp đường dẫn `/api/` tới backend, nhưng hiện chưa có API nghiệp vụ để kiểm tra luồng này.

## 3. Xem log, cập nhật và dừng

```powershell
docker compose -f compose.yaml logs -f backend
docker compose -f compose.yaml logs -f web-admin
docker compose -f compose.yaml up -d --build
docker compose -f compose.yaml down
```

Nhấn `Ctrl+C` để thoát chế độ theo dõi log; các container vẫn chạy. Sau khi sửa mã nguồn, dùng `up -d --build` để tạo lại image. `down` dừng và xóa các container cùng network của Compose.

Nếu báo không kết nối được Docker daemon, mở Docker Desktop và kiểm tra `docker version`. Nếu tải dependency báo `Unknown host` hoặc `Connection reset`, kiểm tra DNS/mạng, proxy trong Docker Desktop rồi build lại. Nếu cổng 3000 hoặc 8080 đã bị chiếm, giải phóng cổng hoặc đổi số cổng bên trái dấu `:` trong `compose.yaml`.

Docker Compose này phục vụ phát triển và demo trên một máy. Việc triển khai lên VM, TLS, secret, cơ sở dữ liệu và rollback được theo dõi riêng trong [hướng dẫn CI/CD](./CI_CD_SETUP.md).
