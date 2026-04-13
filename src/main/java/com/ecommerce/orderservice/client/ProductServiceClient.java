package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceClient {

    private final WebClient productServiceWebClient;

    public ProductResponse getProduct(String productId) {
        try {
            ApiResponse<ProductResponse> response = productServiceWebClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<ProductResponse>>() {})
                    .block();

            return response != null ? response.getData() : null;
        } catch (Exception e) {
            log.error("Error getting product: {}", productId, e);
            throw new RuntimeException("Failed to get product from product service");
        }
    }

    public boolean checkStock(String productId, Integer quantity) {
        try {
            ProductResponse product = getProduct(productId);
            log.info("Checking stock for product: {}, requested quantity: {}, available stock: {}",
                    productId, quantity, product != null ? product.stockQuantity : "N/A");

            return product != null
                    && product.stockQuantity != null
                    && product.stockQuantity >= quantity;

        } catch (Exception e) {
            log.error("Error checking stock for product: {}", productId, e);
            return false;
        }
    }

    /**
     * Reduce product stock after order creation
     */
//    public void reduceStock(String productId, Integer quantity) {
//        try {
//            productServiceWebClient.put()
//                    .uri("/api/products/{id}/stock/reduce", productId)
//                    .bodyValue(new StockUpdateRequest(quantity))
//                    .retrieve()
//                    .toBodilessEntity()
//                    .block();
//            log.info("Stock reduced for product: {} by quantity: {}", productId, quantity);
//        } catch (Exception e) {
//            log.error("Error reducing stock for product: {}", productId, e);
//            throw new RuntimeException("Failed to reduce stock for product: " + productId);
//        }
//    }

    // Inner classes for Product responses
    public static class ProductResponse {
        public String id;
        public String name;
        public String description;
        public java.math.BigDecimal price;
        public Integer stockQuantity;
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

