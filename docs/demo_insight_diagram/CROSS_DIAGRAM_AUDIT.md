# Cross-diagram audit — Multilingual Audio Guide MVP

## Phạm vi và cách đọc

Đối chiếu `docs/en/PRD_Report.md`, bản dịch Việt, sáu XML trong `demo/` và sáu audit độc lập. Đây là backlog chỉnh thiết kế, không tự thay đổi yêu cầu.

**Đã xác nhận trong PRD:** Mobile chỉ nhận nội dung APPROVED từ package đã publish; package cài đặt nguyên tử; UC-02 fallback Selected → EN → VI; UC-05 chỉ mở Location Details rồi UC-02, không auto-play; UC-06 không ghi đè bản published; UC-04 giữ active version cũ khi lỗi/cancel.

**Chờ Product Owner chốt:** audio có bắt buộc không; Publisher là role hay external service; scope/base package; quy tắc thêm ngôn ngữ; reopen feedback RESOLVED; nơi lưu current language; mobile target Android-only hay Android+iOS. Các XML hiện là **draft**, không phải baseline triển khai.

## Blocker

| ID | Finding đã xác nhận / quyết định chờ | Trace UC/BR | Diagram bị ảnh hưởng | Sửa tối thiểu trước code |
|---|---|---|---|---|
| B-01 | **Publication chain không enforce published APPROVED source of truth.** Content Workflow, Package Distribution và object storage chưa tạo chuỗi kiểm chứng; ERD/DB không cấm DRAFT/PENDING vào package; online query có thể đọc APPROVED nhưng chưa chắc published. Đây là yêu cầu đã xác nhận. | Publication dependency; UC-02 BR1; UC-04 BR2/BR4; UC-06 BR2/BR4 | 01 ERD, 02 DB, 03 Class, 04 Component, 05 Deployment, 06 Sequence | Chỉ external Publisher/adapter được trigger build. Validate mọi revision APPROVED, freeze manifest/version/hash rồi mới PUBLISHED; API/mobile chỉ resolve active published manifest/package. |
| B-02 | **Package/offline install không có manifest integrity hay local install state.** Không đủ metadata để UC-04 verify, resume, atomic swap hoặc rollback. Đây là yêu cầu đã xác nhận; resume detail là pending design. | UC-04 steps 3–7, E2/E3, BR2–BR4 | 01 ERD, 02 DB, 03 Class, 04 Component, 05 Deployment, 06 Sequence | Server package: immutable manifest URI/key, scope, version, size, hash/signature, status, publishedAt. Mobile-local: InstalledPackage + download journal/staging; verify toàn bộ rồi atomic active-pointer swap, giữ bản cũ khi fail/cancel/crash. |
| B-03 | **Revision/audio/package mapping chưa giữ bất biến bản published.** `PackageItem` có hai FK độc lập nên ghép audio revision khác script được; edit audio có thể ghi đè asset của revision đang publish; không có unique mapping theo `(package, location, language, scriptType)`. | UC-03 BR1–BR2; UC-04 BR4; UC-06 A2, BR3–BR4 | 01 ERD, 02 DB, 03 Class, 04 Component, 06 Sequence | Revision đã approved/published phải immutable; edit tạo revision/audio mới PENDING_REVIEW. Bỏ `audio_id` khỏi PackageItem và derive từ revision, hoặc dùng composite constraint; enforce tối đa một mapping triple/package và snapshot immutable. |
| B-04 | **UC-05 chưa khả thi để cam kết trên target chưa chốt.** Permission journey, background lifecycle, registered-geofence limit và cooldown persistence per device/install chưa được thiết kế; DB/Class còn thiếu NotificationLog. Mobile target là pending decision, nhưng không được tuyên bố UC-05 complete trước spike thiết bị thật. | UC-05 E1/E2, BR1–BR4; UC-01 BR4 | 01 ERD, 02 DB, 03 Class, 04 Component, 05 Deployment | Chốt target; chạy spike real device. Mô hình permission state và local cooldown `{locationId,lastNotifiedAt}` (installation scope nếu đồng bộ); persist log sau delivery thành công, không ghi ở deny/cooldown. |

## High

| ID | Finding đã xác nhận / quyết định chờ | Trace UC/BR | Diagram bị ảnh hưởng | Sửa tối thiểu |
|---|---|---|---|---|
| H-01 | **Audio optional đang được vẽ như baseline dù PRD còn pending.** PRD hiện xác nhận audio-preferred/TTS fallback, nhưng optional cho mọi language chỉ là proposal. | Pending #1/#4; UC-03 E1, BR2; UC-04; UC-06 BR3 | 01 ERD, 02 DB, 03 Class, 04 Component, 06 Sequence | Gắn note “conditional”. Sau quyết định: optional thì publish script-only hợp lệ và test TTS-unavailable; mandatory thì multiplicity/UC/package đổi thành 1 audio/revision. |
| H-02 | **Content resolution contract chưa đủ trace.** Sequence/component không trả resolved language, revision/package source hoặc định nghĩa consistent online/offline fallback. | UC-02 BR1–BR2; UC-03 BR1; UC-05 BR3 | 04 Component, 06 Sequence, 01–03 data/domain | Define `resolveApprovedContent(locationId, requestedLanguageCode, scriptType)` → resolvedLanguageCode, revisionId, packageVersion/source, scriptText, audio metadata?; guards Selected→EN→VI→unavailable. |
| H-03 | **Activity semantics UC-04/05/07 bỏ mất rule kiểm thử được.** UC-04 no network kết thúc thay vì resume; progress fork/join không kết thúc rõ; UC-05 không persist cooldown; UC-07 không validate transition hoặc empty filter. | UC-04 E2/BR2–4; UC-05 BR1; UC-07 BR2/E1 | PRD activity source `docs/en/System_Analysist.drawio.xml` (không phải demo XML) | Thêm resume event/offset và interruptible progress loop; persist notification after delivered; guard NEW→IN_REVIEW→RESOLVED và Empty State quay về filter/dashboard. |
| H-04 | **RBAC/audit và deployment trust boundary vắng mặt.** Admin/Publisher separation chỉ là chữ; DB/MinIO exposure, secret, TLS, backup và persistence chưa được vẽ. | UC-06 assumptions/BR2–BR4; UC-07 BR1 | 01–03, 04 Component, 05 Deployment | Add AuthN/AuthZ policy: Tourist read-only, Admin authoring, Publisher approval/publish; audit actor/timestamps. Ingress HTTPS only; DB/object storage private, service credentials/secrets, volumes/backup/healthchecks. |
| H-05 | **Localized location name và language/settings source chưa được mô hình hóa.** `Location.name` không đủ cho notification language; Language catalog không định nghĩa current/resolved value. | UC-01; UC-02 BR2; UC-05 BR3 | 01 ERD, 02 DB, 03 Class, 04 Component, 06 Sequence | Pending #6 must be confirmed; then use `AppSettings.selectedLanguageCode`, `resolvedLanguageCode`, and either LocationTranslation or an explicit canonical/fallback policy. |
| H-06 | **Feedback/analytics model không đáp ứng dashboard và data provenance.** Thiếu category/updatedAt/validated status transition; event language FK and ingestion/idempotency contract are incomplete. | UC-07 BR1–BR3, A1–A3, E1–E2 | 01 ERD, 02 DB, 03 Class, 04 Component, 05 Deployment | Add status constraint, `updated_at`, category, event envelope/idempotency and selected internal-vs-external ingest boundary. Do not implement reopening unless pending #5 is approved. |

## Medium

| ID | Finding | Trace UC/BR | Diagram bị ảnh hưởng | Sửa tối thiểu |
|---|---|---|---|---|
| M-01 | Location domain không nêu visibility/description/thumbnail, positive geofence radius, hoặc chuẩn `GeoPoint` vs PostGIS geography. | UC-01 assumptions/BR1–BR3; UC-05 | 01–03 | Keep PostGIS `geography(Point,4326)`, server-side 50–500m search validation, `geofence_radius_m > 0`, active/visibility and media/description boundary. |
| M-02 | Package scope/base bundle/version comparator/min app compatibility chưa rõ. `scopeType` text không biểu diễn địa điểm/region. | UC-01 offline assumption; UC-04 BR1–BR3 | 01–05 | Pending #3: select scope model, mark bundled vs optional, use monotonic/semver per scope and manifest compatibility; no automatic downgrade. |
| M-03 | Class UML composition diamonds nằm phía child; PackageItem nêu FK thay vì references; NotificationLog/AnalyticsEvent/local install missing. | Domain trace all UC | 03 Class, 01 ERD, 02 DB | Put diamonds at aggregate owner; model association references/multiplicity; synchronize missing domain types once decisions are locked. |
| M-04 | Media/TTS runtime policy incomplete: focus, route disconnect, locale availability, no unintended resume. | UC-03 A1, E1–E3, BR1–BR2 | 04 Component, 05 Deployment, 06 Sequence | Playback state machine; TTS locale check; no resume after permanent loss/disconnect without explicit policy; sequence may be labeled happy path and reference exception activity. |
| M-05 | Map provider, worker ownership, privacy and observability boundaries are absent. | UC-01, UC-04, UC-05, UC-07 | 04 Component, 05 Deployment | Draw Map Provider separately from OS GPS; worker only after Publisher trigger; health/log/audit metrics; minimize raw location collection and document consent/retention. |

## Resolution order

1. Confirm pending #1, #2, #3, #6 and mobile target; record ADRs.
2. Resolve B-01 through B-03 as one content/package contract, then synchronize ERD → DB → class → component → deployment → sequence.
3. Complete B-04 spike and H-03 operational activity corrections before promising UC-04/UC-05.
4. Add RBAC, feedback/analytics and documentation/evidence gates before admin/dashboard claims.

## Non-regression checks after redraw

- A package cannot publish with a non-APPROVED revision, duplicate mapping triple, or mismatched audio.
- Pending edits cannot change the Mobile-visible revision/package.
- Failed/cancelled/crashed install retains exactly the prior active package.
- Notification selection routes to Location Details then UC-02; it never starts UC-03 automatically.
- A demo claiming geofence or TTS fallback includes real-device evidence and the actual OS/permission state.
