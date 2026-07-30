# AWS Service Application with Shopping Cart

Spring Boot 4.1.0 application demonstrating AWS service integration including S3 storage, SQS messaging, and DynamoDB-backed shopping cart with inventory management.

## Tech Stack

- **Framework**: Spring Boot 4.1.0 (Spring Framework 7)
- **Language**: Java 21
- **Database**: DynamoDB (with LocalStack for development)
- **AWS SDK**: AWS SDK v2 (software.amazon.awssdk)
- **API Documentation**: Swagger/OpenAPI (springdoc-openapi)
- **Testing**: JUnit 5, Mockito, TestContainers, LocalStack

## Features

### 1. S3 Storage Service
- Read/write objects to S3
- LocalStack support for testing
- Endpoint override for local development

### 2. SQS Messaging
- Send messages to SQS queues
- Receive and process messages
- Support for outbound and inbound queues

### 3. **DynamoDB Shopping Cart** (NEW)
- **Three-table data model**: ShoppingCart, Item, Product
- **Full cart lifecycle**: pending → paid → delivering → complete
- **Inventory management**:
  - Stock reservation on item addition
  - Permanent reservation after checkout
  - Partial fill support (allow partial quantities if stock limited)
  - Stock cannot go negative (enforced)
- **Atomic transactions**: All stock mutations use DynamoDB TransactWriteItems
- **CRUD Operations**:
  - Create/retrieve/update/delete carts
  - Add/remove/update items with transaction safety
  - Product catalog management
  - List carts by status (via GSI)

## Getting Started

### Prerequisites
- Java 21+
- Docker (for LocalStack)
- Maven 3.8+

### Run with LocalStack (Development)

1. **Start LocalStack**:
   ```bash
   docker run -d --name localstack -p 4566:4566 localstack/localstack:4.4.0
   ```

2. **Build and run**:
   ```bash
   mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
   ```

3. **Access Swagger UI**:
   ```
   http://localhost:8080/swagger-ui.html
   ```

### Run Tests
```bash
# All tests (unit + integration)
mvn clean verify

# Unit tests only
mvn clean test

# Integration tests only
mvn clean verify -Dgroups=integration
```

## API Documentation

### Swagger/OpenAPI
Once the application is running, access the interactive API documentation:
```
http://localhost:8080/swagger-ui.html
```

### Shopping Cart Endpoints

#### Create Cart
```bash
POST /api/carts
Content-Type: application/json

{
  "name": "John Doe",
  "address": "123 Main St"
}

# Response (201 Created)
{
  "cartId": "uuid",
  "name": "John Doe",
  "address": "123 Main St",
  "status": "PENDING",
  "createdAt": 1234567890000,
  "updatedAt": 1234567890000
}
```

#### Add Item to Cart
```bash
POST /api/carts/{cartId}/items
Content-Type: application/json

{
  "productId": "product-uuid",
  "requestedQuantity": 5
}

# Response (201 Created)
{
  "productId": "product-uuid",
  "quantity": 5,
  "totalCost": 99.95,
  "partialFilled": false,
  "requestedQuantity": 5,
  "addedAt": 1234567890000
}

# If stock limited (partial fill)
{
  "productId": "product-uuid",
  "quantity": 3,                # Only 3 available
  "totalCost": 59.97,
  "partialFilled": true,        # Flag indicates partial fill
  "requestedQuantity": 5,       # Original request
  "addedAt": 1234567890000
}
```

#### Get Cart Items
```bash
GET /api/carts/{cartId}/items

# Response (200 OK)
[
  {
    "productId": "product-id-1",
    "quantity": 2,
    "totalCost": 39.98,
    "partialFilled": false,
    "requestedQuantity": 2,
    "addedAt": 1234567890000
  },
  ...
]
```

#### Update Item Quantity
```bash
PUT /api/carts/{cartId}/items/{productId}?quantity=10

# Response (200 OK) - updated item with new quantity
```

#### Remove Item from Cart
```bash
DELETE /api/carts/{cartId}/items/{productId}

# Response (204 No Content)
```

#### Update Cart Status
```bash
PUT /api/carts/{cartId}/status?status=PAID

# Response (200 OK) - updated cart with new status
# Valid transitions: PENDING → PAID → DELIVERING → COMPLETE
# Items can only be modified when status = PENDING
```

#### List Carts by Status
```bash
GET /api/carts?status=PAID

# Response (200 OK)
[
  {
    "cartId": "uuid-1",
    "name": "Customer 1",
    "address": "Address 1",
    "status": "PAID",
    ...
  },
  ...
]
```

### Product Endpoints

#### Create Product
```bash
POST /api/products
Content-Type: application/json

{
  "name": "Widget",
  "description": "A useful widget",
  "cost": 19.99
}

# Response (201 Created)
{
  "productId": "uuid",
  "name": "Widget",
  "description": "A useful widget",
  "cost": 19.99,
  "quantity": 0,        # Initial inventory
  "reserved": 0,        # Stock held by pending carts
  "available": 0,       # quantity - reserved
  "createdAt": 1234567890000,
  "updatedAt": 1234567890000
}
```

#### Get Product
```bash
GET /api/products/{productId}

# Response (200 OK) - product details
```

#### List Products
```bash
GET /api/products

# Response (200 OK) - array of all products
```

## Data Model

### ShoppingCart Table
| Field | Type | Notes |
|-------|------|-------|
| `cartId` | String (PK) | UUID |
| `name` | String | Customer name |
| `address` | String | Shipping address |
| `status` | String | PENDING, PAID, DELIVERING, COMPLETE |
| `createdAt` | Number | Unix timestamp |
| `updatedAt` | Number | Unix timestamp |

**Index**: `statusIndex` (GSI) for querying by status

### Item Table
| Field | Type | Notes |
|-------|------|-------|
| `cartId` | String (PK) | References ShoppingCart |
| `productId` | String (SK) | References Product (composite key) |
| `quantity` | Number | Fulfilled quantity (may be < requested) |
| `totalCost` | Number | IMMUTABLE: qty × product.cost |
| `partialFilled` | Boolean | true if fulfilled < requested |
| `requestedQuantity` | Number | Original request (if partialFilled) |
| `addedAt` | Number | Unix timestamp |

### Product Table
| Field | Type | Notes |
|-------|------|-------|
| `productId` | String (PK) | UUID |
| `name` | String | Product name |
| `description` | String | Product description |
| `cost` | Number | IMMUTABLE unit cost |
| `quantity` | Number | Available inventory (≥ 0) |
| `reserved` | Number | Stock held by pending carts |
| `createdAt` | Number | Unix timestamp |
| `updatedAt` | Number | Unix timestamp |

## Configuration

### application.yml (Production)
```yaml
spring:
  application:
    name: s3-storage-service

app:
  s3:
    bucket: s3-storage-service-bucket
    region: us-east-1
  sqs:
    out-queue-name: out-queue
    in-queue-name: in-queue
    region: us-east-1
  dynamodb:
    region: us-east-1
```

### application-local.yml (LocalStack)
```yaml
app:
  s3:
    endpoint-override: http://localhost:4566
  sqs:
    endpoint-override: http://localhost:4566
  dynamodb:
    endpoint-override: http://localhost:4566
```

## Key Behaviors

### Inventory Management
- **Stock Reservation**: When items are added to a cart, stock is immediately reserved
- **Partial Fills**: If requested quantity exceeds available stock, the maximum available is added and `partialFilled` is flagged
- **No Negative Stock**: Stock adjustments ensure quantity never goes below 0
- **Permanent Reservation**: Once cart status changes from PENDING, stock remains reserved permanently

### Cart Lifecycle
- **PENDING**: Initial state, items can be added/removed/updated
- **PAID**: Customer has paid, items are locked, stock is permanently reserved
- **DELIVERING**: In transit
- **COMPLETE**: Delivered, no further changes allowed

### Transactional Safety
- All stock mutations use DynamoDB `TransactWriteItems`
- Ensures atomicity: if item update fails, stock adjustment rolls back
- No partial updates possible

## Error Handling

Standard error response format:
```json
{
  "errorCode": "INSUFFICIENT_STOCK",
  "message": "Only 3 available of 5 requested"
}
```

Common error codes:
- `NOT_FOUND`: Cart or product not found (404)
- `INSUFFICIENT_STOCK`: Not enough stock available (400)
- `INVALID_TRANSITION`: Invalid status transition (400)
- `INVALID_STATE`: Operation not allowed in current state (400)
- `INTERNAL_ERROR`: Server-side error (500)

## Development

### Build
```bash
mvn clean package
```

### Run Integration Tests
```bash
mvn clean verify
```

The integration tests use LocalStack to create real DynamoDB tables and test the full workflow.

### Code Structure
```
src/main/java/com/example/aws/
├── config/
│   ├── DynamoDbClientConfig.java
│   ├── DynamoDbProperties.java
│   ├── DynamoDbTableInitializer.java
│   ├── S3ClientConfig.java
│   ├── SqsClientConfig.java
│   └── SwaggerConfig.java
├── api/
│   ├── CartController.java
│   ├── ProductController.java
│   ├── GlobalExceptionHandler.java
│   └── dto/
│       ├── CreateCartRequest.java
│       ├── CartResponse.java
│       ├── AddItemRequest.java
│       ├── CartItemResponse.java
│       ├── ProductRequest.java
│       ├── ProductResponse.java
│       └── ErrorResponse.java
├── service/
│   ├── CartService.java
│   ├── ProductService.java
│   ├── S3StorageService.java
│   ├── InboundQueueReceiver.java
│   ├── OutboundQueueSender.java
│   └── cart/
│       ├── ShoppingCart.java
│       ├── CartItem.java
│       ├── Product.java
│       └── CartStatus.java
└── AwsServiceApplication.java
```

## Future Enhancements

- [ ] Payment processing integration
- [ ] Shipping integration
- [ ] Order history and analytics
- [ ] Multi-currency support
- [ ] User authentication
- [ ] Wishlist functionality
- [ ] Coupon/discount system
- [ ] Real-time inventory updates via WebSocket

## License

MIT License
