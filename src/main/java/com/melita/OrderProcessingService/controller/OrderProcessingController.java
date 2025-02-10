package com.melita.OrderProcessingService.controller;

import com.melita.OrderProcessingService.service.OrderProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderProcessingController {

    private final OrderProcessingService orderProcessingService;

    @PutMapping("/{orderId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveOrder(@PathVariable Long orderId) {
        orderProcessingService.manuallyApproveOrder(orderId);
        return ResponseEntity.ok("Order " + orderId + " approved successfully.");
    }
}
