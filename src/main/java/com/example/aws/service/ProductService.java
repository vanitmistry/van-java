package com.example.aws.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.example.aws.service.cart.Product;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

@Service
public class ProductService {

    private final DynamoDbAsyncClient dynamoDbAsyncClient;

    public ProductService(DynamoDbAsyncClient dynamoDbAsyncClient) {
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
    }

    public Product createProduct(String name, String description, double cost) {
        try {
            String productId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            Product product = new Product(productId, name, description, cost, 0, 0, now, now);
            persistProduct(product);
            return product;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to create product", e);
        }
    }

    public Product getProduct(String productId) {
        try {
            GetItemRequest request = GetItemRequest.builder()
                    .tableName("Product")
                    .key(java.util.Map.of("productId", AttributeValue.builder().s(productId).build()))
                    .build();

            GetItemResponse response = dynamoDbAsyncClient.getItem(request).get();
            if (!response.hasItem()) {
                return null;
            }
            return mapItemToProduct(response.item());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to get product", e);
        }
    }

    public List<Product> listProducts() {
        try {
            ScanRequest request = ScanRequest.builder()
                    .tableName("Product")
                    .build();

            ScanResponse response = dynamoDbAsyncClient.scan(request).get();
            List<Product> products = new ArrayList<>();
            for (var item : response.items()) {
                products.add(mapItemToProduct(item));
            }
            return products;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to list products", e);
        }
    }

    public void updateProduct(String productId, Product updates) {
        try {
            Product existing = getProduct(productId);
            if (existing == null) {
                throw new RuntimeException("Product not found: " + productId);
            }

            if (updates.getName() != null) {
                existing.setName(updates.getName());
            }
            if (updates.getDescription() != null) {
                existing.setDescription(updates.getDescription());
            }
            if (updates.getQuantity() >= 0) {
                existing.setQuantity(updates.getQuantity());
            }

            existing.setUpdatedAt(System.currentTimeMillis());
            persistProduct(existing);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to update product", e);
        }
    }

    private void persistProduct(Product product) throws ExecutionException, InterruptedException {
        PutItemRequest request = PutItemRequest.builder()
                .tableName("Product")
                .item(mapProductToItem(product))
                .build();

        dynamoDbAsyncClient.putItem(request).get();
    }

    private java.util.Map<String, AttributeValue> mapProductToItem(Product product) {
        return java.util.Map.ofEntries(
                java.util.Map.entry("productId", AttributeValue.builder().s(product.getProductId()).build()),
                java.util.Map.entry("name", AttributeValue.builder().s(product.getName()).build()),
                java.util.Map.entry("description", AttributeValue.builder().s(product.getDescription()).build()),
                java.util.Map.entry("cost", AttributeValue.builder().n(String.valueOf(product.getCost())).build()),
                java.util.Map.entry("quantity", AttributeValue.builder().n(String.valueOf(product.getQuantity())).build()),
                java.util.Map.entry("reserved", AttributeValue.builder().n(String.valueOf(product.getReserved())).build()),
                java.util.Map.entry("createdAt", AttributeValue.builder().n(String.valueOf(product.getCreatedAt())).build()),
                java.util.Map.entry("updatedAt", AttributeValue.builder().n(String.valueOf(product.getUpdatedAt())).build())
        );
    }

    private Product mapItemToProduct(java.util.Map<String, AttributeValue> item) {
        return new Product(
                item.get("productId").s(),
                item.get("name").s(),
                item.get("description").s(),
                Double.parseDouble(item.get("cost").n()),
                Integer.parseInt(item.get("quantity").n()),
                Integer.parseInt(item.get("reserved").n()),
                Long.parseLong(item.get("createdAt").n()),
                Long.parseLong(item.get("updatedAt").n())
        );
    }
}
