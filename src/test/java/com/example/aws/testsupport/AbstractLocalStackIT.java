package com.example.aws.testsupport;

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
public abstract class AbstractLocalStackIT {

    protected static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.4.0"))
                    .withServices(Service.S3, Service.SQS);

    static {
        LOCALSTACK.start();
        createSharedQueues();
    }

    /**
     * OutboundQueueSender/InboundQueueReceiver resolve their queue URL eagerly at bean
     * construction, and component scanning constructs them in every IT context regardless
     * of which capability's test is running - so both queues must exist before any subclass's
     * Spring context is built, not just before the SQS-specific test's own scenarios.
     */
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

    /**
     * LocalStack serves every enabled service behind one edge port, so every subclass's
     * Spring context needs both capabilities' endpoint override set here, not only the one
     * the subclass's own test is exercising - all component-scanned beans (S3 and SQS alike)
     * are constructed in every context regardless of which IT class is running.
     */
    @DynamicPropertySource
    static void localstackProperties(DynamicPropertyRegistry registry) {
        registry.add("app.s3.endpoint-override", () -> LOCALSTACK.getEndpointOverride(Service.S3).toString());
        registry.add("app.s3.region", LOCALSTACK::getRegion);
        registry.add("app.sqs.endpoint-override", () -> LOCALSTACK.getEndpointOverride(Service.SQS).toString());
        registry.add("app.sqs.region", LOCALSTACK::getRegion);
    }
}
