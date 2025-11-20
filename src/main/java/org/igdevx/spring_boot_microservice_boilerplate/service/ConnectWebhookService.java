package org.igdevx.spring_boot_microservice_boilerplate.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Payout;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for processing Stripe Connect webhook events
 * Handles events related to connected accounts (producers)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectWebhookService {

    @Value("${stripe.webhook.connect-secret:}")
    private String connectWebhookSecret;

    /**
     * Process Stripe Connect webhook events
     */
    @Transactional
    public void processConnectWebhookEvent(String payload, String sigHeader) {
        try {
            Event event;
            
            // Verify webhook signature - required for security
            if (connectWebhookSecret == null || connectWebhookSecret.isEmpty()) {
                log.warn("Connect webhook secret not configured - processing without signature verification");
                log.warn("⚠️ THIS IS NOT SECURE - Configure stripe.webhook.connect-secret in production!");
                // For development with Stripe CLI, parse without verification
                event = Event.GSON.fromJson(payload, Event.class);
            } else {
                event = Webhook.constructEvent(payload, sigHeader, connectWebhookSecret);
                log.info("Connect webhook signature verified successfully");
            }

            // Extract connected account ID from event
            String connectedAccountId = event.getAccount();
            log.info("Processing Connect webhook event: {} for account: {}", event.getType(), connectedAccountId);

            // Handle different event types
            switch (event.getType()) {
                case "account.updated":
                    handleAccountUpdated(event, connectedAccountId);
                    break;
                case "payout.failed":
                    handlePayoutFailed(event, connectedAccountId);
                    break;
                case "payout.paid":
                    handlePayoutPaid(event, connectedAccountId);
                    break;
                case "account.application.deauthorized":
                    handleAccountDeauthorized(event, connectedAccountId);
                    break;
                case "account.external_account.updated":
                    handleExternalAccountUpdated(event, connectedAccountId);
                    break;
                case "payment_intent.succeeded":
                    handleConnectPaymentSucceeded(event, connectedAccountId);
                    break;
                default:
                    log.info("Unhandled Connect event type: {}", event.getType());
            }

        } catch (SignatureVerificationException e) {
            log.error("Invalid signature for Connect webhook: {}", e.getMessage());
            throw new RuntimeException("Invalid Connect webhook signature", e);
        } catch (Exception e) {
            log.error("Error processing Connect webhook: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process Connect webhook", e);
        }
    }

    /**
     * Handle account.updated event
     * Monitors changes to connected account status and requirements
     */
    private void handleAccountUpdated(Event event, String accountId) {
        Account account = (Account) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        
        if (account == null) {
            log.error("Account object is null for event: {}", event.getId());
            return;
        }

        log.info("Account updated for connected account: {}", accountId);
        
        // Check if charges are enabled
        boolean chargesEnabled = account.getChargesEnabled();
        boolean payoutsEnabled = account.getPayoutsEnabled();
        
        log.info("Connected account {} - Charges enabled: {}, Payouts enabled: {}", 
                accountId, chargesEnabled, payoutsEnabled);
        
        // Check requirements
        if (account.getRequirements() != null) {
            var requirements = account.getRequirements();
            if (requirements.getCurrentlyDue() != null && !requirements.getCurrentlyDue().isEmpty()) {
                log.warn("Connected account {} has outstanding requirements: {}", 
                        accountId, requirements.getCurrentlyDue());
                // TODO: Notify producer via notification service or email
            }
            
            if (requirements.getDisabledReason() != null) {
                log.error("Connected account {} is disabled: {}", 
                        accountId, requirements.getDisabledReason());
                // TODO: Alert producer - urgent action needed
            }
        }
        
        // TODO: Update account status in your database via Account Service
        // Could publish event to Kafka or call Account Service directly
    }

    /**
     * Handle payout.failed event
     * Critical: Producer needs to know their payout failed
     */
    private void handlePayoutFailed(Event event, String accountId) {
        Payout payout = (Payout) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        
        if (payout == null) {
            log.error("Payout object is null for event: {}", event.getId());
            return;
        }

        log.error("❌ PAYOUT FAILED for connected account: {}", accountId);
        log.error("Payout ID: {}, Amount: {} {}, Failure reason: {}", 
                payout.getId(), 
                payout.getAmount(), 
                payout.getCurrency(),
                payout.getFailureMessage());
        
        // TODO: CRITICAL - Notify producer immediately
        // - Send email notification
        // - Create in-app notification
        // - Update order status if needed
        // - Log in database for tracking
        
        log.warn("⚠️ Producer needs to update their bank account details");
    }

    /**
     * Handle payout.paid event
     * Good news: Producer successfully received funds
     */
    private void handlePayoutPaid(Event event, String accountId) {
        Payout payout = (Payout) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        
        if (payout == null) {
            log.error("Payout object is null for event: {}", event.getId());
            return;
        }

        log.info("✅ Payout succeeded for connected account: {}", accountId);
        log.info("Payout ID: {}, Amount: {} {}", 
                payout.getId(), 
                payout.getAmount(), 
                payout.getCurrency());
        
        // TODO: Optional - Notify producer of successful payout
        // - Send confirmation email
        // - Update payout history in database
    }

    /**
     * Handle account.application.deauthorized event
     * Producer has disconnected their Stripe account from your platform
     */
    private void handleAccountDeauthorized(Event event, String accountId) {
        log.warn("⚠️ Connected account {} deauthorized the application", accountId);
        
        // TODO: Important - Clean up producer data
        // - Mark producer as inactive in database
        // - Remove Stripe account ID
        // - Notify admins
        // - Update order processing rules
        
        log.info("Producer has disconnected their Stripe account - cleanup needed");
    }

    /**
     * Handle account.external_account.updated event
     * Producer updated their bank account or card
     */
    private void handleExternalAccountUpdated(Event event, String accountId) {
        log.info("External account updated for connected account: {}", accountId);
        
        // TODO: Optional - Log bank account changes
        // - Audit trail
        // - Notify admins if suspicious activity
    }

    /**
     * Handle payment_intent.succeeded on connected account
     * This is for direct charges on the connected account (if you use them)
     */
    private void handleConnectPaymentSucceeded(Event event, String accountId) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        
        if (paymentIntent == null) {
            log.error("PaymentIntent object is null for event: {}", event.getId());
            return;
        }

        log.info("Payment succeeded on connected account: {}", accountId);
        log.info("Payment Intent ID: {}, Amount: {} {}", 
                paymentIntent.getId(),
                paymentIntent.getAmount(),
                paymentIntent.getCurrency());
        
        // TODO: If using direct charges, update payment records
        // For destination charges (what you're using), standard webhooks handle this
    }
}
