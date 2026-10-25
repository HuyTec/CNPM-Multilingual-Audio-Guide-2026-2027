# TravelVoice · Web Admin

React/Vite/TypeScript cho biên tập POI đa ngôn ngữ, duyệt/xuất bản và thống kê/phản hồi từ PostgreSQL.
Tài liệu: [Admin Guide](../docs/guide/ADMIN_GUIDE.md), [API](../docs/guide/API_list.md), [chạy demo](../docs/guide/DEMO_CODE_GUIDE.md).

```powershell
npm.cmd ci
npm.cmd run dev
npm.cmd run build
npm.cmd run lint
npm.cmd run test:e2e -- --workers=1
```

Đặt API_PROXY_TARGET trong .env.local nếu backend khác mặc định 8080. Live-demo dùng 8081.
Tài khoản seed demo: editor / EditorDemo!2026; reviewer / ReviewerDemo!2026 (chỉ seed lần đầu).
Phiên bearer chỉ ở bộ nhớ tab; reload cần đăng nhập lại. Basic Auth cũ đã bị thay thế.
HTTP fixture tests không chứng minh kết nối DB; suite publication.live dùng backend/DB riêng có chủ đích.
