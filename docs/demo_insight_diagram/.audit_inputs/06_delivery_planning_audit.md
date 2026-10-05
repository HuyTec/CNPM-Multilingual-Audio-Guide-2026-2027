# Tuyến kiểm tra độc lập 06 — Kế hoạch delivery, truy vết và minh chứng

## 1. Kết luận phạm vi

Bộ demo mô tả hợp lý một MVP **modular monolith**: Mobile Flutter, Web Admin React, Spring Boot API, PostgreSQL/PostGIS, Object Storage. Trình tự an toàn để làm trong ba ngày không phải là làm đủ mọi chức năng UC-01 đến UC-07, mà là tạo ba lát cắt có thể chứng minh được: **dữ liệu và nội dung đã duyệt**, **trải nghiệm đọc/nghe offline**, rồi **quản trị và quan sát**. Mỗi sprint dưới đây kéo dài đúng một ngày; backlog nào chưa đạt Definition of Done phải quay lại Product Backlog, không được đánh dấu hoàn thành.

## 2. Đối chiếu liên lược đồ và các điểm phải chốt trước code

| Chủ đề | PRD/UC | ERD + Database + Class | Component + Deployment + Sequence | Nhận định/việc phải làm |
|---|---|---|---|---|
| Chỉ phục vụ content đã duyệt | UC-02 BR1, UC-04, UC-06 BR2/BR4 | `GuideContentRevision.status`, `ContentPackage.status` có mặt | Guide Content ghi “APPROVED query”; sequence có truy vấn APPROVED | Phải có query/endpoint chỉ đọc revision nằm trong package **published APPROVED**; chỉ lọc revision APPROVED là chưa đủ để bảo đảm nó đã xuất bản. |
| Giữ bản cũ khi sửa | UC-06 BR4 | Có `revisionNo`, package version và `PackageItem` | Workflow và Package Distribution tách riêng | Cần ràng buộc nghiệp vụ/transaction: revision PENDING_REVIEW không thay thế item trong package đang cài; đây không nên chỉ là quy ước UI. |
| Script–audio khớp nhau, cài nguyên tử | UC-03 BR1, UC-04 bước 5–6, UC-06 BR3 | `PackageItem(contentId,audioId?)` mô tả quan hệ phù hợp | Package Distribution và Object Storage hiện diện | Bổ sung manifest có checksum, content revision/version, file list; local install staging + atomic swap + rollback. `audio_id` nullable chỉ hợp lệ sau khi chốt chính sách TTS. |
| Audio/TTS | UC-03 E1/BR2, quyết định chờ chốt #1 | ERD/class cho audio 0..1; DB `audio_assets.content_id UNIQUE` | Sequence trả audio metadata optional và gọi OS TTS | PRD ghi quyết định này chưa được chốt nhưng demo đã chọn audio tùy chọn. Hoặc chốt TTS fallback, hoặc đổi multiplicity/schema/sequence để audio bắt buộc. Không được để hai cách hiểu cùng tồn tại. |
| Tìm gần và geofence | UC-01 BR1–4, UC-05 BR1–4 | `coordinates GEOGRAPHY(Point,4326)`, GIST; Location có radius | Catalog và OS GPS/Geofence được thể hiện | Cần validate 50–500m cho **search radius**; xác định giới hạn hợp lệ cho `geofence_radius_m`, permission flow, cooldown key (thiết bị/ẩn danh + location), và xử lý multiple geofence. |
| Đa ngôn ngữ/fallback | UC-02 BR2, UC-05 BR3 | `Language`, revision có `language_code`; event cũng có language | Guide Content có fallback; sequence EN/VI | Cần unique/mapping rõ `(location, language, script_type, revision)` và test thứ tự Selected → EN → VI. Notification name/title phải lấy local verified data theo current language; schema Location hiện chỉ có `name`, chưa mô hình hóa tên bản địa hoá. |
| Offline baseline và gói tùy chọn | UC-01 assumption, UC-04 | `ContentPackage.scopeType`; chưa có InstalledPackage/local manifest | Offline Sync trên Mobile, SQLite/package files trên device | Cần xác định base bundle và scope region/group (quyết định chờ chốt #3), bảng/local metadata `installed_package(version, scope, checksum, installed_at)`, download resume và việc không xoá base bundle. |
| Quản trị quyền và xuất bản | UC-06, UC-07 BR1 | Chưa có User/Admin/Role/audit actor trong ERD/database/class | API Gateway ghi Authentication; Publisher là external | Đây là thiếu hụt dữ liệu đáng kể nếu làm thật: cần ít nhất Admin identity/role, `created_by`/`updated_by`, audit timestamp và boundary/API/event cho Reviewer/Publisher. Nếu external hoàn toàn, phải có contract nhận publication result. |
| Feedback và analytics | UC-07 BR1–3 | Feedback/Event có status/time cơ bản | Một component chung Feedback & Analytics, worker reporting | Cần `updated_at` cho Feedback (PRD yêu cầu), constraint trạng thái một chiều NEW→IN_REVIEW→RESOLVED, event contract/version, timezone/report-period và dữ liệu export. Dashboard không tự tạo event: Mobile phải gửi/queue event, hoặc nêu rõ external dependency mock trong MVP. |
| Tải sequence và trách nhiệm runtime | Sequence UC-02→03 | Database có revision/audio nhưng không mô tả local cache | Deployment có API/worker/DB/storage | Sequence thiếu nhánh cache local/offline, HTTP/content package source, lỗi không có content sau EN/VI, và outcome audio focus/pause/resume. Đây là phạm vi mở rộng sau lát cắt Sprint 2, không nên giả vờ đã được demo hiện tại bao phủ. |

## 3. Product Backlog có truy vết

Ưu tiên theo giá trị và phụ thuộc. “AC” là acceptance criteria tối thiểu có thể kiểm thử; không diễn giải các quyết định chờ chốt thành yêu cầu đã phê duyệt.

| ID | Product Backlog Item | Truy vết UC/BR | Phụ thuộc | AC tối thiểu |
|---|---|---|---|---|
| PB-01 | Thiết lập repo, CI, Docker Compose, environment và seed dữ liệu verified | Nền tảng | Không | Build/lint/test chạy trong CI; `.env.example`; seed có category, location, EN/VI content. |
| PB-02 | Catalog địa điểm và tìm nearby/manual | UC-01 BR1–4 | PB-01, PostGIS migration | Radius chỉ 50–500m; sort distance; permission denied vẫn có keyword/category/map; 20 items/page. |
| PB-03 | Model nội dung versioned, ngôn ngữ và query approved với fallback | UC-02 BR1–2; UC-06 BR2/BR4 | PB-01 | API không trả DRAFT/PENDING; selected→EN→VI; không còn content thì explicit unavailable; revision đang published không bị query mới ghi đè. |
| PB-04 | Mobile Location Details hiển thị script và đổi language/mode | UC-02 A1, trigger | PB-03 | Nhận `locationId`; gửi `languageCode`, `scriptType`; render fallback source rõ ràng và disable Listen nếu unavailable. |
| PB-05 | Phát pre-rendered audio/TTS fallback và audio-focus controls | UC-03 BR1–2, A1, E1–E3 | PB-04, quyết định audio | Metadata khớp location/language/type; audio ưu tiên; thiếu/lỗi audio mới TTS; pause/resume/disconnect đúng trạng thái. |
| PB-06 | Publication contract và sinh approved package manifest | UC-04; UC-06 BR2–4 | PB-03, quyết định publisher/audio/package scope | Chỉ package APPROVED; manifest có version/checksum/items; revision pending không được publish; có sự kiện/callback từ external publisher. |
| PB-07 | Offline update/download verify/install/remove | UC-04 BR1–4 | PB-04, PB-06 | Không update giữ nguyên local; verify trước atomic swap; mất mạng resume nếu hỗ trợ; không xoá base; cancel/low disk không đổi version. |
| PB-08 | Geofence notification và handoff Location Details | UC-05 BR1–4 | PB-02, PB-04, current-language decision | Permission/cooldown/multiple geofence; notification chứa locationId; chọn chỉ mở Details rồi UC-02; không tự play audio. |
| PB-09 | Web Admin authoring DRAFT/PENDING_REVIEW và asset validation | UC-06 BR1–4 | PB-03, auth/publisher contract | Draft incomplete được lưu; submit thiếu required bị chặn; invalid upload giữ form; edit approved tạo candidate mới, không thay published. |
| PB-10 | Identity/RBAC và audit trail cho Admin/Publisher boundary | UC-06 assumption; UC-07 BR1 | PB-01 | Admin-only routes bị chặn khi thiếu quyền; actor/timestamp của thay đổi lưu được; external publication request/result xác thực được. |
| PB-11 | Feedback intake, event ingestion, dashboard/filter/status/export | UC-07 BR1–3, A1–A3, E1–E2 | PB-10, event contract | 30 ngày default; Empty State; transition status một chiều; export lỗi không đổi dữ liệu; report có evidence data. |
| PB-12 | Observability, security và release documentation | Toàn hệ thống | PB-01…11 | Structured logs không chứa content/secret nhạy cảm; migration/backup/rollback doc; release notes và traceability cập nhật. |

## 4. Ba sprint, mỗi sprint một ngày

### Sprint 1 — Ngày 1: “Một content đã duyệt có thể được đọc đúng”

- **Sprint goal:** có một vertical slice từ dữ liệu seed đến Mobile Details, chứng minh lọc APPROVED và fallback ngôn ngữ.
- **Chọn:** PB-01, phần lõi PB-03, PB-04; chỉ demo manual-search seed tối thiểu của PB-02 nếu còn thời gian.
- **Thứ tự:** migration Flyway/PostGIS + schema tối thiểu → seed EN/VI/selected content và một DRAFT/PENDING negative fixture → API approved/fallback → Flutter Details → integration test/API collection.
- **Không cam kết ngày 1:** offline update, geofence, upload audio, dashboard, export, full auth.
- **Demo cuối ngày:** mở Details với selected language có content, không có content nhưng fallback EN/VI, và không có APPROVED content; chứng minh DRAFT/PENDING không lộ ra.

### Sprint 2 — Ngày 2: “Nội dung có thể nghe và cài offline an toàn”

- **Sprint goal:** phát đúng nội dung và cài một package APPROVED có thể kiểm chứng mà không làm hỏng bản local cũ.
- **Chọn:** PB-05; phần nền tảng PB-06 (fixture publisher/manifest); phần update cốt lõi PB-07.
- **Thứ tự:** chốt decision #1/#3 hoặc ghi feature flag/mock rõ ràng → manifest + checksum fixture → local package metadata/staging → download/verify/atomic swap → audio/TTS + audio focus smoke test.
- **Ràng buộc:** không đưa UI remove, resume download hay full external Publisher vào Done nếu chưa có thời gian; chúng ở PB-07/PB-06 còn lại.
- **Demo cuối ngày:** package v1 đang chạy; package v2 checksum sai thì vẫn v1; package v2 hợp lệ đổi đồng thời script/audio; file audio thiếu thì TTS (chỉ nếu đã chốt option đó).

### Sprint 3 — Ngày 3: “Quản trị an toàn và quan sát được”

- **Sprint goal:** Admin tạo candidate an toàn, Mobile/Backend tạo evidence analytics, và Dashboard báo cáo được dữ liệu mẫu.
- **Chọn:** lát cắt PB-10 → PB-09 → phần tối thiểu PB-11; PB-08 chỉ là stretch goal sau khi core hoàn thành.
- **Thứ tự:** auth/RBAC + audit fields → DRAFT/PENDING form/API, validation/upload mock → Mobile event contract + feedback fixture → dashboard 30 ngày/filter/status transition/Empty State → CSV/XLSX export nếu core pass.
- **Không đánh dấu “done” geofence** nếu không test trên thiết bị thật với permission/cooldown; đây là backlog phát hành tiếp theo.
- **Demo cuối ngày:** Admin không quyền bị từ chối; draft không lộ; candidate pending không thay bản published; Dashboard hiển thị event có trace id, feedback status hợp lệ và empty state.

## 5. Definition of Ready và Definition of Done

### Definition of Ready (trước khi kéo PBI vào sprint)

1. Có UC/BR và sơ đồ liên quan; actor, input/output, lỗi chính và owner đã xác định.
2. Quyết định ảnh hưởng trực tiếp đã chốt, hoặc có feature flag/mock và phạm vi demo ghi rõ (đặc biệt audio, publisher, package scope).
3. Có AC testable, dữ liệu fixture và dependency đã sẵn sàng.
4. Không vượt sức chứa một ngày; nếu lớn, tách vertical slice trước.

### Definition of Done (không chỉ “code chạy máy em”)

1. Code review, formatter/lint, unit test và integration/API test pass trong CI.
2. Migration có thể chạy từ database trống; rollback/compatibility được mô tả nếu đụng package/content version.
3. AC được đối chiếu bằng test hoặc kịch bản demo; test case gồm một happy path và các exception/permission/approval liên quan.
4. Không lộ DRAFT/PENDING qua API/cache/package; kiểm tra authorization ở backend, không chỉ ở UI.
5. README sprint, backlog status, sơ đồ/ADR bị ảnh hưởng và release note được cập nhật; demo có ảnh/video/log truy vết.

## 6. Cách làm việc để để lại minh chứng

### Nhịp làm việc hằng ngày

1. **Planning 15 phút:** chọn PBI, link UC/BR/diagram, ghi owner, AC, test case và rủi ro vào `docs/04-delivery/sprints/sprint-0X/sprint-backlog.md`.
2. **Thiết kế 20 phút:** cập nhật ADR nhỏ khi có lựa chọn khó đổi; cập nhật một diagram source `.drawio.xml` nếu quan hệ/contract thay đổi. Không sửa file export mà không sửa source.
3. **Thực hiện theo branch:** `feature/PB-03-approved-content`; commit Conventional Commits có PBI, ví dụ `feat(content): enforce approved fallback (PB-03)`.
4. **Review và demo 20 phút:** người khác chạy checklist từ checkout sạch/CI; ghi URL run hoặc log, screenshot/video và kết quả pass/fail.
5. **Close 10 phút:** cập nhật Product Backlog, `review.md`, `retrospective.md`, `CHANGELOG.md`; item chưa đạt DoD trả về trạng thái Ready/Blocked.

### Cây thư mục minh chứng

```text
docs/04-delivery/
  product-backlog.md
  traceability-matrix.md                 # PB ↔ UC/BR ↔ diagram ↔ test
  sprints/sprint-01/
    sprint-goal.md
    sprint-backlog.md
    review.md                            # demo script, actual result, link evidence
    retrospective.md
    evidence/
      api-tests.json
      ci-run.txt
      screenshots/
      test-results/
  adr/ADR-00X-audio-policy.md
```

**Quy tắc evidence:** không commit secret, token, dữ liệu cá nhân hay binary package/audio lớn vào Git. Giữ checksum/manifest, link Object Storage hoặc release asset, fixture nhỏ có license rõ ràng, kết quả test text/JUnit và export ảnh/SVG của diagram khi cần review. Tag cuối mỗi ngày/sprint, ví dụ `v0.1.0-sprint-01`, và bảo vệ `main` bằng CI/review.

## 7. Rủi ro và giả định cần theo dõi

| Rủi ro/giả định | Tác động | Cách giảm thiểu / quyết định cần có |
|---|---|---|
| Sáu quyết định PRD chưa chốt | Schema, package, TTS, notification có thể phải làm lại | Khóa #1 audio, #2 publisher, #3 package scope trước Sprint 2; các quyết định còn lại trước PBI liên quan. |
| External Reviewer/Publisher không có interface | Không có đường hợp lệ PENDING→APPROVED→published | Viết OpenAPI/event contract, mock adapter và ownership/SLA trước PB-06/PB-09. |
| OS geofence/TTS khác nhau trên Android/iOS | UC-03/05 không nhất quán hoặc không chạy background | Làm spike thiết bị thật, capability matrix, graceful degradation; không hứa offline TTS đa ngôn ngữ nếu OS không cung cấp voice. |
| Atomic package sai hoặc storage đầy | Hỏng offline content | Staging directory, checksum/signature, atomic pointer swap, disk preflight, giữ package cũ và test crash/failure. |
| Analytics thiếu event hoặc lệch privacy | Dashboard vô nghĩa/rủi ro dữ liệu | Version event schema, anonymous install id tối thiểu, retention/consent policy và test event end-to-end. |
| Ba ngày quá ngắn cho toàn UC | Dễ có “done giả” | Chỉ commit vertical slices nêu ở sprint; backlog còn lại phải minh bạch là pending, không tô xanh toàn bộ diagram. |

## 8. Ma trận release gate cuối Sprint 3

- **Chấp nhận demo MVP:** PB-01, PB-03, PB-04, phần core PB-05/PB-06/PB-07, PB-09/PB-10/PB-11 có evidence pass.
- **Chưa được tuyên bố hoàn chỉnh:** full UC-01, geofence UC-05, external publication production, resume/remove optional packages, XLSX export nếu chưa có test thực.
- **Gate bắt buộc:** migration sạch; API không phục vụ unpublished content; test package failure giữ bản cũ; authorization backend; traceability matrix đầy đủ cho PBI đã Done.
