# Điểm cần xác nhận — demo Web Admin

1. **Nguồn PRD:** AGENTS.md gọi docs/PRD.md và docs/PRD_Report.md nhưng file thật là docs/en/PRD_Report.md và docs/vi/PRD_Report_vi.md. Demo đối chiếu hai bản; cần xác nhận bản chuẩn khi chúng khác nhau.
2. **Đăng nhập/phân quyền:** PRD chỉ nêu Admin đã được xác thực/ủy quyền. Chưa quy định màn hình đăng nhập, provider hay quyền Editor/System Admin. Demo giả định phiên Admin, không tạo login.
3. **Ngôn ngữ/danh mục:** PRD giả định đã cấu hình nhưng không liệt kê. Fixture minh họa dùng vi/en/fr và Văn hóa/Lịch sử/Ẩm thực; chưa phải cấu hình sản phẩm được chốt.
4. **Audio:** chưa quy định allowlist, codec, giới hạn dung lượng hay quan hệ audio FULL/SHORT. Fixture demo dùng MP3/WAV/OGG, chưa áp dụng giới hạn dung lượng; tách audio theo ngôn ngữ × FULL/SHORT để thể hiện BR3. Cần xác nhận trước backend.
5. **Package version và publication:** BR3/BR4 yêu cầu liên kết phiên bản và giữ bản published, nhưng chưa có hợp đồng cấp version/approval. Demo giữ published riêng và nhãn working version, không tạo flow duyệt.
6. **Bán kính/tọa độ:** kiểm tra GPS trong giới hạn vật lý và bán kính dương để minh họa dữ liệu hợp lệ; PRD chưa chốt min/max bán kính hay độ chính xác.
7. **Analytics:** chưa xác định event schema, loại lượt phát, taxonomy feedback, múi giờ/boundary báo cáo, layout XLSX. Fixture dùng lượt phát mock theo ngày, ngày vi-VN/Asia_Ho_Chi_Minh bao gồm hai đầu kỳ, danh mục minh họa; workbook hai sheet Analytics/Feedback. Đây là lựa chọn trình diễn, cần xác nhận cho production.
8. **Lưu trữ demo:** localStorage chỉ phục vụ demo, không thay thế API; file audio preview chỉ còn trong phiên tab, metadata giữ qua reload. Chưa có upload thật hay dữ liệu khách du lịch thật.
