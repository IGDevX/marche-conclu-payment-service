package org.igdevx.spring_boot_microservice_boilerplate.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.spring_boot_microservice_boilerplate.entity.OrderPayment;
import org.igdevx.spring_boot_microservice_boilerplate.repository.OrderPaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final OrderPaymentRepository orderPaymentRepository;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    /**
     * Process Stripe webhook events
     */
    @Transactional
    public void processWebhookEvent(String payload, String sigHeader) {
        try {
            Event event;
            
            // Verify webhook signature - required for security
            if (webhookSecret == null || webhookSecret.isEmpty()) {
                throw new RuntimeException("Webhook secret must be configured for security");
            }
            
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Webhook signature verified successfully");

            log.info("Processing webhook event: {} with ID: {}", event.getType(), event.getId());

            // Handle payment-related events
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentFailed(event);
                    break;
                case "payment_intent.processing":
                    handlePaymentProcessing(event);
                    break;
                case "charge.refunded":
                    handleChargeRefunded(event);
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }

        } catch (SignatureVerificationException e) {
            log.error("Invalid signature for webhook: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook signature", e);
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process webhook", e);
        }
    }

    /**
     * Handle successful payment
     */
    private void handlePaymentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        
        if (paymentIntent == null) {
            log.error("PaymentIntent object is null for event: {}", event.getId());
            return;
        }

        log.info("Payment succeeded for payment intent: {}", paymentIntent.getId());

        Optional<OrderPayment> existingPayment = orderPaymentRepository.findByPaymentIntentId(paymentIntent.getId());
        
        if (existingPayment.isPresent()) {
            // Update existing payment record
            OrderPayment payment = existingPayment.get();
            payment.setStatus(OrderPayment.PaymentStatus.SUCCEEDED);
            payment.setErrorMessage(null); // Clear any previous error
            orderPaymentRepository.save(payment);
            log.info("Updated payment record to SUCCEEDED for payment intent: {}", paymentIntent.getId());
        } else {
            log.warn("No existing payment record found for payment intent: {}", paymentIntent.getId());
            // In a minimal implementation, we might not create records from webhooks
            // if they weren't created through our API first
        }
    }

    /**
     * Handle failed payment
     */
    private void handlePaymentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        
        if (paymentIntent == null) {
            log.error("PaymentIntent object is null for event: {}", event.getId());
            return;
        }

        log.info("Payment failed for payment intent: {}", paymentIntent.getId());

        Optional<OrderPayment> existingPayment = orderPaymentRepository.findByPaymentIntentId(paymentIntent.getId());
        
        if (existingPayment.isPresent()) {
            OrderPayment payment = existingPayment.get();
            payment.setStatus(OrderPayment.PaymentStatus.FAILED);
            payment.setErrorMessage(getPaymentErrorMessage(paymentIntent));
            orderPaymentRepository.save(payment);
            log.info("Updated payment record to FAILED for payment intent: {}", paymentIntent.getId());
        } else {
            log.warn("No existing payment record found for failed payment intent: {}", paymentIntent.getId());
        }
    }

    /**
     * Extract error message from failed payment intent
     */
    private String getPaymentErrorMessage(PaymentIntent paymentIntent) {
        if (paymentIntent.getLastPaymentError() != null) {
            return paymentIntent.getLastPaymentError().getMessage();
        }
        return "Payment failed";
    }

    /**
     * Handle payment_intent.processing event
     * Async payment methods (bank transfers, SEPA) take time to complete
     */
    private void handlePaymentProcessing(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        
        if (paymentIntent == null) {
            log.error("PaymentIntent object is null for event: {}", event.getId());
            return;
        }

        log.info("⏳ Payment processing for payment intent: {}", paymentIntent.getId());
        log.info("Payment method: {}", paymentIntent.getPaymentMethodTypes());

        Optional<OrderPayment> existingPayment = orderPaymentRepository.findByPaymentIntentId(paymentIntent.getId());
        
        if (existingPayment.isPresent()) {
            OrderPayment payment = existingPayment.get();
            // Keep status as PENDING while processing
            if (payment.getStatus() != OrderPayment.PaymentStatus.SUCCEEDED) {
                payment.setStatus(OrderPayment.PaymentStatus.PENDING);
                payment.setErrorMessage("Payment is being processed");
                orderPaymentRepository.save(payment);
                log.info("Payment is processing - awaiting confirmation");
            }
        } else {
            log.warn("No existing payment record found for processing payment intent: {}", paymentIntent.getId());
        }
        
        // TODO: Notify customer that payment is being processed
        // - Send email: "Your payment is being processed"
        // - For digital goods, might want to fulfill order now
    }

    /**
     * Handle charge.refunded event
     * Important: Customer got their money back
     */
    private void handleChargeRefunded(Event event) {
        com.stripe.model.Charge charge = (com.stripe.model.Charge) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        
        if (charge == null) {
            log.error("Charge object is null for event: {}", event.getId());
            return;
        }

        log.warn("💰 REFUND processed for charge: {}", charge.getId());
        log.warn("Payment Intent: {}, Amount refunded: {} {}", 
                charge.getPaymentIntent(),
                charge.getAmountRefunded(),
                charge.getCurrency());

        // Find payment by payment intent ID
        if (charge.getPaymentIntent() != null) {
            Optional<OrderPayment> existingPayment = orderPaymentRepository
                    .findByPaymentIntentId(charge.getPaymentIntent());
            
            if (existingPayment.isPresent()) {
                OrderPayment payment = existingPayment.get();
                payment.setStatus(OrderPayment.PaymentStatus.FAILED);
                payment.setErrorMessage("Payment was refunded");
                orderPaymentRepository.save(payment);
                log.info("Updated payment record to FAILED due to refund");
            }
        }
        
        // TODO: Handle refund business logic
        // - Update order status to refunded
        // - Notify producer if connected account payment
        // - Send refund confirmation email to customer
        // - Update inventory if needed
    }
}