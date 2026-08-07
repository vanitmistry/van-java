package com.example.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class AwsServiceApplicationTests {

    @TestConfiguration
    static class StubSqsClientConfig {

        @Bean
        @Primary
        DynamoDbAsyncClient dynamoDbAsyncClient() {
            DynamoDbAsyncClient stub = mock(DynamoDbAsyncClient.class);
            when(stub.listTables(any(ListTablesRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(
                            ListTablesResponse.builder().tableNames(java.util.Collections.emptyList()).build()));
            when(stub.createTable(any(CreateTableRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(CreateTableResponse.builder().build()));
            return stub;
        }

        @Bean
        @Primary
        SqsClient sqsClient() {
            SqsClient stub = mock(SqsClient.class);
            when(stub.getQueueUrl(any(GetQueueUrlRequest.class)))
                    .thenReturn(GetQueueUrlResponse.builder()
                            .queueUrl("http://localhost:4566/000000000000/stub-queue")
                            .build());
            return stub;
        }
    }

    @Test
    void contextLoads() {
        // Verifies the default (non-local) profile wires all beans (S3Client, SqsClient,
        // OutboundQueueSender, InboundQueueReceiver) without requiring LocalStack or real
        // AWS credentials. SqsClient is stubbed here only because OutboundQueueSender and
        // InboundQueueReceiver resolve their queue URL eagerly at construction time.
    }
}
