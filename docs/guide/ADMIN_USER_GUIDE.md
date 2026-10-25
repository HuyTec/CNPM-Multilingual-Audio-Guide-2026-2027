# Hướng dẫn thao tác Web Admin

Dành cho quản trị viên mới. Có thể mở hướng dẫn ngay trong ứng dụng bằng menu **Hướng dẫn sử dụng** (`/help`). Xem [ADMIN_GUIDE.md](ADMIN_GUIDE.md) cho cấu hình và kiểm thử kỹ thuật.

## 1. Đăng nhập và chọn vai trò

Mở địa chỉ Admin do nhóm vận hành cung cấp (demo local: `http://localhost:3001`), nhập tài khoản/mật khẩu được cấp và kiểm tra tên tài khoản ở thanh điều hướng.

| Vai trò | Được thao tác |
|---|---|
| Biên tập viên — EDITOR | Tạo/sửa địa điểm, ảnh, tên và bài đa ngôn ngữ; lưu nháp, gửi duyệt; xem thống kê và xử lý phản hồi. |
| Người duyệt — REVIEWER | Xem bản biên tập; chấp thuận/từ chối; theo dõi và thử tạo lại gói; xem thống kê và xử lý phản hồi. Không sửa nội dung trực tiếp. |

Tài khoản `admin` trong bản demo hiện có quyền EDITOR nên không có menu **Duyệt và xuất bản**. Muốn duyệt, đăng xuất ở góc trên bên phải rồi dùng tài khoản REVIEWER được cấp riêng. Không ghi hoặc gửi mật khẩu trong tài liệu/báo lỗi.

Phiên chỉ giữ trong tab. Tải lại trang có thể yêu cầu đăng nhập lại; lưu nháp trước khi đóng trang hoặc đổi tài khoản.

## 2. Tạo địa điểm và nội dung

1. Vào **Địa điểm**, tìm bản đã có để chỉnh sửa hoặc chọn **Thêm địa điểm**.
2. Chọn **Nhập thủ công** hoặc **Dịch tự động**. Chuyển phương án giữ dữ liệu đang nhập.
3. Trong **Ảnh địa điểm**, chọn JPEG/PNG tối đa 5 MB, mỗi chiều tối đa 4096 px và tối đa 12 triệu điểm ảnh. Kiểm tra ảnh xem trước. Một ảnh dùng chung cho mọi ngôn ngữ; ảnh nháp chưa công khai.
4. Nhập tên, danh mục, vĩ độ, kinh độ và bán kính geofence (số nguyên dương, đơn vị mét). Xác nhận tọa độ thực tế trước khi gửi duyệt.
5. Chọn **Ngôn ngữ** và **Loại kịch bản** để nhập bài. `FULL` là bài đầy đủ; `SHORT` là bài rút gọn, phải nhập riêng.

**Nhập thủ công:** chọn lần lượt từng ngôn ngữ, nhập tên và bài FULL/SHORT tương ứng. Đổi ngôn ngữ không xóa bản khác. Viết dấu câu rõ ràng để TTS đọc tốt hơn.

**Dịch tự động:** nhập tên/bài ở ngôn ngữ nguồn, chọn ngôn ngữ đích rồi bấm **Dịch tự động**. Nếu đích đã có dữ liệu, xác nhận trước khi thay thế. Đọc và sửa kết quả trước khi lưu. **Provider mặc định hiện chỉ giả lập**, có cảnh báo và tiền tố `[DEMO ...]`; không xem đây là bản dịch thật. Nhóm vận hành phải cấu hình dịch vụ để dịch thật. Lỗi dịch giữ dữ liệu và vẫn cho nhập tay.

**TTS và audio:** Web/Android đọc văn bản bằng TTS thiết bị khi không có audio thu sẵn hoặc audio phát lỗi. Không cần tạo file âm thanh cho bài TTS. Nếu có audio, mở **Audio thu sẵn (tùy chọn)**, tải MP3/WAV/OGG tối đa 20 MB rồi nghe thử. Audio gắn với ngôn ngữ và loại bài đang chọn; thay một biến thể không đổi biến thể khác. Lỗi tải giữ nội dung để thử lại.

## 3. Lưu nháp và gửi duyệt

| Trạng thái | Ý nghĩa |
|---|---|
| DRAFT — Bản nháp | Được phép thiếu thông tin; mở lại để tiếp tục. |
| PENDING_REVIEW — Chờ duyệt | Đã gửi, chưa công khai bản mới. |
| REJECTED — Bị từ chối | Đọc nhận xét, sửa và gửi lại. |
| APPROVED — Đã chấp thuận | Trong bản làm việc: chờ công bố gói. Trên public API: trạng thái tương thích để client đọc dữ liệu đã công khai. |
| PUBLISHED — Đã xuất bản | Tác vụ công bố hoàn thành. |

**Lưu nháp** không bắt buộc đủ thông tin. **Gửi duyệt** yêu cầu tên, danh mục, tọa độ và bán kính hợp lệ; sửa các trường được đánh dấu khi có lỗi. Dữ liệu đang nhập được giữ lại. Người duyệt còn yêu cầu có ít nhất một bài có nội dung trước khi chấp thuận.

Sửa địa điểm đã công khai vẫn giữ bản cũ đến khi bản thay thế được duyệt và tạo gói thành công. Nếu báo **xung đột phiên bản**, chép lại phần đang nhập trước khi mở lại bản mới nhất; không cố lưu đè người đang sửa cùng địa điểm.

## 4. Duyệt và theo dõi tạo gói

1. Đăng nhập REVIEWER → **Duyệt và xuất bản** → chọn địa điểm trong hàng đợi.
2. So sánh thông tin vị trí, tên đa ngôn ngữ và bài đang công khai/bài đề xuất. Mở **Xem đầy đủ bản biên tập** để xem ảnh và form chỉ đọc.
3. Chọn **Từ chối** khi cần sửa và nhập nhận xét bắt buộc; hoặc chọn **Chấp thuận và tạo gói**, xác nhận quyết định.
4. Theo dõi **Tác vụ tạo gói**; trang cập nhật khi đang mở, có nút làm mới.

| Tác vụ | Bước tiếp theo |
|---|---|
| Đang tạo gói — BUILDING | Đợi xử lý; chưa coi là đã công khai. |
| Sẵn sàng — READY | Mở Web du khách để kiểm tra và tải. |
| Tạo thất bại — FAILED | Xem lỗi, xử lý nguyên nhân rồi Thử tạo lại gói. Data/gói công khai cũ vẫn giữ nguyên. |

## 5. Kiểm tra tải gói trên Web du khách

Web du khách có địa chỉ riêng do nhóm vận hành cung cấp. Link Cloudflare chỉ hoạt động khi máy demo, Docker và tunnel còn chạy.

1. Sau khi tác vụ READY, mở Web du khách → **Nội dung offline** → **Kiểm tra cập nhật**. Polling khoảng 30 giây khi trang đang hoạt động cũng kiểm tra gói mới; kiểm tra lại khi quay về tab/kết nối trở lại.
2. Chọn **Tải gói** hoặc **Cập nhật gói**. Gói cơ bản chứa tuyến demo; các địa điểm thuộc danh mục khác nằm trong gói bổ sung.
3. Hộp xác nhận hiện giữa màn hình → **Xác nhận tải** → đợi thông báo **Đã cài nội dung được xác minh**. Có thể hủy khi tải; lỗi tải/xác minh không thay gói cũ. Nếu đang nghe hoặc tạm dừng bài, dừng bài để cài gói ngay.
4. Trong **Đang lưu trên thiết bị**, mở **Xem … địa điểm trong gói**, chọn tên để đọc bài.

**Gói lưu trong bộ nhớ trình duyệt, không phải file trong thư mục Downloads.** Đổi trình duyệt, điện thoại hoặc link Cloudflare cần tải lại ở địa chỉ mới. Không xóa bộ nhớ chỉ để thử cập nhật.

Trang khám phá mặc định xem mọi danh mục đã công khai. Có thể tìm theo tên hoặc mở từ danh sách. Vùng nhìn bản đồ ban đầu là Quận 1/Quận 3. **Điểm mẫu A ở (1,1) cách xa Việt Nam**: tìm theo tên hoặc mở từ gói, không tìm trong danh sách gần bạn khi đang ở Việt Nam. Không dùng tọa độ mẫu làm vị trí triển khai thật.

Text/audio được kiểm checksum và chữ ký trước khi lưu. Ảnh tự tải lên hiện chỉ dùng online, chưa đóng byte ảnh offline; Android chưa có UI ảnh mới. Mở lại toàn bộ Web khi mất mạng còn cần app shell/service worker đã chuẩn bị; TTS offline cần giọng cài trên máy.

## 6. Thống kê và phản hồi

1. Vào **Thống kê & phản hồi**, chọn **Từ ngày / Đến ngày**. Mặc định 30 ngày, theo giờ Việt Nam. Chỉ tính lượt phát khi bài thực sự bắt đầu phát, không tính khi chỉ mở địa điểm.
2. Xem tổng lượt phát, địa điểm nổi bật, ngôn ngữ và tỷ lệ ngoại tuyến. Nếu rỗng, thử kỳ báo cáo khác hoặc kiểm tra đã phát bài thực tế chưa.
3. Lọc phản hồi theo danh mục/trạng thái → **Xem chi tiết**. Chuyển đúng thứ tự **Mới → Đang xử lý → Đã giải quyết**, không bỏ bước.
4. **Xuất báo cáo .xlsx** dùng kỳ và bộ lọc phản hồi đang chọn, gồm toàn bộ dòng phù hợp chứ không chỉ trang hiện tại. Nếu xuất lỗi, giữ bộ lọc và thử lại.

Admin quản lý phản hồi đã có trong hệ thống; không thay thế chức năng gửi phản hồi của du khách.

## 7. QR tải Android và báo lỗi

Trang chủ Web du khách có nút APK và QR cùng đường dẫn. Mở **đúng link Cloudflare hiện tại** trước khi quét hoặc bấm **Lưu mã QR**; dưới mã hiển thị tên miền tải APK. Nếu tunnel đổi địa chỉ, lưu/quét QR mới từ link mới. QR mặc định lấy tên miền website đang mở, trừ khi nhóm vận hành đã cấu hình nơi lưu APK riêng.

APK hiện là bản debug demo. Tải APK khác với tải gói nội dung. Cài app xong cần cấu hình **Kết nối máy chủ** theo địa chỉ Web du khách được cấp. Điện thoại thật không dùng `localhost` hoặc địa chỉ máy ảo `10.0.2.2` để kết nối máy tính từ Internet.

Khi báo lỗi, ghi trang đang dùng, tên địa điểm, vai trò tài khoản, thao tác vừa làm và thông báo lỗi; không gửi mật khẩu/token. Nếu chưa thấy bài mới, kiểm tra lần lượt: gửi duyệt → chấp thuận → tác vụ READY → đúng link → đúng gói → đã xác nhận tải → danh sách địa điểm trong gói.
