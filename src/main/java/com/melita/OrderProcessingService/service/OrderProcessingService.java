package com.melita.OrderProcessingService.service;

import com.melita.OrderProcessingService.event.NotificationEvent;
import com.melita.OrderProcessingService.exception.OrderNotFoundException;
import com.melita.OrderProcessingService.model.Order;
import com.melita.OrderProcessingService.model.OrderStatus;
import com.melita.OrderProcessingService.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Order approveOrder(Long orderId) {
        if (orderId == null) {
            log.error("Order ID cannot be null");
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        log.info("Attempting to approve order with ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderId);
                    return new OrderNotFoundException("Order not found: " + orderId);
                });

        if (Objects.requireNonNull(order.getStatus()).equals(OrderStatus.REJECTED) ||
                order.getStatus().equals(OrderStatus.APPROVED) ||
                order.getStatus().equals(OrderStatus.COMPLETED)) {
            log.warn("Order with ID {} is already processed with status: {}", orderId, order.getStatus());
            throw new IllegalStateException("Order already processed.");
        }

        order.setStatus(OrderStatus.APPROVED);
        orderRepository.save(order);
        log.info("Order {} has been approved and saved successfully", orderId);

        NotificationEvent notificationEvent = new NotificationEvent(order.getCustomerEmail(), "Order Approved",
                "Dear " + order.getCustomerName() + ", your order has been approved.");

        try {
            rabbitTemplate.convertAndSend("notification.exchange", "notification.email", notificationEvent);
            log.info("Notification event sent successfully for order ID: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to send notification for order ID: {}", orderId, e);
            throw new RuntimeException("Failed to send notification", e);
        }

        return order;
    }
}
