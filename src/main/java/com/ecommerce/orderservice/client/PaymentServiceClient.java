package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.payment.CreatePaymentRequest;
import com.ecommerce.orderservice.dto.payment.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    @Qualifier("paymentServiceWebClient")
    private final WebClient paymentServiceWebClient;

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for order: {}", request.getOrderNumber());

        try {
            PaymentResponse response = paymentServiceWebClient.post()
                    .uri("/api/payments/create")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResponse.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException("Payment service returned empty response");
            }

            log.info("Payment created for order: {}, paymentNumber: {}",
                    request.getOrderNumber(), response.getPaymentNumber());
            return response;
        } catch (Exception e) {
            log.error("Error creating payment for order: {}", request.getOrderNumber(), e);
            throw new RuntimeException("Failed to create payment", e);
        }
    }

    // MIGRATION: New method for REST API (replaces Kafka events)
    public void updatePaymentStatus(String orderId, String status) {
        log.info("Updating payment status for order: {}, newStatus: {}", orderId, status);

        try {
            paymentServiceWebClient.put()
                    .uri("/api/payments/order/{orderId}/status?status={status}", orderId, status)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Payment status updated for order: {}, newStatus: {}", orderId, status);
        } catch (Exception e) {
            log.error("Error updating payment status for order: {}", orderId, e);
            throw new RuntimeException("Failed to update payment status", e);
        }
    }
}

