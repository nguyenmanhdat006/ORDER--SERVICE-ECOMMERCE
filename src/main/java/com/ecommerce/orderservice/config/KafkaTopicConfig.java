package com.ecommerce.orderservice.config;

/*
Kafka topic declarations are disabled because the Order Service now uses REST API calls only.

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name("order.confirmed")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderShippedTopic() {
        return TopicBuilder.name("order.shipped")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order.cancelled")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
*/

