package org.group1.coffeeshopapi.mail.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.mail.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    // Referenced by the template as an inline cid: image rather than a public URL, so the logo
    // shows up without needing to host it anywhere — see logoUrl handling below.
    private static final String LOGO_CONTENT_ID = "logo";
    private static final String LOGO_CLASSPATH_LOCATION = "templates/email/images/590stCafeLogo.jpeg";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${mail.shop.email}")
    private String shopEmail;

    @Override
    @Async("mailTaskExecutor")
    public void sendOtpEmail(String to, String fullName, String otp, int expiryMinutes, String purposeLabel,
                              String telegramDeepLink) {
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("otp", otp);
            context.setVariable("expiryMinutes", expiryMinutes);
            context.setVariable("purposeLabel", purposeLabel);
            context.setVariable("shopEmail", shopEmail);
            context.setVariable("telegramDeepLink", telegramDeepLink);

            Resource logo = new ClassPathResource(LOGO_CLASSPATH_LOCATION);
            boolean logoAvailable = logo.exists();
            if (logoAvailable) {
                context.setVariable("logoUrl", "cid:" + LOGO_CONTENT_ID);
            }

            String html = templateEngine.process("email/otp-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(shopEmail);
            helper.setSubject(purposeLabel + " Verification Code");
            helper.setText(html, true);
            if (logoAvailable) {
                helper.addInline(LOGO_CONTENT_ID, logo);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}", to, ex);
        }
    }
}