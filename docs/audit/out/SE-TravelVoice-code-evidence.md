# TravelVoice · Bằng chứng code theo ô sơ đồ

**36 trang có nhãn, 274 ô có tham chiếu code và 462 lượt tham chiếu. 6 ô được tô vàng để kiểm tra thêm.**

Đối chiếu tĩnh code tại `6e1faf8`. Mỗi nhãn trên sơ đồ hiển thị tên hàm hoặc thành phần cùng `tệp:dòng`. **Edit Data → code_ref** lưu đường dẫn đầy đủ. Có thể bật/tắt lớp **Bằng chứng code** trên từng trang.

ERD dẫn đến DDL và lớp truy vấn. Các trang triển khai dẫn đến cấu hình hoặc entry point. Nền Activity, UC và kiến trúc lấy từ tệp đính kèm `SE.drawio (6).xml`; DFD giữ các hiệu chỉnh từ bản làm việc còn lưu.

## UC1

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 1.1. Tìm theo địa điểm lân cận | `useBrowserLocation()` · [web-user/src/lib/useBrowserLocation.ts:6](../../../web-user/src/lib/useBrowserLocation.ts#L6)<br>`NearbyPlaces()` · [web-user/src/components/NearbyPlaces.tsx:10](../../../web-user/src/components/NearbyPlaces.tsx#L10) |  |
| 1.2. Tìm theo tìm kiếm | `LocationSearch()` · [web-user/src/components/LocationSearch.tsx:5](../../../web-user/src/components/LocationSearch.tsx#L5)<br>`LocationDiscoveryService.search()` · [backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java:31](../../../backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java#L31) |  |
| UC-01 Explore & Select Location | `LocationEntry()` · [web-user/src/pages/LocationEntry.tsx:7](../../../web-user/src/pages/LocationEntry.tsx#L7)<br>`LocationCatalog()` · [web-user/src/components/LocationCatalog.tsx:11](../../../web-user/src/components/LocationCatalog.tsx#L11) |  |
| 1.3. Tìm theo danh mục | `CategoryFilter()` · [web-user/src/components/CategoryFilter.tsx:9](../../../web-user/src/components/CategoryFilter.tsx#L9)<br>`LocationDiscoveryService.categories()` · [backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java:60](../../../backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java#L60) |  |
| 1.4. Tìm theo quét mã QR hay link địa điểm | `LocationEntry.openLocation()` · [web-user/src/pages/LocationEntry.tsx:11](../../../web-user/src/pages/LocationEntry.tsx#L11)<br>`GuidePage()` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48) | Ứng dụng xử lý liên kết và điều hướng sau khi quét. Chưa xác định bộ quét QR nội bộ trong Tourist Web. |

## UC1.1_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Tìm địa điểm gần đây | `TourMap.startNearby` · [web-user/src/components/TourMap.tsx:104](../../../web-user/src/components/TourMap.tsx#L104) |  |
| Yêu cầu và nhận vị trí trình duyệt | `useBrowserLocation.acquire` · [web-user/src/lib/useBrowserLocation.ts:13](../../../web-user/src/lib/useBrowserLocation.ts#L13) |  |
| Có vị trí dùng được? | `useBrowserLocation.acquire` · [web-user/src/lib/useBrowserLocation.ts:41](../../../web-user/src/lib/useBrowserLocation.ts#L41) |  |
| E1 · Chưa lấy được vị trí. Hướng dẫn thử lại hoặc tìm thủ công. | `NearbyPlaces` · [web-user/src/components/NearbyPlaces.tsx:159](../../../web-user/src/components/NearbyPlaces.tsx#L159) |  |
| Tính khoảng cách, lọc theo bán kính và xếp từ gần đến xa. Mặc định 100 m; phạm vi 100–500 m. | `NearbyPlaces` · [web-user/src/components/NearbyPlaces.tsx:51](../../../web-user/src/components/NearbyPlaces.tsx#L51) |  |
| Có điểm trong bán kính? | `NearbyPlaces` · [web-user/src/components/NearbyPlaces.tsx:51](../../../web-user/src/components/NearbyPlaces.tsx#L51) |  |
| Stop? | `TourMap.closeNearby` · [web-user/src/components/TourMap.tsx:117](../../../web-user/src/components/TourMap.tsx#L117) |  |
| Dừng thao tác tìm gần | `TourMap.closeNearby` · [web-user/src/components/TourMap.tsx:117](../../../web-user/src/components/TourMap.tsx#L117) |  |
| E2 · Không có điểm. Điều chỉnh bán kính hoặc chọn cách khác. | `NearbyPlaces` · [web-user/src/components/NearbyPlaces.tsx:191](../../../web-user/src/components/NearbyPlaces.tsx#L191) |  |
| Hiện tối đa 5 điểm gần nhất kèm khoảng cách; cho xem thêm nếu còn điểm. | `NearbyPlaces` · [web-user/src/components/NearbyPlaces.tsx:217](../../../web-user/src/components/NearbyPlaces.tsx#L217) |  |
| Du khách chọn kết quả tìm gần | `NearbyPlaces` · [web-user/src/components/NearbyPlaces.tsx:216](../../../web-user/src/components/NearbyPlaces.tsx#L216)<br>`LocationResults` · [web-user/src/components/LocationResults.tsx:28](../../../web-user/src/components/LocationResults.tsx#L28) |  |
| Mở chi tiết theo mã địa điểm | `GuidePage` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48) |  |

## UC1.2_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| A1 · Nhập từ khóa | `LocationSearch` · [web-user/src/components/LocationSearch.tsx:29](../../../web-user/src/components/LocationSearch.tsx#L29) |  |
| Lọc tên trong danh sách địa điểm hiện tại | `LocationSearch` · [web-user/src/components/LocationSearch.tsx:18](../../../web-user/src/components/LocationSearch.tsx#L18) |  |
| Có tên phù hợp? | `LocationSearch` · [web-user/src/components/LocationSearch.tsx:18](../../../web-user/src/components/LocationSearch.tsx#L18) |  |
| E3 · Không có tên phù hợp. Thử tên khác hoặc thử cách khác | `LocationSearch` · [web-user/src/components/LocationSearch.tsx:57](../../../web-user/src/components/LocationSearch.tsx#L57) |  |
| Stop? | `LocationSearch` · [web-user/src/components/LocationSearch.tsx:43](../../../web-user/src/components/LocationSearch.tsx#L43) |  |
| Hiện gợi ý; du khách chọn một điểm | `LocationSearch.choose` · [web-user/src/components/LocationSearch.tsx:22](../../../web-user/src/components/LocationSearch.tsx#L22) |  |
| Dừng thao tác tìm từ khóa | `LocationSearch` · [web-user/src/components/LocationSearch.tsx:47](../../../web-user/src/components/LocationSearch.tsx#L47) |  |
| Hiển thị khung thông tin địa điểm | `popupFor` · [web-user/src/components/TourMap.tsx:16](../../../web-user/src/components/TourMap.tsx#L16)<br>`TourMap.selectPlace` · [web-user/src/components/TourMap.tsx:136](../../../web-user/src/components/TourMap.tsx#L136) |  |
| Du khách bấm Xem thuyết minh | `popupFor` · [web-user/src/components/TourMap.tsx:16](../../../web-user/src/components/TourMap.tsx#L16) |  |
| Mở chi tiết theo mã địa điểm | `GuidePage` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48) |  |

## UC1.3_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Lưu ý nơi có thuyết minh mới có ghim để chọn | `TourMap` · [web-user/src/components/TourMap.tsx:123](../../../web-user/src/components/TourMap.tsx#L123) | Code lọc điểm theo tọa độ hợp lệ trong danh sách đã tải. Chưa thấy kiểm tra bài thuyết minh riêng khi tạo ghim. |
| A2 · Xem bản đồ | `TourMap` · [web-user/src/components/TourMap.tsx:43](../../../web-user/src/components/TourMap.tsx#L43) |  |
| Hiển thị bản đồ và ghim địa điểm | `attachMapTiles` · [web-user/src/lib/mapTiles.ts:13](../../../web-user/src/lib/mapTiles.ts#L13)<br>`TourMap` · [web-user/src/components/TourMap.tsx:210](../../../web-user/src/components/TourMap.tsx#L210) |  |
| Du khách chọn ghim | `TourMap` · [web-user/src/components/TourMap.tsx:234](../../../web-user/src/components/TourMap.tsx#L234) |  |
| Hiển thị khung thông tin địa điểm | `popupFor` · [web-user/src/components/TourMap.tsx:16](../../../web-user/src/components/TourMap.tsx#L16) |  |
| Du khách bấm Xem thuyết minh | `popupFor` · [web-user/src/components/TourMap.tsx:16](../../../web-user/src/components/TourMap.tsx#L16) |  |
| Mở chi tiết theo mã địa điểm | `GuidePage` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48) |  |

## UC1.4_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| A4 · Mở liên kết hoặc quét mã QR | `LocationEntry.openLocation` · [web-user/src/pages/LocationEntry.tsx:11](../../../web-user/src/pages/LocationEntry.tsx#L11) | Ứng dụng xử lý liên kết. Quét và mở QR do thiết bị thực hiện. |
| Kiểm tra đường dẫn và mã địa điểm | `LocationEntry.openLocation` · [web-user/src/pages/LocationEntry.tsx:11](../../../web-user/src/pages/LocationEntry.tsx#L11)<br>`GuidePage` · [web-user/src/pages/GuidePage.tsx:64](../../../web-user/src/pages/GuidePage.tsx#L64) |  |
| Đường dẫn hợp lệ? | `LocationEntry.openLocation` · [web-user/src/pages/LocationEntry.tsx:11](../../../web-user/src/pages/LocationEntry.tsx#L11)<br>`GuidePage` · [web-user/src/pages/GuidePage.tsx:64](../../../web-user/src/pages/GuidePage.tsx#L64) |  |
| Báo lỗi liên kết và hướng dẫn mở lại hoặc về Khám phá | `LocationEntry.openLocation` · [web-user/src/pages/LocationEntry.tsx:20](../../../web-user/src/pages/LocationEntry.tsx#L20)<br>`GuidePage` · [web-user/src/pages/GuidePage.tsx:64](../../../web-user/src/pages/GuidePage.tsx#L64) |  |
| Mở chi tiết theo mã địa điểm | `LocationEntry.openLocation` · [web-user/src/pages/LocationEntry.tsx:11](../../../web-user/src/pages/LocationEntry.tsx#L11)<br>`GuidePage` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48) |  |

## UC2

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| UC-02 · Truy cập nội dung thuyết minh | `GuidePage()` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48)<br>`PlaybackService.guide()` · [backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java#L33) |  |

## UC2_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Nhận mã địa điểm, ngôn ngữ và loại bài. Mặc định VI và FULL khi thiếu. | `GuidePage` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48) |  |
| Tra bài đã duyệt theo mã địa điểm, ngôn ngữ và loại bài | `useGuide.load` · [web-user/src/lib/useGuide.ts:18](../../../web-user/src/lib/useGuide.ts#L18)<br>`PlaybackService.guide` · [backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java#L33)<br>`GuideRepository.find` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| Kết quả truy xuất? | `useGuide.load` · [web-user/src/lib/useGuide.ts:18](../../../web-user/src/lib/useGuide.ts#L18) |  |
| Thử đọc bài từ gói ngoại tuyến đã xác minh | `useGuide.load` · [web-user/src/lib/useGuide.ts:18](../../../web-user/src/lib/useGuide.ts#L18)<br>`offlineGuide` · [web-user/src/lib/packages.ts:186](../../../web-user/src/lib/packages.ts#L186) |  |
| E1 · Nếu thiếu ngôn ngữ yêu cầu, dùng EN rồi VI, giữ nguyên loại bài | `GuideRepository.find` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33)<br>`offlineGuide` · [web-user/src/lib/packages.ts:186](../../../web-user/src/lib/packages.ts#L186) |  |
| E2 · Báo bài đang cập nhật; Phát không khả dụng | `GuideReader` · [web-user/src/pages/GuidePage.tsx:18](../../../web-user/src/pages/GuidePage.tsx#L18) |  |
| Có bài cục bộ hợp lệ? | `offlineGuide` · [web-user/src/lib/packages.ts:186](../../../web-user/src/lib/packages.ts#L186) |  |
| Báo lỗi tải và cho thử lại | `GuideReader` · [web-user/src/pages/GuidePage.tsx:24](../../../web-user/src/pages/GuidePage.tsx#L24) |  |
| Hiện bài và ngôn ngữ thực tế. Báo khi dùng ngôn ngữ dự phòng. | `GuideReader` · [web-user/src/pages/GuidePage.tsx:29](../../../web-user/src/pages/GuidePage.tsx#L29) |  |
| A2 · Đổi FULL hoặc SHORT, giữ ngôn ngữ yêu cầu | `GuidePage.change` · [web-user/src/pages/GuidePage.tsx:70](../../../web-user/src/pages/GuidePage.tsx#L70) |  |
| A1 · Đổi ngôn ngữ, giữ loại bài | `GuidePage.change` · [web-user/src/pages/GuidePage.tsx:70](../../../web-user/src/pages/GuidePage.tsx#L70) |  |
| Du khách chọn thao tác nào? | `GuidePage.change` · [web-user/src/pages/GuidePage.tsx:70](../../../web-user/src/pages/GuidePage.tsx#L70) |  |

## UC3

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| UC-03 · Nghe thuyết minh | `GuidePlayer()` · [web-user/src/components/GuidePlayer.tsx:7](../../../web-user/src/components/GuidePlayer.tsx#L7)<br>`GuidePlayback.play()` · [web-user/src/lib/playback.ts:77](../../../web-user/src/lib/playback.ts#L77) |  |

## UC3_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Du khách bấm Phát | `GuidePlayer` · [web-user/src/components/GuidePlayer.tsx:71](../../../web-user/src/components/GuidePlayer.tsx#L71)<br>`GuidePlayback.play` · [web-user/src/lib/playback.ts:77](../../../web-user/src/lib/playback.ts#L77) |  |
| Có tệp âm thanh của bài? | `GuidePlayback.play` · [web-user/src/lib/playback.ts:77](../../../web-user/src/lib/playback.ts#L77) |  |
| E1 · Thử giọng đọc theo ngôn ngữ thực tế của bài | `GuidePlayback.startTts` · [web-user/src/lib/playback.ts:109](../../../web-user/src/lib/playback.ts#L109) |  |
| Nạp và thử phát tệp âm thanh | `GuidePlayback.play` · [web-user/src/lib/playback.ts:77](../../../web-user/src/lib/playback.ts#L77) |  |
| Tệp phát được? | `GuidePlayback.play` · [web-user/src/lib/playback.ts:77](../../../web-user/src/lib/playback.ts#L77)<br>`GuidePlayback.armTimeout` · [web-user/src/lib/playback.ts:102](../../../web-user/src/lib/playback.ts#L102) |  |
| Giọng phù hợp và phát được? | `GuidePlayback.startTts` · [web-user/src/lib/playback.ts:109](../../../web-user/src/lib/playback.ts#L109)<br>`GuidePlayback.speak` · [web-user/src/lib/playback.ts:128](../../../web-user/src/lib/playback.ts#L128) |  |
| Báo lỗi phát, giữ văn bản để đọc | `GuidePlayback.fail` · [web-user/src/lib/playback.ts:184](../../../web-user/src/lib/playback.ts#L184)<br>`GuidePlayer` · [web-user/src/components/GuidePlayer.tsx:51](../../../web-user/src/components/GuidePlayer.tsx#L51) |  |
| Hiển thị bộ điều khiển và trạng thái phát | `GuidePlayer` · [web-user/src/components/GuidePlayer.tsx:22](../../../web-user/src/components/GuidePlayer.tsx#L22) |  |
| Điều gì xảy ra tiếp theo? | `GuidePlayer` · [web-user/src/components/GuidePlayer.tsx:71](../../../web-user/src/components/GuidePlayer.tsx#L71) |  |
| A1 · Tạm dừng, tiếp tục, tua hoặc đổi tốc độ | `GuidePlayback.pause` · [web-user/src/lib/playback.ts:174](../../../web-user/src/lib/playback.ts#L174)<br>`GuidePlayback.seek` · [web-user/src/lib/playback.ts:187](../../../web-user/src/lib/playback.ts#L187)<br>`GuidePlayback.setRate` · [web-user/src/lib/playback.ts:200](../../../web-user/src/lib/playback.ts#L200) |  |
| Phát xong hoặc du khách rời trình phát | `GuidePlayer` · [web-user/src/components/GuidePlayer.tsx:29](../../../web-user/src/components/GuidePlayer.tsx#L29)<br>`GuidePlayback.stop` · [web-user/src/lib/playback.ts:180](../../../web-user/src/lib/playback.ts#L180) |  |

## UC4

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| UC-04 · Quản lý nội dung ngoại tuyến | `OfflinePage.check()` · [web-user/src/pages/OfflinePage.tsx:61](../../../web-user/src/pages/OfflinePage.tsx#L61)<br>`OfflinePage.install()` · [web-user/src/pages/OfflinePage.tsx:73](../../../web-user/src/pages/OfflinePage.tsx#L73) |  |

## UC4_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Du khách mở Nội dung ngoại tuyến | `OfflinePage` · [web-user/src/pages/OfflinePage.tsx:8](../../../web-user/src/pages/OfflinePage.tsx#L8) |  |
| Hiển thị gói đã cài; kiểm tra phiên bản khi có mạng | `OfflinePage.refresh` · [web-user/src/pages/OfflinePage.tsx:24](../../../web-user/src/pages/OfflinePage.tsx#L24)<br>`OfflinePage.check` · [web-user/src/pages/OfflinePage.tsx:61](../../../web-user/src/pages/OfflinePage.tsx#L61)<br>`checkPackageUpdates` · [web-user/src/lib/packageUpdates.ts:13](../../../web-user/src/lib/packageUpdates.ts#L13) |  |
| Chưa kiểm tra được phiên bản. Giữ gói đã cài nếu có và cho thử lại. | `OfflinePage.check` · [web-user/src/pages/OfflinePage.tsx:70](../../../web-user/src/pages/OfflinePage.tsx#L70) |  |
| Kết quả kiểm tra phiên bản? | `OfflinePage.check` · [web-user/src/pages/OfflinePage.tsx:61](../../../web-user/src/pages/OfflinePage.tsx#L61) |  |
| A1 · Gói đã cài khớp phiên bản đang công bố | `OfflinePage.check` · [web-user/src/pages/OfflinePage.tsx:66](../../../web-user/src/pages/OfflinePage.tsx#L66) |  |
| Hiển thị thông tin gói và dung lượng cần thiết | `OfflinePage` · [web-user/src/pages/OfflinePage.tsx:122](../../../web-user/src/pages/OfflinePage.tsx#L122)<br>`packageSize` · [web-user/src/lib/packages.ts:15](../../../web-user/src/lib/packages.ts#L15) |  |
| Du khách xác nhận tải hoặc cập nhật? | `OfflinePage` · [web-user/src/pages/OfflinePage.tsx:144](../../../web-user/src/pages/OfflinePage.tsx#L144) |  |
| A3 · Hủy, giữ gói đã cài nếu có | `OfflinePage.install` · [web-user/src/pages/OfflinePage.tsx:91](../../../web-user/src/pages/OfflinePage.tsx#L91) |  |
| Có đủ dung lượng? | `downloadPackage` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| E1 · Báo thiếu dung lượng, giữ gói đã cài | `downloadPackage` · [web-user/src/lib/packages.ts:132](../../../web-user/src/lib/packages.ts#L132) |  |
| Tải dữ liệu gói và từng tệp âm thanh; hiển thị tiến độ | `OfflinePage.install` · [web-user/src/pages/OfflinePage.tsx:73](../../../web-user/src/pages/OfflinePage.tsx#L73)<br>`fetchPackageBytes` · [web-user/src/lib/packages.ts:92](../../../web-user/src/lib/packages.ts#L92) |  |
| E2 · Mất mạng khi tải. A3 · Du khách hủy tải. Giữ gói đã cài. | `OfflinePage.network` · [web-user/src/pages/OfflinePage.tsx:42](../../../web-user/src/pages/OfflinePage.tsx#L42)<br>`OfflinePage.install` · [web-user/src/pages/OfflinePage.tsx:87](../../../web-user/src/pages/OfflinePage.tsx#L87) |  |
| Tải hoàn tất? | `OfflinePage.install` · [web-user/src/pages/OfflinePage.tsx:80](../../../web-user/src/pages/OfflinePage.tsx#L80) |  |
| Xác minh gói đã duyệt, chữ ký và mã toàn vẹn của gói và âm thanh | `verifyPackage` · [web-user/src/lib/packages.ts:47](../../../web-user/src/lib/packages.ts#L47)<br>`downloadPackage` · [web-user/src/lib/packages.ts:141](../../../web-user/src/lib/packages.ts#L141) |  |
| Qua xác minh? | `verifyPackage` · [web-user/src/lib/packages.ts:47](../../../web-user/src/lib/packages.ts#L47) |  |
| E3 · Xác minh hoặc cài thất bại; giữ gói đã cài nếu có | `OfflinePage.install` · [web-user/src/pages/OfflinePage.tsx:87](../../../web-user/src/pages/OfflinePage.tsx#L87)<br>`downloadPackage` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| Cài nguyên khối vào bộ nhớ trình duyệt | `downloadPackage` · [web-user/src/lib/packages.ts:157](../../../web-user/src/lib/packages.ts#L157) |  |
| Ghi gói thành công? | `downloadPackage` · [web-user/src/lib/packages.ts:167](../../../web-user/src/lib/packages.ts#L167) |  |
| Giao diện mở lại ngoại tuyến đã sẵn sàng? | `OfflinePage.control` · [web-user/src/pages/OfflinePage.tsx:34](../../../web-user/src/pages/OfflinePage.tsx#L34) |  |
| Đã cài gói; hướng dẫn chuẩn bị giao diện để mở lại ngoại tuyến | `OfflinePage` · [web-user/src/pages/OfflinePage.tsx:136](../../../web-user/src/pages/OfflinePage.tsx#L136) |  |
| Hiển thị Sẵn sàng ngoại tuyến | `OfflinePage` · [web-user/src/pages/OfflinePage.tsx:136](../../../web-user/src/pages/OfflinePage.tsx#L136) |  |

## UC5

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| UC-05 · Nhận gợi ý theo vị trí | `useGeofencing()` · [web-user/src/lib/useGeofencing.ts:5](../../../web-user/src/lib/useGeofencing.ts#L5)<br>`reserveNotification()` · [web-user/src/lib/geofence.ts:63](../../../web-user/src/lib/geofence.ts#L63) |  |

## UC5_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Du khách bật gợi ý theo vị trí | `GeofenceNotices` · [web-user/src/components/GeofenceNotices.tsx:19](../../../web-user/src/components/GeofenceNotices.tsx#L19) |  |
| Có gói khảo sát đã xác minh và quyền vị trí? | `useGeofencing.start` · [web-user/src/lib/useGeofencing.ts:29](../../../web-user/src/lib/useGeofencing.ts#L29)<br>`localGeofences` · [web-user/src/lib/geofence.ts:22](../../../web-user/src/lib/geofence.ts#L22) |  |
| Chưa đủ điều kiện: kiểm tra gói, trình duyệt hoặc quyền vị trí (E1) | `useGeofencing.start` · [web-user/src/lib/useGeofencing.ts:29](../../../web-user/src/lib/useGeofencing.ts#L29) |  |
| Theo dõi vị trí trong phiên mở | `useGeofencing.start` · [web-user/src/lib/useGeofencing.ts:44](../../../web-user/src/lib/useGeofencing.ts#L44) |  |
| Phát hiện vào vùng? | `geofenceEntries` · [web-user/src/lib/geofence.ts:37](../../../web-user/src/lib/geofence.ts#L37) |  |
| E1 · Quyền vị trí bị thu hồi; vẫn khám phá thủ công | `useGeofencing.revoke` · [web-user/src/lib/useGeofencing.ts:24](../../../web-user/src/lib/useGeofencing.ts#L24) |  |
| A3 · Chưa hết giãn cách, tiếp tục theo dõi | `reserveNotification` · [web-user/src/lib/geofence.ts:63](../../../web-user/src/lib/geofence.ts#L63)<br>`useGeofencing.start` · [web-user/src/lib/useGeofencing.ts:44](../../../web-user/src/lib/useGeofencing.ts#L44) |  |
| Đã hết giãn cách của địa điểm? | `reserveNotification` · [web-user/src/lib/geofence.ts:63](../../../web-user/src/lib/geofence.ts#L63) |  |
| A1 · Hiển thị banner theo ngôn ngữ hiện tại, kèm mã địa điểm | `geofenceMessage` · [web-user/src/lib/geofence.ts:85](../../../web-user/src/lib/geofence.ts#L85)<br>`GeofenceNotices` · [web-user/src/components/GeofenceNotices.tsx:26](../../../web-user/src/components/GeofenceNotices.tsx#L26) |  |
| Du khách chọn banner? | `GeofenceNotices` · [web-user/src/components/GeofenceNotices.tsx:28](../../../web-user/src/components/GeofenceNotices.tsx#L28) |  |
| Mở chi tiết theo mã địa điểm. UC-02 tiếp tục. | `GeofenceNotices` · [web-user/src/components/GeofenceNotices.tsx:28](../../../web-user/src/components/GeofenceNotices.tsx#L28)<br>`GuidePage` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48) |  |
| A2 · Bỏ qua banner, tiếp tục khám phá | `GeofenceNotices` · [web-user/src/components/GeofenceNotices.tsx:29](../../../web-user/src/components/GeofenceNotices.tsx#L29) |  |

## UC6

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| UC-06 · Quản lý nội dung địa điểm | `LocationForm.save()` · [web-admin/src/pages/LocationForm.tsx:416](../../../web-admin/src/pages/LocationForm.tsx#L416)<br>`AdminContentService.save()` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:47](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L47) |  |

## UC6_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Chọn tạo mới hoặc sửa địa điểm | `LocationList` · [web-admin/src/pages/LocationList.tsx:24](../../../web-admin/src/pages/LocationList.tsx#L24)<br>`LocationForm` · [web-admin/src/pages/LocationForm.tsx:47](../../../web-admin/src/pages/LocationForm.tsx#L47) |  |
| A2 · Nạp bản làm việc hiện có, kể cả bản bị trả về | `LocationForm` · [web-admin/src/pages/LocationForm.tsx:95](../../../web-admin/src/pages/LocationForm.tsx#L95)<br>`AdminContentService.detail` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:34](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L34) |  |
| Mở biểu mẫu trống | `blankLocation` · [web-admin/src/pages/LocationForm.tsx:32](../../../web-admin/src/pages/LocationForm.tsx#L32) |  |
| Nhập địa điểm và bài theo ngôn ngữ, loại FULL hoặc SHORT | `LocationForm.change` · [web-admin/src/pages/LocationForm.tsx:178](../../../web-admin/src/pages/LocationForm.tsx#L178) |  |
| Có tải lên hoặc thay âm thanh? | `LocationForm.upload` · [web-admin/src/pages/LocationForm.tsx:806](../../../web-admin/src/pages/LocationForm.tsx#L806) |  |
| Chọn tệp và tải theo ngôn ngữ, loại bài | `LocationForm.upload` · [web-admin/src/pages/LocationForm.tsx:339](../../../web-admin/src/pages/LocationForm.tsx#L339)<br>`AdminContentService.upload` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:92](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L92) |  |
| Tải âm thanh thành công? | `LocationForm.upload.finish` · [web-admin/src/pages/LocationForm.tsx:382](../../../web-admin/src/pages/LocationForm.tsx#L382) |  |
| E2 · Đánh dấu trường lỗi và giữ biểu mẫu | `LocationForm.validate` · [web-admin/src/pages/LocationForm.tsx:307](../../../web-admin/src/pages/LocationForm.tsx#L307) |  |
| E1 · Báo lỗi, giữ dữ liệu, cho chọn tệp hoặc thử lại | `LocationForm.upload.finish` · [web-admin/src/pages/LocationForm.tsx:392](../../../web-admin/src/pages/LocationForm.tsx#L392) |  |
| Nghe thử nếu có âm thanh; kiểm tra biểu mẫu | `LocationForm.validate` · [web-admin/src/pages/LocationForm.tsx:307](../../../web-admin/src/pages/LocationForm.tsx#L307)<br>`LocationForm` · [web-admin/src/pages/LocationForm.tsx:872](../../../web-admin/src/pages/LocationForm.tsx#L872) |  |
| Chọn cách lưu | `LocationForm.requestSave` · [web-admin/src/pages/LocationForm.tsx:447](../../../web-admin/src/pages/LocationForm.tsx#L447) |  |
| A1 · Lưu DRAFT, cho phép thiếu trường bắt buộc | `LocationForm.save` · [web-admin/src/pages/LocationForm.tsx:416](../../../web-admin/src/pages/LocationForm.tsx#L416)<br>`AdminContentService.save` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:47](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L47) |  |
| Đủ dữ liệu bắt buộc? | `LocationForm.validate` · [web-admin/src/pages/LocationForm.tsx:307](../../../web-admin/src/pages/LocationForm.tsx#L307) |  |
| Lưu thất bại · Báo lỗi và giữ biểu mẫu | `LocationForm.save` · [web-admin/src/pages/LocationForm.tsx:437](../../../web-admin/src/pages/LocationForm.tsx#L437) |  |
| Lưu PENDING_REVIEW, giữ bản công khai hiện hành | `LocationForm.save` · [web-admin/src/pages/LocationForm.tsx:416](../../../web-admin/src/pages/LocationForm.tsx#L416)<br>`AdminContentService.save` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:47](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L47) |  |

## UC7

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| UC-07 · Phân tích và quản lý phản hồi | `LiveAnalytics()` · [web-admin/src/pages/LiveAnalytics.tsx:17](../../../web-admin/src/pages/LiveAnalytics.tsx#L17)<br>`LiveFeedback()` · [web-admin/src/components/LiveFeedback.tsx:19](../../../web-admin/src/components/LiveFeedback.tsx#L19) |  |

## UC7_activity

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Chưa có phiên hoặc phiên hết hạn · Hiện đăng nhập | `LiveAnalytics` · [web-admin/src/pages/LiveAnalytics.tsx:106](../../../web-admin/src/pages/LiveAnalytics.tsx#L106)<br>`useLiveAnalytics.refresh` · [web-admin/src/lib/live-analytics.ts:35](../../../web-admin/src/lib/live-analytics.ts#L35) |  |
| Mở Phân tích và phản hồi | `LiveAnalytics` · [web-admin/src/pages/LiveAnalytics.tsx:17](../../../web-admin/src/pages/LiveAnalytics.tsx#L17) |  |
| Tải bốn chỉ số theo kỳ; mặc định hôm nay và 29 ngày trước | `useLiveAnalytics.refresh` · [web-admin/src/lib/live-analytics.ts:35](../../../web-admin/src/lib/live-analytics.ts#L35)<br>`PlaybackService.analytics` · [backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java:75](../../../backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java#L75) |  |
| Có dữ liệu phù hợp? | `LiveAnalytics` · [web-admin/src/pages/LiveAnalytics.tsx:25](../../../web-admin/src/pages/LiveAnalytics.tsx#L25) |  |
| E1 · Hiển thị tổng 0 và trạng thái trống cho biểu đồ; danh sách trống nếu không có phản hồi | `LiveAnalytics` · [web-admin/src/pages/LiveAnalytics.tsx:166](../../../web-admin/src/pages/LiveAnalytics.tsx#L166)<br>`LiveFeedback` · [web-admin/src/components/LiveFeedback.tsx:125](../../../web-admin/src/components/LiveFeedback.tsx#L125) |  |
| Hiển thị chỉ số hoặc phản hồi phù hợp với kỳ và bộ lọc | `LiveAnalytics` · [web-admin/src/pages/LiveAnalytics.tsx:166](../../../web-admin/src/pages/LiveAnalytics.tsx#L166)<br>`LiveFeedback` · [web-admin/src/components/LiveFeedback.tsx:125](../../../web-admin/src/components/LiveFeedback.tsx#L125) |  |
| Quản trị viên chọn thao tác nào? | `LiveAnalytics` · [web-admin/src/pages/LiveAnalytics.tsx:95](../../../web-admin/src/pages/LiveAnalytics.tsx#L95) |  |
| A1 · Đổi kỳ báo cáo | `LiveAnalytics.period` · [web-admin/src/pages/LiveAnalytics.tsx:35](../../../web-admin/src/pages/LiveAnalytics.tsx#L35) |  |
| A2 · Lọc danh mục hoặc trạng thái phản hồi | `LiveFeedback.filter` · [web-admin/src/components/LiveFeedback.tsx:75](../../../web-admin/src/components/LiveFeedback.tsx#L75)<br>`FeedbackService.search` · [backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java:23](../../../backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java#L23) |  |
| Mở chi tiết phản hồi | `LiveFeedback` · [web-admin/src/components/LiveFeedback.tsx:59](../../../web-admin/src/components/LiveFeedback.tsx#L59)<br>`FeedbackService.detail` · [backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java:31](../../../backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java#L31) |  |
| NEW → IN_REVIEW → RESOLVED. Lưu thời điểm cập nhật. | `LiveFeedback.advance` · [web-admin/src/components/LiveFeedback.tsx:84](../../../web-admin/src/components/LiveFeedback.tsx#L84)<br>`FeedbackService.advance` · [backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java:35](../../../backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java#L35) |  |
| A3 · Tạo tệp .xlsx theo kỳ và bộ lọc | `LiveAnalytics.download` · [web-admin/src/pages/LiveAnalytics.tsx:43](../../../web-admin/src/pages/LiveAnalytics.tsx#L43)<br>`exportReport` · [web-admin/src/lib/export.ts:4](../../../web-admin/src/lib/export.ts#L4) |  |
| Xuất thành công? | `LiveAnalytics.download` · [web-admin/src/pages/LiveAnalytics.tsx:43](../../../web-admin/src/pages/LiveAnalytics.tsx#L43) |  |
| E2 · Báo lỗi xuất và cho thử lại | `LiveAnalytics.download` · [web-admin/src/pages/LiveAnalytics.tsx:88](../../../web-admin/src/pages/LiveAnalytics.tsx#L88) |  |
| Tải tệp .xlsx | `exportReport` · [web-admin/src/lib/export.ts:55](../../../web-admin/src/lib/export.ts#L55) |  |

## ERD

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Danh mục | `CREATE TABLE categories` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:10](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L10) |  |
| Phân loại | `locations.category_id → categories.id` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:24](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L24) |  |
| Địa điểm | `CREATE TABLE locations` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:22](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L22)<br>`LocationRepository.findPublicById()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:56](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L56) |  |
| Có tên | `location_translations.location_id → locations.id` · [backend/multi-audio-guide/src/main/resources/db/migration/V5__feedback_and_localized_names.sql:3](../../../backend/multi-audio-guide/src/main/resources/db/migration/V5__feedback_and_localized_names.sql#L3) |  |
| Tên địa điểm theo ngôn ngữ | `CREATE TABLE location_translations` · [backend/multi-audio-guide/src/main/resources/db/migration/V5__feedback_and_localized_names.sql:2](../../../backend/multi-audio-guide/src/main/resources/db/migration/V5__feedback_and_localized_names.sql#L2) |  |
| Liên quan | `feedbacks.location_id → locations.id` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:79](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L79) |  |
| Phản hồi | `CREATE TABLE feedbacks` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:77](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L77)<br>`FeedbackRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/FeedbackRepository.java:46](../../../backend/multi-audio-guide/src/main/java/com/application/repository/FeedbackRepository.java#L46) |  |
| Viết bằng | `location_translations.language_code → languages.code` · [backend/multi-audio-guide/src/main/resources/db/migration/V5__feedback_and_localized_names.sql:4](../../../backend/multi-audio-guide/src/main/resources/db/migration/V5__feedback_and_localized_names.sql#L4) |  |
| Chứa điểm | `ContentPackageService.build()` · [backend/multi-audio-guide/src/main/java/com/application/service/ContentPackageService.java:75](../../../backend/multi-audio-guide/src/main/java/com/application/service/ContentPackageService.java#L75)<br>`content_package_versions.payload` · [backend/multi-audio-guide/src/main/resources/db/demo/V6__publication_and_admin_accounts.sql:8](../../../backend/multi-audio-guide/src/main/resources/db/demo/V6__publication_and_admin_accounts.sql#L8) | Địa điểm nằm trong snapshot payload của phiên bản gói. |
| Có bài | `guide_content_revisions.location_id → locations.id` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:36](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L36) |  |
| Tài nguyên âm thanh | `CREATE TABLE audio_assets` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:47](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L47) |  |
| Ngôn ngữ | `CREATE TABLE languages` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:16](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L16) |  |
| Có tệp | `audio_assets.content_id → guide_content_revisions.id` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:49](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L49) |  |
| Phiên bản gói ngoại tuyến | `CREATE TABLE content_package_versions` · [backend/multi-audio-guide/src/main/resources/db/demo/V6__publication_and_admin_accounts.sql:8](../../../backend/multi-audio-guide/src/main/resources/db/demo/V6__publication_and_admin_accounts.sql#L8)<br>`PublicationRepository.ready()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:68](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L68) |  |
| Dùng ngôn ngữ | `guide_content_revisions.language_code → languages.code` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:37](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L37) |  |
| Chứa bản bài | `ContentPackageService.build()` · [backend/multi-audio-guide/src/main/java/com/application/service/ContentPackageService.java:75](../../../backend/multi-audio-guide/src/main/java/com/application/service/ContentPackageService.java#L75)<br>`content_package_versions.payload` · [backend/multi-audio-guide/src/main/resources/db/demo/V6__publication_and_admin_accounts.sql:8](../../../backend/multi-audio-guide/src/main/resources/db/demo/V6__publication_and_admin_accounts.sql#L8) | Bản bài nằm trong snapshot payload của phiên bản gói. |
| Bản chỉnh sửa bài thuyết minh | `CREATE TABLE guide_content_revisions` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:34](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L34)<br>`GuideRepository.findPublicById()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:42](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L42) |  |
| Gắn với bài | `analytics_events.content_id → guide_content_revisions.id` · [backend/multi-audio-guide/src/main/resources/db/migration/V3__playback_content_reference.sql:2](../../../backend/multi-audio-guide/src/main/resources/db/migration/V3__playback_content_reference.sql#L2) |  |
| Lượt bắt đầu phát | `CREATE TABLE analytics_events` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:87](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L87)<br>`analytics_events.content_id` · [backend/multi-audio-guide/src/main/resources/db/migration/V3__playback_content_reference.sql:2](../../../backend/multi-audio-guide/src/main/resources/db/migration/V3__playback_content_reference.sql#L2) |  |

## Cơ sở dữ liệu

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Ảnh sơ đồ cơ sở dữ liệu | `spring.flyway.locations` · [backend/multi-audio-guide/src/main/resources/application-demo.properties:3](../../../backend/multi-audio-guide/src/main/resources/application-demo.properties#L3)<br>`CREATE EXTENSION postgis` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:7](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L7)<br>`CREATE TABLE content_package_versions` · [backend/multi-audio-guide/src/main/resources/db/demo/V6__publication_and_admin_accounts.sql:8](../../../backend/multi-audio-guide/src/main/resources/db/demo/V6__publication_and_admin_accounts.sql#L8) | Hình sơ đồ là ảnh nhúng. Đối chiếu schema đang chạy qua nhóm migration được cấu hình. |

## Thành phần

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| Nguồn ô bản đồ bên ngoài OSM / OSM France | `attachMapTiles()` · [web-user/src/lib/mapTiles.ts:13](../../../web-user/src/lib/mapTiles.ts#L13)<br>`sources` · [web-user/src/lib/mapTiles.ts:6](../../../web-user/src/lib/mapTiles.ts#L6) |  |
| Khả năng trình duyệt Định vị · Phát audio · Tổng hợp giọng nói | `useBrowserLocation()` · [web-user/src/lib/useBrowserLocation.ts:6](../../../web-user/src/lib/useBrowserLocation.ts#L6)<br>`GuidePlayer()` · [web-user/src/components/GuidePlayer.tsx:7](../../../web-user/src/components/GuidePlayer.tsx#L7)<br>`GuidePlayback.startTts()` · [web-user/src/lib/playback.ts:109](../../../web-user/src/lib/playback.ts#L109) |  |
| Web du khách · Trình duyệt | `createRoot().render()` · [web-user/src/main.tsx:16](../../../web-user/src/main.tsx#L16) |  |
| Gateway Web User · Node Phục vụ giao diện và chuyển tiếp API công khai | `createPublicServer()` · [web-user/scripts/public-server.mjs:18](../../../web-user/scripts/public-server.mjs#L18) |  |
| Khám phá địa điểm UC-01 | `LocationEntry()` · [web-user/src/pages/LocationEntry.tsx:7](../../../web-user/src/pages/LocationEntry.tsx#L7)<br>`LocationController.search()` · [backend/multi-audio-guide/src/main/java/com/application/controller/LocationController.java:26](../../../backend/multi-audio-guide/src/main/java/com/application/controller/LocationController.java#L26) |  |
| Trình phát audio và giọng đọc UC-03 | `GuidePlayer()` · [web-user/src/components/GuidePlayer.tsx:7](../../../web-user/src/components/GuidePlayer.tsx#L7)<br>`GuidePlayback.startTts()` · [web-user/src/lib/playback.ts:109](../../../web-user/src/lib/playback.ts#L109) |  |
| Đọc bài thuyết minh UC-02 | `GuidePage()` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48)<br>`GuideController.guide()` · [backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java:19](../../../backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java#L19) |  |
| Tải, xác minh và cài gói UC-04 | `downloadPackage()` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126)<br>`verifyPackage()` · [web-user/src/lib/packages.ts:47](../../../web-user/src/lib/packages.ts#L47) |  |
| IndexedDB Gói nội dung đã xác minh | `readInstalledPackages()` · [web-user/src/lib/packages.ts:71](../../../web-user/src/lib/packages.ts#L71) |  |
| Backend TravelVoice · Spring Boot | `MultiAudioGuideApplication.main()` · [backend/multi-audio-guide/src/main/java/com/application/MultiAudioGuideApplication.java:8](../../../backend/multi-audio-guide/src/main/java/com/application/MultiAudioGuideApplication.java#L8) |  |
| Hàng đợi đồng bộ lượt phát | `enqueue()` · [web-user/public/playback-outbox.js:19](../../../web-user/public/playback-outbox.js#L19)<br>`flushReceipts()` · [web-user/public/playback-outbox.js:49](../../../web-user/public/playback-outbox.js#L49) |  |
| API du khách Địa điểm · Thuyết minh · Audio · Ảnh · Gói · Sự kiện | `LocationController.search()` · [backend/multi-audio-guide/src/main/java/com/application/controller/LocationController.java:26](../../../backend/multi-audio-guide/src/main/java/com/application/controller/LocationController.java#L26)<br>`GuideController.guide()` · [backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java:19](../../../backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java#L19)<br>`ContentPackageController.packages()` · [backend/multi-audio-guide/src/main/java/com/application/controller/ContentPackageController.java:28](../../../backend/multi-audio-guide/src/main/java/com/application/controller/ContentPackageController.java#L28) |  |
| Gợi ý theo vị trí UC-05 · Nghiệm thu có điều kiện | `useGeofencing()` · [web-user/src/lib/useGeofencing.ts:5](../../../web-user/src/lib/useGeofencing.ts#L5)<br>`GeofenceNotices()` · [web-user/src/components/GeofenceNotices.tsx:5](../../../web-user/src/components/GeofenceNotices.tsx#L5) |  |
| Service Worker và Cache Storage · Giao diện Web | `service worker: install` · [web-user/public/sw.js:3](../../../web-user/public/sw.js#L3)<br>`service worker: fetch` · [web-user/public/sw.js:28](../../../web-user/public/sw.js#L28) |  |
| Phân phối gói đã công bố Hiện trạng: BASE và OPTIONAL | `ContentPackageController.packages()` · [backend/multi-audio-guide/src/main/java/com/application/controller/ContentPackageController.java:28](../../../backend/multi-audio-guide/src/main/java/com/application/controller/ContentPackageController.java#L28)<br>`ContentPackageController.download()` · [backend/multi-audio-guide/src/main/java/com/application/controller/ContentPackageController.java:44](../../../backend/multi-audio-guide/src/main/java/com/application/controller/ContentPackageController.java#L44) | Nhãn BASE/OPTIONAL đã cũ đối với Tourist Web. API chọn WEB khi client là web. |
| Web Admin · UC-06 / UC-07 Vận hành nội bộ, cấu hình triển khai riêng | `createRoot().render()` · [web-admin/src/main.tsx:6](../../../web-admin/src/main.tsx#L6)<br>`createBrowserRouter()` · [web-admin/src/App.tsx:183](../../../web-admin/src/App.tsx#L183) |  |
| API quản trị và xác thực phiên Biên tập · Duyệt · Phân tích · Phản hồi | `AdminAuthController.login()` · [backend/multi-audio-guide/src/main/java/com/application/controller/AdminAuthController.java:12](../../../backend/multi-audio-guide/src/main/java/com/application/controller/AdminAuthController.java#L12)<br>`ReviewController.approve()` · [backend/multi-audio-guide/src/main/java/com/application/controller/ReviewController.java:15](../../../backend/multi-audio-guide/src/main/java/com/application/controller/ReviewController.java#L15) |  |
| Worker xuất bản theo lịch Xác minh và công bố nội dung cùng phiên bản gói | `PublicationWorker.process()` · [backend/multi-audio-guide/src/main/java/com/application/component/PublicationWorker.java:12](../../../backend/multi-audio-guide/src/main/java/com/application/component/PublicationWorker.java#L12)<br>`PublicationService.publish()` · [backend/multi-audio-guide/src/main/java/com/application/service/PublicationService.java:13](../../../backend/multi-audio-guide/src/main/java/com/application/service/PublicationService.java#L13) |  |
| PostgreSQL / PostGIS Nội dung · Âm thanh tải lên · Job · Phiên bản gói · Sự kiện · Phản hồi | `services.postgres` · [compose.production.yaml:3](../../../compose.production.yaml#L3)<br>`CREATE EXTENSION postgis` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:7](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L7)<br>`spring.flyway.locations` · [backend/multi-audio-guide/src/main/resources/application-demo.properties:3](../../../backend/multi-audio-guide/src/main/resources/application-demo.properties#L3) |  |

## Triển khai

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| «thiết bị» Điện thoại du khách | `createRoot().render()` · [web-user/src/main.tsx:16](../../../web-user/src/main.tsx#L16) | Code chỉ xác nhận điểm vào web. Chưa nghiệm thu trên điện thoại. |
| «nút» VPS · Docker Compose | `services` · [compose.production.yaml:2](../../../compose.production.yaml#L2)<br>`networks` · [compose.production.yaml:70](../../../compose.production.yaml#L70) | Cấu hình có trong repo. Chưa xác nhận VPS đang chạy. |
| «môi trường thực thi» Trình duyệt Web du khách | `createRoot().render()` · [web-user/src/main.tsx:16](../../../web-user/src/main.tsx#L16) |  |
| «container» Caddy Mạng public Cổng công khai 80/TCP, 443/TCP và 443/UDP | `services.caddy` · [compose.production.yaml:52](../../../compose.production.yaml#L52)<br>`services.caddy.ports` · [compose.production.yaml:58](../../../compose.production.yaml#L58)<br>`reverse_proxy web:4176` · [deploy/vps/Caddyfile:7](../../../deploy/vps/Caddyfile#L7) |  |
| Volume caddy_data, caddy_config Dữ liệu và cấu hình Caddy | `services.caddy.volumes` · [compose.production.yaml:62](../../../compose.production.yaml#L62)<br>`volumes.caddy_data` · [compose.production.yaml:76](../../../compose.production.yaml#L76)<br>`volumes.caddy_config` · [compose.production.yaml:77](../../../compose.production.yaml#L77) |  |
| Lưu trữ phía trình duyệt IndexedDB · Gói đã xác minh Cache Storage · Giao diện Web Service Worker · Quản lý bộ nhớ đệm | `readInstalledPackages()` · [web-user/src/lib/packages.ts:71](../../../web-user/src/lib/packages.ts#L71)<br>`service worker: fetch` · [web-user/public/sw.js:28](../../../web-user/public/sw.js#L28) |  |
| «container» Spring Boot Mạng data · profile demo | `services.backend` · [compose.production.yaml:19](../../../compose.production.yaml#L19)<br>`MultiAudioGuideApplication.main()` · [backend/multi-audio-guide/src/main/java/com/application/MultiAudioGuideApplication.java:8](../../../backend/multi-audio-guide/src/main/java/com/application/MultiAudioGuideApplication.java#L8) |  |
| «container» Web User · Node Mạng public và data Phục vụ giao diện và chuyển tiếp API công khai | `services.web` · [compose.production.yaml:42](../../../compose.production.yaml#L42)<br>`createPublicServer()` · [web-user/scripts/public-server.mjs:18](../../../web-user/scripts/public-server.mjs#L18) |  |
| Worker xuất bản theo lịch Chạy trong tiến trình Spring Boot | `PublicationWorker.process()` · [backend/multi-audio-guide/src/main/java/com/application/component/PublicationWorker.java:12](../../../backend/multi-audio-guide/src/main/java/com/application/component/PublicationWorker.java#L12) |  |
| «container» PostgreSQL + PostGIS Mạng data nội bộ · CSDL audio_guide Volume pg_data | `services.postgres` · [compose.production.yaml:3](../../../compose.production.yaml#L3)<br>`CREATE EXTENSION postgis` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:7](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L7) |  |
| Nguồn ô bản đồ bên ngoài OpenStreetMap / OSM France | `attachMapTiles()` · [web-user/src/lib/mapTiles.ts:13](../../../web-user/src/lib/mapTiles.ts#L13) |  |
| Phạm vi Compose trong repo: Web du khách. Web Admin cần cấu hình triển khai riêng. | `services.web` · [compose.production.yaml:42](../../../compose.production.yaml#L42)<br>`createBrowserRouter()` · [web-admin/src/App.tsx:183](../../../web-admin/src/App.tsx#L183) | Điểm vào Web Admin nằm riêng. Compose production hiện khai báo web-user. |

## 1 · M0 Ngữ cảnh

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 1.0 Chọn địa điểm và nghe thuyết minh | `LocationCatalog()` · [web-user/src/components/LocationCatalog.tsx:11](../../../web-user/src/components/LocationCatalog.tsx#L11)<br>`GuidePage()` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48) |  |
| 1.0 Chọn địa điểm và nghe thuyết minh | `OfflinePage.check()` · [web-user/src/pages/OfflinePage.tsx:61](../../../web-user/src/pages/OfflinePage.tsx#L61)<br>`downloadPackage()` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| 1.0 Chọn địa điểm và nghe thuyết minh | `useGeofencing.start()` · [web-user/src/lib/useGeofencing.ts:29](../../../web-user/src/lib/useGeofencing.ts#L29)<br>`reserveNotification()` · [web-user/src/lib/geofence.ts:63](../../../web-user/src/lib/geofence.ts#L63) |  |

## 1 · M1 Tiến trình

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 1.1 Khám phá và chọn điểm | `LocationCatalog()` · [web-user/src/components/LocationCatalog.tsx:11](../../../web-user/src/components/LocationCatalog.tsx#L11)<br>`LocationDiscoveryService.search()` · [backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java:31](../../../backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java#L31) |  |
| D01 · Danh mục, bài và âm thanh công khai API công khai và tài nguyên âm thanh | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| 1.2 Cấp bài thuyết minh | `GuidePage()` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48)<br>`GuideController.guide()` · [backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java:19](../../../backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java#L19) |  |
| D02 · Gói ngoại tuyến đã xác minh IndexedDB đã xác minh, được cập nhật ở 1.4 | `readInstalledPackages()` · [web-user/src/lib/packages.ts:71](../../../web-user/src/lib/packages.ts#L71)<br>`downloadPackage()` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| 1.3 Phát bài và đồng bộ lượt nghe | `GuidePlayer()` · [web-user/src/components/GuidePlayer.tsx:7](../../../web-user/src/components/GuidePlayer.tsx#L7)<br>`GuidePlayback.play()` · [web-user/src/lib/playback.ts:77](../../../web-user/src/lib/playback.ts#L77)<br>`recordPlayback()` · [web-user/src/lib/playbackOutbox.ts:47](../../../web-user/src/lib/playbackOutbox.ts#L47) |  |
| D03 · Hàng đợi lượt phát Hàng đợi trình duyệt gửi API lượt phát | `enqueue()` · [web-user/public/playback-outbox.js:19](../../../web-user/public/playback-outbox.js#L19)<br>`flushReceipts()` · [web-user/public/playback-outbox.js:49](../../../web-user/public/playback-outbox.js#L49) |  |
| D06 · Phiên bản gói đã xuất bản | `PublicationRepository.ready()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:68](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L68)<br>`PublicationRepository.manifests()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:91](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L91) |  |
| 1.4 · Quản lý gói ngoại tuyến Web | `checkPackageUpdates()` · [web-user/src/lib/packageUpdates.ts:13](../../../web-user/src/lib/packageUpdates.ts#L13)<br>`downloadPackage()` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| D09 · Kho khóa ký gói | `SigningKeyRepository.loadOrCreate()` · [backend/multi-audio-guide/src/main/java/com/application/repository/SigningKeyRepository.java:11](../../../backend/multi-audio-guide/src/main/java/com/application/repository/SigningKeyRepository.java#L11)<br>`PackageSigner.publicKey()` · [backend/multi-audio-guide/src/main/java/com/application/component/PackageSigner.java:31](../../../backend/multi-audio-guide/src/main/java/com/application/component/PackageSigner.java#L31) |  |
| D02 · Gói IndexedDB đã xác minh | `readInstalledPackages()` · [web-user/src/lib/packages.ts:71](../../../web-user/src/lib/packages.ts#L71)<br>`downloadPackage()` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| D02 · Gói IndexedDB đã xác minh | `readInstalledPackages()` · [web-user/src/lib/packages.ts:71](../../../web-user/src/lib/packages.ts#L71)<br>`downloadPackage()` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| 1.5 · Geofence và thông báo Web | `useGeofencing.start()` · [web-user/src/lib/useGeofencing.ts:29](../../../web-user/src/lib/useGeofencing.ts#L29)<br>`reserveNotification()` · [web-user/src/lib/geofence.ts:63](../../../web-user/src/lib/geofence.ts#L63) |  |
| D10 · Cooldown theo địa điểm | `cooldownTransaction()` · [web-user/src/lib/geofence.ts:49](../../../web-user/src/lib/geofence.ts#L49)<br>`reserveNotification()` · [web-user/src/lib/geofence.ts:63](../../../web-user/src/lib/geofence.ts#L63) |  |

## 1 · M2 1.1

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 1.1.1 Lấy danh mục địa điểm | `useLocationCatalog.load()` · [web-user/src/lib/useLocationCatalog.ts:25](../../../web-user/src/lib/useLocationCatalog.ts#L25)<br>`LocationController.search()` · [backend/multi-audio-guide/src/main/java/com/application/controller/LocationController.java:26](../../../backend/multi-audio-guide/src/main/java/com/application/controller/LocationController.java#L26) |  |
| D01 · Danh mục, bài và âm thanh công khai API công khai và tài nguyên âm thanh | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| D02 · Gói ngoại tuyến đã xác minh IndexedDB đã xác minh, được cập nhật ở 1.4 | `readInstalledPackages()` · [web-user/src/lib/packages.ts:71](../../../web-user/src/lib/packages.ts#L71)<br>`downloadPackage()` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| 1.1.2 Lọc, xếp kết quả và xác định điểm | `NearbyPlaces()` · [web-user/src/components/NearbyPlaces.tsx:10](../../../web-user/src/components/NearbyPlaces.tsx#L10)<br>`LocationCatalog.submit()` · [web-user/src/components/LocationCatalog.tsx:33](../../../web-user/src/components/LocationCatalog.tsx#L33)<br>`LocationDiscoveryService.search()` · [backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java:31](../../../backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java#L31) |  |
| 1.2 Cấp bài thuyết minh | `GuidePage()` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48)<br>`GuideController.guide()` · [backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java:19](../../../backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java#L19) |  |
| 1.1.3 Yêu cầu và hiển thị ô bản đồ | `attachMapTiles()` · [web-user/src/lib/mapTiles.ts:13](../../../web-user/src/lib/mapTiles.ts#L13)<br>`TourMap()` · [web-user/src/components/TourMap.tsx:43](../../../web-user/src/components/TourMap.tsx#L43) |  |

## 1 · M2 1.2

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 1.1 Khám phá và chọn điểm | `LocationCatalog()` · [web-user/src/components/LocationCatalog.tsx:11](../../../web-user/src/components/LocationCatalog.tsx#L11)<br>`LocationDiscoveryService.search()` · [backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java:31](../../../backend/multi-audio-guide/src/main/java/com/application/service/LocationDiscoveryService.java#L31) |  |
| 1.2.1 Tra bài theo lựa chọn và ngôn ngữ dự phòng | `GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33)<br>`offlineGuide()` · [web-user/src/lib/packages.ts:186](../../../web-user/src/lib/packages.ts#L186) |  |
| D01 · Danh mục, bài và âm thanh công khai API công khai và tài nguyên âm thanh | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| D02 · Gói ngoại tuyến đã xác minh IndexedDB đã xác minh, được cập nhật ở 1.4 | `readInstalledPackages()` · [web-user/src/lib/packages.ts:71](../../../web-user/src/lib/packages.ts#L71)<br>`downloadPackage()` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| 1.2.2 Trả bài hợp lệ hoặc trạng thái tải | `GuideReader()` · [web-user/src/pages/GuidePage.tsx:10](../../../web-user/src/pages/GuidePage.tsx#L10)<br>`useGuide.load()` · [web-user/src/lib/useGuide.ts:18](../../../web-user/src/lib/useGuide.ts#L18) |  |
| 1.3 Phát bài và đồng bộ lượt nghe | `GuidePlayer()` · [web-user/src/components/GuidePlayer.tsx:7](../../../web-user/src/components/GuidePlayer.tsx#L7)<br>`GuidePlayback.play()` · [web-user/src/lib/playback.ts:77](../../../web-user/src/lib/playback.ts#L77)<br>`recordPlayback()` · [web-user/src/lib/playbackOutbox.ts:47](../../../web-user/src/lib/playbackOutbox.ts#L47) |  |

## 1 · M2 1.3

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 1.2 Cấp bài thuyết minh | `GuidePage()` · [web-user/src/pages/GuidePage.tsx:48](../../../web-user/src/pages/GuidePage.tsx#L48)<br>`GuideController.guide()` · [backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java:19](../../../backend/multi-audio-guide/src/main/java/com/application/controller/GuideController.java#L19) |  |
| 1.3.1 Chuẩn bị âm thanh hoặc giọng đọc | `GuidePlayback.play()` · [web-user/src/lib/playback.ts:77](../../../web-user/src/lib/playback.ts#L77)<br>`GuidePlayback.startTts()` · [web-user/src/lib/playback.ts:109](../../../web-user/src/lib/playback.ts#L109) |  |
| D01 · Danh mục, bài và âm thanh công khai API công khai và tài nguyên âm thanh | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| 1.3.2 Phát bài và cập nhật trạng thái | `GuidePlayback.didStart()` · [web-user/src/lib/playback.ts:73](../../../web-user/src/lib/playback.ts#L73)<br>`GuidePlayback.pause()` · [web-user/src/lib/playback.ts:174](../../../web-user/src/lib/playback.ts#L174) |  |
| D02 · Gói ngoại tuyến đã xác minh IndexedDB đã xác minh, được cập nhật ở 1.4 | `readInstalledPackages()` · [web-user/src/lib/packages.ts:71](../../../web-user/src/lib/packages.ts#L71)<br>`downloadPackage()` · [web-user/src/lib/packages.ts:126](../../../web-user/src/lib/packages.ts#L126) |  |
| 1.3.3 Ghi và đồng bộ lượt bắt đầu phát | `recordPlayback()` · [web-user/src/lib/playbackOutbox.ts:47](../../../web-user/src/lib/playbackOutbox.ts#L47)<br>`flushReceipts()` · [web-user/public/playback-outbox.js:49](../../../web-user/public/playback-outbox.js#L49) |  |
| D03 · Hàng đợi lượt phát Hàng đợi trình duyệt gửi API lượt phát | `enqueue()` · [web-user/public/playback-outbox.js:19](../../../web-user/public/playback-outbox.js#L19)<br>`flushReceipts()` · [web-user/public/playback-outbox.js:49](../../../web-user/public/playback-outbox.js#L49) |  |

## 2 · M0 Ngữ cảnh

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 2.0 Tạo, duyệt và xuất bản nội dung | `AdminContentService.save()` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:47](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L47)<br>`ReviewService.approve()` · [backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java:16](../../../backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java#L16)<br>`PublicationService.publish()` · [backend/multi-audio-guide/src/main/java/com/application/service/PublicationService.java:13](../../../backend/multi-audio-guide/src/main/java/com/application/service/PublicationService.java#L13) |  |
| 2.0 Tạo, duyệt và xuất bản nội dung | `AdminAuthService.login()` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminAuthService.java:20](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminAuthService.java#L20)<br>`DemoAdminFilter.doFilterInternal()` · [backend/multi-audio-guide/src/main/java/com/application/config/DemoAdminFilter.java:20](../../../backend/multi-audio-guide/src/main/java/com/application/config/DemoAdminFilter.java#L20) |  |
| 2.0 Tạo, duyệt và xuất bản nội dung | `LocationForm.translate()` · [web-admin/src/pages/LocationForm.tsx:224](../../../web-admin/src/pages/LocationForm.tsx#L224)<br>`TranslationService.translate()` · [backend/multi-audio-guide/src/main/java/com/application/service/TranslationService.java:15](../../../backend/multi-audio-guide/src/main/java/com/application/service/TranslationService.java#L15) |  |

## 2 · M1 Tiến trình

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 2.1 Biên tập và gửi duyệt | `LocationForm.save()` · [web-admin/src/pages/LocationForm.tsx:416](../../../web-admin/src/pages/LocationForm.tsx#L416)<br>`AdminContentService.save()` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:47](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L47) |  |
| D04 · Bản làm việc và tệp tải lên Bản làm việc, tệp âm thanh và ảnh tải lên | `AdminContentRepository.save()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java:101](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java#L101)<br>`AdminContentRepository.insertAudio()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java:114](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java#L114) |  |
| 2.2 Xét duyệt nội dung | `ReviewService.approve()` · [backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java:16](../../../backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java#L16)<br>`ReviewService.reject()` · [backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java:22](../../../backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java#L22) |  |
| D01 · Nội dung công khai Địa điểm và các phiên bản bài công khai | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| D06 · Phiên bản gói ngoại tuyến content_package_versions · 1.4 đọc gói WEB · BUILDING / FAILED / READY | `PublicationRepository.ready()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:68](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L68)<br>`PublicationRepository.manifests()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:91](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L91) |  |
| 2.3 Xuất bản nội dung và gói | `PublicationWorker.process()` · [backend/multi-audio-guide/src/main/java/com/application/component/PublicationWorker.java:12](../../../backend/multi-audio-guide/src/main/java/com/application/component/PublicationWorker.java#L12)<br>`PublicationService.publish()` · [backend/multi-audio-guide/src/main/java/com/application/service/PublicationService.java:13](../../../backend/multi-audio-guide/src/main/java/com/application/service/PublicationService.java#L13) |  |
| D05 · Hàng đợi công việc xuất bản publication_jobs | `PublicationRepository.enqueue()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:25](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L25)<br>`PublicationRepository.pending()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:34](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L34) |  |
| 2.4 · Xác thực phiên quản trị | `AdminAuthService.login()` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminAuthService.java:20](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminAuthService.java#L20)<br>`DemoAdminFilter.doFilterInternal()` · [backend/multi-audio-guide/src/main/java/com/application/config/DemoAdminFilter.java:20](../../../backend/multi-audio-guide/src/main/java/com/application/config/DemoAdminFilter.java#L20) |  |
| D11 · Tài khoản và phiên quản trị | `AdminAuthRepository.session()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminAuthRepository.java:19](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminAuthRepository.java#L19)<br>`AdminAuthRepository.identity()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminAuthRepository.java:22](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminAuthRepository.java#L22) |  |
| 2.5 · Dịch nội dung hỗ trợ | `LocationForm.translate()` · [web-admin/src/pages/LocationForm.tsx:224](../../../web-admin/src/pages/LocationForm.tsx#L224)<br>`TranslationService.translate()` · [backend/multi-audio-guide/src/main/java/com/application/service/TranslationService.java:15](../../../backend/multi-audio-guide/src/main/java/com/application/service/TranslationService.java#L15) |  |

## 2 · M2 2.1

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 2.1.1 Nạp biểu mẫu và bản làm việc | `LocationForm()` · [web-admin/src/pages/LocationForm.tsx:47](../../../web-admin/src/pages/LocationForm.tsx#L47)<br>`AdminContentService.detail()` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:34](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L34) |  |
| D04 · Bản làm việc và tệp tải lên Bản làm việc, tệp âm thanh và ảnh tải lên | `AdminContentRepository.save()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java:101](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java#L101)<br>`AdminContentRepository.insertAudio()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java:114](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java#L114) |  |
| 2.1.2 Kiểm tra và lưu tệp tải lên | `AdminContentService.upload()` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:92](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L92)<br>`LocationImageService.upload()` · [backend/multi-audio-guide/src/main/java/com/application/service/LocationImageService.java:19](../../../backend/multi-audio-guide/src/main/java/com/application/service/LocationImageService.java#L19) |  |
| D01 · Nội dung công khai Địa điểm và các phiên bản bài công khai | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| 2.1.3 Kiểm tra và lưu bản làm việc | `AdminContentService.save()` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:47](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L47)<br>`AdminContentRepository.save()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java:101](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java#L101) |  |

## 2 · M2 2.2

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| D04 · Bản làm việc và tệp tải lên Bản làm việc, tệp âm thanh và ảnh tải lên | `AdminContentRepository.save()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java:101](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java#L101)<br>`AdminContentRepository.insertAudio()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java:114](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java#L114) |  |
| 2.2.1 Cấp hồ sơ và bản công khai để so sánh | `ReviewQueue.load()` · [web-admin/src/pages/ReviewQueue.tsx:34](../../../web-admin/src/pages/ReviewQueue.tsx#L34)<br>`AdminContentService.detail()` · [backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java:34](../../../backend/multi-audio-guide/src/main/java/com/application/service/AdminContentService.java#L34) |  |
| D05 · Hàng đợi công việc xuất bản publication_jobs | `PublicationRepository.enqueue()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:25](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L25)<br>`PublicationRepository.pending()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:34](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L34) |  |
| D01 · Nội dung công khai Địa điểm và các phiên bản bài công khai | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| 2.2.2 Ghi quyết định và xếp việc xuất bản | `ReviewService.approve()` · [backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java:16](../../../backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java#L16)<br>`PublicationRepository.enqueue()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:25](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L25) |  |
| D06 · Phiên bản gói ngoại tuyến content_package_versions · 1.4 đọc gói WEB · BUILDING / FAILED / READY | `PublicationRepository.ready()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:68](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L68)<br>`PublicationRepository.manifests()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:91](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L91) |  |

## 2 · M2 2.3

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 2.3.1 Nhận việc chờ xử lý hoặc yêu cầu thử lại | `PublicationWorker.process()` · [backend/multi-audio-guide/src/main/java/com/application/component/PublicationWorker.java:12](../../../backend/multi-audio-guide/src/main/java/com/application/component/PublicationWorker.java#L12)<br>`ReviewService.retry()` · [backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java:28](../../../backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java#L28) |  |
| D01 · Nội dung công khai Địa điểm và các phiên bản bài công khai | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| D06 · Phiên bản gói ngoại tuyến content_package_versions · 1.4 đọc gói WEB · BUILDING / FAILED / READY | `PublicationRepository.ready()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:68](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L68)<br>`PublicationRepository.manifests()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:91](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L91) |  |
| 2.3.2 Công bố bản mới và gói trong cùng giao dịch | `PublicationService.publish()` · [backend/multi-audio-guide/src/main/java/com/application/service/PublicationService.java:13](../../../backend/multi-audio-guide/src/main/java/com/application/service/PublicationService.java#L13)<br>`ContentPackageService.build()` · [backend/multi-audio-guide/src/main/java/com/application/service/ContentPackageService.java:75](../../../backend/multi-audio-guide/src/main/java/com/application/service/ContentPackageService.java#L75) |  |
| D05 · Hàng đợi công việc xuất bản publication_jobs | `PublicationRepository.enqueue()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:25](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L25)<br>`PublicationRepository.pending()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:34](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L34) |  |
| D04 · Bản làm việc và tệp tải lên Bản làm việc, tệp âm thanh và ảnh tải lên | `AdminContentRepository.save()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java:101](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java#L101)<br>`AdminContentRepository.insertAudio()` · [backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java:114](../../../backend/multi-audio-guide/src/main/java/com/application/repository/AdminContentRepository.java#L114) |  |
| 2.3.3 Trả trạng thái xuất bản | `ReviewService.jobs()` · [backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java:27](../../../backend/multi-audio-guide/src/main/java/com/application/service/ReviewService.java#L27)<br>`PublicationRepository.jobs()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java:32](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PublicationRepository.java#L32) |  |

## 3 · M0 Ngữ cảnh

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 3.0 Theo dõi và xem thông số du khách | `VisitorController.heartbeat()` · [backend/multi-audio-guide/src/main/java/com/application/controller/VisitorController.java:18](../../../backend/multi-audio-guide/src/main/java/com/application/controller/VisitorController.java#L18)<br>`PlaybackService.analytics()` · [backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java:75](../../../backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java#L75)<br>`LiveAnalytics()` · [web-admin/src/pages/LiveAnalytics.tsx:17](../../../web-admin/src/pages/LiveAnalytics.tsx#L17) |  |
| 3.0 Theo dõi và xem thông số du khách | `LiveFeedback()` · [web-admin/src/components/LiveFeedback.tsx:19](../../../web-admin/src/components/LiveFeedback.tsx#L19)<br>`FeedbackService.advance()` · [backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java:35](../../../backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java#L35) |  |
| 3.0 Theo dõi và xem thông số du khách | `LiveAnalytics.download()` · [web-admin/src/pages/LiveAnalytics.tsx:43](../../../web-admin/src/pages/LiveAnalytics.tsx#L43)<br>`exportReport()` · [web-admin/src/lib/export.ts:4](../../../web-admin/src/lib/export.ts#L4) |  |

## 3 · M1 Tiến trình

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 3.1 Ghi tín hiệu hiện diện | `startVisitorPresence()` · [web-user/src/lib/visitor.ts:13](../../../web-user/src/lib/visitor.ts#L13)<br>`VisitorController.heartbeat()` · [backend/multi-audio-guide/src/main/java/com/application/controller/VisitorController.java:18](../../../backend/multi-audio-guide/src/main/java/com/application/controller/VisitorController.java#L18) |  |
| D07 · Dấu hiện diện theo hồ sơ và nền tảng Dấu hiện diện theo định danh cài đặt và nền tảng | `VisitorRepository.touch()` · [backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java:17](../../../backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java#L17)<br>`CREATE TABLE visitor_installations` · [backend/multi-audio-guide/src/main/resources/db/migration/V9__anonymous_visitors.sql:1](../../../backend/multi-audio-guide/src/main/resources/db/migration/V9__anonymous_visitors.sql#L1) |  |
| 3.2 Ghi lượt bắt đầu phát | `DemoPlaybackController.record()` · [backend/multi-audio-guide/src/main/java/com/application/controller/DemoPlaybackController.java:24](../../../backend/multi-audio-guide/src/main/java/com/application/controller/DemoPlaybackController.java#L24)<br>`PlaybackService.record()` · [backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java:44](../../../backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java#L44) |  |
| D08 · Lượt bắt đầu phát đã ghi analytics_events | `PlaybackRepository.insert()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java:29](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java#L29)<br>`CREATE TABLE analytics_events` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:87](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L87) |  |
| D01 · Bài và địa điểm công khai Bài đã duyệt và địa điểm, gồm bản bài lịch sử dùng khi xác thực lượt phát | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| 3.3 Tổng hợp thông số | `VisitorService.stats()` · [backend/multi-audio-guide/src/main/java/com/application/service/VisitorService.java:17](../../../backend/multi-audio-guide/src/main/java/com/application/service/VisitorService.java#L17)<br>`PlaybackService.analytics()` · [backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java:75](../../../backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java#L75) |  |
| 3.4 · Quản lý phản hồi | `LiveFeedback()` · [web-admin/src/components/LiveFeedback.tsx:19](../../../web-admin/src/components/LiveFeedback.tsx#L19)<br>`FeedbackService.advance()` · [backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java:35](../../../backend/multi-audio-guide/src/main/java/com/application/service/FeedbackService.java#L35) |  |
| D12 · Kho phản hồi | `FeedbackRepository.list()` · [backend/multi-audio-guide/src/main/java/com/application/repository/FeedbackRepository.java:40](../../../backend/multi-audio-guide/src/main/java/com/application/repository/FeedbackRepository.java#L40)<br>`FeedbackRepository.advance()` · [backend/multi-audio-guide/src/main/java/com/application/repository/FeedbackRepository.java:49](../../../backend/multi-audio-guide/src/main/java/com/application/repository/FeedbackRepository.java#L49) |  |
| D01 · Địa điểm công khai | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| 3.5 · Xuất báo cáo XLSX | `LiveAnalytics.download()` · [web-admin/src/pages/LiveAnalytics.tsx:43](../../../web-admin/src/pages/LiveAnalytics.tsx#L43)<br>`exportReport()` · [web-admin/src/lib/export.ts:4](../../../web-admin/src/lib/export.ts#L4) |  |

## 3 · M2 3.1

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 3.1.1 Nhận và kiểm tra heartbeat | `VisitorController.heartbeat()` · [backend/multi-audio-guide/src/main/java/com/application/controller/VisitorController.java:18](../../../backend/multi-audio-guide/src/main/java/com/application/controller/VisitorController.java#L18)<br>`VisitorHeartbeat` · [backend/multi-audio-guide/src/main/java/com/application/dto/VisitorHeartbeat.java:6](../../../backend/multi-audio-guide/src/main/java/com/application/dto/VisitorHeartbeat.java#L6) |  |
| D07 · Dấu hiện diện theo hồ sơ và nền tảng Dấu hiện diện theo định danh cài đặt và nền tảng | `VisitorRepository.touch()` · [backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java:17](../../../backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java#L17)<br>`CREATE TABLE visitor_installations` · [backend/multi-audio-guide/src/main/resources/db/migration/V9__anonymous_visitors.sql:1](../../../backend/multi-audio-guide/src/main/resources/db/migration/V9__anonymous_visitors.sql#L1) |  |
| 3.1.2 Cập nhật dấu hiện diện | `VisitorService.heartbeat()` · [backend/multi-audio-guide/src/main/java/com/application/service/VisitorService.java:15](../../../backend/multi-audio-guide/src/main/java/com/application/service/VisitorService.java#L15)<br>`VisitorRepository.touch()` · [backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java:17](../../../backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java#L17) |  |

## 3 · M2 3.2

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 3.2.1 Kiểm tra sự kiện, mã bài và trùng lặp | `PlaybackService.record()` · [backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java:44](../../../backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java#L44)<br>`PlaybackRepository.contentForEvent()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java:38](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java#L38) |  |
| D08 · Lượt bắt đầu phát đã ghi analytics_events | `PlaybackRepository.insert()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java:29](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java#L29)<br>`CREATE TABLE analytics_events` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:87](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L87) |  |
| 3.2.2 Lưu lượt phát hợp lệ | `PlaybackRepository.insert()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java:29](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java#L29)<br>`PlaybackRepository.attribute()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java:43](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java#L43) |  |
| D01 · Bài và địa điểm công khai Bài đã duyệt và địa điểm, gồm bản bài lịch sử dùng khi xác thực lượt phát | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |

## 3 · M2 3.3

| Ô sơ đồ | Hàm, thành phần hoặc cấu hình | Ghi chú |
|---|---|---|
| 3.3.1 Tổng hợp khách vãng lai theo cửa sổ | `VisitorService.stats()` · [backend/multi-audio-guide/src/main/java/com/application/service/VisitorService.java:17](../../../backend/multi-audio-guide/src/main/java/com/application/service/VisitorService.java#L17)<br>`VisitorRepository.stats()` · [backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java:24](../../../backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java#L24) |  |
| D07 · Dấu hiện diện theo hồ sơ và nền tảng Dấu hiện diện theo định danh cài đặt và nền tảng | `VisitorRepository.touch()` · [backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java:17](../../../backend/multi-audio-guide/src/main/java/com/application/repository/VisitorRepository.java#L17)<br>`CREATE TABLE visitor_installations` · [backend/multi-audio-guide/src/main/resources/db/migration/V9__anonymous_visitors.sql:1](../../../backend/multi-audio-guide/src/main/resources/db/migration/V9__anonymous_visitors.sql#L1) |  |
| D08 · Lượt bắt đầu phát đã ghi analytics_events | `PlaybackRepository.insert()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java:29](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java#L29)<br>`CREATE TABLE analytics_events` · [backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql:87](../../../backend/multi-audio-guide/src/main/resources/db/migration/V1__demo.sql#L87) |  |
| 3.3.2 Tổng hợp bốn chỉ số lượt phát theo kỳ | `PlaybackService.analytics()` · [backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java:75](../../../backend/multi-audio-guide/src/main/java/com/application/service/PlaybackService.java#L75)<br>`PlaybackRepository.count()` · [backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java:48](../../../backend/multi-audio-guide/src/main/java/com/application/repository/PlaybackRepository.java#L48) |  |
| D01 · Bài và địa điểm công khai Bài đã duyệt và địa điểm, gồm bản bài lịch sử dùng khi xác thực lượt phát | `LocationRepository.search()` · [backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java:39](../../../backend/multi-audio-guide/src/main/java/com/application/repository/LocationRepository.java#L39)<br>`GuideRepository.find()` · [backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java:33](../../../backend/multi-audio-guide/src/main/java/com/application/repository/GuideRepository.java#L33) |  |
| 3.5 Xuất báo cáo XLSX | `LiveAnalytics.download()` · [web-admin/src/pages/LiveAnalytics.tsx:43](../../../web-admin/src/pages/LiveAnalytics.tsx#L43)<br>`exportReport()` · [web-admin/src/lib/export.ts:4](../../../web-admin/src/lib/export.ts#L4) |  |
