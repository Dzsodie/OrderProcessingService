package com.melita.OrderProcessingService;

import com.melita.OrderProcessingService.event.model.NotificationEvent;
import com.melita.OrderProcessingService.event.model.OrderApprovedEvent;
import com.melita.OrderProcessingService.exception.OrderNotFoundException;
import com.melita.OrderProcessingService.model.Order;
import com.melita.OrderProcessingService.model.OrderStatus;
import com.melita.OrderProcessingService.repository.OrderRepository;
import com.melita.OrderProcessingService.service.OrderProcessingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderProcessingServiceImpl orderProcessingServiceImpl;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.order.approved}")
    private String orderApprovedRoutingKey;

    @Value("${rabbitmq.routing.notification}")
    private String notificationRoutingKey;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setCustomerEmail("customer@example.com");
        order.setCustomerName("John Doe");
        order.setStatus(OrderStatus.PENDING);
    }

    @Test
    void approveOrder_SuccessfulApproval() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderProcessingServiceImpl.approveOrder(order);

        // Then
        assertEquals(OrderStatus.APPROVED, order.getStatus());
        verify(orderRepository, times(1)).save(order);
        verify(rabbitTemplate, times(1)).convertAndSend(exchangeName, orderApprovedRoutingKey, new OrderApprovedEvent(order));
        verify(rabbitTemplate, times(1)).convertAndSend(exchangeName, notificationRoutingKey, new NotificationEvent(
                order.getCustomerEmail(),
                "Order Approved",
                "Dear " + order.getCustomerName() + ", your order has been approved."
        ));
    }

    @Test
    void approveOrder_ThrowsExceptionWhenOrderIdIsNull() {
        // Given
        order.setId(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderProcessingServiceImpl.approveOrder(order);
        });

        assertEquals("Order ID cannot be null", exception.getMessage());
    }

    @Test
    void approveOrder_ThrowsExceptionWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class, () -> {
            orderProcessingServiceImpl.approveOrder(order);
        });

        assertEquals("Order not found: 1", exception.getMessage());
    }

    @Test
    void approveOrder_DoesNothingWhenOrderAlreadyApproved() {
        // Given
        order.setStatus(OrderStatus.APPROVED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        orderProcessingServiceImpl.approveOrder(order);

        // Then
        verify(orderRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(Optional.ofNullable(any()), any(), any());
    }

    @Test
    void approveOrder_DoesNothingWhenOrderRejected() {
        // Given
        order.setStatus(OrderStatus.REJECTED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        orderProcessingServiceImpl.approveOrder(order);

        // Then
        verify(orderRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(Optional.ofNullable(any()), any(), any());
    }

    @Test
    void approveOrder_DoesNothingWhenOrderCompleted() {
        // Given
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        orderProcessingServiceImpl.approveOrder(order);

        // Then
        verify(orderRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(Optional.ofNullable(any()), any(), any());
    }
}
