# Shopping Cart Implementation Design

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│           Spring Boot 4.1.0 Application                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │         REST API Layer (@RestController)         │  │
│  │  • CartController                                │  │
│  │  • ProductController                             │  │
│  │  • (Swagger/OpenAPI auto-documented)             │  │
│  └──────────┬───────────────────────────────────────┘  │
│             │                                           │
│  ┌──────────▼───────────────────────────────────────┐  │
│  │         Service Layer (@Service)                  │  │
│  │  • CartService                                   │  │
│  │  • ProductService                                │  │
│  │  • (Business logic, transactions)                │  │
│  └──────────┬───────────────────────────────────────┘  │
│             │                                           │
│  ┌──────────▼───────────────────────────────────────┐  │
│  │    DynamoDB Client / Configuration               │  │
│  │  • DynamoDbClientConfig (@Configuration)         │  │
│  │  • DynamoDbProperties (region, endpoint)         │  │
│  │  • TableInitializer (onCreate, setup tables)     │  │
│  └──────────┬───────────────────────────────────────┘  │
│             │                                           │
│             ▼                                           │
│     AWS SDK v2 (software.amazon.awssdk)                │
│            DynamoDbClient                              │
│             │                                           │
└─────────────┼───────────────────────────────────────────┘
              │
    ┌─────────▼─────────┐
    │   DynamoDB        │
    │  (LocalStack)     │
    └───────────────────┘
```

## Package Structure

```
src/main/java/com/example/aws/
├── config/
│   ├── DynamoDbClientConfig.java          [NEW]
│   ├── DynamoDbProperties.java             [NEW]
│   ├── DynamoDbTableInitializer.java       [NEW]
│   └── ... (existing S3/SQS configs)
│
├── service/
│   ├── CartService.java                    [NEW]
│   ├── ProductService.java                 [NEW]
│   ├── cart/
│   │   ├── ShoppingCart.java               [NEW]
│   │   ├── CartItem.java                   [NEW]
│   │   └── Product.java                    [NEW]
│   └── ... (existing S3/SQS services)
│
└── api/
    ├── CartController.java                 [NEW]
    └── ProductController.java              [NEW]
```

## Key Design Decisions

### 1. DynamoDB Client Bean
Follow existing pattern from S3ClientConfig/SqsClientConfig:
- Single `@Configuration` class that creates `DynamoDbAsyncClient` bean
- Supports endpoint override for LocalStack testing
- Region configured via `DynamoDbProperties` (yaml config)
- Credentials use `AwsBasicCredentials` for local testing

**Why async client**: Better performance for high-throughput cart operations and leverages Spring's reactive ecosystem if needed in future.

### 2. Table Initialization
Implement `DynamoDbTableInitializer` (runs on `@PostConstruct`):
- Creates tables if they don't exist (idempotent)
- Sets up GSI on ShoppingCart.status
- Handles LocalStack vs AWS differences
- Used in integration tests

### 3. Service Layer
**CartService**:
- Handles all Item + Cart operations
- TransactWriteItems logic (add, remove, update item)
- Stock reservation and validation
- Partial fill calculations
- Converts SDK responses to POJOs

**ProductService**:
- CRUD for products
- Inventory queries
- Stock availability checks (product.quantity - product.reserved)

### 4. Data Models (POJOs)
```java
// Cart entity
public class ShoppingCart {
    String cartId;
    String name;
    String address;
    CartStatus status;  // enum: PENDING, PAID, DELIVERING, COMPLETE
    long createdAt;
    long updatedAt;
}

// Line item entity
public class CartItem {
    String cartId;
    String productId;
    int quantity;
    double totalCost;
    boolean partialFilled;
    int requestedQuantity;  // if partialFilled
    long addedAt;
}

// Product entity
public class Product {
    String productId;
    String name;
    String description;
    double cost;
    int quantity;
    int reserved;
    long createdAt;
    long updatedAt;
    
    public int getAvailable() { return quantity - reserved; }
}
```

### 5. REST API Design
**Endpoints**:
```
POST   /api/carts                           Create cart
GET    /api/carts/{cartId}                 Get cart details
PUT    /api/carts/{cartId}/status          Update cart status
GET    /api/carts/{cartId}/items           List items in cart
POST   /api/carts/{cartId}/items           Add item to cart
PUT    /api/carts/{cartId}/items/{productId}  Update item quantity
DELETE /api/carts/{cartId}/items/{productId}  Remove item from cart

GET    /api/products/{productId}           Get product
GET    /api/products                        List all products
POST   /api/products                        Create product
```

**Request/Response DTOs**:
```java
// AddItemRequest
{ productId: "...", requestedQuantity: 5 }

// ItemResponse
{ 
    productId: "...", 
    quantity: 3,                  // fulfilled qty
    totalCost: 29.97,
    partialFilled: true,          // only 3 of 5 available
    requestedQuantity: 5,
    addedAt: 1234567890
}

// CartResponse
{
    cartId: "...",
    name: "John Doe",
    address: "123 Main St",
    status: "pending",
    items: [ ... ],               // nested array
    createdAt: 1234567890,
    updatedAt: 1234567890
}
```

### 6. Swagger/OpenAPI Configuration
- Add `springdoc-openapi-starter-webmvc-ui` dependency
- Auto-scans `@RestController` and `@GetMapping`, etc.
- Accessible at `/swagger-ui.html`
- Includes full request/response schemas from DTOs
- DTOs annotated with `@Schema` for clarity

### 7. Transaction Handling
DynamoDB `TransactWriteItems` in CartService:
```java
TransactWriteItemsRequest req = TransactWriteItemsRequest.builder()
    .transactItems(
        TransactWriteItem.builder()
            .put(Put.builder()...build())    // Put Item
            .build(),
        TransactWriteItem.builder()
            .update(Update.builder()...build())  // Update Product
            .build()
    )
    .build();

dynamoDbClient.transactWriteItems(req);
```

Failures throw `DynamoDbException` → caught and re-thrown as application exception with user-friendly message.

### 8. Integration with Existing App
- DynamoDB config follows same pattern as S3/SQS clients
- Reuses LocalStack for testing (already in pom.xml testcontainers)
- No changes to existing S3StorageService or SQS messaging
- Adds new `app.dynamodb.*` properties to application.yml

### 9. Testing Strategy
**Unit tests**:
- CartService transaction logic (mocked DynamoDB client)
- Stock reservation calculations
- Partial fill logic

**Integration tests** (LocalStack):
- Full flow: CreateCart → AddItem → UpdateItem → Checkout
- Stock validation and reservation
- Transaction atomicity
- Error cases (out of stock, invalid transitions)

Extends existing `AbstractLocalStackIT` pattern.

## Error Handling Strategy

Map DynamoDB exceptions to domain errors:
```
ConditionalCheckFailedException → INVALID_STATE ("Stock exhausted" or "Cart already paid")
ResourceNotFoundException → NOT_FOUND
ValidationException → BAD_REQUEST
TransactionConflictException → CONFLICT ("Operation failed due to concurrent update")
```

All endpoints return standard error response:
```json
{
    "errorCode": "INSUFFICIENT_STOCK",
    "message": "Only 3 available of 5 requested",
    "details": { ... }
}
```

## Configuration

**application.yml**:
```yaml
app:
  dynamodb:
    region: us-east-1
    endpoint-override: http://localhost:4566     # LocalStack
    
  s3: ...    # existing
  sqs: ...   # existing
```

**Dependencies** (pom.xml):
```xml
<!-- DynamoDB SDK already in spring-boot-starter-parent BOM for AWS -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb</artifactId>
</dependency>

<!-- Swagger -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.x</version>
</dependency>
```

## Deployment Considerations

1. **IAM Permissions** (AWS production):
   - `dynamodb:PutItem`, `dynamodb:GetItem`, `dynamodb:UpdateItem`, `dynamodb:DeleteItem`
   - `dynamodb:Query` for status lookups
   - `dynamodb:TransactWriteItems`

2. **Table Capacity**:
   - On-demand billing (pay-per-request) for simplicity initially
   - Can switch to provisioned if costs become concern

3. **Backup/Recovery**:
   - Enable point-in-time recovery (AWS console)
   - No application-level backup logic needed

4. **Monitoring**:
   - CloudWatch metrics for read/write throughput
   - Can add Micrometer integration later
