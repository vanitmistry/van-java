package com.example.aws.config;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

@Configuration
@EnableConfigurationProperties(DynamoDbProperties.class)
public class DynamoDbClientConfig {

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient(DynamoDbProperties properties) {
        DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder()
                .region(Region.of(properties.getRegion()));

        if (StringUtils.hasText(properties.getEndpointOverride())) {
            builder.endpointOverride(URI.create(properties.getEndpointOverride()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));
        }

        return builder.build();
    }
}
