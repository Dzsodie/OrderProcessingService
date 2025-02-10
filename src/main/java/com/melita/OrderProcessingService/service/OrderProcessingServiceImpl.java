package com.melita.OrderProcessingService.service;

import com.melita.OrderProcessingService.event.service.EventPublisherService;
import com.melita.OrderProcessingService.exception.OrderNotFoundException;
import com.melita.OrderProcessingService.model.Order;
import com.melita.OrderProcessingService.model.OrderStatus;
import com.melita.OrderProcessingService.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessingServiceImpl implements OrderProcessingService {

    private final OrderRepository orderRepository;
    private final EventPublisherService eventPublisherService;

    @Override
    @Transactional
    @RabbitListener(queues = "order-processing.queue")
    public void approveOrder(Order order) {
        if (order.getStatus() == OrderStatus.REJECTED ||
                order.getStatus() == OrderStatus.APPROVED ||
                order.getStatus() == OrderStatus.COMPLETED) {
            log.warn("Skipping order {} with status: {}", order.getId(), order.getStatus());
            return;
        }

        validateOrder(order);
        Order processedOrder = processApproval(order);
        eventPublisherService.publishOrderApprovedEvent(processedOrder);
        eventPublisherService.publishNotificationEvent(processedOrder);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void manuallyApproveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        Order processedOrder = processApproval(order);
        eventPublisherService.publishOrderApprovedEvent(processedOrder);
        eventPublisherService.publishNotificationEvent(processedOrder);
    }

    private void validateOrder(Order order) {
        if (order.getId() == null) {
            log.error("Order ID cannot be null");
            throw new IllegalArgumentException("Order ID cannot be null");
        }
    }

    private Order processApproval(Order order) {
        log.info("Approving order with ID: {}", order.getId());
        Order processedOrder = orderRepository.findById(order.getId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + order.getId()));

        if (processedOrder.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is already processed with status: {}", processedOrder.getId(), processedOrder.getStatus());
            return processedOrder;
        }

        processedOrder.setStatus(OrderStatus.APPROVED);
        orderRepository.save(processedOrder);
        log.info("Order {} approved and saved successfully", processedOrder.getId());
        return processedOrder;
    }
}
