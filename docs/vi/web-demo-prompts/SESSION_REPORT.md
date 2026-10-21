# Báo cáo phiên soạn prompt — 20/10/2026 (Asia/Saigon)

**Phạm vi:** tạo bộ prompt thực thi Web du khách trước, đồng bộ Web Admin, ma trận nghiệm thu và mẫu báo cáo. **Kết luận:** bộ giao việc đã tạo; chưa triển khai/nghiệm thu thêm chức năng ứng dụng trong phiên soạn này.

## Đã thực hiện

- Đọc PRD §2.4 và UC-01–07, đối chiếu source API/audio/events, Admin storage/export, workflow CI và Compose; không thao tác Jira.
- Tạo prompt điều phối, prompt Backend/User/Admin/QA có ownership và dependency, checklist theo AC, kịch bản QR→text→audio/TTS→analytics và bảo vệ published.
- Tách D0/D1 để ưu tiên nhanh nhưng không tuyên bố đạt toàn PRD; TTS thật và xử lý lỗi là phần bắt buộc của D0. Offline TTS và UC-04 được nghiệm thu riêng ở D1.
- Áp dụng hướng dẫn frontend-design-principles, frontend-ui-engineering, web-design-guidelines vào yêu cầu UI. Sau khi người dùng chọn không cài Figma, đã bỏ bước phụ thuộc plugin khỏi các prompt; UI dùng tokens/component hiện có và browser/screenshot review. Chưa triển khai UI mới.

## Kiểm chứng và CI/CD

| Phần | Trạng thái trong phiên này |
| --- | --- |
| Kiểm tra bộ prompt | PASS: 6 tệp, 66 AC không trùng ID; liên kết nội bộ tồn tại, code fence cân bằng; git diff --check không báo lỗi |
| Chức năng mới đã demo | Không có; phiên này chỉ sinh prompt/tài liệu giao việc |
| App build/test/TTS runtime | NOT_RUN trong phiên soạn prompt; không dùng kết quả kiểm tra trước trong chat để đánh dấu lại PASS |
| CI cấu hình | Đã đọc YAML: backend Maven verify với Postgres/PostGIS; Admin lint/build. Chưa có job user/E2E trong workflow hiện tại |
| CI run GitHub | NOT_RUN: chưa kiểm tra run đúng commit trên GitHub |
| CD tự động | NOT_CONFIGURED trong cây workflow đang đọc; Docker Compose/Dockerfile có sẵn không phải CD đã chạy |
| Public QR / APK / TTS nghe thực | NOT_RUN trong phiên soạn prompt |

**Giới hạn:** source/PRD đã có thay đổi của người dùng từ trước; không commit/push, không sửa ứng dụng hay workflow. Bộ prompt là hướng thực thi, không phải cam kết mọi tính năng sẽ hoàn thành trong một tuần.

**Bước tiếp theo:** đưa nội dung `00_MASTER_PROMPT.md` cho agent chính để bắt đầu M0, chốt contract rồi triển khai M1. Khi triển khai, cập nhật tệp này theo mẫu trong `03_DEMO_AND_REPORT.md` bằng bằng chứng của phiên mới.

## Cập nhật mốc backend text — 20/10/2026, 20:29 (Asia/Saigon)

**Kết luận:** đã sửa backend và chuẩn bị hợp đồng/fixture; **chưa nghiệm thu tích hợp DB/Web thật**.

- Tái sử dụng `GuideRepository` / `PlaybackService.guide`; tách GET `/api/locations/{id}/guide` khỏi controller demo để dùng ở mọi profile. Mặc định VI/FULL, giữ JSON hiện có và effective `languageCode`.
- Giữ Selected → EN → VI và scriptType, chỉ APPROVED với location/category/language công khai. Loại thêm text chỉ chứa tab/xuống dòng; no-approved trả 404 ProblemDetail với thông báo đang cập nhật; DB lỗi vẫn là 503.
- Thêm **migration demo mới V3_2** cho địa điểm `20000000-0000-0000-0000-000000000001`: VI/EN × FULL/SHORT. Không sửa migration cũ; VI/SHORT + audio cũ giữ nguyên, ba biến thể mới không có audio. Android gửi rõ VI/SHORT theo source `DemoApi.kt`; chưa chạy Android runtime ở phiên này.
- Hợp đồng và lệnh chạy: `backend/GUIDE_API.md`. Web dùng cùng origin/proxy `/api`, so sánh language yêu cầu với languageCode để báo fallback; CORS riêng chưa cấu hình.

| Bằng chứng trong phiên | Kết quả |
| --- | --- |
| Maven local, Java 21.0.11, Maven 3.9.9 cache | BUILD SUCCESS: 56 test được báo, **25 pass, 31 skip**, 0 failure/error |
| GuideControllerTest (repository giả lập) | **9 pass**: VI/FULL mặc định, text không audio, chuẩn hóa language/giữ SHORT, đầu vào sai, 404 no-approved, 503 DB; không tạo playback |
| LocationControllerTest hồi quy | **16 pass**, repository giả lập |
| GuideIntegrationTest / discovery / playback PostgreSQL | **SKIPPED**: thiếu TEST_DATABASE_URL; chưa chứng minh SQL fallback, bốn biến thể, migration/fixture và bảo vệ revision bằng DB thật |
| Docker | Không có Linux engine pipe; `docker desktop start --detach` thất bại do thiếu registry key Docker Desktop |
| CI/CD, Web/Android runtime, TTS | NOT_RUN; không suy ra đạt từ Maven local |

Lệnh test thành công dùng Maven cache với `-B -ntp -o -DargLine=-javaagent:target/mockito-agent.jar test`; agent là bản sao dependency Mockito **5.23.0 đã có sẵn** từ `.m2`, đặt trong `target/` để tránh lỗi attach pipe/đường dẫn có dấu cách. Không thêm dependency hay sửa POM.

**U201–U205:** lớp HTTP/service đã kiểm tra như trên; SQL APPROVED/fallback/giữ mode/no-approved còn chờ DB. Request cũ đến muộn, thông báo fallback và khóa Nghe là phần Web chưa thực hiện. Không đánh dấu các AC đạt toàn bộ.

**Bước tiếp theo:** cung cấp PostgreSQL/PostGIS test riêng, đặt TEST_DATABASE_URL/USERNAME/PASSWORD và chạy test theo `backend/GUIDE_API.md`; sau đó xác nhận bốn request VI/EN × FULL/SHORT và các nhánh fallback/no-approved trước khi chốt tích hợp. Không UI/TTS/offline/Jira/deploy, không subagent, không commit/push; giữ nguyên thay đổi có trước của người dùng.

## Cập nhật Web du khách đọc text — 20/10/2026 (Asia/Saigon)

- **Branch:** `codex/web-user-guide-text`, tách trước khi triển khai; giữ toàn bộ thay đổi có trước, chưa commit/push. Lượt này chỉ thêm `web-user/` và cập nhật báo cáo, không sửa backend/Android/Admin.
- Không có `API_CONTRACT.md` trong cây tệp đã kiểm tra; dùng hợp đồng hiện có `backend/GUIDE_API.md`. Chỉ đọc PRD §2.4/UC-02, AC W01–W03/W05 và U201–U205; áp dụng ba skill frontend cùng `command.md` bắt buộc.
- `web-user/`: React/Vite/TypeScript/Tailwind; `/locations/:locationId?language=VI&scriptType=FULL`, đọc text nguyên văn, chọn VI/EN và FULL/SHORT, back/forward/reload, báo effective language/fallback nhưng giữ requested language. Có loading, 404/no-content, lỗi mạng/503/retry, timeout, đường dẫn sai và text dài; request cũ bị hủy/chặn, response sai địa điểm/mode/ngôn ngữ không được hiển thị.
- `/` cho dán liên kết địa điểm khác; chưa có danh sách/tìm kiếm UC-01. Ứng dụng luôn gọi `/api`, không thay bằng mock, không audio/TTS/playback/offline. Proxy cấu hình bằng `API_PROXY_TARGET`; cách chạy trong `web-user/README.md`.

| Kiểm tra thực chạy | Kết quả |
| --- | --- |
| Node 24.19.0 / npm 11.17.0; `npm run lint`, `npm run build` | PASS, TypeScript + production Vite build |
| `npm run test:e2e` — Chromium, API giả lập trong test | **17/17 PASS**: bốn biến thể, deep link/reload/history, fallback EN/VI, giữ language/mode, late response, 404/503/network/retry, response lệch/rỗng, keyboard/focus và XSS text |
| Axe WCAG 2 A/AA + 2.1 AA | 0 violation ở reader tại 320/768/1024/1440 và trạng thái 404; không thay thế đánh giá screen reader/thiết bị thật |
| Responsive/visual | Không tràn ngang ở bốn width; đã xem bốn ảnh stress text `web-user/test-results/guide-Guide-browser-contra-168b2-ed-responsive-and-axe-clean/reader-{width}.png` (dữ liệu tổng hợp trong test, tệp ignored) |
| API thật `127.0.0.1:8080` | Connection refused; chưa kiểm chứng Web → backend → PostgreSQL. Không nghiệm thu APPROVED/fallback SQL hay toàn bộ U201–U205 bằng mock |
| CI/CD, public HTTPS/QR, Android | NOT_RUN trong lượt này |

Review theo local Web Interface Guidelines và [bản nguồn Vercel](https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md): P1 `web-user/src/pages/GuidePage.tsx:29` — title động nhiều child đã sửa thành một string; P1 control language/mode — đã sửa trạng thái radio khi URL cập nhật bất đồng bộ và giữ focus bằng native control + đồng bộ URL. Kiểm tra lại: resolved, không còn P0/P1 phát hiện trong phạm vi đọc text. `git diff --check` sạch.

**Bước tiếp theo:** khởi động backend/DB với fixture V3_2, chạy `npm run dev` trong `web-user/` và smoke test API thật trước khi demo. Dừng ở mốc đọc text này; không tuyên bố đã tích hợp DB hay hoàn thành toàn bộ D0.

## Kiểm chứng tích hợp thật — 20/10/2026 (Asia/Saigon)

**Kết luận:** mốc mở URL → đọc text → đổi language/FULL–SHORT đã kiểm chứng trên Web → backend → PostgreSQL thật ở local, bằng fixture giả định đã ghi rõ trong bài. Các giới hạn DB ở hai mục trước đã được giải quyết trong lần kiểm tra này.

- Ban đầu cổng 8080 chạy image backend từ 18/10, route guide chưa tồn tại; PostgreSQL image cũ chưa có PostGIS, schema public rỗng. Đã rebuild backend từ source hiện tại, dùng PostGIS 17 theo Compose và profile `demo`; giữ nguyên volume `congnghephanmem_2026-2027_postgres_data`. Flyway áp dụng thành công 6 migration đến **3.2**, không sửa migration hay source backend.
- API thật: VI/FULL → content 004, VI/SHORT → 001 + audio cũ, EN/FULL → 005, EN/SHORT → 006; JA → EN cho cả hai mode. Địa điểm 002: JA/SHORT → VI, FULL → 404; địa điểm chưa công khai 004 → 404. Content ID ở đây viết suffix; prefix theo `backend/GUIDE_API.md`.

| Bằng chứng thực chạy | Kết quả |
| --- | --- |
| Backend Maven trên PostgreSQL/PostGIS test riêng | **76/76 PASS, 0 skip**: 25 HTTP/service; 23 guide DB + 19 discovery DB + 9 playback DB. Kiểm chứng APPROVED, text rỗng/null/whitespace, Selected → EN → VI, giữ SHORT, latest revision/audio, no-approved và migration validate/idempotency |
| Chromium `LIVE_GUIDE_API=1`, `guide.live.spec.ts` | **2/2 PASS**, không route interception/mock: bốn biến thể text, URL/reload/history, EN/VI fallback, giữ lựa chọn yêu cầu, thiếu FULL, địa điểm chưa công khai |
| DB demo trước/sau đọc Web | analytics_events **0 → 0**; 4 revision APPROVED của địa điểm chính. Test DB dùng container tạm riêng cổng 55432, đã dọn sau test |
| Lint / TypeScript / production build | PASS sau khi thêm test live; 17 test UI giả lập đã pass ở mục trước, không chạy lại vì app source không đổi |
| Visual live | Đã xem desktop 1440 và mobile 390, ảnh `web-user/test-results/guide.live-Live-Web-→-back-e3402-witching-reload-and-history/live-reader-{desktop,mobile}.png` (ignored) |

Đã thêm test live và hướng dẫn chạy trong `web-user/README.md`. Giữ branch `codex/web-user-guide-text`, chưa commit/push. Tự động phê duyệt từ chối bước phụ lưu mật khẩu DB vào `.env` vì tạo thêm bản lưu secret; tệp này **không được tạo**. Container hiện tại giữ profile demo; khi tạo lại bằng Compose cần cung cấp cấu hình local như README.

**Giới hạn còn lại:** chưa kiểm tra QR/HTTPS public, thiết bị điện thoại thật, Android runtime, audio/TTS/offline hoặc CI/CD; không tuyên bố hoàn thành toàn bộ D0. Dừng tại mốc đọc text đã tích hợp local.

## Đối chiếu bộ nội dung Quận 1 — 20/10/2026

- Người dùng định hướng web-app nghe thuyết minh du lịch Quận 1, demo 6 địa điểm / 5 ngôn ngữ.
- Kiểm tra read-only: API cổng 8080 hiện vẫn trả 3 địa điểm công khai cũ; DB `audio_guide` trong container Compose chỉ có ngôn ngữ VI/EN.
- Tệp mới `docs/en/multilingual_tour_content.md` thực có **7 địa điểm × 6 ngôn ngữ × FULL/SHORT = 84 bài**. Ngôn ngữ: VI, EN, FR, JA, ZH, RU. Có chỗ chưa thống nhất giữa bản dịch (tên kiến trúc sư, số ghế) và lỗi trộn từ trong tiếng Nhật; chưa thẩm định nội dung.
- Đang chờ chốt danh sách 6 điểm / 5 ngôn ngữ, xác định DB mới hay nhập từ tài liệu, và phạm vi bước tiếp theo (danh sách + ngôn ngữ hay cả audio/TTS). Chưa sửa app, backend hoặc dữ liệu ở lượt đối chiếu này.

## Đồng bộ tuyến đa ngôn ngữ và đổi bản đồ — 20/10/2026

- Chốt theo phản hồi người dùng: nhập nguyên văn tài liệu vào DB demo; tài liệu thực có 7 điểm / 6 ngôn ngữ VI–EN–FR–JA–ZH–RU / FULL–SHORT = 84 bài. V3_3 đã áp dụng live; `generate-tour-fixture.mjs --check` đạt 7/84. Không sửa migration cũ, không thay text của nhóm; nội dung/tọa độ vẫn cần rà soát.
- Web: danh sách/tìm kiếm → bài theo URL → audio ưu tiên, TTS đúng effective language khi thiếu/lỗi audio; pause/resume/seek/rate, không autoplay. Android có luồng thật tương ứng, VI/FULL mặc định; đường demo VI/SHORT cũ còn tương thích. ID và nội dung từ cùng backend.
- Gói demo BASE/OPTIONAL dùng chung hợp đồng: SHA-256 + chữ ký RSA trên raw bytes và checksum từng WAV; xác nhận trước tải, cài nguyên tử, bảo vệ BASE. Web dùng IndexedDB + service worker production; Android có BASE ký sẵn. Đã sửa lỗi cache asset Vary: Origin làm Web trắng khi mở lại offline. Hợp đồng: backend/CONTENT_PACKAGE_API.md.
- Theo yêu cầu mới, Web thay MapLibre bằng Google Maps SDK chính thức, chưa thêm thư viện. Camera cố định 10.777/106.701, zoom 14–19, strictBounds; chỉ ghim bốn ID 101/102/103/107. Ba điểm lân cận giữ trong danh sách. Android giữ MapLibre, cùng bốn ID và giới hạn tâm/zoom; chưa chuyển SDK Android sang Google. Khung demo là hình chữ nhật, chưa phải polygon ranh hành chính; nền giáp ranh có thể xuất hiện.
- Google chưa có key trong cấu hình: .env.local chưa tồn tại. Cần tự đặt VITE_GOOGLE_MAPS_API_KEY theo web-user/.env.example, bật Maps JavaScript API/billing và giới hạn referrer, restart Vite. Chưa xác minh Google tiles thật; không tính SDK giả lập là tích hợp Google đạt.
- Bằng chứng: backend **80 test / 0 failure / 0 error / 0 skip** trên PostGIS thật; Web lint/build đạt, **17 UI** giả lập đạt, **14 media/live** đạt (gồm 5 ca backend/DB thật, có WAV thật và lượt nghe thật), **4 Google-adapter** giả lập đạt, **2 offline-package** production đạt (gói thật, reopen khi offline; ca lỗi chủ động sửa bytes). TTS adapter kiểm tra đủ 6 ngôn ngữ; chưa nghe/kiểm tra đủ 6 voice thật. Ca WAV sinh lượt nghe thật trong DB demo.
- Review web-design-guidelines (command.md local + nguồn Vercel): đã sửa nhãn select tại web-user/src/components/GuidePlayer.tsx:40 và LocationCatalog.tsx:79; TourMap có loading/empty/config/error, region có tên, link marker + danh sách bàn phím. Không còn P0/P1 xác định trong phần Web sửa; Google runtime vẫn cần key để kiểm chứng.
- Android: **assembleDebug, 11 unit/UI-host test, lintDebug và assembleDebugAndroidTest đều đạt** sau thay đổi vùng (Gradle BUILD SUCCESSFUL). Lint còn 8 warning / 2 hint, 0 error; instrumentation trên emulator chưa hoàn tất (đã dừng do thiếu RAM), không tính là runtime đạt.
- Giới hạn: key ký backend chỉ tồn tại theo tiến trình và public key tải cùng origin HTTP demo, chưa có production trust/pinning/version history; voice mạng và map không offline. Web chưa có playback outbox. Chưa xác nhận toàn bộ UC-01–UC-05, thiết bị thật/QR/HTTPS hoặc CI/CD. Không Jira/deploy/commit/push; branch codex/web-user-guide-text.

## Web Admin dùng dữ liệu chung và APK mới — 21/10/2026

- Branch `codex/web-user-guide-text`; không commit/push/Jira/deploy, giữ thay đổi có trước. Admin danh sách/form/thống kê dùng backend 8080 và PostgreSQL chung. V3_4 thêm workspace/audio; không sửa migration đã áp dụng. Hợp đồng: `backend/ADMIN_CONTENT_API.md`.
- Nháp/gửi duyệt thật, sáu ngôn ngữ × FULL/SHORT, audio MP3/WAV/OGG, optimistic version/409 và audit. Giữ bản APPROVED đang phục vụ (BR4); chưa có quy trình duyệt/xuất bản bên ngoài. Preview cần auth; audio nháp không công khai/gói tải. Phản hồi Admin vẫn ở prototype riêng `source=mock`.
- Android APK **0.2-shared-content / versionCode 2** mặc định đọc máy chủ chung; thêm Làm mới, phân biệt nguồn server/offline, chuyển emulator 8081 → 8080. Google Maps SDK Android thay MapLibre, cùng ID/vùng camera với Web. Cấu hình credentials/key đều ignored, không ghi giá trị vào source/báo cáo.

| Kiểm chứng thực chạy | Kết quả |
| --- | --- |
| Backend trên PostGIS riêng | **87 PASS / 0 skip**; chạy lại 7 Admin test sau bổ sung nhánh audio APPROVED/package: **7 PASS** |
| Admin production local + DB thật | **3 E2E PASS**, đối chiếu 12 biến thể, lưu/reload/upload/gửi duyệt và thống kê; axe 0 violation. Đã dọn đúng UUID test, DB còn 0 bản test |
| Web Tourist | Lint/build PASS; **4 guide/tour live PASS**, 1 map opt-in skip; **2 offline production PASS**, **4 Google adapter PASS** (giả SDK, không chứng minh tiles) |
| Android build/unit/lint | assembleDebug + 11 unit test + lintDebug + assembleDebugAndroidTest PASS; lint 0 error / 13 warning / 2 hint |
| Android runtime API 37 | Đã cài đúng version 2; **SharedTourFlowTest 1/1 PASS**: nguồn máy chủ, sáu ngôn ngữ, FR/SHORT, BASE bảo vệ. **MapFlowTest 1/1 PASS**: Google tải xong, giới hạn camera, mở đúng bài, không autoplay. Lần khởi động instrumentation đầu ANR; chạy lại sau boot ổn định đạt |
| Google Web SDK thật, kiểm tra lại theo yêu cầu | **FAIL: gm_authFailure**, SDK tải được nhưng 0 marker/không đạt tiles. Người dùng đang kiểm tra API/billing; chưa xác định chính xác cấu hình gây từ chối. Danh sách/text vẫn dùng được |

Admin lint/build và image local đã cập nhật; còn cảnh báo chunk ExcelJS lớn. Review local command.md + nguồn Vercel: không còn P0/P1 phát hiện trong các màn sửa; git diff --check sạch. Fixture generator PASS 7 điểm / 84 bài; DB live Flyway 3.4.

Giới hạn: khung bản đồ là hình chữ nhật demo, chưa phải polygon hành chính; chưa kiểm tra đủ sáu voice TTS/thiết bị thật/GPS/geofence/QR/HTTPS/CI/CD. Gói ký demo còn key theo tiến trình, chưa production trust/history. Bước tiếp theo: sửa xác thực key Web trên Google Cloud rồi chạy lại `playwright.google-live.config.ts`; không suy ra Web Maps đạt từ Android hoặc adapter giả.

## Hướng dẫn đọc code — 21/10/2026

- Cập nhật docs/vi/DEMO_CODE_GUIDE.md theo source 5dd713d: 15 mục, routes/data/SQL, Web/Android/Admin, audio/TTS/map/offline, workspace, analytics, legacy và test; thêm link từ README gốc.
- Phân biệt workspace/published, source thật/mock, số liệu offline chưa đầy đủ, stack 8080/3000 và stack 8081/3001 riêng. Giữ kết quả runtime cũ kèm ngày/giới hạn, không chạy lại suite để viết docs.
- Chỉ thay tài liệu; kiểm tra 123 đích link file, 15 anchor mục lục, reference/code fence: PASS; git diff --check sạch. Không sửa source, cấu hình, DB, Jira hoặc triển khai.
