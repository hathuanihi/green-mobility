-- V1__init_schema.sql: Initial Schema for Green Mobility Platform

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Bảng Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(120) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_phone ON users(phone_number);

-- 2. Bảng Hồ sơ Tài xế
CREATE TABLE driver_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    citizen_id VARCHAR(30) UNIQUE NOT NULL,
    driver_license_number VARCHAR(30) UNIQUE NOT NULL,
    license_class VARCHAR(10) NOT NULL,
    kyc_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    kyc_rejection_reason TEXT,
    face_encoding_vector FLOAT8[],
    is_active_shift BOOLEAN DEFAULT FALSE,
    rating_avg NUMERIC(3, 2) DEFAULT 5.00,
    total_trips_completed INT DEFAULT 0,
    total_co2_saved_kg NUMERIC(10, 3) DEFAULT 0.000,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng Phương tiện Xe điện
CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id UUID UNIQUE NOT NULL REFERENCES driver_profiles(id) ON DELETE CASCADE,
    vehicle_type VARCHAR(30) NOT NULL,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    license_plate VARCHAR(20) UNIQUE NOT NULL,
    color VARCHAR(30) NOT NULL,
    battery_capacity_kwh NUMERIC(5, 2) NOT NULL,
    range_per_charge_km INT NOT NULL,
    registration_certificate_url VARCHAR(500),
    inspection_expiry_date DATE NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng Lịch sử Xác thực Khuôn mặt vào ca
CREATE TABLE face_verification_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id UUID NOT NULL REFERENCES driver_profiles(id),
    selfie_image_url VARCHAR(500) NOT NULL,
    similarity_score NUMERIC(5, 4) NOT NULL,
    is_passed BOOLEAN NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_face_logs_driver ON face_verification_logs(driver_id, verified_at DESC);

-- 5. Bảng Hệ số Phát thải Chuẩn
CREATE TABLE emission_factors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_category VARCHAR(50) NOT NULL,
    baseline_gasoline_factor_gco2_km NUMERIC(8, 2) NOT NULL,
    ev_energy_consumption_kwh_km NUMERIC(6, 4) NOT NULL,
    grid_emission_factor_gco2_kwh NUMERIC(8, 2) NOT NULL,
    calculated_ev_factor_gco2_km NUMERIC(8, 2) NOT NULL,
    net_co2_saving_per_km NUMERIC(8, 2) NOT NULL,
    region VARCHAR(50) DEFAULT 'VIETNAM_NATIONAL',
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_emission_active ON emission_factors(vehicle_category, is_active);

-- 6. Bảng Chuyến đi
CREATE TABLE trips (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_code VARCHAR(32) UNIQUE NOT NULL,
    customer_id UUID NOT NULL REFERENCES users(id),
    driver_id UUID REFERENCES driver_profiles(id),
    vehicle_id UUID REFERENCES vehicles(id),
    vehicle_type VARCHAR(30) NOT NULL,
    pickup_geom geometry(Point, 4326) NOT NULL,
    pickup_address TEXT NOT NULL,
    dropoff_geom geometry(Point, 4326) NOT NULL,
    dropoff_address TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    cancel_reason TEXT,
    cancelled_by VARCHAR(20),
    estimated_distance_m INT NOT NULL,
    estimated_duration_s INT NOT NULL,
    actual_distance_m INT,
    actual_duration_s INT,
    fare_amount NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) DEFAULT 0.00,
    final_amount NUMERIC(12, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    co2_saved_grams NUMERIC(10, 2) DEFAULT 0.00,
    carbon_credits_earned NUMERIC(10, 4) DEFAULT 0.0000,
    loyalty_points_earned INT DEFAULT 0,
    requested_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    matched_at TIMESTAMP WITH TIME ZONE,
    arrived_pickup_at TIMESTAMP WITH TIME ZONE,
    started_trip_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_trips_pickup_geom ON trips USING GIST(pickup_geom);
CREATE INDEX idx_trips_customer ON trips(customer_id, requested_at DESC);
CREATE INDEX idx_trips_driver ON trips(driver_id, requested_at DESC);
CREATE INDEX idx_trips_status ON trips(status);

-- 7. Bảng Hóa đơn Tác động Xanh
CREATE TABLE trip_impact_receipts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id UUID UNIQUE NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    co2_saved_grams NUMERIC(10, 2) NOT NULL,
    baseline_gasoline_co2_grams NUMERIC(10, 2) NOT NULL,
    ev_emitted_co2_grams NUMERIC(10, 2) NOT NULL,
    tree_absorption_days_equiv NUMERIC(6, 2) NOT NULL,
    led_bulb_hours_equiv NUMERIC(8, 2) NOT NULL,
    shareable_slug VARCHAR(64) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Tài khoản Sổ cái
CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number VARCHAR(50) UNIQUE NOT NULL,
    owner_id UUID REFERENCES users(id),
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(20) NOT NULL,
    balance NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. Giao dịch Sổ cái
CREATE TABLE ledger_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_code VARCHAR(40) UNIQUE NOT NULL,
    trip_id UUID REFERENCES trips(id),
    transaction_type VARCHAR(30) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 10. Bút toán Đối ứng
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL REFERENCES ledger_transactions(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES ledger_accounts(id),
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ledger_entries_tx ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_acc ON ledger_entries(account_id);

-- 11. Bảng Danh mục Đổi thưởng
CREATE TABLE rewards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(150) NOT NULL,
    description TEXT,
    partner_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    carbon_credit_cost NUMERIC(10, 2) DEFAULT 0.00,
    loyalty_point_cost INT DEFAULT 0,
    image_url VARCHAR(500),
    quantity_available INT NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 12. Bảng Lịch sử Đổi thưởng
CREATE TABLE reward_redemptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    reward_id UUID NOT NULL REFERENCES rewards(id),
    redemption_code VARCHAR(32) UNIQUE NOT NULL,
    points_spent INT DEFAULT 0,
    carbon_spent NUMERIC(10, 2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 13. Bảng Cảnh báo Gian lận
CREATE TABLE fraud_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id UUID REFERENCES trips(id),
    driver_id UUID REFERENCES driver_profiles(id),
    customer_id UUID REFERENCES users(id),
    alert_type VARCHAR(50) NOT NULL,
    risk_score NUMERIC(5, 2) NOT NULL,
    details JSONB NOT NULL,
    resolution_status VARCHAR(30) DEFAULT 'PENDING',
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(resolution_status);
