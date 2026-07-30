# Shopping Cart Implementation Tasks

## Phase 1: Foundation (DynamoDB Setup)

### 1.1 Add DynamoDB Dependencies
**Time**: 15 min

- [x] Update `pom.xml`: verify `software.amazon.awssdk:dynamodb` is available via Spring Boot parent BOM (4.1.0)
- [x] Add `springdoc-openapi-starter-webmvc-ui` for Swagger
- [x] Run `mvn dependency:resolve` to verify no conflicts
- [x] Commit: "Add DynamoDB and Swagger dependencies"

### 1.2 Create DynamoDB Configuration
**Time**: 30 min

- [x] Create `src/main/java/com/example/aws/config/DynamoDbProperties.java`
  - Properties: region, endpoint-override
  - Annotated with `@ConfigurationProperties(prefix="app.dynamodb")`
  
- [x] Create `src/main/java/com/example/aws/config/DynamoDbClientConfig.java`
  - Bean: `DynamoDbAsyncClient` (follows S3ClientConfig/SqsClientConfig pattern)
  - Support endpoint override for LocalStack
  - Credentials provider (test mode)
  
- [x] Update `src/main/resources/application.yml`:
  ```yaml
  app:
    dynamodb:
      region: us-east-1
      endpoint-override: http://localhost:4566
  ```

- [x] Update `src/main/resources/application-local.yml`:
  ```yaml
  app:
    dynamodb:
      region: us-east-1
      endpoint-override: http://localhost:4566
  ```

- [x] Build and verify no errors: `mvn clean compile`
- [x] Commit: "Add DynamoDB client configuration"

### 1.3 Create DynamoDB Table Initializer
**Time**: 45 min

- [x] Create `src/main/java/com/example/aws/config/DynamoDbTableInitializer.java`
  - `@Component` that runs on `@PostConstruct`
  - Methods:
    - `createShoppingCartTable()`: creates with statusIndex GSI
    - `createItemTable()`: composite key (cartId, productId)
    - `createProductTable()`: simple key
  - Idempotent (check if exists before creating)
  - Handles ResourceInUseException gracefully
  
- [x] Test locally with LocalStack running
- [x] Commit: "Add DynamoDB table initializer"

---

## Phase 2: Data Models & Services

### 2.1 Create POJO Data Models
**Time**: 30 min

- [x] Create `src/main/java/com/example/aws/service/cart/ShoppingCart.java`
  - Fields: cartId, name, address, status (enum), createdAt, updatedAt
  - Getters/setters (or Lombok @Data)
  
- [x] Create `src/main/java/com/example/aws/service/cart/CartItem.java`
  - Fields: cartId, productId, quantity, totalCost, partialFilled, requestedQuantity, addedAt
  
- [x] Create `src/main/java/com/example/aws/service/cart/Product.java`
  - Fields: productId, name, description, cost, quantity, reserved, createdAt, updatedAt
  - Method: `getAvailable()` returns quantity - reserved
  
- [x] Create enum `src/main/java/com/example/aws/service/cart/CartStatus.java`
  - Values: PENDING, PAID, DELIVERING, COMPLETE
  
- [x] Commit: "Add shopping cart data models"

### 2.2 Create ProductService
**Time**: 1 hour

- [x] Create `src/main/java/com/example/aws/service/ProductService.java`
  - Inject DynamoDbAsyncClient
  - Methods:
    - `createProduct(name, description, cost)`: generates UUID, returns Product
    - `getProduct(productId)`: reads from DynamoDB
    - `listProducts()`: scans Product table
    - `updateProduct(productId, updates)`: conditional update
  - Error handling: wrap DynamoDB exceptions in ApplicationException
  
- [x] Create unit test: `src/test/java/com/example/aws/service/ProductServiceTest.java`
  - Mock DynamoDbAsyncClient
  - Test getProduct success/not-found
  - Test createProduct UUID generation
  
- [x] Commit: "Add ProductService with CRUD operations"

### 2.3 Create CartService (Part A: Basic CRUD)
**Time**: 1.5 hours

- [x] Create `src/main/java/com/example/aws/service/CartService.java`
  - Inject DynamoDbAsyncClient, ProductService
  - Methods:
    - `createCart(name, address)`: generates UUID, returns cart
    - `getCart(cartId)`: reads from DynamoDB
    - `updateCartStatus(cartId, newStatus)`: validates transition, updates atomically
    - `listCartsByStatus(status)`: queries GSI
  - Validation: throw CartStatusTransitionException on invalid transitions
  
- [x] Create unit test: `src/test/java/com/example/aws/service/CartServiceTest.java`
  - Test createCart, getCart
  - Test status transitions (valid/invalid)
  
- [x] Commit: "Add CartService basic CRUD"

### 2.4 Create CartService (Part B: Item Operations with Transactions)
**Time**: 2 hours

- [x] Implement `addItemToCart(cartId, productId, requestedQuantity)` in CartService
  - Read product, validate stock availability
  - Calculate fillQty, totalCost, partialFilled
  - Build TransactWriteItems: Put Item + Update Product
  - Execute transaction, handle ConditionalCheckFailedException
  - Return CartItem with response details
  
- [x] Implement `removeItemFromCart(cartId, productId)` in CartService
  - Precondition: cart.status = PENDING
  - Read item to get reserved qty
  - Build TransactWriteItems: Delete Item + Update Product
  - Execute transaction
  
- [x] Implement `updateItemQuantity(cartId, productId, newQuantity)` in CartService
  - Precondition: cart.status = PENDING
  - Read item and product
  - Calculate delta, check availability, apply partial fill logic
  - Handle deletion if newQuantity = 0
  - Build TransactWriteItems: Update/Delete Item + Update Product
  - Execute transaction
  
- [x] Create unit test: `src/test/java/com/example/aws/service/CartServiceTransactionTest.java`
  - Mock DynamoDB responses
  - Test successful add with no partial fill
  - Test partial fill scenario (5 requested, 3 available)
  - Test update reducing quantity
  - Test removal restores stock
  
- [x] Commit: "Add CartService item operations with transactions"

### 2.5 Create CartService (Part C: Query Operations)
**Time**: 30 min

- [x] Implement `getCartItems(cartId)` in CartService
  - Query Items where cartId = requested
  - Convert DynamoDB items to CartItem POJOs
  
- [x] Implement `getCartWithProducts(cartId)` in CartService
  - Call getCart, getCartItems
  - For each item, call productService.getProduct
  - Return combined response (cart with full product details)
  
- [x] Unit test: verify item retrieval and product fetching
- [x] Commit: "Add CartService query operations"

---

## Phase 3: REST API & Swagger

### 3.1 Create Request/Response DTOs
**Time**: 30 min

- [x] Create `src/main/java/com/example/aws/api/dto/CreateCartRequest.java`
- [x] Create `src/main/java/com/example/aws/api/dto/AddItemRequest.java`
- [ ] Create `src/main/java/com/example/aws/api/dto/UpdateItemRequest.java`
- [x] Create `src/main/java/com/example/aws/api/dto/CartResponse.java`
- [x] Create `src/main/java/com/example/aws/api/dto/CartItemResponse.java`
- [x] Create `src/main/java/com/example/aws/api/dto/ProductResponse.java`
- [x] Create `src/main/java/com/example/aws/api/dto/ErrorResponse.java`
- [x] Annotate with `@Schema` for Swagger documentation
- [x] Commit: "Add REST API DTOs"

### 3.2 Create CartController
**Time**: 1 hour

- [x] Create `src/main/java/com/example/aws/api/CartController.java`
  - Inject CartService, ProductService
  - Endpoints:
    - `POST /api/carts`: createCart
    - `GET /api/carts/{cartId}`: getCart
    - `PUT /api/carts/{cartId}/status`: updateCartStatus
    - `GET /api/carts/{cartId}/items`: getCartItems
    - `POST /api/carts/{cartId}/items`: addItemToCart
    - `PUT /api/carts/{cartId}/items/{productId}`: updateItemQuantity
    - `DELETE /api/carts/{cartId}/items/{productId}`: removeItemFromCart
  
  - All endpoints return 200 on success, 400/404/409 on error
  - Map service exceptions to HTTP status codes
  - Annotate with `@Tag`, `@Operation`, `@ApiResponse` for Swagger
  
- [ ] Create unit test: `src/test/java/com/example/aws/api/CartControllerTest.java`
  - Mock CartService
  - Test 200/400/404 responses
  
- [x] Commit: "Add CartController with Swagger annotations"

### 3.3 Create ProductController
**Time**: 45 min

- [x] Create `src/main/java/com/example/aws/api/ProductController.java`
  - Inject ProductService
  - Endpoints:
    - `POST /api/products`: createProduct
    - `GET /api/products/{productId}`: getProduct
    - `GET /api/products`: listProducts
  
  - Swagger annotations
  
- [ ] Unit test: ProductControllerTest
- [x] Commit: "Add ProductController"

### 3.4 Create Global Exception Handler
**Time**: 30 min

- [ ] Create `src/main/java/com/example/aws/api/GlobalExceptionHandler.java`
  - `@RestControllerAdvice`
  - Handle CartStatusTransitionException → 400
  - Handle InsufficientStockException → 400
  - Handle CartNotFoundException → 404
  - Handle ProductNotFoundException → 404
  - Handle DynamoDbException → 500
  
- [ ] Return ErrorResponse for all exceptions
- [ ] Commit: "Add global exception handler"

### 3.5 Enable Swagger/OpenAPI
**Time**: 15 min

- [x] Create `src/main/java/com/example/aws/config/SwaggerConfig.java`
  - `@Configuration`
  - `@OpenAPIDefinition` with title, version, description
  - Bean: `SpringDocUtils.getConfig()` for custom config if needed
  
- [x] Verify Swagger UI accessible at `http://localhost:8080/swagger-ui.html`
- [x] Build and test: `mvn clean package`
- [x] Commit: "Add Swagger/OpenAPI configuration"

---

## Phase 4: Integration Testing

### 4.1 Create LocalStack Integration Test Base
**Time**: 30 min

- [ ] Extend `AbstractLocalStackIT` with DynamoDB support
  - Verify LocalStack is running with S3, SQS, and DynamoDB services
  - Ensure tables are created before tests run
  
- [ ] Create `src/test/java/com/example/aws/cart/CartIntegrationTest.java`
  - Annotate with `@SpringBootTest`, `@ActiveProfiles("local")`
  - Inject CartService, ProductService
  
- [ ] Commit: "Add LocalStack integration test setup for DynamoDB"

### 4.2 Integration Tests: Cart Lifecycle
**Time**: 1.5 hours

- [ ] Test: CreateCart → GetCart
- [ ] Test: CreateProduct → GetProduct
- [ ] Test: AddItem → GetCartItems (full flow)
- [ ] Test: AddItem with partial fill (5 requested, 3 available)
- [ ] Test: UpdateItem increasing quantity (partial fill scenario)
- [ ] Test: UpdateItem decreasing quantity (restore stock)
- [ ] Test: RemoveItem (restore all stock)
- [ ] Test: CheckoutCart (status: PENDING → PAID)
- [ ] Test: Cannot modify items after checkout
- [ ] Test: Stock stays reserved after checkout
- [ ] Commit: "Add comprehensive cart integration tests"

### 4.3 Integration Tests: Error Cases
**Time**: 1 hour

- [ ] Test: AddItem with zero available stock → error
- [ ] Test: RemoveItem from non-existent cart → 404
- [ ] Test: UpdateItem to zero quantity → delete item
- [ ] Test: Invalid status transition → error
- [ ] Test: Concurrent adds to same product (transaction isolation)
- [ ] Commit: "Add integration tests for error scenarios"

---

## Phase 5: Documentation & Polish

### 5.1 Add Code Documentation
**Time**: 30 min

- [ ] Add JavaDoc to all public service methods
- [ ] Add inline comments for complex transaction logic
- [ ] Commit: "Add service layer documentation"

### 5.2 Build & Full Test
**Time**: 30 min

- [ ] Run `mvn clean verify` (all tests)
- [ ] Verify Swagger UI shows all endpoints
- [ ] Test one endpoint manually in Swagger UI
- [ ] Verify LocalStack integration tests pass
- [ ] Commit: "Verify all tests pass, Swagger UI functional"

### 5.3 Update README
**Time**: 15 min

- [ ] Add DynamoDB section to project README
- [ ] Document how to access Swagger UI
- [ ] Document sample curl commands for cart operations
- [ ] Commit: "Update README with shopping cart API docs"

---

## Summary

**Total estimated time**: 10-12 hours (assuming no major blockers)

**Breakdown by phase**:
- Phase 1 (Foundation): ~1.5 hours
- Phase 2 (Services): ~5 hours
- Phase 3 (API): ~3 hours
- Phase 4 (Testing): ~3 hours
- Phase 5 (Docs): ~1 hour

**Key checkpoints**:
- After 1.3: LocalStack tables created successfully
- After 2.2: ProductService unit tests pass
- After 2.4: Transaction logic tested and working
- After 3.5: Swagger UI accessible and documented
- After 4.2: All integration tests pass
