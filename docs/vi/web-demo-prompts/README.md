# Bộ prompt demo Web trước — một tuần

Ngày soạn: **20/10/2026**. Mục tiêu của phiên soạn: giao việc có thể thực thi và nghiệm thu; **chưa triển khai chức năng**.

## Bắt đầu

Dán nội dung [00_MASTER_PROMPT.md](00_MASTER_PROMPT.md) cho agent chính trong repository này. Agent đọc các tệp còn lại và triển khai theo từng mốc. Không cần tạo task/chat mới hoặc cập nhật Jira.

| Tệp | Dùng để làm gì |
| --- | --- |
| [00_MASTER_PROMPT.md](00_MASTER_PROMPT.md) | Một prompt điều phối toàn bộ đợt triển khai, kiểm soát phạm vi và cách báo cáo |
| [01_ACCEPTANCE_MATRIX.md](01_ACCEPTANCE_MATRIX.md) | 66 tiêu chí có ID cho cả UC-01–07, đặc biệt audio/TTS, đồng bộ và các nhánh lỗi |
| [02_AGENT_PROMPTS.md](02_AGENT_PROMPTS.md) | Prompt chia việc Backend, Web du khách, Admin và QA/CI; có ranh giới file |
| [03_DEMO_AND_REPORT.md](03_DEMO_AND_REPORT.md) | Kịch bản demo, điều kiện Go/No-Go và mẫu báo cáo cuối mỗi phiên |

## Phạm vi và cách dùng

- **Web du khách trước:** quét QR bằng camera điện thoại → URL HTTPS địa điểm → đọc/chọn ngôn ngữ/FULL–SHORT → nghe audio/TTS → xem số liệu trên Admin. Tải APK là tùy chọn; không xây camera scanner trong website ở mốc đầu.
- **Đồng bộ Admin:** chung API/DB/ID/revision; thay localStorage/mock bằng lưu thật. Lưu DRAFT/PENDING_REVIEW **không xuất bản ngay cho du khách**. Phụ thuộc duyệt/xuất bản bên ngoài phải được minh họa bằng công cụ fixture có nhãn, không thêm nút tự duyệt vào UC-06.
- **Mốc D0:** luồng QR/text/audio/TTS/analytics + biên tập Admin thật. **Mốc D1:** bổ sung offline, vị trí/map/banner và phản hồi/xuất đầy đủ theo PRD. Đây là thứ tự phát triển; không tuyên bố hoàn thành toàn PRD khi chỉ đạt D0.
- Android được giữ tương thích hợp đồng API; không mở rộng phát triển Android trong đợt Web nếu chưa cần sửa tương thích. Không biến Android mock thành bằng chứng nghiệm thu Web.
- Yêu cầu mới của người dùng trong chat ưu tiên hơn quy ước cũ: đợt gấp rút này không đọc/tạo/sửa/chuyển task Jira; ghi tiến độ trong chat và báo cáo gọn tại thư mục này.

## Nguồn và giới hạn

Nguồn nghiệp vụ: [PRD EN](../../en/PRD_Report.md), [PRD VI](../PRD_Report_vi.md), nhất là §2.4, UC-01–07, §6.4, NFR. Mọi ô `[ ]`, `[?]` là chưa xác nhận, không được biến thành yêu cầu đã chốt.

Snapshot source lúc soạn: có Android live VI/SHORT + MediaPlayer + playback outbox, API guide APPROVED và analytics thật; Admin form/feedback còn mock; chưa có Web du khách/TTS. Agent phải đọc lại vì source có thể thay đổi.

Snapshot CI: `.github/workflows/ci.yml` có backend Maven verify với PostgreSQL/PostGIS và Admin lint/build. Chưa có job Web du khách, Playwright E2E hoặc workflow CD trong cây source đang đọc. Không suy ra trạng thái run GitHub từ YAML hay từ build local.

Người dùng đã chọn **không cài Figma cho đợt này**. Bộ prompt dùng `frontend-design-principles`, `frontend-ui-engineering`, `web-design-guidelines`, tokens/component hiện có và browser preview để thực hiện/review giao diện. Không chờ kết nối hoặc đề xuất lại Figma; không coi thiếu Figma là blocker.

## Lịch gợi ý, điều chỉnh theo kết quả

| Thời gian | Kết quả ưu tiên |
| --- | --- |
| Ngày 1 | M0: baseline, hợp đồng API, dữ liệu thử và hướng UI; bắt đầu backend/Web song song sau khi thống nhất hợp đồng |
| Ngày 2–3 | M1: QR → text → audio/TTS → playback DB → Admin analytics |
| Ngày 3–4 | M2: Admin lưu nội dung/audio thật, bảo vệ published, thử chuyển phiên bản bằng dependency ngoài |
| Ngày 5–6 | M3: từng phần UC-04/05/01 và UC-07 còn lại; chỉ nhận thêm phần có thể nghiệm thu |
| Ngày 7 | M4: đóng tính năng, kiểm tra hồi quy, CI/CD có bằng chứng, tập demo và bàn giao |

Không lấy lịch dự kiến làm bằng chứng tính năng đã chạy. Nếu thiếu năng lực/thời gian, báo rõ phạm vi chưa đạt; không dựng tiến độ/phần trăm giả.
