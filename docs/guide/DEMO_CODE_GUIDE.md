# Mục lục mã nguồn và kịch bản demo

Cập nhật 25/10/2026 theo code hiện tại. UC-06 đã bổ sung EDITOR/REVIEWER và xuất bản trong hệ thống theo yêu cầu mới; không còn coi phê duyệt là chức năng bên ngoài. Yêu cầu chuẩn: [PRD EN](../en/PRD_Report.md), [PRD VI](../vi/PRD_Report_vi.md). Hồ sơ thiết kế và snapshot lịch sử ở [HVP-21](https://trannhuthuy897.atlassian.net/browse/HVP-21).

## Đọc theo thành phần

| Tài liệu | Nội dung |
|---|---|
| [BACKEND_GUIDE.md](BACKEND_GUIDE.md) | Kiến trúc JDBC ba lớp, class/luồng, ERD, xác thực, migration, chạy/test. |
| [ADMIN_GUIDE.md](ADMIN_GUIDE.md) | Form/dịch/nháp, duyệt, thống kê/phản hồi/XLSX, phân quyền và UI states. |
| [USER_WEB_GUIDE.md](USER_WEB_GUIDE.md) | Danh sách/bản đồ/thuyết minh, polling/offline và tải APK Android bằng QR. |
| [ANDROID_GUIDE.md](ANDROID_GUIDE.md) | Kotlin client chung, GPS/map/player, gói/analytics/polling và build APK. |
| [API_list.md](API_list.md) | Nguồn duy nhất cho method/path/DTO/JSON/lỗi/quyền và component gọi. |

## Khởi động theo thứ tự

1. Mở Docker Desktop, đợi Linux engine hoạt động (`docker version` phải có Client và Server). Từ gốc repo chạy [start-live-demo.ps1](../../scripts/start-live-demo.ps1); script tạo .env.demo.local riêng khi chưa có, khởi động cả PostgreSQL → backend → Web Admin và Web du khách qua một [compose.yaml](../../compose.yaml). Mặc định backend 8081, Admin 3001, Web du khách 14176; tất cả dùng một DB. Xem [DOCKER_GUIDE.md](DOCKER_GUIDE.md). Không in/commit password DB.
2. Đợi health DB UP; mở Admin. Seed mới: editor / EditorDemo!2026 và reviewer / ReviewerDemo!2026. Có thể override DEMO_EDITOR_PASSWORD/DEMO_REVIEWER_PASSWORD trước lần seed đầu; không reset mật khẩu đã tồn tại. Đăng nhập cũ admin/Basic không còn dùng.
3. Chuẩn bị Android bằng [prepare-android-download.ps1](../../scripts/prepare-android-download.ps1) để build APK và copy metadata; đặt API_PROXY_TARGET của Web User tới backend phù hợp, rồi npm ci và npm run dev trong web-user (5175). Để kiểm offline shell, chạy build/preview hoặc public server; dev page mở được chưa chứng minh offline reopen.
4. Muốn người khác tải APK bằng QR, phục vụ Web qua HTTPS/LAN mà điện thoại truy cập được. QR tự theo origin website; localhost của PC không dùng trên điện thoại. Build Web sau khi chuẩn bị APK; xem [USER_WEB_GUIDE.md](USER_WEB_GUIDE.md). APK được gitignore, không tự xuất hiện khi người khác chỉ git clone.

Để public Web du khách, chạy [share-docker-web.ps1](../../scripts/share-docker-web.ps1) sau khi stack healthy. Script mặc định cổng 14176 và chọn Cloudflare Quick Tunnel; không đưa Admin ra internet. `-TouristWeb` của script khởi động chỉ còn để tương thích lệnh cũ; cả hai web luôn được khởi động. Xem kết quả thử tunnel và lệnh dừng trong User Web Guide.

## Kịch bản và kết quả mong đợi

1. **EDITOR tạo POI:** nhập tên VI, category, tọa độ/bán kính và FULL/SHORT. Chọn English làm đích, Dịch tự động. Mặc định demo hiện [DEMO en] và cảnh báo giả lập; sửa tên/script được, nhãn nguồn thành EDITED_AFTER_TRANSLATE. Nhập thủ công mọi ngôn ngữ cũng được. Không tự tạo audio; upload nếu có. Lưu nháp thiếu trường vẫn được; đóng/rời trang khi chưa lưu có cảnh báo.
2. **Gửi duyệt:** thử thiếu trường để thấy inline error; điền đủ và gửi. Kỳ vọng PENDING_REVIEW, dữ liệu chưa xuất hiện trên public API. Hai tab cùng editVersion, tab lưu sau nhận 409 và giữ dữ liệu đang nhập.
3. **REVIEWER quyết định:** mở phiên/tab riêng, đăng nhập reviewer, bấm menu Duyệt và xuất bản (không reload mất phiên). So sánh bản cũ/đề xuất. Từ chối thiếu nhận xét bị chặn; từ chối có comment → EDITOR đọc được REJECTED và sửa/gửi lại. Chấp thuận → APPROVED + BUILDING job; sau worker thành READY thì workspace PUBLISHED. Nếu gói lỗi: FAILED, bản cũ vẫn public, có nút thử lại.
4. **Du khách nhận và tải:** mở Web/Android có mạng; polling trong foreground báo có nội dung mới và dung lượng. Chỉ tải khi xác nhận; gói/checksum/chữ ký/audio phải hợp lệ. Đang nghe thì chờ kết thúc/stop mới thay gói; lỗi giữ gói cũ. Chọn ngôn ngữ để thấy tên POI tương ứng trên list/map/detail và đọc nội dung mới. Web lần đầu phải chuẩn bị shell/data trước offline; Android có BASE bundled.
5. **Lượt nghe, phản hồi và APK:** phát thật một bài rồi kiểm thống kê Admin tăng; gửi lại eventId không đếm thêm. Lọc phản hồi theo kỳ/category/status, chuyển tuần tự và xuất XLSX giữ đúng bộ lọc. Trên Web mở khối Android, kiểm version/dung lượng, quét QR hoặc bấm tải: cùng một APK. APK debug có thể cần cho phép cài từ trình duyệt; không phải Play Store.

## Giới hạn đã biết và bằng chứng

- Tính năng admin/package/playback vẫn nằm trong profile demo kèm fixture; chưa tách cấu hình production. Tài khoản seed công khai chỉ cho demo, chưa có UI quản lý tài khoản/rate limit login, TLS/KMS/backup và quy trình vận hành production. Khóa ký lưu DB, chưa quản lý bằng KMS.
- Provider demo chỉ giả lập, không phải dịch thật; LibreTranslate phải được vận hành/cấu hình riêng và kiểm chất lượng/ngôn ngữ hỗ trợ. Chưa gọi provider thật có credential trong lần kiểm này. Audio upload riêng, TTS phụ thuộc voice và mạng/thiết bị.
- BASE/OPTIONAL hiện chia theo category demo cố định, chưa có UI tạo scope tùy ý. Không có push lúc app đóng; polling foreground không hứa cập nhật tức thời hoặc chạy nền liên tục. Lưu gói/audio BYTEA/Base64 phù hợp demo, chưa tối ưu dữ liệu lớn/CDN.
- Đã chạy Maven/test PostgreSQL riêng và Chromium live cho luồng tạo–duyệt–gói–polling–download; chạy compiler/unit tests/build APK Android. Không coi đó là kiểm GPS/audio/QR/cài APK trên thiết bị thật, Safari/Firefox hay production. Kết quả lệnh cuối được báo trong bàn giao; fixture tests không thay integration tests.
- ExcelJS lazy-load vẫn có cảnh báo chunk khoảng 930KB. APK/downloads là output tạo cục bộ, cần chuẩn bị lại trước build/deploy Web. Sơ đồ PRD cũ mô tả external publication cần vẽ lại theo quyết định mới đã ghi rõ ở UC-06.
- Snapshot trước lần nhập Android từ main (25/10/2026): Maven verify 118 PASS/0 skip trên DB riêng; Web Admin và Web User build/lint PASS; hồi quy Admin 16 PASS/5 gated SKIP, hai kiểm thử bổ sung dịch lỗi/audio PASS; hai kiểm thử Chromium live PASS và kiểm QR/APK qua Cloudflare PASS. Android assembleDebug/testDebugUnitTest/lintDebug PASS (11 tests, 0 lỗi lint, 13 warnings/2 hints). Kiểm tài liệu: 6 file, 110 liên kết nguồn và 29 method/path. Các bước kiểm trên thiết bị thật nêu ở trên chưa thực hiện.
