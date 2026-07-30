package com.example.aws.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.example.aws.service.cart.CartItem;
import com.example.aws.service.cart.CartStatus;
import com.example.aws.service.cart.Product;
import com.example.aws.service.cart.ShoppingCart;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Service
public class CartService {

    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final ProductService productService;

    public CartService(DynamoDbAsyncClient dynamoDbAsyncClient, ProductService productService) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.productService = productService;
    }

    public ShoppingCart createCart(String name, String address) {
        try {
            String cartId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            ShoppingCart cart = new ShoppingCart(cartId, name, address, CartStatus.PENDING, now, now);
            persistCart(cart);
            return cart;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to create cart", e);
        }
    }

    public ShoppingCart getCart(String cartId) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName("ShoppingCart")
                    .key(Map.of("cartId", AttributeValue.builder().s(cartId).build()))
                    .build();

            GetItemResponse response = dynamoDbAsyncClient.getItem(request).get();
            if (!response.hasItem()) {
                return null;
            }
            return mapItemToCart(response.item());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get cart", e);
        }
    }

    public ShoppingCart updateCartStatus(String cartId, CartStatus newStatus) {
        try {
            ShoppingCart cart = getCart(cartId);
            if (cart == null) {
                throw new RuntimeException("Cart not found: " + cartId);
            }

            validateStatusTransition(cart.getStatus(), newStatus);
            cart.setStatus(newStatus);
            cart.setUpdatedAt(System.currentTimeMillis());

            persistCart(cart);
            return cart;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to update cart status", e);
        }
    }

    public List<ShoppingCart> listCartsByStatus(CartStatus status) {
        try {
            QueryRequest request = QueryRequest.builder()
                    .tableName("ShoppingCart")
                    .indexName("statusIndex")
                    .keyConditionExpression("statusAttribute = :status")
                    .expressionAttributeValues(Map.of(
                            ":status", AttributeValue.builder().s(status.toString()).build()
                    ))
                    .build();

            QueryResponse response = dynamoDbAsyncClient.query(request).get();
            List<ShoppingCart> carts = new ArrayList<>();
            for (Map<String, AttributeValue> item : response.items()) {
                carts.add(mapItemToCart(item));
            }
            return carts;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to list carts by status", e);
        }
    }

    public List<CartItem> getCartItems(String cartId) {
        try {
            QueryRequest request = QueryRequest.builder()
                    .tableName("Item")
                    .keyConditionExpression("cartId = :cartId")
                    .expressionAttributeValues(Map.of(
                            ":cartId", AttributeValue.builder().s(cartId).build()
                    ))
                    .build();

            QueryResponse response = dynamoDbAsyncClient.query(request).get();
            List<CartItem> items = new ArrayList<>();
            for (Map<String, AttributeValue> item : response.items()) {
                items.add(mapItemToCartItem(item));
            }
            return items;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get cart items", e);
        }
    }

    public CartItem addItemToCart(String cartId, String productId, int requestedQuantity) {
        try {
            ShoppingCart cart = getCart(cartId);
            if (cart == null) {
                throw new RuntimeException("Cart not found: " + cartId);
            }

            if (cart.getStatus() != CartStatus.PENDING) {
                throw new RuntimeException("Cart status must be PENDING to add items");
            }

            Product product = productService.getProduct(productId);
            if (product == null) {
                throw new RuntimeException("Product not found: " + productId);
            }

            int available = product.getAvailable();
            int fillQty = Math.min(requestedQuantity, available);

            if (fillQty == 0) {
                throw new RuntimeException("Out of stock: " + productId);
            }

            double totalCost = fillQty * product.getCost();
            boolean partialFilled = fillQty < requestedQuantity;
            long now = System.currentTimeMillis();

            CartItem item = new CartItem(cartId, productId, fillQty, totalCost, partialFilled, requestedQuantity, now);

            // Atomic transaction: add item and update product stock
            executeTransactionWithItem(
                    createPutItemTransaction(item),
                    createUpdateProductReservedTransaction(productId, fillQty)
            );

            return item;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to add item to cart", e);
        }
    }

    public void removeItemFromCart(String cartId, String productId) {
        try {
            ShoppingCart cart = getCart(cartId);
            if (cart == null) {
                throw new RuntimeException("Cart not found: " + cartId);
            }

            if (cart.getStatus() != CartStatus.PENDING) {
                throw new RuntimeException("Cart status must be PENDING to remove items");
            }

            CartItem item = getCartItem(cartId, productId);
            if (item == null) {
                throw new RuntimeException("Item not found in cart");
            }

            // Atomic transaction: delete item and restore product stock
            executeTransactionWithItem(
                    createDeleteItemTransaction(cartId, productId),
                    createUpdateProductReservedTransaction(productId, -item.getQuantity())
            );
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to remove item from cart", e);
        }
    }

    public CartItem updateItemQuantity(String cartId, String productId, int newQuantity) {
        try {
            ShoppingCart cart = getCart(cartId);
            if (cart == null) {
                throw new RuntimeException("Cart not found: " + cartId);
            }

            if (cart.getStatus() != CartStatus.PENDING) {
                throw new RuntimeException("Cart status must be PENDING to update items");
            }

            CartItem existingItem = getCartItem(cartId, productId);
            if (existingItem == null) {
                throw new RuntimeException("Item not found in cart");
            }

            if (newQuantity == 0) {
                removeItemFromCart(cartId, productId);
                return null;
            }

            Product product = productService.getProduct(productId);
            if (product == null) {
                throw new RuntimeException("Product not found: " + productId);
            }

            int currentQty = existingItem.getQuantity();
            int delta = newQuantity - currentQty;
            int available = product.getAvailable() + currentQty; // Account for already reserved

            int fillQty = newQuantity;
            if (delta > 0) {
                int addableQty = Math.min(delta, product.getQuantity() - (product.getReserved() - currentQty));
                fillQty = currentQty + addableQty;
            }

            boolean partialFilled = fillQty < newQuantity;
            existingItem.setQuantity(fillQty);
            existingItem.setPartialFilled(partialFilled);

            int stockDelta = fillQty - currentQty;
            // Atomic transaction: update item and adjust reserved stock
            executeTransactionWithItem(
                    createUpdateItemQuantityTransaction(cartId, productId, fillQty, partialFilled),
                    createUpdateProductReservedTransaction(productId, stockDelta)
            );

            return existingItem;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to update item quantity", e);
        }
    }

    private CartItem getCartItem(String cartId, String productId) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName("Item")
                    .key(Map.of(
                            "cartId", AttributeValue.builder().s(cartId).build(),
                            "productId", AttributeValue.builder().s(productId).build()
                    ))
                    .build();

            GetItemResponse response = dynamoDbAsyncClient.getItem(request).get();
            if (!response.hasItem()) {
                return null;
            }
            return mapItemToCartItem(response.item());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get cart item", e);
        }
    }

    private void persistCart(ShoppingCart cart) throws ExecutionException, InterruptedException {
        PutItemRequest request = PutItemRequest.builder()
                .tableName("ShoppingCart")
                .item(mapCartToItem(cart))
                .build();

        dynamoDbAsyncClient.putItem(request).get();
    }

    private void executeTransactionWithItem(TransactWriteItem item1, TransactWriteItem item2) throws ExecutionException, InterruptedException {
        TransactWriteItemsRequest request = TransactWriteItemsRequest.builder()
                .transactItems(item1, item2)
                .build();

        dynamoDbAsyncClient.transactWriteItems(request).get();
    }

    private TransactWriteItem createPutItemTransaction(CartItem item) {
        return TransactWriteItem.builder()
                .put(software.amazon.awssdk.services.dynamodb.model.Put.builder()
                        .tableName("Item")
                        .item(mapCartItemToItem(item))
                        .build())
                .build();
    }

    private TransactWriteItem createDeleteItemTransaction(String cartId, String productId) {
        return TransactWriteItem.builder()
                .delete(software.amazon.awssdk.services.dynamodb.model.Delete.builder()
                        .tableName("Item")
                        .key(Map.of(
                                "cartId", AttributeValue.builder().s(cartId).build(),
                                "productId", AttributeValue.builder().s(productId).build()
                        ))
                        .build())
                .build();
    }

    private TransactWriteItem createUpdateItemQuantityTransaction(String cartId, String productId, int quantity, boolean partialFilled) {
        return TransactWriteItem.builder()
                .update(software.amazon.awssdk.services.dynamodb.model.Update.builder()
                        .tableName("Item")
                        .key(Map.of(
                                "cartId", AttributeValue.builder().s(cartId).build(),
                                "productId", AttributeValue.builder().s(productId).build()
                        ))
                        .updateExpression("SET quantity = :qty, partialFilled = :pf")
                        .expressionAttributeValues(Map.of(
                                ":qty", AttributeValue.builder().n(String.valueOf(quantity)).build(),
                                ":pf", AttributeValue.builder().bool(partialFilled).build()
                        ))
                        .build())
                .build();
    }

    private TransactWriteItem createUpdateProductReservedTransaction(String productId, int delta) {
        return TransactWriteItem.builder()
                .update(software.amazon.awssdk.services.dynamodb.model.Update.builder()
                        .tableName("Product")
                        .key(Map.of("productId", AttributeValue.builder().s(productId).build()))
                        .updateExpression("ADD reserved :delta")
                        .expressionAttributeValues(Map.of(
                                ":delta", AttributeValue.builder().n(String.valueOf(delta)).build()
                        ))
                        .build())
                .build();
    }

    private void validateStatusTransition(CartStatus currentStatus, CartStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        boolean validTransition = switch (currentStatus) {
            case PENDING -> newStatus == CartStatus.PAID;
            case PAID -> newStatus == CartStatus.DELIVERING;
            case DELIVERING -> newStatus == CartStatus.COMPLETE;
            case COMPLETE -> false;
        };

        if (!validTransition) {
            throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }

    private Map<String, AttributeValue> mapCartToItem(ShoppingCart cart) {
        return Map.ofEntries(
                Map.entry("cartId", AttributeValue.builder().s(cart.getCartId()).build()),
                Map.entry("name", AttributeValue.builder().s(cart.getName()).build()),
                Map.entry("address", AttributeValue.builder().s(cart.getAddress()).build()),
                Map.entry("status", AttributeValue.builder().s(cart.getStatus().toString()).build()),
                Map.entry("createdAt", AttributeValue.builder().n(String.valueOf(cart.getCreatedAt())).build()),
                Map.entry("updatedAt", AttributeValue.builder().n(String.valueOf(cart.getUpdatedAt())).build())
        );
    }

    private ShoppingCart mapItemToCart(Map<String, AttributeValue> item) {
        return new ShoppingCart(
                item.get("cartId").s(),
                item.get("name").s(),
                item.get("address").s(),
                CartStatus.valueOf(item.get("status").s()),
                Long.parseLong(item.get("createdAt").n()),
                Long.parseLong(item.get("updatedAt").n())
        );
    }

    private Map<String, AttributeValue> mapCartItemToItem(CartItem cartItem) {
        if (cartItem.isPartialFilled()) {
            return Map.ofEntries(
                    Map.entry("cartId", AttributeValue.builder().s(cartItem.getCartId()).build()),
                    Map.entry("productId", AttributeValue.builder().s(cartItem.getProductId()).build()),
                    Map.entry("quantity", AttributeValue.builder().n(String.valueOf(cartItem.getQuantity())).build()),
                    Map.entry("totalCost", AttributeValue.builder().n(String.valueOf(cartItem.getTotalCost())).build()),
                    Map.entry("partialFilled", AttributeValue.builder().bool(cartItem.isPartialFilled()).build()),
                    Map.entry("requestedQuantity", AttributeValue.builder().n(String.valueOf(cartItem.getRequestedQuantity())).build()),
                    Map.entry("addedAt", AttributeValue.builder().n(String.valueOf(cartItem.getAddedAt())).build())
            );
        } else {
            return Map.ofEntries(
                    Map.entry("cartId", AttributeValue.builder().s(cartItem.getCartId()).build()),
                    Map.entry("productId", AttributeValue.builder().s(cartItem.getProductId()).build()),
                    Map.entry("quantity", AttributeValue.builder().n(String.valueOf(cartItem.getQuantity())).build()),
                    Map.entry("totalCost", AttributeValue.builder().n(String.valueOf(cartItem.getTotalCost())).build()),
                    Map.entry("partialFilled", AttributeValue.builder().bool(cartItem.isPartialFilled()).build()),
                    Map.entry("addedAt", AttributeValue.builder().n(String.valueOf(cartItem.getAddedAt())).build())
            );
        }
    }

    private CartItem mapItemToCartItem(Map<String, AttributeValue> item) {
        int requestedQty = item.containsKey("requestedQuantity") ?
                Integer.parseInt(item.get("requestedQuantity").n()) : 0;

        return new CartItem(
                item.get("cartId").s(),
                item.get("productId").s(),
                Integer.parseInt(item.get("quantity").n()),
                Double.parseDouble(item.get("totalCost").n()),
                item.get("partialFilled").bool(),
                requestedQty,
                Long.parseLong(item.get("addedAt").n())
        );
    }
}
