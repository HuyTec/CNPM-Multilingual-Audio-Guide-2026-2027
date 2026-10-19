# Quy ước dùng chung: Docker và database

## Chạy môi trường local

- Dùng Docker Desktop; chạy lệnh tại thư mục gốc dự án.
- `compose.yaml` lưu cấu hình các service; `Dockerfile` lưu cách build image.
- Mỗi thành viên có database local riêng; dùng chung cấu hình và migration qua Git.
- Sao chép `.env.example` thành `.env`, đặt `POSTGRES_PASSWORD` cho máy mình (`.env` đã được Git bỏ qua). Không commit mật khẩu hoặc file backup dữ liệu.

```dotenv
POSTGRES_PASSWORD=mat_khau_local_cua_ban
```

```powershell
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail 100 backend postgres
```

## PostgreSQL

- Database và user: `audio_guide`; container backend kết nối tới `postgres:5432`.
- Kiểm tra `http://localhost:8080/actuator/health`: trạng thái tổng và `components.db.status` đều phải là `UP`.
- Dữ liệu nằm trong volume `postgres_data`; mỗi máy có volume riêng.
- `docker compose down` giữ volume. `docker compose down -v` xóa volume và dữ liệu: không chạy nếu chưa chủ động chấp nhận mất dữ liệu.
- Đổi mật khẩu trong `.env` không tự đổi mật khẩu của database đã được khởi tạo.

## Thay đổi database và backup

- Flyway đã được tích hợp và tự chạy khi backend khởi động; migration tại `backend/multi-audio-guide/src/main/resources/db/migration/`, bắt đầu bằng `V1__demo.sql` theo trang Database của sơ đồ. Tên migration dùng hai dấu gạch dưới: `V<version>__<description>.sql`.
- Schema demo dùng PostGIS; service `postgres` dùng image `postgis/postgis:17-3.6-alpine` (vẫn PostgreSQL 17).
- Mỗi thay đổi cấu trúc DB tạo migration mới; thống nhất số phiên bản trước khi merge để tránh trùng.
- Không sửa migration đã chạy trên DB dùng chung; tạo migration tiếp theo để điều chỉnh.
- Thêm/sửa/xóa dữ liệu qua ứng dụng không cần migration cho từng thao tác.
- Flyway quản lý phiên bản DB; backup sao lưu dữ liệu. Volume không thay thế backup.
- DB có dữ liệu quan trọng cần backup định kỳ và trước migration có rủi ro; dùng `pg_dump` và kiểm tra khả năng khôi phục.
