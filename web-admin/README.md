# TravelVoice · Web Admin

Danh sách, biên tập và thống kê mặc định dùng PostgreSQL thật qua cùng backend cổng 8080 với Web du khách/Android. Hợp đồng: [ADMIN_CONTENT_API.md](../backend/ADMIN_CONTENT_API.md). Bản APPROVED tiếp tục được phục vụ khi sửa nháp hoặc gửi duyệt; chưa có hành động tự duyệt/xuất bản. Phản hồi giả lập chỉ ở `/analytics?source=mock`.

## Demo Web Admin UC-06 / UC-07

Giao diện tiếng Việt, React Router, Tailwind. Yêu cầu hiện hành: [PRD](../docs/en/PRD_Report.md), UC-06/UC-07. Mã mock cũ được giữ cho prototype riêng, không thay thế API của danh sách/form chính.

- `/locations`: danh sách, tìm kiếm, trạng thái và phân trang.
- `/locations/new`, `/locations/:id/edit`: lưu nháp, gửi duyệt, kịch bản ngôn ngữ × FULL/SHORT, audio preview/retry và cảnh báo chưa lưu. Không thay thế bản published.
- `/analytics`: thống kê lượt nghe Web/Android thật, kỳ 30 ngày, tự cập nhật 5 giây và xuất XLSX từ API.

Đăng nhập bằng tài khoản Basic Auth demo cấu hình tại backend. Local hiện lưu cấu hình trong `.env.admin.local` ở root (ignored); không gửi mật khẩu vào chat/commit. Phiên chỉ giữ trong bộ nhớ tab, reload cần đăng nhập lại. Form có sáu ngôn ngữ × FULL/SHORT; text và audio lưu trong DB, tải lại vẫn nghe thử được. Xung đột phiên bản trả 409 và giữ dữ liệu đang nhập. Lưu nháp cho phép thiếu trường; gửi duyệt kiểm tra tên/danh mục/tọa độ/bán kính. Audio MP3/WAV/OGG tối đa 20 MB; upload lỗi giữ text và cho thử lại.

Kiểm thử tích hợp local với backend và Web Admin Docker đang chạy ở cổng 3000:

```powershell
npx.cmd playwright install chromium
# Đặt DEMO_ADMIN_USERNAME/DEMO_ADMIN_PASSWORD trong môi trường test, không ghi giá trị vào lệnh.
$env:LIVE_CONTENT_API='1'
npx.cmd playwright test --config playwright.content-live.config.ts
Remove-Item Env:LIVE_CONTENT_API
```

Suite live không giả lập API; tạo bản nháp có tiền tố `Codex UI integration`, ghi ID vào `test-results-content/owned-ids.json` để người chạy dọn đúng dữ liệu test. Không dùng suite prototype cũ làm bằng chứng DB tích hợp. Không chạy hai bộ Playwright đồng thời cùng thư mục output.

## Cài đặt và chạy trên Windows

[Vite 8](https://v8.vite.dev/guide/) cần Node.js 20.19+ hoặc 22.12+; Node.js 24 cũng phù hợp. Nếu chưa có Node.js và máy có `winget`, cài bản LTS rồi mở lại PowerShell:

```powershell
winget install --id OpenJS.NodeJS.LTS -e
node --version
npm.cmd --version
```

Mở PowerShell tại thư mục gốc repository, rồi chạy:

```powershell
cd .\web-admin
npm.cmd ci
npm.cmd run dev -- --port 5174
```

Vite cổng 5174 proxy `/api` và `/demo-audio` sang 8080; có thể đổi `API_PROXY_TARGET`. Bản Compose ở http://127.0.0.1:3000/ dùng nginx proxy backend. `npm.cmd ci` cài đúng phiên bản trong `package-lock.json`. Kiểm tra và đóng gói bằng:

```powershell
npm.cmd run lint
npm.cmd run build
npm.cmd run preview
```

Chạy `npm.cmd run preview` sau khi build để xem bản đóng gói. Trên PowerShell dùng `npm.cmd` nếu chính sách thực thi chặn `npm.ps1`.
