package org.igdevx.spring_boot_microservice_boilerplate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequest {
    private String status; // "paid", "pending", etc.
    private String paymentIntentId;
}