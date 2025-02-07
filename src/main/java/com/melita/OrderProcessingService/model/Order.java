package com.melita.OrderProcessingService.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String customerName;

    @NotNull
    private String customerEmail;

    @NotNull
    private String customerPhone;

    @NotNull
    private String installationAddress;

    @NotNull
    private LocalDateTime installationDate;

    @NotNull
    private String timeSlot;

    @ElementCollection
    private List<String> productTypes;

    @ElementCollection
    private List<String> packageTypes;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
}

