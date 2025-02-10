package com.melita.OrderProcessingService.service;

import com.melita.OrderProcessingService.model.Order;
import org.springframework.stereotype.Service;

@Service
public interface OrderProcessingService {
    public void approveOrder(Order order);
    public void manuallyApproveOrder(Long orderId);

}
