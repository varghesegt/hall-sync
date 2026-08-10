package com.exam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${app.aws.s3.endpoint:#{null}}")
    private String endpointUrl;

    @Value("${app.aws.region:us-east-1}")
    private String region;

    @Value("${app.aws.access-key:dummy-key}")
    private String accessKey;

    @Value("${app.aws.secret-key:dummy-secret}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        
        var overrideConfig = software.amazon.awssdk.core.client.config.ClientOverrideConfiguration.builder()
                .apiCallTimeout(java.time.Duration.ofSeconds(3))
                .apiCallAttemptTimeout(java.time.Duration.ofSeconds(2))
                .build();

        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .overrideConfiguration(overrideConfig);

        // If an endpoint URL is provided (e.g. for MinIO local dev), configure it
        if (endpointUrl != null && !endpointUrl.isBlank()) {
            builder.endpointOverride(URI.create(endpointUrl))
                   .serviceConfiguration(S3Configuration.builder()
                           .pathStyleAccessEnabled(true) // Required for MinIO
                           .build());
        }

        return builder.build();
    }
}
