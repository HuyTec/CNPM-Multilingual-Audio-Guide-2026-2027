# Danh sách API dùng chung

Đối chiếu trực tiếp annotation controller ngày 25/10/2026: **34 method/path**. Các chức năng admin/package/playback trong profile demo, còn discovery/guide có ở profile mặc định. Base URL tùy chạy local/Compose; đường dẫn dưới đây chưa gồm host. Response thành công mặc định HTTP 200; manifest có thể 304. Body lỗi do controller là ProblemDetail (status/detail); filter có thể trả lỗi servlet 401/403, client không được giả định mọi lỗi đều cùng JSON.

Admin gửi Authorization: Bearer <token> sau login; không còn Basic Auth cấu hình. Vai trò được backend kiểm trên từng request; thiếu/hết hạn/thu hồi token → 401, trái vai trò → 403. Không gửi admin token từ client du khách.

| Nhóm | Method | Path | Request/query | Response DTO | Quyền | Component gọi | Lỗi |
|---|---|---|---|---|---|---|---|
| Auth | POST | /api/admin/auth/login | AdminAuthDto.Login | AdminAuthDto.Session | Public | AdminAccess | 400/401/503 |
| Auth | GET | /api/admin/auth/me | — | AdminAuthDto.Identity | EDITOR/REVIEWER | AdminProvider | 401/503 |
| Auth | POST | /api/admin/auth/logout | — | AdminAuthDto.Identity | EDITOR/REVIEWER | AdminProvider | 401/503 |
| Discovery | GET | /api/locations | keyword, categoryId?, page=0, size=20 (1..50) | LocationSearchResponse | Public | useLocationCatalog; DemoApi.locations | 400/404/503 |
| Discovery | GET | /api/locations/{locationId} | UUID path | LocationResponse | Public | Detail/public links | 400/404/503 |
| Discovery | GET | /api/categories | — | List<CategoryResponse> | Public | LocationCatalog | 503 |
| Guide | GET | /api/locations/{id}/guide | language=VI; scriptType=FULL | GuideContent | Public | useGuide; DemoApi.guide | 400/404/503 |
| Playback | POST | /api/playbacks | PlaybackRequest | PlaybackReceipt | Public | playbackOutbox; PlaybackOutbox | 400/404/409/503 |
| Analytics | GET | /api/admin/analytics | from?, to? (ISO date) | AnalyticsResponse | EDITOR/REVIEWER | useLiveAnalytics | 400/401/403/503 |
| Visitors | POST | /api/visitors/heartbeat | VisitorHeartbeat: installationId UUID, platform WEB/ANDROID | 204 no body | Public | VisitorPresence; VisitorApi | 400/503 |
| Visitors | GET | /api/admin/visitors | — | VisitorStats: generatedAt, activeWindowMinutes=5, platforms | EDITOR/REVIEWER | VisitorStats | 401/403/503 |
| Editor | GET | /api/admin/content/config | — | AdminContentDto.Config | EDITOR/REVIEWER | AdminProvider | 401/403/503 |
| Editor | GET | /api/admin/content/locations | — | List<AdminContentDto.LocationDetail> | EDITOR/REVIEWER | LocationList | 401/403/503 |
| Editor | GET | /api/admin/content/locations/{id} | UUID path | AdminContentDto.LocationDetail | EDITOR/REVIEWER | LocationForm | 400/401/403/404/503 |
| Editor | PUT | /api/admin/content/locations/{id} | AdminContentDto.SaveRequest | AdminContentDto.LocationDetail | EDITOR | LocationForm | 400/401/403/409/503 |
| Audio | POST | /api/admin/content/audio | multipart: locationId, language, scriptType, file | AdminContentDto.UploadResponse | EDITOR | LocationForm | 400/401/403/413/503 |
| Image | POST | /api/admin/content/images | multipart: locationId, file (JPEG/PNG, tối đa 5 MB) | LocationImageService.Upload (imageUrl) | EDITOR | LocationImageField | 400/401/403/413/503 |
| Image | GET | /api/admin/content/images/{id}.png | UUID | PNG bytes | EDITOR/REVIEWER | LocationImageField preview | 400/401/403/404/503 |
| Image | GET | /api/content-images/{id}.png | UUID | PNG bytes (ảnh hiện đang công khai) | Public | LocationResults, TourMap, PublishedLocationPhoto | 400/404/503 |
| Audio | GET | /api/admin/content/audio/{id}.{format} | UUID; mp3/wav/ogg | audio bytes | EDITOR/REVIEWER | LocationForm preview | 400/401/403/404/503 |
| Audio | GET | /api/content-audio/{id}.{format} | UUID; mp3/wav/ogg | audio bytes (published reference only) | Public | GuidePlayer; SharedNarrator; packages | 400/404/503 |
| Translation | POST | /api/admin/content/translate | TranslationDto.Request | TranslationDto.Result | EDITOR | LocationForm | 400/401/403/502/503 |
| Review | GET | /api/admin/review/locations | — | List<AdminContentDto.LocationDetail> | REVIEWER | ReviewQueue | 401/403/503 |
| Review | POST | /api/admin/review/locations/{id}/approve | PublicationDto.Decision | PublicationDto.Job | REVIEWER | ReviewQueue | 400/401/403/404/409/503 |
| Review | POST | /api/admin/review/locations/{id}/reject | PublicationDto.Decision | AdminContentDto.LocationDetail | REVIEWER | ReviewQueue | 400/401/403/409/503 |
| Review | GET | /api/admin/review/jobs | — | List<PublicationDto.Job> (latest 50) | REVIEWER | ReviewQueue | 401/403/503 |
| Review | POST | /api/admin/review/jobs/{id}/retry | UUID path | PublicationDto.Job | REVIEWER | ReviewQueue | 400/401/403/409/503 |
| Packages | GET | /api/content-packages/key | — | ContentPackageDto.PublicKey | Public | Web/Android download | 503 |
| Packages | GET | /api/content-packages | If-None-Match? header | List<ContentPackageDto.Manifest>; 304 no body | Public | packageUpdates; SharedTourRepository.available | 503 |
| Packages | GET | /api/content-packages/{scope}/{version} | BASE or OPTIONAL; persisted version | raw signed JSON bytes (ContentPackageDto.Payload) | Public | Web/Android download | 400/404/503 |
| Feedback | GET | /api/admin/feedback | FeedbackDto.Query | FeedbackDto.Page | EDITOR/REVIEWER | LiveFeedback | 400/401/403/503 |
| Feedback | GET | /api/admin/feedback/export | FeedbackDto.Query (all matching rows) | List<FeedbackDto.Item> | EDITOR/REVIEWER | LiveFeedback → XLSX | 400/401/403/503 |
| Feedback | GET | /api/admin/feedback/{id} | UUID path | FeedbackDto.Item | EDITOR/REVIEWER | LiveFeedback detail | 400/401/403/404/503 |
| Feedback | PATCH | /api/admin/feedback/{id}/status | FeedbackDto.StatusRequest | FeedbackDto.Item | EDITOR/REVIEWER | LiveFeedback | 400/401/403/404/409/503 |

## DTO và ví dụ JSON

Các ID/hash/token dưới đây là minh họa, cần dùng giá trị server thực trả. UUID phải giữ nguyên giữa body/path. Tọa độ trong SaveRequest là chuỗi để lưu nháp rỗng; public DTO trả số. Trường names có khóa ngôn ngữ chữ thường, GuideContent.languageCode dùng chữ hoa. Các record nguồn nằm trong [dto](../../backend/multi-audio-guide/src/main/java/com/application/dto) và [GuideContent.java](../../backend/multi-audio-guide/src/main/java/com/application/model/GuideContent.java).

### Auth

```json
{"username":"editor","password":"EditorDemo!2026"}
```

Session: token, username, role, expiresAt. Ví dụ:

```json
{"token":"<opaque-token>","username":"editor","role":"EDITOR","expiresAt":"2026-10-25T16:00:00Z"}
```

Identity (me/logout): `{"username":"editor","role":"EDITOR"}`. Logout xóa phiên trong DB, dù client còn chuỗi token cũng không dùng tiếp được.

### Biên tập, upload và config

SaveRequest tối thiểu lưu nháp:

```json
{"id":"20000000-0000-0000-0000-000000000999","name":"","names":{},"category":"","latitude":"","longitude":"","radius":"","status":"DRAFT","assets":{},"editVersion":0,"fieldOrigins":{}}
```

Ví dụ gửi duyệt sau lần lưu đầu (editVersion phải lấy từ lần đọc mới nhất):

```json
{"id":"20000000-0000-0000-0000-000000000999","name":"POI mới","names":{"vi":"POI mới","en":"New POI"},"category":"Bảo tàng","latitude":"10.78","longitude":"106.70","radius":"80","status":"PENDING_REVIEW","assets":{"vi:FULL":{"locationId":"20000000-0000-0000-0000-000000000999","languageCode":"vi","scriptType":"FULL","packageVersion":"Chưa gán","text":"Nội dung gốc","audioUrl":null,"audioName":null,"audioStatus":null}},"editVersion":1,"fieldOrigins":{"name:vi":"MANUAL","name:en":"EDITED_AFTER_TRANSLATE","script:vi:FULL":"MANUAL"}}
```

LocationDetail giữ các trường trên, thêm updatedBy, updatedAt, reviewerComment, packageVersion và published. published gồm version/name/assets/names/category/latitude/longitude/radius của snapshot công khai; null nếu chưa public. editVersion tăng khi save hoặc quyết định duyệt/từ chối. assets khóa language:FULL/SHORT; audio phải thuộc đúng POI/ngôn ngữ/loại. Nháp thiếu trường được phép, nhưng không thể tự gán APPROVED/PUBLISHED qua PUT.

Config có categories là mảng tên và languages là mảng code/label, ví dụ `{"categories":["Bảo tàng"],"languages":[{"code":"vi","label":"Tiếng Việt"}]}`. Upload trả UploadResponse gồm audioUrl/audioName/audioStatus=ready; URL có UUID server cấp, không tự dựng ID upload. MP3/WAV/OGG phải có header hợp lệ, 12 byte..20MB; multipart request tối đa 21MB.

### Dịch và duyệt

```json
{"sourceLanguage":"vi","targetLanguages":["en"],"name":"POI mới","full":"Bài đầy đủ","shortText":"Bài ngắn"}
```

```json
{"provider":"demo","simulated":true,"variants":[{"language":"en","name":"[DEMO en] POI mới","full":"[DEMO en] Bài đầy đủ","shortText":"[DEMO en] Bài ngắn"}]}
```

Tên nguồn bắt buộc, tối đa 255; FULL/SHORT tối đa 10.000 ký tự mỗi trường; 1..5 ngôn ngữ đích đang hoạt động. Provider thật trả simulated=false. Không tự ghi workspace, không tạo audio. Lỗi provider/timeout trả 502, client giữ form.

Decision dùng cho approve/reject: `{"editVersion":2,"comment":"Nội dung phù hợp"}`. Reject yêu cầu comment không rỗng, tối đa 4000. Approve cần ít nhất một script không rỗng; stale/non-pending → 409. Job trả id/locationId/editVersion/payload/status/errorLog/createdAt; payload là LocationDetail snapshot lúc chấp thuận. BUILDING chưa phải đã public; theo dõi jobs tới READY hoặc FAILED. Retry chỉ áp dụng job FAILED.

### POI, guide, lượt nghe và thống kê

LocationResponse: `{"locationId":"20000000-0000-0000-0000-000000000999","name":"POI mới","categoryId":"10000000-0000-0000-0000-000000000001","categoryName":"Bảo tàng","latitude":10.78,"longitude":106.7,"geofenceRadiusM":80,"names":{"vi":"POI mới","en":"New POI"}}`.

LocationSearchResponse gồm items (LocationResponse[]), page, size, totalElements, totalPages, message, suggestedCategories; CategoryResponse gồm categoryId/name/locationCount. Kiểm chính xác tên trường record ở [LocationSearchResponse.java](../../backend/multi-audio-guide/src/main/java/com/application/dto/LocationSearchResponse.java) và [CategoryResponse.java](../../backend/multi-audio-guide/src/main/java/com/application/dto/CategoryResponse.java). Search page bắt đầu 0; category không tồn tại/ngừng hoạt động → 404; rỗng → 200 với items rỗng.

GuideContent: `{"contentId":"30000000-0000-0000-0000-000000000999","locationId":"20000000-0000-0000-0000-000000000999","locationName":"POI mới","languageCode":"VI","scriptType":"FULL","scriptText":"Nội dung gốc","audioUrl":null}`. Fallback Selected → EN → VI, giữ scriptType; thiếu tất cả → 404. contentId là UUID revision, không phải POI ID.

PlaybackRequest: `{"eventId":"40000000-0000-0000-0000-000000000999","contentId":"30000000-0000-0000-0000-000000000999","occurredAt":"2026-10-25T08:00:00Z","offline":false}`; occurredAt/offline tùy chọn. PlaybackReceipt là eventId/created: `{"eventId":"40000000-0000-0000-0000-000000000999","created":true}`. Gửi lại cùng eventId/contentId không đếm thêm; tái dùng eventId cho contentId khác → 409. Thời điểm tương lai quá 5 phút → 400.

AnalyticsResponse gồm from/to/timeZone/generatedAt, totalPlaybacks/onlinePlaybacks/offlinePlaybacks và topLocations/languages (mảng label/count). Kỳ mặc định 30 ngày, múi giờ Asia/Ho_Chi_Minh. Không cộng số bản dịch thành lượt nghe.

### Gói

PublicKey: algorithm/keyId/spki, RSA SHA-256 với public SPKI Base64. Manifest: schemaVersion=1, scope, version, status=APPROVED, isBase, sizeBytes, sha256, signature, keyId, downloadUrl, assets. assets gồm url/sizeBytes/sha256. Ví dụ cấu trúc rút gọn:

```json
[{"schemaVersion":1,"scope":"BASE","version":"base-<sha256>","status":"APPROVED","isBase":true,"sizeBytes":12345,"sha256":"<sha256>","signature":"<base64>","keyId":"<public-key-sha256>","downloadUrl":"/api/content-packages/BASE/base-<sha256>","assets":[]}]
```

Manifest chỉ lấy READY trong DB nhưng wire status giữ APPROVED cho tương thích schema=1. Nếu chưa có gói READY trả mảng rỗng. ETag phải giữ cả dấu quote từ header; gửi lại If-None-Match thì 304 nếu không đổi. Download trả đúng raw bytes đã ký, không stringify lại JSON để kiểm chữ ký. Payload có schemaVersion/status/scope/isBase/locations/guides/languages/assets và geofencing tùy đủ khảo sát. locations bổ sung names. Header X-Content-SHA256 chứa hash JSON; kích thước tải toàn bộ = sizeBytes JSON + tổng assets.sizeBytes.

### Phản hồi và lỗi

FeedbackDto.Query: from/to ISO date, category/status tùy chọn, page mặc định 1, size mặc định 10 (1..50). Chọn kỳ tối đa 366 ngày. Page: items/page/size/total/totalPages/categories. Item: id/locationId/location/category/content/rating/status/created/updated/createdAt, rating có thể null. Ví dụ StatusRequest `{"status":"IN_REVIEW"}`; chỉ NEW → IN_REVIEW → RESOLVED, sai bước hoặc race → 409. Export trả mọi Item khớp kỳ/category/status, XLSX được dựng ở Web Admin.

Ví dụ lỗi controller: `{"type":"about:blank","title":"Conflict","status":409,"detail":"Bản hoặc trạng thái đã thay đổi. Hãy tải lại."}`. 503 DB/audio không chứng minh request đã lưu; giữ form và tải lại để xác nhận trước retry. Kết quả lỗi dịch không thay dữ liệu đã lưu.

## Static và vận hành

Actuator health không phải controller ứng dụng; tùy cấu hình Actuator. Audio fixture /demo-audio/*.wav là static classpath. /downloads/android-release.json và /downloads/travelvoice.apk là static Web User được script chuẩn bị, không phải API Spring. Metadata gồm url/version/sizeBytes/sha256. Không nhầm endpoint static APK với gói dữ liệu offline của ứng dụng.


## Khách vãng lai và nền tảng lượt nghe

PlaybackRequest nhận thêm installationId (UUID) và platform (WEB/ANDROID), tùy chọn nhưng phải đi cùng nhau. Client cũ không gửi vẫn hoạt động. Metadata chỉ được gắn vào sự kiện được tạo lần đầu; retry cùng eventId không thay attribution hay tăng số đếm. VisitorStats là số liệu tích lũy từ lúc bật heartbeat, độc lập kỳ báo cáo lượt nghe; active là last_seen_at trong 5 phút gần nhất theo giờ server. Không công khai danh sách ID khách, không thu email/hardware ID, không coi tải APK là đã mở app.
