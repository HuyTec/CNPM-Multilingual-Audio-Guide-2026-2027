package com.application.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TouristMailSender {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    public TouristMailSender(ObjectProvider<JavaMailSender> sender,@Value("${tourist.mail.from:}") String from) { this.sender=sender; this.from=from; }
    public void send(String email,String code) {
        var mailer=sender.getIfAvailable();
        if (mailer==null || from.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Dịch vụ email chưa sẵn sàng. Bạn vẫn có thể sử dụng app với tư cách khách.");
        var message=new SimpleMailMessage(); message.setFrom(from); message.setTo(email);
        message.setSubject("TravelQ1 — Mã đăng nhập");
        message.setText("Mã đăng nhập TravelQ1: "+code+"\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này. Nếu không yêu cầu đăng nhập, bạn có thể bỏ qua email.");
        try { mailer.send(message); }
        catch (org.springframework.mail.MailException failure) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa gửi được email. Hãy thử lại sau; bạn vẫn có thể dùng app với tư cách khách."); }
    }
}
