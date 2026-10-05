# Demo kiến trúc — Multilingual Audio Guide MVP

Thư mục này là bộ sơ đồ **minh họa độc lập**, được tạo từ PRD hiện hành (docs/en/PRD_Report.md và bản dịch Việt). Mỗi sơ đồ là một file Draw.io XML riêng để có thể chỉnh sửa mà không làm chồng lấn các sơ đồ khác.

> **Trạng thái:** Các sơ đồ là bản nháp, đang chờ xử lý các phát hiện audit liên sơ đồ và các quyết định pending trong PRD; chưa phải baseline triển khai.

## Nguồn sự thật và giới hạn

- PRD hiện hành UC-01 đến UC-07 là nguồn nghiệp vụ chính; không dùng inventory sáu UC trong DOCX cũ làm hợp đồng thiết kế.
- Chỉ content APPROVED được Mobile sử dụng; package có version và phải được cài nguyên tử.
- Các quyết định về audio tùy chọn, vai trò Publisher, nhóm package offline, ngôn ngữ mới, feedback và local language vẫn là **chờ chốt**. Demo dùng giả định audio có thể không tồn tại để minh họa TTS fallback; hãy đổi nếu team chốt khác.

## Các XML

| File | Mục đích | Cách đọc ngắn gọn |
|---|---|---|
| 01_erd.drawio.xml | ERD logic | Quan hệ nghiệp vụ và bội số giữa Location, content revision, audio, package, feedback và analytics. |
| 02_database.drawio.xml | Database schema | Gợi ý bảng PostgreSQL, PK/FK, unique constraint và index PostGIS. |
| 03_class.drawio.xml | Class diagram | Domain model, thuộc tính và vài hành vi cốt lõi; không phải class code hoàn chỉnh. |
| 04_component.drawio.xml | Component diagram | Ranh giới trách nhiệm của Mobile, Admin Web, backend modules, storage và external publisher. |
| 05_deployment.drawio.xml | Deployment diagram | MVP chạy trên Mobile, Admin Browser, một Cloud host, PostgreSQL/PostGIS và Object Storage. |
| 06_sequence.drawio.xml | Sequence diagram | Happy path UC-02 → UC-03, gồm kiểm tra APPROVED, fallback ngôn ngữ và TTS. |

## Audit và kế hoạch thực hiện

- [Cross-diagram audit](CROSS_DIAGRAM_AUDIT.md) — phát hiện Blocker/High/Medium có truy vết UC/BR và sơ đồ.
- [Product Backlog & 3-day Sprint Plan](PRODUCT_BACKLOG_3_DAY_SPRINT_PLAN.md) — backlog ưu tiên, acceptance criteria, release gates và workflow lưu minh chứng.

## Framework và stack đề xuất

### Phương án khuyến nghị cho MVP

| Lớp | Chọn | Lý do |
|---|---|---|
| Mobile | Flutter | Một codebase Android/iOS; dễ tích hợp SQLite, audio, TTS, vị trí và geofence chạy nền. |
| Admin Web | React + TypeScript + Vite | Nhanh để làm CRUD nội dung, dashboard và upload audio. |
| Backend | Spring Boot modular monolith | Phù hợp phạm vi MVP và kiến thức Java; module rõ nhưng deploy chỉ một service. |
| Database | PostgreSQL + PostGIS + Flyway | Nearest-location và geofence configuration cần truy vấn không gian; Flyway quản lý lịch sử schema. |
| Media/package | S3-compatible object storage (MinIO khi local) | Lưu audio và file package tách khỏi database. |
| Deploy | Docker Compose trên một VM | Ít thành phần, đủ tái lập và dễ demo. |

### Phương án thay thế

- **React Native + Expo development build** thay Flutter nếu đội đã quen TypeScript. Không dùng Expo Go để kết luận UC-05 chạy nền được; geofence/background task cần kiểm chứng trên Android thật.
- **PWA React** chỉ phù hợp prototype UI, tìm kiếm và nội dung online. Không nên dùng làm phương án cuối cho UC-05 vì background geofence/notification không đồng nhất trên thiết bị.
- Chưa cần Kafka hay microservices. Dùng Spring Scheduler/worker đơn giản cho tác vụ nền; chỉ tách dịch vụ khi có số liệu tải hoặc yêu cầu môn học.
- Redis là tùy chọn sau MVP, cho cache/rate limit; không là dependency bắt buộc của luồng content chính.

## Component lõi để tổ chức code

| Module | Trách nhiệm | UC |
|---|---|---|
| Location Catalog | nearby search, keyword, category, map data | UC-01 |
| Guide Content | chỉ đọc revision APPROVED; language fallback | UC-02 |
| Audio Playback | pre-rendered audio, TTS fallback, audio focus | UC-03 |
| Package Distribution | manifest/version, integrity, atomic install | UC-04 |
| Geofence Notification | cooldown, notification/banner, route locationId | UC-05 |
| Content Workflow | DRAFT/PENDING_REVIEW; bảo vệ published version | UC-06 |
| Feedback & Analytics | feedback status và event reporting | UC-07 |

Không nên tạo component chỉ vì công nghệ: PostgreSQL, TTS OS và S3/MinIO là dependency/infrastructure của component, không phải component nghiệp vụ.

## Việc team cần tự rà trước khi dùng làm thiết kế chính thức

1. Chốt sáu quyết định đang pending trong PRD, đặc biệt audio là bắt buộc hay optional.
2. Xác nhận Mobile target Android-only hay Android/iOS để chọn Flutter hoặc React Native.
3. Xác nhận trách nhiệm Content Reviewer / Publisher là vai trò người dùng hay external service.
4. Rà trực quan từng XML trong Draw.io và điều chỉnh layout/tên theo convention của team.
