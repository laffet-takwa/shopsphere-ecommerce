package com.shopsphere.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Guards the "charge an order at most once" rule that makes redelivered {@code inventory.reserved}
 * messages safe.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentProcessorTest {

    private static final Long ORDER_ID = 55L;
    private static final Long USER_ID = 7L;

    @Mock
    private PaymentRepository payments;

    @Mock
    private SimulatedPaymentGateway gateway;

    @InjectMocks
    private PaymentProcessor processor;

    private Payment saved() {
        Payment payment = new Payment(ORDER_ID, USER_ID, new BigDecimal("49.00"),
                PaymentStatus.SUCCESS, "SIM-1");
        when(payments.save(any(Payment.class))).thenReturn(payment);
        return payment;
    }

    @Test
    void chargesAndPersistsAnApprovedPayment() {
        when(payments.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(gateway.authorize(anyLong(), any(BigDecimal.class)))
                .thenReturn(new SimulatedPaymentGateway.Authorization(true, "SIM-1", null));
        saved();

        Payment result = processor.charge(ORDER_ID, USER_ID, new BigDecimal("49.00"));

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.getTransactionReference()).isEqualTo("SIM-1");
        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        verify(payments, times(1)).save(any(Payment.class));
    }

    @Test
    void persistsADeclinedPaymentSoTheOutcomeIsAuditable() {
        when(payments.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(gateway.authorize(anyLong(), any(BigDecimal.class)))
                .thenReturn(new SimulatedPaymentGateway.Authorization(false, "SIM-2", "declined"));
        saved();

        Payment result = processor.charge(ORDER_ID, USER_ID, new BigDecimal("50000.00"));

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(payments, times(1)).save(any(Payment.class));
    }

    @Test
    void doesNotChargeAnAlreadyPaidOrderTwice() {
        Payment existing = new Payment(ORDER_ID, USER_ID, new BigDecimal("49.00"),
                PaymentStatus.SUCCESS, "SIM-1");
        when(payments.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

        Payment result = processor.charge(ORDER_ID, USER_ID, new BigDecimal("49.00"));

        assertThat(result).isSameAs(existing);
        verify(payments, never()).save(any(Payment.class));
        verify(gateway, never()).authorize(anyLong(), any(BigDecimal.class));
    }
}