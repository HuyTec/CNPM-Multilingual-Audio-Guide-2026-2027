# Audit độc lập: UC, Activity và Sequence

## Phạm vi và cách đối chiếu

- Nguồn yêu cầu: `docs/en/PRD_Report.md` (UC-01 đến UC-07 và các BR kèm theo).
- Nguồn sơ đồ: bảy activity trong `docs/en/System_Analysist.drawio.xml` và sequence demo `demo/06_sequence.drawio.xml`.
- Chỉ kết luận theo yêu cầu đã ghi. Sáu quyết định ở đầu PRD vẫn là **pending**, không được coi là requirement đã chốt.

## Phát hiện cần xử lý trước khi xem bộ sơ đồ là nhất quán

| Mức | Mã | Phát hiện và bằng chứng | Tác động / sửa tối thiểu |
|---|---|---|---|
| Cao | F-01 | **UC-04 E2 chưa có nhánh resume.** Activity đi từ `Download completed? = No` tới `E2 – Network lost, pause and keep progress` rồi kết thúc. PRD UC-04 E2 và Postcondition nói tiến trình được giữ để Tourist có thể resume khi kết nối trở lại. | Thêm sự kiện `network restored` và hành động Tourist `Resume download`, quay về download với offset/progress đã lưu; chỉ verify sau khi tải hoàn tất. |
| Cao | F-02 | Fork–join UC-04 tách `Download package bytes` và `Update download progress on UI`, nhưng không nêu vòng lặp/cancel trên nhánh tiến trình. Một join chỉ đúng khi cả hai hoạt động hữu hạn; "update progress" là hành vi lặp trong suốt download. | Đổi nhánh UI thành loop/interruptible activity `while downloading: publish progress`; khi download hoàn tất hoặc bị mất mạng thì đóng loop. Giữ fork–join chỉ nếu hai nhánh có điều kiện kết thúc rõ ràng. |
| Cao | F-03 | **UC-05 không ghi nhận thời điểm gửi thông báo.** Activity kiểm tra `Notification cooldown expired?` nhưng sau `Display OS notification` hoặc `Show In-App Banner` không có bước persist `lastNotifiedAt`/notification log. | Không thể thực thi BR1 đáng tin cậy ở lần geofence sau. Thêm bước System ghi log sau khi delivery thành công; không ghi khi permission lỗi/cooldown/no display. |
| Cao | F-04 | **UC-07 không kiểm soát state transition feedback.** Node `Update feedback status; save ... (BR2)` cho phép bất kỳ trạng thái nào mà không kiểm tra `NEW → IN_REVIEW → RESOLVED`. | Thêm decision/guard `valid next status?`; chặn bỏ qua trạng thái hoặc mở lại `RESOLVED`, phù hợp UC-07 BR2 và quyết định MVP pending #5. |
| Cao | F-05 | Sau `Filter feedback?`, activity UC-07 luôn tới chọn record. Không có nhánh Empty State khi bộ lọc feedback không có bản ghi; E1 PRD nói áp dụng khi không có dữ liệu cho period **hoặc filter**. | Thêm `matching feedback exists?` sau A2 (và khi mở list), hiển thị Empty State nhưng vẫn cho đổi filter/quay dashboard. |

## Lệch trách nhiệm, điều hướng và lifecycle

| Mức | Mã | Phát hiện và bằng chứng | Tác động / sửa tối thiểu |
|---|---|---|---|
| Trung bình | F-06 | Swimlane UC-07 chưa phân tách đúng Actor/System: các node ở lane Actor như `Open feedback list: display recent feedback`, `Select a feedback record and display its details`, và `Update feedback status; save ...` gộp cả thao tác Admin lẫn response/lưu của System. | Tách mỗi cặp thành action Actor (`Open list`, `Select record`, `Choose next status`) và action System (`Load/display`, `Validate transition and persist timestamp`). Không chỉ đổi màu hoặc vị trí. |
| Trung bình | F-07 | Activity UC-07 đặt Export chỉ sau khi Admin đã chọn và cập nhật feedback. PRD A3 là lựa chọn export dữ liệu analytics/feedback của kỳ hiện tại, không yêu cầu phải đổi feedback trước. | Rẽ nhánh export từ dashboard/list (hoặc một decision menu) để A3 độc lập với update feedback; giữ E2 retry tại nhánh export. |
| Trung bình | F-08 | Activity UC-01 yêu cầu quyền vị trí rồi đi thẳng tới decision `Permission granted?`; không có action Actor rõ ràng cho `grant/deny`, trong khi Basic Flow bước 3 là hành động Tourist. | Đặt `Tourist grants/denies permission` ở lane Actor giữa request và decision. Giữ E1 quay về các phương án thủ công. |
| Trung bình | F-09 | UC-03 TTS path đi từ `E1 – Audio unavailable/invalid; fallback to OS TTS` thẳng vào `Playback event?`. Không có bước hệ thống khởi tạo TTS, bắt đầu phát, và đưa state về Playing/controls. | Thêm `Prepare/start TTS using scriptText` rồi hội tụ với luồng playback trước event loop. Điều này chứng minh Postcondition UC-03 và BR2, thay vì chỉ ghi fallback bằng nhãn. |
| Trung bình | F-10 | Sequence UC-02→UC-03 chỉ có note cho trường hợp không còn content; không có combined fragment/guard thể hiện rõ thứ tự fallback `Selected → EN → VI` của UC-02 BR2. Label trả về `Approved revision / EN / VI fallback result` không đủ để chỉ ra thứ tự hay chỉ dùng APPROVED. | Thêm `alt`/`opt` guards: selected APPROVED; nếu không có thì EN APPROVED; nếu không có thì VI APPROVED; cuối cùng no content. Có thể giữ một API call nếu API đảm nhận fallback, nhưng message phải nêu contract/priority. |
| Trung bình | F-11 | Sequence gọi `Audio / OS TTS` bằng một message duy nhất `Play pre-rendered audio; otherwise ... TTS`; không mô hình hóa E1 playback failure, E2 audio focus loss/restore, E3 output-device disconnect của UC-03. | Nếu sequence được gọi là demo happy-path thì đổi tên thành **Happy path** và dẫn tới activity cho exceptions. Nếu là sequence đại diện UC-03 thì thêm các `alt`/`break` tương ứng, gồm pause và stored position. |
| Trung bình | F-12 | UC-06 activity kết thúc tại `PENDING_REVIEW`, đúng scope UC-06, nhưng trong bộ hiện được audit không có biểu diễn nào về external boundary `PENDING_REVIEW → APPROVED → published ContentPackage`. Đây là điều kiện để UC-02/UC-04 có dữ liệu mobile. | Không đưa Publisher vào UC-06 như action của Admin. Thêm note/component/sequence riêng về external Content Reviewer/Publisher và handoff package version; liên kết rõ tới "Content publication dependency" đầu PRD. |

## Điểm cần chốt hoặc làm rõ trước khi mã hóa

| Mã | Quan sát | Quyết định cần có |
|---|---|---|
| D-01 | UC-03 cho TTS fallback, nhưng UC-06 activity ghi `Upload audio for each language`; phần publication dependency hiện mô tả package chứa script và audio. Trong khi đó quyết định pending #1 đề xuất audio là tùy chọn. | Chốt audio bắt buộc hay tùy chọn. Nếu tùy chọn: đổi UC-06 thành `upload audio if available`, package cho phép approved script không audio, và UC-04 atomicity áp dụng cho tập asset thực sự có trong package. |
| D-02 | UC-07 Empty State hiện kết thúc toàn bộ activity khi analytics rỗng. PRD chưa nói rõ analytics rỗng có được tiếp tục sang feedback list độc lập không. | Khuyến nghị cho phép tiếp tục Feedback: analytics và feedback là hai tập dữ liệu khác nhau, nên Empty State analytics không được chặn quản lý feedback. |
| D-03 | UC-04 E2 nói có thể preserve/resume `when possible`, nhưng chưa quy định resume integrity, package version đã bị supersede, hoặc cleanup partial file. | Xác định metadata tối thiểu: packageId/version, byte offset, checksum/ETag; nếu server version đổi thì discard partial package và tải package mới. |

## Các điểm đã khớp, không nên thay đổi sai bản chất

- UC-02 activity kiểm tra content APPROVED trước `Load scriptText`, và có E1/E2 phù hợp PRD.
- UC-05 mở Location Details rồi trigger UC-02 bằng `locationId`; không tự phát audio, phù hợp boundary UC-02/UC-03.
- UC-06 bảo toàn published version và chỉ lưu `DRAFT`/`PENDING_REVIEW`, phù hợp BR4 và external publication dependency.
- UC-04 chỉ kiểm tra package APPROVED và đặt verify trước atomic install; hướng xử lý này đúng với yêu cầu tránh ghép script/audio khác version.
- Chỉ UC-04 hiện có fork–join. Các A/E ở UC khác là alternative/exception, không nên vẽ fork giả.

## Thứ tự khuyến nghị để sửa và kiểm chứng

1. Chốt D-01 và D-02; chúng tác động trực tiếp activity, sequence, ERD/class/package contract.
2. Sửa F-01 đến F-05 trước, vì chúng làm sai lifecycle hoặc BR có thể kiểm thử được.
3. Sửa F-06 đến F-12, rồi chạy trace theo các kịch bản: update bị mất mạng/resume, cooldown lần hai, feedback status invalid, filter rỗng, selected-language fallback và audio failure.
4. Sau sửa, mở từng XML trong draw.io để kiểm tra visual; XML parse hợp lệ không chứng minh swimlane, guard và fork/join đúng ngữ nghĩa.
