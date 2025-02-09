package com.melita.OrderProcessingService;

import com.melita.OrderProcessingService.event.NotificationEvent;
import com.melita.OrderProcessingService.exception.OrderNotFoundException;
import com.melita.OrderProcessingService.model.Order;
import com.melita.OrderProcessingService.model.OrderStatus;
import com.melita.OrderProcessingService.repository.OrderRepository;
import com.melita.OrderProcessingService.service.OrderProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderProcessingService orderProcessingService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setCustomerEmail("customer@example.com");
        order.setCustomerName("John Doe");
    }

    @Test
    void testApproveOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order approvedOrder = orderProcessingService.approveOrder(1L);

        assertNotNull(approvedOrder);
        assertEquals(OrderStatus.APPROVED, approvedOrder.getStatus());
        verify(orderRepository).save(order);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }

    @Test
    void testApproveOrder_ThrowsException_WhenOrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(OrderNotFoundException.class, () -> orderProcessingService.approveOrder(1L));

        assertEquals("Order not found: 1", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }

    @Test
    void testApproveOrder_ThrowsException_WhenOrderAlreadyProcessed() {
        order.setStatus(OrderStatus.APPROVED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Exception exception = assertThrows(IllegalStateException.class, () -> orderProcessingService.approveOrder(1L));

        assertEquals("Order already processed.", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }

    @Test
    void testApproveOrder_ThrowsException_WhenOrderIdIsNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> orderProcessingService.approveOrder(null));

        assertEquals("Order ID cannot be null", exception.getMessage());
        verify(orderRepository, never()).findById(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }

    @Test
    void testApproveOrder_ThrowsException_WhenNotificationFails() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        doThrow(new RuntimeException("Messaging error")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));

        Exception exception = assertThrows(RuntimeException.class, () -> orderProcessingService.approveOrder(1L));

        assertEquals("Failed to send notification", exception.getMessage());
        verify(orderRepository).save(order);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(NotificationEvent.class));
    }
}
