package org.igdevx.spring_boot_microservice_boilerplate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordResponse {
    private String id;
    private String orderId;
    private String paymentIntentId;
    private Long amount;
    private String currency;
    private String status;
    private String paidBy;
    private String paidTo;
    private String stripeAccountId; // Connected account ID
    private Long applicationFeeAmount; // Platform fee in cents
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime paymentDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDueDate;
    
    private String errorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;
}