# Giai đoạn 3 — Kiểm tra toàn bộ bổ sung PRD

Đã đọc và kiểm tra cả bốn lượt bổ sung: 7 file UC, 8 file mục ngoài UC, 274 dòng đề xuất và 4 sơ đồ khái niệm Mermaid. Đây là kiểm tra **danh sách thay đổi trước khi dán vào PRD**, không phải chứng nhận DOCX đã được sửa hoặc đã kiểm tra bố cục.

## Kết quả theo bốn điểm bắt buộc

| Điểm | Kết quả | Bằng chứng và giới hạn |
| --- | --- | --- |
| (a) Không có nội dung bị xóa | Đạt đối với thao tác trên file và bộ đề xuất. | SHA-256 của cả DOCX và XLSX bằng giá trị ghi nhận ở Giai đoạn 1. 274 dòng chỉ dùng THÊM CÂU / THÊM DÒNG / ĐIỀN MỤC TRỐNG. Giữ A3 UC-05, Question mục 5, BR-01–BR-04 và heading 6.4 lặp; hiệu đính chỉ bằng chú thích/alias. Chưa thực hiện thao tác dán vào DOCX. |
| (b) Trạng thái/actor/thuật ngữ thống nhất | Đạt về quy ước và nghĩa của bộ bổ sung. | APPROVED bao gồm published; không định nghĩa PUBLISHED mới. UC-06 chỉ lưu DRAFT/PENDING_REVIEW; Admin yêu cầu deactivate, publication service chuyển ARCHIVED. Hiệu lực archive sau install, không xóa vật lý Base Content. Feedback NEW → IN_REVIEW → RESOLVED, RESOLVED final. Ba actor chính thống nhất bằng aliases; tên cũ vẫn tồn tại theo R1. scriptType/effective language, Local Content Store, record version/packageVersion và timestamp/occurredAt được phân biệt. |
| (c) Tham chiếu BR/E/A đều tồn tại | Đạt trên tổ hợp PRD gốc và bộ đề xuất khi áp dụng đủ các đoạn liên quan. | Đã kiểm tra reference có định danh UC cụ thể bằng script và rà soát các mã local. A/E/BR mới được khai báo trong chính file UC hoặc file liên quan. Reference UC-06 A2 → BR4 cũ vẫn giữ nhưng có câu chỉ rõ BR5 là confirmation rule. UC-06 E1/E2 giữ số bước cũ và thêm chỉ dẫn bước thực tế. Áp dụng riêng một phần có thể tạo reference chưa tồn tại; README nêu nhóm đoạn phải dán đồng bộ. |
| (d) Không tạo mâu thuẫn mới | Không phát hiện mâu thuẫn nghiệp vụ mới trong bộ bổ sung sau rà soát. | Chính sách online Server/offline Local được giữ, cùng version location/text/audio; fallback giữ scriptType hết Selected → EN → VI rồi mới FULL; install chờ playback kết thúc kể cả background; toggle notification off khác OS permission denied; audio optional nên checkbox chỉ khi có audio. Những thông tin thiếu được đánh dấu đề xuất/placeholder, không coi là quyết định đã duyệt. Câu mâu thuẫn gốc giữ lại với chú thích như R2, không khẳng định chúng đã bị xóa khỏi PRD. |

### Kiểm tra cấu trúc và định danh

- 15 bảng thay đổi có đúng bốn cột yêu cầu; mọi dòng đều có kiểu được phép.
- 7 UC hiện có được giữ; không định nghĩa UC-08, UC-09 hoặc UC-10.
- 6 Epic giữ tên/số; UC-05 ở Epic 1. 15 mã FR-01–FR-15 là mã truy vết mới, không thêm UC.
- 14 NFR-01–NFR-14 có định danh riêng, giữ số liệu trong [ ]; không báo cáo đã đạt các chỉ tiêu.
- 4 sơ đồ chỉ thể hiện entities/components/boundaries có trong nguồn. ERD khái niệm dùng các cạnh tham chiếu, chưa tự chốt cardinality hoặc khóa tổng hợp. Chưa render Mermaid hay kiểm tra visual QA.

### Khai báo A/E/BR sau khi áp dụng bộ đề xuất

| UC | Alternative Paths | Exception Paths | Business Rules |
| --- | --- | --- | --- |
| UC-01 | A1–A5 | E1–E4 | BR1–BR6 |
| UC-02 | A1–A2 | E1–E2 | BR1–BR4 |
| UC-03 | A1–A4 | E1–E5 | BR1–BR2 |
| UC-04 | A1–A5 | E1–E3 | BR-01–BR-07 |
| UC-05 | A1–A4 | E1–E4 | BR1–BR6 |
| UC-06 | A1–A3 | E1–E3 | BR1–BR8 |
| UC-07 | A1–A3 | E1–E3 | BR1–BR3 |

Đã giữ tham chiếu gốc đúng và thêm lời làm rõ cho tham chiếu tồn tại nhưng sai nghĩa/sai bước. Không renumber để làm đẹp. E2 UC-02 không có thay đổi.

## Truy vết đầy đủ 84 mã Excel

“Đã soạn” chỉ xác nhận đoạn thêm đã có trong bộ Markdown, không xác nhận đã dán vào DOCX hoặc các placeholder đã được nhóm điền. Tên file ngắn dưới đây được dẫn qua README. Những phần trùng nhiều mã được gom trong cùng một dòng thay đổi thay vì tạo câu mới lặp lại.

### M01–M15

| Mã | Xử lý cuối | Nơi đối chiếu |
| --- | --- | --- |
| M01 | Đã soạn phần default còn thiếu; giữ khoảng radius đã sửa. | UC_01.md; 08_Appendix.md. |
| M02 | Đã soạn quan hệ shortlist/danh sách; giữ A1/bước 6. | UC_01.md; 08_Appendix.md. |
| M03 | Giữ nguyên, đã áp dụng. | Goal UC-01 trong PRD gốc. |
| M04 | Giữ nguyên, Đã áp dụng theo điều chỉnh của người dùng; không thêm vào E2. | E2 UC-02 trong PRD gốc; ghi nhận UC_02.md. |
| M05 | Đã soạn clarification bước 6 và Otherwise; không lặp lại scope/trigger đã đúng. | UC_05.md. |
| M06 | Giữ bảng service/lifecycle đã áp dụng; không thêm lần hai. | 6.4/6.4.1 PRD gốc; các điểm N02/N04 được làm rõ riêng. |
| M07 | Đã soạn audio theo bộ ba/script, optional. | UC_06.md; 05_Data_Models.md. |
| M08 | Đã soạn localized names/descriptions/thumbnail và điều kiện Submit đề xuất. | UC_06.md; 05_Data_Models.md. |
| M09 | Đã soạn location data trong package và cùng version. | UC_01–UC_06.md; 05_Data_Models.md. |
| M10 | Đã soạn effective language/scriptType giữa load và play. | UC_02.md; UC_03.md. |
| M11 | Đã soạn actor catalog và aliases. | 02_3_Actors.md; các UC liên quan. |
| M12 | Đã điền sáu Epic theo quyết định giữ nguyên số/tên. | 03_Epics.md. |
| M13 | Đã soạn glossary và giải thích aliases, không replace toàn văn. | 08_Appendix.md; các UC. |
| M14 | Đã soạn fixed MVP configuration, không UC cấu hình mới. | 08_Appendix.md; 05_Data_Models.md; UC_01/02/05/06.md. |
| M15 | Đã thêm BR5 và clarification reference A2, giữ BR4 cũ. | UC_06.md. |

### G01–G45

| Mã | Xử lý cuối | Nơi đối chiếu |
| --- | --- | --- |
| G01 | Đã soạn OS prompt khi permission chưa determined. | UC_01.md. |
| G02 | Đã soạn accuracy warning [100] m. | UC_01.md; 08_Appendix.md. |
| G03 | Đã soạn A5 expansion [100] m, trần 500 m. | UC_01.md. |
| G04 | Đã soạn ranking cụ thể, popularity ngoài MVP. | UC_01.md; 08_Appendix.md. |
| G05 | Đã soạn search language/source theo D04. | UC_01.md. |
| G06 | Đã soạn offline map nền đơn giản, không tiles. | UC_01.md; 06_System_Architecture.md; 07_NFR.md. |
| G07 | Đã soạn fallback SHORT hết language chain rồi FULL. | UC_02.md. |
| G08 | Đã soạn skip duplicates và chuyển E2 khi hết fallback. | UC_02.md; không thêm câu vào E2. |
| G09 | Đã soạn bảng ngôn ngữ; flags/catalog chờ nhóm. | 08_Appendix.md; UC_02.md. |
| G10 | Đã soạn version consistency thay phương án local-only bị D04 loại trừ. | UC_02.md; UC_03.md; UC_04.md. |
| G11 | Đã soạn E4 thiếu TTS voice, vẫn đọc text. | UC_03.md; 07_NFR.md. |
| G12 | Đã soạn E5 stream interruption/Retry/TTS khi offline. | UC_03.md. |
| G13 | Đã soạn TTS controls và seek disabled. | UC_03.md. |
| G14 | Đã soạn language/mode change, background và Completed/Replay. | UC_03.md. |
| G15 | Đã soạn temporary/permanent audio-focus loss. | UC_03.md. |
| G16 | Đã soạn optional-package selection/download. | UC_04.md. |
| G17 | Đã soạn app-launch version check [24] hours. | UC_04.md; 08_Appendix.md. |
| G18 | Đã soạn mobile-data confirmation/cancel download. | UC_04.md. |
| G19 | Đã soạn install chỉ sau playback kết thúc. | UC_04.md; không dùng rời screen làm điều kiện đủ. |
| G20 | Đã soạn SHA-256/signature. | UC_04.md; 07_NFR.md. |
| G21 | Đã soạn manifest removal/archive và bảo vệ file Base Content. | UC_04.md; UC_05.md; 06_System_Architecture.md. |
| G22 | Đã soạn cooldown/geofence range đề xuất và phân biệt search radius. | UC_05.md; UC_06.md; 08_Appendix.md. |
| G23 | Đã soạn nearest [20]; movement threshold chờ nhóm. | UC_05.md; 08_Appendix.md. |
| G24 | Đã soạn foreground-only E4/background permission/re-registration. | UC_05.md. |
| G25 | Đã soạn toggle off tắt cả banner/notification, khác E2. | UC_05.md. |
| G26 | Đã soạn localized-name EN/VI fallback. | UC_05.md. |
| G27 | Đã soạn nearest bằng distance device/location. | UC_05.md. |
| G28 | Không thực hiện thao tác xóa A3 vì R1; giữ A3. | UC_05.md; Otherwise có clarification N01. |
| G29 | Đã soạn Admin request/service ARCHIVED và hiệu lực sau install. | UC_06.md; UC_04/05.md; 06_System_Architecture.md. |
| G30 | Đã soạn Location Management list/search/filter. | UC_06.md. |
| G31 | Đã soạn concurrent edit E3. | UC_06.md; 05_Data_Models.md. |
| G32 | Đã soạn audit fields/history; physical storage chờ nhóm. | UC_06.md; 05_Data_Models.md. |
| G33 | Đã soạn publication-time packageVersion, không bắt buộc với draft/pending. | UC_06.md; 05_Data_Models.md; 06_System_Architecture.md. |
| G34 | Đã soạn FULL VI/EN Submit requirement, ghi đề xuất. | UC_06.md. |
| G35 | Đã soạn audio format/size/duration đề xuất. | UC_06.md; 07_NFR.md; 08_Appendix.md. |
| G36 | Đã nối rejected editing/resubmission, không thêm lifecycle lần hai. | UC_06.md; 06_System_Architecture.md. |
| G37 | Đã soạn checkbox chỉ khi có audio. | UC_06.md. |
| G38 | Đã soạn event contract; metric/event-specific applicability chờ nhóm. | UC_07.md; 05_Data_Models.md; 08_Appendix.md. |
| G39 | Đã soạn external offline queue/batch upload. | UC_07.md; 06_System_Architecture.md. |
| G40 | Đã soạn Feedback fields với rating [1–5]. | UC_07.md; 05_Data_Models.md; 08_Appendix.md. |
| G41 | Đã soạn newest-first, [20]/page. | UC_07.md. |
| G42 | Đã soạn RESOLVED final, không reopen. | UC_07.md. |
| G43 | Đã soạn export period + active feedback filters. | UC_07.md. |
| G44 | Đã soạn matrix existing actors, không System Admin read-only UC-06. | UC_06.md; UC_07.md; 08_Appendix.md. |
| G45 | Đã soạn analytics-load error E3/Retry, feedback accessible. | UC_07.md. |

### S01–S13

| Mã | Xử lý cuối | Nơi đối chiếu |
| --- | --- | --- |
| S01 | Đã soạn Document Control, metadata chờ nhóm. | Document_Control.md. |
| S02 | Đã gán FR-01–FR-15; MoSCoW chờ nhóm. | 03_Epics.md. |
| S03 | Đã soạn KPI proposal; missing targets/measurement chờ nhóm. | 01_Product_Goals.md. |
| S04 | Đã soạn MVP/scope traceability, không cam kết UC mới. | 08_Appendix.md. |
| S05 | Đã soạn risks từ nguồn, không bịa probability. | 08_Appendix.md. |
| S06 | Đã soạn link placeholders theo D12; activity links chờ nhóm. | Bảy file UC; không giả định activity diagram đã tồn tại. |
| S07 | Đã soạn Author(s)/Date placeholders. | Bảy file UC. |
| S08 | Đã soạn data/ERD reference model, giữ Question. | 05_Data_Models.md; diagrams/05_1_ERD.mmd. |
| S09 | Đã soạn architecture specifications và conceptual diagram sources; links/physical allocation chờ nhóm. | 06_System_Architecture.md; ba sơ đồ 06_*.mmd. |
| S10 | Đã thêm hai dependency còn thiếu, không lặp bảy service rows cũ. | 06_System_Architecture.md. |
| S11 | Đã soạn 14 NFR đề xuất, chưa kiểm nghiệm chỉ tiêu. | 07_NFR.md. |
| S12 | Đã soạn Appendix catalogs/contracts/roles/errors/screens. Catalog values và metric rules còn placeholder. | 08_Appendix.md. |
| S13 | Đã điền Actors/sáu Epic đúng mapping. | 02_3_Actors.md; 03_Epics.md. |

### T01–T11

| Mã | Xử lý cuối | Nơi đối chiếu |
| --- | --- | --- |
| T01 | Đã thêm spelling note, không sửa chữ cũ. | Document_Control.md. |
| T02 | Đã thêm Bussiness/Business aliases ở UC còn sai; UC-06 đã đúng nên không thêm lại. | UC_01/02/03.md. |
| T03 | Đã thêm pr ovided/provided editorial note. | UC_03.md. |
| T04 | Không renumber BR-01…; reference cũ đúng được giữ. | UC_04.md quy ước hướng dẫn áp dụng. |
| T05 | Đã thêm filename aliases; links để nhóm điền, không rename file. | UC_05/06/07.md. |
| T06 | Đã thêm Type clarification, giữ Client-side. | UC_01.md. |
| T07 | Đã thêm standardized name note, giữ tên cũ. | UC_01.md. |
| T08 | Không thêm Ref column/đổi bảng; đã thêm reference/initialization numbering clarification. Phần “sai cột UC-01” chưa được chứng minh trong bản hiện tại. | UC_01.md; UC_02.md. |
| T09 | Đã thêm dependency notes cạnh Secondary Actor cũ. | Bảy file UC. |
| T10 | Không xóa Question theo R1; điền nội dung phía sau. | 05_Data_Models.md. |
| T11 | Đã thêm quy ước đọc A1–A4, không chuyển ô hoặc sửa câu cũ. | UC_01.md. |

## Bằng chứng giữ nguyên đầu vào

| File gốc | SHA-256 tại Giai đoạn 1 và sau bốn lượt |
| --- | --- |
| PRODUCT REQUIRMENTS DOCUMENT.docx | b0fde352f7936bc05274bc6fd36bfd86f3c11772e96c18568d73c5a300dc2e64 |
| PRD_Ke_Hoach_Sua_Doi.xlsx | 3975dfabb0675189db94500b0e7825f12958324741daaead845f62f507015022 |

N01 được kiểm tra trên DOCX gốc: trong bảng UC-05, Postcondition có hai paragraph; paragraph thứ hai bắt đầu bằng Otherwise và chứa câu mở màn hình. Vì vậy giữ N01/D01 theo điều kiện người dùng đã đưa ra. Các vấn đề N02–N07 đều đã có clarification hoặc cách bảo toàn cấu trúc trong bộ bổ sung. Không có bản trước cập nhật 2 để xác nhận thời điểm phát sinh lịch sử.

## Danh sách cần người quyết định hoặc nhóm hoàn thiện

Không hỏi lại các quyết định D01–D13 đã chốt. Các điểm dưới đây là dữ liệu/đề xuất chưa có để hoàn thiện, không chặn việc soạn bộ bổ sung:

| Điểm | Phần còn cần | Phương án ít ảnh hưởng nhất và lý do |
| --- | --- | --- |
| Giá trị đề xuất và tiêu chí đo | Mọi giá trị trong [ ], NFR/KPI test conditions, rating [1–5], format/platform/bitrate. | Giữ nguyên đề xuất từ Excel; nhóm xác nhận bằng yêu cầu/đo thực nghiệm, không thay scope hoặc thêm UC. |
| Metadata và sơ đồ | Author(s), Date, version, approver, activity links, link các sơ đồ đã duyệt, hosting allocation. | Điền [nhóm điền] sau khi nhóm xác nhận; sơ đồ khái niệm đi kèm chỉ làm điểm xuất phát, không giả định người/ngày/link/vendor. |
| Catalogs và ngưỡng geofence | Stored-code notation, language/UI/content/TTS flags, category values, movement threshold. | Điền fixed MVP catalogs/parameters; không thêm dashboard cấu hình hoặc UC mới. |
| Priority và KPI | MoSCoW mỗi FR, tỷ lệ địa điểm mục tiêu, cách tính playback success và đo thời gian đến Details. | Gán dữ liệu cho 15 FR và ba metric đã soạn; không suy product priority từ độ ưu tiên sửa tài liệu. |
| Data constraints | packageVersion uniqueness/grouping, cardinality/composite keys, cấu trúc manifest và physical audit storage. | Xác nhận chi tiết cho 12 entities/records đã có; không tự sinh entity hoặc dependency mới. |
| Analytics definitions | Field applicability của content_viewed/offline_package_installed, event counting/aggregation cho bốn metrics. | Hoàn thiện hợp đồng của external dependency; không đưa capture/submission vào scope hoặc thêm UC-08/09. |
| Privacy và device behavior | Chuyên gia xác nhận yêu cầu pháp lý trong NFR-07; kiểm nghiệm geofencing/background/TTS trên thiết bị mục tiêu. | Giữ NFR và exceptions ở dạng yêu cầu cần xác minh; không tuyên bố đạt compliance hoặc đảm bảo OS behavior chưa có bằng chứng. |

Kết quả bàn giao: bộ đoạn thêm có thể rà soát/dán theo vị trí, cùng bảng truy vết và nguồn sơ đồ khái niệm. Việc tích hợp vào DOCX và kiểm tra layout sau tích hợp chưa được thực hiện trong yêu cầu soạn đoạn này.
