-- Per-test fixtures, rolled back after each test. Not shipped in the application.
INSERT INTO locations(id,category_id,name,coordinates,geofence_radius_m,status) VALUES
('20000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001','Bảo tàng Demo',ST_SetSRID(ST_MakePoint(106.700,10.770),4326)::geography,100,'APPROVED'),
('20000000-0000-0000-0000-000000000002','10000000-0000-0000-0000-000000000001','Không gian Bảo tàng Demo',ST_SetSRID(ST_MakePoint(106.701,10.771),4326)::geography,100,'APPROVED'),
('20000000-0000-0000-0000-000000000003','10000000-0000-0000-0000-000000000002','Di tích Demo',ST_SetSRID(ST_MakePoint(106.702,10.772),4326)::geography,150,'APPROVED');
INSERT INTO guide_content_revisions(id,location_id,language_code,script_type,script_text,status) VALUES
('30000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','VI','SHORT','Bài ngắn kiểm thử độc lập.','APPROVED'),
('30000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000002','VI','SHORT','Bài ngắn kiểm thử thứ hai.','APPROVED'),
('30000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000003','VI','SHORT','Bài ngắn kiểm thử thứ ba.','APPROVED'),
('30000000-0000-0000-0000-000000000004','20000000-0000-0000-0000-000000000001','VI','FULL','Bài đầy đủ kiểm thử độc lập.','APPROVED'),
('30000000-0000-0000-0000-000000000005','20000000-0000-0000-0000-000000000001','EN','FULL','Independent full guide test fixture.','APPROVED'),
('30000000-0000-0000-0000-000000000006','20000000-0000-0000-0000-000000000001','EN','SHORT','Independent short guide test fixture.','APPROVED');
INSERT INTO audio_assets(content_id,storage_key,format) VALUES
('30000000-0000-0000-0000-000000000001','/demo-audio/museum.wav','wav'),
('30000000-0000-0000-0000-000000000002','/demo-audio/gallery.wav','wav'),
('30000000-0000-0000-0000-000000000003','/demo-audio/heritage.wav','wav');
