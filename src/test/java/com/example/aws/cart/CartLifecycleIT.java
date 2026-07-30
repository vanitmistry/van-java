package com.example.aws.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.aws.service.CartService;
import com.example.aws.service.ProductService;
import com.example.aws.service.cart.CartItem;
import com.example.aws.service.cart.CartStatus;
import com.example.aws.service.cart.Product;
import com.example.aws.service.cart.ShoppingCart;

class CartLifecycleIT extends AbstractShoppingCartIT {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Test
    void testCreateProductAndCart() {
        // Create a product
        Product product = productService.createProduct("Widget", "A useful widget", 19.99);
        assertNotNull(product.getProductId());
        assertEquals("Widget", product.getName());
        assertEquals(19.99, product.getCost());

        // Create a cart
        ShoppingCart cart = cartService.createCart("John Doe", "123 Main St");
        assertNotNull(cart.getCartId());
        assertEquals("John Doe", cart.getName());
        assertEquals(CartStatus.PENDING, cart.getStatus());
    }

    @Test
    void testAddItemToCart() {
        // Setup: Create product with stock
        Product product = productService.createProduct("Gadget", "A cool gadget", 29.99);
        productService.updateProduct(product.getProductId(), new Product(
                product.getProductId(), product.getName(), product.getDescription(),
                product.getCost(), 10, 0, product.getCreatedAt(), product.getUpdatedAt()
        ));

        // Create cart
        ShoppingCart cart = cartService.createCart("Jane Smith", "456 Oak Ave");

        // Add item to cart
        CartItem item = cartService.addItemToCart(cart.getCartId(), product.getProductId(), 3);
        assertNotNull(item);
        assertEquals(3, item.getQuantity());
        assertEquals(89.97, item.getTotalCost(), 0.01);
        assertEquals(false, item.isPartialFilled());
    }

    @Test
    void testPartialFillWhenStockLimited() {
        // Create product with limited stock
        Product product = productService.createProduct("Rare Item", "Limited stock", 49.99);
        productService.updateProduct(product.getProductId(), new Product(
                product.getProductId(), product.getName(), product.getDescription(),
                product.getCost(), 5, 0, product.getCreatedAt(), product.getUpdatedAt()
        ));

        // Create cart and request more than available
        ShoppingCart cart = cartService.createCart("Bob Johnson", "789 Pine Rd");
        CartItem item = cartService.addItemToCart(cart.getCartId(), product.getProductId(), 10);

        // Verify partial fill
        assertEquals(5, item.getQuantity());
        assertEquals(true, item.isPartialFilled());
        assertEquals(10, item.getRequestedQuantity());
        assertEquals(249.95, item.getTotalCost(), 0.01);
    }

    @Test
    void testRemoveItemRestoresStock() {
        // Setup: Create product and add to cart
        Product product = productService.createProduct("Item A", "Test item", 9.99);
        productService.updateProduct(product.getProductId(), new Product(
                product.getProductId(), product.getName(), product.getDescription(),
                product.getCost(), 20, 0, product.getCreatedAt(), product.getUpdatedAt()
        ));

        ShoppingCart cart = cartService.createCart("Alice Wonder", "321 Elm St");
        cartService.addItemToCart(cart.getCartId(), product.getProductId(), 5);

        // Verify stock is reserved
        Product productAfterAdd = productService.getProduct(product.getProductId());
        assertEquals(5, productAfterAdd.getReserved());

        // Remove item
        cartService.removeItemFromCart(cart.getCartId(), product.getProductId());

        // Verify stock is restored
        Product productAfterRemove = productService.getProduct(product.getProductId());
        assertEquals(0, productAfterRemove.getReserved());
    }

    @Test
    void testUpdateItemQuantity() {
        // Setup: Create product and cart with item
        Product product = productService.createProduct("Item B", "Test item", 15.99);
        productService.updateProduct(product.getProductId(), new Product(
                product.getProductId(), product.getName(), product.getDescription(),
                product.getCost(), 25, 0, product.getCreatedAt(), product.getUpdatedAt()
        ));

        ShoppingCart cart = cartService.createCart("Charlie Brown", "555 Walnut St");
        cartService.addItemToCart(cart.getCartId(), product.getProductId(), 5);

        // Update quantity to 8
        CartItem updated = cartService.updateItemQuantity(cart.getCartId(), product.getProductId(), 8);
        assertEquals(8, updated.getQuantity());

        // Verify stock delta is correct
        Product productAfterUpdate = productService.getProduct(product.getProductId());
        assertEquals(8, productAfterUpdate.getReserved());
    }

    @Test
    void testCheckoutLocksCart() {
        // Setup: Create cart with item
        Product product = productService.createProduct("Item C", "Test item", 25.00);
        productService.updateProduct(product.getProductId(), new Product(
                product.getProductId(), product.getName(), product.getDescription(),
                product.getCost(), 15, 0, product.getCreatedAt(), product.getUpdatedAt()
        ));

        ShoppingCart cart = cartService.createCart("Diana Prince", "999 Hero Lane");
        cartService.addItemToCart(cart.getCartId(), product.getProductId(), 3);

        // Checkout (change status from PENDING to PAID)
        ShoppingCart checkedOut = cartService.updateCartStatus(cart.getCartId(), CartStatus.PAID);
        assertEquals(CartStatus.PAID, checkedOut.getStatus());

        // Verify stock remains reserved permanently
        Product productAfterCheckout = productService.getProduct(product.getProductId());
        assertEquals(3, productAfterCheckout.getReserved());

        // Attempt to modify items should fail
        try {
            cartService.addItemToCart(cart.getCartId(), product.getProductId(), 1);
            assertTrue(false, "Should not allow modifying non-pending cart");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("PENDING"));
        }
    }

    @Test
    void testGetCartItems() {
        // Setup: Create cart with multiple items
        Product product1 = productService.createProduct("Item D", "First item", 10.00);
        Product product2 = productService.createProduct("Item E", "Second item", 20.00);

        productService.updateProduct(product1.getProductId(), new Product(
                product1.getProductId(), product1.getName(), product1.getDescription(),
                product1.getCost(), 30, 0, product1.getCreatedAt(), product1.getUpdatedAt()
        ));
        productService.updateProduct(product2.getProductId(), new Product(
                product2.getProductId(), product2.getName(), product2.getDescription(),
                product2.getCost(), 30, 0, product2.getCreatedAt(), product2.getUpdatedAt()
        ));

        ShoppingCart cart = cartService.createCart("Eve Adams", "777 Paradise Dr");
        cartService.addItemToCart(cart.getCartId(), product1.getProductId(), 2);
        cartService.addItemToCart(cart.getCartId(), product2.getProductId(), 3);

        // Retrieve items
        List<CartItem> items = cartService.getCartItems(cart.getCartId());
        assertEquals(2, items.size());
    }

    @Test
    void testStatusTransitions() {
        ShoppingCart cart = cartService.createCart("Frank Ocean", "111 Song St");
        assertEquals(CartStatus.PENDING, cart.getStatus());

        // PENDING -> PAID
        ShoppingCart paid = cartService.updateCartStatus(cart.getCartId(), CartStatus.PAID);
        assertEquals(CartStatus.PAID, paid.getStatus());

        // PAID -> DELIVERING
        ShoppingCart delivering = cartService.updateCartStatus(cart.getCartId(), CartStatus.DELIVERING);
        assertEquals(CartStatus.DELIVERING, delivering.getStatus());

        // DELIVERING -> COMPLETE
        ShoppingCart complete = cartService.updateCartStatus(cart.getCartId(), CartStatus.COMPLETE);
        assertEquals(CartStatus.COMPLETE, complete.getStatus());

        // COMPLETE -> any should fail
        try {
            cartService.updateCartStatus(cart.getCartId(), CartStatus.PAID);
            assertTrue(false, "Should not allow transition from COMPLETE");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Invalid status transition"));
        }
    }
}
