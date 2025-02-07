package com.melita.OrderProcessingService.controller;

import com.melita.OrderProcessingService.model.Order;
import com.melita.OrderProcessingService.service.OrderProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order-approval")
@RequiredArgsConstructor
public class OrderApprovalController {

    private final OrderProcessingService orderProcessingService;

    @PutMapping("/{orderId}/approve")
    public ResponseEntity<Order> approveOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderProcessingService.approveOrder(orderId));
    }
}

