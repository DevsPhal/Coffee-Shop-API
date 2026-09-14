package org.group1.coffeeshopapi.mail.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.exception.MailDeliveryException;
import org.group1.coffeeshopapi.mail.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    // Synchronous, deliberately: an OTP is the one thing standing between the caller and
    // register/login actually succeeding, so a failure here must reach them as a real error
    // instead of a false "check your email" — see MailDeliveryException. (This used to be
    // @Async, firing the send in the background and swallowing any failure into a log line no
    // caller ever saw — the request looked successful even when no email ever went out.)
    @Override
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
            String plainText = """
                    Hi %s,

                    Your %s code is: %s

                    This code expires in %d minutes.
                    Enter it in the verification form to continue.

                    Didn't request this code? You can safely ignore this email.

                    Need help? Contact us at %s
                    """.formatted(fullName, purposeLabel, otp, expiryMinutes, shopEmail);
            if (telegramDeepLink != null && !telegramDeepLink.isBlank()) {
                plainText += "\nConnect Telegram: " + telegramDeepLink + "\n";
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(shopEmail);
            helper.setSubject(purposeLabel + " Verification Code");
            // Let the email client choose HTML or plain text; both contain the same code.
            helper.setText(plainText, html);
            if (logoAvailable) {
                helper.addInline(LOGO_CONTENT_ID, logo);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}", to, ex);
            throw new MailDeliveryException(
                    "Couldn't send the verification email right now — please try again in a moment", ex);
        }
    }
}
