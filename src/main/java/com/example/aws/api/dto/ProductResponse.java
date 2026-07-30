package com.example.aws.api.dto;

public class ProductResponse {
    private String productId;
    private String name;
    private String description;
    private double cost;
    private int quantity;
    private int reserved;
    private int available;
    private long createdAt;
    private long updatedAt;

    public ProductResponse() {
    }

    public ProductResponse(String productId, String name, String description, double cost,
                           int quantity, int reserved, int available, long createdAt, long updatedAt) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.quantity = quantity;
        this.reserved = reserved;
        this.available = available;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
