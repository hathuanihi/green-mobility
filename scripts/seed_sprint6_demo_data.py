#!/usr/bin/env python3
"""
Script seed dữ liệu demo chuẩn Sprint 6 (Idempotent, Double-Entry Balanced: Δ = 0.0000).
Tạo dữ liệu cho Customer, Driver, Finance, Operator:
- Tài khoản Finance & Operator demo (cùng mật khẩu Admin@123456)
- Chuyến xe mẫu + Hóa đơn Carbon 2024 CV 11232/BNNMT-BĐKH (618.70 gCO2/kWh)
- Award CREDITED (đã giải ngân PCC 70/30 + EcoPoints) & Award HELD_FOR_FRAUD (đợi Admin duyệt)
- Voucher Eco-Store AVAILABLE (GM-DEMO15K) & Chứng nhận Trồng cây (GM-TREE01)
- Giao dịch Quy đổi PCC -> VND (Cân bằng sổ cái kép Δ = 0.0000 cho VND, PCC, ECO_POINT)

Cách chạy:
    python scripts/seed_sprint6_demo_data.py
"""

import os
import shutil
import subprocess
import sys

# BCRYPT hash của 'Admin@123456' (khớp V3__seed_admin_account.sql)
DEMO_PASSWORD_HASH = "$2a$12$JCA1zawBxCOUWZw7NzaX5eVR3waCxB3TjBg7.gweyHP/dPDdo5/Ga"

SEED_CUSTOMER_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
SEED_DRIVER_USER_ID = "9c12b7a8-1234-5678-9abc-def012345678"
SEED_DRIVER_PROFILE_ID = "88888888-1234-5678-9abc-def012345678"

FINANCE_USER_ID = "11111111-6666-4444-8888-000000000002"
OPERATOR_USER_ID = "11111111-6666-4444-8888-000000000003"

DEMO_TRIP_CREDITED_ID = "66666666-1111-4444-8888-000000000001"
DEMO_TRIP_HELD_ID = "66666666-1111-4444-8888-000000000002"
DEMO_RECEIPT_CREDITED_ID = "66666666-2222-4444-8888-000000000001"
DEMO_RECEIPT_HELD_ID = "66666666-2222-4444-8888-000000000002"

VOUCHER_15K_REWARD_ID = "f1000003-1111-4444-8888-000000000003"
TREE_REWARD_ID = "f1000004-1111-4444-8888-000000000004"

SQL_SEED = f"""
BEGIN;

-- 1. Tạo tài khoản ROLE_FINANCE và ROLE_OPERATOR demo (nếu chưa có)
INSERT INTO users (id, phone_number, email, password_hash, full_name, role, status)
VALUES
(
    '{FINANCE_USER_ID}',
    '0900000002',
    'finance@greenmobility.vn',
    '{DEMO_PASSWORD_HASH}',
    'Kế toán Trưởng Green Mobility',
    'ROLE_FINANCE',
    'ACTIVE'
),
(
    '{OPERATOR_USER_ID}',
    '0900000003',
    'operator@greenmobility.vn',
    '{DEMO_PASSWORD_HASH}',
    'Điều phối Viên Green Mobility',
    'ROLE_OPERATOR',
    'ACTIVE'
)
ON CONFLICT (phone_number) DO NOTHING;

-- 2. Đảm bảo ví VND/PCC/ECO_POINT tồn tại cho Customer & Driver demo
INSERT INTO ledger_accounts (account_number, owner_id, account_type, currency, balance, status, allow_negative, version)
VALUES
    ('WALLET:VND:{SEED_CUSTOMER_ID}', '{SEED_CUSTOMER_ID}', 'USER_GREEN_WALLET', 'VND', 0.0000, 'ACTIVE', FALSE, 0),
    ('WALLET:PCC:{SEED_CUSTOMER_ID}', '{SEED_CUSTOMER_ID}', 'USER_CARBON_WALLET', 'PCC', 0.0000, 'ACTIVE', FALSE, 0),
    ('WALLET:PTS:{SEED_CUSTOMER_ID}', '{SEED_CUSTOMER_ID}', 'USER_POINTS_WALLET', 'ECO_POINT', 0.0000, 'ACTIVE', FALSE, 0),
    ('EARN:VND:{SEED_DRIVER_USER_ID}', '{SEED_DRIVER_USER_ID}', 'DRIVER_EARNINGS', 'VND', 0.0000, 'ACTIVE', TRUE, 0),
    ('WALLET:PCC:{SEED_DRIVER_USER_ID}', '{SEED_DRIVER_USER_ID}', 'USER_CARBON_WALLET', 'PCC', 0.0000, 'ACTIVE', FALSE, 0),
    ('WALLET:PTS:{SEED_DRIVER_USER_ID}', '{SEED_DRIVER_USER_ID}', 'USER_POINTS_WALLET', 'ECO_POINT', 0.0000, 'ACTIVE', FALSE, 0)
ON CONFLICT (account_number) DO NOTHING;

-- 3. Tạo 2 chuyến đi mẫu Sprint 6 & Hóa đơn Carbon chuẩn 2024 CV 11232/BNNMT-BĐKH (618.70 g/kWh)
INSERT INTO trips (
    id, trip_code, customer_id, driver_id, vehicle_type,
    pickup_geom, pickup_address, dropoff_geom, dropoff_address,
    status, estimated_distance_m, estimated_duration_s,
    actual_distance_m, actual_duration_s,
    fare_amount, discount_amount, final_amount,
    payment_method, payment_status, co2_saved_grams, carbon_credits_earned, loyalty_points_earned,
    requested_at, completed_at
) VALUES
(
    '{DEMO_TRIP_CREDITED_ID}', 'GM-S6-DEMO01', '{SEED_CUSTOMER_ID}', '{SEED_DRIVER_PROFILE_ID}', 'ELECTRIC_CAR_4SEAT',
    ST_SetSRID(ST_MakePoint(106.6983, 10.7725), 4326), 'Chợ Bến Thành, Quận 1, TP.HCM',
    ST_SetSRID(ST_MakePoint(106.7218, 10.7951), 4326), 'Landmark 81, Bình Thạnh, TP.HCM',
    'COMPLETED', 20000, 1800, 20000, 1850,
    240000.00, 0.00, 240000.00,
    'GREEN_WALLET', 'PAID', 1369.60, 1.3696, 13,
    NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '35 minutes'
),
(
    '{DEMO_TRIP_HELD_ID}', 'GM-S6-DEMO02', '{SEED_CUSTOMER_ID}', '{SEED_DRIVER_PROFILE_ID}', 'ELECTRIC_MOTORBIKE',
    ST_SetSRID(ST_MakePoint(106.6602, 10.7626), 4326), 'Đại học Bách Khoa, Quận 10',
    ST_SetSRID(ST_MakePoint(106.6822, 10.7769), 4326), 'Dinh Độc Lập, Quận 1',
    'COMPLETED', 10000, 900, 10000, 920,
    65000.00, 0.00, 65000.00,
    'CASH', 'PAID', 487.00, 0.4870, 4,
    NOW() - INTERVAL '4 hours', NOW() - INTERVAL '3 hours'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO trip_impact_receipts (
    id, trip_id, co2_saved_grams, baseline_gasoline_co2_grams, ev_emitted_co2_grams,
    tree_absorption_days_equiv, led_bulb_hours_equiv, shareable_slug
) VALUES
(
    '{DEMO_RECEIPT_CREDITED_ID}', '{DEMO_TRIP_CREDITED_ID}',
    1369.60, 3040.00, 1670.40, 22.83, 221.37, 'gm-s6-demo-01'
),
(
    '{DEMO_RECEIPT_HELD_ID}', '{DEMO_TRIP_HELD_ID}',
    487.00, 685.00, 198.00, 8.12, 78.71, 'gm-s6-demo-02'
)
ON CONFLICT (trip_id) DO NOTHING;

INSERT INTO fraud_alerts (id, trip_id, driver_id, customer_id, alert_type, risk_score, details, resolution_status)
VALUES (
    '66666666-3333-4444-8888-000000000002',
    '{DEMO_TRIP_HELD_ID}',
    '{SEED_DRIVER_PROFILE_ID}',
    '{SEED_CUSTOMER_ID}',
    'GPS_VELOCITY_ANOMALY',
    84.50,
    '{{"reason":"Demo cảnh báo vận tốc GPS bất thường để kiểm thử quy trình HELD_FOR_FRAUD"}}'::jsonb,
    'PENDING'
)
ON CONFLICT (id) DO NOTHING;

-- 4. Bút toán Sổ cái Kép Idempotent (VND, PCC, ECO_POINT -> Δ = 0.0000 tuyệt đối)
DO $$
DECLARE
    v_sys_pcc UUID;
    v_sys_pcc_retired UUID;
    v_sys_pcc_partner UUID;
    v_sys_pts UUID;
    v_sys_pts_burn UUID;
    v_sys_pts_partner UUID;
    v_sys_gw_vnd UUID;
    v_sys_buyback_vnd UUID;

    v_cust_pcc UUID;
    v_drv_pcc UUID;
    v_cust_pts UUID;
    v_drv_pts UUID;
    v_cust_vnd UUID;

    v_tx_pcc_award UUID;
    v_tx_pts_award UUID;
    v_tx_pcc_bonus UUID;
    v_tx_pts_bonus UUID;
    v_tx_conv_pcc UUID;
    v_tx_conv_vnd UUID;
    v_tx_red_v_pcc UUID;
    v_tx_red_t_pcc UUID;
    v_tx_red_t_pts UUID;

    v_bal NUMERIC(18, 4);
BEGIN
    SELECT id INTO v_sys_pcc FROM ledger_accounts WHERE account_number = 'SYS:CARBON_RESERVE:PCC';
    SELECT id INTO v_sys_pcc_retired FROM ledger_accounts WHERE account_number = 'SYS:CARBON_RETIRED:PCC';
    SELECT id INTO v_sys_pcc_partner FROM ledger_accounts WHERE account_number = 'SYS:PARTNER_POOL:PCC';
    SELECT id INTO v_sys_pts FROM ledger_accounts WHERE account_number = 'SYS:LOYALTY_RESERVE:ECO_POINT';
    SELECT id INTO v_sys_pts_burn FROM ledger_accounts WHERE account_number = 'SYS:ECO_BURN:ECO_POINT';
    SELECT id INTO v_sys_pts_partner FROM ledger_accounts WHERE account_number = 'SYS:PARTNER_POOL:ECO_POINT';
    SELECT id INTO v_sys_gw_vnd FROM ledger_accounts WHERE account_number = 'SYS:GATEWAY_CLEARING:VND';
    SELECT id INTO v_sys_buyback_vnd FROM ledger_accounts WHERE account_number = 'SYS:CARBON_BUYBACK:VND';

    SELECT id INTO v_cust_pcc FROM ledger_accounts WHERE account_number = 'WALLET:PCC:{SEED_CUSTOMER_ID}';
    SELECT id INTO v_drv_pcc FROM ledger_accounts WHERE account_number = 'WALLET:PCC:{SEED_DRIVER_USER_ID}';
    SELECT id INTO v_cust_pts FROM ledger_accounts WHERE account_number = 'WALLET:PTS:{SEED_CUSTOMER_ID}';
    SELECT id INTO v_drv_pts FROM ledger_accounts WHERE account_number = 'WALLET:PTS:{SEED_DRIVER_USER_ID}';
    SELECT id INTO v_cust_vnd FROM ledger_accounts WHERE account_number = 'WALLET:VND:{SEED_CUSTOMER_ID}';

    -- A. Giải ngân PCC cho chuyến DEMO01: 1.3696 PCC -> Khách 0.9587 (70%), Tài xế 0.4109 (30%)
    IF NOT EXISTS (SELECT 1 FROM ledger_transactions WHERE idempotency_key = 'SEED:S6:INC:DEMO01:PCC') THEN
        v_tx_pcc_award := uuid_generate_v4();
        INSERT INTO ledger_transactions (id, transaction_code, trip_id, transaction_type, description, idempotency_key, reference_type)
        VALUES (v_tx_pcc_award, 'TX-S6-SEED-PCC1', '{DEMO_TRIP_CREDITED_ID}', 'TRIP_CARBON_REWARD',
                'Thưởng Tín chỉ Carbon chuyến mẫu GM-S6-DEMO01', 'SEED:S6:INC:DEMO01:PCC', 'TRIP_AWARD');

        UPDATE ledger_accounts SET balance = balance - 1.3696 WHERE id = v_sys_pcc RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pcc_award, v_sys_pcc, 'DEBIT', 1.3696, v_bal);

        UPDATE ledger_accounts SET balance = balance + 0.9587 WHERE id = v_cust_pcc RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pcc_award, v_cust_pcc, 'CREDIT', 0.9587, v_bal);

        UPDATE ledger_accounts SET balance = balance + 0.4109 WHERE id = v_drv_pcc RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pcc_award, v_drv_pcc, 'CREDIT', 0.4109, v_bal);
    ELSE
        SELECT id INTO v_tx_pcc_award FROM ledger_transactions WHERE idempotency_key = 'SEED:S6:INC:DEMO01:PCC';
    END IF;

    -- B. Giải ngân ECO_POINT cho chuyến DEMO01: Khách 13đ, Tài xế 6đ (Tổng 19đ)
    IF NOT EXISTS (SELECT 1 FROM ledger_transactions WHERE idempotency_key = 'SEED:S6:INC:DEMO01:PTS') THEN
        v_tx_pts_award := uuid_generate_v4();
        INSERT INTO ledger_transactions (id, transaction_code, trip_id, transaction_type, description, idempotency_key, reference_type)
        VALUES (v_tx_pts_award, 'TX-S6-SEED-PTS1', '{DEMO_TRIP_CREDITED_ID}', 'TRIP_POINTS_REWARD',
                'Thưởng Điểm Xanh chuyến mẫu GM-S6-DEMO01', 'SEED:S6:INC:DEMO01:PTS', 'TRIP_AWARD');

        UPDATE ledger_accounts SET balance = balance - 19.0000 WHERE id = v_sys_pts RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pts_award, v_sys_pts, 'DEBIT', 19.0000, v_bal);

        UPDATE ledger_accounts SET balance = balance + 13.0000 WHERE id = v_cust_pts RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pts_award, v_cust_pts, 'CREDIT', 13.0000, v_bal);

        UPDATE ledger_accounts SET balance = balance + 6.0000 WHERE id = v_drv_pts RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pts_award, v_drv_pts, 'CREDIT', 6.0000, v_bal);
    ELSE
        SELECT id INTO v_tx_pts_award FROM ledger_transactions WHERE idempotency_key = 'SEED:S6:INC:DEMO01:PTS';
    END IF;

    -- C. Cấp số dư PCC & ECO_POINT khởi đầu cho tài khoản demo trải nghiệm Eco-Store & Quy đổi PCC
    IF NOT EXISTS (SELECT 1 FROM ledger_transactions WHERE idempotency_key = 'SEED:S6:WELCOME:PCC') THEN
        v_tx_pcc_bonus := uuid_generate_v4();
        INSERT INTO ledger_transactions (id, transaction_code, transaction_type, description, idempotency_key, reference_type)
        VALUES (v_tx_pcc_bonus, 'TX-S6-DEMO-PCC', 'TRIP_CARBON_REWARD',
                'Tín chỉ Carbon tích lũy tháng thử nghiệm Sprint 6', 'SEED:S6:WELCOME:PCC', 'DEMO_SEED');

        UPDATE ledger_accounts SET balance = balance - 15.0000 WHERE id = v_sys_pcc RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pcc_bonus, v_sys_pcc, 'DEBIT', 15.0000, v_bal);

        UPDATE ledger_accounts SET balance = balance + 10.0000 WHERE id = v_cust_pcc RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pcc_bonus, v_cust_pcc, 'CREDIT', 10.0000, v_bal);

        UPDATE ledger_accounts SET balance = balance + 5.0000 WHERE id = v_drv_pcc RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pcc_bonus, v_drv_pcc, 'CREDIT', 5.0000, v_bal);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM ledger_transactions WHERE idempotency_key = 'SEED:S6:WELCOME:PTS') THEN
        v_tx_pts_bonus := uuid_generate_v4();
        INSERT INTO ledger_transactions (id, transaction_code, transaction_type, description, idempotency_key, reference_type)
        VALUES (v_tx_pts_bonus, 'TX-S6-DEMO-PTS', 'TRIP_POINTS_REWARD',
                'Điểm Xanh tích lũy thử nghiệm Eco-Store', 'SEED:S6:WELCOME:PTS', 'DEMO_SEED');

        UPDATE ledger_accounts SET balance = balance - 200.0000 WHERE id = v_sys_pts RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pts_bonus, v_sys_pts, 'DEBIT', 200.0000, v_bal);

        UPDATE ledger_accounts SET balance = balance + 150.0000 WHERE id = v_cust_pts RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pts_bonus, v_cust_pts, 'CREDIT', 150.0000, v_bal);

        UPDATE ledger_accounts SET balance = balance + 50.0000 WHERE id = v_drv_pts RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_pts_bonus, v_drv_pts, 'CREDIT', 50.0000, v_bal);
    END IF;

    -- D. Award Join-Barrier (1 CREDITED, 1 HELD_FOR_FRAUD)
    INSERT INTO trip_incentive_awards (
        id, trip_id, customer_id, driver_user_id, co2_saved_grams,
        total_pcc, customer_pcc, driver_pcc, customer_points, driver_points,
        carbon_ready, payment_ready, status, pcc_ledger_tx_id, points_ledger_tx_id, credited_at
    ) VALUES (
        '66666666-4444-4444-8888-000000000001',
        '{DEMO_TRIP_CREDITED_ID}', '{SEED_CUSTOMER_ID}', '{SEED_DRIVER_USER_ID}', 1369.60,
        1.3696, 0.9587, 0.4109, 13, 6,
        TRUE, TRUE, 'CREDITED', v_tx_pcc_award, v_tx_pts_award, NOW() - INTERVAL '2 days'
    ) ON CONFLICT (trip_id) DO NOTHING;

    INSERT INTO trip_incentive_awards (
        id, trip_id, customer_id, driver_user_id, co2_saved_grams,
        total_pcc, customer_pcc, driver_pcc, customer_points, driver_points,
        carbon_ready, payment_ready, status, hold_reason
    ) VALUES (
        '66666666-4444-4444-8888-000000000002',
        '{DEMO_TRIP_HELD_ID}', '{SEED_CUSTOMER_ID}', '{SEED_DRIVER_USER_ID}', 487.00,
        0.0000, 0.0000, 0.0000, 0, 0,
        TRUE, TRUE, 'HELD_FOR_FRAUD', 'Phát hiện cảnh báo vận tốc GPS bất thường (Risk 84.5) - chờ Admin kiểm tra'
    ) ON CONFLICT (trip_id) DO NOTHING;

    -- E. Giao dịch quy đổi mẫu 1.0000 PCC -> 500đ VND cho Customer
    IF NOT EXISTS (SELECT 1 FROM ledger_transactions WHERE idempotency_key = 'SEED:S6:CONV:PCC') THEN
        v_tx_conv_pcc := uuid_generate_v4();
        INSERT INTO ledger_transactions (id, transaction_code, transaction_type, description, idempotency_key, reference_type)
        VALUES (v_tx_conv_pcc, 'TX-S6-CONV-PCC', 'PCC_CONVERT_BURN',
                'Quy đổi 1.0000 PCC sang 500đ', 'SEED:S6:CONV:PCC', 'CARBON_CONVERSION');
        UPDATE ledger_accounts SET balance = balance - 1.0000 WHERE id = v_cust_pcc RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_conv_pcc, v_cust_pcc, 'DEBIT', 1.0000, v_bal);
        UPDATE ledger_accounts SET balance = balance + 1.0000 WHERE id = v_sys_pcc_retired RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_conv_pcc, v_sys_pcc_retired, 'CREDIT', 1.0000, v_bal);

        v_tx_conv_vnd := uuid_generate_v4();
        INSERT INTO ledger_transactions (id, transaction_code, transaction_type, description, idempotency_key, reference_type)
        VALUES (v_tx_conv_vnd, 'TX-S6-CONV-VND', 'PCC_CONVERT_PAYOUT',
                'Chi trả trợ giá xanh 500đ từ quy đổi 1.0000 PCC', 'SEED:S6:CONV:VND', 'CARBON_CONVERSION');
        UPDATE ledger_accounts SET balance = balance - 500.0000 WHERE id = v_sys_buyback_vnd RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_conv_vnd, v_sys_buyback_vnd, 'DEBIT', 500.0000, v_bal);
        UPDATE ledger_accounts SET balance = balance + 500.0000 WHERE id = v_cust_vnd RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_conv_vnd, v_cust_vnd, 'CREDIT', 500.0000, v_bal);

        INSERT INTO carbon_conversions (
            id, user_id, user_role, target_account_type, pcc_amount, rate_vnd_per_pcc, vnd_amount,
            pcc_ledger_tx_id, vnd_ledger_tx_id, status
        ) VALUES (
            '66666666-5555-4444-8888-000000000001',
            '{SEED_CUSTOMER_ID}', 'ROLE_CUSTOMER', 'USER_GREEN_WALLET', 1.0000, 500, 500,
            v_tx_conv_pcc, v_tx_conv_vnd, 'COMPLETED'
        ) ON CONFLICT (id) DO NOTHING;
    END IF;

    -- F. Eco-Store Redemptions mẫu: 1 Voucher AVAILABLE (GM-DEMO15K) + 1 Trồng cây (GM-TREE01)
    IF NOT EXISTS (SELECT 1 FROM ledger_transactions WHERE idempotency_key = 'SEED:S6:REDEEM:VOUCHER') THEN
        v_tx_red_v_pcc := uuid_generate_v4();
        INSERT INTO ledger_transactions (id, transaction_code, transaction_type, description, idempotency_key, reference_type)
        VALUES (v_tx_red_v_pcc, 'TX-S6-RED-V15K', 'REWARD_REDEEM_PCC',
                'Đổi Voucher Giảm 15.000đ chuyến xe điện', 'SEED:S6:REDEEM:VOUCHER', 'REWARD_REDEMPTION');
        UPDATE ledger_accounts SET balance = balance - 0.5000 WHERE id = v_cust_pcc RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_red_v_pcc, v_cust_pcc, 'DEBIT', 0.5000, v_bal);
        UPDATE ledger_accounts SET balance = balance + 0.5000 WHERE id = v_sys_pcc_partner RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_red_v_pcc, v_sys_pcc_partner, 'CREDIT', 0.5000, v_bal);

        INSERT INTO reward_redemptions (
            id, user_id, reward_id, redemption_code, carbon_spent, points_spent,
            status, pcc_ledger_tx_id, expires_at
        ) VALUES (
            '66666666-7777-4444-8888-000000000001',
            '{SEED_CUSTOMER_ID}', '{VOUCHER_15K_REWARD_ID}', 'GM-DEMO15K', 0.5000, 0,
            'AVAILABLE', v_tx_red_v_pcc, NOW() + INTERVAL '30 days'
        ) ON CONFLICT (redemption_code) DO NOTHING;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM ledger_transactions WHERE idempotency_key = 'SEED:S6:REDEEM:TREE:PCC') THEN
        v_tx_red_t_pcc := uuid_generate_v4();
        INSERT INTO ledger_transactions (id, transaction_code, transaction_type, description, idempotency_key, reference_type)
        VALUES (v_tx_red_t_pcc, 'TX-S6-RED-TR-PCC', 'REWARD_REDEEM_PCC',
                'Đóng góp 1 cây đước Cần Giờ (PCC)', 'SEED:S6:REDEEM:TREE:PCC', 'REWARD_REDEMPTION');
        UPDATE ledger_accounts SET balance = balance - 5.0000 WHERE id = v_cust_pcc RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_red_t_pcc, v_cust_pcc, 'DEBIT', 5.0000, v_bal);
        UPDATE ledger_accounts SET balance = balance + 5.0000 WHERE id = v_sys_pcc_partner RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_red_t_pcc, v_sys_pcc_partner, 'CREDIT', 5.0000, v_bal);

        v_tx_red_t_pts := uuid_generate_v4();
        INSERT INTO ledger_transactions (id, transaction_code, transaction_type, description, idempotency_key, reference_type)
        VALUES (v_tx_red_t_pts, 'TX-S6-RED-TR-PTS', 'REWARD_REDEEM_POINTS',
                'Đóng góp 1 cây đước Cần Giờ (Eco-Points)', 'SEED:S6:REDEEM:TREE:PTS', 'REWARD_REDEMPTION');
        UPDATE ledger_accounts SET balance = balance - 50.0000 WHERE id = v_cust_pts RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_red_t_pts, v_cust_pts, 'DEBIT', 50.0000, v_bal);
        UPDATE ledger_accounts SET balance = balance + 50.0000 WHERE id = v_sys_pts_partner RETURNING balance INTO v_bal;
        INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, amount, balance_after)
        VALUES (uuid_generate_v4(), v_tx_red_t_pts, v_sys_pts_partner, 'CREDIT', 50.0000, v_bal);

        INSERT INTO reward_redemptions (
            id, user_id, reward_id, redemption_code, carbon_spent, points_spent,
            status, pcc_ledger_tx_id, points_ledger_tx_id, expires_at, certificate_metadata
        ) VALUES (
            '66666666-7777-4444-8888-000000000002',
            '{SEED_CUSTOMER_ID}', '{TREE_REWARD_ID}', 'GM-TREE01', 5.0000, 50,
            'COMPLETED', v_tx_red_t_pcc, v_tx_red_t_pts, NOW() + INTERVAL '365 days',
            '{{"certificateCode":"TREE-GM-TREE01","partner":"Quỹ Rừng Xanh Việt Nam","projectTitle":"Đóng Góp 1 Cây Đước Rừng Ngập Mặn Cần Giờ","pccRetired":5.0000}}'::jsonb
        ) ON CONFLICT (redemption_code) DO NOTHING;
    END IF;
END $$;

COMMIT;

-- 5. Kiểm toán tổng thể sau seed (Δ phải bằng 0.0000 cho cả 3 loại tiền tệ)
SELECT currency, SUM(balance) AS net_delta
FROM ledger_accounts
GROUP BY currency
ORDER BY currency;
"""


def run_via_psycopg2():
    import psycopg2

    conn = psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        dbname=os.getenv("DB_NAME", "greenmobility_db"),
        user=os.getenv("DB_USER", "green_admin"),
        password=os.getenv("DB_PASSWORD", "green_password_2026"),
    )
    conn.autocommit = False
    with conn.cursor() as cur:
        cur.execute(SQL_SEED)
        rows = cur.fetchall()
    conn.commit()
    conn.close()
    return rows


def run_via_docker_psql():
    container = os.getenv("POSTGRES_CONTAINER", "greenmobility-postgres")
    db_user = os.getenv("DB_USER", "green_admin")
    db_name = os.getenv("DB_NAME", "greenmobility_db")
    cmd = ["docker", "exec", "-i", container, "psql", "-v", "ON_ERROR_STOP=1", "-U", db_user, "-d", db_name]
    proc = subprocess.run(cmd, input=SQL_SEED, text=True, capture_output=True)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())
    return proc.stdout


def main():
    print("=" * 72)
    print("  GREEN MOBILITY - SEED DEMO SPRINT 6 (DOUBLE-ENTRY Δ = 0.0000)")
    print("=" * 72)

    try:
        try:
            import psycopg2  # noqa: F401
            rows = run_via_psycopg2()
            print("[✓] Seed dữ liệu Sprint 6 qua psycopg2 thành công!")
            for ccy, delta in rows:
                print(f"    - {ccy}: net_delta = {delta}")
                if float(delta) != 0.0:
                    raise AssertionError(f"Mất cân bằng sổ cái {ccy}: {delta}")
        except ImportError:
            if not shutil.which("docker"):
                print("[!] Không tìm thấy psycopg2 hoặc docker CLI. Hãy cài đặt: pip install psycopg2-binary")
                sys.exit(1)
            output = run_via_docker_psql()
            print("[✓] Seed dữ liệu Sprint 6 qua Docker psql thành công!")
            print(output.strip())
        print("\n[✓] Hoàn tất! Dữ liệu sẵn sàng cho Demo Web Admin, Customer App và Driver App.")
    except Exception as exc:
        print(f"[X] Seed thất bại: {exc}")
        sys.exit(1)


if __name__ == "__main__":
    main()
