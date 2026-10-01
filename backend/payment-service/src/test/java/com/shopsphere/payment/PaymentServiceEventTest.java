package com.shopsphere.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceEventTest {

    private static final ObjectMapper MAPPER =
            JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @Mock
    private PaymentProcessor processor;

    @Mock
    private KafkaTemplate<String, Object> kafka;

    private static String reservedEvent() throws Exception {
        return MAPPER.writeValueAsString(new PaymentService.InventoryReservedEvent(
                "evt-1", 55L, 7L, new BigDecimal("49.00"), null, Instant.now()));
    }

    @Test
    void publishesPaymentCompletedWhenApproved() throws Exception {
        Payment payment = new Payment(55L, 7L, new BigDecimal("49.00"),
                PaymentStatus.SUCCESS, "SIM-1");
        when(processor.charge(any(), any(), any())).thenReturn(payment);
        PaymentService service = new PaymentService(processor, kafka, MAPPER);

        service.onInventoryReserved(reservedEvent());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("payment.completed"), eq("55"), captor.capture());
        PaymentService.PaymentEvent event = (PaymentService.PaymentEvent) captor.getValue();
        assertEventMatches(event, PaymentStatus.SUCCESS);
        verify(kafka, never()).send(eq("payment.failed"), any(), any());
    }

    @Test
    void publishesPaymentFailedWhenDeclined() throws Exception {
        Payment payment = new Payment(55L, 7L, new BigDecimal("49.00"),
                PaymentStatus.FAILED, "SIM-2");
        when(processor.charge(any(), any(), any())).thenReturn(payment);
        PaymentService service = new PaymentService(processor, kafka, MAPPER);

        service.onInventoryReserved(reservedEvent());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("payment.failed"), eq("55"), captor.capture());
        PaymentService.PaymentEvent event = (PaymentService.PaymentEvent) captor.getValue();
        assertEventMatches(event, PaymentStatus.FAILED);
    }

    private static void assertEventMatches(PaymentService.PaymentEvent event, PaymentStatus expected) {
        assertThat(event.status()).isEqualTo(expected.name());
        assertThat(event.orderId()).isEqualTo(55L);
        assertThat(event.transactionReference()).isNotBlank();
    }
}
