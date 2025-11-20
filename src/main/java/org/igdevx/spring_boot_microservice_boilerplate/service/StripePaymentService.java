package org.igdevx.spring_boot_microservice_boilerplate.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.spring_boot_microservice_boilerplate.dto.AccountStripeInfoResponse;
import org.igdevx.spring_boot_microservice_boilerplate.dto.PaymentIntentRequest;
import org.igdevx.spring_boot_microservice_boilerplate.dto.PaymentIntentResponse;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripePaymentService {

    private final AccountServiceClient accountServiceClient;

    /**
     * Create a payment intent with Stripe (supports connected accounts)
     * @param request Payment details including producer info for connected accounts
     * @return PaymentIntentResponse with client secret
     * @throws StripeException if payment intent creation fails
     */
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) throws StripeException {
        log.info("Creating payment intent for amount: {} {}", request.getAmount(), request.getCurrency());

        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                );

        // Add metadata for tracking
        if (request.getOrderId() != null) {
            paramsBuilder.putMetadata("order_id", request.getOrderId());
        }

        // Handle connected account payments
        if (request.getProducerKeycloakId() != null) {
            log.info("Creating connected account payment for producer: {}", request.getProducerKeycloakId());
            
            // Get producer's Stripe account info
            AccountStripeInfoResponse producerStripeInfo = accountServiceClient.getProducerStripeInfo(request.getProducerKeycloakId());
            
            // Set up transfer to connected account
            paramsBuilder.setTransferData(
                PaymentIntentCreateParams.TransferData.builder()
                    .setDestination(producerStripeInfo.getStripeAccountId())
                    .build()
            );
            
            // Set application fee if specified
            if (request.getApplicationFeeAmount() != null && request.getApplicationFeeAmount() > 0) {
                paramsBuilder.setApplicationFeeAmount(request.getApplicationFeeAmount());
            }
            
            // Add producer info to metadata
            paramsBuilder.putMetadata("producer_keycloak_id", request.getProducerKeycloakId());
            paramsBuilder.putMetadata("stripe_account_id", producerStripeInfo.getStripeAccountId());
            
            log.info("Payment will be transferred to connected account: {}", producerStripeInfo.getStripeAccountId());
        }

        PaymentIntentCreateParams params = paramsBuilder.build();

        // Create payment intent
        PaymentIntent paymentIntent = PaymentIntent.create(params);
        
        log.info("Payment intent created successfully: {}", paymentIntent.getId());

        return new PaymentIntentResponse(
                paymentIntent.getClientSecret(),
                paymentIntent.getId(),
                paymentIntent.getStatus()
        );
    }

    /**
     * Retrieve a payment intent by ID
     * @param paymentIntentId The ID of the payment intent
     * @return PaymentIntent object
     * @throws StripeException if retrieval fails
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        log.info("Retrieving payment intent: {}", paymentIntentId);
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Cancel a payment intent
     * @param paymentIntentId The ID of the payment intent to cancel
     * @return Cancelled PaymentIntent
     * @throws StripeException if cancellation fails
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        log.info("Cancelling payment intent: {}", paymentIntentId);
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return paymentIntent.cancel();
    }
}
