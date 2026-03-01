package com.ecommerce.orderservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Product Service Client
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceClient {

    private final WebClient productServiceWebClient;

    /**
     * Get product details
     */
    public ProductResponse getProduct(String productId) {
        try {
            return productServiceWebClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(ProductResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Error getting product: {}", productId, e);
            throw new RuntimeException("Failed to get product from product service");
        }
    }

    /**
     * Check if product has sufficient stock
     */
    public boolean checkStock(String productId, Integer quantity) {
        try {
            ProductResponse product = getProduct(productId);
            return product != null && product.stock >= quantity;
        } catch (Exception e) {
            log.error("Error checking stock for product: {}", productId, e);
            return false;
        }
    }

    /**
     * Reduce product stock after order creation
     */
    public void reduceStock(String productId, Integer quantity) {
        try {
            productServiceWebClient.put()
                    .uri("/api/products/{id}/stock/reduce", productId)
                    .bodyValue(new StockUpdateRequest(quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Stock reduced for product: {} by quantity: {}", productId, quantity);
        } catch (Exception e) {
            log.error("Error reducing stock for product: {}", productId, e);
            throw new RuntimeException("Failed to reduce stock for product: " + productId);
        }
    }

    // Inner classes for Product responses
    public static class ProductResponse {
        public String id;
        public String name;
        public String description;
        public java.math.BigDecimal price;
        public Integer stock;
        public String imageUrl;
        public String sku;
    }

    public static class StockUpdateRequest {
        public Integer quantity;

        public StockUpdateRequest(Integer quantity) {
            this.quantity = quantity;
        }
    }
}

