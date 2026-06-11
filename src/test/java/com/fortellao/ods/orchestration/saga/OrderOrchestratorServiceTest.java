package com.fortellao.ods.orchestration.saga;

import com.fortellao.ods.orchestration.domain.product.ProductCommand;
import com.fortellao.ods.orchestration.domain.product.ProductEvent;
import com.fortellao.ods.orchestration.domain.product.ProductOperation;
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
    private ArgumentCaptor<ProductCommand> productCommandCaptor;

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
    void onOrderReceived_savesPendingOrderAndRequestsProductCheckout() {
        OrderEvent event = new OrderEvent(
                "cust-8821",
                "pay-4493",
                List.of(item("prod-001", 2), item("prod-047", 1))
        );

        service.onOrderReceived(event);

        verify(publisher).sendProductCommand(productCommandCaptor.capture());
        ProductCommand cmd = productCommandCaptor.getValue();

        assertNotNull(cmd.orderId());
        assertEquals(ProductOperation.CHECKOUT, cmd.operation());
        assertEquals(event.items(), cmd.items());

        Order stored = orderStore.find(cmd.orderId());
        assertNotNull(stored);
        assertEquals(OrderStatus.PENDING, stored.status());
        assertEquals(event.items(), stored.items());
    }

    @Test
    void onProductEvent_onSuccess_requestsPayment() {
        Order order = savedOrder();

        ProductEvent event = new ProductEvent(order.orderId(), true, null, BigDecimal.valueOf(74.48));

        service.onProductEvent(event);

        verify(publisher).sendPaymentCommand(paymentCommandCaptor.capture());
        PaymentCommand cmd = paymentCommandCaptor.getValue();

        assertEquals(order.orderId(), cmd.orderId());
        assertEquals(order.paymentId(), cmd.paymentId());
        assertEquals(event.totalPrice(), cmd.totalAmount());
        assertNotNull(orderStore.find(order.orderId()));
    }

    @Test
    void onProductEvent_onFailure_sendsFailedStatusAndRemovesOrderFromStore() {
        Order order = savedOrder();

        ProductEvent event = new ProductEvent(order.orderId(), false, null, null);

        service.onProductEvent(event);

        verify(publisher).sendOrderCommand(orderCommandCaptor.capture());
        assertEquals(order.orderId(), orderCommandCaptor.getValue().orderId());
        assertEquals(OrderStatus.FAILED, orderCommandCaptor.getValue().status());
        verify(publisher, never()).sendProductCommand(any());
        assertNull(orderStore.find(order.orderId()));
    }

    @Test
    void onPaymentEvent_onSuccess_confirmsOrderAndRemovesItFromStore() {
        Order order = savedOrder();

        PaymentEvent event = new PaymentEvent(order.orderId(), true);

        service.onPaymentEvent(event);

        verify(publisher).sendOrderCommand(orderCommandCaptor.capture());
        assertEquals(order.orderId(), orderCommandCaptor.getValue().orderId());
        assertEquals(OrderStatus.CONFIRMED, orderCommandCaptor.getValue().status());
        assertNull(orderStore.find(order.orderId()));
    }

    @Test
    void onPaymentEvent_onFailure_releasesProductReservationThenSendsFailedStatus() {
        Order order = savedOrder();

        PaymentEvent event = new PaymentEvent(order.orderId(), false);

        service.onPaymentEvent(event);

        InOrder inOrder = inOrder(publisher);
        inOrder.verify(publisher).sendProductCommand(productCommandCaptor.capture());
        inOrder.verify(publisher).sendOrderCommand(orderCommandCaptor.capture());

        assertEquals(ProductOperation.RELEASE, productCommandCaptor.getValue().operation());
        assertEquals(order.orderId(), productCommandCaptor.getValue().orderId());
        assertEquals(OrderStatus.FAILED, orderCommandCaptor.getValue().status());
        assertNull(orderStore.find(order.orderId()));
    }

    private Order savedOrder() {
        OrderEvent request = new OrderEvent(null, "pay-4493", List.of());

        Order order = new Order("order-123", OrderStatus.PENDING, request);
        orderStore.save(order);
        return order;
    }

    private static Item item(String productId, int quantity) {
        return new Item(productId, quantity);
    }
}