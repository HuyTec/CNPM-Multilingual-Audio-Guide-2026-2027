# TravelVoice · Web User dành cho du khách

Tài liệu giúp thành viên nhóm nắm được Web User làm gì, dữ liệu đi qua đâu,
cách chạy và phần còn cần nghiệm thu. Cập nhật ngày **24/10/2026**.

## Hiện trạng để đọc nhanh

Web User là website cho du khách khám phá địa điểm, đọc và nghe thuyết minh trên
điện thoại/máy tính. Không cần cài app hoặc đăng nhập để mở địa điểm. Web Admin là
ứng dụng quản trị riêng; Android là client du khách khác dùng chung backend,
locationId, contentId và gói nội dung với Web User.

Source hiện đã có cả phần geofence và outbox playback bền vững, cùng thay đổi
backend và kiểm thử tương ứng. Chức năng có trong source không đồng nghĩa đã
được nghiệm thu với dữ liệu khảo sát, GPS hoặc thông báo nền trên thiết bị thật.

| Phần | Đã triển khai | Giới hạn cần hiểu |
| --- | --- | --- |
| Khám phá — UC-01 | Bản đồ mở ở 7 địa điểm, gợi ý tìm tên có/không dấu, ghim và danh sách có ảnh; chọn điểm mở ảnh + nút vào bài thuyết minh. | Bản đồ nền cần mạng; tọa độ demo chỉ minh họa. |
| GPS — UC-01 | Chỉ xin vị trí khi bấm biểu tượng định vị/Lấy vị trí hiện tại; lọc 50–500 m, mặc định 100 m; xếp khoảng cách tăng dần. | Cần quyền và HTTPS/localhost; sai số GPS có thể lớn. Một lần lấy GPS không chứng minh geofence hoạt động. |
| Đọc bài — UC-02 | VI/EN/FR/JA/ZH/RU, FULL/SHORT; backend trả nội dung APPROVED, fallback Selected → EN → VI và giữ loại bài. | Nội dung/tên dịch demo vẫn cần thẩm định; trạng thái fixture không phải chứng nhận biên tập. |
| Nghe bài — UC-03 | Có audio nữ VI cho 7 điểm × FULL/SHORT; ưu tiên audio đúng bài, lỗi/thiếu thì thử TTS đúng ngôn ngữ và nguyên văn bài; có dừng, pause/resume, tua và tốc độ. | TTS tùy giọng trên thiết bị; mở trang, đổi bài hoặc chọn thông báo không tự phát audio. |
| Offline — UC-04 | Xác nhận tải, kiểm tra chữ ký/checksum JSON/audio, cài nguyên tử; giữ bản cũ khi lỗi; bảo vệ BASE và xác nhận trước khi xóa OPTIONAL. | Phải chuẩn bị nội dung/app shell từ trước; không có bản đồ nền offline; text không bảo đảm TTS offline. |
| Geofence — UC-05 | Bật riêng watchPosition; kiểm tra vào/ra có xét sai số/GPS cũ; banner sáu ngôn ngữ, cooldown IndexedDB dùng chung tab/reload; có adapter thông báo hệ thống. | Chỉ dùng gói có dữ liệu khảo sát/cooldown. Demo chưa đủ đầu vào; chưa bảo đảm GPS nền liên tục khi khóa màn hình/đóng tab. |
| Đồng bộ lượt nghe | Lưu IndexedDB sau callback bắt đầu phát; tự gửi lại qua mất mạng/reload, backoff và eventId chống đếm trùng; Background Sync nếu hỗ trợ. | Lỗi lưu trữ phải báo nguy cơ mất sự kiện; sự kiện bị từ chối vĩnh viễn được giữ để kiểm tra. |

### Luồng sử dụng chính

1. Mở khám phá hoặc link địa điểm.
2. Chọn địa điểm, ngôn ngữ và FULL/SHORT để đọc bài.
3. Bấm **Nghe thuyết minh** để nghe; đọc/điều hướng chưa tính là lượt nghe.
4. Vào **Nội dung offline**, kiểm tra và xác nhận tải gói khi có mạng nếu muốn dùng offline.
5. Bật riêng **Gợi ý theo vị trí** nếu muốn nhận banner; thiếu dữ liệu khảo sát/cooldown sẽ được báo chưa sẵn sàng.

## Các trang và kết nối dữ liệu

| URL | Chức năng |
| --- | --- |
| `/` | Khám phá bằng bản đồ, tìm kiếm, danh mục, danh sách và dán link địa điểm. |
| `/locations/:locationId?language=VI&scriptType=FULL` | Đọc/nghe đúng địa điểm; giữ ngôn ngữ/loại bài khi reload hoặc Back/Forward. |
| `/offline` | Kiểm tra, tải, xác minh và quản lý gói đã lưu. |

Từ khóa, ngôn ngữ, bộ lọc, địa điểm được chọn (`place`), bảng danh sách, bán kính và trang kết quả được giữ
trong URL. Tọa độ GPS người dùng không được đưa vào URL hoặc gửi lên backend.

```text
Trình duyệt du khách
  ├─ React + Vite + TypeScript + Tailwind + React Router
  ├─ /api cùng origin → proxy → Spring Boot → PostgreSQL/PostGIS
  ├─ IndexedDB: gói đã xác minh, outbox playback, cooldown
  └─ Service worker production: app shell offline, Sync và chọn thông báo
```

Leaflet hiển thị nền OpenStreetMap, tự chuyển sang OSM France/HOT khi nguồn chính lỗi hoặc quá hạn. Không còn link OsmAnd. Backend quyết
định nội dung public/ngôn ngữ thực tế; frontend không tự sinh hoặc dịch lại bài.
Receipt gồm eventId/contentId/occurredAt/offline; backend giữ thời điểm nghe ban
đầu và trạng thái offline khi gửi trễ. Đồng hồ/offline là dữ liệu client báo.
API playback và xuất bản gói của bộ triển khai hiện vẫn thuộc profile `demo`.

Migration V4 thêm `geofence_configuration` và `location_geofence_surveys`, bắt đầu
rỗng. Chỉ xuất metadata geofence khi khảo sát khớp tọa độ/bán kính và đủ tên sáu
ngôn ngữ; chưa tự đặt cooldown nghiệp vụ hoặc chứng nhận các ghim demo.

## Đọc code theo thứ tự nào?

| Nhóm file | Vai trò |
| --- | --- |
| `src/main.tsx`, `src/pages/LocationEntry.tsx` | Layout, routes, đăng ký worker và vào khám phá. |
| `src/components/LocationCatalog.tsx`, `TourMap.tsx`, `LocationSearch.tsx`, `NearbyPlaces.tsx` | Bản đồ, bộ lọc, danh sách và kết quả quanh GPS. |
| `src/lib/useBrowserLocation.ts`, `useLocationCatalog.ts`, `osmMap.ts` | Xin GPS, tải địa điểm, tính khoảng cách; `mapTiles.ts` quản lý nguồn nền và lỗi tải, `locationPhotos.ts` gắn ảnh theo ID. |
| `src/pages/GuidePage.tsx`, `src/lib/useGuide.ts`, `guide.ts` | Chọn/kiểm tra bài theo địa điểm, ngôn ngữ và FULL/SHORT. |
| `src/components/GuidePlayer.tsx`, `src/lib/playback.ts`, `usePlaybackReceipt.ts` | Điều khiển audio/TTS và tạo sự kiện khi thực sự phát. |
| `src/pages/OfflinePage.tsx`, `src/lib/packages.ts` | Tải, xác minh, lưu và đọc gói offline. |
| `src/lib/playbackOutbox.ts`, `public/playback-outbox.js`, `src/components/PlaybackSyncStatus.tsx` | Outbox bền vững và trạng thái đồng bộ; page/worker dùng chung phần lưu trữ. |
| `src/lib/geofence.ts`, `useGeofencing.ts`, `src/components/GeofenceNotices.tsx` | Dữ liệu geofence, vào/ra, cooldown, quyền và banner/thông báo. |
| `public/sw.js`, `scripts/public-server.mjs` | Worker production và gateway chỉ phục vụ app/API công khai. |

## Kết quả đã kiểm chứng và phần còn thiếu

Ngày **24/10/2026**, sau thay đổi bản đồ/tìm kiếm/ảnh/audio: **94 kiểm thử backend** đạt trên
PostGIS riêng; **16 ca bản đồ/GPS**, **4 ca offline production** và **51 ca hồi quy** đạt
(hai ca bản đồ/tour dùng API thật được chạy lại sau khi sửa vòng đời Leaflet).
Popup đã kiểm tra ở 320/768/1024/1440 px, không tràn ngang và axe không có vi phạm.
QR mới đã thử qua Chrome giả lập Pixel 7 và iPhone 13: HTTPS, 7 ảnh/địa điểm, MP3 VI phát thật,
GPS giả lập và chặn route nội bộ đều đạt. Đây chưa phải kiểm tra trên Safari/iPhone hoặc GPS vật lý.
Nếu từng tải gói BASE cũ, vào **Nội dung offline → Kiểm tra cập nhật** và tải lại gói để có MP3 VI.

Bổ sung cùng ngày: **18 ca bản đồ/GPS/kiểu chữ** đạt sau điều chỉnh giữ ghim khi tìm kiếm,
đánh dấu đỏ và xóa ô tìm kiếm để tự reset. Có Back/Forward, giữ focus và kiểm tra font/axe
tại 320/390/768/1024/1440 px; phần lưu ý demo được gỡ khỏi UI và giữ trong tài liệu.

Ở phiên triển khai ngày 23/10 có **93 kiểm thử backend** đạt trên PostGIS riêng,
**69 ca trình duyệt** đạt qua các bộ chạy; một ca tiles OSM live được bỏ qua.
Đã kiểm tra WAV thật từ gói offline → phát → lưu sự kiện → reload khi mất mạng →
kết nối lại tự gửi → replay cùng UUID không tăng lượt. Đây là kết quả của phiên
triển khai; không tự thay thế một lần chạy kiểm thử mới khi thay đổi source.

Geofence dùng gói ký/GPS/notification adapter giả lập trong test. Sync được kích
hoạt qua Chrome DevTools, notificationclick là sự kiện tổng hợp; không chứng minh
GPS nền/OS notification trên điện thoại thật. Dữ liệu khảo sát, tên/nội dung đã
thẩm định, cooldown được nhóm chốt, ma trận thiết bị/voice TTS và triển khai HTTPS
ổn định vẫn cần hoàn thiện trước nghiệm thu toàn bộ PRD.

Nguồn yêu cầu: [PRD tiếng Anh](../docs/en/PRD_Report.md) và
[PRD tiếng Việt](../docs/vi/PRD_Report_vi.md). Thiết kế, review và quyết định mở
được theo dõi ở [HVP-1](https://trannhuthuy897.atlassian.net/browse/HVP-1),
[HVP-6](https://trannhuthuy897.atlassian.net/browse/HVP-6) và
[HVP-7](https://trannhuthuy897.atlassian.net/browse/HVP-7).

## Mở bằng điện thoại qua mã QR

QR demo hiện tại nằm ở [ảnh PNG](../docs/qr/web-user-demo.png), [bản SVG để in](../docs/qr/web-user-demo.svg)
và [đường dẫn HTTPS](../docs/qr/web-user-demo.txt). Dùng camera Android/iPhone quét ảnh rồi chọn mở liên kết.
Điện thoại có thể dùng Wi-Fi khác hoặc 4G/5G; không cần cùng mạng với máy tính.
GPS cần được cấp quyền trên điện thoại; giám sát geofence cần bật riêng bằng nút gợi ý theo vị trí.

Đây là link demo Cloudflare Quick Tunnel: máy tính, Docker/backend, cổng Web User và tunnel phải tiếp tục chạy.
Tắt/ngủ máy hoặc dừng tunnel sẽ làm link không truy cập được; tạo tunnel mới có thể đổi URL và QR.
Để dùng một QR cố định lâu dài, cần triển khai Web User/backend lên hosting HTTPS và dùng tên miền cố định.
Đặc tính link tạm: [Cloudflare Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

Khởi động lại từ thư mục gốc dự án (máy này đã chuẩn bị công cụ):

```powershell
docker compose up -d backend postgres
powershell -ExecutionPolicy Bypass -File .\web-user\scripts\share-phone.ps1
```

Script giữ lại link cũ nếu tunnel còn khỏe; nếu tạo link mới sẽ ghi lại cả PNG, SVG và TXT trong `docs/qr/`.
Muốn cập nhật giao diện khi link đang chạy, chạy `npm run build` tại `web-user`, rồi tải lại trang trên điện thoại.
Dừng đúng các tiến trình chia sẻ do script tạo:

```powershell
powershell -ExecutionPolicy Bypass -File .\web-user\scripts\share-phone.ps1 -Stop
```

Cổng chia sẻ dùng production build tại `127.0.0.1:4176` (đổi bằng `PUBLIC_WEB_PORT`), chuyển tiếp những API
công khai cần cho du khách. Không chuyển tiếp API quản trị, actuator, mã nguồn Vite hoặc file `.env`.
Không dùng trực tiếp Vite dev server làm địa chỉ công khai.

Trên máy khác, chuẩn bị Node, Python với Pillow và các thư viện QR cục bộ trước khi chạy script:

```powershell
New-Item -ItemType Directory -Path .local-tools/python -Force
python -m pip install --target .local-tools/python "qrcode[pil]"
```

Tải cloudflared từ [release chính thức](https://github.com/cloudflare/cloudflared/releases), lưu executable Windows
tại `.local-tools/cloudflared.exe` và đối chiếu SHA-256 với asset của release. Máy này dùng bản `2026.10.3`.
Thư mục `.local-tools` chứa công cụ, PID và log cục bộ, được bỏ qua bởi Git. Không cần token Cloudflare cho link demo.

Khi đã có tên miền HTTPS cố định, tạo lại QR bằng:

```powershell
python .\web-user\scripts\make-qr.py https://ten-mien-cua-ban.example
```

Kiểm tra cổng và link public:

```powershell
cd web-user
npm run test:gateway
node scripts/phone-share.smoke.mjs
```

Smoke test dùng Chrome với kích thước/user-agent Android và iPhone, GPS giả lập; đây không phải bằng chứng
chạy trên Safari/iOS vật lý. Không tự phát audio hoặc ghi lượt nghe khi chạy test này.

## Mở bằng điện thoại qua mã QR

QR demo hiện tại nằm ở [ảnh PNG](../docs/qr/web-user-demo.png), [bản SVG để in](../docs/qr/web-user-demo.svg)
và [đường dẫn HTTPS](../docs/qr/web-user-demo.txt). Dùng camera Android/iPhone quét ảnh rồi chọn mở liên kết.
Điện thoại có thể dùng Wi-Fi khác hoặc 4G/5G; không cần cùng mạng với máy tính.
GPS cần được cấp quyền trên điện thoại; giám sát geofence cần bật riêng bằng nút gợi ý theo vị trí.

Đây là link demo Cloudflare Quick Tunnel: máy tính, Docker/backend, cổng Web User và tunnel phải tiếp tục chạy.
Tắt/ngủ máy hoặc dừng tunnel sẽ làm link không truy cập được; tạo tunnel mới có thể đổi URL và QR.
Để dùng một QR cố định lâu dài, cần triển khai Web User/backend lên hosting HTTPS và dùng tên miền cố định.
Đặc tính link tạm: [Cloudflare Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

Khởi động lại từ thư mục gốc dự án (máy này đã chuẩn bị công cụ):

```powershell
docker compose up -d backend postgres
powershell -ExecutionPolicy Bypass -File .\web-user\scripts\share-phone.ps1
```

Script giữ lại link cũ nếu tunnel còn khỏe; nếu tạo link mới sẽ ghi lại cả PNG, SVG và TXT trong `docs/qr/`.
Muốn cập nhật giao diện khi link đang chạy, chạy `npm run build` tại `web-user`, rồi tải lại trang trên điện thoại.
Dừng đúng các tiến trình chia sẻ do script tạo:

```powershell
powershell -ExecutionPolicy Bypass -File .\web-user\scripts\share-phone.ps1 -Stop
```

Cổng chia sẻ dùng production build tại `127.0.0.1:4176` (đổi bằng `PUBLIC_WEB_PORT`), chuyển tiếp những API
công khai cần cho du khách. Không chuyển tiếp API quản trị, actuator, mã nguồn Vite hoặc file `.env`.
Không dùng trực tiếp Vite dev server làm địa chỉ công khai.

Trên máy khác, chuẩn bị Node, Python với Pillow và các thư viện QR cục bộ trước khi chạy script:

```powershell
New-Item -ItemType Directory -Path .local-tools/python -Force
python -m pip install --target .local-tools/python "qrcode[pil]"
```

Tải cloudflared từ [release chính thức](https://github.com/cloudflare/cloudflared/releases), lưu executable Windows
tại `.local-tools/cloudflared.exe` và đối chiếu SHA-256 với asset của release. Máy này dùng bản `2026.10.3`.
Thư mục `.local-tools` chứa công cụ, PID và log cục bộ, được bỏ qua bởi Git. Không cần token Cloudflare cho link demo.

Khi đã có tên miền HTTPS cố định, tạo lại QR bằng:

```powershell
python .\web-user\scripts\make-qr.py https://ten-mien-cua-ban.example
```

Kiểm tra cổng và link public:

```powershell
cd web-user
npm run test:gateway
node scripts/phone-share.smoke.mjs
```

Smoke test dùng Chrome với kích thước/user-agent Android và iPhone, GPS giả lập; đây không phải bằng chứng
chạy trên Safari/iOS vật lý. Không tự phát audio hoặc ghi lượt nghe khi chạy test này.

React + Vite + TypeScript + Tailwind; cùng locationId, guide và gói nội dung với Android.
Hợp đồng: [GUIDE_API.md](../backend/GUIDE_API.md) và
[CONTENT_PACKAGE_API.md](../backend/CONTENT_PACKAGE_API.md).

```powershell
cd web-user
npm ci
npm run dev
```

Mở http://127.0.0.1:5175/ để chọn địa điểm. Proxy đọc `BACKEND_PORT` từ `.env` ở thư mục gốc
(nếu chưa đặt thì dùng 8080). Có thể ghi đè bằng `API_PROXY_TARGET` trong `web-user/.env.local`.
Backend dùng profile demo; phần geofence mới cần migration V4. Môi trường được kiểm tra ngày 22/10/2026 dùng cổng 8081.
Danh sách mặc định là bảy điểm từ tài liệu nhóm cung cấp; vẫn có lựa chọn tất cả
địa điểm cũ. Nội dung chưa thẩm định; tọa độ chỉ minh họa.

- URL /locations/:locationId?language=VI&scriptType=FULL giữ lựa chọn khi reload/back.
  Sáu ngôn ngữ VI/EN/FR/JA/ZH/RU; server quyết định APPROVED và fallback Selected → EN → VI,
  giữ FULL/SHORT. 404 không dùng bài offline cũ để thay nội dung đã ngừng công khai.
- Audio đúng bài được ưu tiên; lỗi/thiếu audio dùng TTS đúng effective language và
  nguyên văn scriptText. Chỉ phát khi bấm, có pause/resume, seek và tốc độ. TTS cần
  voice tương ứng trên trình duyệt/thiết bị; voice mạng không bảo đảm hoạt động offline.
  Lượt nghe được đưa vào IndexedDB khi media/TTS báo bắt đầu, giữ eventId/contentId,
  thời điểm bắt đầu và trạng thái offline. Tự đồng bộ khi mở lại/có mạng/trở lại trang,
  có backoff và nút thử lại; cùng eventId tránh đếm trùng khi mất response.
  Background Sync dùng service worker nếu trình duyệt hỗ trợ; nếu không, hàng đợi gửi khi mở trang.
  Lỗi 4xx không thể thử lại được giữ riêng để kiểm tra; lỗi bộ nhớ được báo rõ, không hứa lưu bền khi thiết bị chặn lưu trữ.
- /offline: kiểm tra → xác nhận tải → xác minh chữ ký/checksum JSON và từng audio →
  cài đặt nguyên tử. BASE được bảo vệ; OPTIONAL có thể xóa sau xác nhận. Bản cũ giữ
  nguyên khi lỗi/hủy. Chỉ thông báo sẵn sàng offline khi gói đã được xác minh.
- Mở lại ứng dụng offline cần production build/preview và service worker đã cài
  trong lần online trước. Dev server không cung cấp app shell offline. HTTPS cần
  cho triển khai ngoài localhost; chưa deploy.

## Bản đồ, tìm kiếm và ảnh địa điểm

Trang chủ mở bản đồ vừa khung **7 địa điểm trong danh mục demo**, dù người dùng đang ở Bình Dương
hay nơi khác. Không tự lấy GPS khi mở. Bấm biểu tượng **Định vị vị trí hiện tại** ở góc dưới phải
hoặc **Lấy vị trí hiện tại** để chuyển bản đồ tới vị trí của mình. Bật GPS trên điện thoại không tự
cấp quyền cho website; trình duyệt cần HTTPS/localhost và quyền vị trí. GPS không gửi lên backend.

Gõ `D` để thấy gợi ý **Dinh Độc Lập**; hỗ trợ tìm không dấu (`dinh doc lap`), phím mũi tên,
Enter để chọn và Escape để đóng. Chọn gợi ý hoặc bấm kính lúp sẽ chuyển ghim của địa điểm phù hợp
sang **đỏ**, mở popup có đúng ảnh/tên/nút **Mở bài thuyết minh**, đồng thời giữ toàn bộ ghim khác
và số thứ tự trong danh mục. Xóa hết chữ hoặc bấm **Xóa tìm kiếm** sẽ đóng popup, bỏ đánh dấu và
tự đưa bản đồ về khung mặc định ngay, không cần bấm tìm lại. Không có kết quả thì báo để thử tên khác,
vẫn giữ các ghim/danh sách. Link giữ ngôn ngữ và FULL/SHORT;
không tự phát khi tìm kiếm. `place` lưu ID đang mở trong URL để reload/Back khôi phục lựa chọn.
Nút **Danh sách** mở danh mục và các địa điểm có ảnh nhỏ; chọn một điểm đóng bảng và mở popup.
Không còn liên kết OsmAnd.

Giao diện du khách bỏ các lưu ý và hậu tố demo; tài liệu này vẫn giữ thông tin về nguồn dữ liệu và
giới hạn kiểm chứng. Font sans-serif hệ thống được dùng thống nhất cho ô tìm kiếm, bản đồ Leaflet,
popup, danh sách và trang nghe. Ô tìm kiếm/chọn ngôn ngữ dùng chữ 16 px; tên dài xuống dòng,
giao diện được rà ở 320/390/768/1024/1440 px.

Bảy ảnh do người dùng cung cấp được lưu trong `src/assets/locations/`, nén WebP và gắn theo ID
trong `src/lib/locationPhotos.ts`, gồm Bến Nhà Rồng, Chứng tích Chiến tranh, Dinh Độc Lập,
Nhà hát Thành phố, Nhà thờ Đức Bà, Đại học Sài Gòn – Cơ sở 2 và Bitexco. Trang bài đọc cũng hiện ảnh.
Ảnh được đóng gói cùng app shell production; không phải dữ liệu ảnh do backend quản lý.
Các ảnh và tọa độ demo không thay thế kết quả khảo sát/biên tập.

Bản đồ nền cần Internet. Nguồn chính là `tile.openstreetmap.org`; nếu lỗi tải hoặc quá 10 giây,
ứng dụng thử nguồn dự phòng OSM France/HOT. Nếu cả hai lỗi, báo lỗi hữu hạn, giữ danh sách/ảnh/bài đọc
và cho **Tải lại bản đồ**. Có ghi công nguồn trên bản đồ; không tải trước hàng loạt hoặc lưu tile offline.
Nguồn dự phòng chỉ phù hợp demo công khai, phi lợi nhuận, lưu lượng vừa phải theo
[điều kiện OSM France](https://www.openstreetmap.fr/usage/). Production có lưu lượng lớn/thương mại cần
nhà cung cấp phù hợp hoặc tự host. Không cần Google Maps API key cho Web User;
Android tiếp tục dùng key Google Maps SDK riêng theo `android/README.md`.

### Giọng nữ tiếng Việt

Migration demo `V3_5__vietnamese_female_narration` nạp 14 MP3 nữ **vi-VN-HoaiMyNeural**
(tốc độ -8%, cao độ -2 Hz) cho đúng văn bản VI đã APPROVED, đủ FULL/SHORT của 7 điểm.
Audio tổng hợp được tạo sẵn và lưu trong backend, nên máy du khách **không cần cài giọng VI**.
API `/api/content-audio/:id.mp3` và gói offline dùng cơ chế công bố/checksum hiện có.
Migration so SHA-256 văn bản, kiểm tra byte audio và không ghi đè audio Admin đã có.
Không đổi bài/ngôn ngữ để né lỗi TTS. Các ngôn ngữ chưa có audio vẫn phụ thuộc giọng máy.

Sau khi pull source mới, chạy từ thư mục gốc trong Terminal VS Code/PowerShell (Docker Desktop đang chạy):

File `.env` ở thư mục gốc cần `SPRING_PROFILES_ACTIVE=demo` như phần thiết lập backend;
migration audio mẫu chỉ chạy trong profile demo.

```powershell
docker compose up -d --build backend postgres
cd web-user
npm ci
npm run build
npm run dev
```

File MP3 đã có trong repo; không cần gọi dịch vụ tạo giọng khi chạy app. Nếu cần tạo lại cho văn bản
đã duyệt mới, xem `backend/scripts/generate-demo-narration.py` (dùng `edge-tts==7.3.1`;
gửi văn bản công khai tới Microsoft). Không sửa audio/manifest của migration đã áp dụng: tạo migration mới
để giữ kiểm tra checksum Flyway. Giọng tổng hợp chưa thay thế nghiệm thu nội dung/giọng đọc thực tế.

### Tìm quanh GPS

Bấm **Lấy vị trí hiện tại**, cấp quyền rồi kéo **Phạm vi quét** dưới bản đồ từ 50–500 m
(mặc định 100 m, mỗi bước 1 m; hỗ trợ phím mũi tên, Home/End). Vòng tròn thể hiện bán kính;
danh sách phía dưới chỉ giữ các địa điểm trong phạm vi và xếp từ gần nhất đến xa dần.
Ban đầu hiển thị 5 điểm gần nhất; bấm **Xem tất cả địa điểm trong phạm vi** để mở rộng.
Ứng dụng quét toàn bộ các trang địa điểm trong danh mục hiện tại, tính đường chim bay trên trình duyệt;
không gửi GPS lên API hay lưu tọa độ vào URL. Danh sách dài phân trang 50 mục.

GPS chỉ lấy sau thao tác người dùng, bấm **Cập nhật vị trí** khi di chuyển. Mỗi yêu cầu bỏ qua cache
và chờ tối đa 12 giây để có điểm ban đầu; nếu sai số hơn 25 m hoặc chưa có điểm, thử GPS chính xác hơn tối đa 45 giây.
Nút cập nhật yêu cầu vị trí mới, không dùng lại cache cũ. Một lần thử chính xác lỗi không xóa điểm gần đúng đã có.
Trên điện thoại nên mở liên kết trực tiếp bằng Safari/Chrome, bật Dịch vụ vị trí và Vị trí chính xác cho trình duyệt,
chọn Cho phép khi được hỏi. Web không thể tự bật GPS hoặc vượt qua quyền người dùng đã từ chối. Trình duyệt báo độ chính xác;
nếu sai số lớn hơn bán kính, kết quả có thể sai lệch. Tọa độ demo chưa kiểm chứng,
không thay thế dữ liệu định vị đã khảo sát. Thiếu quyền GPS vẫn tìm kiếm thủ công được.

### Kiểm tra kết nối backend

Mốc kiểm tra local trước đó dùng backend tại `127.0.0.1:8081`; chạy `docker compose ps` để kiểm tra trạng thái hiện tại.
Web User gọi cùng origin `/api` qua Vite; không cần mở CORS hoặc nhúng mật khẩu backend vào mã frontend.

```powershell
Invoke-RestMethod 'http://127.0.0.1:8081/actuator/health'
Invoke-RestMethod 'http://127.0.0.1:5175/api/locations?size=50&categoryId=10000000-0000-0000-0000-000000000101'
Invoke-RestMethod 'http://127.0.0.1:5175/api/categories'
```

Nếu đổi cổng backend, sửa `BACKEND_PORT` tại `.env` gốc hoặc `API_PROXY_TARGET`, rồi khởi động lại Vite.
API phân trang chỉ chấp nhận `size` từ 1 đến 50; web tải tuần tự mọi trang để quét đủ địa điểm.
Trong **Danh sách**, chọn **Danh mục** để duyệt nhóm hoặc tất cả địa điểm; mặc định vẫn là tuyến 7 điểm.

Gói cơ bản cần xác nhận tải và xác minh trước khi dùng offline. Production build xuất
`asset-manifest.json`; service worker lưu cả JS/CSS tải chậm để danh sách vẫn mở được sau khi mất mạng.
Audio đã lưu được kiểm tra checksum trước khi báo gói đã xác minh. Test offline phải chạy bằng cấu hình
`playwright.offline.config.ts` với production preview; cấu hình test mặc định không chạy nhóm này.

### Gợi ý theo vị trí — UC-05

Bấm **Bật gợi ý theo vị trí** để mở phiên `watchPosition` xuyên các trang. Phiên kết thúc khi tắt
hoặc tải lại/đóng trang; phải bật lại sau khi mở lại. Khác với nút GPS trên bản đồ, phiên này nhận
các cập nhật vị trí và kiểm tra vào/ra geofence. Không lưu/gửi tọa độ người dùng lên máy chủ.

Chỉ theo dõi các điểm trong gói cục bộ đã xác minh có metadata khảo sát, tên VI/EN/FR/JA/ZH/RU
và cooldown do nguồn xuất bản cấu hình. Xem [hợp đồng xuất bản](../backend/CONTENT_PACKAGE_API.md).
Gói demo hiện không có chứng nhận khảo sát hoặc cooldown; vì vậy giao diện báo chưa sẵn sàng
thay vì phát thông báo từ tọa độ minh họa. Cần tải lại gói sau khi nguồn xuất bản cung cấp dữ liệu.

Vào geofence hiển thị banner theo ngôn ngữ ứng dụng. Chọn banner mở đúng locationId ở FULL;
không chọn thì không điều hướng, không tự phát audio/TTS. Cooldown lưu IndexedDB và dùng chung
giữa các tab/reload. GPS quá 15 giây, tọa độ không hợp lệ, hoặc vùng sai số chưa nằm hẳn trong
geofence không được coi là vào vùng; cần ra ngoài rõ ràng trước khi ghi nhận lần vào tiếp theo.

**Cho phép thông báo hệ thống** xin quyền sau click. Chỉ gọi system notification khi có cập nhật
GPS mới trong lúc trang ẩn, có service worker và được cấp quyền; không bảo đảm trình duyệt sẽ
cung cấp các cập nhật ấy. Trang báo rõ giới hạn khi ẩn, chờ GPS mới khi trở lại. Theo
[đặc tả Geolocation](https://www.w3.org/TR/geolocation/), cập nhật vị trí phụ thuộc trạng thái hiển thị
và hoạt động của tài liệu; service worker không cung cấp một dịch vụ GPS liên tục khi đóng tab.
Từ chối GPS vẫn tìm thủ công được; từ chối notification vẫn dùng banner khi trang hoạt động.
Chrome/Safari/Firefox trên thiết bị thật, khóa màn hình và chuyển ứng dụng còn phải kiểm chứng.

## Kiểm tra

```powershell
npm run lint
npm run build
npm run test:gateway
npm run test:e2e
$env:LIVE_GUIDE_API='1'
npm run test:e2e -- guide.live.spec.ts tour.live.spec.ts
npm run test:e2e -- --config playwright.offline.config.ts
npm run test:e2e -- --config playwright.background.config.ts
npm run test:e2e -- --config playwright.maps.config.ts
Remove-Item Env:LIVE_GUIDE_API
```

Nếu chưa cài Chromium của Playwright nhưng máy đã có Google Chrome, đặt
`$env:PLAYWRIGHT_CHANNEL='chrome'` trước khi chạy cấu hình `playwright.maps.config.ts`.
Test này dùng GPS, API và tile giả lập; không đọc vị trí thật của thiết bị.

`geofence.spec.ts` dùng gói ký và tọa độ khảo sát **fixture**, GPS giả lập; `outbox.spec.ts`
kiểm tra lưu bền, gửi lại, cạnh tranh tab và lỗi bộ nhớ. `background.spec.ts` chạy production worker,
phát sự kiện Sync bằng Chrome DevTools và sự kiện chọn thông báo tổng hợp. Đây là kiểm tra adapter,
không phải bằng chứng thông báo OS thật hay GPS nền trên điện thoại. Không dùng số test đạt để suy ra
toàn bộ PRD đã hoàn thành.

Guide/playback UI tests giả lập API hoặc media adapter, không chứng minh DB/voice thật.
Guide/tour live tests dùng backend/PostgreSQL thật. Offline live dùng production preview
cổng 4175 và gói thật; ca lỗi chủ động sửa response để kiểm tra checksum. Map tests kiểm
tra marker, đúng ảnh/ID popup, gợi ý bàn phím, GPS sau click và chuyển nguồn nền; chúng có thể giả lập tile để không phụ
thuộc mạng bên ngoài.

Triển khai cần reverse proxy /api và /demo-audio, SPA fallback index.html, HTTPS và tile
provider phù hợp lưu lượng. Chưa có bằng chứng CI/CD, thiết bị thật hoặc đủ sáu voice TTS.

Bộ VPS độc lập laptop và quy trình chuyển database được hướng dẫn tại
[deploy/vps/README.md](../deploy/vps/README.md). Cấu hình đã được chạy thử trong Docker cục bộ;
chưa có VPS, tên miền hay chứng chỉ HTTPS production thực tế.
