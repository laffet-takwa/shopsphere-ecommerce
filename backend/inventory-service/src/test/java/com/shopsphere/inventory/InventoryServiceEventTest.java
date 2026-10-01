package com.shopsphere.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceEventTest {

    private static final Long ORDER_ID = 71L;
    private static final Long USER_ID = 7L;

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            JsonMapper.builder().addModule(new JavaTimeModule()).build();

    @Mock
    private InventoryRepository inventory;

    @Mock
    private KafkaTemplate<String, Object> kafka;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(inventory, kafka, MAPPER);
    }

    private static String orderCreated(Long productId, String sku, int quantity) throws Exception {
        return MAPPER.writeValueAsString(new InventoryEvents.OrderCreatedEvent(
                "evt-1", ORDER_ID, USER_ID, new BigDecimal("99.00"),
                List.of(new InventoryEvents.Item(productId, sku, quantity)), Instant.now()));
    }

    private void givenItem(long productId, int quantity) {
        InventoryItem item = new InventoryItem(productId, "SKU-" + productId, quantity);
        lenient().when(inventory.findByProductId(productId)).thenReturn(Optional.of(item));
        lenient().when(inventory.save(any(InventoryItem.class))).thenReturn(item);
        lenient().when(inventory.findByReservationsIsNotEmpty()).thenReturn(List.of(item));
    }

    @Test
    void publishesReservedWhenStockIsSufficient() throws Exception {
        givenItem(1001L, 5);

        service.onOrderCreated(orderCreated(1001L, "SKU-1001", 3));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("inventory.reserved"), eq(String.valueOf(ORDER_ID)), captor.capture());
        InventoryEvents.InventoryResultEvent event = (InventoryEvents.InventoryResultEvent) captor.getValue();
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.userId()).isEqualTo(USER_ID);
    }

    @Test
    void publishesInsufficientAndKeepsStockUntouchedWhenShort() throws Exception {
        InventoryItem item = new InventoryItem(1001L, "SKU-1001", 1);
        when(inventory.findByProductId(1001L)).thenReturn(Optional.of(item));
        when(inventory.save(any(InventoryItem.class))).thenReturn(item);

        service.onOrderCreated(orderCreated(1001L, "SKU-1001", 5));

        verify(kafka).send(eq("inventory.insufficient"), eq(String.valueOf(ORDER_ID)), any());
        assertThat(item.getReservedQuantity()).isZero();
        assertThat(item.availableQuantity()).isEqualTo(1);
    }

    @Test
    void rollsBackEarlierLinesWhenALaterLineIsShort() throws Exception {
        InventoryItem first = new InventoryItem(1001L, "SKU-1001", 10);
        InventoryItem second = new InventoryItem(1002L, "SKU-1002", 1);
        when(inventory.findByProductId(1001L)).thenReturn(Optional.of(first));
        when(inventory.findByProductId(1002L)).thenReturn(Optional.of(second));
        when(inventory.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventory.findByReservationsIsNotEmpty()).thenReturn(List.of(first, second));
        String body = MAPPER.writeValueAsString(new InventoryEvents.OrderCreatedEvent(
                "evt-2", ORDER_ID, USER_ID, new BigDecimal("99.00"),
                List.of(new InventoryEvents.Item(1001L, "SKU-1001", 2),
                        new InventoryEvents.Item(1002L, "SKU-1002", 9)),
                Instant.now()));

        service.onOrderCreated(body);

        verify(kafka).send(eq("inventory.insufficient"), eq(String.valueOf(ORDER_ID)), any());
        assertThat(first.hasReservation(ORDER_ID)).isFalse();
        assertThat(first.getReservedQuantity()).isZero();
        assertThat(second.getReservedQuantity()).isZero();
    }

    @Test
    void releasesReservationsWhenPaymentFails() throws Exception {
        InventoryItem item = new InventoryItem(1001L, "SKU-1001", 5);
        item.reserve(ORDER_ID, 3);
        when(inventory.findByReservationsIsNotEmpty()).thenReturn(List.of(item));
        when(inventory.save(any(InventoryItem.class))).thenReturn(item);
        String body = MAPPER.writeValueAsString(new InventoryService.PaymentFailedEvent(
                "evt-3", ORDER_ID, USER_ID, new BigDecimal("99.00"), "declined", Instant.now()));

        service.onPaymentFailed(body);

        assertThat(item.hasReservation(ORDER_ID)).isFalse();
        assertThat(item.availableQuantity()).isEqualTo(5);
        verify(inventory).save(item);
    }
}