# Rà soát độc lập: framework, component và deployment

## Phạm vi và tiêu chí

Đã đối chiếu `docs/en/PRD_Report.md`, `demo/README.md`, `demo/04_component.drawio.xml` và `demo/05_deployment.drawio.xml`. Đây là kiểm tra độc lập đối với tính khả thi MVP, ranh giới thành phần, phụ thuộc ngoài, an toàn và khả năng triển khai. Các đề xuất còn ở mục **Pending design decisions** của PRD được coi là giả định, không phải yêu cầu đã chốt.

## Kết luận nhanh

Phương án Flutter + React/Vite + Spring Boot modular monolith + PostgreSQL/PostGIS + S3/MinIO + Docker Compose là phù hợp để khởi đầu MVP. Hai sơ đồ hiện giải thích được các khối lớn, nhưng **chưa đủ để trở thành kiến trúc triển khai chính thức**: đường đi từ duyệt nội dung đến package `APPROVED`, local persistence của Mobile, xác thực/phân quyền, và biên giới các dịch vụ ngoài chưa được biểu diễn nhất quán.

## Phát hiện bắt buộc xử lý trước khi dùng làm baseline

| Mức | Phát hiện | Bằng chứng và hệ quả UC/BR | Điều chỉnh đề xuất |
|---|---|---|---|
| Cao | Luồng xuất bản bị nối tắt qua Content Workflow | PRD quy định Admin chỉ tạo `DRAFT`/`PENDING_REVIEW`; bên ngoài mới duyệt/xuất bản `APPROVED`. Component diagram lại nối `Content Reviewer / Publisher → Package Distribution`, trong khi `Content Workflow` chỉ nối database và không có quan hệ với Package Distribution. Không thể chứng minh package chỉ lấy revision đã duyệt; mâu thuẫn UC-04, UC-06, BR2/BR4. | Vẽ rõ chuỗi `Content Workflow → external Publisher → Publication/Package Builder → Package Distribution`. Package Builder phải đọc revision `APPROVED` bất biến, tạo manifest/version/hash, rồi ghi database và object storage. Không cho Admin/API thông thường tự chuyển trạng thái hoặc phát hành package. |
| Cao | Package Distribution thiếu nguồn dữ liệu version/approval và cơ chế toàn vẹn | Component diagram chỉ nối Package Distribution với S3/MinIO. Nhưng UC-04 cần kiểm tra version mới, chỉ tải `APPROVED`, xác minh trước khi thay toàn bộ package nguyên tử. Object storage không tự trả lời package nào được publish hay hash nào đáng tin. | Thêm dependency Package Distribution ↔ PostgreSQL (package manifest: trạng thái, version, phạm vi, hash, kích thước, publishAt), và Object Storage cho payload. Mobile nhận manifest qua API; tải payload bằng URL có chữ ký hoặc API; xác minh checksum/chữ ký trước swap. |
| Cao | Local persistence của Mobile không tồn tại trong Component Diagram | UC-02 yêu cầu đọc bundled base content/package đã cài offline; UC-04 yêu cầu cài thay thế nguyên tử; UC-05 cần cooldown; README nói Mobile có offline sync. Component diagram chỉ có một khối Mobile và không có Local Content Store/Package Installer/Notification state. | Tách tối thiểu các component client: `Local Content Store`, `Package Installer`, `Settings & Notification State`, `Guide Content`, `Audio Playback`, `Geofence`. Thể hiện các interface `IApprovedContentQuery`, `IPackageInstall`, `INotificationCooldownStore`. |
| Cao | Authentication/RBAC chỉ là chữ trong API Gateway, không có ranh giới quyền | UC-07 BR1 chỉ Admin được xem analytics/quản lý feedback; UC-06 cần Admin quản lý draft/review; Publisher là quyền khác. Cả hai sơ đồ không có Identity Provider/credential store/token validation hoặc policy Admin vs Publisher. | Đặt `Authentication & Authorization` là cross-cutting component trong Backend; vẽ external IdP hoặc nêu rõ Spring Security + bảng user/role của MVP. Áp chính sách ít nhất: Tourist anonymous, Admin quản lý, Publisher duyệt/xuất bản. Bảo vệ admin API, audit action publish/status. |
| Cao | Deployment chưa thể hiện đường kết nối và bảo mật của tài nguyên dữ liệu | Các node PostgreSQL/MinIO hiển thị như service riêng, nhưng không nêu network private, persistent volume, TLS/secret, backup hay quyền truy cập. Nếu public exposure xảy ra, database/MinIO có nguy cơ bị truy cập trực tiếp. | Thêm public ingress/reverse proxy (HTTPS/TLS) chỉ cho API và Admin SPA; DB/Object Storage trong private network; Docker secrets/env không commit; persistent volumes; backup/restore cho PostgreSQL và object storage. Ghi rõ chỉ API/worker có service credential. |

## Thiếu sót quan trọng cần đưa vào backlog kiến trúc

| Chủ đề | Vấn đề liên quan | Đề xuất MVP có thể triển khai |
|---|---|---|
| Bản đồ và GPS | UC-01 nêu Map Service, GPS locator; component/deployment chỉ gom trong `OS GPS / Geofence`. Map tile/geocoding/routing (nếu có) không có boundary, quota hay điều kiện offline. | Chốt phạm vi: chỉ nearby + danh sách/map marker, dùng một Map SDK/provider; vẽ provider ngoài Mobile. Không cam kết route navigation nếu PRD chưa có. GPS/geofence là OS service, Map provider là external API riêng. |
| Audio và media | `Guide Content` chỉ đọc database, nhưng UC-03 cần file audio hoặc TTS. Chưa rõ API trả audio stream, URL object storage hay mobile tải trong package. | Content API chỉ trả metadata và `audioAssetId`; online playback lấy URL ngắn hạn từ Media Access API, offline đọc file package cục bộ. Không public bucket. TTS dùng OS service và cần kiểm thử ngôn ngữ/giọng trên Android thật. |
| Atomicity và crash recovery | UC-04 yêu cầu không ghép script mới/audio cũ. Deployment có worker nhưng không có transaction boundary hay trạng thái cài client. | Server publish theo hai pha: tạo immutable payload + manifest rồi mới đánh dấu published. Client tải vào staging, verify, đổi con trỏ package/version atomically, giữ bản cũ khi lỗi/cancel/crash. Ghi lại installedVersion và retry policy. |
| Analytics/feedback là external dependency | PRD nói capture analytics và feedback submission là external dependency, còn diagram đặt `Feedback & Analytics` nội bộ mà không chỉ ranh giới ingest/submission. | Chọn một trong hai: (a) xây API ingest/feedback trong modular monolith và sửa PRD bỏ chữ external dependency; hoặc (b) vẽ External Analytics/Feedback provider, adapter, failure queue/retry. Không để trạng thái nửa trong nửa ngoài. |
| Notification và privacy | UC-05 dùng GPS/geofence/background notification; chưa thấy permission lifecycle, cooldown storage, opt-out, tối thiểu hóa location data. | Lưu cooldown local theo `locationId`; không gửi tọa độ liên tục về server nếu không cần; giải thích permission/background limitation; có settings tắt notification. Chỉ thêm server notification nếu PRD yêu cầu push. |
| Worker | Deployment ghi `Spring Scheduler / Worker` cho package/reporting jobs nhưng không nêu trigger, idempotency, retry hay ownership. Scheduler không thay thế Publisher. | Ban đầu chạy cùng Spring Boot (một deployable artifact) hoặc tách worker container khi có job nặng. Mỗi job có lock/idempotency; report export là on-demand async nếu thời gian dài. |
| Khả năng quan sát | Chưa có health check, structured log, metric, audit event hoặc quy trình xử lý lỗi. Đây là chỗ dễ khiến demo không có minh chứng vận hành. | Docker healthcheck cho API/DB/MinIO; log JSON có requestId; endpoint actuator nội bộ; tối thiểu ghi audit `content submitted/approved/published` và metric download/failed install. |

## Đánh giá stack và điều kiện chọn framework

| Lớp | Đánh giá | Điều kiện phải kiểm chứng trước khi khóa |
|---|---|---|
| Flutter | Lựa chọn tốt cho Android/iOS chung và các plugin location/audio/TTS. | Làm spike trên **thiết bị Android thật** cho background geofence, notification, audio focus/Bluetooth và TTS ngôn ngữ mục tiêu; simulator không đủ chứng minh UC-03/UC-05. |
| React + TypeScript + Vite | Đủ cho Admin CRUD/dashboard, dễ triển khai static SPA. | Cấu hình route protection phía server/API, không dựa vào việc ẩn menu React để phân quyền; upload audio có giới hạn MIME/kích thước và quét lỗi. |
| Spring Boot modular monolith | Phù hợp phạm vi, dùng module package rõ ràng thay microservice/Kafka sớm. | Có Spring Security, validation, transaction và Flyway. Tách dependency theo module để `Guide Content` không đọc draft và `Package` không tự approve. |
| PostgreSQL + PostGIS | Phù hợp nearby search và dữ liệu giao dịch. | Chọn `geography(Point, 4326)`, index không gian, giới hạn radius server-side; không tin radius/locationId do client gửi. |
| MinIO/S3 | Phù hợp asset/package immutable, tách blob khỏi DB. | Bucket private, object key không đoán được, manifest/hash trong DB, signed URL TTL ngắn hoặc API proxy; lifecycle/backup. |
| Docker Compose/VM | Hợp lý để demo và MVP một môi trường. | Có `.env.example` không chứa secret, volume, healthcheck, migration strategy, backup test và deploy runbook. Chưa đưa Kafka/Redis/Kubernetes vào baseline. |

## Ranh giới component nên có sau khi chỉnh

```text
Mobile App
  Location Discovery ──> Map Provider, OS Location
  Guide Content ──> Approved Content API + Local Content Store
  Audio Playback ──> Local Audio Store | Media Access API | OS TTS/Audio Focus
  Offline Package Manager ──> Package Manifest API + Package Installer + Local Store
  Geofence Notification ──> OS Geofence/Notification + local cooldown/settings

Backend modular monolith
  AuthN/AuthZ ──> Admin/Publisher policies
  Location Catalog ──> PostgreSQL/PostGIS
  Guide Content Query ──> approved published projection only
  Content Workflow ──> DRAFT/PENDING_REVIEW
  Publication Adapter ──> External Publisher boundary
  Package Builder/Distribution ──> manifest DB + private object storage
  Feedback/Analytics ──> chosen internal API or explicit external adapters
```

## Các quyết định cần chốt để không tạo mâu thuẫn mới

1. External Publisher là người dùng nội bộ có màn hình/quyền riêng, hay là một hệ thống tích hợp? Quyết định này xác định Auth/RBAC, component và deployment.
2. Chọn analytics/feedback tự xây trong Backend hay external provider. PRD và diagrams phải cùng một lựa chọn.
3. Chốt audio optional + TTS fallback hay audio bắt buộc. Khi optional, publication/package phải cho phép revision không có `AudioAsset`.
4. Chốt Android-only hay Android/iOS, và chạy spike cho background geofence trước khi cam kết UI/framework.
5. Xác định base/offline package theo khu vực/nhóm và giới hạn kích thước; đó là input bắt buộc cho manifest, storage và UC-04.

## Thứ tự sửa sơ đồ được khuyến nghị

1. Chốt năm quyết định trên và sửa wording PRD tương ứng.
2. Sửa Component Diagram để phản ánh local store, publication chain, RBAC và các external dependency.
3. Sửa Deployment Diagram với ingress/TLS, private network, persistence/backup và runtime worker rõ ràng.
4. Chạy hai spike có minh chứng: geofence/audio trên thiết bị thật; publish → manifest → download → verify → atomic rollback.

