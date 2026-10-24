# TravelQ1 1.0.0

Bản phát hành chính thức đầu tiên của TravelQ1, thay cho các bản prerelease demo trước đây.

## Có trong bản này

- Bản đồ là màn chính, tìm kiếm phụ, Home, nút GPS về vị trí hiện tại và box mô tả địa điểm.
- Chi tiết chọn ngôn ngữ thuyết minh + FULL/SHORT, đọc nội dung tương ứng, audio/TTS với timeline phút:giây, tua ±10 giây và tốc độ phát.
- Profile tùy chọn: khách vào app ngay; đăng nhập email bằng OTP khi backend Gmail được cấu hình. Ghi lượt nghe thật, retry không nhân đôi sự kiện.
- Giao diện Tiếng Việt/English độc lập với ngôn ngữ thuyết minh; sáng/tối/theo hệ thống.
- 7 địa điểm thật, 84 bài thuyết minh; thu hồi điểm hư cấu trong cả gói offline cũ. Cache guide và âm thanh TTS, gói offline có kiểm tra chữ ký/checksum.
- Thông báo điều khiển khi đang nghe ở nền; ẩn thông báo của app khi quay lại.

## Cài đặt và kết nối

Tải **TravelQ1-v1.0.0.apk**. Package: `vn.hvp.travelvoice`; versionCode 1; Android 8.0+; APK ký release riêng, không debuggable, có R8/resource shrinking.

**Theo lựa chọn của chủ dự án, APK lần này dùng backend local tại `http://10.0.2.2:8080/`, dành cho Android Emulator.** Trên điện thoại thật cần build với địa chỉ backend LAN/HTTPS phù hợp. Chưa kèm backend online công khai. Source code và Compose được cung cấp cùng tag; xem QUICKSTART đính kèm và `android/README.md`.

Gmail SMTP chưa có tài khoản gửi cấu hình tại lần kiểm tra: chưa xác minh giao OTP tới hộp thư thật. Khách vẫn dùng được; quản trị viên cần cấu hình Gmail ở backend. Google Maps cần key được cấp quyền cho package/chứng chỉ release. Ảnh địa điểm chưa có nguồn published trong dữ liệu hiện tại. Force-stop dừng phát; việc nghe nền không đồng nghĩa tiếp tục sau force-stop.

Nội dung biên tập được giữ theo nguồn đã có trong database; chưa thay đổi PRD hoặc tuyên bố đã nghiệm thu toàn bộ PRD/khảo sát thực địa. Các bản prerelease cũ được giữ để truy vết.

## Kiểm chứng

- APK ký release: `assembleRelease` thành công, chữ ký APK v2 hợp lệ, RSA 3072 bit; version 1.0.0 / code 1.
- Android: 19/19 unit tests đạt; `lintRelease` có 0 lỗi và 45 cảnh báo. Backend: 91/91 tests đạt trên database kiểm thử riêng.
- Mở mới APK release trên Android Emulator: giao diện tiếng Việt, tải 7 địa điểm thật từ backend local; không ghi nhận crash/ANR trong lần kiểm tra này.
- Đổi giao diện sang English hoạt động độc lập ngôn ngữ thuyết minh. EN/FULL phát TTS với timeline thực 0:27/0:54; khi phát lại và ra nền có notification media, mở app lại thì notification được gỡ.
- Chưa hoàn tất kiểm chứng Maps trên release: đang chờ cấp quyền package/chứng chỉ release trong Google Cloud. Chưa xác minh gửi Gmail OTP tới hộp thư thật.
- SHA-256 APK: `404fd793459710902a0475332bb73ddc9916437d2b5cf10902a03fc18ce839aa`.
