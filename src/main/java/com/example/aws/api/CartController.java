package com.example.aws.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.aws.api.dto.AddItemRequest;
import com.example.aws.api.dto.CartItemResponse;
import com.example.aws.api.dto.CartResponse;
import com.example.aws.api.dto.CreateCartRequest;
import com.example.aws.service.CartService;
import com.example.aws.service.cart.CartItem;
import com.example.aws.service.cart.CartStatus;
import com.example.aws.service.cart.ShoppingCart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/carts")
@Tag(name = "Shopping Cart", description = "Shopping cart management endpoints")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    @Operation(summary = "Create a new shopping cart")
    public ResponseEntity<CartResponse> createCart(@RequestBody CreateCartRequest request) {
        try {
            ShoppingCart cart = cartService.createCart(request.getName(), request.getAddress());
            CartResponse response = new CartResponse(
                    cart.getCartId(), cart.getName(), cart.getAddress(),
                    cart.getStatus(), cart.getCreatedAt(), cart.getUpdatedAt()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cart: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{cartId}")
    @Operation(summary = "Get cart details")
    public ResponseEntity<CartResponse> getCart(@PathVariable String cartId) {
        ShoppingCart cart = cartService.getCart(cartId);
        if (cart == null) {
            return ResponseEntity.notFound().build();
        }
        CartResponse response = new CartResponse(
                cart.getCartId(), cart.getName(), cart.getAddress(),
                cart.getStatus(), cart.getCreatedAt(), cart.getUpdatedAt()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{cartId}/items")
    @Operation(summary = "Get all items in a cart")
    public ResponseEntity<List<CartItemResponse>> getCartItems(@PathVariable String cartId) {
        ShoppingCart cart = cartService.getCart(cartId);
        if (cart == null) {
            return ResponseEntity.notFound().build();
        }

        List<CartItem> items = cartService.getCartItems(cartId);
        List<CartItemResponse> responses = items.stream()
                .map(item -> new CartItemResponse(
                        item.getProductId(), item.getQuantity(), item.getTotalCost(),
                        item.isPartialFilled(), item.getRequestedQuantity(), item.getAddedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{cartId}/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartItemResponse> addItemToCart(
            @PathVariable String cartId,
            @RequestBody AddItemRequest request) {
        try {
            CartItem item = cartService.addItemToCart(
                    cartId, request.getProductId(), request.getRequestedQuantity()
            );
            CartItemResponse response = new CartItemResponse(
                    item.getProductId(), item.getQuantity(), item.getTotalCost(),
                    item.isPartialFilled(), item.getRequestedQuantity(), item.getAddedAt()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @PutMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Update item quantity in cart")
    public ResponseEntity<CartItemResponse> updateItemQuantity(
            @PathVariable String cartId,
            @PathVariable String productId,
            @RequestParam int quantity) {
        try {
            CartItem item = cartService.updateItemQuantity(cartId, productId, quantity);
            if (item == null) {
                return ResponseEntity.noContent().build();
            }
            CartItemResponse response = new CartItemResponse(
                    item.getProductId(), item.getQuantity(), item.getTotalCost(),
                    item.isPartialFilled(), item.getRequestedQuantity(), item.getAddedAt()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @DeleteMapping("/{cartId}/items/{productId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<Void> removeItemFromCart(
            @PathVariable String cartId,
            @PathVariable String productId) {
        try {
            cartService.removeItemFromCart(cartId, productId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @PutMapping("/{cartId}/status")
    @Operation(summary = "Update cart status")
    public ResponseEntity<CartResponse> updateCartStatus(
            @PathVariable String cartId,
            @RequestParam CartStatus status) {
        try {
            ShoppingCart cart = cartService.updateCartStatus(cartId, status);
            CartResponse response = new CartResponse(
                    cart.getCartId(), cart.getName(), cart.getAddress(),
                    cart.getStatus(), cart.getCreatedAt(), cart.getUpdatedAt()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @GetMapping
    @Operation(summary = "List carts by status")
    public ResponseEntity<List<CartResponse>> listCartsByStatus(@RequestParam CartStatus status) {
        List<ShoppingCart> carts = cartService.listCartsByStatus(status);
        List<CartResponse> responses = carts.stream()
                .map(cart -> new CartResponse(
                        cart.getCartId(), cart.getName(), cart.getAddress(),
                        cart.getStatus(), cart.getCreatedAt(), cart.getUpdatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
