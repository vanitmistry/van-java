# Shopping Cart Specification

## Overview

The shopping cart system manages e-commerce transactions via three DynamoDB tables with transactional integrity and inventory reservation.

## Data Model

### ShoppingCart Table
**Primary Key**: `cartId` (String: UUID)

| Field | Type | Notes |
|-------|------|-------|
| `cartId` | String | UUID, primary key |
| `name` | String | Customer name |
| `address` | String | Shipping address |
| `status` | String | pending \| paid \| delivering \| complete |
| `createdAt` | Number | Unix timestamp |
| `updatedAt` | Number | Unix timestamp |

**Indexes**:
- GSI1: `status` (PK) → enables querying carts by status

### Item Table
**Primary Key**: `cartId` (Partition Key), `productId` (Sort Key)

| Field | Type | Notes |
|-------|------|-------|
| `cartId` | String | References ShoppingCart |
| `productId` | String | References Product (composite key) |
| `quantity` | Number | Fulfilled quantity (may be < requested if partial) |
| `totalCost` | Number | IMMUTABLE: quantity × product.cost at add time |
| `partialFilled` | Boolean | true if fulfilled < requested (stock limited) |
| `requestedQuantity` | Number | Original requested qty (if partialFilled=true) |
| `addedAt` | Number | Unix timestamp |

**Constraints**:
- One item per product per cart (composite key enforces uniqueness)
- Delete row if quantity becomes 0
- Items can only be modified when cart.status = "pending"

### Product Table
**Primary Key**: `productId` (String: UUID)

| Field | Type | Notes |
|-------|------|-------|
| `productId` | String | UUID, primary key |
| `name` | String | Product name |
| `description` | String | Product description |
| `cost` | Number | IMMUTABLE unit cost |
| `quantity` | Number | Available inventory (≥ 0, cannot go negative) |
| `reserved` | Number | Stock held by pending carts |
| `createdAt` | Number | Unix timestamp |
| `updatedAt` | Number | Unix timestamp |

**Derived Fields**:
- `available` = quantity - reserved

## Operations

### AddItemToCart(cartId, productId, requestedQuantity)

**Precondition**: cart.status = "pending"

**Logic**:
1. Read Product
2. Calculate `fillQty = min(requestedQuantity, product.quantity - product.reserved)`
3. If `fillQty = 0`: reject with error "Out of stock"
4. Calculate `totalCost = fillQty × product.cost`
5. Set `partialFilled = (fillQty < requestedQuantity)`
6. **Atomic TransactWriteItems**:
   - Put Item(cartId, productId, quantity=fillQty, totalCost, partialFilled, requestedQuantity)
   - Update Product(reserved += fillQty)

**Response**: Item with quantity, totalCost, partialFilled flag

### RemoveItemFromCart(cartId, productId)

**Precondition**: cart.status = "pending"

**Logic**:
1. Read Item (get current quantity)
2. **Atomic TransactWriteItems**:
   - Delete Item(cartId, productId)
   - Update Product(reserved -= quantity)

**Response**: Confirmation of removal

### UpdateItemQuantity(cartId, productId, newQuantity)

**Precondition**: cart.status = "pending"

**Logic**:
1. Read Item (current quantity)
2. Read Product
3. Calculate `delta = newQuantity - current.quantity`
4. Calculate `available = product.quantity - (product.reserved - current.quantity)`
5. If `delta > 0` (increasing qty):
   - If `delta > available`: partial fill logic applies
   - `fillDelta = min(delta, available)`
   - `newQuantity = current + fillDelta`
   - `partialFilled = (newQuantity < requestedQuantity)` (or set based on new request)
6. If `delta < 0` (decreasing qty):
   - Simply reduce quantity and reserved stock
7. If `newQuantity = 0`: delete the Item row instead
8. **Atomic TransactWriteItems**:
   - Update Item (or Delete if qty=0)
   - Update Product(reserved += delta)

**Response**: Updated Item

### GetCartItems(cartId)

**Query**: Items where cartId = requested value

**Response**: List of Items with all fields

### GetCart(cartId)

**Get**: ShoppingCart where cartId = requested value

**Response**: Cart with name, address, status, timestamps

### CreateCart(name, address)

**Logic**:
1. Generate UUID for cartId
2. Set status = "pending"
3. Put ShoppingCart

**Response**: cartId and created cart

### UpdateCartStatus(cartId, newStatus)

**Precondition**:
- newStatus ∈ {pending, paid, delivering, complete}
- Status transitions: pending → paid → delivering → complete (linear)
- Once status ≠ "pending", items are locked (cannot be modified)
- Stock remains reserved permanently after status changes

**Logic**:
1. Read Cart
2. Validate transition is allowed
3. Update Cart(status = newStatus, updatedAt = now)

**Response**: Updated Cart

### GetProduct(productId)

**Get**: Product where productId = requested value

**Response**: Product with all fields including available = (quantity - reserved)

### ListProductsByStatus(status)

**Query**: Carts where status = requested value

**Response**: List of Carts

## Validation Rules

1. **Stock Non-Negativity**: Product.quantity ≥ 0 always (enforce in updates)
2. **Immutable Fields**: totalCost and product.cost cannot be updated
3. **Cart Lifecycle**: Items modifiable only in "pending" state; once transitioned, locked
4. **Partial Fills**: Quantity < requestedQuantity implies partialFilled = true
5. **Reserved Stock**: Cannot exceed available inventory (reserved ≤ quantity)

## Error Handling

| Scenario | Error Code | Message |
|----------|-----------|---------|
| Out of stock | INSUFFICIENT_STOCK | "Only X available of Y requested" |
| Cart not found | NOT_FOUND | "Cart not found" |
| Invalid status transition | INVALID_TRANSITION | "Cannot transition from X to Y" |
| Cannot modify non-pending cart | INVALID_STATE | "Items cannot be modified when cart status is not pending" |
| Product not found | NOT_FOUND | "Product not found" |
| Duplicate item in cart | DUPLICATE | "Product already in cart (use UpdateItemQuantity)" |

## Transactions

All stock mutations use `DynamoDB.TransactWriteItems` to ensure atomicity:
- AddItemToCart: Put Item + Update Product (2 operations)
- RemoveItemFromCart: Delete Item + Update Product (2 operations)
- UpdateItemQuantity: Update/Delete Item + Update Product (2 operations)

Transactional failures (e.g., stock depleted between Read and Write) result in error returned to client; no partial updates.
