package com.re.travelokahotelbookingservice.service.impl;

import com.re.travelokahotelbookingservice.exception.BusinessException;
import com.re.travelokahotelbookingservice.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
        log.info("Bắt đầu gửi email đặt lại mật khẩu tới địa chỉ: {}", toEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu - Traveloka Hotel");

            String htmlContent = "<div style=\"font-family: Arial, Helvetica, sans-serif; max-width: 580px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">"
                    + "<div style=\"text-align: center; margin-bottom: 24px;\">"
                    + "<h2 style=\"color: #0194f3; margin: 0; font-size: 24px; font-weight: bold;\">Traveloka Hotel Booking</h2>"
                    + "</div>"
                    + "<h3 style=\"color: #1e293b; font-size: 18px; margin-bottom: 12px;\">Yêu cầu đặt lại mật khẩu</h3>"
                    + "<p style=\"color: #475569; font-size: 14px; line-height: 1.6;\">Xin chào,</p>"
                    + "<p style=\"color: #475569; font-size: 14px; line-height: 1.6;\">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Traveloka Hotel của bạn. Vui lòng bấm vào nút bên dưới để tiến hành tạo mật khẩu mới:</p>"
                    + "<div style=\"text-align: center; margin: 32px 0;\">"
                    + "<a href=\"" + resetLink + "\" style=\"background-color: #0194f3; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 15px; display: inline-block; box-shadow: 0 2px 4px rgba(1, 148, 243, 0.2);\">Đặt lại mật khẩu</a>"
                    + "</div>"
                    + "<p style=\"color: #64748b; font-size: 13px; line-height: 1.5;\">Đường dẫn này chỉ có hiệu lực trong vòng <strong>15 phút</strong> vì lý do bảo mật.</p>"
                    + "<p style=\"color: #94a3b8; font-size: 12px; line-height: 1.5; margin-top: 24px; border-top: 1px solid #f1f5f9; pt: 16px;\">Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này. Mật khẩu hiện tại của bạn vẫn được giữ an toàn tuyệt đối.</p>"
                    + "<p style=\"color: #64748b; font-size: 13px; margin-top: 16px;\">Trân trọng,<br><strong>Đội ngũ Traveloka Hotel</strong></p>"
                    + "</div>";

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Đã gửi email đặt lại mật khẩu thành công tới hộp thư: {}", toEmail);
        } catch (Exception e) {
            log.error("Gửi email thất bại tới [{}]: {}", toEmail, e.getMessage());
            throw new BusinessException("Không thể gửi email xác nhận. Vui lòng kiểm tra lại cấu hình tài khoản gửi thư của hệ thống!");
        }
    }
}
