package com.melita.OrderProcessingService.event.service;

import com.melita.OrderProcessingService.model.Order;
import org.springframework.stereotype.Service;

@Service
public interface EventPublisherService {
    public void publishOrderApprovedEvent(Order order);
    public void publishNotificationEvent(Order order);
}
