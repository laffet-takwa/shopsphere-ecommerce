package com.shopsphere.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test against a real PostgreSQL instance.
 *
 * <p>{@code PaymentProcessor} is the only place a payment row is written, so the database-level
 * guarantee that an order can be charged once is verified here rather than mocked. H2 is not used
 * on purpose: the unique constraint on {@code order_id} is PostgreSQL behaviour worth asserting.
 *
 * <p>Requires a working Docker daemon; it is skipped automatically when one is unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "shopsphere.payment.decline-above-amount=10000"
})
class PaymentProcessorIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16.4-alpine")
                    .withDatabaseName("shopsphere_payments")
                    .withUsername("shopsphere")
                    .withPassword("shopsphere");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private PaymentProcessor processor;

    @Autowired
    private PaymentRepository payments;

    @Test
    void chargesAnOrderOnlyOnce() {
        processor.charge(900L, 7L, new BigDecimal("49.00"));

        Payment second = processor.charge(900L, 7L, new BigDecimal("49.00"));

        assertThat(payments.countByOrderId(900L)).isEqualTo(1);
        assertThat(second.getTransactionReference())
                .isEqualTo(payments.findByOrderId(900L).orElseThrow().getTransactionReference());
    }

    @Test
    void persistsDeclinedPaymentAboveTheSimulatedCardLimit() {
        Payment payment = processor.charge(901L, 7L, new BigDecimal("50000.00"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getTransactionReference()).startsWith("SIM-");
        assertThat(payments.findByOrderId(901L)).isPresent();
    }
}