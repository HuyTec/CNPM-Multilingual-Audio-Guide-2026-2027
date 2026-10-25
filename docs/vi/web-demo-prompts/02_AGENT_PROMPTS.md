# Prompt giao việc có phạm vi và owner

Các prompt dưới đây chạy dưới [00_MASTER_PROMPT.md](00_MASTER_PROMPT.md). Agent chính cung cấp đường dẫn này và AC tương ứng khi giao việc; không bắt mỗi agent đọc lại toàn bộ lịch sử chat/Jira. Không tạo chat độc lập.

## Trình tự và tránh xung đột

```text
Agent chính: M0 + contract + route/UI tokens
                    |
       +------------+------------+
       |            |            |
   Backend      Web du khách   Web Admin
       +------------+------------+
                    |
     QA/CI sau khi tích hợp, trong lúc các owner sửa lỗi riêng
                    |
     D0 demo + report → từng phần D1 → release verification
```

Owner mặc định: Backend giữ `backend/`; Web du khách giữ `web-user/`; Admin giữ `web-admin/src/`. Agent chính giữ root Compose, reverse proxy, `.gitignore`, API contract, shared files và báo cáo. QA giữ test mới/workflow theo giao cụ thể, không đụng file owner khác đang sửa. Cùng pool tối đa 4 agents kể cả agent chính: QA thay một nhánh đã kết thúc, không mở thêm vô hạn. Nếu cùng checkout, tuyệt đối không reset/revert thay đổi của agent khác; nếu cần worktree thì agent chính quyết định theo quyền hiện có và tích hợp có kiểm soát.

Giao diện frontend có thể làm UI shell theo contract trước; test tích hợp chỉ PASS sau khi backend thật sẵn sàng. Không copy fixture vào runtime để che API chưa có.

## Prompt A — Backend và nội dung nhất quán

```text
Thực hiện backend cho Web demo theo 00_MASTER_PROMPT.md, không thao tác Jira.
Owner của bạn: backend/. Đọc PRD §2.4, UC-02/03/06/07, §5/6.4; đọc source/test/migrations hiện có.

1. Tận dụng public location/category/guide/playback/analytics. Báo profile demo guard và đề xuất runtime config nhỏ nhất; giữ APK cũ tương thích. Chốt contract với agent chính trước frontend tích hợp; không đổi root config nếu chưa nhận ownership.
2. Hoàn thiện APPROVED resolver, Selected→EN→VI, giữ scriptType, FULL default; contentId/revision/audio optional. Fixtures test VI/EN×FULL/SHORT, đủ trạng thái ẩn/public. Không sửa migration đã chạy.
3. API Admin riêng: list/edit incl drafts; save metadata/translations/scripts, incomplete DRAFT, submit required location fields→PENDING_REVIEW, actor/time, upload/replace preview assets. Bảo vệ published metadata/script/audio khi sửa. Auth/role enforced server-side bằng dependency/config hiện có, không account system mới.
4. Có publication fixture/script nội bộ tách product API, thể hiện dependency ngoài; không thêm Editor auto-approve. Tài liệu thao tác ngắn, không chứa secret. API đọc bản public không lấy draft mới nhất nhầm.
5. Playback idempotent event UUID/content validation, real analytics/timezone. Đối với offline event mới, bảo toàn occurredAt/connectivity và APK cũ có default phù hợp; không tự sửa contract gây data migration không rõ. Feedback DB+strict transition/report filters thuộc D1 sau D0.

AC chính: U201–205, U301/302 về audio contract, A601–608, EV01/02, A701/704/706, W05/06.
AC tiếp theo: A609, A702/703/705, EV03 và package interfaces U4 nếu được agent chính giao.
Test bằng PostgreSQL/PostGIS test riêng, migration apply/validate, API+DB assertions, fail paths; mocks không thay DB integration. Không dùng DB demo để reset fixture.

Kết thúc bằng API changes, migration mới, tests/counts/skips, file chính và mismatch chưa giải quyết. Gửi phần cần root config cho agent chính; không ghi đè báo cáo chung. Tiếp tục sửa lỗi trong scope tới khi AC được giao có evidence hoặc có blocker rõ.
```

## Prompt B — Web du khách, QR, audio và TTS

```text
Xây web-user trước theo 00_MASTER_PROMPT.md. Owner: web-user/ và tests tương ứng; không sửa backend/web-admin/root files nếu chưa được giao.
Đọc PRD §2.4, UC-01/02/03; 01_ACCEPTANCE_MATRIX.md; API_CONTRACT.md sau khi agent chính chốt.
Đọc các skill frontend-design-principles/app.md, frontend-ui-engineering, web-design-guidelines + local command.md. Dùng palette/tokens hiện có, preview và review screenshot thật; người dùng đã bỏ Figma cho đợt này, không chờ hoặc đề xuất lại plugin.

Mốc đầu:
- React+Vite+TS+Tailwind, UI vi-VN. Mobile-first: discovery search/category; location deep-link; text; language + FULL/SHORT query; player; APK link tùy chọn. Trang nghe là trọng tâm, không làm homepage quảng cáo hoặc camera QR scanner.
- Public HTTPS QR mở đúng ID, refresh/Back hoạt động; không login/GPS gate; API lỗi hiện error/retry giữ selection, không silent mock fallback.
- UC-02 resolver từ backend: effective language hiển thị; no approved → không nghe; stale request không đè nội dung mới.
- Player audio ưu tiên → TTS fallback đúng nguyên văn; voice match effective language, voiceschanged/timeout/cancel/error; chỉ 1 source, no autoplay; báo E3 và giữ text khi thất bại.
- Pause/resume/seek/speed, interruption, cleanup theo matrix; TTS cần spike sớm vị trí/controls trên browser demo, không giả duration/timeline. Nếu giới hạn browser chưa vượt được thì báo AC FAIL/BLOCKED, không lược requirement.
- Session playback_started thực sự, outbox retry cùng UUID, không đếm pause/seek/rate/fallback như session mới; làm cùng contract backend.

States loading/empty/error/long/permission; semantic button/link, focus, labels, keyboard player, transcripts=text, aria-live, safe areas; Intl, URL state, 320/768/1024/1440 render. Review skill sau mỗi screen, sửa P0/P1. Thử giọng bằng tai trên browser thật; fake speech test chỉ là logic evidence.
AC D0: W01–05, U101, U201–205, U301–311, EV01/02, UI01–03. W04 chỉ READY khi link APK download thật.

Khi D0 tích hợp xong mới nhận offline U4, nearby/map U1, foreground banners U5 theo thứ tự agent chính. Mỗi phần có storage/permission errors, không dummy timers/numbers mô phỏng chạy thật.
Handoff: routes, component/state/adapter map, commands, browser/voice evidence, test results và limitations. Giữ code đủ nhỏ để người dùng giải thích đường API→UI→playback event.
```

## Prompt C — nối Web Admin có sẵn, không viết lại

```text
Tích hợp web-admin hiện có theo 00_MASTER_PROMPT.md; không Jira. Owner: web-admin/src/ và test Admin được giao. Đọc PRD UC-06/07 và API contract; kiểm tra LocationList, LocationForm, AnalyticsFeedback/LiveAnalytics, mocks/data.ts, lib/export.ts và components/ui hiện có.
Đọc các skill frontend và local guideline command; review bằng browser/screenshot thật, không phụ thuộc Figma. Giữ nhận diện, props và hành vi UX đã có; không redesign toàn bộ dashboard.

1. Thay list/edit/save/script/upload khỏi localStorage/mock bằng Admin API có quyền. UI mock/scenario nếu còn thì gắn nhãn và tách route/source; không dùng làm mặc định nghiệm thu.
2. DRAFT không đòi đủ fields; Submit name/category/lat/lon/radius hợp lệ, inline/focus lỗi đầu. Upload error giữ toàn form. Thay một language×type không xóa assets khác; dirty guard, keyboard file select/audio preview thật, request-busy state.
3. Banner giải thích published hiện hành tiếp tục phục vụ du khách. Đồng bộ nghĩa là chung dữ liệu/revision/lifecycle; không biến Save Draft/Submit thành publish. Khi dependency ngoài xuất bản fixture revision mới, User đọc bản mới sau invalidation; giữ bản cũ trong lúc pending.
4. Analytics 4 metric, default 30 ngày, period/filter/detail/page query, DB live refresh. Export analytics đúng kỳ và nhãn phạm vi khi feedback chưa tích hợp. Empty data không fake chart; lỗi export retry giữ state.
5. Khi được giao D1: feedback DB list/detail/filter/pagination, server strict NEW→IN_REVIEW→RESOLVED, updatedAt+aria-live; .xlsx đủ period/active filters/all matched pages. Dữ liệu feedback từ fixture dependency ngoài có nhãn, không tạo thêm Tourist submission feature.

AC D0: A601–608, A701/704/706, W05/06, UI01–03. D1: A609/A702/703/705.
Test round-trip reload và assert DB/API từ browser, không chỉ localStorage; two tabs public/Admin kiểm tra published protection. Skill review P0/P1, keyboard và small viewport. Không coi test mock hiện có là evidence lưu thật.
Handoff: files changed, API mapping, demo steps, tests/counts/skips, lỗi và phần còn mock. Không sửa báo cáo chung hoặc root workflow do agent khác sở hữu.
```

## Prompt D — QA, CI/CD và kiểm tra trước demo

```text
Làm QA/integration theo 01_ACCEPTANCE_MATRIX.md và 03_DEMO_AND_REPORT.md, không Jira. Owner: test/workflow/scripts do agent chính chỉ định, không sửa đồng thời file feature owner đang sửa. Nếu nhận lỗi thuộc feature owner, cung cấp repro+expected/actual+file:line để owner sửa; agent chính điều phối thay đổi chung.

1. Đọc source thực và AC; inventory live/mock/unimplemented. Table-driven resolver/guards vét tổ hợp hợp lệ; test real DB/public-private/versions/outbox/status/report. Kiểm tra concurrency đổi nhanh language/mode, cancel, double Play, lost ack/retry và published protection.
2. Playwright thực trên hai web + API/DB, không route.fulfill mọi endpoint để rồi báo integrated. Mocks chỉ cho lỗi có chủ đích/guard tests; ghi đúng evidence. Có mobile/desktop screenshots và review UI, keyboard/axe. Không có khả năng nghe voice thì ghi manual NOT_RUN, không tính headless speech stub như real TTS.
3. Backend JDK21 Maven verify + PostgreSQL/PostGIS test service; web-admin và web-user npm ci/lint/build; Playwright install browsers, start backend+hai web, await readiness và run E2E real. Tách credentials fixture/test DB, không in secret. Xử lý npm.cmd Windows trong playwright.config để chạy Linux CI; báo tất cả skipped.
4. Không đánh giá workflow chỉ qua YAML. Khi có quyền, đọc GitHub Actions run đúng commit/branch, link job/status. Không quyền thì NOT_RUN. Kiểm tra actual artifact version và deployed commit, không dùng run cũ cho diff mới.
5. CD: kiểm tra có workflow/target/secret không. Chuẩn bị config/container/reverse proxy HTTPS/media Range/healthcheck/smoke+rollback nếu nằm trong scope. Không tự thuê dịch vụ/mở public/deploy khi chưa được phép; thiếu target báo BLOCKED, tiếp tục kiểm tra local. Docker Compose local không bằng automated CD.
6. Có phép deploy: đúng SHA đã qua CI → staging → kiểm tra public QR từ điện thoại, API/audio/TTS, Admin auth/save/published protection, event/report/export, persistence sau restart và rollback app không rollback phá schema. Lưu run URL/time/health/smoke. Không publish secrets hoặc public Admin bypass.

Cuối phiên bắt buộc bàn giao báo cáo ngắn: chức năng đã demo/AC, PASS/FAIL/NOT_RUN/BLOCKED/MOCK_ONLY, log evidence, CI và CD riêng, risks/next action. Cập nhật SESSION_REPORT.md qua agent chính; không tuyên bố READY toàn PRD nếu còn D1 thiếu.
```

## Prompt tiếp tục ở phiên sau

```text
Đọc docs/vi/web-demo-prompts/SESSION_REPORT.md, API_CONTRACT.md và 00_MASTER_PROMPT.md; xác minh source/git status hiện tại. Tiếp tục từ AC còn thiếu ưu tiên cao nhất, bảo toàn phần đã đạt; không khởi động lại thiết kế hoặc tạo Jira. Chạy lại checks chỉ khi có thay đổi hoặc bằng chứng cũ không còn phù hợp. Kết thúc bằng báo cáo ngắn cập nhật đúng commit và môi trường.
```
