
# Đánh giá Tổng thể từ CODEX

**Cần thay đổi lớn (major_changes).** Phạm vi MVP hiện tại khá nhất quán và các sơ đồ có cấu trúc đủ để sử dụng, tuy nhiên cần giải quyết ba ranh giới luồng nghiệp vụ đầu-cuối trước khi tài liệu này có thể trở thành baseline cho quá trình triển khai:

1. Quy trình phê duyệt và xuất bản nội dung.
2. Hành vi thông báo của UC-05.
3. Luồng chuyển tiếp từ UC-05 sang UC-02.

# Những điểm hiện tại đã tốt

Bảy Use Case đã bao phủ được hành trình chính của khách du lịch và các chức năng quản trị thiết yếu mà không có dấu hiệu mở rộng phạm vi không cần thiết.

Các luồng dự phòng được mô tả cụ thể, các Business Rule bảo vệ được những ranh giới quan trọng liên quan đến nội dung và cập nhật, đồng thời cả bảy Activity Diagram đều có luồng logic từ điểm bắt đầu đến điểm kết thúc, không có tham chiếu cạnh XML bị hỏng.

# Các vấn đề nghiêm trọng

Không có vấn đề nào được phân loại là **Critical**.

Không phát hiện lỗi hỏng dữ liệu nguồn hoặc lỗi cấu trúc đồ thị không thể khôi phục.

# Các cải tiến quan trọng

1. Xác định rõ thành phần chịu trách nhiệm và hợp đồng xử lý cho quá trình **phê duyệt, xuất bản và đồng bộ nội dung APPROVED**.

   Chỉ xuất bản/cài đặt một **gói nội dung đã được phê duyệt có tính nguyên tử (atomic approved package)**, trong đó nội dung script và tài nguyên audio phải tương ứng với nhau.
2. Giữ nguyên ranh giới MVP đã được mô tả trong UC-05:

   Khi người dùng chọn một notification/banner, hệ thống **chỉ mở màn hình Location Details**.

   Loại bỏ việc UC-05 tự động kích hoạt UC-03 để phát audio.
3. Cho phép UC-02 được bắt đầu từ bất kỳ luồng điều hướng hợp lệ nào đến **Location Details** có mang theo `locationId`, bao gồm cả trường hợp người dùng chọn notification từ UC-05.
4. Sửa thứ tự xử lý **approval/fallback** của UC-02 và các luồng **pause/device-disconnect** trong UC-03.
5. Chọn một baseline thống nhất cho trường hợp chạy offline lần đầu và mô tả rõ cơ sở dữ liệu địa điểm sẽ được cung cấp cho ứng dụng như thế nào.

# Tính nhất quán giữa các Use Case

UC-02 yêu cầu nội dung phải ở trạng thái `APPROVED`, trong khi UC-06 kết thúc ở trạng thái `DRAFT/PENDING_REVIEW`.

Vì vậy, UC-04 phải được giới hạn để chỉ làm việc với các **gói nội dung đã được phê duyệt và xuất bản**.

UC-05 cung cấp một `locationId` hợp lệ, tuy nhiên hiện tại UC-02 chỉ mô tả UC-01 là nguồn cung cấp `locationId`.

Ngoài ra, các thành phần sau hiện đang được giả định là dependency bên ngoài:

- Ngôn ngữ hiện tại của người dùng.
- Gửi phản hồi.
- Các sự kiện analytics.

Cần xác định rõ thành phần chịu trách nhiệm hoặc contract cho các dependency này mà không mở rộng phạm vi MVP hiện tại.

# Các vấn đề trong UML / Activity Diagram

Không nên thêm `<<include>>` hoặc `<<extend>>` giữa UC-01, UC-02 và UC-03 vì đây thực chất là các **luồng bàn giao dữ liệu/trạng thái (data/state handoff)**.

Nên bổ sung **System Boundary** vào các Use Case Diagram.

UC-02 phải kiểm tra trạng thái phê duyệt trước khi tải nội dung văn bản, đồng thời biểu diễn rõ chuỗi fallback:

**Selected Language → EN → VI**

UC-03 nên sử dụng một **event loop nhỏ gọn** để xử lý các thao tác điều khiển playback và các sự kiện gián đoạn.

Trạng thái `Remain paused` phải đi đến một **Final Node**.

Khi thiết bị âm thanh bị ngắt kết nối, hệ thống **không được tự động tiếp tục phát**.

Ngoài ra cần bổ sung:

- UC-01: luồng mở rộng bán kính tìm kiếm.
- UC-04: luồng hủy thao tác xóa.
- UC-06: cơ chế bảo vệ phiên bản đã được phê duyệt.
- UC-07: bố cục các optional flow độc lập với nhau.

# Các vấn đề trong tài liệu

Cần chuẩn hóa:

- Mã UC.
- Tham chiếu Activity Diagram.
- Đánh số bước.
- Mã Business Rule.
- Nhãn Actor.
- Thuật ngữ sử dụng.

Ngoài ra cần:

- Sửa tiêu đề và phần mở đầu của PRD.
- Tách bảng metadata khỏi bảng mô tả flow.
- Sửa định dạng UC-03.
- Thay các ghi chú dùng trong quá trình vẽ diagram bằng các tham chiếu chính thức.
- Đồng bộ phạm vi mô tả trong `README` và đường dẫn PRD với sản phẩm thực tế.

# Các vấn đề trong XML

XML không có tham chiếu edge bị hỏng hoặc node logic bị tách rời.

Các lỗi cần sửa chủ yếu là lỗi về **ngữ nghĩa**, bao gồm:

- UC-03 có endpoint `Remain paused` nhưng endpoint này chưa phải Final Node.
- UC-03 có luồng từ trạng thái thiết bị bị ngắt kết nối quay trực tiếp lại playback.
- UC-01 thiếu luồng mở rộng bán kính tìm kiếm.
- UC-04 thiếu luồng hủy thao tác xóa.
- UC-07 có luồng `no-data` nhưng hiện tại vẫn tiếp tục đi đến các bản ghi feedback.

# Các thay đổi tối thiểu được đề xuất

1. Bổ sung một dependency/note về **approval, publication và package version**, đồng thời xác định thành phần chịu trách nhiệm.
2. Đồng bộ UC-03 và UC-05 theo luồng:

   **Notification → Location Details → UC-02**
3. Sửa các Activity Flow của UC-02 và UC-03.
4. Mô tả rõ baseline hoạt động offline.
5. Thực hiện một lượt chuẩn hóa toàn bộ:

   - Thuật ngữ.
   - Đánh số.
   - README.

# Các Activity Flow được tối ưu

### UC-02

`Location Details`

→ xác định ngôn ngữ/chế độ

→ tìm nội dung `APPROVED`

→ fallback:

`Selected Language → EN → VI`

→ tải và hiển thị nội dung

hoặc

→ thông báo nội dung không khả dụng.

### UC-03

`Play`

→ sử dụng audio tương ứng hoặc TTS

→ vòng lặp sự kiện khi đang phát

→ chỉ tiếp tục phát khi có sự kiện `focus restored` rõ ràng

hoặc

→ kết thúc ở trạng thái paused.

Khi thiết bị âm thanh bị ngắt kết nối, hệ thống luôn chuyển sang **pause**.

### UC-04

Khi cập nhật:

→ xác minh dữ liệu

→ thay thế nguyên tử một approved package.

Mọi thao tác cancel hoặc failure đều phải giữ nguyên nội dung hiện tại.

Thao tác Delete phải có nhánh:

`Confirm / Cancel`

riêng.

### UC-05

`Geofence checks`

→ `Notification/Banner`

→ người dùng chọn thông báo

→ `Location Details`

→ UC-02.

### UC-06 / UC-07

Giữ nguyên phiên bản đã được xuất bản trong khi các chỉnh sửa mới đang chờ review.

Các thao tác trên Dashboard cần được thiết kế dưới dạng các optional flow độc lập.

# Các file cần được cập nhật

- `docs/PRODUCT REQUIRMENTS DOCUMENT.md`

  Cập nhật lifecycle, handoff, offline contract, thuật ngữ và cấu trúc tài liệu.
- `docs/Software Engineer.drawio.xml`

  Sửa Activity Flow, label và bổ sung System Boundary cho Use Case Diagram.
- `README.md`

  Cập nhật đúng phạm vi sản phẩm thực tế và đường dẫn tới PRD chính thức.
