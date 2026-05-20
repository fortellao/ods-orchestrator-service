package com.fortellao.ods.orchestration.saga;

import com.fortellao.ods.orchestration.domain.inventory.InventoryCommand;
import com.fortellao.ods.orchestration.domain.inventory.InventoryEvent;
import com.fortellao.ods.orchestration.domain.inventory.InventoryOperation;
import com.fortellao.ods.orchestration.domain.order.Item;
import com.fortellao.ods.orchestration.domain.order.OrderCommand;
import com.fortellao.ods.orchestration.domain.order.OrderEvent;
import com.fortellao.ods.orchestration.domain.order.Order;
import com.fortellao.ods.orchestration.domain.order.OrderStatus;
import com.fortellao.ods.orchestration.domain.payment.PaymentCommand;
import com.fortellao.ods.orchestration.domain.payment.PaymentEvent;
import com.fortellao.ods.orchestration.store.InMemorySagaOrderStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderOrchestratorServiceTest {

    @Mock
    private OrchestratorCommandPublisher publisher;

    @Captor
    private ArgumentCaptor<InventoryCommand> inventoryCommandCaptor;

    @Captor
    private ArgumentCaptor<PaymentCommand> paymentCommandCaptor;

    @Captor
    private ArgumentCaptor<OrderCommand> orderCommandCaptor;

    private InMemorySagaOrderStore orderStore;
    private OrderOrchestratorService service;

    @BeforeEach
    void setUp() {
        orderStore = new InMemorySagaOrderStore();
        service = new OrderOrchestratorService(publisher, orderStore);
    }

    @Test
    void onOrderReceived_savesPendingOrderAndRequestsInventoryCheckout() {
        OrderEvent event = new OrderEvent();
        event.setCustomerId("cust-8821");
        event.setPaymentId("pay-4493");
        event.setItems(List.of(
                item("prod-001", 2), 
                item("prod-047", 1)
        ));

        service.onOrderReceived(event);

        verify(publisher).sendInventoryCommand(inventoryCommandCaptor.capture());
        InventoryCommand cmd = inventoryCommandCaptor.getValue();

        assertNotNull(cmd.getOrderId());
        assertEquals(InventoryOperation.CHECKOUT, cmd.getOperation());
        assertEquals(event.getItems(), cmd.getItems());

        Order stored = orderStore.find(cmd.getOrderId());
        assertNotNull(stored);
        assertEquals(OrderStatus.PENDING, stored.getStatus());
        assertEquals(event.getItems(), stored.getItems());
    }

    @Test
    void onInventoryEvent_onSuccess_requestsPayment() {
        Order order = savedOrder();

        InventoryEvent event = new InventoryEvent();
        event.setOrderId(order.getOrderId());
        event.setSuccess(true);
        event.setTotalPrice(BigDecimal.valueOf(74.48));

        service.onInventoryEvent(event);

        verify(publisher).sendPaymentCommand(paymentCommandCaptor.capture());
        PaymentCommand cmd = paymentCommandCaptor.getValue();

        assertEquals(order.getOrderId(), cmd.getOrderId());
        assertEquals(order.getPaymentId(), cmd.getPaymentId());
        assertEquals(event.getTotalPrice(), cmd.getTotalAmount());
        assertNotNull(orderStore.find(order.getOrderId()));
    }

    @Test
    void onInventoryEvent_onFailure_sendsFailedStatusAndRemovesOrderFromStore() {
        Order order = savedOrder();

        InventoryEvent event = new InventoryEvent();
        event.setOrderId(order.getOrderId());
        event.setSuccess(false);

        service.onInventoryEvent(event);

        verify(publisher).sendOrderCommand(orderCommandCaptor.capture());
        assertEquals(order.getOrderId(), orderCommandCaptor.getValue().getOrderId());
        assertEquals(OrderStatus.FAILED, orderCommandCaptor.getValue().getStatus());
        verify(publisher, never()).sendInventoryCommand(any());
        assertNull(orderStore.find(order.getOrderId()));
    }

    @Test
    void onPaymentEvent_onSuccess_confirmsOrderAndRemovesItFromStore() {
        Order order = savedOrder();

        PaymentEvent event = new PaymentEvent();
        event.setOrderId(order.getOrderId());
        event.setSuccess(true);

        service.onPaymentEvent(event);

        verify(publisher).sendOrderCommand(orderCommandCaptor.capture());
        assertEquals(order.getOrderId(), orderCommandCaptor.getValue().getOrderId());
        assertEquals(OrderStatus.CONFIRMED, orderCommandCaptor.getValue().getStatus());
        assertNull(orderStore.find(order.getOrderId()));
    }

    @Test
    void onPaymentEvent_onFailure_releasesInventoryThenSendsFailedStatus() {
        Order order = savedOrder();

        PaymentEvent event = new PaymentEvent();
        event.setOrderId(order.getOrderId());
        event.setSuccess(false);

        service.onPaymentEvent(event);

        InOrder inOrder = inOrder(publisher);
        inOrder.verify(publisher).sendInventoryCommand(inventoryCommandCaptor.capture());
        inOrder.verify(publisher).sendOrderCommand(orderCommandCaptor.capture());

        assertEquals(InventoryOperation.RELEASE, inventoryCommandCaptor.getValue().getOperation());
        assertEquals(order.getOrderId(), inventoryCommandCaptor.getValue().getOrderId());
        assertEquals(OrderStatus.FAILED, orderCommandCaptor.getValue().getStatus());
        assertNull(orderStore.find(order.getOrderId()));
    }

    private Order savedOrder() {
        OrderEvent request = new OrderEvent();
        request.setPaymentId("pay-4493");
        request.setItems(List.of());

        Order order = new Order(request);
        order.setOrderId("order-123");
        order.setStatus(OrderStatus.PENDING);
        orderStore.save(order);
        return order;
    }

    private static Item item(String productId, int quantity) {
        Item item = new Item();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}