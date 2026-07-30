package com.example.aws.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.aws.service.cart.Product;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

class ProductServiceTest {

    private DynamoDbAsyncClient mockDynamoDbClient;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        mockDynamoDbClient = mock(DynamoDbAsyncClient.class);
        productService = new ProductService(mockDynamoDbClient);
    }

    @Test
    void testCreateProduct() {
        when(mockDynamoDbClient.putItem((software.amazon.awssdk.services.dynamodb.model.PutItemRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Product product = productService.createProduct("Test Product", "A test product", 9.99);

        assertNotNull(product);
        assertNotNull(product.getProductId());
        assertEquals("Test Product", product.getName());
        assertEquals("A test product", product.getDescription());
        assertEquals(9.99, product.getCost());
        assertEquals(0, product.getQuantity());
        assertEquals(0, product.getReserved());
    }

    @Test
    void testGetProductNotFound() {
        GetItemResponse emptyResponse = GetItemResponse.builder().build();
        when(mockDynamoDbClient.getItem((software.amazon.awssdk.services.dynamodb.model.GetItemRequest) any()))
                .thenReturn(CompletableFuture.completedFuture(emptyResponse));

        Product product = productService.getProduct("nonexistent");

        assertNull(product);
    }
}
