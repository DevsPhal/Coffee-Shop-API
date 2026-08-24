package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    /** Internal endpoint the backend uses to talk to MinIO (e.g. http://minio:9000 in Docker). */
    private String endpoint;

    /** Endpoint embedded in URLs handed back to clients; falls back to {@link #endpoint} when unset. */
    private String publicEndpoint;

    private String accessKey;
    private String secretKey;
    private String bucket;

    public String getPublicEndpoint() {
        return (publicEndpoint == null || publicEndpoint.isBlank()) ? endpoint : publicEndpoint;
    }
}
