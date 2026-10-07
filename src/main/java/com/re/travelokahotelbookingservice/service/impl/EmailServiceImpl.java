package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@traveloka.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontEndUrl;

    @Override
    public void sendEmail(String toEmail, String token) {
        String resetLink = frontEndUrl + "/reset-password/" + token;
        log.info("Khởi tạo yêu cầu đặt lại mật khẩu cho email [{}]. Reset Link: {}", toEmail, resetLink);

        SimpleMailMessage message = new SimpleMailMessage();
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

        try {
            mailSender.send(message);
            log.info("Đã gửi email đặt lại mật khẩu thành công tới: {}", toEmail);
        } catch (Exception e) {
            log.warn("Không thể gửi email qua SMTP: {}. Người dùng/Tester có thể sử dụng link reset trực tiếp: {}", e.getMessage(), resetLink);
        }
    }
}
