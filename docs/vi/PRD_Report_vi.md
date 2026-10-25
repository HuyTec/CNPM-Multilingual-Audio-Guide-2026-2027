**TÀI LIỆU YÊU CẦU SẢN PHẨM**
***Ứng dụng thuyết minh âm thanh đa ngôn ngữ***

> Bản dịch từ [PRD tiếng Anh](../en/PRD_Report.md). Các giá trị trong ngoặc vuông, đề xuất và chỗ trống được giữ theo bản nguồn; chưa phải quyết định đã được xác nhận hay kết quả đo thực tế. Các điểm chưa rõ của bản nguồn được ghi tại [Jira HVP-7](https://trannhuthuy897.atlassian.net/browse/HVP-7).

# **1. TỔNG QUAN SẢN PHẨM**

## **1.1. Tên sản phẩm**

*Ứng dụng thuyết minh âm thanh đa ngôn ngữ (Multilingual Audio Guide Application)*

## **1.2. Mục đích sản phẩm**

* Ứng dụng thuyết minh âm thanh đa ngôn ngữ giúp du khách khám phá điểm tham quan và tiếp cận nội dung đã được kiểm chứng qua Web du khách hoặc ứng dụng Android. Hai client phục vụ cùng tác nhân Du khách, dùng chung các use case cốt lõi và quy tắc nghiệp vụ.
* Hệ thống cho phép du khách khám phá địa điểm lân cận, tìm kiếm điểm tham quan, xem vị trí trên bản đồ, đọc nội dung hướng dẫn bằng nhiều ngôn ngữ, nghe thuyết minh và sử dụng tài liệu hướng dẫn thiết yếu ngay cả khi ngoại tuyến.
* Hệ thống cũng có Web Admin Dashboard để quản lý thông tin địa điểm, nội dung hướng dẫn đa ngôn ngữ, tệp âm thanh, thống kê hệ thống và phản hồi của du khách.

## **1.3. Người dùng mục tiêu**

Hệ thống được thiết kế cho các nhóm người dùng chính sau:

* **Du khách (Tourist):** Sử dụng Web du khách hoặc Android để khám phá địa điểm, đọc nội dung đa ngôn ngữ, nghe thuyết minh, nhận thông báo vị trí và quản lý nội dung ngoại tuyến theo điều kiện nền tảng tại mục 2.4. Du khách không cần tài khoản để truy cập.
* **Quản trị viên/Biên tập viên nội dung:** Sử dụng trang quản trị web để tạo và duy trì thông tin địa điểm, kịch bản thuyết minh và tệp âm thanh.
* **Quản trị viên hệ thống/Chủ sản phẩm:** Sử dụng trang quản trị web để theo dõi thống kê sử dụng hệ thống và quản lý phản hồi của du khách.

## **1.4. Mục tiêu sản phẩm**

Các mục tiêu chính của hệ thống:

* Giúp du khách dễ dàng khám phá và lựa chọn điểm tham quan.
* Cung cấp nội dung hướng dẫn đa ngôn ngữ chính xác và đã được kiểm chứng.
* Cung cấp trải nghiệm thuyết minh bằng âm thanh cho các địa điểm được hỗ trợ.
* Hỗ trợ truy cập ngoại tuyến vào nội dung hướng dẫn thiết yếu.
* Thông báo khi du khách đi vào khu vực của một địa điểm du lịch được hỗ trợ.
* Cung cấp công cụ quản lý nội dung địa điểm và tài nguyên âm thanh cho quản trị viên.
* Cung cấp thống kê cơ bản và quản lý phản hồi để hỗ trợ cải thiện dịch vụ trong tương lai.

**Chỉ số thành công đề xuất:** Các chỉ số sau hỗ trợ những mục tiêu sản phẩm hiện có. Mục tiêu trong ngoặc vuông cần được nhóm xác nhận; không phải kết quả đã đo. Các chỉ số được đánh giá từ dữ liệu phân tích thô hoặc kiểm thử nghiệm thu; riêng UC-07 chỉ hiển thị bốn chỉ số trong FR-12.

| Mục tiêu | Chỉ số thành công | Mục tiêu đề xuất/Ghi chú |
| ----- | ----- | ----- |
| Nội dung hướng dẫn đa ngôn ngữ chính xác và đã được kiểm chứng. | Tỷ lệ địa điểm được hỗ trợ có nội dung APPROVED bằng ít nhất \[2\] ngôn ngữ. | Tỷ lệ mục tiêu: \[40%\]. |
| Trải nghiệm thuyết minh bằng âm thanh. | Tỷ lệ phát âm thanh thành công. | Ít nhất \[95\]%. Điều kiện đo và cách tính trường hợp dùng TTS dự phòng: \[\]. |
| Dễ dàng khám phá và lựa chọn điểm tham quan. | Thời gian để đến màn hình Chi tiết địa điểm. | Tối đa \[10\] giây. Mốc bắt đầu đo và điều kiện kiểm thử: \[\]. |

# **2. PHẠM VI VÀ YÊU CẦU SẢN PHẨM**

## **2.1. Trong phạm vi**

**Hệ thống bao gồm các nhóm chức năng sau:**

Thuật ngữ **Ứng dụng du khách (Tourist Application)** bao gồm Web du khách và Android. Cách trình bày, điều hướng, quyền và adapter nền tảng có thể khác nhau; định danh nội dung, quy tắc duyệt/xuất bản, dự phòng ngôn ngữ, yêu cầu phát, an toàn gói và trạng thái phản hồi được dùng chung. Web Admin là giao diện riêng có phân quyền. Điều kiện nền tảng và nghiệm thu được quy định tại mục 2.4.

**Khám phá và chọn địa điểm**

* Khám phá các địa điểm du lịch lân cận.
* Tìm địa điểm theo từ khóa.
* Duyệt địa điểm theo danh mục.
* Chọn địa điểm từ bản đồ tương tác.
* Mở địa điểm qua URL riêng, gồm URL được mã hóa trong QR, không bắt buộc cài Android. Đây là đường vào của UC-01, không phải use case riêng hoặc chức năng quản lý QR.
* Use case liên quan: UC-01 – Khám phá và chọn địa điểm.

**Nội dung hướng dẫn đa ngôn ngữ**

* Truy xuất nội dung hướng dẫn đã được phê duyệt theo địa điểm, ngôn ngữ và chế độ nội dung.
* Hỗ trợ nội dung FULL và SHORT.
* Áp dụng ngôn ngữ dự phòng khi không có ngôn ngữ đã chọn.
* Use case liên quan: UC-02 – Truy cập nội dung hướng dẫn đa ngôn ngữ.

**Phát thuyết minh âm thanh**

* Phát âm thanh thuyết minh đã được tạo sẵn.
* Hỗ trợ tạm dừng, tiếp tục, tua và điều chỉnh tốc độ phát.
* Dùng Text-to-Speech (TTS) của nền tảng làm phương án dự phòng khi không có âm thanh.
* Use case liên quan: UC-03 – Phát thuyết minh âm thanh.

**Quản lý nội dung ngoại tuyến**

* Kiểm tra bản cập nhật nội dung đã được phê duyệt.
* Tải xuống và cài đặt các gói ngoại tuyến đã được xác minh.
* Quản lý nội dung tải thêm tùy chọn.
* Bảo vệ Base Content đã xác minh theo quy tắc cấp phát nền tảng tại mục 2.4.
* Use case liên quan: UC-04 – Quản lý và cập nhật nội dung ngoại tuyến.

**Thông báo theo vị trí**

* Phát hiện khi thiết bị đi vào vùng địa lý (geofence) đã cấu hình của địa điểm du lịch.
* Hiển thị thông báo hệ điều hành hoặc banner trong ứng dụng.
* Mở màn hình Chi tiết địa điểm tương ứng bằng locationId.
* Use case liên quan: UC-05 – Nhận thông báo theo vị trí.

**Quản lý nội dung địa điểm và tài nguyên âm thanh**

* Tạo và chỉnh sửa thông tin địa điểm.
* Quản lý kịch bản hướng dẫn đa ngôn ngữ.
* Tải lên và quản lý tài nguyên âm thanh tương ứng.
* Lưu nội dung ở trạng thái DRAFT hoặc gửi duyệt ở trạng thái PENDING_REVIEW.
* Use case liên quan: UC-06 – Quản lý nội dung địa điểm và tài nguyên âm thanh.

**Thống kê hệ thống và quản lý phản hồi du khách**

* Hiển thị các chỉ số sử dụng hệ thống.
* Xem và lọc phản hồi của du khách.
* Cập nhật trạng thái phản hồi.
* Xuất báo cáo thống kê và phản hồi.
* Use case liên quan: UC-07 – Thống kê hệ thống và quản lý phản hồi du khách.

## **2.2. Ngoài phạm vi**

* Duyệt/xuất bản theo vai trò EDITOR/REVIEWER nay thuộc UC-06, theo quyết định phạm vi ngày 25/10/2026.
* Thu thập sự kiện phân tích và gửi phản hồi từ ứng dụng du khách (UC-07 chỉ sử dụng dữ liệu này).
* UI quản lý tài khoản; xác thực DB và phân quyền EDITOR/REVIEWER nay nằm trong phạm vi.
* Tự tải nội dung hướng dẫn hoặc tự phát âm thanh như một phần của thao tác thông báo. Chọn thông báo chỉ mở màn hình Chi tiết địa điểm; sau đó UC-02 tải nội dung bằng trigger riêng.
* Chỉ đường từng chặng đến địa điểm.
* Tài khoản người dùng, thanh toán và tính năng mạng xã hội.

## **2.3. Tác nhân**

Hệ thống bổ sung REVIEWER bên cạnh các tác nhân chính hiện có. Các phụ thuộc nền tảng và dịch vụ hỗ trợ tương tác như bảng dưới; trách nhiệm được mô tả chi tiết tại mục 6.4.

| Tác nhân | Loại | Mô tả | UC liên quan |
| :---- | :---- | :---- | :---- |
| **Du khách (Tourist)** | Chính | Sử dụng ứng dụng du khách để khám phá và chọn địa điểm, truy cập nội dung đa ngôn ngữ, phát âm thanh, quản lý nội dung ngoại tuyến và nhận thông báo theo vị trí. | UC-01, UC-02, UC-03, UC-04, UC-05 |
| **Quản trị viên nội dung (Content Admin / Editor)** | Chính | Tạo và chỉnh sửa thông tin địa điểm, kịch bản đa ngôn ngữ và tài nguyên âm thanh; lưu bản nháp, gửi nội dung để duyệt. | UC-06 |
| **Quản trị viên hệ thống/Chủ sản phẩm (System Admin / Product Owner)** | Chính | Xem thống kê hệ thống, quản lý trạng thái phản hồi và xuất báo cáo qua Web Admin Dashboard. | UC-07 |
| **Dịch vụ bản đồ (Map Service)** | Dịch vụ phụ | Hiển thị bản đồ tương tác và các điểm đánh dấu; Map View ngoại tuyến dùng nền đơn giản không có ô bản đồ (chỉ trực tuyến; khi ngoại tuyến, duyệt địa điểm qua danh sách). | UC-01 |
| **Định vị và Geofencing của nền tảng (Platform Location & Geofencing)** | Phụ thuộc nền tảng | Cung cấp tọa độ và độ chính xác, hỗ trợ đăng ký geofence và sự kiện đi vào vùng. | UC-01, UC-05 |
| **Thông báo nền tảng (Platform Notification)** | Phụ thuộc nền tảng | Hiển thị thông báo hệ thống và mở ứng dụng du khách bằng locationId. | UC-05 |
| **Âm thanh và TTS của nền tảng (Platform Audio & Text-to-Speech)** | Phụ thuộc nền tảng | Cung cấp phát âm thanh, xử lý quyền ưu tiên âm thanh (audio focus) và TTS dự phòng trên thiết bị. | UC-03 |
| **Máy chủ nội dung / CDN (Content Server / CDN)** | Phụ thuộc hạ tầng | Cung cấp nội dung hướng dẫn APPROVED, tài nguyên âm thanh và các gói nội dung. | UC-02, UC-03, UC-04 |
| **Người duyệt nội dung (REVIEWER)** | Chính | So sánh bản chờ duyệt với bản công khai; từ chối kèm nhận xét hoặc chấp thuận tác vụ xuất bản gói đã xác minh. | UC-06 |
| **Xác thực quản trị viên (Admin Authentication)** | Thành phần nội bộ | Tài khoản DB, BCrypt, phiên bearer và kiểm quyền EDITOR/REVIEWER; UI quản lý tài khoản ngoài phạm vi. | UC-06, UC-07 |

**Tên gọi tương đương:** Content Admin/Editor, Content Administrator và Administrators/Content Editors đều chỉ Quản trị viên nội dung (Content Admin / Editor). System Administrators/Product Owners chỉ Quản trị viên hệ thống/Chủ sản phẩm. “Admin” trong UC-06 là Quản trị viên nội dung; “Admin” hoặc “Administrator” trong UC-07 là Quản trị viên hệ thống/Chủ sản phẩm.

## **2.4. Use case chung và điều kiện nền tảng**

**Quyết định sản phẩm xác nhận ngày 19/10/2026:** Web du khách và Android là hai client của cùng sản phẩm. UC-01–UC-05 mô tả mục tiêu chung của Du khách; UC-06–UC-07 vẫn là mục tiêu Web Admin. Không nhân đôi use case theo nền tảng. Hai client du khách dùng cùng Backend API và cơ sở dữ liệu/kho nội dung có thẩm quyền. Cùng locationId, languageCode, scriptType và tham chiếu nội dung đã xuất bản phải xác định cùng tài liệu trên hai client; không bắt buộc dùng chung mã nguồn UI hoặc adapter thiết bị.

| Use case | Yêu cầu chung | Điều kiện Android | Điều kiện Web du khách |
| --- | --- | --- | --- |
| UC-01 | Khám phá/chọn địa điểm; từ chối quyền vẫn có đường chọn thủ công. | Tích hợp định vị/bản đồ native; liên kết hợp lệ có thể mở địa điểm tương ứng. | URL HTTPS công khai mở địa điểm không cần cài đặt hoặc đăng nhập; xử lý quyền trình duyệt khi tìm lân cận. |
| UC-02 | Nội dung APPROVED, FULL/SHORT, dự phòng Đang chọn → EN → VI và giữ scriptType. | Máy chủ hoặc kho nội dung cục bộ đã xác minh. | Cùng API/định danh nội dung; dùng nội dung cục bộ trình duyệt đã xác minh khi sẵn sàng ngoại tuyến. |
| UC-03 | Phát do người dùng khởi tạo, điều khiển phát, TTS phù hợp, xử lý lỗi; không tự phát khi điều hướng. | Audio/TTS nền tảng và vòng đời audio focus. | Adapter audio/TTS trình duyệt/nền tảng; phải kiểm thử giọng và điều khiển, không mặc định giống Android. |
| UC-04 | Gói đã xác minh, đồng ý tải rõ ràng, cài nguyên tử, lưu phiên bản và bảo vệ Base Content. | Base Content đóng gói khi cài ứng dụng. | Cấp phát và xác minh Base Content trong lần đầu truy cập có mạng trước khi báo sẵn sàng ngoại tuyến. Không hứa truy cập lần đầu khi chưa có mạng. |
| UC-05 | Phát hiện đi vào geofence khi được hỗ trợ; cooldown, thông báo/banner đúng ngôn ngữ, điều hướng locationId, không tự phát. | Quyền định vị/geofence/thông báo native và vòng đời ứng dụng. | Theo dõi vị trí và banner tiền cảnh cần phiên đang hoạt động, có quyền. Không hứa theo dõi nền/khi đóng tab hoặc thông báo hệ thống nếu chưa kiểm chứng trên danh sách trình duyệt hỗ trợ. |

Khả năng nền tảng không hỗ trợ hoặc bị từ chối phải được giải thích và xử lý bằng luồng ngoại lệ/thủ công tương ứng. Không được âm thầm coi đó là chức năng hoàn thành hoặc thay đổi quy tắc nghiệp vụ. Phiên bản trình duyệt/OS, giọng đọc hỗ trợ, độ bền lưu trữ và phạm vi tiền cảnh/chạy nền cần có ma trận nghiệm thu; phiên bản và môi trường kiểm thử cụ thể còn là \[?\].

**Nghiệm thu theo giai đoạn:** Trước tiên hoàn thành một luồng thật xuyên suốt: mở/chọn địa điểm → lấy nội dung APPROVED → chọn ngôn ngữ và FULL/SHORT → phát audio tương ứng hoặc TTS → Web Admin sử dụng dữ liệu lượt phát đã ghi. Demo luồng này trên cả hai client với cùng backend/nội dung; gắn nhãn rõ nội dung minh họa hoặc hành vi còn mock. Kiểm tra sự kiện được lưu bền vững, thống kê nhất quán và gửi lại an toàn thay vì dựa vào số liệu tĩnh. Thu thập sự kiện vẫn là tích hợp bên ngoài được UC-07 sử dụng. Hoàn thiện dần các luồng khác theo cùng PRD; thứ tự này không loại bỏ ngoại tuyến, thông báo hoặc quản trị. Một mốc demo không chứng minh mọi use case đã hoàn thành.

Để du khách sử dụng thật, Web du khách phải có URL HTTPS truy cập được; URL demo chỉ trong máy/mạng nội bộ là môi trường phát triển/kiểm thử. Có thể giữ màn mock hiện tại làm tham khảo/công cụ tình huống có nhãn rõ trong lúc tích hợp; chúng không phải bằng chứng nghiệm thu lưu dữ liệu thật, audio, quyền hoặc cài gói.

# **3. EPIC VÀ YÊU CẦU SẢN PHẨM**

## **3.1. Epic 1 – Khám phá và chọn địa điểm**

### **3.1.1. Mục tiêu**

Giúp du khách khám phá và chọn điểm tham quan qua tìm kiếm lân cận, từ khóa, danh mục, bản đồ và thông báo theo vị trí, bao gồm chọn thủ công khi không truy cập được vị trí.

### **3.1.2. Chức năng**

* **FR-01:** Tìm kiếm lân cận, sắp xếp khoảng cách tăng dần, danh sách rút gọn và mở rộng bán kính (UC-01).
* **FR-02:** Tìm theo từ khóa, duyệt danh mục và chọn điểm đánh dấu trên bản đồ (UC-01).
* **FR-03:** Thông báo geofence hoặc banner trong ứng dụng, khoảng chờ giữa các thông báo (cooldown) và điều hướng bằng locationId đến Chi tiết địa điểm (UC-05).

### **3.1.3. Use case liên quan**

* **UC-01** — Khám phá và chọn địa điểm.
* **UC-05** — Nhận thông báo theo vị trí.

## **3.2. Epic 2 – Trải nghiệm thuyết minh âm thanh**

### **3.2.1. Mục tiêu**

Cung cấp văn bản và âm thanh hướng dẫn đa ngôn ngữ đã được kiểm chứng cho địa điểm đã chọn, có lựa chọn ngôn ngữ và phương án dự phòng.

### **3.2.2. Chức năng**

* **FR-04:** Truy xuất nội dung APPROVED theo locationId, languageCode và scriptType, hỗ trợ ngôn ngữ và phương án dự phòng (UC-02).
* **FR-05:** Điều khiển phát âm thanh và dùng TTS của nền tảng làm dự phòng khi không có âm thanh sử dụng được (UC-03).

### **3.2.3. Use case liên quan**

* **UC-02** — Truy cập nội dung hướng dẫn đa ngôn ngữ; **UC-03** — Phát thuyết minh âm thanh.

## **3.3. Epic 3 – Trải nghiệm ngoại tuyến**

### **3.3.1. Mục tiêu**

Giữ dữ liệu địa điểm và nội dung hướng dẫn thiết yếu luôn sẵn sàng khi ngoại tuyến; cho phép cài đặt và quản lý an toàn nội dung bổ sung hoặc cập nhật.

### **3.3.2. Chức năng**

* **FR-06:** Base Content cấp phát theo mục 2.4, kiểm tra cập nhật, chọn gói tùy chọn và tải các gói mới hơn đã được phê duyệt (UC-04).
* **FR-07:** Cài đặt nguyên tử, có xác minh, với dữ liệu địa điểm, kịch bản và âm thanh hiện có tương ứng (UC-04).
* **FR-08:** Xóa nội dung tùy chọn, hủy thao tác và bảo vệ vật lý đối với Base Content (UC-04).

### **3.3.3. Use case liên quan**

* **UC-04** — Quản lý và cập nhật nội dung ngoại tuyến.

## **3.4. Epic 4 – Quản lý nội dung**

### **3.4.1. Mục tiêu**

Cho phép Quản trị viên nội dung duy trì thông tin địa điểm, kịch bản đa ngôn ngữ và âm thanh tương ứng trước khi được duyệt và xuất bản bên ngoài.

### **3.4.2. Chức năng**

* **FR-09:** Danh sách địa điểm, thông tin bản địa hóa, kịch bản, tải lên và nghe thử âm thanh (UC-06).
* **FR-10:** Lưu nháp, gửi duyệt, chỉnh sửa và gửi lại nội dung bị từ chối, bảo vệ phiên bản đã được phê duyệt (UC-06).
* **FR-11:** Ghi nhận updatedBy và updatedAt cho nội dung đã lưu (UC-06).

### **3.4.3. Use case liên quan**

* **UC-06** — Quản lý nội dung địa điểm và tài nguyên âm thanh.

## **3.5. Epic 5 – Thống kê và phản hồi du khách**

### **3.5.1. Mục tiêu**

Hỗ trợ cải thiện dịch vụ và nội dung thông qua thống kê sử dụng cơ bản và quản lý phản hồi được cung cấp từ các phụ thuộc bên ngoài.

### **3.5.2. Chức năng**

* **FR-12:** Tổng lượt phát (Total Playbacks), địa điểm hàng đầu (Top Locations), phân bố ngôn ngữ (Language Distribution) và tỷ lệ sử dụng ngoại tuyến/trực tuyến (Offline/Online Usage Ratio) theo kỳ báo cáo (UC-07).
* **FR-13:** Danh sách, chi tiết, bộ lọc, phân trang phản hồi và cập nhật trạng thái tuần tự với RESOLVED là trạng thái cuối (UC-07).
* **FR-14:** Xuất .xlsx theo kỳ báo cáo, áp dụng bộ lọc phản hồi hiện hành và thử lại sau lỗi (UC-07).

### **3.5.3. Use case liên quan**

* **UC-07** — Thống kê hệ thống và quản lý phản hồi du khách.

# **4. ĐẶC TẢ CHI TIẾT USE CASE**

## **4.1. USE CASE 1: Khám phá và chọn địa điểm**

| Thuộc tính | Mô tả | |
| :---- | :---- | :---- |
| **Mã UC** | UC-01 | |
| **Tên UC** | **Khám phá và chọn địa điểm** | |
| **Loại** | Ứng dụng du khách – Web / Android | |
| **Mục tiêu** | Du khách chọn một điểm tham quan để xem chi tiết và truy cập nội dung hướng dẫn đa ngôn ngữ. | |
| **Tác nhân chính** | Du khách | |
| **Tác nhân phụ** | Dịch vụ bản đồ, Định vị và Geofencing của nền tảng | |
| **Tiền điều kiện** | Du khách mở Web du khách hoặc Android, gồm đường vào từ URL riêng của địa điểm. Quyền vị trí có thể được sử dụng cho tìm lân cận; tìm thủ công và liên kết địa điểm hợp lệ vẫn dùng được khi không có quyền đó. | |
| **Hậu điều kiện** | Hệ thống xác định locationId và ứng dụng điều hướng đến **màn hình Chi tiết địa điểm**. | |
| **Luồng chính** | **Hành động của tác nhân** | **Phản hồi của hệ thống** |
| | **1.** Du khách mở ứng dụng và chọn **“Tìm địa điểm lân cận”** hoặc dùng đường vào A1–A4. | **2.** Hệ thống bắt đầu tìm lân cận khi người dùng chọn cách tìm này. |
| | | **3.** Nếu chưa xác định quyền vị trí, hệ thống giải thích lý do cần truy cập và yêu cầu quyền qua nền tảng hiện tại. Nếu được cấp quyền, tiếp tục bước 4; nếu bị từ chối hoặc không hỗ trợ, thực hiện **E1**. |
| | | **4.** Hệ thống lấy vĩ độ, kinh độ và độ chính xác hiện tại. |
| | | **5.** Hệ thống tìm địa điểm trong bán kính cấu hình từ 100 đến 500 mét và sắp xếp kết quả theo khoảng cách tăng dần. **E2** |
| | | **6.** Hệ thống hiển thị các địa điểm trong bán kính theo khoảng cách tăng dần; \[3–5\] địa điểm gần nhất được hiển thị trước kèm khoảng cách. |
| | **7.** Du khách chọn địa điểm. | **8.** Hệ thống lưu locationId tương ứng và điều hướng đến **màn hình Chi tiết địa điểm**. |
| **Luồng thay thế** | **A1** | |
| | **1.** Chọn **“Tìm theo từ khóa”**. | **2.** Hệ thống tìm trong cơ sở dữ liệu địa điểm và hiển thị kết quả phù hợp. **E3** |
| | **3.** Chuyển đến bước 7 của luồng chính. | |
| | **A2** | |
| | **1.** Chọn **“Xem bản đồ”** và duyệt bản đồ. | **2.** Hệ thống duyệt các điểm đánh dấu trên bản đồ đang hiển thị, hỗ trợ phóng to, thu nhỏ và di chuyển bản đồ. |
| | **3.** Chuyển đến bước 7 của luồng chính. | |
| | **A3** | |
| | **1.** Chọn **“Duyệt theo danh mục”**. | **2.** Hệ thống hiển thị các địa điểm thuộc danh mục đã chọn. **E4** |
| | **3.** Chuyển đến bước 7 của luồng chính. | |
| | **A4: Mở liên kết địa điểm / URL từ QR.** Du khách mở URL chứa tham chiếu địa điểm, ví dụ quét QR. Hệ thống kiểm tra locationId và mở Chi tiết địa điểm để kích hoạt UC-02. Web du khách không yêu cầu cài đặt hoặc đăng nhập; điều hướng không bắt đầu phát. Nếu địa điểm không khả dụng, hệ thống giải thích lỗi và cho khám phá thủ công, không mở nhầm địa điểm khác. | |
| **Luồng ngoại lệ** | **E1:** Nếu du khách chưa cấp quyền vị trí hoặc hệ thống không lấy được tọa độ hiện tại, ứng dụng hiển thị **“Cần quyền truy cập vị trí để tự động tìm các điểm tham quan gần bạn”**, kèm cài đặt/hướng dẫn phù hợp nền tảng nếu có. Du khách vẫn tiếp tục bằng từ khóa, danh mục, chọn bản đồ hoặc liên kết địa điểm hợp lệ. | |
| | **E2:** Nếu tọa độ GPS hợp lệ nhưng không tìm thấy địa điểm trong bán kính hiện tại, hệ thống hiển thị **“Không tìm thấy điểm tham quan nào ở gần.”** Du khách có thể mở rộng bán kính hoặc dùng **A1**, **A2**, **A3**. | |
| | **E3:** Nếu từ khóa không khớp địa điểm nào trong cơ sở dữ liệu, hệ thống hiển thị **trạng thái trống (Empty State)** và gợi ý từ khóa khác hoặc địa điểm phổ biến để tiếp tục khám phá. | |
| | **E4:** Nếu danh mục đã chọn không có địa điểm, hệ thống hiển thị **Empty State** và gợi ý danh mục khác hoặc địa điểm phổ biến. | |
| **Điểm mở rộng** | Không áp dụng | |
| **Kích hoạt** | **Do người dùng:** Du khách mở ứng dụng du khách hoặc đến màn hình **Khám phá / Bản đồ**. Du khách chọn **“Tìm địa điểm lân cận”**, sử dụng thanh **Tìm kiếm**, chọn **Danh mục**, chọn một **điểm đánh dấu** trên bản đồ tương tác hoặc mở URL/liên kết QR của địa điểm (A4). | |
| **Giả định** | **Dữ liệu:** Cơ sở dữ liệu máy chủ hoặc kho nội dung cục bộ đã xác minh chứa thông tin địa điểm, gồm tọa độ, mô tả, danh mục và ảnh thu nhỏ. **Nền tảng:** Định vị có thể cần quyền hoặc không khả dụng; vẫn cho chọn thủ công. **Nội dung cơ bản ngoại tuyến:** Android đóng gói dữ liệu địa điểm/hướng dẫn khi cài đặt; Web du khách cấp phát và xác minh khi sử dụng trực tuyến lần đầu theo mục 2.4. UC-04 quản lý gói tùy chọn/mới hơn sau khi nội dung cơ bản sẵn sàng. | |
| **Tham chiếu: Quy tắc nghiệp vụ** | **BR1 – Bán kính tìm GPS:** Bán kính tìm địa điểm có thể cấu hình trong khoảng **100–500 mét**. Hệ thống dùng giá trị mặc định cố định trong cấu hình ứng dụng, không chỉnh qua Web Admin Dashboard. **BR2 – Xếp hạng kết quả:** Kết quả dựa trên GPS được sắp xếp theo **khoảng cách tăng dần**. Kết quả theo từ khóa hoặc danh mục có thể xếp theo **độ liên quan hoặc khoảng cách**. **BR3 – Quyền vị trí:** Nếu du khách từ chối quyền vị trí, hệ thống không được ngăn họ tiếp tục sử dụng ứng dụng. Các chức năng phụ thuộc GPS bị tắt; hệ thống phải cung cấp cách chọn địa điểm khác như **Tìm theo từ khóa**, **Duyệt theo danh mục** hoặc **Xem bản đồ**. | |
| **Tác giả** | | |
| **Ngày** | | |
| **Sơ đồ hoạt động** | Xem UC01_activity.drawio | |

## **4.2. USE CASE 2: Truy cập nội dung hướng dẫn đa ngôn ngữ**

| Thuộc tính | Mô tả | |
| :---- | :---- | :---- |
| **Mã UC** | UC-02 | |
| **Tên UC** | **Truy cập nội dung hướng dẫn đa ngôn ngữ** | |
| **Loại** | Ứng dụng du khách – Web / Android | |
| **Mục tiêu** | Truy xuất và hiển thị nội dung hướng dẫn đã được phê duyệt theo ngôn ngữ và chế độ thời lượng du khách chọn. | |
| **Tác nhân chính** | Du khách | |
| **Tác nhân phụ** | Máy chủ nội dung / CDN | |
| **Tiền điều kiện** | Ứng dụng đã điều hướng đến màn hình Chi tiết địa điểm qua UC-01, UC-05 hoặc một đường điều hướng hợp lệ khác và có locationId hợp lệ. | |
| **Hậu điều kiện** | Nội dung hướng dẫn (scriptText) đã được tải thành công, sẵn sàng hiển thị hoặc dùng cho UC-03. | |
| **Luồng chính** | **Hành động của tác nhân** | **Phản hồi của hệ thống** |
| | **1.** Du khách mở màn hình Chi tiết địa điểm qua một đường điều hướng hợp lệ. | Hệ thống nhận locationId, languageCode hiện tại và scriptType mặc định (FULL). |
| | | **2.** Hệ thống truy xuất nội dung khớp (locationId, languageCode, scriptType) từ nguồn dữ liệu hiện có. |
| | | **3.** Hệ thống xác minh nội dung truy xuất có status = APPROVED trước khi tải. |
| | | **4.** Hệ thống tải scriptText đã được phê duyệt tương ứng. |
| | | **5.** Ứng dụng hiển thị scriptText trên **màn hình Chi tiết địa điểm**. |
| **Luồng thay thế** | **A1:** Du khách chọn ngôn ngữ khác từ bộ chọn ngôn ngữ. Hệ thống truy xuất nội dung theo languageCode mới, giữ nguyên locationId và scriptType, sau đó cập nhật nội dung hướng dẫn trên màn hình. | |
| | **A2:** Du khách chọn chế độ **Tóm tắt ngắn**. Hệ thống truy xuất nội dung với scriptType = SHORT cho địa điểm và ngôn ngữ hiện tại, sau đó cập nhật nội dung hiển thị. | |
| **Luồng ngoại lệ** | **E1:** Nếu không có nội dung hướng dẫn đã được phê duyệt bằng ngôn ngữ đã chọn, hệ thống thông báo chưa có bản dịch và áp dụng quy tắc ngôn ngữ dự phòng. Hệ thống thử nội dung **APPROVED** bằng **tiếng Anh (EN)**; nếu không có tiếng Anh, thử nội dung **APPROVED** bằng **tiếng Việt (VI)**. | |
| | **E2:** Nếu địa điểm đã chọn không có nội dung hướng dẫn đã được phê duyệt, hệ thống hiển thị **“Nội dung cho địa điểm này đang được cập nhật.”** Chức năng **Nghe thuyết minh** không khả dụng. | |
| **Điểm mở rộng** | Không áp dụng | |
| **Kích hoạt** | **Do hệ thống:** Sau khi điều hướng hợp lệ đến màn hình Chi tiết địa điểm, kể cả chọn từ UC-01 hoặc chọn thông báo/banner UC-05, hệ thống kích hoạt UC-02 với ngôn ngữ hiện tại và chế độ FULL mặc định. **Do người dùng:** Khi du khách đổi **Ngôn ngữ** hoặc **Chế độ thời lượng** (FULL / SHORT), hệ thống kích hoạt lại UC-02 để tải nội dung tương ứng. | |
| **Giả định** | **Dữ liệu nội dung:** Mỗi địa điểm có thể có một hoặc nhiều kịch bản theo các ngôn ngữ và loại nội dung khác nhau (FULL, SHORT). **Ngôn ngữ hỗ trợ:** Hệ thống có danh sách ngôn ngữ định trước; mỗi ngôn ngữ dùng languageCode hợp lệ. **Nguồn dữ liệu:** Khi trực tuyến, hệ thống có thể truy xuất nội dung đã được phê duyệt từ API/máy chủ; khi ngoại tuyến, nội dung cần thiết có trong Base Content đã xác minh hoặc gói APPROVED cài qua UC-04, theo mục 2.4. | |
| **Tham chiếu: Quy tắc nghiệp vụ** | **BR1 – Phê duyệt nội dung:** Chỉ nội dung hướng dẫn có status = APPROVED được cung cấp cho du khách. **BR2 – Ngôn ngữ dự phòng:** Thứ tự ưu tiên là Ngôn ngữ đã chọn → Tiếng Anh (EN) → Tiếng Việt (VI). | |
| **Tác giả** | | |
| **Ngày** | | |
| **Sơ đồ hoạt động** | Xem UC02_activity.drawio | |

## **4.3. USE CASE 3: Phát thuyết minh âm thanh**

| Thuộc tính | Mô tả | |
| :---- | :---- | :---- |
| **Mã UC** | UC-03 | |
| **Tên UC** | **Phát thuyết minh âm thanh** | |
| **Loại** | Ứng dụng du khách – Web / Android | |
| **Mục tiêu** | Cho phép du khách nghe nội dung hướng dẫn đã được phê duyệt cho địa điểm, ngôn ngữ thực tế và chế độ nội dung đã chọn, bằng âm thanh tạo sẵn hoặc Text-to-Speech (TTS) của nền tảng khi âm thanh đó không khả dụng hoặc không phát được. | |
| **Tác nhân chính** | Du khách | |
| **Tác nhân phụ** | Âm thanh và TTS của nền tảng, Máy chủ nội dung / CDN | |
| **Tiền điều kiện** | UC-02 đã tải thành công nội dung hướng dẫn APPROVED; có locationId, languageCode thực tế sau quy tắc dự phòng ngôn ngữ của UC-02, scriptType hợp lệ và scriptText không rỗng. Khả năng sử dụng engine và giọng TTS phù hợp được kiểm tra khi cần phát dự phòng; đây không phải điều kiện bắt buộc để đọc văn bản hoặc phát audio đang có. | |
| **Hậu điều kiện** | **Thành công:** Nội dung hướng dẫn đã được phê duyệt được phát bằng audio tương ứng hoặc TTS; ứng dụng cho biết nguồn phát đang dùng. Trạng thái và vị trí hiện tại được duy trì trong phiên phát đang hoạt động. **Trường hợp khác:** Nếu cả audio và TTS đều không sử dụng được (E3), hệ thống không báo phát thành công; văn bản đã tải cùng địa điểm, ngôn ngữ và chế độ nội dung hiện tại vẫn được giữ để đọc hoặc thử lại. | |
| **Luồng chính** | **Hành động của tác nhân** | **Phản hồi của hệ thống** |
| | **1.** Du khách chọn **Phát âm thanh**. | Hệ thống nhận yêu cầu phát nội dung hướng dẫn cho địa điểm hiện tại. |
| | | **2.** Hệ thống xác định audio tương ứng bằng locationId, languageCode thực tế và scriptType do UC-02 cung cấp. |
| | | **3.** Hệ thống kiểm tra tệp âm thanh tương ứng có sẵn từ nguồn dữ liệu hiện tại hay không. Nếu không có, thực hiện E1. |
| | | **4.** Hệ thống tải tệp âm thanh và chuẩn bị phiên phát. Nếu tải hoặc chuẩn bị thất bại, thực hiện E1. |
| | | **5.** Hệ thống bắt đầu phát âm thanh. |
| | | **6.** Khi âm thanh bắt đầu phát, ứng dụng cập nhật trạng thái **Đang phát (Playing)**, cho biết nguồn là audio tạo sẵn và hiển thị các điều khiển phát. |
| | **7.** Du khách tiếp tục nghe thuyết minh. | Hệ thống duy trì trạng thái và vị trí phát hiện tại cho đến khi phát xong hoặc có thao tác/sự kiện khác. |
| **Luồng thay thế** | **A1: Điều khiển phát.** Khi audio tạo sẵn hoặc TTS đang phát, du khách có thể tạm dừng, tiếp tục, tua tới/lùi hoặc đổi tốc độ. Hệ thống cập nhật phiên phát đang hoạt động và tiếp tục từ vị trí tương ứng theo thao tác đã chọn. | |
| | **A2: Phát hướng dẫn bằng Text-to-Speech.** Khi xảy ra E1: **1.** Hệ thống thông báo audio tạo sẵn không khả dụng và sẽ dùng TTS. **2.** Hệ thống khởi tạo dịch vụ TTS của nền tảng và kiểm tra giọng đọc sử dụng được, phù hợp với languageCode thực tế do UC-02 trả về. Khi ngoại tuyến, giọng này phải hoạt động không cần kết nối mạng. Nếu dịch vụ hoặc giọng phù hợp không khả dụng, thực hiện E3. **3.** Hệ thống chuẩn bị giọng đọc từ nguyên văn scriptText đã được phê duyệt cho locationId và scriptType hiện tại; không dịch, tóm tắt hoặc tạo nội dung hướng dẫn mới. **4.** Hệ thống bắt đầu phát TTS. Khi tiếng đọc bắt đầu, hiển thị **Đang phát — Text-to-Speech** cùng các điều khiển tại A1. **5.** Hệ thống duy trì trạng thái và vị trí phát TTS cho đến khi kết thúc hoặc có thao tác/sự kiện khác; gián đoạn audio focus thực hiện E2. | |
| **Luồng ngoại lệ** | **E1: Audio không khả dụng hoặc phát thất bại.** Nếu audio tương ứng không tồn tại hoặc không thể tải, chuẩn bị hay phát, kể cả lỗi trong khi đang phát, hệ thống dừng/giải phóng phiên audio lỗi và thực hiện A2 bằng nội dung UC-02 đã tải. Không được phát đồng thời audio lỗi và TTS. | |
| | **E2: Gián đoạn audio focus.** Nếu sự kiện ưu tiên cao hơn, như cuộc gọi đến hoặc ứng dụng khác, chiếm quyền xuất âm thanh, hệ thống tạm dừng phiên audio hoặc TTS đang hoạt động và lưu vị trí phát hiện tại. Khi audio focus được khôi phục, có thể tiếp tục phát từ vị trí đã lưu. | |
| | **E3: TTS không khả dụng hoặc thất bại.** Nếu engine TTS không khởi tạo được, không có giọng sử dụng được phù hợp với languageCode thực tế, giọng cần mạng nhưng không có kết nối, hoặc tổng hợp/phát tiếng nói thất bại, hệ thống kết thúc lần chuẩn bị/phát và báo lỗi giải thích rằng hiện chưa thể đọc hướng dẫn thành tiếng. Hệ thống giữ scriptText cùng địa điểm, ngôn ngữ và chế độ nội dung hiện tại, cho phép thử lại sau khi nguyên nhân được xử lý. Không được giữ trạng thái Đang tải vô thời hạn, báo phát thành công, âm thầm dùng giọng của ngôn ngữ khác hoặc thay đổi nội dung đã được phê duyệt. | |
| **Điểm mở rộng** | Không áp dụng | |
| **Kích hoạt** | **Do người dùng:** Du khách chọn **Phát âm thanh** trên màn hình Chi tiết địa điểm sau khi UC-02 đã tải nội dung hướng dẫn được phê duyệt. | |
| **Giả định** | Mỗi kịch bản có thể có audio tương ứng tùy chọn theo languageCode và scriptType. Nền tảng hiện tại cung cấp audio/TTS cùng xử lý gián đoạn/vòng đời qua adapter. Engine, giọng, điều khiển và yêu cầu mạng khác nhau giữa thiết bị/trình duyệt và phải được kiểm chứng. Phát ngoại tuyến cần audio cục bộ tương ứng từ Base Content đã xác minh hoặc gói UC-04, hoặc scriptText cục bộ cùng giọng offline phù hợp languageCode thực tế. Chỉ có văn bản không bảo đảm đọc thành tiếng khi ngoại tuyến. Mục 2.4 quy định cấp phát nội dung cơ bản; phạm vi ngôn ngữ/giọng và môi trường kiểm thử cần được nhóm xác nhận. | |
| **Tham chiếu: Quy tắc nghiệp vụ** | **BR1 – Nhất quán nội dung phát:** Audio và TTS phải khớp locationId, languageCode thực tế và scriptType hiện tại do UC-02 cung cấp. **BR2 – Ưu tiên âm thanh tạo sẵn:** Ưu tiên audio tạo sẵn tương ứng và hợp lệ khi phát được; chỉ dùng TTS của nền tảng làm dự phòng khi audio đó không khả dụng hoặc bị lỗi. **BR3 – Nhất quán văn bản đã duyệt và giọng đọc:** TTS đọc nguyên văn scriptText APPROVED do UC-02 tải bằng giọng phù hợp với languageCode thực tế. UC-02 chịu trách nhiệm dự phòng ngôn ngữ; UC-03 không tự dịch, tóm tắt hoặc thay ngôn ngữ hay loại kịch bản khác. **BR4 – Khả năng TTS ngoại tuyến:** Chỉ cung cấp TTS ngoại tuyến khi có văn bản cần thiết và giọng tương thích hoạt động không cần mạng; nếu không, thực hiện E3. **BR5 – Nguồn phát và kích hoạt:** Cho biết audio hay TTS đang hoạt động, chỉ cho một nguồn phát tại một thời điểm và chỉ bắt đầu khi du khách chọn Phát hoặc theo nhánh dự phòng của yêu cầu đó. Việc mở Chi tiết địa điểm hoặc chọn thông báo không tự khởi động audio hay TTS. | |
| **Tác giả** | | |
| **Ngày** | | |
| **Sơ đồ hoạt động** | Xem UC03_activity.drawio | |

## **4.4. USE CASE 4: Quản lý và cập nhật nội dung ngoại tuyến**

| Thuộc tính | Mô tả | |
| :---- | :---- | :---- |
| **Mã UC** | UC-04 | |
| **Tên UC** | **Quản lý và cập nhật nội dung ngoại tuyến** | |
| **Loại** | Ứng dụng du khách – Web / Android | |
| **Mục tiêu** | Cho phép du khách cập nhật, bổ sung hoặc xóa nội dung ngoại tuyến, đồng thời giữ Base Content đã xác minh để văn bản/audio dùng được khi không có mạng sau khi nội dung cơ bản sẵn sàng. Android đóng gói nội dung cơ bản; Web du khách cấp phát trong lần đầu dùng có mạng (mục 2.4). | |
| **Tác nhân chính** | Du khách | |
| **Tác nhân phụ** | Không áp dụng | |
| **Tiền điều kiện** | Base Content đã xác minh sẵn sàng trong kho nội dung cục bộ theo mục 2.4. Cần mạng để cấp phát lần đầu trên Web và để kiểm tra/tải cập nhật; xóa nội dung tùy chọn không cần mạng. Nếu nội dung cơ bản chưa sẵn sàng, giải thích cần chuẩn bị trực tuyến trước thay vì báo Sẵn sàng ngoại tuyến. | |
| **Hậu điều kiện** | **Thành công:** Nội dung ngoại tuyến được cập nhật và hiển thị Đã cập nhật / Sẵn sàng ngoại tuyến (Up to Date / Offline Ready), hoặc nội dung tùy chọn đã chọn được xóa và trạng thái nội dung được cập nhật. **Trường hợp khác (không có cập nhật, hủy hoặc lỗi):** Nội dung ngoại tuyến đã cài hiện tại được giữ nguyên. | |
| **Luồng chính** | **Hành động của tác nhân** | **Phản hồi của hệ thống** |
| | **1.** Du khách mở Quản lý nội dung ngoại tuyến. | Hệ thống hiển thị nội dung đã cài và trạng thái phiên bản. |
| | **2.** Du khách chọn Kiểm tra cập nhật hoặc Cập nhật. | Hệ thống kiểm tra máy chủ để tìm phiên bản gói nội dung **APPROVED** mới hơn. |
| | | **3.** Nếu có gói mới hơn đã được phê duyệt, hệ thống hiển thị thông tin cập nhật (nội dung mới hoặc đã sửa và dung lượng cần thiết). |
| | **4.** Du khách xác nhận cập nhật. | Hệ thống đối chiếu dung lượng cần thiết với dung lượng thiết bị còn trống. Nếu đủ, tải nội dung mới và hiển thị tiến độ tải. |
| | | **5.** Sau khi tải xong, hệ thống xác minh tính toàn vẹn của nội dung đã tải. |
| | | **6.** Nếu gói đã được phê duyệt hợp lệ, hệ thống thay thế nguyên tử các kịch bản và tài nguyên âm thanh cục bộ bị ảnh hưởng bằng gói đã xác minh, đồng thời lưu phiên bản mới. |
| | | **7.** Hệ thống hiển thị Đã cập nhật / Sẵn sàng ngoại tuyến. |
| **Luồng thay thế** | **A1: Không có bản cập nhật.** Tại bước 3, nếu không có phiên bản mới hơn, hệ thống thông báo nội dung hiện tại đã là mới nhất. Use case kết thúc và không thay đổi gì. | |
| | **A2: Xóa nội dung ngoại tuyến tùy chọn.** Du khách chọn Xóa nội dung. Hệ thống hiển thị nội dung đã tải thêm, yêu cầu chọn mục cần xóa và xác nhận. Sau khi xác nhận, hệ thống xóa nội dung đã chọn khỏi bộ nhớ cục bộ và cập nhật trạng thái. Base Content được bảo vệ không bị thao tác này xóa (**BR-01**). Nếu du khách hủy, không thay đổi gì. | |
| | **A3: Du khách hủy cập nhật.** Tại bước 4, nếu không xác nhận cập nhật, hệ thống giữ nguyên nội dung hiện tại và kết thúc use case. | |
| **Luồng ngoại lệ** | **E1: Không đủ dung lượng.** Nếu thiết bị không đủ dung lượng trống để cập nhật, hệ thống dừng thao tác, không thay đổi dữ liệu và thông báo dung lượng cần thiết. Hệ thống có thể đề xuất mở Quản lý nội dung ngoại tuyến để du khách xóa nội dung tùy chọn. | |
| | **E2: Mất kết nối mạng.** Nếu mất Internet khi đang tải, hệ thống dừng tải và loại bỏ dữ liệu chưa hoàn chỉnh. Khi có kết nối, du khách có thể thử lại sau bằng cách chọn Cập nhật lần nữa. Nội dung đã cài hiện tại được giữ nguyên. | |
| | **E3: Gói cập nhật không hợp lệ.** Nếu nội dung tải về không vượt qua xác minh tính toàn vẹn hoặc không cài đặt được, hệ thống loại bỏ dữ liệu không hợp lệ, giữ phiên bản ngoại tuyến hiện tại và thông báo thử lại. | |
| **Điểm mở rộng** | Không áp dụng | |
| **Kích hoạt** | **Do người dùng:** Du khách mở mục Nội dung ngoại tuyến / Cập nhật và chọn Kiểm tra cập nhật, Cập nhật hoặc Xóa nội dung. **Do hệ thống:** Hệ thống phát hiện phiên bản mới hơn và hiển thị Có bản cập nhật (Update Available). Chỉ bắt đầu tải sau khi du khách xác nhận. | |
| **Giả định** | Nội dung cơ bản đóng gói trên Android cho phép dùng offline mà chưa cần tải nội dung trước. Web du khách cần lần truy cập đầu có mạng để cấp phát/xác minh cùng nội dung cơ bản trước khi dùng offline. Máy chủ cung cấp gói có phiên bản, đã xuất bản và được phê duyệt. Kho cục bộ nền tảng giữ nội dung và phiên bản đã cài. Phải kiểm thử khả năng/độ bền lưu trữ trình duyệt; khi kho không có hoặc bị xóa từ bên ngoài, báo đúng trạng thái và yêu cầu chuẩn bị online lại thay vì nhận là nội dung vẫn còn. Chức năng xóa của ứng dụng phải giữ Base Content. | |
| **Tham chiếu: Quy tắc nghiệp vụ** | **BR-01 – Bảo vệ nội dung cơ bản:** Chức năng quản lý nội dung ngoại tuyến của ứng dụng không được xóa Base Content đã xác minh, theo mục 2.4. **BR-02 – Cập nhật an toàn:** Chỉ gói **APPROVED** đã xác minh thành công mới được thay thế nội dung hiện có. Nếu cập nhật lỗi, không được ghi đè hoặc xóa nội dung đã cài. **BR-03 – Quản lý phiên bản:** Hệ thống phải lưu phiên bản nội dung cục bộ để so sánh với máy chủ. **BR-04 – Cài đặt gói nguyên tử:** Một phiên bản gói phải cài đồng thời các kịch bản và tài nguyên âm thanh tương ứng; ứng dụng du khách không được dùng gói thiếu thành phần hoặc có trạng thái DRAFT/PENDING_REVIEW. | |
| **Tác giả** | | |
| **Ngày** | | |
| **Sơ đồ hoạt động** | Xem UC04_activity.drawio | |

## **4.5. USE CASE 5: Nhận thông báo theo vị trí**

| Thuộc tính | Mô tả | |
| :---- | :---- | :---- |
| **Mã UC** | UC-05 | |
| **Tên UC** | **Nhận thông báo theo vị trí** | |
| **Loại** | Ứng dụng du khách – Web / Android | |
| **Mục tiêu** | Thông báo khi du khách đi vào khu vực của một địa điểm du lịch và cho phép truy cập nhanh thông tin địa điểm tương ứng. | |
| **Tác nhân chính** | Du khách | |
| **Tác nhân phụ** | Định vị và Geofencing của nền tảng, Thông báo hệ điều hành | |
| **Tiền điều kiện** | **1.** Web du khách hoặc Android đang chạy trong nền tảng/phiên hỗ trợ theo dõi vị trí theo mục 2.4. **2.** Kho nội dung cục bộ đã xác minh có tọa độ, bán kính geofence, locationId và tên bản địa hóa. Quyền vị trí/thông báo có thể chưa khả dụng; xử lý qua E1/E2. Không được biểu diễn theo dõi nền không hỗ trợ hoặc đã tạm ngưng là dịch vụ đang hoạt động. | |
| **Hậu điều kiện** | **Thành công:** Thông báo theo vị trí hoặc banner trong ứng dụng được hiển thị. Nếu du khách chọn, ứng dụng mở màn hình Chi tiết địa điểm tương ứng bằng locationId; sau đó UC-02 được kích hoạt bằng trigger riêng để tải nội dung hướng dẫn. **Trường hợp khác:** Không hiển thị thông báo theo vị trí (E1, E2, A3) hoặc không điều hướng (A2). | |
| **Luồng chính** | **Hành động của tác nhân** | **Phản hồi của hệ thống** |
| | **1.** Du khách đi vào geofence đã cấu hình của một địa điểm du lịch. | Hệ thống phát hiện sự kiện đi vào geofence. |
| | | **2.** Hệ thống xác định địa điểm tương ứng bằng dữ liệu geofence và locationId. |
| | | **3.** Hệ thống kiểm tra khoảng chờ thông báo của địa điểm đã hết hay chưa. |
| | | **4.** Nếu được phép gửi, hệ thống tạo và hiển thị thông báo theo vị trí, chứa locationId, bằng ngôn ngữ ứng dụng hiện tại. |
| | **5.** Du khách chọn thông báo. | Hệ thống đọc locationId đi kèm. |
| | | **6.** Ứng dụng mở màn hình Chi tiết địa điểm tương ứng và kích hoạt UC-02 bằng locationId đi kèm. |
| **Luồng thay thế** | **A1: Ứng dụng ở tiền cảnh.** Nếu ứng dụng ở tiền cảnh khi xảy ra sự kiện geofence, hệ thống hiển thị banner trong ứng dụng chứa locationId thay cho thông báo hệ điều hành. Khi du khách chọn banner, ứng dụng mở màn hình Chi tiết địa điểm tương ứng bằng locationId đi kèm. | |
| | **A2: Không chọn thông báo.** Nếu du khách không chọn thông báo hoặc banner đã hiển thị, hệ thống không điều hướng. Du khách có thể tiếp tục dùng ứng dụng bình thường. | |
| | **A3: Khoảng chờ thông báo còn hiệu lực.** Tại bước 3, nếu khoảng chờ của địa điểm chưa hết, hệ thống không hiển thị thông báo (BR1). Du khách có thể tiếp tục dùng ứng dụng bình thường. | |
| **Luồng ngoại lệ** | **E1: Quyền vị trí không khả dụng.** Nếu quyền vị trí bị tắt, thu hồi hoặc không khả dụng, hệ thống không thể theo dõi geofence và không tạo thông báo theo vị trí. Du khách vẫn có thể khám phá và chọn địa điểm thủ công bằng UC-01. | |
| | **E2: Quyền thông báo không khả dụng.** Nếu không có quyền hoặc hỗ trợ nền tảng, hệ thống không thể hiển thị thông báo hệ thống. Nếu ứng dụng đang hoạt động và theo dõi vị trí có quyền vẫn khả dụng, có thể hiển thị banner trong ứng dụng (A1). | |
| **Điểm mở rộng** | Không áp dụng | |
| **Kích hoạt** | **Do hệ thống:** Thiết bị đi vào geofence đã cấu hình của một địa điểm du lịch. **Do người dùng:** Sau khi nhận thông báo, du khách chọn để mở địa điểm tương ứng. | |
| **Giả định** | Cơ sở dữ liệu cục bộ chứa thông tin địa điểm đã được kiểm chứng: locationId, latitude, longitude, radius và tên bản địa hóa. Theo dõi geofence có thể hoạt động bằng dữ liệu đã có trên thiết bị. Mỗi địa điểm được theo dõi có bán kính geofence hợp lệ. Ứng dụng có thể mở màn hình Chi tiết địa điểm bằng locationId hợp lệ. **Phạm vi MVP:** Chọn thông báo chỉ mở màn hình Chi tiết địa điểm; bản thân thông báo không tải nội dung hoặc phát âm thanh. Sau khi màn hình mở, UC-02 được kích hoạt bằng trigger do hệ thống của riêng UC-02; phát âm thanh vẫn là thao tác do du khách khởi tạo trong UC-03. | |
| **Tham chiếu: Quy tắc nghiệp vụ** | **BR1 – Khoảng chờ thông báo:** Mỗi địa điểm được tạo tối đa một thông báo trong khoảng chờ đã cấu hình để tránh lặp. **BR2 – Điều hướng bằng mã địa điểm:** Mỗi thông báo hoặc banner trong ứng dụng phải chứa locationId tương ứng. Khi du khách chọn, ứng dụng phải dùng giá trị này để mở đúng địa điểm. **BR3 – Ngôn ngữ thông báo:** Tiêu đề và nội dung của mọi thông báo, banner trong ứng dụng phải dùng ngôn ngữ hiện được cấu hình trong ứng dụng. **BR4 – Dữ liệu địa điểm đã kiểm chứng:** Thông báo theo vị trí phải dùng tên và thông tin lưu trong cơ sở dữ liệu cục bộ đã được kiểm chứng. | |
| **Tác giả** | | |
| **Ngày** | | |
| **Sơ đồ hoạt động** | Xem UC05_activity.drawio | |

## **4.6. USE CASE 6: Quản lý nội dung địa điểm và tài nguyên âm thanh**

**Bổ sung theo yêu cầu người dùng ngày 25/10/2026:** EDITOR tải ảnh đại diện POI JPEG/PNG, xem trước và lưu cùng bản nháp; ảnh chỉ công khai cùng POI sau khi duyệt. Form có hai lựa chọn nhập thủ công / dịch tự động rõ ràng, giữ dữ liệu khi chuyển cách nhập. Web/Android dùng TTS đọc nội dung khi không có audio thu sẵn; tải audio lên là tùy chọn.

**Quyết định phạm vi ngày 25/10/2026 — thay thế ranh giới duyệt/xuất bản và xác thực bên ngoài trước đây:** Web Admin có EDITOR và REVIEWER, xác thực tài khoản trong DB bằng mật khẩu băm BCrypt và phiên bearer có thể thu hồi. EDITOR tạo/sửa POI, nhập tay hoặc dịch tên và kịch bản FULL/SHORT từ một ngôn ngữ nguồn sang các ngôn ngữ được cấu hình; mọi ô đều sửa tay được. Mỗi trường có nguồn MANUAL, AUTO_TRANSLATED hoặc EDITED_AFTER_TRANSLATE. Tọa độ/bán kính do người biên tập xác nhận; dịch không sinh audio, audio upload thủ công. Lỗi dịch giữ dữ liệu và không chặn nhập tay.

Luồng: DRAFT → PENDING_REVIEW → APPROVED hoặc REJECTED; REJECTED sửa rồi gửi lại. REVIEWER so sánh với bản công khai, chấp thuận hoặc từ chối kèm nhận xét bắt buộc. Chấp thuận lưu tác vụ bền trong PostgreSQL; chuyển dữ liệu công khai và công bố gói đã xác minh trong cùng transaction. Bản biên tập chỉ thành PUBLISHED sau thành công. Gói BUILDING/READY/FAILED lưu checksum, chữ ký, kích thước, thời gian và lỗi; lỗi giữ dữ liệu/gói READY cũ và cho thử lại. Giữ contentId của phiên bản cũ để nhận lượt nghe offline. Web/Android polling khi mở/foreground, có mạng lại và định kỳ khi hoạt động, hỗ trợ ETag/304; người dùng chọn tải, chỉ thay gói sau lượt nghe. Push khi đóng app và UI quản lý tài khoản ngoài phạm vi. Sơ đồ cũ mô tả phụ thuộc duyệt/xác thực bên ngoài cần đồng bộ theo quyết định này, không được dùng để ghi đè quyết định mới.

| Thuộc tính | Mô tả | |
| :---- | :---- | :---- |
| **Mã UC** | UC-06 | |
| **Tên UC** | **Quản lý nội dung địa điểm và tài nguyên âm thanh** | |
| **Loại** | Web Admin Dashboard | |
| **Mục tiêu** | Cho phép Quản trị viên nội dung tạo hoặc chỉnh sửa thông tin địa điểm, nội dung hướng dẫn đa ngôn ngữ và tài nguyên âm thanh tương ứng, rồi lưu nháp hoặc gửi duyệt. | |
| **Tác nhân chính** | EDITOR (biên tập) và REVIEWER (duyệt/xuất bản) | |
| **Tác nhân phụ** | Không có | |
| **Tiền điều kiện** | Quản trị viên đã được xác thực và được phân quyền quản lý nội dung. | |
| **Hậu điều kiện** | **Thành công:** Dữ liệu địa điểm, nội dung hướng dẫn và tài nguyên âm thanh được lưu với trạng thái DRAFT hoặc PENDING_REVIEW. Khi chỉnh sửa phiên bản đã được phê duyệt, bản đang xuất bản vẫn sẵn có cho ứng dụng du khách trong khi bản thay thế chờ duyệt. **Trường hợp khác:** Không gửi dữ liệu. Quản trị viên vẫn ở biểu mẫu và dữ liệu đã nhập được giữ nguyên (E1, E2). | |
| **Luồng chính** | **Hành động của tác nhân** | **Phản hồi của hệ thống** |
| | **1.** Quản trị viên chọn địa điểm hiện có hoặc Thêm địa điểm mới. | **2.** Hệ thống mở biểu mẫu tương ứng. |
| | **3.** Quản trị viên nhập hoặc sửa tên địa điểm bản địa hóa cho từng ngôn ngữ hỗ trợ, danh mục, tọa độ GPS và bán kính geofence. | **4.** Hệ thống ghi nhận thông tin đã nhập. |
| | **5.** Quản trị viên nhập hoặc sửa nội dung hướng dẫn cho các ngôn ngữ hỗ trợ và từng loại kịch bản (FULL hoặc SHORT). | **6.** Hệ thống gắn mỗi kịch bản với locationId, languageCode và scriptType tương ứng. |
| | **7.** Quản trị viên tải lên tài nguyên âm thanh tương ứng cho từng ngôn ngữ và loại kịch bản. | **8.** Hệ thống kiểm tra định dạng và lưu tài nguyên âm thanh. |
| | **9.** Quản trị viên nghe thử âm thanh và rà soát nội dung đã nhập. | **10.** Hệ thống cung cấp phần nghe thử và hiển thị dữ liệu đã nhập. |
| | **11.** Quản trị viên chọn Gửi duyệt (Submit for Review). | **12.** Hệ thống kiểm tra tất cả trường bắt buộc. |
| | | **13.** Nếu dữ liệu hợp lệ, hệ thống lưu nội dung và chuyển sang PENDING_REVIEW. Nếu có bản đã được phê duyệt, hệ thống giữ bản đang xuất bản cho đến khi tác vụ xuất bản do REVIEWER chấp thuận hoàn tất thành công. |
| **Luồng thay thế** | **A1: Lưu nháp.** Nếu nội dung chưa hoàn chỉnh, quản trị viên chọn Lưu nháp (Save Draft) thay cho Gửi duyệt. Hệ thống lưu DRAFT mà không yêu cầu điền đủ các trường bắt buộc. Nội dung không được cung cấp cho ứng dụng du khách. | |
| | **A2: Sửa địa điểm hiện có.** Tại bước 1, quản trị viên chọn địa điểm hiện có. Hệ thống tải dữ liệu vào biểu mẫu; quản trị viên sửa một hoặc nhiều mục như tên, tọa độ, nội dung hướng dẫn hoặc âm thanh. Có thể chỉ thay âm thanh của một ngôn ngữ; khi đó hệ thống cập nhật tài nguyên đó và giữ nguyên dữ liệu khác. Thay đổi được lưu DRAFT hoặc gửi PENDING_REVIEW (bước 11–13, A1), đồng thời bảo vệ bản đang xuất bản (BR4). Nội dung được trả về với trạng thái REJECTED được chỉnh sửa tương tự, rồi lưu nháp hoặc gửi duyệt lại. | |
| **Luồng ngoại lệ** | **E1: Tệp âm thanh không hợp lệ hoặc tải lên thất bại.** Tại bước 8, nếu định dạng không được hỗ trợ, tệp không xử lý được hoặc tải lên lỗi, hệ thống từ chối tệp, hiển thị lỗi và giữ nguyên dữ liệu biểu mẫu. Quản trị viên chọn tệp khác hoặc thử tải lại. | |
| | **E2: Thiếu dữ liệu địa điểm bắt buộc.** Tại bước 12, nếu thiếu thông tin như tên, danh mục, vĩ độ, kinh độ hoặc bán kính geofence, hệ thống chặn gửi và đánh dấu các trường cần hoàn thiện. Quản trị viên quay lại biểu mẫu. | |
| **Điểm mở rộng** | Không áp dụng | |
| **Kích hoạt** | **Do người dùng:** Quản trị viên mở Quản lý địa điểm và chọn tạo hoặc sửa địa điểm. | |
| **Giả định** | Web Admin có Internet. Hệ thống hỗ trợ lưu văn bản hướng dẫn và tài nguyên âm thanh. Quản trị viên chuẩn bị nội dung và âm thanh trước hoặc trong khi chỉnh sửa. Ngôn ngữ hỗ trợ và danh mục địa điểm đã được cấu hình. REVIEWER phê duyệt và hệ thống xuất bản gói đã xác minh theo quyết định phạm vi mới ở trên. | |
| **Tham chiếu: Quy tắc nghiệp vụ** | **BR1 – Thông tin địa điểm bắt buộc:** Để gửi duyệt, mỗi địa điểm phải có ít nhất tên hợp lệ, danh mục, tọa độ GPS và bán kính geofence. Có thể lưu DRAFT khi dữ liệu chưa đầy đủ. **BR2 – Trạng thái nội dung:** EDITOR lưu DRAFT/PENDING_REVIEW; REVIEWER quyết định APPROVED/REJECTED, xuất bản gói thành công mới chuyển bản biên tập sang PUBLISHED. Dữ liệu công khai giữ APPROVED để tương thích API. **BR3 – Ánh xạ nội dung:** Mỗi kịch bản và tài nguyên âm thanh phải gắn với ít nhất locationId, languageCode, scriptType để UC-02 và UC-03 xác định nội dung và âm thanh khớp nhau; phiên bản gói được phụ thuộc xuất bản bên ngoài cấp sau. **BR4 – Bảo vệ nội dung đã duyệt:** Chỉnh sửa nội dung đã được phê duyệt không được xóa phiên bản hiện đang cung cấp cho ứng dụng du khách cho đến khi bản thay thế được phê duyệt và xuất bản. | |
| **Tác giả** | | |
| **Ngày** | | |
| **Sơ đồ hoạt động** | Xem UC06_activity.drawio | |

## **4.7. USE CASE 7: Thống kê hệ thống và quản lý phản hồi du khách**

| Thuộc tính | Mô tả | |
| :---- | :---- | :---- |
| **Mã UC** | UC-07 | |
| **Tên UC** | **Thống kê hệ thống và quản lý phản hồi du khách** | |
| **Loại** | Web Admin Dashboard | |
| **Mục tiêu** | Cho phép quản trị viên theo dõi các chỉ số sử dụng hệ thống chính và quản lý phản hồi, đánh giá, đề xuất của du khách để hỗ trợ cải thiện nội dung và dịch vụ. | |
| **Tác nhân chính** | Quản trị viên hệ thống / Chủ sản phẩm (System Admin / Product Owner) | |
| **Tác nhân phụ** | Không có | |
| **Tiền điều kiện** | Quản trị viên đã được xác thực và được cấp quyền truy cập Web Admin Dashboard. | |
| **Hậu điều kiện** | **Thành công:** Dữ liệu thống kê được hiển thị; mọi thay đổi trạng thái phản hồi được lưu kèm thời điểm cập nhật. **Trường hợp khác:** Nếu không có dữ liệu (E1) hoặc xuất báo cáo lỗi (E2), hệ thống hiển thị Empty State hoặc thông báo lỗi và không thay đổi dữ liệu đã lưu. | |
| **Luồng chính** | **Hành động của tác nhân** | **Phản hồi của hệ thống** |
| | **1.** Quản trị viên mở Thống kê và phản hồi (Analytics & Feedback). | Hệ thống tải dữ liệu 30 ngày gần nhất. |
| | | **2.** Hệ thống hiển thị Tổng lượt phát, Địa điểm hàng đầu, Phân bố ngôn ngữ và Tỷ lệ sử dụng ngoại tuyến/trực tuyến. |
| | **3.** Quản trị viên mở danh sách phản hồi. | Hệ thống hiển thị các phản hồi gần đây với danh mục, nội dung, đánh giá và trạng thái. |
| | **4.** Quản trị viên chọn một phản hồi. | Hệ thống hiển thị chi tiết phản hồi. |
| | **5.** Quản trị viên cập nhật trạng thái phản hồi. | Hệ thống lưu trạng thái mới và thời điểm cập nhật. |
| **Luồng thay thế** | **A1: Đổi kỳ báo cáo.** Sau bước 2, quản trị viên chọn kỳ khác. Hệ thống tải lại dữ liệu thống kê cho kỳ đã chọn. | |
| | **A2: Lọc phản hồi.** Sau bước 3, quản trị viên lọc theo danh mục hoặc trạng thái. Hệ thống chỉ hiển thị phản hồi khớp tiêu chí đã chọn. | |
| | **A3: Xuất báo cáo.** Quản trị viên chọn Xuất báo cáo (Export Report). Hệ thống tạo tệp .xlsx chứa dữ liệu thống kê và phản hồi trong kỳ báo cáo hiện tại, áp dụng bộ lọc phản hồi đang bật, rồi cung cấp để tải xuống. | |
| **Luồng ngoại lệ** | **E1: Không có dữ liệu.** Nếu không có dữ liệu khớp kỳ báo cáo hoặc bộ lọc đã chọn, hệ thống hiển thị Empty State thay cho biểu đồ hoặc bản ghi. | |
| | **E2: Xuất báo cáo thất bại.** Tại A3, nếu không tạo được báo cáo, hệ thống hiển thị lỗi và cho phép quản trị viên thử lại. | |
| **Điểm mở rộng** | Không áp dụng | |
| **Kích hoạt** | **Do người dùng:** Quản trị viên chọn Thống kê và phản hồi trên Web Admin Dashboard. | |
| **Giả định** | Ứng dụng du khách ghi các sự kiện sử dụng cần cho thống kê thông qua phụ thuộc thu thập sự kiện phân tích bên ngoài. Phản hồi du khách được gửi và lưu qua phụ thuộc gửi phản hồi bên ngoài. Hệ thống có đủ dữ liệu để tính các chỉ số cơ bản cần thiết. Quản trị viên được phép tải báo cáo khi dùng chức năng Xuất. Lọc, xuất và cập nhật trạng thái là hành vi trong use case này, không phải use case riêng. | |
| **Tham chiếu: Quy tắc nghiệp vụ** | **BR1 – Kiểm soát truy cập:** Chỉ quản trị viên được phân quyền mới được truy cập dữ liệu thống kê và quản lý phản hồi. **BR2 – Trạng thái phản hồi:** Mỗi phản hồi phải có một trạng thái hợp lệ: NEW, IN_REVIEW hoặc RESOLVED. Bản ghi tiến triển theo thứ tự NEW → IN_REVIEW → RESOLVED. **BR3 – Kỳ báo cáo mặc định:** Dashboard mặc định hiển thị dữ liệu 30 ngày gần nhất. | |
| **Tác giả** | | |
| **Ngày** | | |
| **Sơ đồ hoạt động** | Xem UC07_activity.drawio | |

# **5. MÔ HÌNH DỮ LIỆU**

***Câu hỏi:** Hệ thống cần lưu dữ liệu gì để thực hiện đúng 7 use case?*

## **5.1. Mô hình thực thể – liên kết**

**Mô hình thực thể – liên kết đề xuất:** Location tham chiếu Category; LocationTranslation tham chiếu Location và Language; GuideScript tham chiếu Location và Language; AudioAsset tham chiếu GuideScript; GuideScript đã xuất bản tham chiếu ContentPackage qua packageVersion; Feedback có thể tham chiếu Location. AnalyticsEvent lưu mã địa điểm và ngôn ngữ cho dữ liệu phân tích bên ngoài mà UC-07 sử dụng.

**Liên kết ERD:** \[\].

Phiên bản gói được cấp khi xuất bản. Kịch bản DRAFT hoặc PENDING_REVIEW chưa xuất bản không bắt buộc có packageVersion. Manifest liên kết dữ liệu địa điểm bị ảnh hưởng, kịch bản và âm thanh hiện có với phiên bản gói để cài đặt nguyên tử. Tính duy nhất của phiên bản gói cần được nhóm xác nhận trước khi chốt khóa cơ sở dữ liệu và lực lượng liên kết.

## **5.2. Định nghĩa thực thể**

**Định nghĩa thực thể đề xuất:** Các bản ghi sau được rút ra từ use case hiện có và bảng mô hình dữ liệu. Chúng mô tả thông tin cần thiết và tham chiếu bên ngoài; không bổ sung use case quản lý tài khoản, quản lý cấu hình, gửi phản hồi hay thu thập sự kiện.

| Thực thể | Trường dữ liệu | UC sử dụng | Mục đích |
| :---- | :---- | :---- | :---- |
| **Location** | locationId (PK), categoryId (FK), latitude, longitude, geofenceRadius, thumbnailUrl, status | UC-01, UC-04, UC-05, UC-06 | Thông tin địa điểm, bao gồm các bản dịch, và trạng thái nội dung của địa điểm; GuideScript.status theo dõi riêng trạng thái nội dung hướng dẫn. |
| **Location Translation** | locationId (FK), languageCode (FK), name, description | UC-01, UC-04, UC-05, UC-06 | Tên bản địa hóa và mô tả tùy chọn. Quy tắc gửi duyệt đề xuất: có ít nhất một tên bằng VI hoặc EN; Lưu nháp có thể chưa đầy đủ. |
| **Category** | categoryId (PK), tên bản địa hóa, isActive | UC-01, UC-06 | Danh mục địa điểm cố định cho MVP; các mục và giá trị bản địa hóa là \[\]. Không bổ sung use case quản lý danh mục. |
| **Language** | languageCode (PK), name | UC-02, UC-03, UC-06 | Danh mục ngôn ngữ cố định của MVP. Không mặc nhiên coi một ngôn ngữ là được hỗ trợ chỉ vì được nhắc làm ngôn ngữ dự phòng. |
| **GuideScript** | scriptId (PK), locationId (FK), languageCode (FK), scriptType (FULL hoặc SHORT), scriptText, status, packageVersion (không bắt buộc trước khi xuất bản), reviewerComment, updatedBy, updatedAt | UC-02, UC-03, UC-04, UC-06 | Văn bản hướng dẫn cho một địa điểm, ngôn ngữ và loại kịch bản, kèm thông tin duyệt bên ngoài và liên kết với gói đã xuất bản. |
| **AudioAsset** | audioId (PK), scriptId (FK), fileUrl, format, durationSec, sizeBytes, checksum | UC-03, UC-04, UC-06 | Âm thanh tùy chọn gắn với locationId, languageCode và scriptType của kịch bản. Giới hạn đề xuất được nêu tại NFR-14. |
| **Content Package** | packageVersion (PK, cần xác nhận tính duy nhất), sizeBytes, checksum, publishedAt, manifest | UC-04 | Cài đặt nguyên tử có xác minh cho dữ liệu địa điểm bị ảnh hưởng, kịch bản, âm thanh hiện có và các thay đổi về hiển thị mục bị xóa hoặc lưu trữ. |
| **Local Content Version** | installedPackageVersion, installedAt, isBase | UC-04 | Theo dõi nội dung đã cài, phân biệt Base Content được bảo vệ với nội dung tùy chọn (Optional Content). |
| **Feedback** | feedbackId (PK đề xuất), locationId (FK có thể null), category, rating (đề xuất \[1–5\]), content, languageCode, status (NEW, IN_REVIEW, RESOLVED), createdAt, updatedAt | UC-07 | Được cung cấp qua phụ thuộc gửi phản hồi bên ngoài; RESOLVED là trạng thái cuối. |
| **Analytics Event** | eventId (PK đề xuất), eventType, locationId, languageCode, connectivity, occurredAt | UC-07 | Được cung cấp qua phụ thuộc thu thập sự kiện phân tích bên ngoài. Trường timestamp trong hợp đồng sự kiện chỉ thời điểm xảy ra được lưu ở occurredAt, không phải một thời điểm sự kiện bổ sung. Phạm vi áp dụng từng loại sự kiện được nêu trong Phụ lục. |

Thông tin truy vết chỉnh sửa chỉ giới hạn ở updatedBy và updatedAt trên nội dung đã lưu; không giả định lịch sử trạng thái nội dung, thực thể audit bổ sung hay cấu trúc cơ sở dữ liệu khác tại đây.

# **6. KIẾN TRÚC HỆ THỐNG**

## **6.1. Sơ đồ lớp**

**Sơ đồ lớp đề xuất:** Các lớp miền tuân theo định nghĩa thực thể tại mục 5.2: Location, LocationTranslation, Category, Language, GuideScript, AudioAsset, ContentPackage, Feedback và AnalyticsEvent. LocalContentVersion biểu diễn thông tin phiên bản nội dung trên thiết bị. Thuộc tính theo mục 5.2; liên kết theo các tham chiếu ở mục 5.1. Các thao tác được rút ra từ UC-01 đến UC-07; chưa quy định chữ ký phương thức phụ thuộc triển khai hoặc bội số chưa được xác nhận.

## **6.2. Sơ đồ thành phần**

**Sơ đồ thành phần đề xuất:** Web du khách và Android đều có lớp trình bày và adapter nền tảng cho Content Service, Audio Player/TTS, vị trí/thông báo, Offline Manager và Local Content Store. Hai client tuân theo quy tắc chung UC-01–UC-05, dùng cùng Backend API và định danh nội dung đã xuất bản. Trực tuyến lấy nội dung từ backend/kho nội dung; ngoại tuyến dùng kho cục bộ đã xác minh theo mục 2.4. Offline Manager xác minh và cài nguyên tử gói APPROVED. Web Admin là client riêng có phân quyền cho UC-06/UC-07. Backend API dùng Server Database và Object Storage / CDN có thẩm quyền. Content Publication Service bên ngoài thực hiện duyệt, cấp phiên bản, xuất bản và ARCHIVED. Admin Authentication, Analytics Event Capture và Feedback Submission giữ ranh giới bên ngoài; dùng chung backend không cấp quyền Admin cho client du khách.

## **6.3. Sơ đồ triển khai**

**Sơ đồ triển khai đề xuất:** Android chạy trên thiết bị hỗ trợ với dịch vụ native và kho cục bộ; Web du khách chạy trong trình duyệt hỗ trợ với adapter trình duyệt/nền tảng và kho cục bộ. Hai client cùng Web Admin riêng truy cập một Backend API qua HTTPS. API kết nối cơ sở dữ liệu có thẩm quyền và kho nội dung/audio/package. Web du khách được phục vụ từ URL HTTPS du khách truy cập được; địa chỉ LAN chỉ dành cho phát triển không chứng minh khả năng truy cập công khai. Ranh giới xuất bản/xác thực bên ngoài giữ theo mục 6.4. Thu thập sự kiện trên hai client du khách gửi qua cùng hợp đồng sự kiện đã thống nhất; gửi phản hồi vẫn là tích hợp bên ngoài. Nhà cung cấp, phiên bản trình duyệt hỗ trợ, năng lực và phân bổ triển khai còn là \[?\].

## **6.4. Dịch vụ và phụ thuộc bên ngoài**

Dịch vụ nền tảng dưới đây là khả năng OS native trên Android hoặc khả năng trình duyệt/nền tảng trên Web du khách, theo mục 2.4. Hệ thống phụ thuộc vào các dịch vụ sau, nằm ngoài phạm vi các use case trong tài liệu. Cấu hình MVP cố định, lưu trữ và phân phối nội dung chỉ hỗ trợ UC-01 đến UC-07 dưới dạng hạ tầng; không định nghĩa thêm epic, chức năng hoặc use case cho chúng.

| Dịch vụ | Loại | Vai trò | UC liên quan |
| :---- | :---- | :---- | :---- |
| **Map Service** | Bên ngoài | Hiển thị bản đồ tương tác và điểm đánh dấu địa điểm. | UC-01 |
| **Platform Location & Geofencing** | Nền tảng | Cung cấp vị trí, độ chính xác của thiết bị và sự kiện đi vào geofence. | UC-01, UC-05 |
| **Platform Notification** | Nền tảng | Hiển thị thông báo và mở ứng dụng bằng locationId. | UC-05 |
| **Platform Audio & Text-to-Speech** | Nền tảng | Phát âm thanh, quản lý audio focus và đọc scriptText khi âm thanh không khả dụng. | UC-03 |
| **Content Server / CDN** | Phụ thuộc hạ tầng | Lưu và phân phối nội dung hướng dẫn đã được phê duyệt, tệp âm thanh và gói ngoại tuyến. | UC-02, UC-03, UC-04 |
| **Content Publication Service** | Bên ngoài | Nhận nội dung gửi duyệt, hỗ trợ duyệt (APPROVED / REJECTED), cấp phiên bản gói và xuất bản lên Content Server/CDN. | UC-04, UC-06 |
| **Admin Authentication** | Bên ngoài | Xác thực và phân quyền người dùng Web Admin; quản lý tài khoản nằm ngoài phạm vi. | UC-06, UC-07 |

### **6.4.1. Vòng đời nội dung**

| Trạng thái | Ý nghĩa | Bên thiết lập | Hiển thị trên ứng dụng du khách |
| :---- | :---- | :---- | :---- |
| **DRAFT** | Nội dung đang được soạn. | Quản trị viên nội dung (UC-06) | Không |
| **PENDING_REVIEW** | Đã gửi, đang chờ duyệt. | Quản trị viên nội dung (UC-06) | Không; phiên bản đã xuất bản trước đó vẫn sẵn có. |
| **REJECTED** | Người duyệt trả lại kèm nhận xét. | Content Publication Service | Không |
| **APPROVED** | Đã được phê duyệt và đưa vào một phiên bản gói đã xuất bản. | Content Publication Service | Có |
| **ARCHIVED** | Không còn sử dụng; được loại khỏi ứng dụng trong gói tiếp theo. | Content Publication Service | Không |

Luồng: DRAFT → PENDING_REVIEW → APPROVED, hoặc → REJECTED → DRAFT (sau khi chỉnh sửa).

UC-06 chỉ tạo DRAFT và PENDING_REVIEW. Mọi thay đổi trạng thái khác do Content Publication Service thực hiện.

# **7. YÊU CẦU PHI CHỨC NĂNG**

**Yêu cầu phi chức năng đề xuất:** Các giá trị trong ngoặc vuông là mục tiêu ban đầu cần được nhóm xác nhận và kiểm chứng. Thiết bị kiểm thử, điều kiện mạng, tải công việc, phương pháp đo và minh chứng nghiệm thu: \[?\]. NFR-08, NFR-10, NFR-12 và tuyên bố tuân thủ Nghị định 13/2023/NĐ-CP trong NFR-07 là các mục tiêu mong muốn, không phải tiêu chí nghiệm thu MVP.

* **NFR-01 — Hiệu năng:** Màn hình Chi tiết địa điểm tải trong \[2\] giây trên 4G và \[1\] giây từ Local Content Store.
* **NFR-02 — Hiệu năng:** Tìm kiếm lân cận trả kết quả trong \[3\] giây sau khi có vị trí GPS hợp lệ.
* **NFR-03 — Hiệu năng:** Âm thanh tạo sẵn bắt đầu trong \[2\] giây sau khi chọn Phát; TTS dự phòng bắt đầu trong \[3\] giây khi thiết bị có giọng đọc cho languageCode thực tế được sử dụng.
* **NFR-04 — Ngoại tuyến:** Base Content tối đa \[150\] MB. Khi nội dung cơ bản đã xác minh và có cục bộ, tìm kiếm/duyệt danh mục UC-01, UC-02 và UC-03 hoạt động không cần mạng. Android đóng gói nội dung cơ bản; Web du khách cần cấp phát trực tuyến lần đầu và môi trường lưu trữ hỗ trợ đã kiểm chứng. Ứng dụng phải báo đúng trạng thái sẵn sàng offline. Map View cần mạng; offline dùng danh sách. Phát cần audio cục bộ hoặc TTS offline tương thích, theo UC-03 E1/E3 và mục 2.4.
* **NFR-05 — Nền tảng:** Client du khách đã thống nhất là Android và trình duyệt Web. Android tối thiểu đề xuất \[8.0+\]; phiên bản trình duyệt/thiết bị và phạm vi khả năng hỗ trợ còn là \[?\]. Không yêu cầu ứng dụng iOS native trong đợt bàn giao này. Use case chung cần bằng chứng nghiệm thu riêng từng nền tảng, nhất là audio/TTS, kho offline, quyền và hành vi tiền cảnh/chạy nền.
* **NFR-06 — Bảo mật:** Mọi lưu lượng máy khách–máy chủ dùng HTTPS (TLS). Web Admin yêu cầu xác thực và phân quyền theo vai trò. Gói nội dung được xác minh checksum và chữ ký theo UC-04 BR-02.
* **NFR-07 — Quyền riêng tư:** Vị trí chỉ dùng cho tìm kiếm lân cận và geofencing; sự kiện phân tích dùng mã định danh ẩn danh. Yêu cầu tuân thủ đề xuất: xử lý dữ liệu tuân thủ Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân, cần người có chuyên môn xác nhận. Đây là yêu cầu cần rà soát, không phải bằng chứng đã tuân thủ.
* **NFR-08 — Pin:** Theo dõi geofence tiêu thụ tối đa \[3\]% pin mỗi giờ đi bộ sử dụng bình thường. Thiết bị và điều kiện đo: \[nhóm điền\].
* **NFR-09 — Độ tin cậy:** Cập nhật lỗi hoặc gián đoạn không bao giờ làm hỏng hay xóa nội dung đã cài, theo UC-04 BR-02.
* **NFR-10 — Khả năng tiếp cận:** Android hỗ trợ phóng cỡ chữ hệ thống và nhãn trình đọc màn hình; Web du khách hỗ trợ zoom trình duyệt, bàn phím và nhãn trình đọc màn hình. Cả hai hướng đến độ tương phản WCAG 2.1 AA.
* **NFR-11 — Bản địa hóa:** UI và nội dung hướng dẫn dùng Unicode; ngôn ngữ được hỗ trợ và cờ hỗ trợ được liệt kê trong Phụ lục.
* **NFR-12 — Tính sẵn sàng:** Server API sẵn sàng ít nhất \[99.5\]% mỗi tháng.
* **NFR-13 — Khả năng mở rộng:** Hệ thống hỗ trợ ít nhất \[500\] địa điểm và \[1,000\] người dùng đồng thời.
* **NFR-14 — Tài nguyên đa phương tiện:** Tệp âm thanh dùng \[MP3 hoặc M4A (AAC)\] với bitrate \[64–128\] kbps. Giới hạn dung lượng tải lên và thời lượng đề xuất vẫn theo UC-06 BR7.

# **8. PHỤ LỤC**

Phụ lục này định nghĩa tên gọi tương đương, cấu hình MVP cố định, hợp đồng dữ liệu, quyền, truy vết phạm vi, rủi ro và tham chiếu giao diện cho bảy use case hiện có. Các đề xuất và chỗ trống trong ngoặc vuông cần được nhóm xác nhận hoặc hoàn thiện.

**8.1. Thuật ngữ và tên gọi tương đương**
