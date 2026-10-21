package com.application;

import com.application.service.TouristMailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"demo.admin.password=test-only-password"})
@AutoConfigureMockMvc @ActiveProfiles("demo") @Transactional
@EnabledIfEnvironmentVariable(named="TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class TouristAuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean TouristMailSender mail;
    private final JsonMapper mapper=JsonMapper.builder().build();
    private final String key=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",()->System.getenv("TEST_DATABASE_URL"));
        registry.add("spring.datasource.username",()->System.getenv("TEST_DATABASE_USERNAME"));
        registry.add("spring.datasource.password",()->System.getenv("TEST_DATABASE_PASSWORD"));
    }
    private String[] request() throws Exception {
        String email=UUID.randomUUID()+"@example.com";
        var result=mvc.perform(post("/api/tourist/otp/request").header("X-Installation-Key",key).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\"}"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.code").doesNotExist()).andReturn();
        var code=ArgumentCaptor.forClass(String.class);verify(mail).send(eq(email),code.capture());
        return new String[]{mapper.readTree(result.getResponse().getContentAsString()).get("challengeId").asString(),code.getValue(),email};
    }
    private org.springframework.test.web.servlet.ResultActions verifyCode(String id,String code,String device) throws Exception {
        return mvc.perform(post("/api/tourist/otp/verify").header("X-Installation-Key",device).contentType(MediaType.APPLICATION_JSON).content("{\"challengeId\":\""+id+"\",\"code\":\""+code+"\"}"));
    }
    @Test void guestIsStableAndHasNoAdminAccess() throws Exception {
        String first=mvc.perform(post("/api/tourist/guest").header("X-Installation-Key",key)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/tourist/guest").header("X-Installation-Key",key)).andExpect(content().json(first));
        mvc.perform(get("/api/admin/analytics").header("Authorization","Bearer "+key)).andExpect(status().isUnauthorized());
    }
    @Test void verifiedEmailOwnsNewPlaybacksAndTokenCanBeRevoked() throws Exception {
        var otp=request();
        var response=verifyCode(otp[0],otp[1],key).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(otp[2])).andReturn();
        var session=mapper.readTree(response.getResponse().getContentAsString());String token=session.get("accessToken").asString();
        verifyCode(otp[0],otp[1],key).andExpect(status().isBadRequest());
        String content=jdbc.queryForObject("SELECT id::text FROM guide_content_revisions WHERE location_id='20000000-0000-0000-0000-000000000101' AND language_code='VI' AND script_type='FULL'",String.class);
        UUID event=UUID.randomUUID();String body="{\"eventId\":\""+event+"\",\"contentId\":\""+content+"\"}";
        mvc.perform(post("/api/playbacks").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT listener_id::text FROM analytics_events WHERE id=?",String.class,event)).isEqualTo(session.get("userId").asString());
        mvc.perform(post("/api/tourist/logout").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
        mvc.perform(post("/api/playbacks").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analytics_events WHERE id=?",Long.class,event)).isEqualTo(1);
    }
    @Test void attemptsArePersistedAndLockedAfterFiveWrongCodes() throws Exception {
        var otp=request(); String wrong=otp[1].equals("000000") ? "000001" : "000000";
        for(int i=0;i<5;i++) verifyCode(otp[0],wrong,key).andExpect(status().isBadRequest());
        verifyCode(otp[0],otp[1],key).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT attempts FROM tourist_otp WHERE id=?::uuid",Integer.class,otp[0])).isEqualTo(5);
    }
    @Test void expiredOrOtherDeviceCannotVerifyAndResendIsThrottled() throws Exception {
        var otp=request();
        verifyCode(otp[0],otp[1],"B".repeat(43)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/tourist/otp/request").header("X-Installation-Key",key).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+otp[2]+"\"}")).andExpect(status().isTooManyRequests());
        jdbc.update("UPDATE tourist_otp SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second' WHERE id=?::uuid",otp[0]);
        verifyCode(otp[0],otp[1],key).andExpect(status().isBadRequest());
    }
}
