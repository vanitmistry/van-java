package com.example.aws.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.aws.api.dto.ProductRequest;
import com.example.aws.api.dto.ProductResponse;
import com.example.aws.service.ProductService;
import com.example.aws.service.cart.Product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Product management endpoints")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductRequest request) {
        try {
            Product product = productService.createProduct(
                    request.getName(), request.getDescription(), request.getCost()
            );
            ProductResponse response = mapToResponse(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create product: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product details")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String productId) {
        Product product = productService.getProduct(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToResponse(product));
    }

    @GetMapping
    @Operation(summary = "List all products")
    public ResponseEntity<List<ProductResponse>> listProducts() {
        List<Product> products = productService.listProducts();
        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getCost(),
                product.getQuantity(),
                product.getReserved(),
                product.getAvailable(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
