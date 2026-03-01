# API Testing Guide - Order Service

## 🔐 Prerequisites

1. **Get JWT Token from Keycloak**
```bash
export TOKEN=$(curl -X POST http://localhost:8080/realms/fashion/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=ecommerce-client" \
  -d "username=user@example.com" \
  -d "password=password" \
  -d "grant_type=password" | jq -r '.access_token')

echo $TOKEN
```

2. **Ensure other services are running**
- Cart Service: http://localhost:8081
- Product Service: http://localhost:8082
- Order Service: http://localhost:8084

---

## 📝 Test Scenarios

### 1. Health Check (No Auth)
```bash
curl -X GET http://localhost:8084/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

---

### 2. Create Order from Cart

**Step 1: Add items to cart first (via Cart Service)**
```bash
curl -X POST http://localhost:8081/api/carts/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-001",
    "quantity": 2
  }'
```

**Step 2: Create Order**
```bash
curl -X POST http://localhost:8084/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "CREDIT_CARD",
    "shippingAddress": {
      "fullName": "Nguyen Van A",
      "phone": "0901234567",
      "addressLine1": "123 Nguyen Hue Street",
      "addressLine2": "Floor 5",
      "city": "Ho Chi Minh City",
      "state": "HCM",
      "zipCode": "700000",
      "country": "Vietnam"
    },
    "billingAddress": {
      "fullName": "Nguyen Van A",
      "phone": "0901234567",
      "addressLine1": "456 Le Loi Street",
      "city": "Ho Chi Minh City",
      "state": "HCM",
      "zipCode": "700000",
      "country": "Vietnam"
    },
    "notes": "Please deliver before 5 PM"
  }'
```

**Expected Response:**
```json
{
  "status": 201,
  "message": "Created successfully",
  "data": {
    "id": "uuid-here",
    "orderNumber": "ORD-20260228-0001",
    "userId": "user-id-from-jwt",
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "paymentMethod": "CREDIT_CARD",
    "items": [
      {
        "id": "uuid",
        "productId": "prod-001",
        "productName": "Product Name",
        "quantity": 2,
        "price": 100.00,
        "subtotal": 200.00
      }
    ],
    "subtotal": 200.00,
    "discount": 0.00,
    "shipping": 10.00,
    "tax": 20.00,
    "total": 230.00,
    "shippingAddress": {...},
    "orderedAt": "2026-02-28T23:00:00",
    "createdAt": "2026-02-28T23:00:00"
  }
}
```

---

### 3. Get My Orders (Paginated)

```bash
curl -X GET "http://localhost:8084/api/orders?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "status": 200,
  "message": "Orders retrieved successfully",
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

### 4. Get Order by ID

```bash
# Replace {order-id} with actual UUID
curl -X GET http://localhost:8084/api/orders/{order-id} \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "status": 200,
  "message": "Order retrieved successfully",
  "data": {
    "id": "uuid",
    "orderNumber": "ORD-20260228-0001",
    ...
  }
}
```

---

### 5. Get Order by Order Number

```bash
curl -X GET http://localhost:8084/api/orders/number/ORD-20260228-0001 \
  -H "Authorization: Bearer $TOKEN"
```

---

### 6. Cancel Order

```bash
curl -X POST http://localhost:8084/api/orders/{order-id}/cancel \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Changed my mind"
  }'
```

**Expected Response:**
```json
{
  "status": 200,
  "message": "Success",
  "data": "Order cancelled successfully"
}
```

---

### 7. Update Order Status (Admin Only)

**Get Admin Token First:**
```bash
export ADMIN_TOKEN=$(curl -X POST http://localhost:8080/realms/fashion/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=ecommerce-client" \
  -d "username=admin@example.com" \
  -d "password=admin" \
  -d "grant_type=password" | jq -r '.access_token')
```

**Update Status:**
```bash
curl -X PUT http://localhost:8084/api/orders/{order-id}/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED",
    "notes": "Payment verified"
  }'
```

**Status Progression:**
```bash
# Confirm order
curl -X PUT http://localhost:8084/api/orders/{id}/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "CONFIRMED", "notes": "Payment received"}'

# Start processing
curl -X PUT http://localhost:8084/api/orders/{id}/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "PROCESSING", "notes": "Preparing order"}'

# Ship order
curl -X PUT http://localhost:8084/api/orders/{id}/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "SHIPPED", "notes": "Out for delivery"}'

# Deliver order
curl -X PUT http://localhost:8084/api/orders/{id}/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "DELIVERED", "notes": "Delivered successfully"}'
```

---

### 8. Search Orders (Admin Only)

```bash
curl -X GET "http://localhost:8084/api/orders/search?keyword=ORD&status=PENDING&page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**With Date Range:**
```bash
curl -X GET "http://localhost:8084/api/orders/search?startDate=2026-02-01&endDate=2026-02-28&page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### 9. Get Order Summary (Admin Only)

```bash
curl -X GET http://localhost:8084/api/orders/summary \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected Response:**
```json
{
  "status": 200,
  "message": "Summary retrieved successfully",
  "data": {
    "totalOrders": 15,
    "totalAmount": 5420.00,
    "ordersByStatus": {
      "PENDING": 3,
      "CONFIRMED": 5,
      "PROCESSING": 2,
      "SHIPPED": 3,
      "DELIVERED": 2
    }
  }
}
```

---

## ❌ Error Cases

### 1. Create Order with Empty Cart
```bash
# Response: 400 Bad Request
{
  "status": 400,
  "message": "Cart is empty"
}
```

### 2. Cancel Already Shipped Order
```bash
# Response: 400 Bad Request
{
  "status": 400,
  "message": "Order cannot be cancelled in current status: SHIPPED"
}
```

### 3. Invalid Status Transition
```bash
# Try to go from PENDING to DELIVERED directly
# Response: 400 Bad Request
{
  "status": 400,
  "message": "Invalid status transition from PENDING to DELIVERED"
}
```

### 4. Unauthorized Access
```bash
# Try to update status without admin role
# Response: 403 Forbidden
{
  "status": 403,
  "message": "Access Denied"
}
```

### 5. Order Not Found
```bash
# Response: 404 Not Found
{
  "status": 404,
  "message": "Order not found"
}
```

---

## 🧪 Full Test Flow

```bash
# 1. Health check
curl http://localhost:8084/actuator/health

# 2. Get user token
export TOKEN="your-jwt-token"

# 3. Add items to cart (via Cart Service)
curl -X POST http://localhost:8081/api/carts/items \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"productId":"prod-001","quantity":2}'

# 4. Create order
ORDER_RESPONSE=$(curl -X POST http://localhost:8084/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"paymentMethod":"CREDIT_CARD","shippingAddress":{...}}')

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.data.id')
echo "Created Order ID: $ORDER_ID"

# 5. Get order details
curl -X GET http://localhost:8084/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN"

# 6. Get my orders
curl -X GET "http://localhost:8084/api/orders?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# 7. (Admin) Update status to CONFIRMED
curl -X PUT http://localhost:8084/api/orders/$ORDER_ID/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"status":"CONFIRMED","notes":"Payment verified"}'

# 8. (Admin) Get summary
curl -X GET http://localhost:8084/api/orders/summary \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 🎯 Validation Tests

### Test Validation Errors

```bash
# Missing required fields
curl -X POST http://localhost:8084/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}'

# Expected: 400 with validation errors
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "paymentMethod": "Payment method is required",
    "shippingAddress": "Shipping address is required"
  }
}
```

---

## 📊 Performance Testing

```bash
# Load test với Apache Bench
ab -n 100 -c 10 \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/orders

# Response time monitoring
curl -w "@curl-format.txt" -o /dev/null -s \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8084/api/orders
```

**curl-format.txt:**
```
    time_namelookup:  %{time_namelookup}\n
       time_connect:  %{time_connect}\n
    time_appconnect:  %{time_appconnect}\n
      time_redirect:  %{time_redirect}\n
   time_pretransfer:  %{time_pretransfer}\n
 time_starttransfer:  %{time_starttransfer}\n
                    ----------\n
         time_total:  %{time_total}\n
```

---

**Happy Testing! 🚀**

