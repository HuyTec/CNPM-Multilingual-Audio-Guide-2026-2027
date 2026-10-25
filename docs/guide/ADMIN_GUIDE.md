# Web Admin — biên tập, duyệt và thống kê

Hợp đồng HTTP và JSON: [API_list.md](API_list.md). Backend/state/schema: [BACKEND_GUIDE.md](BACKEND_GUIDE.md). Đây là React/Vite/TypeScript đang có, không viết lại giao diện.

## Cấu trúc và trang

| File | Trách nhiệm |
|---|---|
| [App.tsx](../../web-admin/src/App.tsx) | Router/layout/skip link; danh sách, form, reviews, analytics; menu duyệt theo role. |
| [AdminAccess.tsx](../../web-admin/src/components/AdminAccess.tsx) | Form username/password, POST login, busy/error/focus; không tạo Basic header. |
| [AdminProvider.tsx](../../web-admin/src/components/AdminProvider.tsx) | Token trong bộ nhớ tab; nạp config và identity; logout thu hồi token. Reload cần đăng nhập lại. |
| [admin-content.ts](../../web-admin/src/lib/admin-content.ts) | Context, ContentLocation/ContentConfig và adminRequest gắn bearer; báo lỗi và giữ dữ liệu form. |
| [LocationList.tsx](../../web-admin/src/pages/LocationList.tsx) | q/status/page trong URL; lọc, thống kê trạng thái, readiness từng ngôn ngữ, phân trang. REVIEWER không thấy nút thêm. |
| [LocationForm.tsx](../../web-admin/src/pages/LocationForm.tsx) | Tên theo ngôn ngữ, category/GPS/radius, script FULL/SHORT và audio; nháp/gửi duyệt, guard chưa lưu, optimistic editVersion. |
| [ReviewQueue.tsx](../../web-admin/src/pages/ReviewQueue.tsx) | REVIEWER xem queue, chọn location trong query, so sánh names/script/POI cũ và đề xuất; comment; approve/reject; jobs và retry. Làm mới mỗi 5s. |
| [AnalyticsFeedback.tsx](../../web-admin/src/pages/AnalyticsFeedback.tsx) | Trang thống kê, kỳ báo cáo và đường mock có chọn rõ. |
| [live-analytics.ts](../../web-admin/src/lib/live-analytics.ts), [LiveFeedback.tsx](../../web-admin/src/components/LiveFeedback.tsx) | Thống kê thật refresh 5s và phản hồi API thật, query ngày/category/status/page/detail; cập nhật tuần tự và timestamp. |
| [export.ts](../../web-admin/src/lib/export.ts) | Dynamic import ExcelJS khi xuất; workbook thống kê + toàn bộ phản hồi theo bộ lọc, không chỉ trang đang xem. |
| [components/ui](../../web-admin/src/components/ui), [format.ts](../../web-admin/src/lib/format.ts) | Button busy, Field liên kết label/error, dialog/audio/table/states; Intl vi-VN. |

Routes: /locations với q/status/page; /locations/new và /locations/:id/edit với language/type; /reviews với location/page (20 bản mỗi trang); /analytics với from/to/category/status/page/feedback. source=mock chỉ là lựa chọn kiểm thử rõ ràng ở analytics, không tự fallback khi API lỗi. Không dùng localStorage làm nguồn dữ liệu mặc định cho biên tập.

## Biên tập thủ công hoặc dịch

Form hiển thị hai lựa chọn **Nhập thủ công** và **Dịch tự động từ nội dung nguồn**, đồng bộ `mode=manual|auto` vào URL. Mặc định thủ công; chuyển mode giữ nội dung. Với thủ công, chọn từng ngôn ngữ rồi nhập tên và FULL/SHORT. Với tự động, nhập nguồn rồi chọn các ngôn ngữ đích. Chưa có dịch vụ thật thì provider demo vẫn chỉ giả lập, form hiển thị cảnh báo rõ ràng.

Thẻ **Ảnh địa điểm** tải JPEG/PNG tối đa 5 MB, mỗi chiều tối đa 4096 px và tối đa 12 triệu điểm ảnh. Backend decode rồi mã hóa lại PNG (tối đa 10 MB sau xử lý) và lưu byte trong PostgreSQL bằng migration [V8](../../backend/multi-audio-guide/src/main/resources/db/migration/V8__location_images.sql). `imageUrl` nằm trong DTO bản nháp; kiểm tra ảnh thuộc đúng locationId. Preview cần bearer; ảnh chỉ mở công khai sau publication commit. Thay ảnh trong nháp giữ ảnh public cũ. Lỗi upload giữ nội dung và cho chọn lại. Client cũ không gửi imageUrl vẫn giữ ảnh hiện có.

**Nội dung thuyết minh & TTS** là vùng chính. Web/Android đọc text bằng TTS thiết bị nếu không có audio; phần **Audio thu sẵn (tùy chọn)** giữ lại cho các dữ liệu có sẵn. Dịch văn bản không tự tạo file âm thanh.

Chọn ngôn ngữ đang hiển thị làm nguồn. Nhập tên, FULL và SHORT; tick ngôn ngữ đích rồi bấm Dịch tự động. Nếu đích đã có dữ liệu, phải xác nhận thay thế. Chỉ khóa form trong request; provider lỗi giữ mọi ô. Kết quả demo có tiền tố [DEMO ...] và thông báo đây không phải bản dịch thật. Mọi ô vẫn sửa được. Các khóa fieldOrigins là name:vi hoặc script:en:FULL; giá trị MANUAL, AUTO_TRANSLATED, EDITED_AFTER_TRANSLATE được lưu trong workspace và hiển thị trên form/review. Đây là dấu vết thao tác do client khai báo, không phải chứng nhận chất lượng dịch.

Tọa độ/bán kính do người nhập xác nhận. Audio upload riêng theo locationId/language/type, preview dùng blob đã xác thực. Đổi audio một biến thể không xóa biến thể khác. Audio lỗi không làm mất script. Lưu nháp thiếu được; gửi duyệt chặn thiếu tên/category/GPS/radius, inline error và focus trường đầu. Save gửi editVersion: 409 giữ dữ liệu đang nhập để người dùng mở lại đối chiếu. Điều hướng/rời tab có thay đổi chưa lưu phải xác nhận. Lần lưu đầu giữ query ngôn ngữ và thông báo thành công khi chuyển sang trang sửa.

## Duyệt và công bố

REVIEWER mở Duyệt và xuất bản, chọn bản PENDING_REVIEW. Xem tên đa ngôn ngữ, category/vị trí/radius và kịch bản so với bản đang public. Từ chối bắt buộc nhận xét; EDITOR thấy REJECTED + comment và sửa/gửi lại. Chấp thuận cần xác nhận, backend trả BUILDING job và workspace APPROVED; worker mới tạo/công bố gói. Job READY → workspace PUBLISHED. FAILED hiển thị lỗi và nút thử lại. Gói/data cũ được giữ khi lỗi. UI ẩn/khóa thao tác trái role; backend mới là nơi quyết định quyền.

## Trạng thái và kiểm thử

Runtime local ngày 25/10/2026: Admin `3001` dùng stack `hvp-live-demo`. Admin `3000` có thể dùng cùng backend qua [compose.admin-shared.yaml](../../compose.admin-shared.yaml) và [nginx-admin-shared.conf](../../scripts/nginx-admin-shared.conf). DB legacy của stack gốc có lịch sử migration khác (V4/V4.1/V4.2); không repair/reset DB đó để ép chạy code nhánh này. Khởi động UI dùng chung sau khi stack live-demo healthy:

```powershell
docker compose --env-file .env.demo.local --env-file .env.admin.local -f compose.yaml -f compose.admin-shared.yaml -p congnghephanmem_2026-2027 up -d --no-deps web-admin
```

Tài khoản `admin` vai trò EDITOR được cấp trong DB demo đang chạy theo yêu cầu người dùng; mật khẩu lưu trong cấu hình local bị Git bỏ qua, không ghi vào tài liệu. Tài khoản này không tự xuất hiện trên DB mới: seed mặc định của source vẫn là editor/reviewer.

List có loading/empty/error/retry/long + pagination; form có loading/error/field errors/dirty/busy; review có queue rỗng/loading/error/retry, nội dung dài ngắt dòng và details; jobs tối đa 50. Analytics/feedback có empty theo kỳ, lỗi API, retry xuất và aria-live khi đổi trạng thái. Mật khẩu seed và giới hạn xác thực xem Backend Guide.

```powershell
cd web-admin
npm.cmd ci
# API_PROXY_TARGET=http://127.0.0.1:8081 trong .env.local nếu dùng live-demo
npm.cmd run dev
npm.cmd run build
npm.cmd run lint
npm.cmd run test:e2e -- --workers=1
# Với backend TEST RIÊNG đang chạy ở 18081, có seed accounts mặc định
$env:PUBLICATION_API='http://127.0.0.1:18081'
npx.cmd playwright test --config playwright.publication.config.ts
```

[admin-fixture.ts](../../web-admin/tests/admin-fixture.ts) dùng HTTP fixtures cho hồi quy UI, không chứng minh PostgreSQL. [publication.live.spec.ts](../../web-admin/tests/publication.live.spec.ts) dùng backend/DB thật để biên tập → duyệt → browser polling/tải/xác minh và QR APK. Không chạy suite live trên dữ liệu quan trọng: nó tạo POI. Config live dùng output riêng tránh hai tiến trình xóa artifact của nhau. Còn cảnh báo chunk ExcelJS khoảng 930KB dù đã lazy-load; kiểm tra Chromium/axe không thay thế screen reader và trình duyệt/thiết bị khác.
