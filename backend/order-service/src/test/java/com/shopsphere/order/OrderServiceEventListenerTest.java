package com.shopsphere.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

/**
 * Exercises the Kafka listeners that drive an order forward. No broker, no database, no HTTP:
 * collaborators are mocked and the event payloads are real JSON so the deserialization contract
 * is covered too.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceEventListenerTest {

    private static final Long ORDER_ID = 42L;
    private static final Long USER_ID = 7L;

    private static final ObjectMapper MAPPER =
            JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @Mock
    private OrderRepository orders;

    @Mock
    private ProcessedEventRepository processedEvents;

    @Mock
    private KafkaTemplate<String, Object> kafka;

    @Mock
    private RestClient.Builder catalogClient;

    @Mock
    private RestClient restClient;

    private OrderService service;

    @BeforeEach
    void setUp() {
        lenient().when(catalogClient.baseUrl(anyString())).thenReturn(catalogClient);
        lenient().when(catalogClient.build()).thenReturn(restClient);
        service = new OrderService(orders, processedEvents, kafka, catalogClient, MAPPER);
    }

    private OrderEntity givenOrder() {
        OrderEntity order = new OrderEntity(USER_ID, new BigDecimal("99.00"));
        when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(processedEvents.existsByEventId(anyString())).thenReturn(false);
        return order;
    }

    @Test
    void marksOrderPaidOnPaymentCompleted() throws Exception {
        OrderEntity order = givenOrder();
        String body = MAPPER.writeValueAsString(new OrderService.PaymentCompletedEvent(
                "evt-1", ORDER_ID, USER_ID, new BigDecimal("99.00"), "SUCCESS", "SIM-1", Instant.now()));

        service.onPaymentCompleted(body);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orders, times(1)).save(order);
    }

    @Test
    void ignoresAlreadyProcessedPaymentEvent() throws Exception {
        when(processedEvents.existsByEventId("evt-dup")).thenReturn(true);
        String body = MAPPER.writeValueAsString(new OrderService.PaymentCompletedEvent(
                "evt-dup", ORDER_ID, USER_ID, new BigDecimal("99.00"), "SUCCESS", "SIM-1", Instant.now()));

        service.onPaymentCompleted(body);

        verify(orders, never()).findById(any());
        verify(orders, never()).save(any());
        verify(processedEvents, never()).save(any());
    }

    @Test
    void recordsEventTopicForAudit() throws Exception {
        givenOrder();
        String body = MAPPER.writeValueAsString(new OrderService.PaymentCompletedEvent(
                "evt-2", ORDER_ID, USER_ID, new BigDecimal("99.00"), "SUCCESS", "SIM-1", Instant.now()));

        service.onPaymentCompleted(body);

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEvents).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("evt-2");
        assertThat(captor.getValue().getTopic()).isEqualTo("payment.completed");
    }

    @Test
    void cancelsOrderWhenPaymentFails() throws Exception {
        OrderEntity order = givenOrder();
        String body = MAPPER.writeValueAsString(new OrderService.PaymentFailedEvent(
                "evt-3", ORDER_ID, USER_ID, new BigDecimal("99.00"), "declined", Instant.now()));

        service.onPaymentFailed(body);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelledOrderStaysCancelledWhenLateSuccessArrives() throws Exception {
        OrderEntity order = givenOrder();
        String failed = MAPPER.writeValueAsString(new OrderService.PaymentFailedEvent(
                "evt-5", ORDER_ID, USER_ID, new BigDecimal("99.00"), "declined", Instant.now()));
        service.onPaymentFailed(failed);
        when(processedEvents.existsByEventId("evt-6")).thenReturn(false);
        String paid = MAPPER.writeValueAsString(new OrderService.PaymentCompletedEvent(
                "evt-6", ORDER_ID, USER_ID, new BigDecimal("99.00"), "SUCCESS", "SIM-2", Instant.now()));

        service.onPaymentCompleted(paid);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelsOrderWhenInventoryIsInsufficient() throws Exception {
        OrderEntity order = givenOrder();
        String body = MAPPER.writeValueAsString(new OrderService.InventoryFailureEvent(
                "evt-4", ORDER_ID, USER_ID, new BigDecimal("99.00"), "Insufficient stock", Instant.now()));

        service.onInventoryInsufficient(body);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void unknownOrderIdIsAcknowledgedWithoutFailing() throws Exception {
        when(orders.findById(ORDER_ID)).thenReturn(Optional.empty());
        when(processedEvents.existsByEventId(anyString())).thenReturn(false);
        String body = MAPPER.writeValueAsString(new OrderService.PaymentCompletedEvent(
                "evt-7", ORDER_ID, USER_ID, new BigDecimal("99.00"), "SUCCESS", "SIM-3", Instant.now()));

        service.onPaymentCompleted(body);

        verify(orders, never()).save(any());
        verify(processedEvents, times(1)).save(any(ProcessedEvent.class));
    }
}