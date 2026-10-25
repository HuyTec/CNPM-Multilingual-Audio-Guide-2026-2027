package com.application;

import com.application.service.ContentPackageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
@Transactional
class TourPackageIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ContentPackageService packages;
    @Autowired com.application.repository.PublicationRepository versions;
    @Autowired com.application.service.PublicationService publisher;
    @org.junit.jupiter.api.BeforeEach void materializePackages() {
        var job=versions.enqueue(null);publisher.publish(job);
    }
    private final JsonMapper mapper = JsonMapper.builder().build();
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("TEST_DATABASE_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("TEST_DATABASE_PASSWORD"));
    }
    @Test void allSevenTimesSixTimesTwoArePublicAndAudioIsOptional() throws Exception {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guide_content_revisions WHERE id::text LIKE '31000000-%'", Integer.class)).isEqualTo(84);
        for (int index=101; index<=107; index++) for (String language : new String[]{"VI","EN","FR","JA","ZH","RU"}) for (String type : new String[]{"FULL","SHORT"}) {
            String id = "20000000-0000-0000-0000-" + String.format("%012d", index);
            String text = jdbc.queryForObject("SELECT script_text FROM guide_content_revisions WHERE location_id=?::uuid AND language_code=? AND script_type=?", String.class, id, language, type);
            var response = mvc.perform(get("/api/locations/{id}/guide", id).param("language", language).param("scriptType", type))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.languageCode").value(language))
                    .andExpect(jsonPath("$.scriptType").value(type)).andExpect(jsonPath("$.scriptText").value(text));
            if (language.equals("VI")) {
                String url = mapper.readTree(response.andReturn().getResponse().getContentAsString()).get("audioUrl").asString();
                assertThat(url).matches("/api/content-audio/[0-9a-f-]{36}\\.mp3");
                byte[] audio = mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("audio/mpeg")).andReturn().getResponse().getContentAsByteArray();
                assertThat(audio.length).isGreaterThan(1024);
                var bundle = packages.build("BASE");
                assertThat(bundle.manifest().assets()).anySatisfy(asset -> {
                    assertThat(asset.url()).isEqualTo(url);
                    assertThat(asset.sha256()).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(audio)));
                });
            } else response.andExpect(jsonPath("$.audioUrl").isEmpty());
        }
    }
    @Test void signedSnapshotMatchesPublicContentAndIsStable() throws Exception {
        var bundle = packages.build("BASE"); var manifest = bundle.manifest();
        assertThat(packages.build("BASE").manifest().version()).isEqualTo(manifest.version());
        assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bundle.bytes()))).isEqualTo(manifest.sha256());
        var key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(packages.publicKey().spki())));
        var verifier = Signature.getInstance("SHA256withRSA"); verifier.initVerify(key); verifier.update(bundle.bytes());
        assertThat(verifier.verify(Base64.getDecoder().decode(manifest.signature()))).isTrue();
        var payload = mapper.readTree(bundle.bytes());
        assertThat(payload.get("locations").size()).isEqualTo(7);
        assertThat(payload.get("guides").size()).isEqualTo(84);
        assertThat(payload.get("assets").size()).isEqualTo(14);
        mvc.perform(get(manifest.downloadUrl())).andExpect(status().isOk()).andExpect(content().bytes(bundle.bytes()));
        byte[] tampered = bundle.bytes().clone(); tampered[tampered.length-2] ^= 1;
        verifier.initVerify(key); verifier.update(tampered);
        assertThat(verifier.verify(Base64.getDecoder().decode(manifest.signature()))).isFalse();
    }
    @Test void unapprovedReplacementIsExcludedAndApprovedChangeGetsNewVersion() {
        String old = packages.build("BASE").manifest().version();
        jdbc.update("UPDATE guide_content_revisions SET status='DRAFT' WHERE id='31000000-0000-0000-0000-000000000001'");
        assertThat(packages.build("BASE").manifest().version()).isNotEqualTo(old);
        assertThat(new String(packages.build("BASE").bytes(), java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("\"contentId\":\"31000000-0000-0000-0000-000000000001\"");
    }
    @Test void narrationMigrationCannotAttachSpeechToChangedTextOrReplaceExistingAudio() throws Exception {
        var connection = org.springframework.jdbc.datasource.DataSourceUtils.getConnection(jdbc.getDataSource());
        var migration = new db.demo.V3_5__vietnamese_female_narration();
        var context = new org.flywaydb.core.api.migration.Context() {
            public java.sql.Connection getConnection() { return connection; }
            public org.flywaydb.core.api.configuration.Configuration getConfiguration() { return null; }
        };
        String id = "31000000-0000-0000-0000-000000000001";
        String original = jdbc.queryForObject("SELECT storage_key FROM audio_assets WHERE content_id=?::uuid",String.class,id);
        migration.migrate(context);
        assertThat(jdbc.queryForObject("SELECT storage_key FROM audio_assets WHERE content_id=?::uuid",String.class,id)).isEqualTo(original);
        jdbc.update("DELETE FROM audio_assets WHERE content_id=?::uuid",id);
        jdbc.update("UPDATE guide_content_revisions SET script_text='Bài đã sửa, chưa tạo audio mới' WHERE id=?::uuid",id);
        migration.migrate(context);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audio_assets WHERE content_id=?::uuid",Integer.class,id)).isZero();
    }
    @Test void geofencePublicationRequiresSurveyAndPolicyAndMatchingCoordinates() {
        jdbc.update("DELETE FROM location_geofence_surveys"); jdbc.update("DELETE FROM geofence_configuration");
        assertThat(mapper.readTree(packages.build("BASE").bytes()).has("geofencing")).isFalse();
        jdbc.update("INSERT INTO geofence_configuration(cooldown_seconds) VALUES (3600)");
        assertThat(mapper.readTree(packages.build("BASE").bytes()).has("geofencing")).isFalse();
        jdbc.update("""
                INSERT INTO location_geofence_surveys(location_id,latitude,longitude,radius_m,surveyed_at,source_reference,localized_names)
                SELECT id,ST_Y(coordinates::geometry),ST_X(coordinates::geometry),geofence_radius_m,'2026-01-01T00:00:00Z',
                'TEST FIXTURE ONLY','{"VI":"Test VI","EN":"Test EN","FR":"Test FR","JA":"Test JA","ZH":"Test ZH","RU":"Test RU"}'::jsonb
                FROM locations WHERE id='20000000-0000-0000-0000-000000000101'
                """);
        var metadata = mapper.readTree(packages.build("BASE").bytes()).get("geofencing");
        assertThat(metadata.get("cooldownSeconds").asInt()).isEqualTo(3600);
        assertThat(metadata.get("localizedNames").size()).isEqualTo(1);
        assertThat(metadata.get("localizedNames").get("20000000-0000-0000-0000-000000000101").get("VI").asString())
                .isEqualTo(jdbc.queryForObject("SELECT name FROM location_translations WHERE location_id='20000000-0000-0000-0000-000000000101' AND language_code='VI'",String.class));
        jdbc.update("UPDATE location_geofence_surveys SET radius_m=radius_m+1");
        assertThat(mapper.readTree(packages.build("BASE").bytes()).has("geofencing")).isFalse();
    }

    @Test void optionalPackageContainsMatchingChecksummedAudioAndWrongVersionIs404() throws Exception {
        var optional = packages.build("OPTIONAL").manifest();
        assertThat(optional.isBase()).isFalse(); assertThat(optional.assets()).hasSize(3);
        assertThat(optional.assets()).allSatisfy(asset -> { assertThat(asset.sizeBytes()).isGreaterThan(100_000); assertThat(asset.sha256()).hasSize(64); });
        mvc.perform(get("/api/content-packages/BASE/wrong-version")).andExpect(status().isNotFound());
        mvc.perform(get("/api/content-packages/WRONG/wrong-version")).andExpect(status().isBadRequest());
    }
}
