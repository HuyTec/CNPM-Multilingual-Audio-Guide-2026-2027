-- Fictional, pre-authored text for UC-02 integration (demo profile only).
-- Keep the existing VI/SHORT revision and its Android demo audio unchanged.
INSERT INTO languages(code, display_name) VALUES ('EN', 'English') ON CONFLICT DO NOTHING;

INSERT INTO guide_content_revisions(id, location_id, language_code, script_type, script_text, status) VALUES
('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001', 'VI', 'FULL',
 'Chào mừng bạn đến với Bảo tàng Demo. Đây là một địa điểm giả lập của dự án, dùng để thử nghiệm bài thuyết minh đầy đủ. Không gian minh họa giới thiệu đời sống và văn hóa qua những hiện vật mẫu. Bạn có thể dừng chân tại từng khu trưng bày để quan sát, đọc nội dung và khám phá những câu chuyện được biên soạn sẵn. Xin giữ vệ sinh chung và tôn trọng không gian tham quan. Bài viết này chỉ phục vụ kiểm thử phần mềm, không phải thông tin du lịch đã được xác minh.', 'APPROVED'),
('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001', 'EN', 'FULL',
 'Welcome to Demo Museum. This fictional location belongs to a software project and demonstrates a full guide script. Its illustrative displays introduce everyday life and culture through sample objects. Take a moment at each display to observe the details, read the prepared descriptions, and explore the stories. Please respect the exhibition space and keep it clean. This pre-authored text is provided for software testing only and is not verified travel information.', 'APPROVED'),
('30000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000001', 'EN', 'SHORT',
 'Welcome to Demo Museum, a fictional exhibition of everyday life and culture. Explore the sample displays and respect the shared space. This short guide is prepared for software testing only, not as verified travel information.', 'APPROVED');
-- No audio rows: text retrieval must work independently of audio availability.
