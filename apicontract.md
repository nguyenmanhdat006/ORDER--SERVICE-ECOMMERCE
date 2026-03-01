# API CONTRACT - ORDER SERVICE

**Backend URL:** `http://localhost:8084`

---

## 🛍️ ORDER APIs

### POST /api/orders
Create order from cart

**Request:**
```typescript
{
  paymentMethod: "CREDIT_CARD" | "PAYPAL" | "COD" | "VNPAY";
  shippingAddress: {
    fullName: string;
    phone: string;
    addressLine1: string;
    addressLine2?: string;
    city: string;
    state: string;
    zipCode: string;
    country: string;
  };
  billingAddress?: Address;  // Optional, use shipping if null
  notes?: string;
}
```

**Response:** OrderResponse (201 CREATED)

### GET /api/orders
Get my orders (paginated)

**Query:** `?page=0&size=20`

### GET /api/orders/{id}
Get order details

### GET /api/orders/number/{orderNumber}
Get order by order number (ORD-20240221-0001)

### PUT /api/orders/{id}/status
Update order status (Admin only)

**Request:**
```typescript
{
  status: "CONFIRMED" | "PROCESSING" | "SHIPPED" | "DELIVERED";
  notes?: string;
}
```

### POST /api/orders/{id}/cancel
Cancel order

**Request:**
```typescript
{
  reason: string;
}
```

### GET /api/orders/search
Search orders (Admin only)

**Query:**
```
?keyword=...
&status=PENDING
&startDate=2024-01-01
&endDate=2024-12-31
&page=0&size=20
```

### GET /api/orders/summary
Get order statistics (Admin only)

---

## 📝 TYPESCRIPT TYPES

```typescript
interface Order {
  id: string;
  orderNumber: string;
  userId: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  paymentMethod: PaymentMethod;
  items: OrderItem[];
  subtotal: number;
  discount: number;
  shipping: number;
  tax: number;
  total: number;
  shippingAddress: Address;
  billingAddress: Address;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  notes?: string;
  orderedAt: string;
  confirmedAt?: string;
  shippedAt?: string;
  deliveredAt?: string;
  createdAt: string;
  updatedAt: string;
}

type OrderStatus = 
  | "PENDING" 
  | "CONFIRMED" 
  | "PROCESSING" 
  | "SHIPPED" 
  | "DELIVERED" 
  | "CANCELLED" 
  | "REFUNDED";

type PaymentStatus = "PENDING" | "PAID" | "FAILED" | "REFUNDED";

type PaymentMethod = 
  | "CREDIT_CARD" 
  | "DEBIT_CARD" 
  | "PAYPAL" 
  | "BANK_TRANSFER" 
  | "CASH_ON_DELIVERY" 
  | "VNPAY" 
  | "MOMO";
```