package org.igdevx.spring_boot_microservice_boilerplate.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.spring_boot_microservice_boilerplate.dto.*;
import org.igdevx.spring_boot_microservice_boilerplate.entity.OrderPayment;
import org.igdevx.spring_boot_microservice_boilerplate.repository.OrderPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderPaymentService {

    private final OrderPaymentRepository orderPaymentRepository;
    private final StripePaymentService stripePaymentService;

    /**
     * Record payment for an order (upsert pattern)
     */
    @Transactional
    public PaymentRecordResponse recordPayment(String orderId, PaymentRecordRequest request) {
        log.info("Recording payment for order: {} with payment intent: {}", orderId, request.getPaymentIntentId());

        // Find existing payment record or create new one
        Optional<OrderPayment> existingPayment = orderPaymentRepository.findByOrderId(orderId);
        
        OrderPayment payment;
        if (existingPayment.isPresent()) {
            // Update existing payment (upsert behavior)
            payment = existingPayment.get();
            log.info("Updating existing payment record for order: {}", orderId);
        } else {
            // Create new payment record
            payment = new OrderPayment();
            payment.setOrderId(orderId);
            log.info("Creating new payment record for order: {}", orderId);
        }

        // Update payment details
        payment.setPaymentIntentId(request.getPaymentIntentId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(OrderPayment.PaymentStatus.valueOf(request.getStatus().toUpperCase()));
        payment.setPaidBy(request.getPaidBy());
        payment.setPaidTo(request.getPaidTo());
        payment.setStripeAccountId(request.getStripeAccountId());
        payment.setApplicationFeeAmount(request.getApplicationFeeAmount());
        payment.setPaymentDate(request.getPaymentDate());
        payment.setPaymentDueDate(request.getPaymentDueDate());
        payment.setErrorMessage(request.getErrorMessage());

        OrderPayment savedPayment = orderPaymentRepository.save(payment);
        log.info("Payment recorded successfully with ID: {}", savedPayment.getId());

        return mapToResponse(savedPayment);
    }

    /**
     * Get payment status for an order
     */
    public PaymentRecordResponse getPaymentStatus(String orderId) {
        log.info("Retrieving payment status for order: {}", orderId);

        Optional<OrderPayment> payment = orderPaymentRepository.findByOrderId(orderId);
        
        if (payment.isPresent()) {
            return mapToResponse(payment.get());
        } else {
            throw new RuntimeException("No payment record found for order: " + orderId);
        }
    }

    /**
     * Verify payment with Stripe (server-side verification)
     */
    public PaymentRecordResponse verifyPayment(String paymentIntentId) {
        log.info("Verifying payment intent: {}", paymentIntentId);

        try {
            // Get payment intent from Stripe
            PaymentIntent stripePayment = stripePaymentService.retrievePaymentIntent(paymentIntentId);
            
            // Find local payment record
            Optional<OrderPayment> localPayment = orderPaymentRepository.findByPaymentIntentId(paymentIntentId);
            
            if (localPayment.isPresent()) {
                OrderPayment payment = localPayment.get();
                
                // Update status based on Stripe data (webhook priority)
                OrderPayment.PaymentStatus stripeStatus = mapStripeStatus(stripePayment.getStatus());
                if (!payment.getStatus().equals(stripeStatus)) {
                    log.info("Updating payment status from {} to {} based on Stripe verification", 
                            payment.getStatus(), stripeStatus);
                    payment.setStatus(stripeStatus);
                    payment = orderPaymentRepository.save(payment);
                }
                
                return mapToResponse(payment);
            } else {
                throw new RuntimeException("No local payment record found for payment intent: " + paymentIntentId);
            }
            
        } catch (StripeException e) {
            log.error("Error verifying payment intent with Stripe: {}", e.getMessage());
            throw new RuntimeException("Failed to verify payment with Stripe", e);
        }
    }

    /**
     * Update order status (placeholder - would integrate with order service)
     */
    public OrderStatusUpdateResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request) {
        log.info("Updating order status for order: {} to status: {}", orderId, request.getStatus());
        
        // In a real implementation, this would call the order service
        // For now, we'll just return a response indicating the operation
        return new OrderStatusUpdateResponse(
                orderId,
                request.getStatus(),
                "unknown", // Would get from order service
                LocalDateTime.now(),
                "payment-service" // Updated by this service
        );
    }

    /**
     * Map OrderPayment entity to response DTO
     */
    private PaymentRecordResponse mapToResponse(OrderPayment payment) {
        return new PaymentRecordResponse(
                payment.getId().toString(),
                payment.getOrderId(),
                payment.getPaymentIntentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().toString(),
                payment.getPaidBy(),
                payment.getPaidTo(),
                payment.getStripeAccountId(),
                payment.getApplicationFeeAmount(),
                payment.getPaymentDate(),
                payment.getPaymentDueDate(),
                payment.getErrorMessage(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    /**
     * Map Stripe payment status to our enum
     */
    private OrderPayment.PaymentStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded", "requires_capture" -> OrderPayment.PaymentStatus.SUCCEEDED;
            case "processing", "requires_payment_method", "requires_confirmation" -> OrderPayment.PaymentStatus.PENDING;
            default -> OrderPayment.PaymentStatus.FAILED;
        };
    }
}