# KAFKA MIGRATION - GITHUB COPILOT GUIDE

**Migrate from REST API to Event-Driven Architecture**

---

## 🎯 OVERVIEW

**Current (REST API):**
```
Order Service → WebClient.block() → Cart/Payment/Notification Services
```

**Target (Event-Driven):**
```
Order Service → Kafka Event → Consumers (Cart/Payment/Notification)
```

**Total Files:** ~40 files  
**Time:** 6-8 hours  
**Services to modify:** 6 services (Order, Payment, User, Cart, Notification, Shipping)

---

## 📁 PROJECT STRUCTURE

```
kafka-events/                          # New shared library
├── pom.xml
└── src/main/java/com/ecommerce/events/
    ├── OrderCreatedEvent.java
    ├── OrderConfirmedEvent.java
    ├── PaymentSuccessEvent.java
    ├── UserRegisteredEvent.java
    └── ProductStockLowEvent.java

order-service/
├── config/
│   ├── KafkaProducerConfig.java      # NEW
│   └── KafkaTopicConfig.java         # NEW
├── event/
│   └── OrderEventProducer.java       # NEW
└── service/
    └── OrderService.java              # MODIFY

payment-service/
├── config/
│   ├── KafkaProducerConfig.java      # NEW
│   ├── KafkaConsumerConfig.java      # NEW
│   └── KafkaTopicConfig.java         # NEW
├── event/
│   ├── PaymentEventProducer.java     # NEW
│   └── OrderEventConsumer.java       # NEW
└── service/
    └── PaymentService.java            # MODIFY

cart-service/
├── config/
│   └── KafkaConsumerConfig.java      # NEW
├── event/
│   └── OrderEventConsumer.java       # NEW
└── service/
    └── CartService.java               # MODIFY

notification-service/
├── config/
│   └── KafkaConsumerConfig.java      # NEW
├── event/
│   └── EventConsumer.java            # NEW (all events)
└── service/
    └── EmailService.java              # MODIFY
```

---

## 🚀 PHASE 1: Setup Kafka Infrastructure (30 min)

### Step 1.1: Docker Compose

**File:** `docker-compose.yml`

**Prompt:**
```yaml
# Add to existing docker-compose.yml

# Zookeeper for Kafka coordination
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  container_name: zookeeper
  ports:
    - "2181:2181"
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181
    ZOOKEEPER_TICK_TIME: 2000
  networks:
    - ecommerce-network

# Kafka Broker 1
kafka-1:
  image: confluentinc/cp-kafka:7.5.0
  container_name: kafka-1
  depends_on:
    - zookeeper
  ports:
    - "9092:9092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
  networks:
    - ecommerce-network

# Kafka UI (for monitoring)
kafka-ui:
  image: provectuslabs/kafka-ui:latest
  container_name: kafka-ui
  depends_on:
    - kafka-1
  ports:
    - "8090:8080"
  environment:
    KAFKA_CLUSTERS_0_NAME: ecommerce-cluster
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka-1:9092
    KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
  networks:
    - ecommerce-network

networks:
  ecommerce-network:
    driver: bridge
```

**Start Kafka:**
```bash
docker-compose up -d zookeeper kafka-1 kafka-ui
# Access UI: http://localhost:8090
```

---

## 📦 PHASE 2: Create Shared Event Library (1 hour)

### Step 2.1: Create New Maven Module

**File:** `kafka-events/pom.xml`

**Prompt:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.ecommerce</groupId>
    <artifactId>kafka-events</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
            <scope>provided</scope>
        </dependency>
        
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.3</version>
        </dependency>
    </dependencies>
</project>
```

---

### Step 2.2: Event Classes

#### File: `OrderCreatedEvent.java`

**Prompt:**
```java
package com.ecommerce.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a new order is created.
 * 
 * Consumers:
 * - Cart Service: Clear user's cart
 * - Payment Service: Create payment intent
 * - Shipping Service: Calculate shipping
 * - Notification Service: Send order confirmation email
 * - Analytics Service: Track order metrics
 * 
 * Topic: order.created
 * Partitions: 3 (partitioned by orderId)
 * Replicas: 2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    
    // Event metadata
    private String eventId;              // UUID for idempotency
    private String eventType;            // "ORDER_CREATED"
    private LocalDateTime timestamp;     // Event creation time
    
    // Order data
    private String orderId;
    private String orderNumber;
    private String userId;
    private String userEmail;
    
    // Order items
    private List<OrderItemDto> items;
    
    // Amounts
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shipping;
    private BigDecimal tax;
    private BigDecimal total;
    
    // Addresses
    private AddressDto shippingAddress;
    private AddressDto billingAddress;
    
    // Payment
    private String paymentMethod;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDto {
        private String productId;
        private String productName;
        private String productImageUrl;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressDto {
        private String fullName;
        private String phone;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }
}
```

#### File: `PaymentSuccessEvent.java`

**Prompt:**
```java
package com.ecommerce.events;

/**
 * Event published when payment is successful.
 * 
 * Consumers:
 * - Order Service: Update order status to CONFIRMED
 * - Notification Service: Send payment receipt email
 * - Analytics Service: Track revenue
 * 
 * Topic: payment.success
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {
    private String eventId;
    private String eventType;            // "PAYMENT_SUCCESS"
    private LocalDateTime timestamp;
    
    private String paymentId;
    private String orderId;
    private String orderNumber;
    private String userId;
    
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String transactionId;
}
```

#### File: `UserRegisteredEvent.java`

**Prompt:**
```java
package com.ecommerce.events;

/**
 * Event published when a new user registers.
 * 
 * Consumers:
 * - Notification Service: Send welcome email
 * - Marketing Service: Add to campaign
 * - Analytics Service: Track user acquisition
 * 
 * Topic: user.registered
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisteredEvent {
    private String eventId;
    private String eventType;            // "USER_REGISTERED"
    private LocalDateTime timestamp;
    
    private String userId;
    private String email;
    private String fullName;
    private String phone;
}
```

#### File: `OrderConfirmedEvent.java`

**Prompt:**
```java
package com.ecommerce.events;

/**
 * Event published when order payment is confirmed.
 * 
 * Consumers:
 * - Order Service: Update status
 * - Notification Service: Send confirmation email
 * 
 * Topic: order.confirmed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderConfirmedEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    
    private String orderId;
    private String orderNumber;
    private String userId;
}
```

**Build & Install:**
```bash
cd kafka-events
mvn clean install
```

---

## 🔧 PHASE 3: Order Service (Producer) (1.5 hours)

### Step 3.1: Add Dependencies

**File:** `order-service/pom.xml`

**Add:**
```xml
<dependencies>
    <!-- Existing dependencies... -->
    
    <!-- Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- Shared Events -->
    <dependency>
        <groupId>com.ecommerce</groupId>
        <artifactId>kafka-events</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

---

### Step 3.2: Kafka Producer Configuration

**File:** `order-service/config/KafkaProducerConfig.java`

**Prompt:**
```java
package com.ecommerce.orderservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer Configuration for Order Service.
 * 
 * Produces events to:
 * - order.created
 * - order.confirmed
 * - order.shipped
 * - order.cancelled
 * 
 * Configuration:
 * - JSON serialization for event objects
 * - Idempotence enabled (exactly-once semantics)
 * - Acks=all (wait for all replicas)
 * - Retries=3 (automatic retry on failure)
 */
@Configuration
public class KafkaProducerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Bootstrap servers
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Serializers
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Reliability
        config.put(ProducerConfig.ACKS_CONFIG, "all");              // Wait for all replicas
        config.put(ProducerConfig.RETRIES_CONFIG, 3);               // Retry 3 times
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Exactly-once
        
        // Performance
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

---

### Step 3.3: Topic Configuration

**File:** `order-service/config/KafkaTopicConfig.java`

**Prompt:**
```java
package com.ecommerce.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topic Configuration.
 * 
 * Auto-creates topics if they don't exist.
 * 
 * Topics managed by Order Service:
 * - order.created (3 partitions, 2 replicas)
 * - order.confirmed (3 partitions, 2 replicas)
 * - order.shipped (3 partitions, 2 replicas)
 * - order.cancelled (3 partitions, 2 replicas)
 */
@Configuration
public class KafkaTopicConfig {
    
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created")
            .partitions(3)           // 3 partitions for parallelism
            .replicas(2)             // 2 replicas for fault tolerance
            .config("retention.ms", "604800000")  // 7 days retention
            .build();
    }
    
    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name("order.confirmed")
            .partitions(3)
            .replicas(2)
            .build();
    }
    
    @Bean
    public NewTopic orderShippedTopic() {
        return TopicBuilder.name("order.shipped")
            .partitions(3)
            .replicas(2)
            .build();
    }
    
    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order.cancelled")
            .partitions(3)
            .replicas(2)
            .build();
    }
}
```

---

### Step 3.4: Event Producer

**File:** `order-service/event/OrderEventProducer.java`

**Prompt:**
```java
package com.ecommerce.orderservice.event;

import com.ecommerce.events.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Publishes order-related events to Kafka.
 * 
 * Events:
 * - order.created → When order is placed
 * - order.confirmed → When payment succeeds
 * - order.shipped → When order is shipped
 * - order.cancelled → When order is cancelled
 * 
 * All events use orderId as partition key for ordering.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_CONFIRMED_TOPIC = "order.confirmed";
    private static final String ORDER_SHIPPED_TOPIC = "order.shipped";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";
    
    /**
     * Publish OrderCreatedEvent.
     * Called after order is saved to database.
     */
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("ORDER_CREATED")
            .timestamp(LocalDateTime.now())
            .orderId(order.getId().toString())
            .orderNumber(order.getOrderNumber())
            .userId(order.getUserId())
            .userEmail(order.getCustomerEmail())
            .items(mapOrderItems(order.getItems()))
            .subtotal(order.getSubtotal())
            .discount(order.getDiscount())
            .shipping(order.getShipping())
            .tax(order.getTax())
            .total(order.getTotal())
            .shippingAddress(mapAddress(order.getShippingAddress()))
            .billingAddress(mapAddress(order.getBillingAddress()))
            .paymentMethod(order.getPaymentMethod().name())
            .build();
        
        sendEvent(ORDER_CREATED_TOPIC, order.getId().toString(), event);
    }
    
    /**
     * Publish OrderConfirmedEvent.
     * Called when payment is confirmed.
     */
    public void publishOrderConfirmed(Order order) {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("ORDER_CONFIRMED")
            .timestamp(LocalDateTime.now())
            .orderId(order.getId().toString())
            .orderNumber(order.getOrderNumber())
            .userId(order.getUserId())
            .build();
        
        sendEvent(ORDER_CONFIRMED_TOPIC, order.getId().toString(), event);
    }
    
    /**
     * Generic send method with logging and error handling.
     */
    private void sendEvent(String topic, String key, Object event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(topic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event published successfully: topic={}, key={}, partition={}, offset={}", 
                        topic, key, 
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event: topic={}, key={}", topic, key, ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing event to topic: {}", topic, e);
            // Don't fail the order, just log
        }
    }
    
    private List<OrderCreatedEvent.OrderItemDto> mapOrderItems(List<OrderItem> items) {
        return items.stream()
            .map(item -> OrderCreatedEvent.OrderItemDto.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productImageUrl(item.getProductImageUrl())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getSubtotal())
                .build())
            .collect(Collectors.toList());
    }
    
    private OrderCreatedEvent.AddressDto mapAddress(String addressJson) {
        // Parse JSON string to AddressDto
        // Implementation depends on how you store address
        return OrderCreatedEvent.AddressDto.builder()
            // ... map fields
            .build();
    }
}
```

---

### Step 3.5: Update OrderService

**File:** `order-service/service/OrderService.java`

**BEFORE:**
```java
@Service
@Transactional
public class OrderService {
    
    @Async
    public void executePostOrderOperations(Order order) {
        // OLD: Sync calls
        cartServiceClient.clearCart(order.getUserId());
        paymentServiceClient.createPaymentIntent(order);
        shippingServiceClient.createShipment(order);
        notificationServiceClient.sendEmail(order);
    }
}
```

**AFTER (with Copilot prompt):**
```java
/**
 * Update createOrder method to publish Kafka event instead of sync calls.
 * 
 * BEFORE:
 * - Sync WebClient calls to Cart, Payment, Shipping, Notification services
 * - Wait for all responses (5-10 seconds)
 * 
 * AFTER:
 * - Publish order.created event to Kafka
 * - Return immediately (500ms)
 * - Consumers process async
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;  // NEW
    
    // Remove: CartServiceClient, PaymentServiceClient, etc.
    
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1-7: Create order (same as before)
        Order order = buildOrder(request);
        Order savedOrder = orderRepository.save(order);
        
        // 8: Publish event to Kafka (REPLACE async calls)
        eventProducer.publishOrderCreated(savedOrder);
        
        // 9: Return immediately (don't wait)
        return orderMapper.toResponse(savedOrder);
    }
    
    // REMOVE executePostOrderOperations method
}
```

---

### Step 3.6: Application Properties

**File:** `order-service/src/main/resources/application.yml`

**Add:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
```

---

## 🎧 PHASE 4: Cart Service (Consumer) (1 hour)

### Step 4.1: Add Dependencies

**Same as Order Service - add kafka-events dependency**

---

### Step 4.2: Consumer Configuration

**File:** `cart-service/config/KafkaConsumerConfig.java`

**Prompt:**
```java
package com.ecommerce.cartservice.config;

import com.ecommerce.events.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer Configuration for Cart Service.
 * 
 * Consumes events from:
 * - order.created (to clear cart after order placed)
 * 
 * Consumer Group: cart-service-group
 * Concurrency: 3 (3 concurrent consumers)
 */
@Configuration
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "cart-service-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ecommerce.events");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            new JsonDeserializer<>(OrderCreatedEvent.class, false)
        );
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> 
        kafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 3 concurrent consumers
        return factory;
    }
}
```

---

### Step 4.3: Event Consumer

**File:** `cart-service/event/OrderEventConsumer.java`

**Prompt:**
```java
package com.ecommerce.cartservice.event;

import com.ecommerce.cartservice.service.CartService;
import com.ecommerce.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes order events to clear user's cart.
 * 
 * Topic: order.created
 * Group: cart-service-group
 * 
 * When order is created, clear the user's cart.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    
    private final CartService cartService;
    
    @KafkaListener(
        topics = "order.created",
        groupId = "cart-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(
        @Payload OrderCreatedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received OrderCreatedEvent: eventId={}, orderId={}, userId={}, partition={}, offset={}", 
            event.getEventId(), event.getOrderId(), event.getUserId(), partition, offset);
        
        try {
            // Clear user's cart
            cartService.clearCartByUserId(event.getUserId());
            log.info("Cart cleared successfully for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to clear cart for event: {}", event.getEventId(), e);
            // Kafka will retry based on error handling config
            throw e;  // Rethrow to trigger retry
        }
    }
}
```

---

### Step 4.4: Application Properties

**File:** `cart-service/application.yml`

**Add:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: cart-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: com.ecommerce.events
```

---

## 💳 PHASE 5: Payment Service (Producer + Consumer) (1.5 hours)

### Step 5.1: Consumer Config

**Same structure as Cart Service**

---

### Step 5.2: Order Event Consumer

**File:** `payment-service/event/OrderEventConsumer.java`

**Prompt:**
```java
package com.ecommerce.paymentservice.event;

import com.ecommerce.events.OrderCreatedEvent;
import com.ecommerce.paymentservice.event.PaymentEventProducer;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order.created events to create payment intents.
 * 
 * Topic: order.created
 * Group: payment-service-group
 * 
 * When order is created:
 * 1. Create Stripe payment intent
 * 2. If auto-confirmed (COD), publish payment.success event
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    
    private final PaymentService paymentService;
    private final PaymentEventProducer eventProducer;
    
    @KafkaListener(
        topics = "order.created",
        groupId = "payment-service-group"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Creating payment for order: orderId={}, orderNumber={}", 
            event.getOrderId(), event.getOrderNumber());
        
        try {
            // Create payment intent
            var paymentIntent = paymentService.createPaymentIntent(
                event.getOrderId(),
                event.getTotal(),
                event.getPaymentMethod()
            );
            
            log.info("Payment intent created: paymentId={}", paymentIntent.getId());
            
            // If auto-confirmed payment (e.g., COD)
            if (isAutoConfirmed(event.getPaymentMethod())) {
                eventProducer.publishPaymentSuccess(paymentIntent, event);
            }
        } catch (Exception e) {
            log.error("Failed to create payment for order: {}", event.getOrderId(), e);
            // Could publish payment.failed event here
            throw e;
        }
    }
    
    private boolean isAutoConfirmed(String paymentMethod) {
        return "CASH_ON_DELIVERY".equals(paymentMethod);
    }
}
```

---

### Step 5.3: Payment Event Producer

**File:** `payment-service/event/PaymentEventProducer.java`

**Prompt:**
```java
package com.ecommerce.paymentservice.event;

import com.ecommerce.events.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publishes payment-related events.
 * 
 * Events:
 * - payment.success → When payment is confirmed
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String PAYMENT_SUCCESS_TOPIC = "payment.success";
    
    public void publishPaymentSuccess(PaymentIntent payment, OrderCreatedEvent orderEvent) {
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("PAYMENT_SUCCESS")
            .timestamp(LocalDateTime.now())
            .paymentId(payment.getId())
            .orderId(orderEvent.getOrderId())
            .orderNumber(orderEvent.getOrderNumber())
            .userId(orderEvent.getUserId())
            .amount(payment.getAmount())
            .currency("USD")
            .paymentMethod(orderEvent.getPaymentMethod())
            .transactionId(payment.getTransactionId())
            .build();
        
        kafkaTemplate.send(PAYMENT_SUCCESS_TOPIC, orderEvent.getOrderId(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("PaymentSuccessEvent published: eventId={}", event.getEventId());
                } else {
                    log.error("Failed to publish PaymentSuccessEvent", ex);
                }
            });
    }
}
```

---

## 📧 PHASE 6: Notification Service (Consumer) (1 hour)

### Step 6.1: Multi-Event Consumer

**File:** `notification-service/event/EventConsumer.java`

**Prompt:**
```java
package com.ecommerce.notificationservice.event;

import com.ecommerce.events.*;
import com.ecommerce.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes multiple events to send notifications.
 * 
 * Topics:
 * - order.created → Send order confirmation email
 * - order.shipped → Send shipping notification
 * - payment.success → Send payment receipt
 * - user.registered → Send welcome email
 * 
 * Group: notification-service-group
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {
    
    private final EmailService emailService;
    
    @KafkaListener(
        topics = "order.created",
        groupId = "notification-service-group"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Sending order confirmation email: orderId={}, email={}", 
            event.getOrderId(), event.getUserEmail());
        
        try {
            emailService.sendOrderConfirmation(event);
            log.info("Order confirmation email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send order confirmation email", e);
            throw e;
        }
    }
    
    @KafkaListener(
        topics = "order.shipped",
        groupId = "notification-service-group"
    )
    public void handleOrderShipped(OrderShippedEvent event) {
        log.info("Sending shipping notification: orderId={}", event.getOrderId());
        emailService.sendShippingNotification(event);
    }
    
    @KafkaListener(
        topics = "payment.success",
        groupId = "notification-service-group"
    )
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Sending payment receipt: orderId={}", event.getOrderId());
        emailService.sendPaymentReceipt(event);
    }
    
    @KafkaListener(
        topics = "user.registered",
        groupId = "notification-service-group"
    )
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Sending welcome email: userId={}", event.getUserId());
        emailService.sendWelcomeEmail(event);
    }
}
```

---

## 🔄 PHASE 7: Error Handling & Retry (30 min)

### Step 7.1: Error Handler Config

**File:** `cart-service/config/KafkaErrorHandlingConfig.java`

**Prompt:**
```java
package com.ecommerce.cartservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Kafka error handling and retry configuration.
 * 
 * Strategy:
 * - Retry 3 times with exponential backoff
 * - Initial delay: 1 second
 * - Multiplier: 2 (1s, 2s, 4s)
 * - After 3 failures, send to Dead Letter Topic (DLT)
 */
@Configuration
@Slf4j
public class KafkaErrorHandlingConfig {
    
    @Bean
    public DefaultErrorHandler errorHandler() {
        // Exponential backoff: 1s, 2s, 4s
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);      // 1 second
        backOff.setMultiplier(2.0);             // Double each time
        backOff.setMaxInterval(10000L);         // Max 10 seconds
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
        
        // Log errors
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Retry attempt {} for record: topic={}, partition={}, offset={}", 
                deliveryAttempt,
                record.topic(),
                record.partition(),
                record.offset(),
                ex);
        });
        
        return errorHandler;
    }
}
```

---

### Step 7.2: Dead Letter Topic Listener

**File:** `cart-service/event/DeadLetterConsumer.java`

**Prompt:**
```java
package com.ecommerce.cartservice.event;

import com.ecommerce.events.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes failed events from Dead Letter Topic.
 * 
 * Topic: order.created.DLT
 * 
 * Actions:
 * - Log failed event
 * - Save to database for manual processing
 * - Send alert to admin
 */
@Component
@Slf4j
public class DeadLetterConsumer {
    
    @KafkaListener(
        topics = "order.created.DLT",
        groupId = "cart-service-dlt-group"
    )
    public void handleFailedEvent(OrderCreatedEvent event) {
        log.error("Event failed after all retries: eventId={}, orderId={}", 
            event.getEventId(), event.getOrderId());
        
        // TODO: Save to failed_events table for manual processing
        // TODO: Send alert to admin
        
        // For now, just log
        log.error("Failed event details: {}", event);
    }
}
```

---

## 📊 PHASE 8: Monitoring & Testing (1 hour)

### Step 8.1: Testing

**Create test:**

```bash
# Start all services
docker-compose up -d

# Create order via API
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{...order data...}'

# Check Kafka UI
# http://localhost:8090
# - View topics
# - Check messages
# - Monitor consumer lag
```

---

### Step 8.2: Application Metrics

**Add to all services:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,kafka
  endpoint:
    kafka:
      enabled: true
```

**Access metrics:**
```
http://localhost:8081/actuator/metrics/kafka.producer.record.send.total
http://localhost:8083/actuator/metrics/kafka.consumer.records.consumed.total
```

---

## ✅ VERIFICATION CHECKLIST

```
Infrastructure:
[ ] Kafka cluster running (docker-compose up)
[ ] Kafka UI accessible (http://localhost:8090)
[ ] Zookeeper healthy

Shared Library:
[ ] kafka-events built (mvn clean install)
[ ] All event classes created
[ ] Version 1.0.0 in local Maven repo

Order Service (Producer):
[ ] Kafka dependency added
[ ] KafkaProducerConfig created
[ ] KafkaTopicConfig created
[ ] OrderEventProducer created
[ ] OrderService updated to publish events
[ ] Old WebClient calls removed
[ ] Application properties configured
[ ] Service starts successfully

Cart Service (Consumer):
[ ] Kafka dependency added
[ ] KafkaConsumerConfig created
[ ] OrderEventConsumer created
[ ] CartService updated
[ ] Application properties configured
[ ] Service starts successfully

Payment Service (Producer + Consumer):
[ ] Both producer and consumer configs
[ ] OrderEventConsumer created
[ ] PaymentEventProducer created
[ ] Topic configs added
[ ] Service starts successfully

Notification Service (Consumer):
[ ] Multi-event consumer created
[ ] All 4 event handlers implemented
[ ] EmailService updated
[ ] Service starts successfully

Error Handling:
[ ] Error handler config in all consumers
[ ] Dead letter topic listeners
[ ] Retry logic tested

Testing:
[ ] Created test order successfully
[ ] Events appear in Kafka UI
[ ] Cart cleared automatically
[ ] Payment intent created
[ ] Email sent
[ ] No errors in logs
```

---

## 🚀 DEPLOYMENT

### Production Kafka Cluster

```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    
  kafka-1:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-1:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
      
  kafka-2:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_BROKER_ID: 2
      
  kafka-3:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_BROKER_ID: 3
```

---

## 💡 BEST PRACTICES

1. **Idempotency:** Use eventId to prevent duplicate processing
2. **Ordering:** Use same key (orderId) for related events
3. **Retry:** Configure appropriate retry logic
4. **DLT:** Handle failed events properly
5. **Monitoring:** Track consumer lag, throughput
6. **Logging:** Log all event processing
7. **Testing:** Test failure scenarios

---

## 📈 PERFORMANCE COMPARISON

**Before (REST API):**
- Order creation: 5-10 seconds
- User waits for all services
- Service coupling

**After (Kafka):**
- Order creation: 500ms
- User gets immediate response
- Services decoupled
- Better scalability

---

**MIGRATION COMPLETE! 🎉**

**Time spent:** ~6-8 hours  
**Files created:** ~40 files  
**Services updated:** 6 services  
**Benefits:** Async, scalable, decoupled