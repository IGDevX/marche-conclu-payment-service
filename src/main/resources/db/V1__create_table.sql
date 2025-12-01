CREATE TABLE order_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id VARCHAR(255) NOT NULL,
    payment_intent_id VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(255) NOT NULL,
    paid_by VARCHAR(255) NOT NULL,
    paid_to VARCHAR(255) NOT NULL,
    payment_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    payment_due_date DATE NOT NULL,
    error_message TEXT,
    stripe_account_id VARCHAR(255),
    application_fee_amount BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for better query performance
    CONSTRAINT uk_order_id UNIQUE (order_id),
    CONSTRAINT uk_payment_intent_id UNIQUE (payment_intent_id)
);

-- Create indexes
CREATE INDEX idx_order_payments_order_id ON order_payments(order_id);
CREATE INDEX idx_order_payments_payment_intent_id ON order_payments(payment_intent_id);
CREATE INDEX idx_order_payments_status ON order_payments(status);
CREATE INDEX idx_order_payments_paid_by ON order_payments(paid_by);
CREATE INDEX idx_order_payments_paid_to ON order_payments(paid_to);
