package com.shopsphere.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SimulatedPaymentGatewayTest {

    private static final BigDecimal CARD_LIMIT = new BigDecimal("10000");

    private static SimulatedPaymentGateway gateway() {
        return new SimulatedPaymentGateway(CARD_LIMIT);
    }

    @Test
    void approvesAmountWithinTheSimulatedCardLimit() {
        var authorization = gateway().authorize(1L, new BigDecimal("49.99"));

        assertThat(authorization.approved()).isTrue();
        assertThat(authorization.reason()).isNull();
    }

    @Test
    void alwaysIssuesATransactionReference() {
        assertThat(gateway().authorize(1L, new BigDecimal("10.00")).transactionReference())
                .startsWith("SIM-");
    }

    @Test
    void issuesADistinctReferencePerAttempt() {
        var gateway = gateway();

        assertThat(gateway.authorize(1L, new BigDecimal("10.00")).transactionReference())
                .isNotEqualTo(gateway.authorize(1L, new BigDecimal("10.00")).transactionReference());
    }

    @Test
    void declinesAmountAboveTheSimulatedCardLimit() {
        var authorization = gateway().authorize(1L, new BigDecimal("10000.01"));

        assertThat(authorization.approved()).isFalse();
        assertThat(authorization.reason()).contains("exceeds the simulated card limit");
    }

    @Test
    void declinesAreDeterministicForTheSameOrder() {
        var gateway = gateway();

        assertThat(gateway.authorize(9L, new BigDecimal("25000")).approved()).isFalse();
        assertThat(gateway.authorize(9L, new BigDecimal("25000")).approved()).isFalse();
    }

    @Test
    void approvesExactlyTheLimit() {
        assertThat(gateway().authorize(1L, CARD_LIMIT).approved()).isTrue();
    }
}