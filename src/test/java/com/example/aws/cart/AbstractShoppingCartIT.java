package com.example.aws.cart;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("local")
public abstract class AbstractShoppingCartIT {

    protected static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.4.0"))
                    .withServices(Service.S3, Service.SQS, Service.DYNAMODB);

    static {
        LOCALSTACK.start();
    }

    @DynamicPropertySource
    static void localstackProperties(DynamicPropertyRegistry registry) {
        registry.add("app.dynamodb.endpoint-override", () -> LOCALSTACK.getEndpointOverride(Service.DYNAMODB).toString());
        registry.add("app.dynamodb.region", LOCALSTACK::getRegion);
    }
}
