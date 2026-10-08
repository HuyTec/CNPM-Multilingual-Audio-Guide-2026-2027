# Review Web Admin — Part 0–5

Nguồn: `en/PRD_Report.md` UC-06/UC-07, `ui-design.md`, `.agents/skills/web-design-guidelines/command.md`. Đã tải lại [quy tắc Vercel](https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md) ngày 07/10/2026. Không áp dụng Hydration Safety và Content & Copy theo AGENTS.md.

## Phạm vi theo phần

| Phần | Kết quả / phạm vi review |
|---|---|
| 0 | Chạy `web-admin/scripts/setup-skill.sh` từ root bằng Git Bash; thêm Git usr/bin vào PATH. Đọc SKILL.md và command.md trước UI. |
| 1 | `ui-design.md`: sitemap, routes/query, trạng thái và A/E, shared components, ánh xạ Vercel. `open-questions.md`: vấn đề PRD chưa chốt. |
| 2 | `web-admin/src/components/ui/*.tsx`, `lib/format.ts`, layout App và index.css: semantic controls, label/error/live region, modal native, keyboard, focus, skip link, reduced motion. |
| 3 | `pages/LocationForm.tsx`, `pages/LocationList.tsx`, `mocks/data.ts`: UC-06 A1/A2/E1/E2 và BR1–4. |
| 4 | `pages/AnalyticsFeedback.tsx`, `lib/export.ts`: UC-07 A1/A2/A3/E1/E2 và BR1–3 trong phạm vi mock. |
| 5 | Đọc lại source mới tạo/sửa, lint, TypeScript build, Playwright, axe, kiểm tra ảnh desktop/mobile. |

## Các vấn đề phát hiện và đã sửa

Vị trí bên dưới dùng source cuối cùng sau định dạng; mô tả ghi lại vấn đề trước khi sửa.

### P0

Không phát hiện vấn đề P0 trong phạm vi demo. BR4 giữ published riêng và không có thao tác publish. Không có real API/auth; không coi phiên mock là bảo mật production.

### P1 — resolved

- `web-admin/src/pages/LocationForm.tsx:81` - guard chặn cả đổi query ngôn ngữ sau khi nhập; chỉ chặn khi rời pathname, giữ cảnh báo đóng tab.
- `web-admin/src/pages/LocationForm.tsx:148` - retry đọc ref trong render và có thể ghi đè text bằng snapshot cũ; đổi pending file thành state, merge chỉ audio vào asset hiện tại.
- `web-admin/src/pages/LocationForm.tsx:168` - tệp đúng extension nhưng hỏng nội dung có thể đi vào preview; thêm kiểm tra browser decode, lỗi giữ form và chọn lại/retry.
- `web-admin/src/components/ui/ConfirmDialog.tsx:33` - hai dialog dùng chung ID heading; useId tạo liên kết nhãn riêng.
- `web-admin/src/pages/LocationForm.tsx:318` - thay đổi field trong lúc lưu có thể bị snapshot cũ ghi đè; khóa field trong request lưu, hành động có spinner.
- `web-admin/src/index.css:105` - chữ phụ thiếu tương phản ở sidebar/footer/footnote; tăng tương phản theo kết quả axe.
- `web-admin/src/index.css:272` - eyebrow chỉ đạt 4.44:1; đổi sang xanh đậm hơn.
- `web-admin/src/mocks/data.ts:76` - packageVersion asset published không khớp version published; fixture published dùng 1-demo.

### P2 — resolved

- `web-admin/src/lib/status.ts:1` - constants trong file component gây lỗi Fast Refresh; tách constants khỏi StatusBadge.
- `web-admin/src/pages/LocationList.tsx:143` - GPS hiển thị chuỗi floating-point dài; format bằng Intl vi-VN, tối đa 6 số lẻ.
- `web-admin/src/pages/LocationForm.tsx:439` - file picker có nhãn hệ điều hành tiếng Anh; thêm nút chọn file tiếng Việt, input native có label.
- `web-admin/src/pages/LocationForm.tsx:241` - loading form thiếu h1; thêm heading khi đang tải.

## Kiểm tra PRD

| Yêu cầu | Bằng chứng |
|---|---|
| UC-06 A1 / E2 | Kiểm thử focus tên khi thiếu field; lưu DRAFT thiếu dữ liệu và đọc localStorage sau reload. |
| UC-06 A2 / E1 / BR4 | Edit loc-1, thay audio en:FULL, retry giữ text; vi:FULL và published không đổi; chỉ working thành PENDING_REVIEW. WAV fixture hợp lệ. |
| Nội dung FULL/SHORT | Asset key locationId/languageCode/scriptType/packageVersion, selector URL; không ghi đè các key khác. |
| Chưa lưu | beforeunload và router guard; Escape ở lại form, xác nhận mới rời trang. |
| UC-07 BR2 | NEW → IN_REVIEW → RESOLVED, timestamp lưu; reload chi tiết giữ trạng thái, không có nút skip/reopen. |
| UC-07 A1 / E1 | Mặc định from/to 30 ngày; đổi kỳ ngoài dữ liệu hiện EmptyState cho cả metric và feedback. |
| UC-07 A2 | category/status/page/feedback trong URL; 10 rows/trang, chi tiết từ URL. |
| UC-07 A3 / E2 | XLSX đọc lại bằng ExcelJS, hai sheet, ngày đúng kỳ, toàn bộ feedback kỳ vượt một trang; retry lỗi giữ state. |
| Vercel | Native audio/dialog/controls, lỗi inline/live, focus, skip link, bảng thay thế chart; screenshot và kiểm tra không overflow ở 390px. |

## Giới hạn và P2 còn lại

- `web-admin/src/lib/export.ts:3` - chunk ExcelJS khoảng 930 kB minified. Đã lazy-load chỉ khi xuất, không nạp trong bundle ban đầu; còn cảnh báo chunk size khi build.
- `web-admin/package-lock.json:1` - npm audit báo 2 moderate qua ExcelJS → uuid. Chưa ép downgrade major hoặc override dependency; cần đánh giá thay thư viện/phiên bản cho production.
- Kiểm tra Chromium không thay thế kiểm tra Safari/Firefox, thiết bị touch thực, screen reader thực hoặc chất lượng audio bằng nghe thủ công.
- Dữ liệu giả lập, cấu hình audio/ngôn ngữ/danh mục và version vẫn cần xác nhận ở `open-questions.md` trước khi nối backend.

## Kết quả xác minh cuối

- `npm.cmd run lint`: PASS, không có lỗi/warning ESLint.
- `npm.cmd run build`: PASS TypeScript + Vite; còn cảnh báo chunk ExcelJS như P2 ở trên.
- `npm.cmd run test:e2e`: **9/9 PASS**, Chromium, 54.1 giây trong vòng cuối.
- Sau bổ sung tỷ lệ offline/online vào workbook: lint/build PASS và kiểm thử A3 XLSX PASS riêng.
- Sau đổi toàn bộ font sang Arial: build PASS; kiểm thử dữ liệu dài/mobile, axe và screenshot/keyboard PASS (3/3). Đã xem lại screenshot desktop Arial.
- Axe WCAG 2 A/AA và 2.1 AA: không phát hiện violation trên LocationList, LocationForm, AnalyticsFeedback và dialog phản hồi đã kiểm tra. Đây là kiểm tra tự động, không thay thế screen reader thực.
- Đã xem ảnh ba màn hình desktop và AnalyticsFeedback mobile 390px có dữ liệu dài; kiểm thử xác nhận page không tràn ngang, bảng có vùng cuộn riêng.
- Review cuối: **P0 còn 0; P1 còn 0; không có P0/P1 mới**. P2 còn 2 mục dependency/chunk ở trên. Điểm PRD chưa chốt được tách vào open-questions; không coi fixture là quyết định production.

Các vòng đầu có lỗi harness do hai tiến trình cùng dùng test-results và selector đếm cả bảng phụ trợ chart; đã chạy tuần tự, scope selector đúng bảng phản hồi. Những vòng lỗi này không được tính là bằng chứng luồng thành công. Vòng cuối 9/9 pass được chạy trên source đã sửa.
