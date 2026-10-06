package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.service.EmailService;
import jdk.dynalink.linker.support.SimpleLinkRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontEndUrl;

    @Override
    public void sendEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        String resetLink = frontEndUrl + "/reset-password/" + token;

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Đặt lại mật khẩu - Traveloka Hotel");
        message.setText(
                "Xin chào,\n\n"
                + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu "
                + "cho tài khoản Traveloka Hotel của bạn.\n\n"
                + "Vui lòng truy cập đường dẫn bên dưới để đặt lại mật khẩu:\n\n"
                + resetLink
                + "\n\n"
                + "Đường dẫn này sẽ hết hạn sau 15 phút.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, "
                + "vui lòng bỏ qua email này. "
                + "Mật khẩu hiện tại của bạn sẽ không thay đổi.\n\n"
                + "Trân trọng,\n"
                + "Traveloka Hotel"
        );

        mailSender.send(message);
    }
}
