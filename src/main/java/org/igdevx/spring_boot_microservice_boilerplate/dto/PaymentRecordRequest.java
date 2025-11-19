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
public class PaymentRecordRequest {
    private String paymentIntentId;
    private Long amount;
    private String currency = "eur";
    private String status; // "pending", "succeeded", "failed"
    private String paidBy;
    private String paidTo;
    private String stripeAccountId; // Connected account ID
    private Long applicationFeeAmount; // Platform fee in cents
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime paymentDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDueDate;
    
    private String errorMessage;
}