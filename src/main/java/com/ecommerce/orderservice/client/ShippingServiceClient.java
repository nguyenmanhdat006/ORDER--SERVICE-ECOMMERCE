package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.shipping.CalculateFeeRequest;
import com.ecommerce.orderservice.dto.shipping.CalculateFeeResponse;
import com.ecommerce.orderservice.dto.shipping.CreateShipmentRequest;
import com.ecommerce.orderservice.dto.shipping.CreateShipmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingServiceClient {

    @Qualifier("shippingServiceWebClient")
    private final WebClient shippingServiceWebClient;

    public CalculateFeeResponse calculateFee(CalculateFeeRequest request) {
        log.info("Calculating shipping fee for city: {}, province: {}", request.getCity(), request.getProvince());

        try {
            CalculateFeeResponse response = shippingServiceWebClient.post()
                    .uri("/api/shipping/calculate-fee")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CalculateFeeResponse.class)
                    .block();

            if (response == null || response.getShippingFee() == null) {
                throw new IllegalStateException("Shipping service returned invalid fee response");
            }

            log.info("Shipping fee calculated: {}", response.getShippingFee());
            return response;
        } catch (Exception e) {
            log.error("Error calculating shipping fee", e);
            throw new RuntimeException("Failed to calculate shipping fee", e);
        }
    }

    public CreateShipmentResponse createShipment(CreateShipmentRequest request) {
        log.info("Creating shipment for order: {}", request.getOrderNumber());

        try {
            CreateShipmentResponse response = shippingServiceWebClient.post()
                    .uri("/api/shipping/create")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CreateShipmentResponse.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException("Shipping service returned empty shipment response");
            }

            log.info("Shipment created for order: {}, tracking: {}",
                    request.getOrderNumber(), response.getShipmentNumber());
            return response;
        } catch (Exception e) {
            log.error("Error creating shipment for order: {}", request.getOrderNumber(), e);
            throw new RuntimeException("Failed to create shipment", e);
        }
    }
}

