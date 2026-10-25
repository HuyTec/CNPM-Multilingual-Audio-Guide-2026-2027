# Backend

**Luồng nghe/thống kê thật:** profile `demo` có SHORT guide, WAV tiếng Việt, `POST /api/playbacks` idempotent và `/api/admin/analytics` có xác thực. V3/V3.1 bổ sung liên kết revision/sự kiện và seed ba bài ngắn, không seed lượt nghe. Xem [LIVE_DEMO_SETUP](../docs/vi/LIVE_DEMO_SETUP.md).

Spring Boot API cho nội dung hướng dẫn, quy trình duyệt và quản trị. Mã nguồn ở `multi-audio-guide/`, dùng Java 21, Spring Boot 4.1.1 và Maven Wrapper 3.3.4 (Maven 3.9.9).

## Cài đặt và chạy trên Windows

Cài JDK 21 (nếu máy có `winget`):

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
java -version
javac -version
```

Mở lại PowerShell sau khi cài; `java` và `javac` phải hiển thị phiên bản 21. Đặt `JAVA_HOME` trỏ tới JDK 21 nếu máy có nhiều phiên bản Java.

Mở PowerShell tại thư mục gốc repository, rồi chạy:

```powershell
cd .\backend\multi-audio-guide
.\mvnw.cmd --version
.\mvnw.cmd test
.\mvnw.cmd package
```

`test` tải dependency và chạy các test hiện có; `package` tạo JAR trong `target/`. Không cần cài Maven toàn hệ thống: Maven Wrapper tự tải Maven 3.9.9 vào cache người dùng trong lần đầu. Lần chạy đầu cần kết nối Maven Central.

## Chạy backend với PostgreSQL

Dùng [Docker Compose](../docs/vi/DOCKER_SETUP.md) tại thư mục gốc: sao chép `.env.example` thành `.env`, đặt mật khẩu local, rồi chạy `docker compose up -d --build`. Backend dùng JDBC tới `postgres:5432/audio_guide`; PostgreSQL không công khai cổng ra máy host. Xem thêm [quy ước dùng chung](../GUILD.md).

Nếu chạy `spring-boot:run` ngoài Docker, phải có database truy cập được từ máy host và tự đặt `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` phù hợp. Tên host `postgres` chỉ dùng trong mạng Compose.

## Flyway và schema demo

Flyway tự chạy migration khi backend khởi động, dùng cùng datasource PostgreSQL. Migration đầu tiên là [`V1__demo.sql`](multi-audio-guide/src/main/resources/db/migration/V1__demo.sql); tên có **hai dấu gạch dưới** giữa phiên bản và mô tả.

Migration tạo 9 bảng theo trang **Database** của [`SE.drawio (1).xml`](../docs/en/SE.drawio%20%281%29.xml): `categories`, `languages`, `locations`, `guide_content_revisions`, `audio_assets`, `content_packages`, `content_package_items`, `feedbacks`, `analytics_events`. Đây là schema demo vật lý trong sơ đồ, chưa phải mô hình PRD hoàn chỉnh; không chèn dữ liệu mẫu. Thông tin địa điểm và script text cho phép thiếu khi lưu DRAFT; các trường bắt buộc phải được kiểm tra khi gửi duyệt.

`locations.coordinates` dùng PostGIS `GEOGRAPHY(Point,4326)` và GiST index. Compose dùng PostgreSQL 17 kèm PostGIS (`postgis/postgis:17-3.6-alpine`); nếu dùng PostgreSQL ngoài Docker, cần cài PostGIS và cấp quyền tạo extension cho tài khoản chạy migration, hoặc nhờ quản trị viên bật extension trước.

Sau khi chuẩn bị `.env` theo hướng dẫn ở trên, chạy từ thư mục gốc:

```powershell
docker compose up -d --build
docker compose logs --tail 100 backend
docker compose exec postgres psql -U audio_guide -d audio_guide -c 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;'
```

Kết quả mong đợi: có phiên bản `1` (`demo`) và `2` (`location discovery`), `success = t`; profile demo có thêm `2.1` (`seed discovery demo`); luồng lượt nghe bổ sung `3` (`playback content reference`) và demo `3.1` (`seed short guides`). Không sửa migration sau khi đã áp dụng; tạo phiên bản tiếp theo để thay đổi schema. Với DB đã có bảng ứng dụng nhưng chưa có lịch sử Flyway, cần đối chiếu schema trước khi baseline; không tự bật `baseline-on-migrate` để bỏ qua V1.

## Chạy demo US-02 và US-03

Luồng xử lý: `LocationController -> LocationDiscoveryService -> LocationRepository / CategoryRepository -> PostgreSQL`. Model giữ dữ liệu truy vấn, Mapper chuyển sang DTO trả về. Backend dùng JDBC hiện có, không cần thêm JPA.

| API | Mục đích |
| --- | --- |
| `GET /api/categories` | Danh mục đang hoạt động, kèm số địa điểm APPROVED; có cả danh mục rỗng |
| `GET /api/locations?keyword=...&categoryId=...&page=0&size=20` | Tìm tên và/hoặc lọc danh mục, không cần GPS |
| `GET /api/locations/{locationId}` | Lấy đúng thông tin địa điểm đã chọn; chưa tải script/audio |

`keyword` được bỏ khoảng trắng đầu/cuối, tối đa 255 ký tự; tìm literal không phân biệt hoa/thường, vẫn phân biệt dấu. Thứ tự: tên khớp hoàn toàn, tiền tố, chứa từ khóa; sau đó tên và UUID để phân trang ổn định. `page` bắt đầu từ 0; `size` từ 1 đến 50. Từ khóa trống trả danh sách công khai. Đây là quy ước demo; các điểm cần chốt cho sản phẩm được ghi tại Jira HVP-1.

Response danh sách có `items`, `page`, `size`, `totalElements`, `totalPages`, `message`, `suggestedCategories`. Không khớp trả HTTP 200, `items=[]`, thông báo và các danh mục khác đang có dữ liệu; không tự suy đoán địa điểm phổ biến. Danh mục/địa điểm không tồn tại hoặc không công khai trả 404; tham số sai trả 400; lỗi truy cập DB trả 503 với thông báo thử lại. Các lỗi có dạng Problem Details (`status`, `title`, `detail`).

V2 thêm `locations.status`, mặc định DRAFT cho dữ liệu cũ; chỉ APPROVED thuộc danh mục đang bật được công khai. Profile `demo` nạp thêm migration `V2_1__seed_discovery_demo.sql` chứa địa điểm **hư cấu**, gồm danh mục có/rỗng, DRAFT, PENDING_REVIEW và danh mục đã tắt. Dùng database riêng cho demo và giữ profile demo khi dùng lại database đó, vì lịch sử Flyway có các migration demo 2.1/3.1. Profile mặc định chạy V1/V2/V3 và không nạp fixtures.

Chạy từ thư mục gốc, sau khi chuẩn bị mật khẩu local theo hướng dẫn Docker:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'demo'
docker compose -p hvp-us02-demo up -d --build
```

Tên project `hvp-us02-demo` tạo volume riêng. Demo dùng các cổng 8080/3000 như Compose hiện có. Gọi thử trong PowerShell:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/categories'
$keyword = [uri]::EscapeDataString('Bảo tàng')
Invoke-RestMethod "http://localhost:8080/api/locations?keyword=$keyword"
Invoke-RestMethod 'http://localhost:8080/api/locations?categoryId=10000000-0000-0000-0000-000000000003'
Invoke-RestMethod 'http://localhost:8080/api/locations/20000000-0000-0000-0000-000000000001'
```

Public discovery giữ trường name tương thích và bổ sung names đa ngôn ngữ. Editor UC-06 có names theo ngôn ngữ trong workspace JSONB và bảng location_translations cho dữ liệu published; sửa nháp không thay dữ liệu đang phục vụ. Offline package, phân lớp và API phản hồi hiện tại được mô tả trong [DEMO_CODE_GUIDE.md](../docs/guide/DEMO_CODE_GUIDE.md). Không dùng snapshot mô tả demo cũ để kết luận các luồng mới chưa có.

### Kiểm thử

`mvnw.cmd verify` chạy kiểm thử HTTP/service với repository giả lập. Integration test đi qua HTTP, service, JDBC và Flyway thật khi có `TEST_DATABASE_URL`; nếu thiếu biến này, nhóm integration được báo skipped. CI backend đã cấu hình PostGIS riêng để chạy cả hai nhóm; cần xem lần chạy GitHub Actions thực tế để xác nhận CI.

Để chạy integration test local, trỏ các biến sau tới **database PostgreSQL/PostGIS kiểm thử riêng** (test sẽ áp dụng V1/V2/V3 và fixtures demo):

```powershell
$env:TEST_DATABASE_URL = 'jdbc:postgresql://localhost:5432/discovery_test'
$env:TEST_DATABASE_USERNAME = 'audio_guide'
$env:TEST_DATABASE_PASSWORD = 'mat_khau_database_kiem_thu'
cd .\backend\multi-audio-guide
.\mvnw.cmd -B -ntp verify
```

Các thay đổi dữ liệu trong từng test được rollback; schema và fixtures Flyway vẫn còn để chạy lại. Bộ integration kiểm tra tìm kiếm, danh mục rỗng, phân trang, tọa độ, ẩn dữ liệu chưa duyệt, ký tự LIKE/SQL và việc migration không chạy lặp.
