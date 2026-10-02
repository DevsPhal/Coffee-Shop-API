package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.table")
public class TableProperties {
    private String scanBaseUrl = "https://590stcafe.shop/table/";
}
