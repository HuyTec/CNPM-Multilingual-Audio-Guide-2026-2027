# Tiêu chí nghiệm thu có truy vết PRD

**Chưa có hàng nào tự động PASS.** Đây là checklist giao việc, không phải biên bản kiểm tra.

- **D0:** bắt buộc trước demo Web cốt lõi và đồng bộ Admin. TTS thật nằm trong D0.
- **D1:** phần còn lại của PRD cần triển khai/kiểm tra tiếp; chưa đạt thì chỉ được nói demo một phần, không “hoàn thành UC”.
- Trạng thái kết quả: `PASS`, `FAIL`, `NOT_RUN`, `BLOCKED`, `MOCK_ONLY`. Chỉ PASS khi có evidence đúng loại. N/A chỉ dùng khi PRD cho phép nền tảng không hỗ trợ và đường thay thế đã được xác minh.
- Mỗi ID có: commit hoặc local diff, môi trường/browser, bước/thao tác, expected/actual và link evidence. Không lấy UI screenshot làm bằng chứng DB/voice/offline.

## Luồng vào Web, quyền và dữ liệu chung

| ID | Mốc | Nguồn | Tiêu chí quan sát được |
| --- | --- | --- | --- |
| W01 | D0 | §2.4, UC-01 A4 | QR chứa URL HTTPS public đúng locationId; quét bằng camera điện thoại khác mở đúng bài; không cần login/APK/GPS |
| W02 | D0 | UC-01 A4 | Refresh/deep link hoạt động; ID sai/không public báo lỗi và đường về khám phá, không mở một địa điểm khác |
| W03 | D0 | UC-01/02/03 | Language/mode nằm trong URL, back/forward không đổi nhầm bài; mở URL hoặc banner không phát audio/TTS |
| W04 | D0 | Yêu cầu bổ sung người dùng | Có link APK hợp lệ, nêu rõ Android và phiên bản; thử tải. Nếu chưa có artifact: trạng thái chưa khả dụng, không link chết/giả; chặn riêng tuyên bố “tải APK sẵn sàng” |
| W05 | D0 | §2.4, §6.2 | Public/Admin dùng chung DB/ID/revision; mạng lỗi không tự thay nội dung bằng mock; reload vẫn đọc dữ liệu đã lưu |
| W06 | D0 | UC-06/07, NFR-06 | Public không truy cập dữ liệu/endpoint Admin; thiếu/sai quyền bị từ chối; credentials không nằm trong bundle/log/URL |

## UC-01 — khám phá và chọn địa điểm

| ID | Mốc | Nguồn | Tiêu chí quan sát được |
| --- | --- | --- | --- |
| U101 | D0 | A1/A3, E3/E4 | Tìm từ khóa/danh mục qua API thật; kết quả rỗng có gợi ý; chọn đúng ID mở detail; phân trang/filter không mất trạng thái |
| U102 | D1 | Main, BR1/2 | Nearby có permission flow, lat/lon/accuracy thật; radius 100–500 m, sort khoảng cách tăng; giá trị shortlist `[3–5]` được ghi là đề xuất nếu chưa chốt |
| U103 | D1 | E1, BR3 | Denied/revoked/unsupported/GPS timeout không chặn keyword/category/QR; không hiển thị khoảng cách fixture như đo thật |
| U104 | D1 | E2 | Không có điểm gần thì báo đúng, cho mở rộng trong giới hạn radius hoặc chọn thủ công |
| U105 | D1 | A2, §2.4 | Map online pan/zoom/marker mở đúng bài; lỗi provider/offline quay về list; attribution theo provider, không tải bulk tiles |

## UC-02 — text thuyết minh

| ID | Mốc | Nguồn | Tiêu chí quan sát được |
| --- | --- | --- | --- |
| U201 | D0 | Main, BR1 | Hợp lệ location + language + mặc định FULL → chỉ lấy nonempty APPROVED; DRAFT/PENDING_REVIEW/REJECTED/ARCHIVED không lộ qua direct URL/API |
| U202 | D0 | A1 | Đổi ngôn ngữ giữ locationId/scriptType; text và audio cùng effective language; request cũ đến muộn không đè lựa chọn mới |
| U203 | D0 | A2 | FULL/SHORT là bài tương ứng đã duyệt, không cắt chuỗi/tóm tắt AI runtime; đổi mode giữ location/ngôn ngữ yêu cầu |
| U204 | D0 | E1, BR2 | Thiếu ngôn ngữ đã chọn → EN → VI; giữ scriptType, hiển thị rõ fallback; không đổi sang FULL chỉ vì SHORT thiếu |
| U205 | D0 | E2 | Không có APPROVED ở mọi fallback → thông báo đang cập nhật, khóa Nghe, không gọi audio/TTS; điều hướng khám phá vẫn dùng được |
| U206 | D1 | Assumptions, NFR-04 | Offline đọc đúng bản đã xác minh; thiếu baseline không báo Offline Ready; nội dung không bị đổi revision âm thầm |

## UC-03 — audio và TTS: D0, riêng nghiệm thu offline U310 thuộc D1

| ID | Nguồn | Tiêu chí quan sát được |
| --- | --- | --- |
| U301 | Main, BR1/2/5 | Bấm Play mới phát; ưu tiên audio hợp lệ khớp location/effective language/mode/revision; nhãn nguồn phát chính xác |
| U302 | E1, A2 | Audio null/404/không decode/lỗi load/prepare/lỗi giữa bài → release nguồn lỗi, dùng TTS đúng text; không hai nguồn phát đồng thời |
| U303 | A2, BR3 | TTS đọc nguyên văn scriptText APPROVED đã tải; không dịch/sáng tác/tóm tắt; speech lang theo effective language sau UC-02 fallback |
| U304 | A2, E3 | Giọng xuất hiện trễ/voiceschanged được xử lý; không có engine, không có giọng đúng ngôn ngữ, lỗi tổng hợp, network voice mất mạng → thông báo cụ thể, giữ text/lựa chọn, cho retry |
| U305 | Main, E3 | Preparing có timeout/error exit; chỉ chuyển Playing khi audio/TTS thực sự start; không spinner vô hạn hoặc báo thành công khi im lặng |
| U306 | A1 | Pause/Resume có hiệu lực cho cả hai nguồn; seek tiến/lùi và tốc độ thực sự tác động tới nội dung đọc, không chỉ đổi label |
| U307 | A1/E2 | TTS có vị trí/đoạn theo dõi được; kiểm chứng pause/resume/seek/rate trên browser demo. Nêu độ chính xác; thanh thời gian giả không được PASS |
| U308 | E2 | Gián đoạn đầu ra/thiết bị theo khả năng browser chọn: phiên dừng/giữ vị trí và tiếp tục đúng vị trí; nếu không quan sát được thì NOT_RUN, không giả hỗ trợ native |
| U309 | BR1/5 | Đổi location/language/mode, back, unmount, double-click, phản hồi cũ: không speech trùng/nguồn cũ chạy tiếp; stop/cancel không kích hoạt callback hoàn thành sai |
| U310 | BR4, E3; D1 | Khi kiểm tra offline: text có sẵn + giọng đúng ngôn ngữ hoạt động không mạng mới được dùng; offline voice thiếu/lỗi thì giữ text/báo E3. Không có môi trường offline → NOT_RUN. D0 vẫn phải xử lý lỗi network voice, nhưng chưa được tuyên bố TTS offline hoàn tất |
| U311 | §2.4 acceptance | Nghe thử trên browser/điện thoại thật, ghi OS/browser/voice/language và nguồn; mock voice/headless không thay evidence nghe thực |

Nếu một control TTS không thực hiện được: ghi FAIL/BLOCKED kèm nguyên nhân và giải pháp; không tự loại A1/E2 khỏi PRD để tuyên bố hoàn thành UC-03.

## UC-04 — offline và cập nhật an toàn

| ID | Mốc | Nguồn | Tiêu chí quan sát được |
| --- | --- | --- | --- |
| U401 | D1 | §2.4, Precondition | Web lần đầu online giải thích dung lượng/chuẩn bị nội dung và xác minh; first-ever offline không báo đã sẵn sàng |
| U402 | D1 | Main, BR2/3/4 | Manifest/version, APPROVED, checksum và signature theo NFR-06 được kiểm tra; kích hoạt đúng bộ location/script/audio nguyên tử, không trộn phiên bản |
| U403 | D1 | A1/A3 | Không có bản mới hoặc hủy xác nhận: không tải/cài/đổi bản hiện tại |
| U404 | D1 | E1 | Thiếu quota/storage hoặc lỗi ghi: báo dung lượng/cách thử lại, không thay bản cũ |
| U405 | D1 | E2/E3, NFR-09 | Mất mạng bỏ tải dở; checksum/signature sai hoặc commit lỗi: bỏ staging, giữ bản cũ dùng được; retry không làm hỏng kho |
| U406 | D1 | A2, BR1 | Chỉ xóa optional có xác nhận; hủy không đổi; Base Content không bị app xóa; optional delete dùng được offline |
| U407 | D1 | NFR-04, §2.4 | Reload trang khi offline vẫn mở shell/list/text/audio đã chuẩn bị; storage bị xóa/thu hồi phản ánh đúng readiness và yêu cầu chuẩn bị lại |
| U408 | D1 | §5.1, lifecycle | Gói xử lý archived/removed-item visibility cùng phiên bản; không hiển thị nội dung bị loại qua local cache cũ sau commit |

## UC-05 — vị trí và banner trên Web

| ID | Mốc | Nguồn | Tiêu chí quan sát được |
| --- | --- | --- | --- |
| U501 | D1 | Main, A1, §2.4 | Phiên active có location monitoring và local verified geofence: entry mới tạo banner đúng điểm; không tuyên bố closed-tab/background chưa test |
| U502 | D1 | A3, BR1 | Cooldown theo cấu hình mỗi location, vào/ra liên tục không spam; không tự bịa thời gian là PRD đã chốt |
| U503 | D1 | A2, BR2/3/4 | Banner theo ngôn ngữ hiện tại và verified name, chứa đúng locationId; không click thì không điều hướng; click mở detail/UC-02, không Play |
| U504 | D1 | E1/E2 | Location denied/revoked ngừng theo dõi; notification denied/unsupported có thể dùng foreground banner nếu location còn hợp lệ; manual entry luôn dùng được |

## UC-06 — Admin có lưu thật và bảo vệ published

| ID | Mốc | Nguồn | Tiêu chí quan sát được |
| --- | --- | --- | --- |
| A601 | D0 | Main/A2, Precondition | Admin được xác thực/đúng quyền; list/edit load từ DB gồm bản biên tập, không chỉ public APPROVED; refresh giữ dữ liệu |
| A602 | D0 | A1, BR2 | Save Draft cho phép thiếu trường; lưu DRAFT và updatedBy/updatedAt thật; không lộ public |
| A603 | D0 | Main, E2, BR1 | Submit kiểm tra tên/danh mục/lat/lon/radius hợp lệ cả server và UI; inline lỗi/focus lỗi đầu; thành công chỉ PENDING_REVIEW |
| A604 | D0 | Main, BR3 | Script VI/EN × FULL/SHORT lưu riêng đúng khóa/revision; đổi tab/language không ghi đè biến thể khác |
| A605 | D0 | Main, E1 | Upload định dạng được cấu hình/chấp thuận, storage thật và preview thật; sai format/network fail giữ toàn form và cho chọn lại/retry; không tự áp giới hạn chưa xác nhận |
| A606 | D0 | A2 | Thay audio một language/type giữ nguyên các script/audio/fields còn lại; reload vẫn đúng; không lưu blob URL tạm làm URL public lâu dài |
| A607 | D0 | BR4 | Sửa cả metadata lẫn script/audio tạo bản thay thế; Web vẫn nhận bản published cũ đến khi dependency ngoài duyệt/xuất bản |
| A608 | D0 | §6.4, Assumptions | Demo publication bằng fixture/script nội bộ có nhãn, không nút tự APPROVED trong UC-06, không public bypass; revision mới nhất quán và cache được invalidate |
| A609 | D1 | A2 | REJECTED có dữ liệu/comment phù hợp; sửa → DRAFT hoặc PENDING_REVIEW; version public cũ vẫn được giữ |

## UC-07 và sự kiện sử dụng

| ID | Mốc | Nguồn | Tiêu chí quan sát được |
| --- | --- | --- | --- |
| EV01 | D0 | §2.4 acceptance | Thực sự start mới tạo event; server derive content/location/language; cùng eventId retry chỉ một record; xung đột cùng ID khác content bị từ chối |
| EV02 | D0 | UC-03 A1, analytics dependency | Pause/resume/seek/rate/fallback trong một phiên không đếm thêm; chủ động replay sau hoàn tất tạo session mới; enqueue/ack bền vững |
| EV03 | D1 | §5.2 event contract | occurredAt là lúc xảy ra, connectivity phản ánh thực; retry offline không biến thành online/timestamp lúc nhận; không thu GPS/device ID ngoài phạm vi |
| A701 | D0 | Main, A1, BR1/3 | Admin có quyền, mặc định 30 ngày; 4 metric lấy từ cùng DB/period/timezone, refresh sau lượt nghe thấy số liệu đúng; zero offline hợp lệ khi chưa có sự kiện offline |
| A702 | D1 | Main, A2 | Feedback list/detail/category/rating/status từ DB; filter category/status và phân trang đúng trong kỳ, URL lưu period/filter/detail/page |
| A703 | D1 | Main, BR2 | Server chỉ NEW → IN_REVIEW → RESOLVED, không skip/reopen; updatedAt lưu thật; UI aria-live; refresh giữ trạng thái |
| A704 | D0 | A3/E2, demo slice | XLSX metric kỳ đang chọn khớp DB; lỗi xuất giữ state, có Retry. Nếu chưa có feedback thật, file ghi rõ phạm vi chỉ analytics, không tự nhận toàn A3 |
| A705 | D1 | A3/E2 | XLSX đủ analytics + feedback khớp active filters trên toàn bộ các trang; lỗi xuất thử lại được; không dùng một trang đang nhìn làm toàn báo cáo |
| A706 | D0 | E1 | Kỳ không có dữ liệu hiển thị Empty State thật, không chart seed thay thế; thay period không làm mất state khác không liên quan |

## UI, CI/CD và điều kiện bàn giao

| ID | Mốc | Tiêu chí quan sát được |
| --- | --- | --- |
| UI01 | D0 | Đọc ba skill frontend/local command trước UI; dùng tokens/component và nhận diện Admin hiện có, preview và review screenshot thật. Figma không thuộc điều kiện nghiệm thu đợt này theo lựa chọn người dùng |
| UI02 | D0 | 320/768/1024/1440 px; loading/empty/error/long; keyboard/focus-visible/label/aria-live/skip-link; không overflow, sticky player không che nội dung/focus; Intl vi-VN |
| UI03 | D0 | Preview và review giao diện đã render; báo P0/P1/P2 theo file:line, sửa hết P0/P1 của phần demo; không coi “enterprise” là thêm animation/card |
| QA01 | D0 | Backend verify + integration Postgres/PostGIS riêng; hai web lint/build; real-backend E2E. Báo pass/fail/skipped, không coi test bị skip là đạt |
| QA02 | D0 | Test tự động cho resolver/state/errors + nghe TTS thật; regress Admin/Android API không bị phá; không sửa test để che mất chức năng |
| CI01 | D0 | Workflow có job đúng phần đã đổi; validate startup/OS/browser deps; run GitHub đúng SHA có URL/status. Chưa có run thì NOT_RUN dù local pass |
| CD01 | D0* | Tách trạng thái CD: chưa cấu hình / đã cấu hình chưa chạy / deploy thành công đã smoke. Public QR chỉ READY khi URL HTTPS truy cập từ điện thoại và audio/API ổn. Thiếu target/secret là BLOCKED riêng CD |
| R01 | D0 | Cuối phiên có báo cáo ngắn theo template: đã demo, kiểm tra, CI/CD riêng, thiếu sót và bước tiếp; dữ liệu/screenshot/log có provenance |

`D0*`: có thể demo local có nhãn khi chưa có CD; không được nói khách truy cập public được hoặc CD chạy tốt. APK chưa có chặn W04 READY, không chặn luồng đọc/nghe Web độc lập.

## Bộ tổ hợp kiểm tra “brute force” hữu hạn

Mục tiêu là vét các nhánh nghiệp vụ quan trọng, không chạy hàng nghìn thao tác UI lặp hoặc brute-force đăng nhập.

| Chiều thử | Giá trị |
| --- | --- |
| Nội dung | VI/EN × FULL/SHORT; selected language thiếu; EN thiếu; mọi bản thiếu; APPROVED/DRAFT/PENDING_REVIEW/REJECTED/ARCHIVED |
| Audio | Hợp lệ; thiếu; URL/format lỗi; lỗi giữa bài |
| Voice | Đúng ngôn ngữ local; đúng ngôn ngữ cần mạng; sai ngôn ngữ; không có/engine lỗi; voices đến trễ |
| Mạng/kho | Online; offline đã chuẩn bị; offline chưa chuẩn bị; quota/commit lỗi; gói sai hash/signature; cache cũ |
| Thao tác | Open; Play; double Play; Pause/Resume; seek/rate; switch language/mode/location; Back; unmount; retry lost ack; replay |
| Quyền | Public; Editor; System Admin; anonymous; denied/revoked location/notification |

1. Dùng table-driven unit/contract tests vét **mọi tổ hợp hợp lệ** của resolver và playback guards, prune case không thể xảy ra sau khi content bị từ chối. Ghi expected outcome: audio / TTS / text-only error / reject. Các guard test dùng adapter giả là hợp lệ nhưng không chứng minh âm thanh thực.
2. Real DB integration kiểm tra từng status/language/type, revision protection, upload một biến thể, idempotency, strict feedback transition và report filtering.
3. Browser E2E chọn từng AC + tổ hợp nguy hiểm: fallback language + audio lỗi + voice sai; đổi bài khi đang prepare; lost ack + retry; draft edit + public refresh; offline update lỗi + đọc bản cũ.
4. Manual trên máy/điện thoại demo: QR ngoài localhost, TTS nghe được, voice đúng, controls, audio interruption, offline reload và permissions. Thiếu thiết bị thì NOT_RUN.

## Điểm chưa xác nhận không được tự bịa

Browser/OS hỗ trợ; catalogue ngôn ngữ; cooldown; quota/base size; signature/key publication; audio limits; hosting và secret deploy. NFR số trong ngoặc như 2s/3s/150MB/500 điểm/1.000 người là mục tiêu đề xuất, không tự tuyên bố đạt benchmark. Báo đo thực tế cùng điều kiện; không thêm Kafka, live AI viết bài, login du khách, reviewer UI hoặc feedback submission mới chỉ để lấp khoảng trống.
