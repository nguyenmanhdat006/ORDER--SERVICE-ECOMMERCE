package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartServiceClient {

    private final WebClient cartServiceWebClient;

    public CartResponse getCart(String userId, String token) {
        try {
            log.info("Calling Cart Service getCart for userId={}, authTokenPresent={}",
                    userId, token != null && !token.isBlank());

            ApiResponse<CartResponse> response = cartServiceWebClient.get()
                    .uri("/api/cart")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<CartResponse>>() {})
                    .block();

            CartResponse cart = response != null ? response.getData() : null;
            log.info("Cart Service response received for userId={}, cartPresent={}, itemCount={}",
                    userId,
                    cart != null,
                    cart != null && cart.items != null ? cart.items.size() : 0);

            return cart;
        } catch (Exception e) {
            log.error("Error getting cart for user: {}", userId, e);
            throw new RuntimeException("Failed to get cart from cart service");
        }
    }

    public void clearCart(String userId, String token) {
        try {
            log.info("Calling Cart Service clearCart for userId={}, authTokenPresent={}",
                    userId, token != null && !token.isBlank());

            cartServiceWebClient.delete()
                    .uri("/api/cart")
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

