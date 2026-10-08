# Review LocationList — UC-06

Ngày: 07/10/2026. Đối chiếu UC-06 trong `en/PRD_Report.md`, `ui-design.md`, bảy yêu cầu chỉnh sửa của người dùng và `.agents/skills/web-design-guidelines/command.md`. Đã đọc lại quy tắc Vercel hiện hành; bỏ Hydration Safety và Content & Copy theo AGENTS.md. Áp dụng frontend-ui-engineering, frontend-design-principles và checklist React best practices.

## Phạm vi

`web-admin/src/pages/LocationList.tsx`, `components/locations/{LocationRow,LanguageCoverage}.tsx`, `src/index.css`, `src/App.tsx`, `src/mocks/data.ts`, thay đổi metadata audio ở `LocationForm.tsx`, fixtures và kiểm thử. Giữ Arial, logic tìm kiếm/lọc/phân trang, workflow DRAFT/PENDING_REVIEW và bảo vệ bản published. Chỉ bổ sung timestamp khi lưu; không reset dữ liệu localStorage đã có.

## Vấn đề và xử lý

### P0

Không phát hiện.

### P1 — resolved

- `web-admin/src/components/locations/LanguageCoverage.tsx:70` - chip cần mô tả đầy đủ cho trình đọc màn hình và bàn phím: thêm aria-label, aria-describedby, focus, tooltip và Escape.
- `web-admin/src/components/locations/LocationRow.tsx:20` - yêu cầu cả hàng mở form phải giữ hành vi liên kết: dùng Link với vùng phủ CSS, không dùng click trên tr; nút Chỉnh sửa là Link độc lập có nhãn tên địa điểm.
- `web-admin/src/pages/LocationList.tsx:79` - số liệu cần lọc được và có trạng thái truy cập: button, aria-pressed, focus-visible; dùng hàm filter hiện có để giữ query tìm kiếm/reset page.
- `web-admin/src/index.css:1167` - khoảng cách giữa chip và tooltip có thể làm mất hover khi di chuyển con trỏ: thêm vùng nối trong suốt; tooltip vẫn đóng được bằng Escape.

### P2 — resolved

- `web-admin/src/App.tsx:73` - topbar chứa nhãn kỹ thuật của demo: bỏ nhãn; LocationRow không hiển thị ID/version.
- `web-admin/src/pages/LocationList.tsx:101` - tìm kiếm và select không đồng bộ style: dùng cùng location-filter-control.
- `web-admin/src/components/locations/LocationRow.tsx:40` - GPS cần cùng dòng và tên đại lượng: nowrap, tabular-nums, Intl, nhãn Lat/Lng ẩn thị giác.
- `web-admin/src/components/locations/LocationRow.tsx:55` - thiếu thời điểm cập nhật: time datetime và Intl vi-VN; dữ liệu cũ thiếu timestamp hiện “Chưa ghi nhận”.
- `web-admin/src/index.css:1099` - chữ phụ cần tối thiểu 12px: chỉnh phạm vi danh sách và shell; kiểm tra kích thước computed và tương phản bằng axe.
- `web-admin/src/mocks/data.ts:53` - fixture chưa phản ánh biên tập đa ngôn ngữ: tọa độ đối chiếu nguồn trong location-list-design.md, giờ cập nhật khác nhau, thiếu bản dịch/audio/kịch bản và audio lỗi. VI/EN/FR theo cấu hình hiện tại; audio tùy chọn không chặn gửi duyệt.
- `web-admin/src/pages/LocationList.tsx:169` - biểu tượng chú giải chưa khớp chip: dùng cùng Check/VolumeX/FileText/Minus/TriangleAlert, không chỉ dựa vào màu.

## Xác minh

- Lint và TypeScript/Vite build PASS; cảnh báo chunk ExcelJS đã tồn tại được ghi ở ui-review.md, ngoài phạm vi LocationList.
- Toàn bộ 12 kiểm thử Chromium PASS (56.5 giây): thêm ba kiểm thử LocationList bên cạnh chín kiểm thử hồi quy UC-06/UC-07.
- Sau sửa cuối về tooltip/chú giải: lint/build PASS, ba kiểm thử LocationList PASS, gồm hover tooltip, Escape, native row link, lọc bằng bàn phím, URL/Back, timestamp, aria và chữ phụ.
- Axe WCAG 2 A/AA và 2.1 AA không phát hiện violation trên màn danh sách; bộ hồi quy cũng kiểm tra form, analytics và dialog phản hồi.
- Đã xem ảnh desktop 1440px và mobile 320px; kiểm thử 320/768/1024/1440px không tràn toàn trang. Bảng rộng cuộn trong vùng riêng trên mobile.
- Các lỗi harness (click ô bên dưới Link phủ hàng, selector nhãn không exact, giữ focus trên chip khác khi kiểm tra hover) đã sửa và chạy lại; không tính vòng lỗi là bằng chứng thành công.

Review cuối: **P0 còn 0, P1 còn 0; không có P0/P1 mới.** Không còn vấn đề P2 mới trong phạm vi chỉnh LocationList. Giới hạn dependency/chunk và kiểm tra screen reader/thiết bị thực của demo vẫn được ghi riêng trong ui-review.md.
