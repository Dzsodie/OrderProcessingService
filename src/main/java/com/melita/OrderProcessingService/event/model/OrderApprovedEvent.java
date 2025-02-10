package com.melita.OrderProcessingService.event.model;

import com.melita.OrderProcessingService.model.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderApprovedEvent {
    private Order order;
}