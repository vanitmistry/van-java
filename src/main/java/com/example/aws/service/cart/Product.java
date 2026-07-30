package com.example.aws.service.cart;

public class Product {
    private String productId;
    private String name;
    private String description;
    private double cost;
    private int quantity;
    private int reserved;
    private long createdAt;
    private long updatedAt;

    public Product() {
    }

    public Product(String productId, String name, String description, double cost, int quantity, int reserved,
                   long createdAt, long updatedAt) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.quantity = quantity;
        this.reserved = reserved;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getAvailable() {
        return quantity - reserved;
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
