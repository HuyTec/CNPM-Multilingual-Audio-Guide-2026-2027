# Prompt chính — triển khai Web du khách và đồng bộ Admin

Bạn là agent triển khai trong `E:\Project\CongNghePhanMem_2026-2027`. Hãy thực hiện công việc, không chỉ lập kế hoạch. Nhóm có khoảng một tuần để demo, cần đi nhanh nhưng người dùng vẫn hiểu và kiểm soát code.

## 1. Mục tiêu và nguồn yêu cầu

Xây Web du khách trước: camera điện thoại quét QR → URL địa điểm → đọc text APPROVED, đổi ngôn ngữ/FULL–SHORT → bấm nghe audio hoặc TTS dự phòng → dữ liệu lượt nghe được lưu và xuất hiện trên Web Admin. Có liên kết tải APK nếu có artifact hợp lệ. Đồng bộ Web Admin hiện có với cùng backend/DB/nội dung.

Đọc theo thứ tự:
1. `AGENTS.md` và các hướng dẫn áp dụng trong phần sẽ sửa. Yêu cầu trực tiếp trong prompt này ưu tiên khi xung đột với quy trình cũ.
2. `docs/en/PRD_Report.md`: §2.4, UC-01–07, §5–7; dùng bản VI để hỗ trợ đọc.
3. `docs/vi/web-demo-prompts/01_ACCEPTANCE_MATRIX.md` và `03_DEMO_AND_REPORT.md`.
4. Source backend, Web Admin, demo Android liên quan, `.github/workflows`, Compose và test hiện có. `DEMO_CODE_GUIDE.md` là bản đồ đọc code, không thay PRD.

Không dùng Jira trong đợt này: không đọc, tạo, cập nhật hoặc đổi trạng thái task. Không dừng công việc để quản trị backlog. Ghi checkpoint ngắn trong chat và một báo cáo cuối phiên. Không sửa PRD để hợp thức hóa chức năng chưa đạt.

## 2. Cách thực hiện nhanh và kiểm soát được

- Kiểm tra `git status` trước khi sửa; bảo toàn thay đổi của người dùng. Không reset/rewrite repository, không sửa migration Flyway đã áp dụng. Không tự commit/push/deploy công khai khi chưa được người dùng cho phép.
- Trước mỗi mốc: nêu một flow ngắn, danh sách phần sẽ sửa và AC sẽ đạt. Sau mốc: nêu file chính, dữ liệu đi đâu, kiểm tra nào đạt, phần còn thiếu. Không hỏi lại cho lựa chọn nhỏ/reversible đã nằm trong phạm vi.
- Dùng Controller → Service → Repository và nền React/Vite/TypeScript/Tailwind hiện có. Chỉ thêm abstraction khi được dùng thật; không Kafka, microservices, đổi stack, viết lại Admin hoặc tạo auth/account system ngoài PRD.
- Đề xuất mặc định tạo `web-user/` riêng để route công khai không va chạm `/locations` của Admin. Root Compose phục vụ hai web qua hai service/origin; QA kiểm tra cả hai. Nếu kiểm tra repo cho thấy gộp app tiết kiệm đáng kể, ghi lý do và tách layout/route public-admin rõ ràng trước khi làm. Không migrate monorepo chỉ để chia sẻ vài component.
- Public và Admin dùng chung ID/revision/content, không dùng chung quyền. Không nối Web du khách vào `src/mocks` của Admin. Mạng/API lỗi không âm thầm trả fixture.
- Dữ liệu minh họa được ghi rõ; prepared fixture chứa bài đã duyệt, không giả lịch sử lượt nghe. Dữ liệu mẫu phải đi qua API/DB thật trong test tích hợp.
- Người dùng cho phép chia subtasks cho agents trong phạm vi phiên này. Nếu có công cụ subagent, dùng tối đa 3 nhánh triển khai đồng thời; không tạo chat độc lập của người dùng. Hợp đồng API/fixture/route phải thống nhất trước khi frontend tích hợp. Một file chỉ có một owner; agent chính sở hữu file root/shared và tích hợp. Nếu không có subagent, làm tuần tự cùng các prompt con.

## 3. M0 — baseline, hợp đồng và UI

Chạy/đọc đủ để phân biệt live/mock; kiểm tra endpoint guide/playback hiện đang thuộc profile `demo`, schema seed, auth `/api/admin/*`, audio URL, timezone và outbox. Chọn cách đưa API vào runtime của Web mà vẫn giữ demo Android tương thích; không xóa profile guard tùy tiện hoặc mở Admin ra public.

Chuẩn bị một địa điểm tham chiếu và bộ thử VI/EN × FULL/SHORT. Đây là fixture kiểm thử, không tự chốt danh mục ngôn ngữ sản phẩm. Có đủ trường hợp: audio hợp lệ, thiếu audio, audio lỗi, thiếu bản dịch, không có APPROVED, text dài, DRAFT/PENDING_REVIEW/REJECTED, vị trí/QR sai. Không sửa bản seed đã chạy; dùng migration mới hoặc fixture test riêng có thể reset an toàn trong DB test.

Đóng băng hợp đồng tối thiểu bằng ghi chú ngắn `docs/vi/web-demo-prompts/API_CONTRACT.md`: public location/search/guide; requested/effective language; scriptType; content/revision; optional audio; errors; Admin editing/status/upload; analytics event/retry/report period. Tận dụng API hiện có, không ép đổi tên endpoint cho đẹp. Payload mới phải không làm hỏng APK cũ; fields mới cần default hoặc version rõ.

Trước khi viết UI, đọc skill và command thực tế:
- `.agents/skills/frontend-design-principles/SKILL.md` và `app.md`.
- `.agents/skills/frontend-ui-engineering/SKILL.md`.
- `.agents/skills/web-design-guidelines/SKILL.md` và `command.md`; áp dụng quy tắc local bắt buộc, bỏ Hydration Safety/Content & Copy theo AGENTS; fetch guideline mới khi skill yêu cầu và có mạng.
- Người dùng đã chọn không cài Figma cho đợt này. Không tìm/cài/đề xuất lại plugin, không chờ kết nối Figma. Thực hiện UI bằng ba skill frontend ở trên, tokens/component Admin hiện có và browser preview; dùng screenshot thực để review. Không báo đã tạo hoặc nghiệm thu thiết kế Figma.

Định hướng được giao trong prompt: du khách đang đứng tại địa điểm, ưu tiên đọc/nghe bằng một tay; Admin là công cụ biên tập có mật độ thông tin vừa phải. Giữ nhận diện hiện có (Arial/system fallback, ink xanh đậm, accent teal, nền sáng), spacing nhất quán, border nhẹ; Web user thoáng để đọc, Admin gọn để thao tác. Không landing page quảng cáo lớn, gradient trang trí hoặc dashboard card lặp vô nghĩa. Nêu intent/domain/color/signature và quyết định UI ngắn trước khi code; bảo toàn style/props/logic Admin ngoài phần cần tích hợp.

## 4. M1 — Web chạy xuyên suốt, có TTS thật

Làm AC nhóm W/U2/U3/EVENT trong matrix. `web-user` có màn khám phá tối thiểu, URL địa điểm, đọc/nghe; query params phản ánh language/mode/filter/page. Refresh URL vẫn mở đúng bài; invalid ID không chuyển sang địa điểm khác. Khách không phải đăng nhập/cài APK/cấp GPS để đọc/nghe. QR trỏ đến URL HTTPS khả dụng; mã QR phải được quét thử từ điện thoại khác.

UC-02 chọn bài trước, UC-03 chỉ phát bài đã chọn: Selected → EN → VI giữ nguyên scriptType, mặc định FULL. Thông báo rõ ngôn ngữ thực đang dùng. Không có APPROVED thì khóa Nghe, không gọi TTS tạo nội dung mới.

UC-03 bắt buộc gồm audio ưu tiên và browser TTS dự phòng. Dùng adapter audio/TTS nhỏ, session/state rõ; không thêm cả framework player. Kiểm tra API/voice support, voiceschanged, voice đúng effective language, timeout/error/cancel; offline cần voice đọc không mạng và đã kiểm chứng. Không tự dùng default voice sai ngôn ngữ.

Chỉ một nguồn phát. Khi đổi địa điểm/ngôn ngữ/mode hoặc rời phiên, dừng nguồn cũ và bỏ callback response cũ. Loading có đường thoát. Chỉ ghi nhận playback sau khi thực sự bắt đầu; pause/resume, seek, đổi tốc độ và đổi audio sang TTS trong cùng phiên không tạo lượt mới. Replay chủ động sau khi kết thúc tạo phiên mới. Event UUID giữ nguyên qua retry; server không tin location/language do client tùy ý gửi.

Pause/Resume/seek/tốc độ và giữ vị trí khi gián đoạn áp dụng cho cả audio/TTS theo A1/E2. Web Speech API không có seek theo giây giống file audio: khảo sát trên browser demo, có thể dùng chia đoạn nguyên văn + offset/boundary và phát lại từ vị trí được theo dõi. Không sửa text, không giả timestamp chuẩn xác, không dùng timer chạy giả làm tiến độ thật. Độ chính xác/giới hạn phải có bằng chứng. Nếu không đạt A1/E2 trên browser chọn, ghi AC chưa đạt và tìm phương án khả thi; không ẩn nút rồi coi UC-03 hoàn tất.

## 5. M2 — Admin và Web du khách thực sự dùng chung dữ liệu

Làm nhóm A6/A7/SYNC. Giữ các màn/controls hiện có, thay storage/mock bằng API. Form lưu incomplete DRAFT; Submit kiểm tra name/category/lat/lon/radius, highlight inline/focus lỗi đầu, giữ form khi upload/save lỗi. Audio gắn đúng ngôn ngữ và FULL/SHORT; thay một asset không mất asset khác. Quy tắc media còn trong ngoặc hoặc tham chiếu BR không tồn tại phải báo chưa rõ, không bịa giới hạn bắt buộc.

Thiết kế lưu bản sửa riêng với published snapshot/revision cho cả thông tin địa điểm lẫn script/audio. Chỉ UC-06 không được tự APPROVED. Dùng fixture/script kiểm soát để mô phỏng dependency publication ngoài sản phẩm: label rõ, chọn đúng revision, đảm bảo script/audio cùng phiên bản; không có endpoint public hoặc nút Editor tự duyệt. Sau thao tác mô phỏng, invalidation/cache phải làm Web đọc bản mới; trước đó bản cũ tiếp tục chạy.

UC-07: đủ 4 metric, default 30 ngày, period/filter/detail/page nằm trong URL; feedback từ DB, trạng thái NEW → IN_REVIEW → RESOLVED, timestamp lưu thật. Feedback submission vẫn dependency ngoài: seed/inject qua fixture có nhãn, không thêm form gửi mới nếu chưa được giao. Xuất XLSX lấy đúng period + active feedback filters và toàn bộ trang khớp. Offline/online không được tô số giả; dùng event contract đủ provenance/time và dữ liệu có nguồn.

## 6. M3 — mở rộng dần theo PRD, không che thiếu sót

Làm từng nhóm U1/U4/U5. Baseline Web cần lần online đầu và kiểm tra storage; shell offline, manifest/version/hash/signature, staging/commit nguyên tử, không xóa Base Content; update mất mạng bỏ tải dở và giữ bản hiện hành. TTS offline cần thử giọng thật, không suy từ `localService` đơn thuần.

Nearby dùng tọa độ/accuracy thật trong phạm vi 100–500 m; GPS denied vẫn tìm tay. Map online có pan/zoom/marker; offline dùng list, không tải hàng loạt tile. UC-05 chỉ cam kết phiên active/foreground có hỗ trợ; cooldown theo cấu hình, banner có locationId và ngôn ngữ hiện tại, click mới mở detail, không autoplay. Khoảng cooldown/chữ ký package/provider chưa chốt là cấu hình/quyết định còn mở, không tự gán yêu cầu PRD.

Ưu tiên đóng D0 trước. Tính năng D1 chưa xong ghi chính xác từng AC; không tuyên bố đạt UC-01–07 đầy đủ, không thay PRD. Nếu môi trường chặn một việc, tiếp tục phần độc lập và ghi nguyên nhân, không giảm chất lượng dữ liệu/thẩm quyền để demo xanh.

## 7. M4 — QA, CI/CD và bàn giao mỗi phiên

Đọc prompt QA trong `02_AGENT_PROMPTS.md`. Chạy test phù hợp trên DB test riêng; không ghi/xóa fixture trong DB demo đang dùng. Test mock chỉ chứng minh logic/UI. TTS cần nghe thực trên browser/thiết bị chọn; headless/mock voice không chứng minh giọng phát ra loa.

CI phải bao phủ backend verify + DB integration, lint/build hai web và E2E real backend. Kiểm tra `playwright.config.ts` đang dùng `npm.cmd`: sửa có điều kiện theo OS trước khi đưa lên Linux CI. Browser install, fixture, server readiness và counts skipped phải được kiểm tra. Không `skip`/`continue-on-error`/bỏ assertions để đạt xanh. Không nâng test matrix quá mức cần cho một tuần.

Đánh giá CI/CD theo đúng commit SHA: cấu hình → chạy local → run trên GitHub → deploy staging → smoke trên URL thật là các bằng chứng khác nhau. Không có quyền/token/đích deploy thì báo NOT_RUN/NOT_CONFIGURED/BLOCKED; chuẩn bị workflow/script reviewable, không bịa deploy thành công. Không tự chọn dịch vụ trả phí hay xuất bản public nếu chưa được cho phép. Compose local là triển khai thủ công, không phải CD tự động.

Trước mỗi lần kết thúc phiên, dù xong hay bị chặn:
1. Cập nhật `docs/vi/web-demo-prompts/SESSION_REPORT.md` theo mẫu ở `03_DEMO_AND_REPORT.md`; gọn khoảng một trang, kết quả mới nhất và các giới hạn thật.
2. Cho biết các AC PASS/FAIL/NOT_RUN/BLOCKED/MOCK_ONLY, chức năng đã thực sự demo và môi trường; link log/screenshot/report/run, không copy secret.
3. Báo CI và CD riêng, commit/local diff và điều kiện chưa đủ; một bước tiếp theo cụ thể.
4. Giải thích tối đa 5 file chính: trách nhiệm, input/output và đường dữ liệu; dùng flow ngắn khi cần. Không viết một báo cáo dài thay cho sửa lỗi.

Tự tiếp tục qua các mốc đã giao trong phạm vi phiên; không dừng chỉ vì đã viết được skeleton. Chỉ kết luận D0/D1 READY khi mọi tiêu chí tương ứng có bằng chứng. Bắt đầu bằng M0 ngay.
