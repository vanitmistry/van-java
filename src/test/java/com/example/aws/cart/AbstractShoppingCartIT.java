package com.example.aws.cart;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

@SpringBootTest
@ActiveProfiles("local")
public abstract class AbstractShoppingCartIT {

    protected static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.4.0"))
                    .withServices(Service.S3, Service.SQS, Service.DYNAMODB);

    static {
        LOCALSTACK.start();
        createSharedQueues();
    }

    private static void createSharedQueues() {
        try (SqsClient bootstrapClient = SqsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(Service.SQS))
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build()) {
            bootstrapClient.createQueue(CreateQueueRequest.builder().queueName("out-queue").build());
            bootstrapClient.createQueue(CreateQueueRequest.builder().queueName("in-queue").build());
        }
    }

    @DynamicPropertySource
    static void localstackProperties(DynamicPropertyRegistry registry) {
        registry.add("app.s3.endpoint-override", () -> LOCALSTACK.getEndpointOverride(Service.S3).toString());
        registry.add("app.s3.region", LOCALSTACK::getRegion);
        registry.add("app.sqs.endpoint-override", () -> LOCALSTACK.getEndpointOverride(Service.SQS).toString());
        registry.add("app.sqs.region", LOCALSTACK::getRegion);
        registry.add("app.dynamodb.endpoint-override", () -> LOCALSTACK.getEndpointOverride(Service.DYNAMODB).toString());
        registry.add("app.dynamodb.region", LOCALSTACK::getRegion);
    }
}
