package com.melita.OrderProcessingService;

import com.melita.OrderProcessingService.event.service.EventPublisherService;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisherService eventPublisherService;

    @InjectMocks
    private OrderProcessingServiceImpl orderProcessingServiceImpl;

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
        verify(eventPublisherService, times(1)).publishOrderApprovedEvent(order);
        verify(eventPublisherService, times(1)).publishNotificationEvent(order);
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
        lenient().when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        orderProcessingServiceImpl.approveOrder(order);

        // Then
        verify(orderRepository, never()).save(any());
        verify(eventPublisherService, never()).publishOrderApprovedEvent(any());
        verify(eventPublisherService, never()).publishNotificationEvent(any());
    }

    @Test
    void approveOrder_DoesNothingWhenOrderRejected() {
        // Given
        order.setStatus(OrderStatus.REJECTED);
        lenient().when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        orderProcessingServiceImpl.approveOrder(order);

        // Then
        verify(orderRepository, never()).save(any());
        verify(eventPublisherService, never()).publishOrderApprovedEvent(any());
        verify(eventPublisherService, never()).publishNotificationEvent(any());
    }

    @Test
    void approveOrder_DoesNothingWhenOrderCompleted() {
        // Given
        order.setStatus(OrderStatus.COMPLETED);
        lenient().when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // When
        orderProcessingServiceImpl.approveOrder(order);

        // Then
        verify(orderRepository, never()).save(any());
        verify(eventPublisherService, never()).publishOrderApprovedEvent(any());
        verify(eventPublisherService, never()).publishNotificationEvent(any());
    }
}
