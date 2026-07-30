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

import com.example.aws.service.cart.CartStatus;
import com.example.aws.service.cart.ShoppingCart;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

class CartServiceTest {

    private DynamoDbAsyncClient mockDynamoDbClient;
    private ProductService mockProductService;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        mockDynamoDbClient = mock(DynamoDbAsyncClient.class);
        mockProductService = mock(ProductService.class);
        cartService = new CartService(mockDynamoDbClient, mockProductService);
    }

    @Test
    void testCreateCart() {
        when(mockDynamoDbClient.putItem(any(software.amazon.awssdk.services.dynamodb.model.PutItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        ShoppingCart cart = cartService.createCart("John Doe", "123 Main St");

        assertNotNull(cart);
        assertNotNull(cart.getCartId());
        assertEquals("John Doe", cart.getName());
        assertEquals("123 Main St", cart.getAddress());
        assertEquals(CartStatus.PENDING, cart.getStatus());
    }

    @Test
    void testGetCartNotFound() {
        GetItemResponse emptyResponse = GetItemResponse.builder().build();
        when(mockDynamoDbClient.getItem(any(software.amazon.awssdk.services.dynamodb.model.GetItemRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(emptyResponse));

        ShoppingCart cart = cartService.getCart("nonexistent");

        assertNull(cart);
    }
}
