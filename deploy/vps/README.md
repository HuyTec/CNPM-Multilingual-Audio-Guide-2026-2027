# Chạy Web User độc lập laptop

Bộ triển khai này dành cho một VPS Linux chạy liên tục: Caddy cấp HTTPS, Web User chuyển tiếp API tới backend, PostgreSQL giữ dữ liệu trong volume. Chưa có VPS hoặc tên miền thực tế được tạo. QR trong `docs/qr/` hiện vẫn trỏ tới tunnel demo và phụ thuộc laptop.

## Chuẩn bị server

Chọn VPS Ubuntu 24.04 LTS x86_64. Cấu hình khởi đầu đề xuất cho bản demo Java và PostgreSQL cùng máy là 2 vCPU, RAM 4 GB; cần theo dõi tài nguyên thực tế khi có người dùng. Đây chưa phải kết quả kiểm thử tải. Có thể chọn bất kỳ nhà cung cấp hỗ trợ Docker Compose, không cần đúng một hãng.

Cài Docker Engine và Compose plugin theo [hướng dẫn Docker cho Ubuntu](https://docs.docker.com/engine/install/ubuntu/). Trỏ bản ghi DNS A của tên miền tới IPv4 VPS. Chỉ thêm AAAA nếu IPv6 của VPS hoạt động. Cho phép SSH từ địa chỉ quản trị; mở TCP 80/443, UDP 443 tùy chọn cho HTTP/3. Không mở 5432, 8080 hoặc 4176 ra Internet.

Đưa bản source đã kiểm tra lên server bằng Git hoặc SCP; không gửi `.env`, `.env.local`, `node_modules` hoặc thư mục `.local-tools` lên Git. Nếu chuyển dữ liệu đang dùng, làm theo mục di chuyển bên dưới trước khi chạy toàn bộ dịch vụ.

## Cấu hình và khởi động

Các lệnh sau chạy trong thư mục gốc repository trên VPS bằng Bash:

```bash
cp deploy/vps/production.env.example .env.production
chmod 600 .env.production
nano .env.production
```

Thay `DOMAIN` bằng hostname thật, không có `https://` hay đường dẫn; đặt email thật và hai mật khẩu ngẫu nhiên khác nhau. File này chỉ nằm trên server, đã được `.gitignore` loại trừ. Mẫu không chứa thông tin đăng nhập thật. Không dùng các giá trị `replace_with...` trong mẫu để chạy công khai.

```bash
docker compose --env-file .env.production -f compose.production.yaml config --quiet
docker compose --env-file .env.production -f compose.production.yaml up -d --build
docker compose --env-file .env.production -f compose.production.yaml ps
docker compose --env-file .env.production -f compose.production.yaml logs --tail 100 caddy backend web
```

Chờ postgres/backend/web có trạng thái healthy, Caddy cấp chứng chỉ thành công rồi mở `https://TEN_MIEN_THAT`. Caddy cần DNS đúng và cổng 80/443 truy cập được từ Internet; kiểm tra [Automatic HTTPS](https://caddyserver.com/docs/automatic-https) nếu cấp chứng chỉ thất bại. Chỉ kiểm tra cấu hình Caddy không chứng minh đã có chứng chỉ thật.

Backend hiện chạy profile `demo` để giữ tuyến 7 điểm, gói nội dung và API playback đang có. Đây là bản demo được hosting, chưa phải xác nhận đã hoàn thành toàn bộ PRD. Gateway công khai chỉ cho phép API của khách tham quan; `/api/admin/*`, `/actuator/*`, source và file môi trường bị chặn. Web Admin không được xuất bản bằng bộ này.

Sau khi HTTPS hoạt động: dùng điện thoại qua Wi-Fi và 4G mở trang, kiểm tra đủ 7 điểm, bài thuyết minh, tải gói và GPS. Trình duyệt vẫn phải được cấp quyền vị trí. Tắt laptop để xác minh trang tiếp tục hoạt động trên VPS. Không dùng máy tính đang mở tunnel làm bằng chứng cho bước này.

## Di chuyển dữ liệu hiện có

Tạo bản sao PostgreSQL từ môi trường local trước khi chuyển; dump gồm các bảng nội dung và audio upload lưu trong database. Audio demo đi cùng backend image. Không chia sẻ dump công khai vì có thể chứa dữ liệu ứng dụng. Ví dụ PowerShell tại laptop:

```powershell
New-Item -ItemType Directory -Force .local-tools/backups | Out-Null
docker compose exec -T postgres pg_dump -U audio_guide -d audio_guide -Fc -f /tmp/audio-guide-migration.dump
docker compose cp postgres:/tmp/audio-guide-migration.dump .local-tools/backups/audio-guide-migration.dump
```

Chuyển dump riêng tới server qua SCP, đặt quyền 600. Khi database đích còn mới và chưa có bảng ứng dụng, chỉ chạy postgres trước:

```bash
docker compose --env-file .env.production -f compose.production.yaml up -d postgres
docker compose --env-file .env.production -f compose.production.yaml exec -T postgres psql -U audio_guide -d audio_guide -c "SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename <> 'spatial_ref_sys';"
```

Chỉ tiếp tục khi kết quả không có bảng ứng dụng. Nếu đã có dữ liệu, dừng tại đây để lập phương án nhập/khôi phục riêng; không dùng `--clean` hoặc xóa volume.

```bash
docker compose --env-file .env.production -f compose.production.yaml exec -T postgres pg_restore -U audio_guide -d audio_guide --no-owner --no-acl --exit-on-error < audio-guide-migration.dump
docker compose --env-file .env.production -f compose.production.yaml up -d --build
```

Đối chiếu số địa điểm, ngôn ngữ và audio sau nhập. Giữ bản sao local cho tới khi xác minh hoàn tất.

## QR theo tên miền cố định

Chỉ tạo QR mới sau khi kiểm tra địa chỉ HTTPS thật. Chạy script sẵn có trên laptop (thay URL):

```powershell
python web-user/scripts/make-qr.py https://TEN_MIEN_THAT --output docs/qr/web-user-vps
```

Script cần thư viện `qrcode` (bản cài phục vụ QR demo đã nằm tại `.local-tools/python` trên laptop hiện tại) và xuất PNG, SVG, TXT. Không chạy với hostname mẫu. QR là đường dẫn tĩnh: việc website tiếp tục hoạt động phụ thuộc VPS, tên miền và HTTPS được duy trì.

## Sao lưu và cập nhật

Chạy `sh deploy/vps/backup.sh` trên VPS để tạo dump có thời gian tại `.local-tools/backups/`. Sao chép bản sao ra một nơi độc lập với VPS và kiểm tra khôi phục định kỳ. Chưa cài lịch backup tự động hoặc dịch vụ backup trả phí.

Trước cập nhật, sao lưu database. Đưa source mới lên server rồi chạy lại `up -d --build` như trên. Giữ volume và bản build/source trước đó để có thể quay lại. Không chạy `docker compose down -v`: thao tác đó xóa dữ liệu volume. Đổi mật khẩu trong `.env.production` không tự đổi mật khẩu của user PostgreSQL trong volume đã khởi tạo; cần quy trình đổi đồng bộ trong database.

## Kết quả kiểm tra ngày 23 tháng 10 năm 2026

Bộ Compose được build và chạy cục bộ trong project Docker riêng `travelvoice-vps-check`: postgres/backend/web healthy, trang `/` và API categories/locations trả HTTP 200, danh sách tuyến có 7 điểm, API admin qua web trả 404. `caddy validate` thành công. Đã xuất dump local và khôi phục thành công vào database thử mới, xác nhận 13 địa điểm tổng. Chưa kiểm tra DNS, cấp chứng chỉ công khai, tải đồng thời, tự khởi động VPS hoặc backup/restore trên server thật vì chưa có VPS.
