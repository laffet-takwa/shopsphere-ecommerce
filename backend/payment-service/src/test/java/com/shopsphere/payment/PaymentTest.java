package com.shopsphere.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {
    @Test
    void recordsSimulatedPaymentWithoutExternalProviderData() {
        var payment = new Payment(21L, 4L, new BigDecimal("45.00"), PaymentStatus.SUCCESS, "SIM-test");

        assertEquals(21L, payment.getOrderId());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("USD", payment.getCurrency());
        assertEquals("SIM-test", payment.getTransactionReference());
    }
}