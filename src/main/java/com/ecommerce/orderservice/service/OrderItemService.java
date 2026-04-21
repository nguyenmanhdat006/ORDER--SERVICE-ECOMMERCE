package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.CartServiceClient;
import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.exception.BadRequestException;
import com.ecommerce.orderservice.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Item Service
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductServiceClient productServiceClient;

    /**
     * Create order items from cart items
     */
    public List<OrderItem> createOrderItems(Order order, List<CartServiceClient.CartItem> cartItems) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartServiceClient.CartItem cartItem : cartItems) {
            // Validate stock for each item
            if (!productServiceClient.checkStock(cartItem.productId, cartItem.quantity)) {
                log.error("Insufficient stock for product: {} (requested: {})", cartItem.productId, cartItem.quantity);
                throw new BadRequestException("Insufficient stock for product: " + cartItem.productName);
            }

            // Create OrderItem with price snapshot
            BigDecimal price = cartItem.price != null ? cartItem.price : BigDecimal.ZERO;
            BigDecimal subtotal = cartItem.subtotal != null
                    ? cartItem.subtotal
                    : price.multiply(BigDecimal.valueOf(cartItem.quantity != null ? cartItem.quantity : 0));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(cartItem.productId)
                    .productVariantId(cartItem.productVariantId)
                    .productName(cartItem.productName)
                    .productImageUrl(cartItem.productImageUrl)
                    .quantity(cartItem.quantity)
                    .price(price)
                    .subtotal(subtotal)
                    .build();

            orderItems.add(orderItem);

            // Reduce product stock
//            try {
//                productServiceClient.reduceStock(cartItem.productId, cartItem.quantity);
//            } catch (Exception e) {
//                log.error("Failed to reduce stock for product: {}", cartItem.productId, e);
//                throw new BadRequestException("Failed to update product stock");
//            }
        }

        // Save all order items
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);
        log.info("Order items created for order: {}", order.getId());
        return savedItems;
    }

    /**
     * Get order items for an order
     */
    public List<OrderItem> getOrderItems(java.util.UUID orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    /**
     * Calculate total from items
     */
    public BigDecimal calculateItemsTotal(List<CartServiceClient.CartItem> cartItems) {
        return cartItems.stream()
                .map(item -> item.subtotal != null ? item.subtotal : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

