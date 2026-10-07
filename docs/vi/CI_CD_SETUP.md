# Thiết lập CI/CD

## Trạng thái hiện tại

- CI đã có tại `.github/workflows/ci.yml`. Workflow chạy khi push, mở/cập nhật pull request và khi kích hoạt thủ công.
- Job backend dùng JDK 21 và Maven Wrapper để chạy `verify` (biên dịch, test, đóng gói). Job web-admin dùng Node.js 24, `npm ci`, lint và build.
- Ngày 07/10/2026, các lệnh tương ứng chạy thành công cục bộ: backend `verify` đạt 1 test; web-admin lint và build đạt. Chưa có bằng chứng workflow đã chạy trên GitHub vì các file hiện chưa được commit/push.
- Dockerfile cho backend/web-admin và `compose.yaml` đã có; build image và chạy hai container cục bộ thành công ngày 07/10/2026. Xem [cách cài và chạy Docker](./DOCKER_SETUP.md).
- CD lên máy chủ chưa được thiết lập: chưa chốt nơi triển khai, biến môi trường vận hành, registry hoặc workflow phát hành. CI xanh và Compose chạy cục bộ chưa chứng minh ứng dụng đã được triển khai.

## Bật CI trên GitHub

1. Commit và push cùng nhau `.github/workflows/ci.yml`, `backend/multi-audio-guide/pom.xml`, `backend/multi-audio-guide/mvnw*`, `backend/multi-audio-guide/.mvn/wrapper/maven-wrapper.properties`, `web-admin/package.json` và `web-admin/package-lock.json`. Workflow sẽ không chạy đúng nếu thiếu một trong các file build này.
2. Mở pull request vào `main`. Trong tab **Actions**, kiểm tra hai job **Backend / Maven verify** và **Web admin / lint and build** đều xanh. Có thể chạy thủ công bằng **Actions → CI → Run workflow** sau khi workflow có trên nhánh mặc định.
3. Sau lần chạy đầu, cấu hình bảo vệ nhánh `main` trong **Settings → Rules → Rulesets** (hoặc **Branches** nếu repo dùng branch protection): yêu cầu pull request, review và hai status check trên trước khi merge. Chỉ chọn tên check đã xuất hiện trong một lần chạy thực tế.
4. Khi CI lỗi, mở job tương ứng và sửa lỗi ở nguồn; không dùng `-DskipTests` hoặc bỏ lint chỉ để có trạng thái xanh.

Chạy lại cùng các bước trên máy Windows:

```powershell
cd .\backend\multi-audio-guide
.\mvnw.cmd -B -ntp verify

cd ..\..\web-admin
npm.cmd ci
npm.cmd run lint
npm.cmd run build
```

## Thiết kế CD theo hướng Docker Compose trên VM

Đây là hướng triển khai trong tài liệu kiến trúc hiện tại; chưa phải pipeline đang chạy. Trước khi tự động triển khai cần hoàn thành:

1. Dockerfile cho backend và web-admin cùng `compose.yaml`/healthcheck đã sẵn sàng cho môi trường cục bộ. Khi ứng dụng bắt đầu cần biến môi trường, tạo `.env.example` chỉ chứa tên biến; không đưa mật khẩu, token, khóa SSH hoặc `.env` thật vào Git.
2. Chốt VM, domain/TLS, nơi lưu image (ví dụ GHCR), PostgreSQL/PostGIS và object storage. Chỉ mở cổng của reverse proxy ra Internet; DB và object storage dùng mạng riêng. Chuẩn bị volume và backup trước khi đưa dữ liệu thật vào.
3. Tạo môi trường `staging` trên GitHub và cấu hình secret triển khai cho VM tại repository/environment phù hợp với quyền của gói GitHub. Khóa SSH triển khai dùng quyền tối thiểu; không chia sẻ khóa quản trị máy. Bật phê duyệt trước khi triển khai production nếu gói GitHub và loại repo hỗ trợ.
4. Tạo workflow CD riêng chỉ chạy sau CI thành công và theo sự kiện phát hành đã chốt (tag hoặc chạy thủ công). Build image với tag bất biến theo commit SHA, đẩy lên registry, cho VM pull đúng tag rồi chạy `docker compose up -d`. Kiểm tra healthcheck trước khi công bố hoàn tất; giữ tag trước đó để rollback.
5. Thử trên `staging`: khởi động sạch, cập nhật phiên bản, lỗi healthcheck, rollback, khôi phục backup và bảo đảm dữ liệu tồn tại qua lần triển khai mới. Sau đó mới bật production.

Chưa tạo workflow CD vì chưa có VM và chính sách secret/rollback. Sau khi chốt đích triển khai, cấu hình CD phải được kiểm tra bằng một lần deploy staging thật.

Tham khảo: [GitHub Actions environments](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments), [required status checks](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches).
