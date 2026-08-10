package org.group1.coffeeshopapi.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.buckets.public:590st-public}")
    private String publicBucketName;

    @Value("${minio.buckets.private:590st-private}")
    private String privateBucketName;

    @Value("${minio.buckets.receipts:590st-receipts}")
    private String receiptsBucketName;

    @Value("${minio.buckets.reports:590st-reports}")
    private String reportsBucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    @Profile("!test")
    public ApplicationRunner minioBucketInitializer(MinioClient minioClient) {
        return args -> List.of(bucketName, publicBucketName, privateBucketName, receiptsBucketName, reportsBucketName).stream()
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .forEach(name -> createBucketIfMissing(minioClient, name));
    }

    private void createBucketIfMissing(MinioClient minioClient, String name) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(name).build());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize MinIO bucket: " + name, exception);
        }
    }
}
