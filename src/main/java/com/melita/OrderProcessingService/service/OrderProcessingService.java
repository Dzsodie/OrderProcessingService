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

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Order approveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getStatus().equals(OrderStatus.REJECTED)
                || order.getStatus().equals(OrderStatus.APPROVED)
                || order.getStatus().equals(OrderStatus.COMPLETED)) {
            throw new IllegalStateException("Order already processed.");
        }

        order.setStatus(OrderStatus.APPROVED);
        orderRepository.save(order);

        NotificationEvent notificationEvent = new NotificationEvent(order.getCustomerEmail(), "Order Approved",
                "Dear " + order.getCustomerName() + ", your order has been approved.");
        rabbitTemplate.convertAndSend("notification.exchange", "notification.email", notificationEvent);

        log.info("Order {} approved. Notification event sent.", orderId);
        return order;
    }

}
