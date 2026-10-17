# Bộ bổ sung PRD để rà soát tổng thể

Đã soạn đủ Giai đoạn 2 qua bốn lượt và thực hiện Giai đoạn 3 trên toàn bộ danh sách bổ sung. Bộ này gồm **274 dòng đề xuất**, không phải 274 yêu cầu nghiệp vụ mới. Hai file đầu vào giữ nguyên; chưa dán các đoạn vào DOCX hoặc Excel.

Nguồn duy nhất cho nội dung là hai file đính kèm và các quyết định của người dùng. PRD trong repository không được dùng để thay thế nguồn. M04 đã được điều chỉnh thành Đã áp dụng; **UC-02 E2 không có đoạn thêm**. N01 được giữ vì câu mở Location Details thực sự nằm trong đoạn Otherwise của DOCX.

## Nội dung theo UC

| Lượt | File | Dòng đề xuất |
| --- | --- | ---: |
| 1 | [UC-01](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/uc/UC_01.md) | 17 |
| 1 | [UC-02](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/uc/UC_02.md) | 12 |
| 1 | [UC-03](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/uc/UC_03.md) | 17 |
| 2 | [UC-04](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/uc/UC_04.md) | 16 |
| 2 | [UC-05](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/uc/UC_05.md) | 17 |
| 3 | [UC-06](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/uc/UC_06.md) | 27 |
| 3 | [UC-07](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/uc/UC_07.md) | 16 |

## Nội dung ngoài UC

| File | Dòng đề xuất | Nội dung |
| --- | ---: | --- |
| [Document Control](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/sections/Document_Control.md) | 4 | Bảng kiểm soát tài liệu, chú thích hiệu đính tiêu đề. |
| [Product Goals](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/sections/01_Product_Goals.md) | 4 | KPI đề xuất; không tự đặt số còn thiếu. |
| [Actors](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/sections/02_3_Actors.md) | 11 | Ba actor chính và dependency roles/aliases. |
| [Epics](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/sections/03_Epics.md) | 18 | Sáu Epic giữ số/tên cũ; 15 FR để truy vết, MoSCoW chờ nhóm. |
| [Data Models](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/sections/05_Data_Models.md) | 16 | Mô hình dữ liệu, 12 entity/record definitions và các giả định chưa chốt. |
| [System Architecture](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/sections/06_System_Architecture.md) | 11 | Mô tả sơ đồ, hai dependency mới và làm rõ lifecycle. |
| [NFR](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/sections/07_NFR.md) | 15 | Đoạn dẫn và 14 NFR đề xuất. |
| [Appendix](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/sections/08_Appendix.md) | 73 | Glossary, cấu hình, catalogs, schemas, quyền, MVP, risks, errors, screens. |

## Sơ đồ khái niệm đi kèm

Các file Mermaid dưới đây là bản dựng từ nguồn để nhóm xem xét, không phải link sơ đồ đã duyệt. Link dán vào PRD vẫn là [nhóm điền] theo D12. Chưa render hoặc xác nhận visual QA; không chứa công nghệ/vendor, cardinality hoặc method signatures ngoài nguồn.

- [ERD ở mức tham chiếu entity](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/diagrams/05_1_ERD.mmd): không tự gán cardinality.
- [Class Diagram đề xuất](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/diagrams/06_1_Class.mmd).
- [Component Diagram đề xuất](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/diagrams/06_2_Component.mmd).
- [Deployment boundaries đề xuất](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/diagrams/06_3_Deployment.mmd).

## Cách rà soát và áp dụng

1. Đọc [báo cáo Giai đoạn 3](/E:/Project/CongNghePhanMem_2026-2027/docs/edited/Giai_Doan_3_Kiem_Tra.md) và danh sách nhóm cần hoàn thiện ở cuối báo cáo.
2. Rà soát các UC và mục ngoài UC theo thứ tự trong các bảng trên. Mỗi dòng ghi vị trí, kiểu thay đổi, văn bản tiếng Anh và mã nguồn Excel/quyết định.
3. Khi áp dụng, chỉ thêm vào vị trí chỉ định; giữ câu cũ, các bảng UC ba cột và mọi tham chiếu cũ. Các bảng mới như Document Control/Entity Definitions/Appendix không thay cấu trúc bảng UC.
4. Áp dụng đồng bộ các reference mới: UC-03 A4 với UC-04 BR-06; UC-05 BR5 với UC-06 validation; UC-06 A3 với owner ARCHIVED tại 6.4.1; fallback/version giữa UC-02 và UC-03.
5. Sau khi nhóm dán vào DOCX, kiểm tra lại nội dung và render toàn bộ trang. Kiểm tra trên bộ đề xuất hiện tại không chứng minh bố cục của DOCX sau tích hợp.

Lưu ý: trong UC-01 có một dòng chỉ định cùng lời làm rõ tại bước 6 và A1; đây là một đề xuất với hai vị trí dán. Không chép lại flow hoặc BR đã ổn. Không thực hiện thao tác remove/replace/move được nêu trong Excel nếu nó trái R1–R6.
