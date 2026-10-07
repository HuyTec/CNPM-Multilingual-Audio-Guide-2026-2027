# React + TypeScript + Vite

## Demo Web Admin UC-06 / UC-07

Giao diện tiếng Việt, React Router, Tailwind, dữ liệu giả lập trong `src/mocks/`. Đọc thiết kế tại `../docs/ui-design.md`, câu hỏi chưa chốt tại `../docs/open-questions.md`, và kết quả review tại `../docs/ui-review.md`.

- `/locations`: danh sách, tìm kiếm, trạng thái và phân trang.
- `/locations/new`, `/locations/:id/edit`: lưu nháp, gửi duyệt, kịch bản ngôn ngữ × FULL/SHORT, audio preview/retry và cảnh báo chưa lưu. Không thay thế bản published.
- `/analytics`: kỳ 30 ngày, thống kê, bộ lọc/chi tiết phản hồi trong URL, trạng thái một chiều và XLSX thật từ dữ liệu mock.

Dùng **Tình huống kiểm thử** cuối trang để kiểm tra rỗng, lỗi, dữ liệu dài, lỗi upload và lỗi export. Loading xuất hiện khi tải hoặc đổi kỳ. Mặc định giả lập Admin đã đăng nhập; chưa tích hợp backend hay dịch vụ xác thực. Audio nghe thử chỉ tồn tại trong phiên; sau reload chọn lại file, metadata vẫn được giữ. localStorage dùng hai khóa `hvp-demo-locations-v1` và `hvp-demo-feedback-v1`.

Kiểm thử luồng và accessibility:

```powershell
npx.cmd playwright install chromium
npm.cmd run test:e2e
```

Ảnh kiểm tra desktop/mobile nằm trong `test-results/` (được gitignore). Không chạy hai bộ Playwright đồng thời vào cùng thư mục output.

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
npm.cmd run dev
```

`npm.cmd ci` cài đúng phiên bản trong `package-lock.json`. Sau khi phát triển, kiểm tra và đóng gói bằng:

```powershell
npm.cmd run lint
npm.cmd run build
npm.cmd run preview
```

Chạy `npm.cmd run preview` sau khi build để xem bản đóng gói. Trên PowerShell dùng `npm.cmd` nếu chính sách thực thi chặn `npm.ps1`.

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

You can also install [eslint-plugin-react-x](https://npmx.dev/package/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://npmx.dev/package/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```
