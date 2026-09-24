package org.group1.coffeeshopapi.common.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailSenderPropertiesTest {

    @Test
    void defaultsToTheShopOtpAddress() {
        MailSenderProperties properties = new MailSenderProperties();

        assertThat(properties.getAddress()).isEqualTo("otp@590stcafe.shop");
        assertThat(properties.getSupportEmail()).isEqualTo("otp@590stcafe.shop");
        assertThat(properties.getName()).isEqualTo("590st Cafe");
    }

    @Test
    void blankValuesFromAnEnvFileFallBackInsteadOfBreakingEveryEmail() {
        MailSenderProperties properties = new MailSenderProperties();
        properties.setAddress("  ");
        properties.setSupportEmail("");

        assertThat(properties.getAddress()).isEqualTo("otp@590stcafe.shop");
        assertThat(properties.getSupportEmail()).isEqualTo("otp@590stcafe.shop");
    }

    @Test
    void supportEmailFollowsACustomSenderUnlessSetItself() {
        MailSenderProperties properties = new MailSenderProperties();
        properties.setAddress("hello@590stcafe.shop");

        assertThat(properties.getSupportEmail()).isEqualTo("hello@590stcafe.shop");

        properties.setSupportEmail("support@590stcafe.shop");
        assertThat(properties.getSupportEmail()).isEqualTo("support@590stcafe.shop");
    }
}
