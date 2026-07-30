package com.example.aws.api.dto;

public class AddItemRequest {
    private String productId;
    private int requestedQuantity;

    public AddItemRequest() {
    }

    public AddItemRequest(String productId, int requestedQuantity) {
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }
}
