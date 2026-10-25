# Demo Admin content API

Profile `demo`, workspace V3_4 và phần mở rộng V5/V5_1; cùng PostgreSQL và locationId với Web/Android. Luồng code: Controller → Service → Repository → JDBC. Hướng dẫn đầy đủ: [DEMO_CODE_GUIDE.md](../docs/vi/DEMO_CODE_GUIDE.md).
`/api/admin/*` dùng Basic Auth hiện có (`DEMO_ADMIN_USERNAME`,
`DEMO_ADMIN_PASSWORD`); thiếu cấu hình → 503, thiếu/sai đăng nhập → 401.
Chỉ dùng HTTP trên local demo; triển khai thật cần HTTPS và xác thực phù hợp.

| Request | Kết quả |
| --- | --- |
| `GET /api/admin/content/config` | Danh mục và ngôn ngữ đang hoạt động trong DB; code ngôn ngữ chữ thường ở form |
| `GET /api/admin/content/locations` | Danh sách địa điểm và bản làm việc |
| `GET /api/admin/content/locations/{uuid}` | Bản làm việc nếu có, nếu không lấy dữ liệu hiện hành; có `published` khi đã công khai |
| `PUT /api/admin/content/locations/{uuid}` | Lưu `DRAFT` hoặc `PENDING_REVIEW`, trả bản đã lưu cùng `editVersion`, `updatedBy`, `updatedAt` |
| `POST /api/admin/content/audio` | Multipart: `locationId`, `language` (VI/EN/FR/JA/ZH/RU), `scriptType`, `file`; MP3/WAV/OGG, tối đa 20 MB, kiểm tra header |
| `GET /api/admin/content/audio/{uuid}.{format}` | Nghe thử tệp đã lưu, bắt buộc đăng nhập |
| `GET /api/content-audio/{uuid}.{format}` | 404 cho audio nháp; chỉ công khai khi được bản APPROVED của địa điểm APPROVED tham chiếu |

Body PUT: `id`, `name`, `category`, `latitude`, `longitude`, `radius` là string;
`status` là string; `editVersion` là số phiên bản mong đợi (0 cho bản làm việc mới),
`assets` là object theo khóa `vi:FULL`, `fr:SHORT`, ... Mỗi asset có
`locationId`, `languageCode` chữ thường, `scriptType`, `text`, tùy chọn
`audioUrl`, `audioName`. Gửi đầy đủ assets để giữ các biến thể khác.
Audio chỉ được gắn vào đúng địa điểm/ngôn ngữ/loại bài đã upload hoặc đã có.

Body/response nay có record DTO trong `AdminContentDto`. Trường thêm `names` là object mã ngôn ngữ chữ thường → tên; giữ `name` legacy để tương thích client cũ. Tên mặc định VI → EN → ngôn ngữ khác → legacy. `reviewerComment` chỉ có ở response; status REJECTED do reviewer bên ngoài ghi, editor chỉ lưu DRAFT/PENDING_REVIEW. `edit_version` phải tăng khi reviewer bên ngoài thay đổi kết quả review.

UC-07 có GET `/api/admin/feedback` (from/to/category/status/page=1/size=10), GET `/{id}`, PATCH `/{id}/status` và GET `/export` trả toàn bộ JSON đúng bộ lọc để trình duyệt tạo XLSX. Chỉ NEW → IN_REVIEW → RESOLVED; không mở lại. Không có endpoint gửi feedback mới trong Web Admin. Xem `FeedbackDto` và bảng API trong hướng dẫn code.

Nháp cho phép thiếu thông tin. Gửi duyệt cần tên, danh mục hoạt động, tọa độ
hợp lệ và bán kính nguyên dương. Body không hợp lệ → 400; phiên bản cũ → 409,
không ghi đè thay đổi mới. Client giữ nội dung và cho mở lại/thử lại khi lỗi.

UC-06 không tự duyệt/xuất bản: PUT không nhận APPROVED, không sửa bảng published.
Bản đang phục vụ du khách và gói hiện hành được giữ nguyên (BR4). Quy trình
duyệt/xuất bản bên ngoài chưa được triển khai trong mốc này. Sau khi có bản
APPROVED mới, Web/Android gọi lại guide hoặc tải gói mới để nhận cùng dữ liệu.
Tài khoản demo dùng chung nên `updatedBy` là username cấu hình, chưa có phân quyền
biên tập viên riêng hay lịch sử kiểm duyệt đầy đủ.

Local hiện dùng `.env.admin.local` ở repository root (ignored, không commit);
khởi động Compose với `--env-file .env.admin.local` và cung cấp cấu hình DB hiện có.
Không ghi mật khẩu/key thật vào tài liệu hoặc source.
