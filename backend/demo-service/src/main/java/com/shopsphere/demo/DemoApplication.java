package com.shopsphere.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * The hosted demo runs every ShopSphere service in one JVM.
 *
 * <p>The reason is a platform constraint, not a design preference. Free web hosts give a single
 * service that sleeps when idle, so an eight-service topology on a free tier cannot resolve its own
 * dependencies after a cold start. This module keeps the real service classes - they are compiled
 * from their own source directories, not copied - and supplies the few things the topology used to
 * provide:
 *
 * <ul>
 *   <li>a single datasource and a single Mongo database instead of database-per-service</li>
 *   <li>a servlet filter in place of the reactive {@code api-gateway}, because Spring Cloud Gateway
 *       is reactive and cannot be hosted in the same servlet context</li>
 *   <li>{@code SimpleDiscoveryClient} so order-service's load-balanced call to {@code product-service}
 *       resolves without Eureka</li>
 * </ul>
 *
 * <p>{@code docker compose up} still runs the genuine eight-service topology; this module exists
 * only so there is a live URL that survives a cold start.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {
            "com.shopsphere.demo",
            "com.shopsphere.auth",
            "com.shopsphere.product",
            "com.shopsphere.order",
            "com.shopsphere.inventory",
            "com.shopsphere.payment",
            "com.shopsphere.notification"
        },
        // Each service declares its own entry point, a service-scoped exception handler and (for
        // auth) a security chain that would lock the whole app down. They are replaced below by
        // equivalents defined in this package. The pattern is anchored on those simple names so
        // ordinary components in the same packages are still picked up.
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.shopsphere\\.(auth|product|order|inventory|payment|notification)\\..*"
                        + "(Application|ApiExceptionHandler|SecurityConfiguration)$"))
@EnableMongoRepositories(
        basePackages = {
            "com.shopsphere.inventory",
            "com.shopsphere.notification"
        })
// Spring Boot only auto-detects entities and repositories under the entry-point package, so the
// service packages are declared explicitly here. inventory and notification are Mongo-only and are
// therefore absent from the JPA lists.
@EntityScan(basePackages = {
        "com.shopsphere.auth.user",
        "com.shopsphere.product",
        "com.shopsphere.order",
        "com.shopsphere.payment"
})
@EnableJpaRepositories(basePackages = {
        "com.shopsphere.auth.user",
        "com.shopsphere.product",
        "com.shopsphere.order",
        "com.shopsphere.payment"
})
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
