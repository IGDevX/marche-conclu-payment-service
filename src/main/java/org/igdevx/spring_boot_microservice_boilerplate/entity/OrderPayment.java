package org.igdevx.spring_boot_microservice_boilerplate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "order_id", nullable = false, unique = true) // One payment per order
    private String orderId;
    
    @Column(name = "payment_intent_id", nullable = false, unique = true)
    private String paymentIntentId;
    
    @Column(nullable = false)
    private Long amount; // Amount in cents
    
    @Column(nullable = false, length = 3)
    private String currency = "eur";
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Column(name = "paid_by", nullable = false)
    private String paidBy; // Keycloak user ID
    
    @Column(name = "paid_to", nullable = false)
    private String paidTo; // Producer name or Keycloak ID
    
    @Column(name = "stripe_account_id")
    private String stripeAccountId; // Connected account ID
    
    @Column(name = "application_fee_amount")
    private Long applicationFeeAmount; // Platform fee in cents
    
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;
    
    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum PaymentStatus {
        PENDING, SUCCEEDED, FAILED
    }
}