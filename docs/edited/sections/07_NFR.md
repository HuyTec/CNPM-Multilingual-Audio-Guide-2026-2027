# Mục 7 — Non Functional Requirements

Giai đoạn 2, Lượt 4. Điền mục 7 trống bằng bảng NFR ID / Category / Requirement. Toàn bộ chỉ tiêu từ sheet 8 là đề xuất, chưa phải kết quả kiểm thử. Giữ giá trị trong [ ]; không bổ sung số liệu hoặc khẳng định tuân thủ pháp lý đã được xác nhận.

| Vị trí | Kiểu | Văn bản tiếng Anh dán được | Mã liên quan |
| --- | --- | --- | --- |
| 7 — đoạn dẫn trước bảng NFR mới | ĐIỀN MỤC TRỐNG | Proposed Non-Functional Requirements: Bracketed values are initial targets requiring team confirmation and validation. Test devices, network conditions, workload, measurement methods, and acceptance evidence are [nhóm điền]. | S11, sheet 8, D11 |
| 7 — NFR-01 Performance | THÊM DÒNG | NFR-01 — Performance: Location Details Screen loads within [2] s on 4G and within [1] s from the Local Content Store. | S11, NFR-01 |
| 7 — NFR-02 Performance | THÊM DÒNG | NFR-02 — Performance: Nearby-location search returns results within [3] s after a valid GPS fix. | S11, NFR-02 |
| 7 — NFR-03 Performance | THÊM DÒNG | NFR-03 — Performance: Pre-rendered audio starts within [2] s of selecting Play; TTS fallback starts within [3] s when a device voice for the effective languageCode is available. | S11, NFR-03, G11 |
| 7 — NFR-04 Offline | THÊM DÒNG | NFR-04 — Offline: Base Content bundle is at most [150] MB. UC-01 search and category browsing, UC-02, and UC-03 work without network access using Base Content. Offline Map View displays location markers on a simplified background without map tiles. Audio playback uses available bundled audio or device TTS, subject to UC-03 E4. | S11, NFR-04, G06, G11, D06 |
| 7 — NFR-05 Platform | THÊM DÒNG | NFR-05 — Platform: Proposed supported platforms are Android [8.0+] and iOS [15+]. | S11, NFR-05 |
| 7 — NFR-06 Security | THÊM DÒNG | NFR-06 — Security: All client–server traffic uses HTTPS (TLS). Web Admin requires authenticated, role-based access. Content packages are verified by checksum and signature under UC-04 BR-02. | S11, NFR-06, G20 |
| 7 — NFR-07 Privacy | THÊM DÒNG | NFR-07 — Privacy: Location is used only for nearby search and geofencing; analytics events use anonymous identifiers. Proposed compliance requirement: data handling complies with Decree 13/2023/ND-CP on personal data protection, subject to confirmation by a qualified reviewer. This is a requirement for review, not evidence of established compliance. | S11, NFR-07, sheet 8 ghi chú pháp lý |
| 7 — NFR-08 Battery | THÊM DÒNG | NFR-08 — Battery: Geofence monitoring consumes at most [3]% battery per hour of normal walking use. Measurement device and conditions are [nhóm điền]. | S11, NFR-08 |
| 7 — NFR-09 Reliability | THÊM DÒNG | NFR-09 — Reliability: A failed or interrupted update never corrupts or removes installed content, as required by UC-04 BR-02. | S11, NFR-09 |
| 7 — NFR-10 Accessibility | THÊM DÒNG | NFR-10 — Accessibility: Mobile UI supports system font scaling, screen-reader labels, and WCAG 2.1 AA contrast. | S11, NFR-10 |
| 7 — NFR-11 Localization | THÊM DÒNG | NFR-11 — Localization: UI and guide content use Unicode; the supported languages and support flags are listed in the Appendix. | S11, NFR-11, G09 |
| 7 — NFR-12 Availability | THÊM DÒNG | NFR-12 — Availability: Server API availability is at least [99.5]% per month. | S11, NFR-12 |
| 7 — NFR-13 Scalability | THÊM DÒNG | NFR-13 — Scalability: The system supports at least [500] locations and [1,000] concurrent users. | S11, NFR-13 |
| 7 — NFR-14 Media | THÊM DÒNG | NFR-14 — Media: Audio assets use [MP3 or M4A (AAC)] at a bitrate of [64–128] kbps. Proposed upload size and duration limits remain those in UC-06 BR7. | S11, NFR-14, G35 |

NFR-03/04 được thêm điều kiện TTS để không mâu thuẫn E4 đã soạn. Không dùng các con số OS trong Excel để tuyên bố đã kiểm chứng geofence limits. Yêu cầu pháp lý giữ nguồn Excel và ghi rõ cần xác nhận chuyên môn; chưa thực hiện rà soát pháp lý hiện hành.
