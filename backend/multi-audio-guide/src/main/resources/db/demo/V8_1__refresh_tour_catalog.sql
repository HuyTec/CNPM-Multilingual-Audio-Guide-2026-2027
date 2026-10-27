-- Explicit content-owner request, 27/10/2026: retire the three fictional POIs.
-- Keep immutable historical migrations unchanged. Remove dependent public data
-- and withdrawn package bytes so their download URLs cannot expose retired POIs.
CREATE TEMP TABLE retired_pois ON COMMIT DROP AS
SELECT id FROM locations WHERE id IN (
 '20000000-0000-0000-0000-000000000001',
 '20000000-0000-0000-0000-000000000002',
 '20000000-0000-0000-0000-000000000003'
) OR btrim(name) IN ('Bảo tàng Demo','Không gian Bảo tàng Demo','Di tích Demo');

DELETE FROM content_package_versions v WHERE
 v.job_id IN (SELECT id FROM publication_jobs WHERE location_id IN (SELECT id FROM retired_pois))
 OR EXISTS (SELECT 1 FROM retired_pois p WHERE position(p.id::text IN convert_from(v.payload,'UTF8')) > 0);
DELETE FROM publication_jobs WHERE location_id IN (SELECT id FROM retired_pois);
DELETE FROM content_package_items WHERE content_id IN (SELECT id FROM guide_content_revisions WHERE location_id IN (SELECT id FROM retired_pois));
DELETE FROM analytics_events WHERE location_id IN (SELECT id FROM retired_pois)
 OR content_id IN (SELECT id FROM guide_content_revisions WHERE location_id IN (SELECT id FROM retired_pois));
DELETE FROM feedbacks WHERE location_id IN (SELECT id FROM retired_pois);
DELETE FROM audio_assets WHERE content_id IN (SELECT id FROM guide_content_revisions WHERE location_id IN (SELECT id FROM retired_pois));
DELETE FROM admin_audio_uploads WHERE location_id IN (SELECT id FROM retired_pois);
DELETE FROM location_image_uploads WHERE location_id IN (SELECT id FROM retired_pois);
DELETE FROM admin_content_drafts WHERE location_id IN (SELECT id FROM retired_pois);
DELETE FROM guide_content_revisions WHERE location_id IN (SELECT id FROM retired_pois);
DELETE FROM locations WHERE id IN (SELECT id FROM retired_pois);

-- Source coordinates, not a geofence survey/certification. No survey is inserted.
-- Address: 89 Bà Huyện Thanh Quan, Xuân Hòa, Hồ Chí Minh 70000, Việt Nam.
-- Text source and six-language FULL/SHORT variants: docs/vi/xa_loi_content.json.
INSERT INTO locations(id,category_id,name,coordinates,geofence_radius_m,status)
VALUES ('20000000-0000-0000-0000-000000000108','10000000-0000-0000-0000-000000000101',
 'Chùa Xá Lợi',ST_SetSRID(ST_MakePoint(106.686591,10.777973),4326)::geography,100,'APPROVED');

INSERT INTO location_translations(location_id,language_code,name) VALUES
('20000000-0000-0000-0000-000000000108','VI','Chùa Xá Lợi'),
('20000000-0000-0000-0000-000000000108','EN','Xá Lợi Pagoda'),
('20000000-0000-0000-0000-000000000108','FR','Pagode Xá Lợi'),
('20000000-0000-0000-0000-000000000108','JA','サーロイ寺'),
('20000000-0000-0000-0000-000000000108','ZH','舍利寺'),
('20000000-0000-0000-0000-000000000108','RU','Пагода Салой');

INSERT INTO guide_content_revisions(id,location_id,language_code,script_type,script_text,status) VALUES
('32000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000108','VI','FULL','Chùa Xá Lợi nằm tại số 89 Bà Huyện Thanh Quan, phường Xuân Hòa, Thành phố Hồ Chí Minh, ở góc đường Bà Huyện Thanh Quan và Sư Thiện Chiếu. Chùa được khởi công ngày 5 tháng 8 năm 1956 và hoàn thành ngày 2 tháng 5 năm 1958. Hai kiến trúc sư Trần Văn Đường và Đỗ Bá Vinh thiết kế công trình; các kỹ sư Dư Ngọc Ánh và Hồ Tố Thuận phụ trách xây dựng. Chùa được dựng để thờ xá lợi Phật. Từ cách gọi quen thuộc của người dân, Hòa thượng Khánh Anh đã đặt tên chùa là Xá Lợi khi khánh thành. Năm 1961, chùa xây thêm tháp chuông. Trong giai đoạn 1964–1966, nơi đây còn là cơ sở giảng dạy của Viện Đại học Vạn Hạnh. Từ năm 1981 đến tháng 5 năm 1993, chùa là trụ sở Văn phòng II của Giáo hội Phật giáo Việt Nam. Những dấu mốc ấy cho thấy vai trò của chùa trong đời sống và giáo dục Phật giáo tại thành phố.','APPROVED'),
('32000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000108','VI','SHORT','Chùa Xá Lợi ở số 89 Bà Huyện Thanh Quan, phường Xuân Hòa, Thành phố Hồ Chí Minh. Chùa khởi công năm 1956, hoàn thành năm 1958 để thờ xá lợi Phật. Tháp chuông được xây thêm năm 1961. Chùa từng là cơ sở giảng dạy của Viện Đại học Vạn Hạnh và trụ sở Văn phòng II của Giáo hội Phật giáo Việt Nam.','APPROVED'),
('32000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000108','EN','FULL','Xá Lợi Pagoda stands at 89 Bà Huyện Thanh Quan, Xuân Hòa Ward, Ho Chi Minh City, at the corner with Sư Thiện Chiếu Street. Construction began on August 5, 1956, and was completed on May 2, 1958. Architects Trần Văn Đường and Đỗ Bá Vinh designed the building, while engineers Dư Ngọc Ánh and Hồ Tố Thuận supervised construction. The pagoda was built to enshrine relics of the Buddha. At its inauguration, Venerable Khánh Anh adopted the familiar name Xá Lợi used by local people. A bell tower was added in 1961. Between 1964 and 1966, the pagoda also served as a teaching site for Vạn Hạnh University. From 1981 until May 1993, it housed Office II of the Vietnam Buddhist Sangha. These milestones reflect the pagoda''s place in the city''s Buddhist religious and educational life.','APPROVED'),
('32000000-0000-0000-0000-000000000004','20000000-0000-0000-0000-000000000108','EN','SHORT','Xá Lợi Pagoda is at 89 Bà Huyện Thanh Quan, Xuân Hòa Ward, Ho Chi Minh City. Built between 1956 and 1958 to enshrine Buddha relics, it gained a bell tower in 1961. It later served as a teaching site for Vạn Hạnh University and as Office II of the Vietnam Buddhist Sangha.','APPROVED'),
('32000000-0000-0000-0000-000000000005','20000000-0000-0000-0000-000000000108','FR','FULL','La pagode Xá Lợi se trouve au 89, rue Bà Huyện Thanh Quan, dans le quartier de Xuân Hòa à Hô Chi Minh-Ville, à l''angle de la rue Sư Thiện Chiếu. Sa construction débute le 5 août 1956 et s''achève le 2 mai 1958. Les architectes Trần Văn Đường et Đỗ Bá Vinh en dessinent les plans; les ingénieurs Dư Ngọc Ánh et Hồ Tố Thuận dirigent les travaux. La pagode est destinée à abriter des reliques du Bouddha. Lors de son inauguration, le vénérable Khánh Anh retient le nom Xá Lợi, déjà employé par les habitants. Un clocher est ajouté en 1961. De 1964 à 1966, le lieu accueille aussi des enseignements de l''université Vạn Hạnh. De 1981 à mai 1993, il abrite le Bureau II de la Sangha bouddhiste du Vietnam. Ces étapes témoignent du rôle de la pagode dans la vie religieuse et l''enseignement bouddhistes de la ville.','APPROVED'),
('32000000-0000-0000-0000-000000000006','20000000-0000-0000-0000-000000000108','FR','SHORT','La pagode Xá Lợi est située au 89, rue Bà Huyện Thanh Quan, quartier de Xuân Hòa à Hô Chi Minh-Ville. Construite de 1956 à 1958 pour abriter des reliques du Bouddha, elle reçoit un clocher en 1961. Elle accueille ensuite des enseignements de l''université Vạn Hạnh et le Bureau II de la Sangha bouddhiste du Vietnam.','APPROVED'),
('32000000-0000-0000-0000-000000000007','20000000-0000-0000-0000-000000000108','JA','FULL','サーロイ寺は、ホーチミン市スアンホア地区のバー・フエン・タイン・クアン通り89番地、スー・ティエン・チエウ通りとの角にあります。1956年8月5日に着工し、1958年5月2日に完成しました。建築家チャン・ヴァン・ドゥオンとドー・バー・ヴィンが設計し、技師ズー・ゴック・アインとホー・トー・トゥアンが工事を指揮しました。寺は仏舎利を安置するために建てられました。落成時、カイン・アイン和尚は、人々の間で親しまれていた「サーロイ」という呼び名を正式な寺名にしました。1961年には鐘楼が増築されました。1964年から1966年にはヴァンハイン大学の教育施設としても使われ、1981年から1993年5月まではベトナム仏教教会の第二事務所が置かれました。これらの歩みは、この寺が都市の仏教信仰と教育に果たした役割を伝えています。','APPROVED'),
('32000000-0000-0000-0000-000000000008','20000000-0000-0000-0000-000000000108','JA','SHORT','サーロイ寺はホーチミン市スアンホア地区、バー・フエン・タイン・クアン通り89番地にあります。仏舎利を安置するため1956年から1958年に建設され、1961年に鐘楼が加わりました。後にヴァンハイン大学の教育施設や、ベトナム仏教教会の第二事務所としても使われました。','APPROVED'),
('32000000-0000-0000-0000-000000000009','20000000-0000-0000-0000-000000000108','ZH','FULL','舍利寺位于胡志明市春和坊婆县清关街89号，坐落在婆县清关街与师善照街的交汇处。寺院于1956年8月5日动工，1958年5月2日建成，由建筑师陈文堂和杜伯荣设计，工程师余玉映和胡素顺负责施工。寺院为供奉佛舍利而建。落成时，庆英和尚采用民众惯用的称呼，将寺院正式命名为舍利寺。1961年，寺院增建钟楼。1964年至1966年，这里还曾作为万行大学的教学场所。1981年至1993年5月，越南佛教教会第二办公室设于此处。这些历史阶段体现了寺院在城市佛教信仰生活和教育中的作用。','APPROVED'),
('32000000-0000-0000-0000-000000000010','20000000-0000-0000-0000-000000000108','ZH','SHORT','舍利寺位于胡志明市春和坊婆县清关街89号，为供奉佛舍利于1956年至1958年间修建，1961年增建钟楼。寺院后来曾用作万行大学的教学场所，也曾是越南佛教教会第二办公室所在地。','APPROVED'),
('32000000-0000-0000-0000-000000000011','20000000-0000-0000-0000-000000000108','RU','FULL','Пагода Салой находится по адресу: улица Ба Хюен Тхань Куан, 89, квартал Суанхоа, Хошимин, на углу с улицей Шы Тхиен Тиеу. Строительство началось 5 августа 1956 года и завершилось 2 мая 1958 года. Проект подготовили архитекторы Чан Ван Дыонг и До Ба Винь, а работами руководили инженеры Зы Нгок Ань и Хо То Тхуан. Пагоду возвели для хранения реликвий Будды. При открытии достопочтенный Кхань Ань утвердил привычное местным жителям название Салой. В 1961 году появилась колокольня. В 1964–1966 годах здесь также проходили занятия университета Ванхань. С 1981 года по май 1993 года в пагоде размещался второй офис Буддийской сангхи Вьетнама. Эти этапы отражают её роль в религиозной жизни и буддийском образовании города.','APPROVED'),
('32000000-0000-0000-0000-000000000012','20000000-0000-0000-0000-000000000108','RU','SHORT','Пагода Салой расположена по адресу: улица Ба Хюен Тхань Куан, 89, квартал Суанхоа, Хошимин. Её построили в 1956–1958 годах для хранения реликвий Будды, а в 1961 году добавили колокольню. Позже здесь проводил занятия университет Ванхань и размещался второй офис Буддийской сангхи Вьетнама.','APPROVED');
