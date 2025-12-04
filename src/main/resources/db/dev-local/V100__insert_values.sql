-- ============================================
-- Seed Data (based on your order service data)
-- ============================================

-- Payment for Order 2 (ORD-1002) - PAID status
INSERT INTO order_payments (
    id,
    order_id,
    payment_intent_id,
    amount,
    currency,
    status,
    paid_by,
    paid_to,
    payment_date,
    payment_due_date,
    stripe_account_id,
    application_fee_amount,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    '2',                          -- order_id matching your order service
    'pi_test_002',                -- test payment intent ID
    20000,                        -- 200.00 in cents
    'EUR',
    'succeeded',                  -- paid status
    'customer_7',                 -- customer_id from order
    'producer_6',                 -- producer_id from order
    '2025-11-29 17:36:46.527851', -- payment_date (matching order created_at)
    '2025-11-29',                 -- payment_due_date
    'acct_test_producer6',        -- Stripe Connect account (optional)
    1000,                         -- 10.00 platform fee in cents (optional)
    '2025-11-29 17:36:46.527851',
    '2025-11-29 17:36:46.527851'
);

-- Optional: Payment for Order 1 (ORD-1001) - PENDING status (no payment yet)
INSERT INTO order_payments (
    id,
    order_id,
    payment_intent_id,
    amount,
    currency,
    status,
    paid_by,
    paid_to,
    payment_date,
    payment_due_date,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    '1',
    'pi_test_001',
    15000,                        -- 150.00 in cents
    'EUR',
    'pending',
    'customer_7',
    'producer_6',
    '2025-11-29 17:36:46.527851',
    '2025-11-29',
    '2025-11-29 17:36:46.527851',
    '2025-11-29 17:36:46.527851'
);
