package org.igdevx.spring_boot_microservice_boilerplate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.spring_boot_microservice_boilerplate.dto.*;
import org.igdevx.spring_boot_microservice_boilerplate.service.OrderPaymentService;
import org.igdevx.spring_boot_microservice_boilerplate.service.WebhookService;
import org.igdevx.spring_boot_microservice_boilerplate.service.ConnectWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentController {

    private final OrderPaymentService orderPaymentService;
    private final WebhookService webhookService;
    private final ConnectWebhookService connectWebhookService;

    /**
     * 1. Record Payment (upsert pattern)
     * POST /api/orders/{orderId}/payment
     */
    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<PaymentRecordResponse> recordPayment(
            @PathVariable String orderId,
            @RequestBody PaymentRecordRequest request) {
        
        try {
            log.info("Recording payment for order: {}", orderId);
            PaymentRecordResponse response = orderPaymentService.recordPayment(orderId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Bad request for order payment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error recording payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 2. Get Payment Status
     * GET /api/orders/{orderId}/payment
     */
    @GetMapping("/orders/{orderId}/payment")
    public ResponseEntity<PaymentRecordResponse> getPaymentStatus(@PathVariable String orderId) {
        try {
            log.info("Getting payment status for order: {}", orderId);
            PaymentRecordResponse response = orderPaymentService.getPaymentStatus(orderId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Payment not found for order: {}", orderId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting payment status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 3. Update Order Status
     * PATCH /api/orders/{orderId}/status
     */
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<OrderStatusUpdateResponse> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody OrderStatusUpdateRequest request) {
        
        try {
            log.info("Updating order status for order: {} to: {}", orderId, request.getStatus());
            OrderStatusUpdateResponse response = orderPaymentService.updateOrderStatus(orderId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 4. Stripe Webhook Handler (Standard Account Webhooks)
     * POST /api/webhooks/stripe
     */
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Map<String, Boolean>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        try {
            log.info("Received Stripe webhook");
            webhookService.processWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("received", false));
        }
    }

    /**
     * 4b. Stripe Connect Webhook Handler (Connected Account Events)
     * POST /api/webhooks/stripe/connect
     * 
     * Use Stripe CLI for local testing:
     * stripe listen --forward-connect-to localhost:5004/api/webhooks/stripe/connect
     */
    @PostMapping("/webhooks/stripe/connect")
    public ResponseEntity<Map<String, Boolean>> handleConnectWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        try {
            log.info("Received Stripe Connect webhook");
            connectWebhookService.processConnectWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            log.error("Error processing Connect webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("received", false));
        }
    }

    /**
     * 5. Payment Verification (Optional but recommended)
     * POST /api/payments/verify/{paymentIntentId}
     */
    @PostMapping("/payments/verify/{paymentIntentId}")
    public ResponseEntity<PaymentRecordResponse> verifyPayment(@PathVariable String paymentIntentId) {
        try {
            log.info("Verifying payment intent: {}", paymentIntentId);
            PaymentRecordResponse response = orderPaymentService.verifyPayment(paymentIntentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error verifying payment: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error verifying payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}