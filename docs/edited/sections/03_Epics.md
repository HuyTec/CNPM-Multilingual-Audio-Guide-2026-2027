# Mục 3 — Điền sáu Epic hiện có

Giai đoạn 2, Lượt 4. Giữ tên và số Epic hiện tại. Các FR là mã truy vết mới cho tính năng đã có trong nguồn, không phải UC mới. MoSCoW chưa được nhóm chốt nên để [nhóm điền], không suy từ độ ưu tiên sửa tài liệu trong Excel thành độ ưu tiên sản phẩm.

| Vị trí | Kiểu | Văn bản tiếng Anh dán được | Mã liên quan |
| --- | --- | --- | --- |
| 3.1.1 Objective | ĐIỀN MỤC TRỐNG | Help Tourists discover and select attractions through nearby search, keyword search, categories, maps, and location-based notifications, including manual selection when location access is unavailable. | M12, S13, D10 |
| 3.1.2 Features | ĐIỀN MỤC TRỐNG | FR-01: Nearby search, distance-ascending results, shortlist, radius expansion, and pagination (UC-01). FR-02: Keyword search, category browsing, and map-marker selection, with a simplified offline map background (UC-01). FR-03: Geofence notifications or In-App Banners, cooldown, and locationId navigation to Location Details (UC-05). MoSCoW for each FR: [nhóm điền]. | M12, S02, G03, G06 |
| 3.1.3 Related Use Cases | ĐIỀN MỤC TRỐNG | UC-01 — Explore & Select Location; UC-05 — Receive Location-Based Notifications. | M12, quyết định Epic 1 |
| 3.2.1 Objective | ĐIỀN MỤC TRỐNG | Provide verified multilingual guide text and audio for the selected location with language and script-type fallback. | M12, S13 |
| 3.2.2 Features | ĐIỀN MỤC TRỐNG | FR-04: APPROVED content retrieval by locationId, languageCode, and scriptType, with language and FULL/SHORT fallback (UC-02). FR-05: Audio playback controls, background playback, and operating-system TTS fallback when usable audio is unavailable (UC-03). MoSCoW for each FR: [nhóm điền]. | M12, S02, G07, G14 |
| 3.2.3 Related Use Cases | ĐIỀN MỤC TRỐNG | UC-02 — Access Multilingual Guide Content; UC-03 — Play Audio Guide. | M12 |
| 3.3.1 Objective | ĐIỀN MỤC TRỐNG | Keep essential location data and guide content available offline and allow safe installation and management of optional or updated content. | M12, M09 |
| 3.3.2 Features | ĐIỀN MỤC TRỐNG | FR-06: Bundled Base Content, update checks, and optional-package selection and download (UC-04). FR-07: Verified atomic installation of matching location data, scripts, and available audio after playback ends (UC-04). FR-08: Optional-content deletion, resumable interrupted downloads, cancellation, and physical protection of Base Content (UC-04). MoSCoW for each FR: [nhóm điền]. | M12, S02, G16–G21, D03, D07 |
| 3.3.3 Related Use Cases | ĐIỀN MỤC TRỐNG | UC-04 — Manage & Update Offline Content. | M12 |
| 3.4.1 Objective | ĐIỀN MỤC TRỐNG | Enable the Content Admin to maintain location information, multilingual scripts, and corresponding audio before external review and publication. | M12 |
| 3.4.2 Features | ĐIỀN MỤC TRỐNG | FR-09: Location listing, search and filters, localized information, scripts, and audio upload and preview (UC-06). FR-10: Save Draft, Submit for Review, rejected-content editing and resubmission, and approved-version protection (UC-06). FR-11: Deactivation requests, concurrent-edit detection, and content audit history (UC-06). MoSCoW for each FR: [nhóm điền]. | M12, S02, G29–G37 |
| 3.4.3 Related Use Cases | ĐIỀN MỤC TRỐNG | UC-06 — Manage Location Content & Audio Assets. | M12 |
| 3.5.1 Objective | ĐIỀN MỤC TRỐNG | Support service and content improvement through basic usage analytics and management of feedback supplied by external dependencies. | M12 |
| 3.5.2 Features | ĐIỀN MỤC TRỐNG | FR-12: Total Playbacks, Top Locations, Language Distribution, and Offline/Online Usage Ratio by reporting period (UC-07). FR-13: Feedback listing, detail, filters, pagination, and sequential status updates with RESOLVED final (UC-07). FR-14: Period-based .xlsx export, active feedback filters, and retry after errors (UC-07). MoSCoW for each FR: [nhóm điền]. | M12, S02, G41–G45 |
| 3.5.3 Related Use Cases | ĐIỀN MỤC TRỐNG | UC-07 — System Analytics & Tourist Feedback Management. | M12 |
| 3.6.1 Objective | ĐIỀN MỤC TRỐNG | Provide the supporting storage, content distribution, fixed configuration, and external-service integration required by the existing Mobile App and Web Admin use cases. | M12, M14 |
| 3.6.2 Features | ĐIỀN MỤC TRỐNG | FR-15: Fixed MVP configuration, storage and content distribution, and integration with the services listed in Section 6.4. Content approval and publication, Admin authentication and account management, mobile analytics capture, and tourist feedback submission remain external or out of scope as stated in Section 2.2. MoSCoW: [nhóm điền]. | M12, S02, S10, D02 |
| 3.6 — sau Features, thêm mục Related Use Cases | THÊM CÂU | 3.6.3 Related Use Cases: Infrastructure support for UC-01 through UC-07; no additional use case is defined for this epic. | M12, S13 |

Mã FR-01–FR-15 là định danh bổ sung; không đổi tên/số Epic hoặc các UC. Epic 6 mô tả hạ tầng hỗ trợ, không chuyển các dependency ngoài scope thành tính năng người dùng phải triển khai trong PRD.
