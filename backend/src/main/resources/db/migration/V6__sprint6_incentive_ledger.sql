-- ==============================================================================
-- Migration V6: Sprint 6 - Multi-Currency Incentive Ledger, Carbon Wallet,
-- Eco-Store, Vouchers, PCC->VND Conversion & 2024 Official Emission Factors
-- ==============================================================================

-- 1. Mở rộng ledger_accounts: hỗ trợ đóng băng ví & ràng buộc trạng thái
ALTER TABLE ledger_accounts
    ADD COLUMN IF NOT EXISTS frozen_reason TEXT,
    ADD COLUMN IF NOT EXISTS frozen_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS frozen_at TIMESTAMP WITH TIME ZONE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_ledger_account_status'
    ) THEN
        ALTER TABLE ledger_accounts
            ADD CONSTRAINT chk_ledger_account_status
            CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'));
    END IF;
END $$;

-- 2. DB Constraint Trigger: Mỗi LedgerTransaction chỉ được phép sử dụng 1 loại currency duy nhất
CREATE OR REPLACE FUNCTION trg_check_tx_single_currency() RETURNS trigger AS $$
DECLARE
    ccy_count INT;
BEGIN
    SELECT COUNT(DISTINCT a.currency)
    INTO ccy_count
    FROM ledger_entries e
    JOIN ledger_accounts a ON a.id = e.account_id
    WHERE e.transaction_id = NEW.transaction_id;

    IF ccy_count > 1 THEN
        RAISE EXCEPTION 'Cross-currency ledger transaction % is forbidden (distinct currencies=%)',
            NEW.transaction_id, ccy_count;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS ledger_entries_single_currency ON ledger_entries;
CREATE CONSTRAINT TRIGGER ledger_entries_single_currency
    AFTER INSERT ON ledger_entries
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION trg_check_tx_single_currency();

-- 3. Chuẩn hóa Bảng emission_factors theo Công văn 11232/BNNMT-BĐKH (30/09/2026) - Lưới điện VN 2024: 618.70 gCO2/kWh
-- Đóng hiệu lực các hệ số 580.00 cũ trước khi thêm CHECK constraint
UPDATE emission_factors
SET is_active = FALSE,
    effective_to = COALESCE(effective_to, DATE '2026-10-25')
WHERE grid_emission_factor_gco2_kwh = 580.00
  AND is_active = TRUE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_emission_ev_factor_formula'
    ) THEN
        ALTER TABLE emission_factors
            ADD CONSTRAINT chk_emission_ev_factor_formula
            CHECK (calculated_ev_factor_gco2_km = ROUND(ev_energy_consumption_kwh_km * grid_emission_factor_gco2_kwh, 2));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_emission_net_saving_formula'
    ) THEN
        ALTER TABLE emission_factors
            ADD CONSTRAINT chk_emission_net_saving_formula
            CHECK (net_co2_saving_per_km = baseline_gasoline_factor_gco2_km - calculated_ev_factor_gco2_km);
    END IF;
END $$;

INSERT INTO emission_factors (
    id, vehicle_category, baseline_gasoline_factor_gco2_km,
    ev_energy_consumption_kwh_km, grid_emission_factor_gco2_kwh,
    calculated_ev_factor_gco2_km, net_co2_saving_per_km,
    region, effective_from, is_active
) VALUES
(
    'e2024001-1111-4444-8888-000000000001',
    'ELECTRIC_MOTORBIKE',
    68.50,
    0.0320,
    618.70,
    19.80,
    48.70,
    'VIETNAM_NATIONAL',
    DATE '2026-10-26',
    TRUE
),
(
    'e2024002-1111-4444-8888-000000000002',
    'ELECTRIC_CAR_4SEAT',
    152.00,
    0.1350,
    618.70,
    83.52,
    68.48,
    'VIETNAM_NATIONAL',
    DATE '2026-10-26',
    TRUE
),
(
    'e2024003-1111-4444-8888-000000000003',
    'ELECTRIC_CAR_7SEAT',
    210.00,
    0.1800,
    618.70,
    111.37,
    98.63,
    'VIETNAM_NATIONAL',
    DATE '2026-10-26',
    TRUE
)
ON CONFLICT (id) DO NOTHING;

-- Chỉ đồng bộ dữ liệu seed mẫu từ V4 (không chạm tới chuyến đi thật) theo quy chuẩn 1 PCC = 1 kg CO2 (1000g)
UPDATE trips
SET carbon_credits_earned = ROUND(co2_saved_grams / 1000.0, 4),
    loyalty_points_earned = GREATEST(1, FLOOR(co2_saved_grams / 100.0)::INT)
WHERE id IN ('d4444444-4444-4444-4444-444444444444', '709f91bf-6b40-4a2b-873e-48e68f9ac482')
  AND co2_saved_grams > 0;

UPDATE trip_impact_receipts r
SET led_bulb_hours_equiv = ROUND(r.co2_saved_grams / 6.187, 2)
WHERE r.trip_id IN ('d4444444-4444-4444-4444-444444444444', '709f91bf-6b40-4a2b-873e-48e68f9ac482')
  AND r.co2_saved_grams > 0;

-- 4. Mở rộng rewards & reward_redemptions (Scale PCC 4 chữ số thập phân + Voucher Metadata)
ALTER TABLE rewards
    ALTER COLUMN carbon_credit_cost TYPE NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS discount_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS discount_value BIGINT,
    ADD COLUMN IF NOT EXISTS max_discount_vnd BIGINT,
    ADD COLUMN IF NOT EXISTS min_fare_vnd BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS validity_days INT NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS max_redemptions_per_user INT NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_reward_qty_non_negative') THEN
        ALTER TABLE rewards ADD CONSTRAINT chk_reward_qty_non_negative CHECK (quantity_available >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_reward_cost_non_negative') THEN
        ALTER TABLE rewards ADD CONSTRAINT chk_reward_cost_non_negative CHECK (carbon_credit_cost >= 0 AND loyalty_point_cost >= 0);
    END IF;
END $$;

ALTER TABLE reward_redemptions
    ALTER COLUMN carbon_spent TYPE NUMERIC(18, 4),
    ADD COLUMN IF NOT EXISTS pcc_ledger_tx_id UUID REFERENCES ledger_transactions(id),
    ADD COLUMN IF NOT EXISTS points_ledger_tx_id UUID REFERENCES ledger_transactions(id),
    ADD COLUMN IF NOT EXISTS reversal_pcc_tx_id UUID REFERENCES ledger_transactions(id),
    ADD COLUMN IF NOT EXISTS reversal_points_tx_id UUID REFERENCES ledger_transactions(id),
    ADD COLUMN IF NOT EXISTS applied_trip_id UUID REFERENCES trips(id),
    ADD COLUMN IF NOT EXISTS discount_applied_vnd BIGINT,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS used_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS cancel_reason TEXT,
    ADD COLUMN IF NOT EXISTS certificate_metadata JSONB,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_redemptions_user_status_exp
    ON reward_redemptions(user_id, status, expires_at);
CREATE INDEX IF NOT EXISTS idx_redemptions_applied_trip
    ON reward_redemptions(applied_trip_id) WHERE applied_trip_id IS NOT NULL;

-- 5. Mở rộng payments & trips để hỗ trợ Voucher Subsidy (SYS:PROMO_EXPENSE:VND)
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS gross_fare_vnd BIGINT,
    ADD COLUMN IF NOT EXISTS discount_vnd BIGINT NOT NULL DEFAULT 0;

UPDATE payments
SET gross_fare_vnd = amount_vnd
WHERE gross_fare_vnd IS NULL;

ALTER TABLE trips
    ADD COLUMN IF NOT EXISTS voucher_redemption_id UUID REFERENCES reward_redemptions(id),
    ADD COLUMN IF NOT EXISTS voucher_code VARCHAR(32);

-- 6. Bảng Join-Barrier điều phối thưởng chuyến đi (trip_incentive_awards)
CREATE TABLE IF NOT EXISTS trip_incentive_awards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id UUID UNIQUE NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES users(id),
    driver_user_id UUID REFERENCES users(id),
    co2_saved_grams NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_pcc NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    customer_pcc NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    driver_pcc NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    customer_points INT NOT NULL DEFAULT 0,
    driver_points INT NOT NULL DEFAULT 0,
    carbon_ready BOOLEAN NOT NULL DEFAULT FALSE,
    payment_ready BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(35) NOT NULL DEFAULT 'PENDING_CONDITIONS',
    hold_reason TEXT,
    pcc_ledger_tx_id UUID REFERENCES ledger_transactions(id),
    points_ledger_tx_id UUID REFERENCES ledger_transactions(id),
    clawback_pcc_tx_id UUID REFERENCES ledger_transactions(id),
    clawback_points_tx_id UUID REFERENCES ledger_transactions(id),
    credited_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    review_note TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_award_status CHECK (status IN (
        'PENDING_CONDITIONS',
        'HELD_FOR_FRAUD',
        'CREDITED',
        'VOIDED_FRAUD',
        'CLAWED_BACK',
        'CLAWBACK_SHORTFALL_AUDIT'
    ))
);

CREATE INDEX IF NOT EXISTS idx_awards_status_created
    ON trip_incentive_awards(status, created_at);
CREATE INDEX IF NOT EXISTS idx_awards_customer
    ON trip_incentive_awards(customer_id, created_at DESC);

-- 7. Bảng lịch sử quy đổi PCC -> VND (carbon_conversions)
CREATE TABLE IF NOT EXISTS carbon_conversions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    user_role VARCHAR(30) NOT NULL,
    target_account_type VARCHAR(30) NOT NULL,
    pcc_amount NUMERIC(18, 4) NOT NULL CHECK (pcc_amount > 0),
    rate_vnd_per_pcc BIGINT NOT NULL CHECK (rate_vnd_per_pcc > 0),
    vnd_amount BIGINT NOT NULL CHECK (vnd_amount > 0),
    pcc_ledger_tx_id UUID NOT NULL REFERENCES ledger_transactions(id),
    vnd_ledger_tx_id UUID NOT NULL REFERENCES ledger_transactions(id),
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_carbon_conversions_user_time
    ON carbon_conversions(user_id, created_at DESC);

-- 8. Kiểm tra ràng buộc cột users.role (nếu có constraint cũ thì cập nhật cho phép ROLE_FINANCE)
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'users'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%role%'
    LOOP
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$;

-- 9. Seed 8 tài khoản hệ thống mới (Double-Entry Multi-Currency)
INSERT INTO ledger_accounts (account_number, owner_id, account_type, currency, balance, status, allow_negative)
VALUES
    ('SYS:PROMO_EXPENSE:VND', NULL, 'PROMO_EXPENSE', 'VND', 0.0000, 'ACTIVE', TRUE),
    ('SYS:CARBON_BUYBACK:VND', NULL, 'CARBON_BUYBACK_POOL', 'VND', 0.0000, 'ACTIVE', TRUE),
    ('SYS:CARBON_RESERVE:PCC', NULL, 'SYSTEM_CARBON_RESERVE', 'PCC', 0.0000, 'ACTIVE', TRUE),
    ('SYS:CARBON_RETIRED:PCC', NULL, 'SYSTEM_CARBON_RETIRED', 'PCC', 0.0000, 'ACTIVE', TRUE),
    ('SYS:PARTNER_POOL:PCC', NULL, 'PARTNER_REDEMPTION_POOL', 'PCC', 0.0000, 'ACTIVE', TRUE),
    ('SYS:LOYALTY_RESERVE:ECO_POINT', NULL, 'SYSTEM_LOYALTY_RESERVE', 'ECO_POINT', 0.0000, 'ACTIVE', TRUE),
    ('SYS:PARTNER_POOL:ECO_POINT', NULL, 'PARTNER_REDEMPTION_POOL', 'ECO_POINT', 0.0000, 'ACTIVE', TRUE),
    ('SYS:ECO_BURN:ECO_POINT', NULL, 'SYSTEM_BURN_ACCOUNT', 'ECO_POINT', 0.0000, 'ACTIVE', TRUE)
ON CONFLICT (account_number) DO NOTHING;

-- Backfill ví PCC và ECO_POINT CHỈ cho người dùng ROLE_CUSTOMER và ROLE_DRIVER
INSERT INTO ledger_accounts (account_number, owner_id, account_type, currency, balance, status, allow_negative)
SELECT
    'WALLET:PCC:' || id,
    id,
    'USER_CARBON_WALLET',
    'PCC',
    0.0000,
    'ACTIVE',
    FALSE
FROM users
WHERE role IN ('ROLE_CUSTOMER', 'ROLE_DRIVER')
ON CONFLICT (account_number) DO NOTHING;

INSERT INTO ledger_accounts (account_number, owner_id, account_type, currency, balance, status, allow_negative)
SELECT
    'WALLET:PTS:' || id,
    id,
    'USER_POINTS_WALLET',
    'ECO_POINT',
    0.0000,
    'ACTIVE',
    FALSE
FROM users
WHERE role IN ('ROLE_CUSTOMER', 'ROLE_DRIVER')
ON CONFLICT (account_number) DO NOTHING;

-- 10. Seed 9 khóa platform_settings cho Hệ thống Khuyến khích
INSERT INTO platform_settings (key, value)
VALUES
    ('pcc_grams_per_unit', '1000'),
    ('pcc_customer_share_ratio', '0.70'),
    ('eco_point_grams_per_point', '100'),
    ('driver_eco_point_ratio', '0.50'),
    ('pcc_to_vnd_rate', '500'),
    ('pcc_convert_min', '0.5000'),
    ('pcc_convert_monthly_cap', '100.0000'),
    ('redemptions_frozen', 'false'),
    ('incentive_reconcile_minutes', '5')
ON CONFLICT (key) DO NOTHING;

-- 11. Seed 7 mẫu quà Eco-Store phủ đủ 3 nhóm (VOUCHER_RIDE, TREE_PLANTING, ECO_PRODUCT)
INSERT INTO rewards (
    id, title, description, partner_name, category,
    carbon_credit_cost, loyalty_point_cost, image_url,
    quantity_available, is_active,
    discount_type, discount_value, max_discount_vnd, min_fare_vnd,
    validity_days, max_redemptions_per_user
) VALUES
(
    'f1000001-1111-4444-8888-000000000001',
    'Voucher Giảm 20.000đ Chuyến Xe Điện',
    'Áp dụng giảm trực tiếp 20.000đ cho mọi chuyến xe máy điện hoặc ô tô điện có cước tối thiểu 30.000đ.',
    'Green Mobility',
    'VOUCHER_RIDE',
    0.0000,
    20,
    '/assets/rewards/voucher-20k.png',
    500,
    TRUE,
    'FIXED_VND',
    20000,
    20000,
    30000,
    30,
    10
),
(
    'f1000002-1111-4444-8888-000000000002',
    'Voucher Giảm 30% Chuyến Đi Xanh (Tối đa 50.000đ)',
    'Giảm 30% giá cước chuyến đi tiếp theo, tối đa 50.000đ cho chuyến từ 50.000đ trở lên.',
    'Green Mobility',
    'VOUCHER_RIDE',
    1.5000,
    30,
    '/assets/rewards/voucher-30pct.png',
    300,
    TRUE,
    'PERCENT',
    30,
    50000,
    50000,
    30,
    5
),
(
    'f1000003-1111-4444-8888-000000000003',
    'Voucher Miễn Phí Chuyến Ngắn 15.000đ',
    'Ưu đãi tri ân khách hàng di chuyển xanh, giảm đến 15.000đ cho mọi cuốc xe.',
    'Green Mobility',
    'VOUCHER_RIDE',
    0.5000,
    0,
    '/assets/rewards/voucher-15k.png',
    1000,
    TRUE,
    'FIXED_VND',
    15000,
    15000,
    0,
    30,
    10
),
(
    'f1000004-1111-4444-8888-000000000004',
    'Đóng Góp 1 Cây Đước Rừng Ngập Mặn Cần Giờ',
    'Quy đổi Tín chỉ Carbon cá nhân để trồng mới 1 cây đước tại Khu Dự trữ Sinh quyển Cần Giờ, cấp chứng nhận điện tử.',
    'Quỹ Rừng Xanh Việt Nam',
    'TREE_PLANTING',
    5.0000,
    50,
    '/assets/rewards/tree-cangio.png',
    200,
    TRUE,
    NULL, NULL, NULL, 0,
    365,
    20
),
(
    'f1000005-1111-4444-8888-000000000005',
    'Đóng Góp Phủ Xanh Đồi Trọc Tây Nguyên (5 Cây Thông)',
    'Tài trợ 5 cây thông non tại Lâm Đồng từ lượng khí thải CO2 bạn đã giúp cắt giảm.',
    'Trung tâm Bảo tồn Thiên nhiên Gaia',
    'TREE_PLANTING',
    15.0000,
    120,
    '/assets/rewards/tree-taynguyen.png',
    100,
    TRUE,
    NULL, NULL, NULL, 0,
    365,
    10
),
(
    'f1000006-1111-4444-8888-000000000006',
    'Ly Cà Phê Hữu Cơ / Trà Xanh Không Rác Nhựa',
    'Đổi 1 thức uống size M tại chuỗi đối tác thân thiện môi trường (mang ly cá nhân được tặng thêm topping).',
    'The Coffee House Green',
    'ECO_PRODUCT',
    0.0000,
    40,
    '/assets/rewards/eco-coffee.png',
    250,
    TRUE,
    NULL, NULL, NULL, 0,
    60,
    5
),
(
    'f1000007-1111-4444-8888-000000000007',
    'Bình Giữ Nhiệt Thép Không Gỉ Green Mobility',
    'Bình giữ nhiệt 500ml phiên bản giới hạn Net Zero 2050, thay thế hàng trăm chai nhựa dùng một lần.',
    'VinFast Eco Store',
    'ECO_PRODUCT',
    8.0000,
    80,
    '/assets/rewards/eco-bottle.png',
    80,
    TRUE,
    NULL, NULL, NULL, 0,
    90,
    2
)
ON CONFLICT (id) DO NOTHING;
