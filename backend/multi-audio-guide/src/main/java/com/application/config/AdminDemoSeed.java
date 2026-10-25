package com.application.config;
import com.application.service.AdminAuthService;
import org.springframework.boot.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile("demo")
public class AdminDemoSeed implements ApplicationRunner {
    private final AdminAuthService auth;private final String editor,reviewer;
    public AdminDemoSeed(AdminAuthService auth,@Value("${demo.seed.editor-password:EditorDemo!2026}") String editor,@Value("${demo.seed.reviewer-password:ReviewerDemo!2026}") String reviewer) { this.auth=auth;this.editor=editor;this.reviewer=reviewer; }
    public void run(ApplicationArguments args) { auth.seed("editor",editor,"EDITOR");auth.seed("reviewer",reviewer,"REVIEWER"); }
}
