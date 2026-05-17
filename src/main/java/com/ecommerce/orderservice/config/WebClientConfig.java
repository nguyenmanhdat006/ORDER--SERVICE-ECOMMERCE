package com.ecommerce.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

        @Value("${service.cart.url:http://localhost:8083}")
        private String cartServiceUrl;

        @Value("${service.product.url:http://localhost:8082}")
        private String productServiceUrl;

        @Value("${service.user.url:http://localhost:8083}")
        private String userServiceUrl;

        @Value("${payment-service.url:http://localhost:8085}")
        private String paymentServiceUrl;

        @Value("${shipping-service.url:http://localhost:8088}")
        private String shippingServiceUrl;

        @Bean("cartServiceWebClient")
        public WebClient cartServiceWebClient() {
                HttpClient httpClient = HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(10));

                return WebClient.builder()
                                .baseUrl(cartServiceUrl)
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .build();
        }

        @Bean("productServiceWebClient")
        public WebClient productServiceWebClient() {
                HttpClient httpClient = HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(10));

                return WebClient.builder()
                                .baseUrl(productServiceUrl)
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .build();
        }

        @Bean("userServiceWebClient")
        public WebClient userServiceWebClient() {
                HttpClient httpClient = HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(10));

                return WebClient.builder()
                                .baseUrl(userServiceUrl)
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .build();
        }

        @Bean("paymentServiceWebClient")
        public WebClient paymentServiceWebClient() {
                HttpClient httpClient = HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(10));

                return WebClient.builder()
                                .baseUrl(paymentServiceUrl)
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .build();
        }

        @Bean("shippingServiceWebClient")
        public WebClient shippingServiceWebClient() {
                HttpClient httpClient = HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(10));

                return WebClient.builder()
                                .baseUrl(shippingServiceUrl)
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .build();
        }
}
