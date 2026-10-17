# Mục 2.3 — Actors

Giai đoạn 2, Lượt 4. Điền mục 2.3 trống bằng đoạn dẫn và bảng Actor / Type / Description / Related UC. Không thêm actor nghiệp vụ chính ngoài ba actor hiện có. Content Approver được diễn giải là vai trò bên ngoài của publication dependency, không tạo UC mới.

| Vị trí | Kiểu | Văn bản tiếng Anh dán được | Mã liên quan |
| --- | --- | --- | --- |
| 2.3 — đoạn dẫn | ĐIỀN MỤC TRỐNG | The system has three primary actors. Platform and service dependencies support their interactions as listed below; their responsibilities are detailed in Section 6.4. | M11, S13 |
| 2.3 — Actor Tourist | THÊM DÒNG | Actor: Tourist. Type: Primary. Description: Uses the Mobile App to discover and select locations, access multilingual guide content, play audio, manage offline content, and receive location-based notifications. Related UC: UC-01, UC-02, UC-03, UC-04, UC-05. | M11, sheet 3 |
| 2.3 — Actor Content Admin (Editor) | THÊM DÒNG | Actor: Content Admin (Editor). Type: Primary. Description: Creates and edits location information, multilingual scripts, and audio assets; saves drafts, submits content for review, and requests location deactivation. Related UC: UC-06. | M11, G29, D02 |
| 2.3 — Actor System Admin / Product Owner | THÊM DÒNG | Actor: System Admin / Product Owner. Type: Primary. Description: Views system analytics, manages feedback status, and exports reports through the Web Admin Dashboard. Related UC: UC-07. | M11, G44, D09 |
| 2.3 — Map Service | THÊM DÒNG | Actor or dependency: Map Service. Type: Secondary service. Description: Displays the interactive map and markers; offline Map View uses a simplified background without map tiles. Related UC: UC-01. | sheet 3, G06, D06 |
| 2.3 — OS Location & Geofencing | THÊM DÒNG | Actor or dependency: OS Location & Geofencing. Type: Platform dependency. Description: Provides device coordinates and accuracy and supports geofence registration and entry events. Related UC: UC-01, UC-05. | sheet 3, sheet 7 |
| 2.3 — OS Notification | THÊM DÒNG | Actor or dependency: OS Notification. Type: Platform dependency. Description: Displays system notifications and opens the Mobile App using locationId. Related UC: UC-05. | sheet 7 |
| 2.3 — OS Audio & Text-to-Speech | THÊM DÒNG | Actor or dependency: OS Audio & Text-to-Speech. Type: Platform dependency. Description: Provides audio playback, audio-focus handling, and device TTS fallback. Related UC: UC-03. | sheet 3, sheet 7 |
| 2.3 — Content Server / CDN | THÊM DÒNG | Actor or dependency: Content Server / CDN. Type: Infrastructure dependency. Description: Supplies APPROVED guide content, audio assets, and content packages. Related UC: UC-02, UC-03, UC-04. | sheet 3, sheet 7 |
| 2.3 — Content Publication Service | THÊM DÒNG | Actor or dependency: Content Publication Service. Type: External dependency. Description: Performs review and publication, assigns package versions, and executes requested ARCHIVED transitions. Content Approver is the external review role within this dependency, not an additional in-scope primary actor or use case. Related UC: UC-04, UC-06. | sheet 3, M06, G29, D02 |
| 2.3 — đoạn alias cuối bảng | THÊM CÂU | Naming aliases: Content Admin / Editor, Content Administrator, and Administrators/Content Editors refer to Content Admin (Editor). System Administrators/Product Owners refers to System Admin / Product Owner. “Admin” in UC-06 means Content Admin (Editor); “Admin” or “Administrator” in UC-07 means System Admin / Product Owner. | M11, M13, D13 |

Admin Authentication, Analytics Event Capture và Feedback Submission là dependency contracts của mục 6.4, không tạo vai trò primary mới. Ma trận quyền chỉ dùng actor hiện có, được bổ sung tại Appendix.
