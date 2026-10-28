-- ==============================================================================
-- Migration V5: Sprint 5 - Payment Gateway, Green Wallet & Driver Earnings
-- ==============================================================================

-- 1. Mở rộng ledger_accounts: cho phép âm + version optimistic lock
ALTER TABLE ledger_accounts
    ADD COLUMN IF NOT EXISTS allow_negative BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_balance_non_negative'
    ) THEN
        ALTER TABLE ledger_accounts
            ADD CONSTRAINT chk_balance_non_negative CHECK (allow_negative OR balance >= 0);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_owner_type_ccy
    ON ledger_accounts(owner_id, account_type, currency) WHERE owner_id IS NOT NULL;

-- 2. Mở rộng ledger_transactions: idempotency + tham chiếu nghiệp vụ
ALTER TABLE ledger_transactions
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS reference_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS reference_id UUID,
    ADD COLUMN IF NOT EXISTS reversal_of UUID REFERENCES ledger_transactions(id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_tx_idem
    ON ledger_transactions(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ledger_tx_ref
    ON ledger_transactions(reference_type, reference_id);

-- 3. Mở rộng ledger_entries: snapshot balance_after + kiểm tra entry_type
ALTER TABLE ledger_entries
    ADD COLUMN IF NOT EXISTS balance_after NUMERIC(18, 4);

CREATE INDEX IF NOT EXISTS idx_ledger_entries_acc_time
    ON ledger_entries(account_id, created_at DESC, id DESC);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_entry_type'
    ) THEN
        ALTER TABLE ledger_entries
            ADD CONSTRAINT chk_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT'));
    END IF;
END $$;

-- 4. Bất biến sổ cái: Chặn UPDATE và DELETE ở mức DB (Defense-in-depth)
CREATE OR REPLACE FUNCTION trg_ledger_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries/ledger_transactions are immutable. Use reversal transactions instead.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS ledger_entries_immutable ON ledger_entries;
CREATE TRIGGER ledger_entries_immutable BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION trg_ledger_immutable();

DROP TRIGGER IF EXISTS ledger_tx_immutable ON ledger_transactions;
CREATE TRIGGER ledger_tx_immutable BEFORE UPDATE OR DELETE ON ledger_transactions
    FOR EACH ROW EXECUTE FUNCTION trg_ledger_immutable();

-- 5. Kiểm tra cân bằng bút toán ở mức DB (Constraint Trigger hoãn tới commit)
CREATE OR REPLACE FUNCTION trg_check_tx_balanced() RETURNS trigger AS $$
DECLARE
    d NUMERIC;
    c NUMERIC;
BEGIN
    SELECT COALESCE(SUM(amount) FILTER (WHERE entry_type = 'DEBIT'), 0),
           COALESCE(SUM(amount) FILTER (WHERE entry_type = 'CREDIT'), 0)
    INTO d, c
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id;

    IF d <> c OR d = 0 THEN
        RAISE EXCEPTION 'Unbalanced ledger tx %: debit=%, credit=%', NEW.transaction_id, d, c;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS ledger_entries_balanced ON ledger_entries;
CREATE CONSTRAINT TRIGGER ledger_entries_balanced
    AFTER INSERT ON ledger_entries
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION trg_check_tx_balanced();

-- 6. Bảng payments (Mỗi lần cố gắng thanh toán cuốc xe)
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id UUID NOT NULL REFERENCES trips(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    driver_user_id UUID REFERENCES users(id),
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_vnd BIGINT NOT NULL CHECK (amount_vnd >= 0),
    commission_rate NUMERIC(5, 4),
    commission_vnd BIGINT,
    driver_amount_vnd BIGINT,
    refunded_vnd BIGINT NOT NULL DEFAULT 0,
    gateway VARCHAR(10),
    gateway_txn_ref VARCHAR(64),
    gateway_transaction_no VARCHAR(64),
    failure_code VARCHAR(40),
    failure_message VARCHAR(255),
    expires_at TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ledger_transaction_id UUID REFERENCES ledger_transactions(id),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_payment_refunded CHECK (refunded_vnd <= amount_vnd)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_trip_active
    ON payments(trip_id) WHERE status IN ('INITIATED', 'PENDING', 'PAID', 'PARTIALLY_REFUNDED');

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_gateway_ref
    ON payments(gateway, gateway_txn_ref) WHERE gateway_txn_ref IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_customer ON payments(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

-- 7. Bảng payment_webhook_events (Nhật ký IPN thô - audit & chống replay)
CREATE TABLE IF NOT EXISTS payment_webhook_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    gateway VARCHAR(20) NOT NULL,
    gateway_txn_ref VARCHAR(64),
    raw_payload JSONB NOT NULL,
    signature_valid BOOLEAN NOT NULL DEFAULT FALSE,
    processing_result VARCHAR(30) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_ip VARCHAR(45)
);

CREATE INDEX IF NOT EXISTS idx_webhook_gateway_ref ON payment_webhook_events(gateway, gateway_txn_ref);

-- 8. Bảng topup_requests
CREATE TABLE IF NOT EXISTS topup_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    amount_vnd BIGINT NOT NULL CHECK (amount_vnd > 0),
    gateway VARCHAR(20) NOT NULL,
    gateway_txn_ref VARCHAR(64) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE,
    ledger_transaction_id UUID REFERENCES ledger_transactions(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_topups_user ON topup_requests(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_topups_status ON topup_requests(status);

-- 9. Bảng withdrawal_requests
CREATE TABLE IF NOT EXISTS withdrawal_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_user_id UUID NOT NULL REFERENCES users(id),
    amount_vnd BIGINT NOT NULL CHECK (amount_vnd > 0),
    bank_code VARCHAR(30) NOT NULL,
    bank_account_no_encrypted TEXT NOT NULL,
    bank_account_last4 VARCHAR(4) NOT NULL,
    account_holder_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reject_reason TEXT,
    hold_ledger_tx_id UUID REFERENCES ledger_transactions(id),
    final_ledger_tx_id UUID REFERENCES ledger_transactions(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_withdrawals_driver ON withdrawal_requests(driver_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_withdrawals_status ON withdrawal_requests(status);

-- 10. Bảng idempotency_keys
CREATE TABLE IF NOT EXISTS idempotency_keys (
    key VARCHAR(120) NOT NULL,
    user_id UUID NOT NULL,
    endpoint VARCHAR(80) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_status INT NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, endpoint, key)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_created ON idempotency_keys(created_at);

-- 11. Bảng outbox_events (Transactional Outbox)
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next ON outbox_events(status, next_attempt_at)
    WHERE status IN ('NEW', 'FAILED');

-- 12. Bảng platform_settings
CREATE TABLE IF NOT EXISTS platform_settings (
    key VARCHAR(80) PRIMARY KEY,
    value VARCHAR(255) NOT NULL,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13. Bảng shedlock (Distributed Scheduler Lock)
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- 14. Bảng audit_logs (Quản trị thao tác tài chính)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id UUID,
    actor_email VARCHAR(120),
    action VARCHAR(60) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(100),
    before_state JSONB,
    after_state JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);

-- 15. Bổ sung index cho trips phục vụ guard đặt xe
CREATE INDEX IF NOT EXISTS idx_trips_payment_status
    ON trips(customer_id, payment_status) WHERE payment_status <> 'PAID';

-- 16. Seed các tài khoản hệ thống (SYS:*)
INSERT INTO ledger_accounts (account_number, owner_id, account_type, currency, balance, status, allow_negative)
VALUES
    ('SYS:REVENUE:VND', NULL, 'PLATFORM_REVENUE', 'VND', 0.0000, 'ACTIVE', TRUE),
    ('SYS:GATEWAY_CLEARING:VND', NULL, 'GATEWAY_CLEARING', 'VND', 0.0000, 'ACTIVE', TRUE),
    ('SYS:PAYOUT_CLEARING:VND', NULL, 'PAYOUT_CLEARING', 'VND', 0.0000, 'ACTIVE', TRUE),
    ('SYS:EXTERNAL_BANK:VND', NULL, 'EXTERNAL_BANK', 'VND', 0.0000, 'ACTIVE', TRUE)
ON CONFLICT (account_number) DO NOTHING;

-- 17. Seed platform_settings mặc định (§2.4)
INSERT INTO platform_settings (key, value)
VALUES
    ('commission_rate', '0.20'),
    ('topup_min_vnd', '10000'),
    ('topup_max_vnd', '10000000'),
    ('withdraw_min_vnd', '50000'),
    ('withdraw_max_vnd', '20000000'),
    ('withdraw_daily_limit_vnd', '20000000'),
    ('cash_debt_limit_vnd', '200000'),
    ('payment_expire_minutes', '15'),
    ('cash_auto_confirm_minutes', '30')
ON CONFLICT (key) DO NOTHING;

-- 18. Backfill ví USER_GREEN_WALLET cho mọi user hiện có
INSERT INTO ledger_accounts (account_number, owner_id, account_type, currency, balance, status, allow_negative)
SELECT
    'WALLET:VND:' || id,
    id,
    'USER_GREEN_WALLET',
    'VND',
    0.0000,
    'ACTIVE',
    FALSE
FROM users
ON CONFLICT (account_number) DO NOTHING;

