-- Fictional demo narration only. No playback events are seeded.
INSERT INTO languages(code, display_name) VALUES ('VI', 'Tiếng Việt') ON CONFLICT DO NOTHING;
INSERT INTO guide_content_revisions(id, location_id, language_code, script_type, script_text, status) VALUES
('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'VI', 'SHORT',
 'Chào mừng bạn đến với Bảo tàng Demo. Không gian trưng bày giới thiệu những câu chuyện về đời sống và văn hóa qua các hiện vật minh họa. Mời bạn dừng chân, quan sát và khám phá từng góc nhỏ. Đây là nội dung mẫu dùng để thử nghiệm ứng dụng.', 'APPROVED'),
('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'VI', 'SHORT',
 'Bạn đang khám phá Không gian Bảo tàng Demo. Hãy tưởng tượng một hành trình kết nối ký ức, con người và những thay đổi của thành phố. Mỗi điểm dừng mở ra một câu chuyện ngắn. Nội dung này chỉ phục vụ trình diễn, không phải thông tin du lịch đã xác minh.', 'APPROVED'),
('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'VI', 'SHORT',
 'Chào bạn đến với Di tích Demo. Trong hành trình minh họa này, chúng ta cùng tìm hiểu cách gìn giữ dấu ấn lịch sử và tôn trọng không gian văn hóa. Hãy tham quan nhẹ nhàng và giữ vệ sinh chung. Đây là bài thuyết minh mẫu của dự án.', 'APPROVED');
INSERT INTO audio_assets(content_id, storage_key, format) VALUES
('30000000-0000-0000-0000-000000000001', '/demo-audio/museum.wav', 'wav'),
('30000000-0000-0000-0000-000000000002', '/demo-audio/gallery.wav', 'wav'),
('30000000-0000-0000-0000-000000000003', '/demo-audio/heritage.wav', 'wav');
