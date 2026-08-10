package org.group1.coffeeshopapi.auth.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.config.EmailConfig;
import org.group1.coffeeshopapi.auth.service.EmailService;
import org.group1.coffeeshopapi.common.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.auth.email-required:true}")
    private boolean emailRequired;

    @Override
    public void sendOtpEmail(String recipientEmail, String userName, String otp) {
        if (!StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            if (emailRequired) {
                log.error("Cannot send OTP email to {} because the SMTP sender username or password is not configured.", recipientEmail);
                throw new InvalidRequestException("OTP email sender is not configured. Set MAIL_USERNAME and MAIL_PASSWORD.");
            }
            log.warn("SMTP is not configured. OTP email to {} was not sent; use the OTP printed in backend logs.", recipientEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8"
            );

            Context context = new Context();
            context.setVariable("name", userName);
            context.setVariable("email", recipientEmail);
            context.setVariable("otp", otp);
            context.setVariable("expired", "5 minutes");
            context.setVariable("shopEmail", emailConfig.getShopEmail());

            String htmlContent = templateEngine.process("otp-email", context);
            helper.setTo(recipientEmail);
            helper.setFrom(mailUsername);
            helper.setSubject("590st Cafe - OTP Verification");
            helper.setText(htmlContent, true);

            ClassPathResource logo = new ClassPathResource("static/images/590stCafe.png");
            helper.addInline("logoImage", logo);

            ClassPathResource banner = new ClassPathResource("static/images/590stCafeBanner.png");
            helper.addInline("banner", banner);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}
