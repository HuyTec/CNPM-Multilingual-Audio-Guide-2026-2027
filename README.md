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

- **Du khách (Web và Android):** hai client dùng chung use case cốt lõi, quy tắc nghiệp vụ, Backend API và nguồn nội dung APPROVED; không cần tài khoản du khách. Web cho phép mở URL/QR địa điểm để nghe mà không cài app. Android có dữ liệu cơ bản đóng gói; Web cần chuẩn bị và xác minh nội dung trong lần truy cập có mạng trước khi dùng offline. Khả năng TTS, lưu trữ và theo dõi vị trí được nghiệm thu riêng theo nền tảng.
- **Quản trị viên (Web):** quản lý địa điểm, nội dung, audio; xem thống kê và phản hồi của du khách.

Phạm vi gồm 7 use case (UC-01 đến UC-07) theo tài liệu PRD.

Web du khách + Android dùng chung UC-01–UC-05; Web Admin phục vụ UC-06–UC-07. Source hiện có cả ba client và backend; mức độ hoàn thành phải đối chiếu theo từng luồng. Phạm vi phụ trách của Huy là **backend và Android**; xem [review code và luồng API](docs/vi/BACKEND_ANDROID_CODE_REVIEW.md).

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

### Stack hiện có trong source

- Web: Vite, React, TypeScript, Tailwind.
- Android: Jetpack Compose, OkHttp, Google Maps, MediaPlayer và TTS của thiết bị; chưa dùng Room, Media3 hoặc WorkManager.
- Backend: Spring JDBC, Flyway, PostgreSQL/PostGIS; uploaded audio hiện lưu BYTEA trong database.
- S3/MinIO, xác thực production và quy trình xuất bản nội dung còn là phần cần thiết kế/triển khai, chưa suy ra từ demo.

## Kiến trúc

### Nguyên tắc chung

- **Offline-first:** ứng dụng Android đi kèm dữ liệu gốc (địa điểm, nội dung, audio), nên UC-01, UC-02, UC-03 chạy được mà không cần server.
- **Cập nhật tự chọn:** nội dung mới (UC-04) chỉ tải khi người dùng xác nhận; dữ liệu gốc không bị xóa.
- **Chỉ nội dung đã duyệt (APPROVED)** mới được phát hành cho ứng dụng di động.

### Backend thiết kế theo 3 lớp

- **Presentation Layer:** nhận thao tác từ người dùng và hiển thị kết quả (controller).
- **Business Logic Layer:** xử lý các quy tắc và nghiệp vụ của hệ thống (service).
- **Data Access Layer:** làm việc với cơ sở dữ liệu (repository).

Discovery đi đủ controller → service → repository; Admin content và package hiện dùng JDBC trực tiếp trong service. Chi tiết từng file và các giới hạn nằm trong [báo cáo backend/Android](docs/vi/BACKEND_ANDROID_CODE_REVIEW.md).

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

## Cấu trúc thư mục

```text
/backend       Spring Boot API
/web-admin     React web admin
/web-user      React web du khách
/android       Kotlin Android app
/docs          Tài liệu, sơ đồ
/.github       Workflow GitHub Actions
```

## Tài liệu

- gg doc: [docs.google.com/document/d/1avxIks8YUeUG-7u1_YWe64EFjLieWTuItmzri2PvZOM/edit?usp=sharing](https://docs.google.com/document/d/1avxIks8YUeUG-7u1_YWe64EFjLieWTuItmzri2PvZOM/edit?usp=sharing)
- lược đồ: **[drive.google.com/file/d/1zns1wdhfCAlyiyA4wL7bHa6eJkIttKw4/view?usp=sharing](https://drive.google.com/file/d/1zns1wdhfCAlyiyA4wL7bHa6eJkIttKw4/view?usp=sharing)**
- **PRD hiện hành:** [tiếng Anh](docs/en/PRD_Report.md), [tiếng Việt](docs/vi/PRD_Report_vi.md).
- **Bản Word nguồn:** [PRODUCT REQUIRMENTS DOCUMENT.docx](<docs/en/PRODUCT REQUIRMENTS DOCUMENT.docx>); đối chiếu yêu cầu hiện hành bằng hai bản PRD Markdown ở trên.
- **Sơ đồ gốc:** [SE.drawio (1).xml](<docs/en/SE.drawio (1).xml>).
- **Ghi chú PRD theo Epic:** [Khám phá/thông báo](https://trannhuthuy897.atlassian.net/browse/HVP-1), [hướng dẫn/audio](https://trannhuthuy897.atlassian.net/browse/HVP-3), [ngoại tuyến](https://trannhuthuy897.atlassian.net/browse/HVP-4), [quản lý nội dung](https://trannhuthuy897.atlassian.net/browse/HVP-5), [thống kê/phản hồi](https://trannhuthuy897.atlassian.net/browse/HVP-6).
- **Thiết kế/review Web Admin:** [Jira HVP-21](https://trannhuthuy897.atlassian.net/browse/HVP-21); **Android:** [Jira HVP-22](https://trannhuthuy897.atlassian.net/browse/HVP-22).
- **Thiết lập và hạ tầng:** [Jira HVP-7](https://trannhuthuy897.atlassian.net/browse/HVP-7). Câu hỏi nghiệp vụ được ghi tại Epic tương ứng.

Thiết kế và backlog nháp quản lý trên Jira. Repository giữ PRD, hướng dẫn chạy, nguồn nội dung/sơ đồ/ảnh tham khảo. Theo yêu cầu ngày 22/10/2026, giữ một [báo cáo review backend và Android](docs/vi/BACKEND_ANDROID_CODE_REVIEW.md) có link source, API và luồng xử lý.

> Lưu ý: PRD và các sơ đồ đang trong giai đoạn kiểm chứng, nội dung có thể thay đổi.

## Chạy dự án

Xem hướng dẫn cài và chạy [backend](./backend/README.md), [Android](./android/README.md), [web-admin](./web-admin/README.md), [web-user](./web-user/README.md), [Docker Compose](./docs/vi/DOCKER_SETUP.md) và [CI/CD](./docs/vi/CI_CD_SETUP.md).
