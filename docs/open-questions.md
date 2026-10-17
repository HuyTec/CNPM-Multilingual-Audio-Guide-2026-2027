# Điểm cần xác nhận — demo Web Admin

Nguồn đối chiếu cho lần đồng bộ này: [PRD tiếng Anh](en/PRD_Report.md); [bản tiếng Việt](vi/PRD_Report_vi.md) được dịch theo bản đó. Các ghi chú fixture dưới đây mô tả lựa chọn của demo, không khẳng định đã đáp ứng PRD mới và không thay đổi triển khai.

1. **Đường dẫn PRD:** AGENTS.md còn gọi docs/PRD.md và docs/PRD_Report.md nhưng file thật là docs/en/PRD_Report.md và docs/vi/PRD_Report_vi.md. Lần cập nhật tài liệu này dùng bản tiếng Anh làm nguồn theo yêu cầu người dùng.
2. **Đăng nhập/phân quyền:** PRD mục 2.3 và 6.4 phân biệt Content Admin / Editor (UC-06), System Admin / Product Owner (UC-07), giao xác thực/phân quyền cho Admin Authentication bên ngoài. Màn hình đăng nhập, provider và hợp đồng tích hợp chưa được quy định. Demo giả định phiên Admin, không tạo login.
3. **Ngôn ngữ/danh mục:** PRD giả định đã cấu hình nhưng không liệt kê. Fixture minh họa dùng vi/en/fr và Văn hóa/Lịch sử/Ẩm thực; chưa phải cấu hình sản phẩm được chốt.
4. **Audio:** PRD mục 5.2 mô tả AudioAsset tùy chọn gắn với GuideScript theo locationId + languageCode + scriptType; NFR-14 đề xuất MP3 hoặc M4A (AAC), bitrate [64–128] kbps. Các giá trị này còn cần xác nhận; giới hạn dung lượng/thời lượng tham chiếu UC-06 BR7 chưa tồn tại. Fixture demo dùng MP3/WAV/OGG, chưa áp dụng giới hạn dung lượng; không coi đây là allowlist sản phẩm đã chốt.
5. **Package version và publication:** PRD mục 5.1 và 6.4 đã giao việc duyệt, cấp phiên bản và xuất bản cho Content Publication Service bên ngoài; UC-06 chỉ lưu DRAFT/PENDING_REVIEW và giữ bản published. Tính duy nhất của packageVersion, khóa/bội số và hợp đồng tích hợp cụ thể còn cần xác nhận. Demo giữ published riêng và nhãn working version, không tạo flow duyệt.
6. **Bán kính/tọa độ:** kiểm tra GPS trong giới hạn vật lý và bán kính dương để minh họa dữ liệu hợp lệ; PRD chưa chốt min/max bán kính hay độ chính xác.
7. **Analytics:** PRD mục 5.2 đã đề xuất trường AnalyticsEvent và Feedback; timestamp của hợp đồng sự kiện được lưu ở occurredAt. Định nghĩa eventType, loại lượt phát, taxonomy feedback, múi giờ/boundary báo cáo và layout XLSX vẫn chưa được hoàn thiện. UC-07 A3 yêu cầu xuất theo kỳ hiện tại và áp dụng bộ lọc phản hồi đang bật. Fixture dùng lượt phát mock theo ngày, ngày vi-VN/Asia_Ho_Chi_Minh bao gồm hai đầu kỳ, danh mục minh họa; workbook hai sheet Analytics/Feedback. Đây là lựa chọn trình diễn, cần xác nhận cho production.
8. **Lưu trữ demo:** localStorage chỉ phục vụ demo, không thay thế API; file audio preview chỉ còn trong phiên tab, metadata giữ qua reload. Chưa có upload thật hay dữ liệu khách du lịch thật.

## Điểm chưa rõ trong bản PRD tiếng Anh khi đồng bộ ngày 17/10/2026

Các điểm sau được giữ nguyên ý trong bản dịch; chưa tự bổ sung hoặc sửa yêu cầu của bản nguồn:

- **UC-01, luồng chính bước 1:** Tham chiếu A2, A3, A4 nhưng chỉ định nghĩa A1–A3; cần xác nhận lại nhãn tham chiếu.
- **UC-05 E2:** Banner tiền cảnh dẫn đến A2, trong khi mô tả banner nằm ở A1; cần sửa tham chiếu tại nguồn.
- **Map View ngoại tuyến:** Mục 2.3 vừa nhắc nền đơn giản không có map tiles vừa ghi chỉ trực tuyến; NFR-04 quy định Map View cần mạng và ngoại tuyến dùng danh sách. Cần thống nhất cách diễn đạt tại nguồn.
- **Giới hạn audio:** NFR-14 dẫn UC-06 BR7 nhưng UC-06 chỉ có BR1–BR4; chưa có giới hạn dung lượng hoặc thời lượng hợp lệ để dịch theo tham chiếu này.
- **Phụ lục chưa hoàn thiện:** Mục 8 mới có phần giới thiệu và tiêu đề 8.1, chưa có nội dung chi tiết. Các phần được dẫn chiếu về ngôn ngữ/cờ hỗ trợ, loại sự kiện, cấu hình, hợp đồng dữ liệu, quyền, truy vết, rủi ro và giao diện chưa có nội dung. Các mục tiêu trong ngoặc vuông, dữ liệu danh mục, tính duy nhất packageVersion và thông tin triển khai cũng còn chờ xác nhận/điền.
- **Tham chiếu sơ đồ:** Mục 5.1 còn để trống liên kết ERD; các tên UC01_activity.drawio đến UC07_activity.drawio được ghi trong đặc tả chưa có tệp riêng tương ứng trong docs. Tệp sơ đồ hệ thống hiện có là [System_Analysist.drawio.xml](en/System_Analysist.drawio.xml); chưa mặc nhiên coi nó đã khớp toàn bộ PRD mới.
