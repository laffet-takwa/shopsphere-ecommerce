package com.shopsphere.order;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Supplies the {@code @LoadBalanced} builder used to reach {@code product-service}.
 *
 * <p>Eureka resolves the logical service name to a live instance, so the order service never needs
 * a host or port for the catalog.
 */
@Configuration
class OrderClientConfiguration {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}