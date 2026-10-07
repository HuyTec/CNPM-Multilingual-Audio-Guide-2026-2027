# Ứng dụng thuyết minh đa ngôn ngữ (Multilingual Audio Guide Application)

> Đồ án môn Công nghệ phần mềm, năm học 2026-2027.
> Đề tài: Ứng dụng thuyết minh đa ngôn ngữ, backend thiết kế 3 lớp, CI/CD.

## Thành viên nhóm

| Thành viên        | MSSV       | Vai trò     |
| ----------------- | ---------- | ----------- |
| Trần Nhựt Huy     | 3124411112 | Nhóm trưởng |
| Nguyễn Hoàng Phúc | 3124411236 | Thành viên  |
| Phùng Anh Vũ      | 3124411351 | Thành viên  |

## Giới thiệu

Ứng dụng thuyết minh âm thanh đa ngôn ngữ cho du lịch.

- **Du khách (Android):** chọn địa điểm, xem nội dung hướng dẫn, nghe thuyết minh theo ngôn ngữ đã chọn. Dùng được khi không có mạng nhờ dữ liệu gốc đóng gói sẵn trong ứng dụng; nội dung mới chỉ được tải khi người dùng đồng ý.
- **Quản trị viên (Web):** quản lý địa điểm, nội dung, audio; xem thống kê và phản hồi của du khách.

Phạm vi gồm 7 use case (UC-01 đến UC-07) theo tài liệu PRD.

## Công nghệ sử dụng

| Thành phần     | Công nghệ               | Ghi chú                                             |
| -------------- | ----------------------- | --------------------------------------------------- |
| Web admin      | React.js                | Giao diện quản trị (UC-06, UC-07)                   |
| Backend        | Java 21, Spring Boot 4.1.1, Maven | Kiến trúc 3 lớp                          |
| Android client | Kotlin                  | Ứng dụng cho du khách (UC-01 đến UC-05)             |
| Cơ sở dữ liệu  | PostgreSQL              | Phù hợp với nhiều dịch vụ lưu trữ miễn phí          |
| CI             | GitHub Actions          | Tự động build và test khi push hoặc mở pull request |
| CD / đóng gói  | Docker (Docker Compose) | Chạy được trên nhiều máy, thuận tiện cho demo       |

**Lý do chọn Java cho backend:** cùng nền JVM với Kotlin nên dễ chuyển đổi và đọc hiểu code giữa hai phần.

### Đang cân nhắc (chưa chốt)

Các mục dưới đây mới là đề xuất, chưa phải quyết định của nhóm:

- Web: Vite, TypeScript
- Android: Jetpack Compose, Room, Media3, WorkManager
- Backend: Flyway, PostGIS
- Lưu trữ file audio và gói nội dung: S3 hoặc MinIO

## Kiến trúc

### Nguyên tắc chung

- **Offline-first:** ứng dụng Android đi kèm dữ liệu gốc (địa điểm, nội dung, audio), nên UC-01, UC-02, UC-03 chạy được mà không cần server.
- **Cập nhật tự chọn:** nội dung mới (UC-04) chỉ tải khi người dùng xác nhận; dữ liệu gốc không bị xóa.
- **Chỉ nội dung đã duyệt (APPROVED)** mới được phát hành cho ứng dụng di động.

### Backend thiết kế theo 3 lớp

- **Presentation Layer:** nhận thao tác từ người dùng và hiển thị kết quả (controller).
- **Business Logic Layer:** xử lý các quy tắc và nghiệp vụ của hệ thống (service).
- **Data Access Layer:** làm việc với cơ sở dữ liệu (repository).

> Cấu trúc package chi tiết sẽ được xác định sau khi PRD được kiểm chứng.

## Quy trình và công cụ

| Việc             | Công cụ / quy ước                                         |
| ---------------- | --------------------------------------------------------- |
| Quản lý mã nguồn | GitHub                                                    |
| Quản lý dự án    | Jira (Scrum, sprint 1 tuần, space`HVP`)                   |
| Tên nhánh        | `HVP-<số>-<mô-tả-ngắn>`, ví dụ `HVP-17-backend-skeleton`  |
| Commit           | `HVP-<số>: <mô tả thay đổi>`                              |
| Hợp nhất code    | Qua pull request, CI phải xanh, ít nhất một người xem qua |

**Ví dụ commit:**

- `HVP-17: add health check endpoint`
- `HVP-19: add backend Dockerfile with heap limit`

## Cấu trúc thư mục dự kiến

```text
/backend       Spring Boot API
/web-admin     React web admin
/android       Kotlin Android app
/docs          Tài liệu, sơ đồ
/.github       Workflow GitHub Actions
```

## Tài liệu

- gg doc: [docs.google.com/document/d/1avxIks8YUeUG-7u1_YWe64EFjLieWTuItmzri2PvZOM/edit?usp=sharing](https://docs.google.com/document/d/1avxIks8YUeUG-7u1_YWe64EFjLieWTuItmzri2PvZOM/edit?usp=sharing)
- lược đồ: **[drive.google.com/file/d/1zns1wdhfCAlyiyA4wL7bHa6eJkIttKw4/view?usp=sharing](https://drive.google.com/file/d/1zns1wdhfCAlyiyA4wL7bHa6eJkIttKw4/view?usp=sharing)**
- **PRD:** `./docs/PRD.docx`
- **Sơ đồ:** các lược đồ use case, activity, ERD, class, component, deployment được lưu dưới dạng tệp draw.io (XML) trong `./docs`.

> Lưu ý: PRD và các sơ đồ đang trong giai đoạn kiểm chứng, nội dung có thể thay đổi.

## Chạy dự án

Xem hướng dẫn cài và chạy [backend](./backend/README.md), [web-admin](./web-admin/README.md), [Docker Compose](./docs/vi/DOCKER_SETUP.md) và [thiết lập CI/CD](./docs/vi/CI_CD_SETUP.md).
