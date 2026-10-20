# Demo, điều kiện kết luận và báo cáo cuối phiên

## Chuẩn bị một lần, kiểm tra lại trước buổi diễn

- Chọn browser/OS/điện thoại thật, ghi version và voice VI/EN; thử nghe trước, không đợi đến lúc đứng trình bày. Chọn URL HTTPS reachable ngoài máy dev và QR đúng locationId; không dùng localhost trên điện thoại.
- Có bộ fixture một địa điểm × VI/EN × FULL/SHORT, audio hợp lệ, bài thiếu/lỗi audio, trường hợp fallback/no-approved, bản public cũ và bản sửa draft; không dùng nội dung sai như tư liệu du lịch chính thức. Không seed giả lượt nghe rồi gọi là người dùng thật.
- Chọn period có event demo, ghi tổng đầu; giữ DB/asset volume. Chuẩn bị Admin có quyền qua config/dependency auth, không nhúng mật khẩu vào bundle hoặc ghi ra báo cáo.
- Có cách chủ động gây audio lỗi và publication fixture nội bộ; giải thích rõ chúng là test/dependency ngoài. Không để công cụ lỗi/test hoặc bypass publish public trong UI sản phẩm.
- Fixture test chạy ở DB riêng; script demo không reset DB/volume người dùng. Có phương án khôi phục app/config trước demo mà không xóa dữ liệu.

## Kịch bản D0, khoảng 8–12 phút

| Bước | Thao tác | Kết quả và bằng chứng |
| --- | --- | --- |
| 1 | Dùng camera điện thoại quét QR | HTTPS mở đúng bài, không login/cài APK/GPS; refresh URL vẫn đúng; không tự phát |
| 2 | Đổi VI/EN và FULL/SHORT; thử bản dịch thiếu | Text APPROVED đúng biến thể; fallback hiển thị effective language và giữ mode; video/screenshots cùng contentId |
| 3 | Bấm Nghe, pause/resume, seek và tốc độ | Audio đúng bài; controls tác động thật; không đổi số lượt chỉ vì pause/resume |
| 4 | Chọn fixture không có/lỗi audio | Browser TTS đọc nguyên văn đúng giọng; nhãn nguồn rõ, không phát chồng; có bằng chứng nghe trực tiếp |
| 5 | Thử thiếu giọng hoặc voice lỗi có chủ đích | Text/lựa chọn được giữ, lỗi và Retry; không Playing giả; thay voice/error adapter phải ghi là test mô phỏng lỗi, không thay bước nghe thật |
| 6 | Mở Admin, xem số liệu kỳ hiện tại và XLSX | DB có eventId/contentId thật; retry cùng event không tăng; file export khớp kỳ/tổng; không seed count |
| 7 | Admin sửa một bài/audio, lưu DRAFT rồi gửi duyệt | Reload vẫn còn; bản public cũ vẫn hoạt động; thay một biến thể không mất biến thể khác; invalid submit/upload giữ form và focus lỗi |
| 8 | Thao tác publication fixture bên ngoài UC-06 | Công khai nói đây là dependency mô phỏng; script/audio mới đồng bộ, cache không giữ nhầm bài; không nút Editor tự duyệt |
| 9 | Thử QR sai/no-approved và link APK nếu đã có | Error/manual recovery đúng; no-approved không nghe; tải APK thật nếu link sẵn sàng, không bắt cài để nghe |

Nếu chưa có public URL thì thay bước 1 bằng local/LAN có nhãn và báo `Public QR = BLOCKED/NOT_RUN`, không tuyên bố khách bên ngoài truy cập được. Nếu chưa có APK thì không demo bước tải và không đánh dấu W04 đạt.

## Kịch bản D1, chỉ trình diễn phần có evidence

1. Online chuẩn bị baseline có consent và xác minh → tắt mạng → reload/list/text/audio; thử TTS đúng giọng offline. Lần đầu vào offline hoặc storage bị xóa báo chưa sẵn sàng.
2. Update có version/size/confirm; thử mất mạng/gói sai hash/signature/storage fail: bản cũ vẫn đọc/nghe, tải dở bị bỏ. Xóa optional không xóa base.
3. Nearby có fix/accuracy và radius 100–500 m; GPS denied vẫn search/category/QR. Map marker online đúng ID, offline dùng list.
4. Active-session entry → banner đúng language/locationId/cooldown; không click thì không điều hướng, click không autoplay. Nêu rõ background/closed-tab chưa hỗ trợ nếu chưa kiểm chứng.
5. Feedback lấy từ dependency fixture có nhãn; filter/status/time thật; thử server từ chối skip NEW→RESOLVED; export cả period+filters/all pages.

## Go / No-Go

| Tuyên bố | Đủ điều kiện khi |
| --- | --- |
| D0 local demo READY | Mọi AC D0 chức năng áp dụng đã PASS, nghe TTS thật trên môi trường chọn; có auth/DB/published protection; không còn P0/P1 trong luồng demo. Public QR/APK/CI remote/CD phải báo riêng nếu chưa có evidence |
| Public Web demo READY | D0 chức năng + public HTTPS/QR/audio/API từ điện thoại khác; secret/auth an toàn theo cấu hình, deployed commit và smoke xác minh. Không cần ngụy tạo CD tự động nếu deploy thủ công |
| UC-01–07 hoàn thành | Mọi AC PRD tương ứng gồm D1 và điều kiện nền tảng đã được kiểm chứng; không lấy D0 thay toàn PRD |
| CI PASS | Run GitHub đúng commit/branch, các job cần thiết chạy và đạt; tests quan trọng không bị skip; URL/run/status lưu lại |
| CD PASS | Có automated deployment run đúng artifact/SHA, healthcheck và smoke URL sau deploy; Compose local/manual deployment không đủ |

P0: lộ quyền/secret, mất published/base data, sai bài/ngôn ngữ nghiêm trọng, không khởi động được. P1: luồng chính hoặc TTS bắt buộc lỗi, state sai, đếm trùng, không lưu thật, lỗi export/validation làm không thể demo. Sửa P0 rồi P1; nếu blocked, nêu rõ và không đánh READY. Có thể bàn giao tiến độ partial để tiếp tục, không cần giữ cả phiên im lặng chờ hạ tầng.

## Mẫu SESSION_REPORT.md — khoảng một trang

```markdown
# Báo cáo phiên Web demo — YYYY-MM-DD HH:mm (Asia/Saigon)

**Phạm vi phiên:** [mốc/AC được giao]. **Kết luận:** [D0 local / Public Web / D1 / partial; READY hoặc NOT_READY].
**Source:** [branch + commit SHA; nếu local diff chưa commit phải nêu].
**Môi trường:** [URL/DB test riêng/OS/browser/device/voice; không secret].

| Phần đã thực sự demo | Kết quả/AC | Evidence |
| --- | --- | --- |
| QR/text/language/FULL–SHORT | [...] | [URL/ảnh/video/contentId] |
| Audio/TTS/controls/errors | [...] | [browser+voice, bằng chứng nghe thật hoặc NOT_RUN] |
| Admin save/upload/published protection | [...] | [API/DB round-trip/revision] |
| Playback/analytics/XLSX | [...] | [eventId/query/export] |
| Offline/location/feedback nếu có | [...] | [...] |

| Kiểm tra | PASS / FAIL / skipped hoặc NOT_RUN | Evidence |
| --- | --- | --- |
| Backend unit + real DB integration | [counts riêng] | [command/log/report] |
| Web user/Admin lint + build | [...] | [...] |
| E2E real backend / mock-only UI | [tách riêng] | [...] |
| Manual TTS / QR / responsive | [...] | [...] |

**CI:** [PASS/FAIL/NOT_RUN/BLOCKED] — [run URL, SHA, job results/skips; hoặc chỉ YAML/local được kiểm tra].
**CD:** [PASS/FAIL/NOT_CONFIGURED/NOT_RUN/BLOCKED] — [deployment run/artifact/URL/health/smoke; nêu manual nếu manual].
**Code chính:** [tối đa 5 file + trách nhiệm; flow API→UI→player→event→DB→Admin].
**Còn thiếu:** [tối đa 3 mục quan trọng, tách mock/chưa chạy/lỗi thật].
**Bước tiếp theo:** [một hành động cụ thể].
```

Luôn điền báo cáo khi kết thúc một phiên triển khai, kể cả bị chặn; không dùng lại số test/ngày cũ như vừa chạy hôm nay. Không ghi “CI/CD tốt” nếu chỉ có build local. Lưu artifact nặng vào thư mục output bị ignore hoặc report CI, báo cáo chỉ link evidence không secret.

## Tài liệu kỹ thuật để agent kiểm tra khi triển khai

- [SpeechSynthesis](https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesis): API voice/speak/pause/resume/cancel; không có phương thức seek theo giây tương đương audio element.
- [SpeechSynthesisEvent.charIndex](https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesisEvent/charIndex): điểm bắt đầu cho theo dõi offset; behavior thực tế cần test trên engine/browser chọn.
- [SpeechSynthesisVoice.localService](https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesisVoice/localService): thông tin loại voice, không thay kiểm tra nghe offline thật.
- [Web Interface Guidelines](https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md): đối chiếu thêm với command local và exceptions của AGENTS.

Các tài liệu trên hỗ trợ thiết kế adapter/test; không thay quy tắc UC-03 và không chứng minh tính năng đã được triển khai.
