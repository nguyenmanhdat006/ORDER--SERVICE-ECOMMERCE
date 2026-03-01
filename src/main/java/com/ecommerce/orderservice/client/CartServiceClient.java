package com.ecommerce.orderservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Cart Service Client
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CartServiceClient {

    private final WebClient cartServiceWebClient;

    /**
     * Get cart by user ID
     */
    public CartResponse getCart(String userId, String token) {
        try {
            return cartServiceWebClient.get()
                    .uri("/api/carts/{userId}", userId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(CartResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Error getting cart for user: {}", userId, e);
            throw new RuntimeException("Failed to get cart from cart service");
        }
    }

    /**
     * Clear cart after order creation
     */
    public void clearCart(String userId, String token) {
        try {
            cartServiceWebClient.delete()
                    .uri("/api/carts/{userId}", userId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Cart cleared for user: {}", userId);
        } catch (Exception e) {
            log.warn("Error clearing cart for user: {}", userId, e);
            // Don't throw exception here, as order is already created
        }
    }

    // Inner classes for Cart responses
    public static class CartResponse {
        public String userId;
        public java.util.List<CartItem> items;
        public java.math.BigDecimal subtotal;
        public java.math.BigDecimal total;
    }

    public static class CartItem {
        public String productId;
        public String productVariantId;
        public String productName;
        public String productImageUrl;
        public Integer quantity;
        public java.math.BigDecimal price;
        public java.math.BigDecimal subtotal;
    }
}

