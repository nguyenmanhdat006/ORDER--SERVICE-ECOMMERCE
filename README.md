# Order Service - E-commerce Microservice

Order Management Service cho hệ thống E-commerce Fashion

## Tech Stack

- **Framework:** Spring Boot 3.2.2
- **Database:** PostgreSQL 15
- **Security:** OAuth2 + Keycloak
- **Build Tool:** Maven
- **Java Version:** 17

## Dependencies

- Spring Web
- Spring Data JPA
- Spring Security + OAuth2 Resource Server
- Spring WebFlux (WebClient)
- PostgreSQL Driver
- Lombok
- MapStruct
- Validation
- Actuator

## Architecture

```
Order Service
├── Entities: Order, OrderItem, OrderStatusHistory
├── Services: OrderService, OrderItemService, OrderStatusService
├── Clients: CartServiceClient, ProductServiceClient
├── Controller: OrderController
└── Security: JWT-based authentication via Keycloak
```

## API Endpoints

### User Endpoints (Authenticated)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create order from cart |
| GET | `/api/orders` | Get current user's orders (paginated) |
| GET | `/api/orders/{id}` | Get order details |
| GET | `/api/orders/number/{orderNumber}` | Get order by order number |
| POST | `/api/orders/{id}/cancel` | Cancel order |

### Admin Endpoints (Role: ADMIN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/api/orders/{id}/status` | Update order status |
| GET | `/api/orders/search` | Search orders with filters |
| GET | `/api/orders/summary` | Get order statistics |

## Security

- All endpoints require JWT authentication
- Admin endpoints require `ADMIN` role
- JWT issued by Keycloak realm: `fashion`

## Database Schema

### orders
- id (UUID, PK)
- order_number (VARCHAR, UNIQUE)
- user_id (VARCHAR)
- status (VARCHAR)
- payment_status (VARCHAR)
- payment_method (VARCHAR)
- subtotal, discount, shipping, tax, total (DECIMAL)
- shipping_address, billing_address (JSONB)
- customer_name, customer_email, customer_phone
- notes (TEXT)
- ordered_at, confirmed_at, shipped_at, delivered_at
- created_at, updated_at

### order_items
- id (UUID, PK)
- order_id (UUID, FK)
- product_id, product_variant_id
- product_name, product_image_url (snapshot)
- quantity (INT)
- price, subtotal (DECIMAL)
- created_at

### order_status_history
- id (UUID, PK)
- order_id (UUID, FK)
- status (VARCHAR)
- notes (TEXT)
- changed_by (VARCHAR)
- changed_at (TIMESTAMP)

## Order Status Flow

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
   ↓          ↓           ↓
CANCELLED  CANCELLED  CANCELLED
```

## Getting Started

### Prerequisites

- Java 17
- Maven 3.6+
- PostgreSQL 15
- Keycloak (running on localhost:8080)

### Run with Docker Compose

```bash
# Start database and service
docker-compose up -d

# Check logs
docker-compose logs -f order-service
```

### Run locally

```bash
# Start PostgreSQL
docker-compose up -d orderdb

# Run application
mvn spring-boot:run
```

### Configuration

Edit `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orderdb
    username: postgres
    password: password
  
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/fashion
```

## Business Logic

### Create Order Flow

1. Get userId from JWT token
2. Fetch cart from Cart Service
3. Validate cart is not empty
4. Generate unique order number (ORD-YYYYMMDD-XXXX)
5. Validate stock for all items via Product Service
6. Create order with PENDING status
7. Create order items (snapshot prices)
8. Reduce stock for all products
9. Calculate totals (subtotal + tax + shipping - discount)
10. Save order to database
11. Add status history entry
12. Clear cart via Cart Service
13. Return order response

### Cancel Order Flow

1. Validate order exists and belongs to user
2. Validate order can be cancelled (PENDING or CONFIRMED only)
3. Update status to CANCELLED
4. Restore product stock
5. Add status history entry


## Service Dependencies

- **Cart Service** (port 8081): Get cart, clear cart
- **Product Service** (port 8082): Check stock, reduce stock
- **User Service** (port 8083): Get user info
- **Keycloak** (port 8080): JWT authentication

## 📌 Notes

- Order numbers are unique and auto-generated
- Product prices are snapshot at order time
- Stock is validated and reduced atomically
- Order status transitions are validated
- Cart is automatically cleared after order creation
- Tax is calculated as 10% of subtotal
- Shipping is flat rate ($10)

