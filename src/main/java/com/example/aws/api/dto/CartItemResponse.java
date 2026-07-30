package com.example.aws.api.dto;

public class CartItemResponse {
    private String productId;
    private int quantity;
    private double totalCost;
    private boolean partialFilled;
    private int requestedQuantity;
    private long addedAt;

    public CartItemResponse() {
    }

    public CartItemResponse(String productId, int quantity, double totalCost, boolean partialFilled,
                            int requestedQuantity, long addedAt) {
        this.productId = productId;
        this.quantity = quantity;
        this.totalCost = totalCost;
        this.partialFilled = partialFilled;
        this.requestedQuantity = requestedQuantity;
        this.addedAt = addedAt;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public boolean isPartialFilled() {
        return partialFilled;
    }

    public void setPartialFilled(boolean partialFilled) {
        this.partialFilled = partialFilled;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }
}
