package com.melita.OrderProcessingService.event.service;

import com.melita.OrderProcessingService.event.model.NotificationEvent;
import com.melita.OrderProcessingService.event.model.OrderApprovedEvent;
import com.melita.OrderProcessingService.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventPublisherServiceServiceImpl implements EventPublisherService {
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.order.approved}")
    private String orderApprovedRoutingKey;

    @Value("${rabbitmq.routing.notification}")
    private String notificationRoutingKey;

    @Override
    public void publishOrderApprovedEvent(Order order) {
        OrderApprovedEvent event = new OrderApprovedEvent(order);
        rabbitTemplate.convertAndSend(exchangeName, orderApprovedRoutingKey, event);
        log.info("OrderApprovedEvent sent for order ID: {}", order.getId());
    }

    @Override
    public void publishNotificationEvent(Order order) {
        NotificationEvent notificationEvent = new NotificationEvent(
                order.getCustomerEmail(),
                "Order Approved",
                "Dear " + order.getCustomerName() + ", your order has been approved."
        );
        rabbitTemplate.convertAndSend(exchangeName, notificationRoutingKey, notificationEvent);
        log.info("NotificationEvent sent successfully for order ID: {}", order.getId());
    }
}
