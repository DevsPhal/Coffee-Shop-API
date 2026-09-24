package org.group1.coffeeshopapi.mail.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.exception.MailDeliveryException;
import org.group1.coffeeshopapi.common.properties.MailSenderProperties;
import org.group1.coffeeshopapi.mail.MailService;
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

    // Embedded inline so the logo shows up without needing a public URL.
    private static final String LOGO_CONTENT_ID = "logo";
    private static final String LOGO_CLASSPATH_LOCATION = "templates/email/images/590stCafeLogo.jpeg";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailSenderProperties senderProperties;

    // Synchronous on purpose, so a failed send surfaces as a real error instead of a false
    // "check your email".
    @Override
    public void sendOtpEmail(String to, String fullName, String otp, int expiryMinutes, String purposeLabel,
                              String telegramDeepLink) {
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("otp", otp);
            context.setVariable("expiryMinutes", expiryMinutes);
            context.setVariable("purposeLabel", purposeLabel);
            context.setVariable("supportEmail", senderProperties.getSupportEmail());
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
                    """.formatted(fullName, purposeLabel, otp, expiryMinutes, senderProperties.getSupportEmail());
            if (telegramDeepLink != null && !telegramDeepLink.isBlank()) {
                plainText += "\nConnect Telegram: " + telegramDeepLink + "\n";
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            // Shows as "590st Cafe <otp@590stcafe.shop>"; replies go to the support address.
            helper.setFrom(senderProperties.getAddress(), senderProperties.getName());
            helper.setReplyTo(senderProperties.getSupportEmail());
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
