package org.group1.coffeeshopapi.auth.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.config.EmailConfig;
import org.group1.coffeeshopapi.auth.service.EmailService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig;

    @Override
    public void sendOtpEmail(String email, String userName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8"
            );

            Context context = new Context();
            context.setVariable("name", userName);
            context.setVariable("email", email);
            context.setVariable("otp", otp);
            context.setVariable("expired", "5 minutes");
            context.setVariable("shopEmail", emailConfig.getShopEmail());

            String htmlContent = templateEngine.process("otp-email", context);
            helper.setTo(email);
            helper.setSubject("590st Cafe - OTP Verification");
            helper.setText(htmlContent, true);

            ClassPathResource logo = new ClassPathResource("static/images/590stCafe.png");
            helper.addInline("logoImage", logo);

            ClassPathResource banner = new ClassPathResource("static/images/590stCafeBanner.png");
            helper.addInline("banner", banner);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}