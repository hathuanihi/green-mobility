-- V2__seed_emission_factors.sql: Default Emission Factors from Vietnam MoNRE & IPCC

INSERT INTO emission_factors (
    id, vehicle_category, baseline_gasoline_factor_gco2_km, ev_energy_consumption_kwh_km,
    grid_emission_factor_gco2_kwh, calculated_ev_factor_gco2_km, net_co2_saving_per_km,
    region, effective_from, is_active
) VALUES 
-- 1. Xe máy điện (E-Bike)
(
    'a1b2c3d4-e5f6-7890-abcd-ef1234567801',
    'MOTORBIKE',
    68.50,
    0.0320,
    722.10,
    23.11,
    45.39,
    'VIETNAM_NATIONAL',
    '2026-09-01',
    TRUE
),
-- 2. Ô tô điện 4 chỗ (Compact E-Car)
(
    'a1b2c3d4-e5f6-7890-abcd-ef1234567802',
    'CAR_4SEATS',
    152.00,
    0.1350,
    722.10,
    97.48,
    54.52,
    'VIETNAM_NATIONAL',
    '2026-09-01',
    TRUE
),
-- 3. Ô tô điện 7 chỗ (SUV E-Car)
(
    'a1b2c3d4-e5f6-7890-abcd-ef1234567803',
    'CAR_7SEATS',
    210.00,
    0.1800,
    722.10,
    129.98,
    80.02,
    'VIETNAM_NATIONAL',
    '2026-09-01',
    TRUE
)
ON CONFLICT (id) DO NOTHING;

-- Khởi tạo các tài khoản hệ thống cho Sổ cái kép (Double-Entry Ledger)
INSERT INTO ledger_accounts (
    id, account_number, owner_id, account_type, currency, balance, status
) VALUES
('b1b2c3d4-e5f6-7890-abcd-ef1234567811', 'SYS_CARBON_RESERVE', NULL, 'ASSET', 'CARBON_CREDIT', 1000000.0000, 'ACTIVE'),
('b1b2c3d4-e5f6-7890-abcd-ef1234567812', 'SYS_LOYALTY_EXPENSE', NULL, 'EXPENSE', 'LOYALTY_POINT', 0.0000, 'ACTIVE'),
('b1b2c3d4-e5f6-7890-abcd-ef1234567813', 'PARTNER_REDEMPTION_POOL', NULL, 'LIABILITY', 'LOYALTY_POINT', 0.0000, 'ACTIVE'),
('b1b2c3d4-e5f6-7890-abcd-ef1234567814', 'SYS_BURN_ACCOUNT', NULL, 'EQUITY', 'CARBON_CREDIT', 0.0000, 'ACTIVE')
ON CONFLICT (account_number) DO NOTHING;
