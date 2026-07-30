# Add DynamoDB Shopping Cart

## What

Add a complete shopping cart service backed by DynamoDB with:
- **ShoppingCart** table: manages customer carts with lifecycle (pending → paid → delivering → complete)
- **Item** table: line items in carts with partial fill support and inventory reservation
- **Product** table: product catalog with inventory management
- **CRUD operations**: add/remove/update items, manage cart status, reserve stock
- **Transactional integrity**: atomic operations for inventory changes
- **Swagger UI**: interactive API testing and documentation

## Why

The application currently handles object storage (S3) and messaging (SQS) but lacks transaction/cart capability. E-commerce flows require:
1. Reliable inventory tracking with stock reservation
2. Atomic operations (add item + decrement stock in one transaction)
3. Partial fulfillment support (allow 3 of 5 requested if only 3 available)
4. Persistent cart state across status changes (pending carts lock after checkout)
5. Developer-friendly API testing (Swagger UI)

DynamoDB fits this use case: fast, scalable, transactions via TransactWriteItems, and integrates seamlessly with the existing AWS SDK architecture.

## Scope

### Included
- Three DynamoDB tables (ShoppingCart, Item, Product)
- CRUD service layer with transactional logic
- REST API endpoints for all operations
- Swagger/OpenAPI integration for API documentation
- Integration tests with LocalStack for DynamoDB
- Spring Boot configuration for DynamoDB client

### Excluded
- Payment processing
- Shipping integration
- Order history/analytics
- Admin dashboard
- Multi-currency support

## Non-Goals

- Replace S3 or SQS functionality
- Add user authentication (carts identified by cartId only)
- Add WebSocket/real-time updates

## Success Criteria

1. All three tables created and queryable via SDK
2. Transactions prevent inventory overbooking
3. Partial fills work and are flagged in responses
4. Swagger UI fully documents all endpoints
5. Integration tests pass with LocalStack
6. Build succeeds with Spring Boot 4.1.0
