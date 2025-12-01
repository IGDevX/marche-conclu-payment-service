package org.igdevx.spring_boot_microservice_boilerplate.controller;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.spring_boot_microservice_boilerplate.dto.ErrorResponse;
import org.igdevx.spring_boot_microservice_boilerplate.dto.PaymentIntentRequest;
import org.igdevx.spring_boot_microservice_boilerplate.dto.PaymentIntentResponse;
import org.igdevx.spring_boot_microservice_boilerplate.service.StripePaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for direct Stripe payment operations (create, retrieve, cancel payment intents)
 * For order-related payment tracking, see OrderPaymentController
 */
@RestController
@RequestMapping("/stripe-payments")
@RequiredArgsConstructor
@Slf4j
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;

    /**
     * Create a payment intent (supports connected accounts)
     * POST /stripe-payments/create-intent
     */
    @PostMapping("/create-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        try {
            log.info("Received payment intent request for amount: {}", request.getAmount());
            
            // Validate request
            if (request.getAmount() == null || request.getAmount() <= 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_AMOUNT", "Amount must be greater than 0"));
            }
            
            if (request.getCurrency() == null || request.getCurrency().isBlank()) {
                request.setCurrency("eur"); // Default to EUR
            }

            // Validate connected account payment
            if (request.getProducerKeycloakId() != null) {
                log.info("Creating connected account payment for producer: {}", request.getProducerKeycloakId());
                
                // Validate application fee
                if (request.getApplicationFeeAmount() != null && request.getApplicationFeeAmount() >= request.getAmount()) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse("INVALID_FEE", "Application fee cannot be greater than or equal to payment amount"));
                }
            }

            PaymentIntentResponse response = stripePaymentService.createPaymentIntent(request);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Business logic error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BUSINESS_ERROR", e.getMessage()));
        } catch (StripeException e) {
            log.error("Stripe error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("STRIPE_ERROR", e.getUserMessage()));
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    /**
     * Retrieve payment intent status
     * GET /stripe-payments/{paymentIntentId}
     */
    @GetMapping("/{paymentIntentId}")
    public ResponseEntity<?> getPaymentIntent(@PathVariable String paymentIntentId) {
        try {
            var paymentIntent = stripePaymentService.retrievePaymentIntent(paymentIntentId);
            return ResponseEntity.ok(new PaymentIntentResponse(
                    paymentIntent.getClientSecret(),
                    paymentIntent.getId(),
                    paymentIntent.getStatus()
            ));
        } catch (StripeException e) {
            log.error("Error retrieving payment intent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", e.getUserMessage()));
        }
    }

    /**
     * Cancel a payment intent
     * DELETE /stripe-payments/{paymentIntentId}
     */
    @DeleteMapping("/{paymentIntentId}")
    public ResponseEntity<?> cancelPaymentIntent(@PathVariable String paymentIntentId) {
        try {
            var cancelledIntent = stripePaymentService.cancelPaymentIntent(paymentIntentId);
            return ResponseEntity.ok(new PaymentIntentResponse(
                    cancelledIntent.getClientSecret(),
                    cancelledIntent.getId(),
                    cancelledIntent.getStatus()
            ));
        } catch (StripeException e) {
            log.error("Error cancelling payment intent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("CANCELLATION_FAILED", e.getUserMessage()));
        }
    }

    /**
     * Health check endpoint
     * GET /stripe-payments/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment service is running");
    }
}
