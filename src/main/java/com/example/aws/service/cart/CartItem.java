package com.example.aws.service.cart;

public class CartItem {
    private String cartId;
    private String productId;
    private int quantity;
    private double totalCost;
    private boolean partialFilled;
    private int requestedQuantity;
    private long addedAt;

    public CartItem() {
    }

    public CartItem(String cartId, String productId, int quantity, double totalCost, boolean partialFilled,
                    int requestedQuantity, long addedAt) {
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalCost = totalCost;
        this.partialFilled = partialFilled;
        this.requestedQuantity = requestedQuantity;
        this.addedAt = addedAt;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
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
