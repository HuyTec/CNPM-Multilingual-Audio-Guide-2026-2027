# Đề xuất sửa Graph và Diagram từ XML

Đây là kế hoạch sửa, chưa phải sơ đồ đã sửa. Căn cứ: `se.drawio.xml` có SHA-256 `3c8a56e00bf81cf3d8bed98307bb85fdf7ec4bdc908ffc922610a6bd8bed57cf`, báo cáo 37 phát hiện và các quyết định PRD đã chốt. Bản gốc được giữ nguyên. Khi triển khai, nên sửa trên bản sao `se.revised.drawio.xml`; không ghi đè bản gốc hoặc xóa lịch sử PRD.

## 1. Hướng sửa chung

Chọn subject cho overview là **Multilingual Audio Guide Application**, gồm Mobile App, Web Admin và Backend. Đây là đề xuất về cách thể hiện boundary, không thêm phạm vi sản phẩm. Ba actor chính nằm ngoài boundary; Backend API/Database/owned storage là bên trong. OS services và các external dependencies nằm ngoài ở các interface tương ứng. Nếu vẽ riêng Mobile-only subject, phải ghi rõ vì khi ấy Backend API có thể nằm ngoài sơ đồ con.

Giữ đúng 7 UC và 6 Epic. Không tạo UC mới cho GPS, Login, Publish, Capture Events hoặc Submit Feedback. Không thêm include/extend chỉ để diễn tả UC chạy trước/sau nhau. UC-01/05 mở Details rồi UC-02 tự trigger; UC-03 vẫn cần Tourist chọn Play Audio.

Các thay đổi về node label, connector và style dưới đây dùng cell ID trong XML. Tên `prd20261016_*` là ID đề xuất cho node mới, chưa tồn tại trong source. Geometry và routing cụ thể phải được kiểm tra khi mở/render Draw.io.

## 2. Kế hoạch cho đủ 20 trang

| Trang | Giữ | Sửa cụ thể | Kiểm tra sau sửa |
| --- | --- | --- | --- |
| UC1 | Tourist và UC-01. | Đổi oval GPS Location thành external actor OS Location & Geofencing; đổi cạnh extend thành association thường. Thêm Map Service đúng boundary khi cần. | Chỉ có một oval UC-01; không còn GPS như một UC mới. |
| UC1_activity | Bản flow có prefix `vza5PDOPSUm6RJ7DLLyD-` làm nền vì đã có permission prompt/manual/expand-radius. | Chuyển cụm prefix `3uTMfyMZvL_de9KM9Xpx-` sang trang lịch sử khi sửa; chuẩn hóa một danh sách distance ascending, shortlist đầu danh sách, pagination; thêm guards/notes cho accuracy, ngôn ngữ, nguồn và offline map. | Một flow đang hiệu lực; search 100–500 m; không nhầm geofence [50–500] m. |
| UC2 | Tourist và UC-02. | Bổ sung context boundary ở overview; không tự gắn owned Content Server/CDN thành actor ngoài toàn hệ thống. | Không phát sinh quan hệ include/extend tùy tiện. |
| UC2_activity | APPROVED gate, Language Picker, FULL/SHORT. | Bỏ Send Feedback khỏi node E2 của bản hiệu lực; xóa self-loop A2; chèn SHORT chain → FULL chain; output effective languageCode/scriptType và context version. | E2 chỉ sau khi hết fallback; đổi ngôn ngữ không tự phát audio. |
| UC3 | Tourist và UC-03. | Bổ sung OS Audio & TTS là supporting external/platform actor theo boundary. | Actor không bị biến thành một UC TTS riêng. |
| UC3_activity | Audio priority, focus/disconnect handling. | Thêm Start playback/Playing rõ; kiểm tra voice trước TTS; E4 thiếu voice; temporary/permanent focus loss; E5 stream failure; TTS controls, đổi language/type, background và Completed/Replay. | Thiếu voice vẫn đọc text; permanent loss không auto-resume; không seek trong TTS. |
| UC4 | Tourist và UC-04. | Bổ sung context boundary; Publication Service là upstream dependency, không mặc nhiên actor trực tiếp của mọi hành động tải. | Chỉ nối actor/service nếu có tương tác đúng UC và subject. |
| UC4_activity | Storage checks, verify, fork download/progress đang nối đúng, giữ version khi lỗi. | Package gồm location data/scripts/audio/manifest; chèn playback-ended gate trước install; thêm optional download, cancel-in-progress, mobile-data confirmation; SHA-256/signature; archive visibility. | Playback nền chưa kết thúc thì chưa install; Base files không xóa vật lý. |
| UC5 | Tourist và UC-05. | Bổ sung OS Location & Geofencing/OS Notification theo boundary. | Không coi notification là hành vi auto-play. |
| UC5_activity | Cooldown, nearest, foreground banner, invalid ID và no-selection path. | Thêm app toggle, foreground-only E4, registration/re-registration, loại location archive sau successful install; làm rõ Open Details → UC-02 own trigger; fallback localized name. | Toggle off tắt cả banner/OS notification; invalid ID và không chọn đều không điều hướng. |
| UC6 | UC-06 và actor association hiện có. | Đổi Admin/Manager thành Content Admin (Editor); thêm external interfaces phù hợp, không thêm UC approval/publish. | Không gộp quyền với System Admin. |
| UC6_activity | DRAFT/PENDING_REVIEW và approved-version protection. | Save Draft là lựa chọn độc lập; audio upload/preview có nhánh bỏ qua; mapping language × FULL/SHORT; Submit rules ghi proposed; thêm confirm, concurrent-edit E3, rejected/resubmit, audit, deactivate request. | Admin chỉ yêu cầu deactivate, service chuyển ARCHIVED; không bắt audio khi Save Draft/Submit. |
| UC7 | UC-07 và actor association. | Đổi Admin/Manager thành System Admin / Product Owner. | Không suy ra quyền read-only UC-06. |
| UC7_activity | Metrics/filter/xlsx/export retry. | Tách initialize default period và Load(selectedPeriod); dashboard action hub thay chuỗi bắt buộc update-feedback; thêm E3, feedback truy cập độc lập; terminal RESOLVED; export scope theo kỳ/bộ lọc. | Đổi kỳ không reset 30 ngày; export không bắt cập nhật feedback. |
| ERD | Revision model, optional audio, nullable feedback location; PackageItem có thể giữ. | Bổ sung localized location data, version/thumbnail/description, manifest/integrity mapping, feedback/event fields; phân biệt conceptual/physical và đánh dấu view lịch sử. | Mỗi field phục vụ UC có chỗ lưu hoặc mapping; không tự chốt cardinality chưa duyệt. |
| Sequence | Tourist Play Audio và APPROVED query. | Giữ online happy path có ghi scope hoặc bổ sung alt/ref offline/fallback/no-content/TTS; trả effective values/version. | Failure path không đi tiếp thành Play success; offline không gọi API. |
| Class | Các domain objects có ý nghĩa; không buộc đổi tên GuideContentRevision. | Đổi ba cạnh diamond sai phía thành association thường trong đề xuất tối thiểu; bổ sung localized data/fields hoặc ghi subset và mapping. | Không áp đặt composition/lifetime chưa được chốt. |
| Database | FK/UNIQUE hợp lệ và nullable audio_id/feedback.location_id. | Đồng bộ trường logical model; cạnh nullable audio thành 0..1; record-version/audit mapping; package metadata/manifest. | Feedback lưu được updatedAt và category filter; không biến packageVersion thành edit-lock version. |
| Component | Mobile/Web/Backend/storage, Publisher/OS external hiện có. | Bổ sung Local Content Store/offline path và external Auth/Capture/Submission contracts; request review/deactivate đi tới service. | Capture/submission vẫn ngoài scope; ingestion/consumer nội bộ được phân biệt. |
| Deployment | Device/client/API/database/storage/worker. | Vẽ external dependency nodes/interfaces và giải thích worker jobs; giữ stack là proposed nếu chưa có quyết định công nghệ. | Worker không âm thầm nhận quyền approval/archive; không tự thêm Redis/Kafka/queue. |

## 3. Các thao tác XML có thể xác định ngay

Các thao tác này chỉ là đề xuất để áp dụng trên bản sao. `mechanical_patch_proposal.json` lưu before/after lấy từ source hiện tại; nó không phải chương trình tự sửa toàn bộ sơ đồ.

| Mã | Trang và cell ID | Thao tác | Nội dung/style đề xuất |
| --- | --- | --- | --- |
| MP-01 | UC1 — `AvO_44uf7ePXLSYSitZW-2` | Đổi label và loại shape, giữ ID. | Label: `OS Location & Geofencing`. Đổi ellipse thành actor; geometry cần nới lại theo actor shape. |
| MP-02 | UC1 — `gAk2jW1bffTvXic_H5ub-1` | Đổi semantic connector. | Xóa nhãn `<<extend>>`; set `dashed=0;startArrow=none;endArrow=none`. Giữ source/target, điều chỉnh routing sau đổi shape. |
| MP-03 | UC6 — `iA_47EVZrF2NrLpTxS1p-2` | Đổi label. | `Content Admin (Editor)`. |
| MP-04 | UC7 — `TjRQBHKusHSHiObb22YL-2` | Đổi label. | `System Admin / Product Owner`. |
| MP-05 | UC2_activity — `jr7hXPGmXHG0paXlPZda-9` | Đổi label trong bản sơ đồ hiệu lực. | `Display "Content for this location is currently being updated."; disable Listen to Guide`. Không thêm Send Feedback/external button vào E2. |
| MP-06 | UC2_activity — `diEoupAUMjpGpEfyS3mI-1` | Bỏ cạnh tự nối trong bản sửa. | Giữ cạnh A2 → Retrieve content có scriptType=SHORT; không giữ self-loop vô điều kiện. |
| MP-07 | UC4_activity — `g45D0m5QW9cc8jciQBO7-38` | Đổi label. | `Atomically install matching location data, scripts, available audio and manifest changes; store package version`. |
| MP-08 | UC5_activity — `9Oq7mpxagojBVmxwowkJ-38` | Đổi label để rõ trách nhiệm, không đổi nghĩa trigger đã chốt. | `Open Location Details(locationId); UC-02 starts through its own trigger`. |
| MP-09 | UC7_activity — `Q8Nx3ErHPsax8sTNVdMS-4` | Đổi loader label; phải thêm default-init node cùng lúc. | `Load analytics for selectedPeriod`. 30 ngày chỉ đặt tại init node, không đặt lại trong loader. |
| MP-10 | Class — `HVPNqZoe89-B7RZm4jS7-11`, `...-13`, `...-14` | Bỏ diamond trong đề xuất tối thiểu, giữ source/target/nhãn multiplicity hiện có để nhóm xem tiếp. | Set `startArrow=none;endArrow=none;dashed=0`. Nếu nhóm chọn composition sau này thì phải đặt diamond phía whole và xác nhận lifetime riêng. |
| MP-11 | Database — `EiVs042G670gDdtjzgz2-17` | Sửa nullability ở đầu AudioAsset. | Giữ source package item và target audio; đổi `endArrow=ERone` thành `endArrow=ERzeroToOne`, phù hợp `audio_id NULL`. |

Các style khác không liên quan phải giữ. Không thay source/target hoặc reparent hàng loạt chỉ vì muốn bố cục đẹp; đổi geometry cần kiểm tra absolute position và edge waypoints.

## 4. Các cạnh cần nối lại cùng node mới

### UC-02 — D05 fallback

Không nối thẳng nhánh No của `jr7hXPGmXHG0paXlPZda-7` vào E2 nữa khi SHORT chưa được thử FULL. Quy tắc luồng:

1. Chọn source theo online Server/offline Local; giữ requested scriptType.
2. Thử danh sách ngôn ngữ Selected → EN → VI, bỏ lặp.
3. Nếu hết chuỗi và requested SHORT: thử lại cả chuỗi với FULL; nếu requested FULL thì không có vòng FULL thứ hai.
4. Thành công: xuất effective languageCode/scriptType/scriptText và context version; thông báo nếu dùng FULL thay SHORT.
5. Thất bại sau hết chuỗi áp dụng: E2 hiện có. Version consistency vẫn phải kiểm tra; xử lý version mismatch chưa chốt nên không tự biến thành một E2 mới.

Tham khảo bản graph đề xuất `UC02_Fallback.mmd`.

### UC-03 — focus/TTS

- Cạnh `o0_6KubHvzofkxdgklQR-42` không được giữ điều kiện Yes chung cho mọi focus loss. Chèn decision phân loại temporary/permanent; chỉ temporary + focus restored đi Auto Resume, permanent đi Remain Paused → Tourist Resume.
- Nhánh audio unavailable đi `TTS voice available for effective languageCode?`; Yes → TTS playback, No → E4/read text.
- Stream interrupted → Pause/store audio position → Retry hoặc TTS khi offline; không hứa seek/audio-position chính xác sau khi chuyển sang TTS.

### UC-04 — playback gate

Rewire cạnh `g45D0m5QW9cc8jciQBO7-39` từ Package valid? sang node mới `prd20261016_uc04_playback_active` thay vì đi thẳng install:

`Package valid? [Yes] → Playback active? → [No] Install; [Yes] Wait for playback end → Install`.

Không dùng “left Details” thay “playback ended”. Nhánh package invalid vẫn E3 giữ version hiện có. Fork download/progress đang được nối đúng và cần được bảo toàn.

### UC-06 — chọn thao tác thay vì ép upload

Tạo action hub trên form cho `Save Draft / Edit data / Upload or replace audio / Submit for Review / Request Deactivation / Finish`. Đây là lựa chọn trong UC-06, không phải các UC mới.

- Save Draft: chỉ kiểm tra concurrent edit khi sửa existing; xác nhận lưu theo BR5; lưu DRAFT với incomplete data được phép. Không qua Submit completeness.
- Upload: chọn languageCode + scriptType; file invalid giữ form, retry. Không có audio thì vẫn có thể Submit.
- Submit: validate proposed VI/EN name + FULL VI/EN script và required location fields; nếu có audio thì checkbox audio-script; confirm existing changes; check record-version; lưu PENDING_REVIEW, giữ version approved.
- Request Deactivation: gửi external publication service; không đổi ARCHIVED ngay trên form. Hiệu lực mobile sau successful package install.

Tham khảo `UC06_Content_Management.mmd`. D08 vẫn ghi là đề xuất; không tự coi các số/định mức đã được nhóm nghiệm thu.

### UC-07 — kỳ báo cáo và quyền lựa chọn thao tác

- Thêm `prd20261016_uc07_init_period` với label `Initialize selectedPeriod = last 30 days`; cạnh từ Open Analytics & Feedback đi init rồi loader hiện có.
- Giữ cạnh `Q8Nx3ErHPsax8sTNVdMS-16` quay về loader, nhưng loader đã dùng selectedPeriod. A1 cập nhật selectedPeriod, không qua init.
- Thay chuỗi bắt buộc `Select feedback → Update status → Export?` bằng action hub. Xem chi tiết có thể quay lại dashboard mà không đổi trạng thái; Export có thể được chọn trực tiếp.
- Analytics load error/empty không chấm dứt quyền truy cập feedback; render trạng thái riêng cho từng vùng dữ liệu.
- Update status dùng next valid transition; RESOLVED không có reopen. Export giữ selectedPeriod và active feedback filters.

Tham khảo `UC07_Analytics_Feedback.mmd`.

## 5. Đồng bộ ERD → Class → Database

| Khái niệm cần có | Đề xuất ít thay đổi | Không tự quyết |
| --- | --- | --- |
| Localized location information | Thêm LocationTranslation(locationId, languageCode, name, description) hoặc cấu trúc tương đương; thumbnail và location record-version cần chỗ lưu. | Không ép đổi GuideContentRevision thành GuideScript nếu giữ mapping rõ. |
| Content package cùng version | Giữ PackageItem bridge nếu phù hợp; bổ sung manifest mô tả matching location snapshot/version, scripts/audio và archive changes; metadata size/checksum/signature. | Chưa chốt global packageVersion uniqueness/grouping; không tự thay PK hoặc thêm hàng loạt bảng snapshot. |
| Feedback | Thêm category, languageCode, updatedAt và đủ createdAt. Giữ nullable locationId. | Rating [1–5] vẫn proposal; không thêm user account vào scope. |
| Analytics | Thêm scriptType/source/anonymousDeviceId và event-specific field mapping; isOffline có thể tương đương connectivity. | Không tự chọn event counting formula hoặc bắt mọi field có giá trị ở mọi event. |
| Edit concurrency và audit | Phân biệt record-version/revisionNo với published packageVersion; thể hiện audit fields/history. | Không tự tạo thêm entity history nếu storage design chưa chốt. |
| Base/installed state | Thể hiện LocalContentVersion và quy tắc physical Base files còn tồn tại nhưng location ARCHIVED bị ẩn sau install. | Không suy ra mọi archived file phải xóa vật lý. |

## 6. PRD cần hiệu chỉnh song song trước khi vẽ actor overview

Không giữ cách giải thích “None/N/A = không có thêm human actor” làm quy ước chính thức. Đề xuất thêm correction cạnh nhãn cũ theo R1: `Correction: the previous None/N/A entry does not cover the supporting external actors identified here. Internal infrastructure dependencies are listed separately from actors outside the subject boundary.`

Câu correction là đề xuất mới để nhóm rà soát; chưa được chèn vào các file UC. Actor overview đề xuất không tự biến Database/CDN nội bộ thành actor. Cũng chưa tự nối Publication Service vào UC-04 nếu chỉ có dependency gián tiếp.

## 7. Điều kiện nghiệm thu bản sửa

1. Source gốc giữ nguyên; bản sửa parse được, ID unique, mọi endpoint/parent hợp lệ và không có cạnh mồ côi phát sinh.
2. Chỉ một flow đang hiệu lực cho mỗi Activity; trang lịch sử được phân biệt rõ. Giữ đúng 7 UC và vai trò hiện có.
3. Đi được các tình huống trọng yếu: SHORT thiếu toàn chuỗi, TTS thiếu voice, đang phát nền khi update, archive Base Content, Save Draft không audio, concurrent edit, đổi kỳ, export không đổi feedback, analytics lỗi vẫn mở feedback.
4. Đối chiếu source → node/edge theo SE-01–SE-37; không đánh dấu đóng chỉ vì một label đã đổi trong khi route vẫn sai.
5. Render/mở bản sửa để kiểm tra tất cả trang; kiểm tra nhãn/cạnh/lane/viewport, không suy visual QA từ XML-valid.

## Danh sách cần người quyết định

- Boundary overview dùng toàn hệ thống như đề xuất, hoặc sơ đồ con phải ghi scope rõ.
- Công nghệ demo, package grouping/keys/cardinalities/audit storage và event applicability cần được nhóm duyệt; chưa cần sửa theo một phương án tự chọn.
- Version mismatch khi online content mới hơn location context cần contract xử lý; chưa đưa một hành vi lỗi mới vào UC chỉ để hoàn tất graph.

Không hỏi lại các quyết định D01–D13 đã chốt. Bộ này là đề xuất để sửa sau khi nhóm chốt PRD; chưa thay file XML, UC Markdown hoặc Jira.

## 8. Tệp đề xuất đi kèm

- [Overview bảy UC](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/audit-se/proposed/UC_Overview.mmd).
- [Graph fallback UC-02](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/audit-se/proposed/UC02_Fallback.mmd).
- [Graph quản lý nội dung UC-06](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/audit-se/proposed/UC06_Content_Management.mmd).
- [Graph analytics/feedback UC-07](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/audit-se/proposed/UC07_Analytics_Feedback.mmd).
- [Đề xuất 13 thao tác cơ học có before/after](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/audit-se/mechanical_patch_proposal.json), thuộc 11 nhóm MP-01–MP-11; MP-10 có ba cạnh riêng.

Các file Mermaid là flow/overview khái niệm để duyệt logic, không phải bản thay thế đã render của UML Use Case/Activity trong Draw.io. Các file chưa qua render/visual QA. Chưa có tọa độ layout mới hoặc patch routing đầy đủ nên JSON không được coi là bản sửa tự động hoàn chỉnh.
