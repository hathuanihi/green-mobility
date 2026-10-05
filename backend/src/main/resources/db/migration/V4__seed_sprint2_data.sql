-- V4__seed_sprint2_data.sql: Seed Initial Data for Sprint 2 (EV Ride Booking & Matching Engine)
-- Bao gom:
-- 1. He so phat thai chuan IPCC (emission_factors)
-- 2. Tai khoan Khach hang & Tai xe demo (users, driver_profiles, vehicles)
-- 3. Du lieu chuyen di mau da trang thai (trips: SEARCHING, MATCHED, IN_TRIP, COMPLETED, CANCELLED)
-- 4. Hoa don giam phat thai CO2 (trip_impact_receipts)

-- ============================================================================
-- 1. HE SO PHAT THAI CHUAN (EMISSION FACTORS - IPCC 2006 / GHG PROTOCOL)
-- ============================================================================
INSERT INTO emission_factors (
    id, vehicle_category, baseline_gasoline_factor_gco2_km, 
    ev_energy_consumption_kwh_km, grid_emission_factor_gco2_kwh, 
    calculated_ev_factor_gco2_km, net_co2_saving_per_km, 
    region, effective_from, is_active
) VALUES 
(
    'b3a1aa69-7152-4824-9d69-045b2f06c3c4',
    'ELECTRIC_MOTORBIKE',
    70.00,
    0.0250,
    580.00,
    14.50,
    55.50,
    'VIETNAM_NATIONAL',
    '2026-01-01',
    TRUE
),
(
    '1b6b1b8b-29d6-443a-932e-d485850b7b03',
    'ELECTRIC_CAR_4SEAT',
    140.00,
    0.1400,
    580.00,
    81.20,
    58.80,
    'VIETNAM_NATIONAL',
    '2026-01-01',
    TRUE
),
(
    '76c549a0-358d-46fa-8373-b16f3574ae36',
    'ELECTRIC_CAR_7SEAT',
    180.00,
    0.1800,
    580.00,
    104.40,
    75.60,
    'VIETNAM_NATIONAL',
    '2026-01-01',
    TRUE
)
ON CONFLICT (id) DO UPDATE 
SET baseline_gasoline_factor_gco2_km = EXCLUDED.baseline_gasoline_factor_gco2_km,
    calculated_ev_factor_gco2_km = EXCLUDED.calculated_ev_factor_gco2_km,
    net_co2_saving_per_km = EXCLUDED.net_co2_saving_per_km,
    is_active = TRUE;

-- ============================================================================
-- 2. TAI KHOAN NGUOI DUNG & KHACH HANG DEMO (USERS)
-- Password: User@123456 ($2a$12$JCA1zawBxCOUWZw7NzaX5eVR3waCxB3TjBg7.gweyHP/dPDdo5/Ga)
-- ============================================================================
INSERT INTO users (id, phone_number, email, password_hash, full_name, role, status)
VALUES 
(
    '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    '0901234567',
    'customer.thu@greenmobility.vn',
    '$2a$12$JCA1zawBxCOUWZw7NzaX5eVR3waCxB3TjBg7.gweyHP/dPDdo5/Ga',
    'Phạm Hà Anh Thư',
    'ROLE_CUSTOMER',
    'ACTIVE'
),
(
    '9c12b7a8-1234-5678-9abc-def012345678',
    '0987654321',
    'driver.thien@greenmobility.vn',
    '$2a$12$JCA1zawBxCOUWZw7NzaX5eVR3waCxB3TjBg7.gweyHP/dPDdo5/Ga',
    'Nguyễn Minh Thiện',
    'ROLE_DRIVER',
    'ACTIVE'
),
(
    'a2222222-2222-2222-2222-222222222222',
    '0912345678',
    'driver.mai@greenmobility.vn',
    '$2a$12$JCA1zawBxCOUWZw7NzaX5eVR3waCxB3TjBg7.gweyHP/dPDdo5/Ga',
    'Trần Thị Mai',
    'ROLE_DRIVER',
    'ACTIVE'
),
(
    'a3333333-3333-3333-3333-333333333333',
    '0923456789',
    'driver.nam@greenmobility.vn',
    '$2a$12$JCA1zawBxCOUWZw7NzaX5eVR3waCxB3TjBg7.gweyHP/dPDdo5/Ga',
    'Lê Hoàng Nam',
    'ROLE_DRIVER',
    'ACTIVE'
)
ON CONFLICT (phone_number) DO UPDATE
SET full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    status = EXCLUDED.status;

-- ============================================================================
-- 3. HO SO DOI TAC TAI XE XE DIEN (DRIVER PROFILES)
-- ============================================================================
INSERT INTO driver_profiles (
    id, user_id, citizen_id, driver_license_number, license_class, 
    kyc_status, is_active_shift, rating_avg, total_trips_completed, total_co2_saved_kg
) VALUES
(
    '88888888-1234-5678-9abc-def012345678',
    '9c12b7a8-1234-5678-9abc-def012345678',
    '079202000001',
    'B2-7901234567',
    'B2',
    'APPROVED',
    TRUE,
    5.00,
    142,
    48.250
),
(
    'b2222222-2222-2222-2222-222222222222',
    'a2222222-2222-2222-2222-222222222222',
    '079202000002',
    'A1-7901234568',
    'A1',
    'APPROVED',
    TRUE,
    4.95,
    98,
    31.620
),
(
    'b3333333-3333-3333-3333-333333333333',
    'a3333333-3333-3333-3333-333333333333',
    '079202000003',
    'B2-7901234569',
    'B2',
    'APPROVED',
    TRUE,
    4.90,
    64,
    52.180
)
ON CONFLICT (id) DO UPDATE
SET kyc_status = EXCLUDED.kyc_status,
    is_active_shift = EXCLUDED.is_active_shift,
    rating_avg = EXCLUDED.rating_avg,
    total_trips_completed = EXCLUDED.total_trips_completed,
    total_co2_saved_kg = EXCLUDED.total_co2_saved_kg;

-- ============================================================================
-- 4. PHUONG TIEN XE DIEN (VEHICLES)
-- ============================================================================
INSERT INTO vehicles (
    id, driver_id, vehicle_type, make, model, license_plate, 
    color, battery_capacity_kwh, range_per_charge_km, inspection_expiry_date, is_verified
) VALUES
(
    '77777777-1234-5678-9abc-def012345678',
    '88888888-1234-5678-9abc-def012345678',
    'ELECTRIC_MOTORBIKE',
    'VinFast',
    'Feliz S',
    '59-P1 987.65',
    'Trắng ngọc',
    3.50,
    198,
    '2028-12-31',
    TRUE
),
(
    'c2222222-2222-2222-2222-222222222222',
    'b2222222-2222-2222-2222-222222222222',
    'ELECTRIC_MOTORBIKE',
    'Dat Bike',
    'Weaver++',
    '59-P2 345.67',
    'Đen nhám',
    3.00,
    200,
    '2028-12-31',
    TRUE
),
(
    'c3333333-3333-3333-3333-333333333333',
    'b3333333-3333-3333-3333-333333333333',
    'ELECTRIC_CAR_4SEAT',
    'VinFast',
    'VF e34',
    '51K-888.22',
    'Xanh lục lam',
    42.00,
    318,
    '2028-12-31',
    TRUE
)
ON CONFLICT (driver_id) DO UPDATE
SET license_plate = EXCLUDED.license_plate,
    is_verified = TRUE;

-- ============================================================================
-- 5. DANH SACH CHUYEN DI MAU SPRINT 2 (TRIPS)
-- Da dang trang thai: SEARCHING, MATCHED, IN_TRIP, COMPLETED, CANCELLED
-- ============================================================================
INSERT INTO trips (
    id, trip_code, customer_id, driver_id, vehicle_id, vehicle_type,
    pickup_geom, pickup_address, dropoff_geom, dropoff_address,
    status, estimated_distance_m, estimated_duration_s, actual_distance_m, actual_duration_s,
    fare_amount, discount_amount, final_amount, payment_method, payment_status,
    co2_saved_grams, carbon_credits_earned, loyalty_points_earned,
    requested_at, matched_at, arrived_pickup_at, started_trip_at, completed_at
) VALUES
-- Chuyen 1: Dang tim tai xe (SEARCHING)
(
    'd1111111-1111-1111-1111-111111111111',
    'GM-20261005-9901',
    '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    NULL,
    NULL,
    'ELECTRIC_MOTORBIKE',
    ST_SetSRID(ST_MakePoint(106.7032, 10.7769), 4326),
    'Nhà hát Thành phố, Quận 1, TP.HCM',
    ST_SetSRID(ST_MakePoint(106.6578, 10.7725), 4326),
    'ĐH Bách Khoa, 268 Lý Thường Kiệt, Quận 10, TP.HCM',
    'SEARCHING',
    4726,
    850,
    NULL,
    NULL,
    28000.00,
    0.00,
    28000.00,
    'CASH',
    'PENDING',
    262.34,
    0.0000,
    0,
    NOW() - INTERVAL '3 minutes',
    NULL, NULL, NULL, NULL
),
-- Chuyen 2: Da ghep tai xe thanh cong (MATCHED)
(
    'd2222222-2222-2222-2222-222222222222',
    'GM-20261005-9902',
    '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    '88888888-1234-5678-9abc-def012345678',
    '77777777-1234-5678-9abc-def012345678',
    'ELECTRIC_MOTORBIKE',
    ST_SetSRID(ST_MakePoint(106.6999, 10.7797), 4326),
    'Bưu điện Trung tâm Sài Gòn, Quận 1, TP.HCM',
    ST_SetSRID(ST_MakePoint(106.7042, 10.7716), 4326),
    'Tòa tháp Bitexco Financial, Quận 1, TP.HCM',
    'MATCHED',
    1910,
    420,
    NULL,
    NULL,
    15000.00,
    0.00,
    15000.00,
    'WALLET',
    'PENDING',
    105.95,
    0.0000,
    0,
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '4 minutes 30 seconds',
    NULL, NULL, NULL
),
-- Chuyen 3: Dang tren lo trinh di chuyen (IN_TRIP)
(
    'd3333333-3333-3333-3333-333333333333',
    'GM-20261005-9903',
    '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    'b3333333-3333-3333-3333-333333333333',
    'c3333333-3333-3333-3333-333333333333',
    'ELECTRIC_CAR_4SEAT',
    ST_SetSRID(ST_MakePoint(106.7218, 10.7951), 4326),
    'Landmark 81, Vinhomes Central Park, Bình Thạnh, TP.HCM',
    ST_SetSRID(ST_MakePoint(106.7025, 10.7743), 4326),
    'Phố đi bộ Nguyễn Huệ, Quận 1, TP.HCM',
    'IN_TRIP',
    6970,
    1150,
    NULL,
    NULL,
    82000.00,
    0.00,
    82000.00,
    'WALLET',
    'PENDING',
    410.04,
    0.0000,
    0,
    NOW() - INTERVAL '18 minutes',
    NOW() - INTERVAL '17 minutes',
    NOW() - INTERVAL '14 minutes',
    NOW() - INTERVAL '12 minutes',
    NULL
),
-- Chuyen 4: Da hoan thanh (COMPLETED)
(
    'd4444444-4444-4444-4444-444444444444',
    'GM-20261005-9905',
    '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    'b3333333-3333-3333-3333-333333333333',
    'c3333333-3333-3333-3333-333333333333',
    'ELECTRIC_CAR_4SEAT',
    ST_SetSRID(ST_MakePoint(106.6601, 10.8184), 4326),
    'Sân bay Quốc tế Tân Sơn Nhất, Tân Bình, TP.HCM',
    ST_SetSRID(ST_MakePoint(106.6983, 10.7725), 4326),
    'Chợ Bến Thành, Lê Lợi, Quận 1, TP.HCM',
    'COMPLETED',
    8615,
    1380,
    8615,
    1410,
    101000.00,
    0.00,
    101000.00,
    'WALLET',
    'PAID',
    506.52,
    0.0506,
    10,
    NOW() - INTERVAL '1 hour 15 minutes',
    NOW() - INTERVAL '1 hour 14 minutes',
    NOW() - INTERVAL '1 hour 10 minutes',
    NOW() - INTERVAL '1 hour 8 minutes',
    NOW() - INTERVAL '44 minutes'
),
-- Chuyen 5: Chuyen di bi huy (CANCELLED)
(
    'd5555555-5555-5555-5555-555555555555',
    'GM-20261005-9904',
    '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    NULL,
    NULL,
    'ELECTRIC_MOTORBIKE',
    ST_SetSRID(ST_MakePoint(106.6961, 10.7828), 4326),
    'Hồ Con Rùa, Phường 6, Quận 3, TP.HCM',
    ST_SetSRID(ST_MakePoint(106.7053, 10.7876), 4326),
    'Thảo Cầm Viên Sài Gòn, Quận 1, TP.HCM',
    'CANCELLED',
    1635,
    380,
    NULL,
    NULL,
    12000.00,
    0.00,
    12000.00,
    'CASH',
    'FAILED',
    90.81,
    0.0000,
    0,
    NOW() - INTERVAL '2 hours',
    NULL, NULL, NULL, NULL
),
-- Chuyen 6: Chuyen di hoan thanh truoc do (COMPLETED)
(
    '709f91bf-6b40-4a2b-873e-48e68f9ac482',
    'GM-20261002-5784',
    '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    '88888888-1234-5678-9abc-def012345678',
    '77777777-1234-5678-9abc-def012345678',
    'ELECTRIC_MOTORBIKE',
    ST_SetSRID(ST_MakePoint(106.6983, 10.7725), 4326),
    'Chợ Bến Thành, Lê Lợi, Quận 1, TP.HCM',
    ST_SetSRID(ST_MakePoint(106.7218, 10.7951), 4326),
    'Landmark 81, Bình Thạnh, TP.HCM',
    'COMPLETED',
    4108,
    720,
    4108,
    745,
    24000.00,
    0.00,
    24000.00,
    'WALLET',
    'PAID',
    228.02,
    0.0228,
    5,
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days' + INTERVAL '30 seconds',
    NOW() - INTERVAL '3 days' + INTERVAL '4 minutes',
    NOW() - INTERVAL '3 days' + INTERVAL '5 minutes',
    NOW() - INTERVAL '3 days' + INTERVAL '17 minutes'
)
ON CONFLICT (trip_code) DO UPDATE
SET status = EXCLUDED.status,
    fare_amount = EXCLUDED.fare_amount,
    co2_saved_grams = EXCLUDED.co2_saved_grams,
    payment_status = EXCLUDED.payment_status;

-- Cap nhat ly do huy cho chuyen CANCELLED
UPDATE trips 
SET cancel_reason = 'Khách hàng thay đổi kế hoạch di chuyển', 
    cancelled_by = 'CUSTOMER'
WHERE trip_code = 'GM-20261005-9904';

-- ============================================================================
-- 6. HOA DON TAC DONG XANH (TRIP IMPACT RECEIPTS)
-- ============================================================================
INSERT INTO trip_impact_receipts (
    id, trip_id, co2_saved_grams, baseline_gasoline_co2_grams, 
    ev_emitted_co2_grams, tree_absorption_days_equiv, led_bulb_hours_equiv, shareable_slug
) VALUES
(
    'e1111111-1111-1111-1111-111111111111',
    (SELECT id FROM trips WHERE trip_code = 'GM-20261005-9905'),
    506.52,
    1206.10,
    699.58,
    8.44,
    45.10,
    'gm-receipt-gm202610059905'
),
(
    'e2222222-2222-2222-2222-222222222222',
    (SELECT id FROM trips WHERE trip_code = 'GM-20261002-5784'),
    228.02,
    287.56,
    59.54,
    3.80,
    20.30,
    'gm-receipt-gm202610025784'
)
ON CONFLICT (trip_id) DO UPDATE
SET co2_saved_grams = EXCLUDED.co2_saved_grams,
    tree_absorption_days_equiv = EXCLUDED.tree_absorption_days_equiv,
    led_bulb_hours_equiv = EXCLUDED.led_bulb_hours_equiv;
