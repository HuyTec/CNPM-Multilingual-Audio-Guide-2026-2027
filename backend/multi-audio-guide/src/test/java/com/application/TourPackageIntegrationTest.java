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
            mvc.perform(get("/api/locations/{id}/guide", id).param("language", language).param("scriptType", type))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.languageCode").value(language))
                    .andExpect(jsonPath("$.scriptType").value(type)).andExpect(jsonPath("$.scriptText").value(text))
                    .andExpect(jsonPath("$.audioUrl").isEmpty());
        }
    }
    @Test void signedSnapshotMatchesPublicContentAndIsStable() throws Exception {
        var bundle = packages.build("BASE"); var manifest = bundle.manifest();
        assertThat(packages.build("BASE").manifest().version()).isEqualTo(manifest.version());
        assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bundle.bytes()))).isEqualTo(manifest.sha256());
        var key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(packages.publicKey().get("spki"))));
        var verifier = Signature.getInstance("SHA256withRSA"); verifier.initVerify(key); verifier.update(bundle.bytes());
        assertThat(verifier.verify(Base64.getDecoder().decode(manifest.signature()))).isTrue();
        var payload = mapper.readTree(bundle.bytes());
        assertThat(payload.get("locations").size()).isEqualTo(7);
        assertThat(payload.get("guides").size()).isEqualTo(84);
        assertThat(payload.get("assets").size()).isZero();
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
    @Test void optionalPackageContainsMatchingChecksummedAudioAndWrongVersionIs404() throws Exception {
        var optional = packages.build("OPTIONAL").manifest();
        assertThat(optional.isBase()).isFalse(); assertThat(optional.assets()).hasSize(3);
        assertThat(optional.assets()).allSatisfy(asset -> { assertThat(asset.sizeBytes()).isGreaterThan(100_000); assertThat(asset.sha256()).hasSize(64); });
        mvc.perform(get("/api/content-packages/BASE/wrong-version")).andExpect(status().isNotFound());
        mvc.perform(get("/api/content-packages/WRONG/wrong-version")).andExpect(status().isBadRequest());
    }
}
