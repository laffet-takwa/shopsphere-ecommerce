package com.shopsphere.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Covers the guarded transitions that keep Kafka consumers idempotent: a redelivered event must
 * report "no change" instead of moving an order backwards.
 */
class OrderEntityStatusTest {

    private static OrderEntity pendingOrder() {
        return new OrderEntity(7L, new BigDecimal("99.00"));
    }

    @Test
    void startsPending() {
        assertThat(pendingOrder().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void marksPendingOrderPaid() {
        OrderEntity order = pendingOrder();

        assertThat(order.markPaid()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void redeliveredPaymentEventDoesNotReapplyTransition() {
        OrderEntity order = pendingOrder();
        order.markPaid();

        assertThat(order.markPaid()).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void refusesToMarkCancelledOrderPaid() {
        OrderEntity order = pendingOrder();
        order.cancel();

        assertThat(order.markPaid()).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void refusesToShipBeforePayment() {
        assertThat(pendingOrder().ship()).isFalse();
    }

    @Test
    void shipsPaidOrder() {
        OrderEntity order = pendingOrder();
        order.markPaid();

        assertThat(order.ship()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shippingTwiceIsRejected() {
        OrderEntity order = pendingOrder();
        order.markPaid();
        order.ship();

        assertThat(order.ship()).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void cancelledOrderCannotShip() {
        OrderEntity order = pendingOrder();
        order.markPaid();
        order.cancel();

        assertThat(order.ship()).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancellingAnAlreadyCancelledOrderIsANoOp() {
        OrderEntity order = pendingOrder();
        order.cancel();

        assertThat(order.cancel()).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}