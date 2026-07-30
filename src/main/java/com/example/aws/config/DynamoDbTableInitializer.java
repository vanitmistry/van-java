package com.example.aws.config;

import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Component
public class DynamoDbTableInitializer {

    private final DynamoDbAsyncClient dynamoDbAsyncClient;

    public DynamoDbTableInitializer(DynamoDbAsyncClient dynamoDbAsyncClient) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        initializeTables();
    }

    private void initializeTables() {
        try {
            createShoppingCartTable();
            createItemTable();
            createProductTable();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DynamoDB tables", e);
        }
    }

    private void createShoppingCartTable() throws ExecutionException, InterruptedException {
        String tableName = "ShoppingCart";
        if (tableExists(tableName)) {
            return;
        }

        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("cartId")
                                .keyType(KeyType.HASH)
                                .build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("cartId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("status")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("statusIndex")
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName("status")
                                                .keyType(KeyType.HASH)
                                                .build(),
                                        KeySchemaElement.builder()
                                                .attributeName("cartId")
                                                .keyType(KeyType.RANGE)
                                                .build()
                                )
                                .projection(
                                        Projection.builder()
                                                .projectionType(ProjectionType.ALL)
                                                .build()
                                )
                                .build()
                )
                .build();

        try {
            dynamoDbAsyncClient.createTable(request).get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ResourceInUseException)) {
                throw e;
            }
        }
    }

    private void createItemTable() throws ExecutionException, InterruptedException {
        String tableName = "Item";
        if (tableExists(tableName)) {
            return;
        }

        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("cartId")
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName("productId")
                                .keyType(KeyType.RANGE)
                                .build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("cartId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("productId")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        try {
            dynamoDbAsyncClient.createTable(request).get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ResourceInUseException)) {
                throw e;
            }
        }
    }

    private void createProductTable() throws ExecutionException, InterruptedException {
        String tableName = "Product";
        if (tableExists(tableName)) {
            return;
        }

        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("productId")
                                .keyType(KeyType.HASH)
                                .build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("productId")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        try {
            dynamoDbAsyncClient.createTable(request).get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof ResourceInUseException)) {
                throw e;
            }
        }
    }

    private boolean tableExists(String tableName) throws ExecutionException, InterruptedException {
        ListTablesRequest request = ListTablesRequest.builder().build();
        ListTablesResponse response = dynamoDbAsyncClient.listTables(request).get();
        return response.tableNames().contains(tableName);
    }
}
