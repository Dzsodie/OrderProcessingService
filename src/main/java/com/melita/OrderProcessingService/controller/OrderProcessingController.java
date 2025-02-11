package com.melita.OrderProcessingService.controller;

import com.melita.OrderProcessingService.service.OrderProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Processing", description = "Endpoints for processing orders")
public class OrderProcessingController {

    private final OrderProcessingService orderProcessingService;

    @PutMapping("/{orderId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve an order manually",
            description = "Allows an admin to manually approve an order by ID")
    public ResponseEntity<String> approveOrder(
            @Parameter(description = "ID of the order to be approved", required = true)
            @PathVariable Long orderId) {

        if (orderId == null || orderId <= 0) {
            log.warn("Invalid order ID provided: {}", orderId);
            return ResponseEntity.badRequest().body("Invalid order ID");
        }

        try {
            log.info("Approving order with ID: {}", orderId);
            orderProcessingService.manuallyApproveOrder(orderId);
            log.info("Order {} approved successfully", orderId);
            return ResponseEntity.ok("Order " + orderId + " approved successfully.");
        } catch (Exception e) {
            log.error("Error while approving order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("An error occurred while processing the order.");
        }
    }
}
