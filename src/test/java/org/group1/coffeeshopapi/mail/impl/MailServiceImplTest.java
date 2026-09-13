package org.group1.coffeeshopapi.mail.impl;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceImplTest {

    private JavaMailSender mailSender;
    private MailServiceImpl mailService;
    private MimeMessage message;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        mailSender = mock(JavaMailSender.class);
        message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        mailService = new MailServiceImpl(mailSender, templateEngine);
        ReflectionTestUtils.setField(mailService, "shopEmail", "support@example.com");
    }

    @ParameterizedTest
    @EnumSource(OtpPurpose.class)
    void includesTheActualCodeInHtmlAndPlainTextForEveryPurpose(OtpPurpose purpose) throws Exception {
        mailService.sendOtpEmail("customer@example.com", "Customer <Test>", "482913", 5,
                purpose.label(), null);

        verify(mailSender).send(message);
        MimeMessage delivered = serializeAndReadMessage();
        assertThat(delivered.getAllRecipients()[0].toString()).isEqualTo("customer@example.com");
        assertThat(delivered.getSubject()).isEqualTo(purpose.label() + " Verification Code");

        List<Part> parts = leafParts(delivered);
        String plainText = body(parts, "text/plain");
        String html = body(parts, "text/html");
        assertThat(plainText).contains("482913", purpose.label(), "5 minutes", "support@example.com")
                .doesNotContain("000000", "Connect Telegram");
        assertThat(html).contains(">482913</p>", purpose.label(), "Customer &lt;Test&gt;", "cid:logo")
                .doesNotContain("000000", "${otp}", "th:text", "<a ");
        assertThat(parts).anySatisfy(part -> {
            assertThat(part.getContentType()).startsWith("image/");
            assertThat(part.getDisposition()).isEqualTo(Part.INLINE);
            assertThat(part.getHeader("Content-ID")).containsExactly("<logo>");
        });
    }

    @Test
    void preservesTheOptionalTelegramLinkInBothVersions() throws Exception {
        String telegramLink = "https://t.me/example_bot?start=test-link";
        mailService.sendOtpEmail("customer@example.com", "Customer", "729184", 10,
                OtpPurpose.LOGIN.label(), telegramLink);

        verify(mailSender).send(message);
        List<Part> parts = leafParts(serializeAndReadMessage());
        assertThat(body(parts, "text/plain")).contains("729184", "10 minutes", telegramLink);
        assertThat(body(parts, "text/html")).contains(">729184</p>", telegramLink);
    }

    private MimeMessage serializeAndReadMessage() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeTo(output);
        return new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(output.toByteArray()));
    }

    private List<Part> leafParts(Part part) throws Exception {
        List<Part> parts = new ArrayList<>();
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                parts.addAll(leafParts(multipart.getBodyPart(i)));
            }
        } else {
            parts.add(part);
        }
        return parts;
    }

    private String body(List<Part> parts, String mimeType) throws Exception {
        List<String> bodies = new ArrayList<>();
        for (Part part : parts) {
            if (part.isMimeType(mimeType)) {
                bodies.add((String) part.getContent());
            }
        }
        assertThat(bodies).as("%s message body", mimeType).hasSize(1);
        return bodies.getFirst();
    }
}
