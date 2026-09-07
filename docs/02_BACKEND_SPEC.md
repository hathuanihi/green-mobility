# Green Mobility Platform - Backend Technical Specification

> **Tài liệu**: Đặc tả Kỹ thuật Backend (Backend Specification)  
> **Dự án**: Nền tảng gọi xe điện tích hợp hệ thống đo lường, báo cáo và khuyến khích giảm phát thải carbon  
> **Ngôn ngữ & Framework**: Java 21 LTS, Spring Boot 3.3+, Spring Security 6, Spring Data JPA, Spring WebSocket (STOMP)  
> **Cơ sở dữ liệu**: PostgreSQL 16 + PostGIS, MongoDB 7.0, Redis 7.2  
> **Message Broker**: RabbitMQ 3.13 / Kafka  
> **Phiên bản**: 1.0.0 (Production Blueprint)

---

## 1. Cấu trúc Module Backend (Spring Boot Modular Monolith)

Dự án được tổ chức theo chuẩn **Domain-Driven Design (DDD)** trong một codebase Monolith phân tách các module độc lập:

```text
backend/
├── src/main/java/com/greenmobility/
│   ├── GreenMobilityApplication.java
│   │
│   ├── common/                        # Dùng chung: Exceptions, DTOs, Base Entities, Utils
│   │   ├── exception/
│   │   ├── response/ApiResponse.java
│   │   ├── utils/GeometryUtils.java
│   │   └── security/CurrentUser.java
│   │
│   ├── config/                        # Cấu hình Spring Boot
│   │   ├── SecurityConfig.java
│   │   ├── WebSocketConfig.java
│   │   ├── RedisConfig.java
│   │   ├── RabbitMQConfig.java
│   │   └── PostGisConfig.java
│   │
│   ├── modules/
│   │   ├── identity/                  # Quản lý tài khoản, JWT, Phân quyền (RBAC)
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── entity/User.java, Role.java
│   │   │
│   │   ├── drivervehicle/             # Quản lý hồ sơ tài xế, xe điện, KYC, Face Verification
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── entity/DriverProfile.java, Vehicle.java, FaceVerificationLog.java
│   │   │
│   │   ├── matching/                  # Thuật toán ghép cuốc (Redis GEO + Scoring Engine)
│   │   │   ├── service/MatchingEngineService.java
│   │   │   ├── model/DriverCandidate.java
│   │   │   └── redis/DriverGeoRedisRepository.java
│   │   │
│   │   ├── trip/                      # Quản lý vòng đời cuốc xe, State Machine, Tracking
│   │   │   ├── controller/
│   │   │   ├── service/TripService.java, TripLifecycleManager.java
│   │   │   ├── repository/TripRepository.java
│   │   │   ├── websocket/TripLocationWebSocketHandler.java
│   │   │   └── entity/Trip.java, TripStatus.java, TripReceipt.java
│   │   │
│   │   ├── carbon/                    # Engine tính toán phát thải CO2 & Impact Receipt
│   │   │   ├── controller/
│   │   │   ├── service/CarbonCalculationService.java, EmissionFactorService.java
│   │   │   ├── repository/EmissionFactorRepository.java, CarbonRecordRepository.java
│   │   │   └── entity/EmissionFactor.java, CarbonRecord.java
│   │   │
│   │   ├── incentive/                 # Sổ cái kép (Double-Entry Ledger), Ví Carbon, Loyalty Points, Quà tặng
│   │   │   ├── controller/
│   │   │   ├── service/LedgerEngineService.java, RewardService.java
│   │   │   ├── repository/LedgerTransactionRepository.java, LedgerEntryRepository.java
│   │   │   └── entity/LedgerAccount.java, LedgerTransaction.java, LedgerEntry.java, Reward.java
│   │   │
│   │   ├── payment/                   # Cổng thanh toán trực tuyến (VNPay / MoMo Sandbox)
│   │   │   ├── controller/PaymentController.java
│   │   │   └── service/VnPayService.java, MomoService.java
│   │   │
│   │   └── fraud/                     # Phát hiện gian lận GPS Spoofing & Anomaly Detection
│   │       ├── service/FraudDetectionService.java, IsolationForestClient.java
│   │       └── entity/FraudAlert.java
│   │
└── src/main/resources/
    ├── application.yml
    ├── db/migration/                  # Flyway Database Migrations (V1, V2...)
    └── seed-data/                     # Dữ liệu mẫu ban đầu (Hệ số phát thải, xe mẫu)
```

---

## 2. Thiết kế Cơ sở Dữ liệu Quan hệ & Không gian địa lý (PostgreSQL + PostGIS DDL)

Hệ thống sử dụng **PostgreSQL 16** với extension **PostGIS** để lưu trữ và truy vấn hình học không gian địa lý (`geometry(Point, 4326)`).

### 2.1. Bảng Người dùng & Phân quyền (`identity`)
```sql
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
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_CUSTOMER', -- ROLE_CUSTOMER, ROLE_DRIVER, ROLE_ADMIN, ROLE_OPERATOR
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',      -- ACTIVE, SUSPENDED, PENDING_KYC
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_phone ON users(phone_number);
```

### 2.2. Bảng Tài xế & Phương tiện xe điện (`drivervehicle`)
```sql
-- 2. Bảng Hồ sơ Tài xế
CREATE TABLE driver_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    citizen_id VARCHAR(30) UNIQUE NOT NULL,            -- Số CCCD
    driver_license_number VARCHAR(30) UNIQUE NOT NULL, -- Số GPLX
    license_class VARCHAR(10) NOT NULL,                -- A1, A2, B1, B2
    kyc_status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    kyc_rejection_reason TEXT,
    face_encoding_vector FLOAT8[],                     -- Vector 512 chiều nhận diện khuôn mặt
    is_active_shift BOOLEAN DEFAULT FALSE,             -- Đang trong ca làm việc
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
    vehicle_type VARCHAR(30) NOT NULL,                 -- ELECTRIC_MOTORBIKE, ELECTRIC_CAR_4SEAT, ELECTRIC_CAR_7SEAT
    make VARCHAR(50) NOT NULL,                         -- VinFast, Dat Bike, Yadea
    model VARCHAR(50) NOT NULL,                        -- Feliz S, Klara S, VF e34, VF 8
    license_plate VARCHAR(20) UNIQUE NOT NULL,         -- Biển số xe (VD: 59A-123.45)
    color VARCHAR(30) NOT NULL,
    battery_capacity_kwh NUMERIC(5, 2) NOT NULL,       -- Dung lượng pin thiết kế (kWh)
    range_per_charge_km INT NOT NULL,                  -- Quãng đường di chuyển 1 lần sạc đầy (km)
    registration_certificate_url VARCHAR(500),
    inspection_expiry_date DATE NOT NULL,              -- Hạn kiểm định
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng Lịch sử Xác thực Khuôn mặt vào ca
CREATE TABLE face_verification_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id UUID NOT NULL REFERENCES driver_profiles(id),
    selfie_image_url VARCHAR(500) NOT NULL,
    similarity_score NUMERIC(5, 4) NOT NULL,           -- Độ khớp cosine (0.0000 - 1.0000)
    is_passed BOOLEAN NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_face_logs_driver ON face_verification_logs(driver_id, verified_at DESC);
```

### 2.3. Bảng Cấu hình Hệ số Phát thải Carbon (`carbon`)
```sql
-- 5. Bảng Hệ số Phát thải Chuẩn
CREATE TABLE emission_factors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vehicle_category VARCHAR(50) NOT NULL,            -- MOTORBIKE, CAR_4SEATS, CAR_7SEATS
    baseline_gasoline_factor_gco2_km NUMERIC(8, 2) NOT NULL, -- Hệ số xe xăng đối chứng (gCO2/km)
    ev_energy_consumption_kwh_km NUMERIC(6, 4) NOT NULL,     -- Suất tiêu hao điện năng (kWh/km)
    grid_emission_factor_gco2_kwh NUMERIC(8, 2) NOT NULL,    -- Hệ số lưới điện VN (gCO2/kWh)
    calculated_ev_factor_gco2_km NUMERIC(8, 2) NOT NULL,     -- = SEC * EF_grid
    net_co2_saving_per_km NUMERIC(8, 2) NOT NULL,            -- = baseline - ev_factor
    region VARCHAR(50) DEFAULT 'VIETNAM_NATIONAL',
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_emission_active ON emission_factors(vehicle_category, is_active);
```

### 2.4. Bảng Quản lý Chuyến đi & Tọa độ Không gian (`trip`)
```sql
-- 6. Bảng Chuyến đi
CREATE TABLE trips (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_code VARCHAR(32) UNIQUE NOT NULL,            -- Mã chuyến đi hiển thị (VD: GM-20260907-8891)
    customer_id UUID NOT NULL REFERENCES users(id),
    driver_id UUID REFERENCES driver_profiles(id),
    vehicle_id UUID REFERENCES vehicles(id),
    vehicle_type VARCHAR(30) NOT NULL,
    
    -- Tọa độ điểm đón và trả (PostGIS Point, SRID 4326)
    pickup_geom geometry(Point, 4326) NOT NULL,
    pickup_address TEXT NOT NULL,
    dropoff_geom geometry(Point, 4326) NOT NULL,
    dropoff_address TEXT NOT NULL,
    
    -- Trạng thái
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',   -- REQUESTED, SEARCHING, MATCHED, DRIVER_ARRIVING, ARRIVED, IN_TRIP, COMPLETED, CANCELLED
    cancel_reason TEXT,
    cancelled_by VARCHAR(20),                          -- CUSTOMER, DRIVER, SYSTEM
    
    -- Khoảng cách & Thời gian
    estimated_distance_m INT NOT NULL,                 -- Mét
    estimated_duration_s INT NOT NULL,                 -- Giây
    actual_distance_m INT,                             -- Đo đạc thực tế từ GPS vệt hành trình
    actual_duration_s INT,
    
    -- Tài chính
    fare_amount NUMERIC(12, 2) NOT NULL,               -- Cước phí chuyến đi (VND)
    discount_amount NUMERIC(12, 2) DEFAULT 0.00,
    final_amount NUMERIC(12, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,               -- CASH, VNPAY, MOMO, GREEN_WALLET
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, FAILED, REFUNDED
    
    -- Môi trường & Carbon
    co2_saved_grams NUMERIC(10, 2) DEFAULT 0.00,       -- Lượng CO2 giảm thực tế (gram)
    carbon_credits_earned NUMERIC(10, 4) DEFAULT 0.0000,-- Số tín chỉ carbon thưởng
    loyalty_points_earned INT DEFAULT 0,               -- Điểm thưởng tích lũy
    
    -- Thời gian mốc
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

-- 7. Bảng Hóa đơn Tác động Xanh (Impact Receipt)
CREATE TABLE trip_impact_receipts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id UUID UNIQUE NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    co2_saved_grams NUMERIC(10, 2) NOT NULL,
    baseline_gasoline_co2_grams NUMERIC(10, 2) NOT NULL,
    ev_emitted_co2_grams NUMERIC(10, 2) NOT NULL,
    tree_absorption_days_equiv NUMERIC(6, 2) NOT NULL, -- Tương đương số ngày hấp thụ của 1 cây
    led_bulb_hours_equiv NUMERIC(8, 2) NOT NULL,       -- Tương đương số giờ thắp đèn LED 10W
    shareable_slug VARCHAR(64) UNIQUE NOT NULL,        -- Đường dẫn chia sẻ MXH (VD: /impact/GM-8891)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 2.5. Hệ thống Sổ cái kép (Double-Entry Ledger) cho Điểm & Tín chỉ Carbon (`incentive`)
```sql
-- 8. Tài khoản Sổ cái
CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_number VARCHAR(50) UNIQUE NOT NULL,        -- VD: USR_CB_UUID, SYS_CARBON_POOL, SYS_BURN_POOL
    owner_id UUID REFERENCES users(id),                -- NULL nếu là tài khoản hệ thống
    account_type VARCHAR(30) NOT NULL,                 -- ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    currency VARCHAR(20) NOT NULL,                     -- CARBON_CREDIT (gCO2), LOYALTY_POINT
    balance NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,    -- Số dư được tính toán và đối soát
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. Giao dịch Sổ cái
CREATE TABLE ledger_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_code VARCHAR(40) UNIQUE NOT NULL,      -- Mã giao dịch (VD: TX-CB-20260907-001)
    trip_id UUID REFERENCES trips(id),
    transaction_type VARCHAR(30) NOT NULL,             -- TRIP_EMISSION_REWARD, REDEEM_GIFT, CARBON_OFFSET_TRANSFER
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 10. Bút toán Đối ứng (Bắt buộc Tổng Debit = Tổng Credit trên mỗi Transaction)
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL REFERENCES ledger_transactions(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES ledger_accounts(id),
    entry_type VARCHAR(10) NOT NULL,                   -- DEBIT, CREDIT
    amount NUMERIC(18, 4) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ledger_entries_tx ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_acc ON ledger_entries(account_id);

-- 11. Bảng Danh mục Đổi thưởng (Reward Catalog)
CREATE TABLE rewards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(150) NOT NULL,
    description TEXT,
    partner_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,                     -- VOUCHER_RIDE, TREE_PLANTING, ECO_PRODUCT
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
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',   -- COMPLETED, USED, EXPIRED, CANCELLED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 2.6. Bảng Quản lý Cảnh báo Gian lận (`fraud`)
```sql
-- 13. Bảng Cảnh báo Gian lận
CREATE TABLE fraud_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id UUID REFERENCES trips(id),
    driver_id UUID REFERENCES driver_profiles(id),
    customer_id UUID REFERENCES users(id),
    alert_type VARCHAR(50) NOT NULL,                   -- GPS_SPOOFING, TELEPORTATION, UNREALISTIC_SPEED, CARBON_FARMING
    risk_score NUMERIC(5, 2) NOT NULL,                 -- 0.00 đến 100.00 (hoặc ML Anomaly Score)
    details JSONB NOT NULL,
    resolution_status VARCHAR(30) DEFAULT 'PENDING',  -- PENDING, INVESTIGATING, DISMISSED, CONFIRMED_FRAUD_BANNED
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(resolution_status);
```

---

## 3. Thiết kế Cơ sở Dữ liệu NoSQL (MongoDB Telemetry Schema)

MongoDB lưu trữ chi tiết từng tọa độ GPS của tài xế và hành trình chuyến đi nhằm giảm tải cho PostgreSQL:

### Collection: `driver_gps_traces`
```javascript
{
  "_id": ObjectId("66dc53842fae9102c4889101"),
  "driverId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "tripId": "7bb192a0-4318-4a92-b68e-5b1287c80521", // null nếu đang rảnh
  "vehicleType": "ELECTRIC_CAR_4SEAT",
  "timestamp": ISODate("2026-09-07T14:30:15.000Z"),
  "location": {
    "type": "Point",
    "coordinates": [106.700981, 10.776530] // [longitude, latitude]
  },
  "speedKmh": 32.5,
  "heading": 145.2, // Góc hướng la bàn (0 - 360 độ)
  "accuracyMeters": 4.2,
  "batteryPercent": 84,
  "isMockGpsDetected": false
}
```
*Index*:
```javascript
db.driver_gps_traces.createIndex({ "location": "2dsphere" });
db.driver_gps_traces.createIndex({ "tripId": 1, "timestamp": 1 });
db.driver_gps_traces.createIndex({ "driverId": 1, "timestamp": -1 });
// TTL index tự động giải phóng sau 60 ngày để tiết kiệm dung lượng
db.driver_gps_traces.createIndex({ "timestamp": 1 }, { expireAfterSeconds: 5184000 });
```

---

## 4. Thiết kế Bộ nhớ đệm Redis & Matching Engine

### 4.1. Cấu trúc Khóa trên Redis
* **Vị trí Tài xế Khả dụng (`GEO`)**:
  - Key: `drivers:geo:available:{vehicleType}` (VD: `drivers:geo:available:ELECTRIC_MOTORBIKE`)
  - Giá trị: Lưu trữ `longitude`, `latitude`, `driverId`.
  - Cập nhật: Khi nhận GPS từ tài xế đang `ONLINE` và `AVAILABLE`.
* **Trạng thái Trực tuyến của Tài xế (`HASH`)**:
  - Key: `driver:state:{driverId}`
  - Fields: `status` (ONLINE, OFFLINE, BUSY), `currentTripId`, `batteryPercent`, `lastHeartbeat`, `lat`, `lng`.
  - Expiration: Heartbeat TTL 30 giây.
* **Khóa phân tán Chống tranh chấp nhận cuốc (Distributed Lock)**:
  - Key: `lock:trip:dispatch:{tripId}` hoặc `lock:driver:dispatch:{driverId}`
  - Thời lượng: 15 giây (khớp với thời gian đếm ngược nhận cuốc).

### 4.2. Thuật toán Ghép cặp Tài xế (Tiered Geospatial Matching Algorithm)

```mermaid
flowchart TD
    A[Khách hàng tạo Trip REQUESTED] --> B[Khởi tạo Tier 1: Bán kính R1 = 1.5 km]
    B --> C[Query Redis GEO: GEOSEARCH ... FROMLONLAT ... BYRADIUS R1 WITHDIST]
    C --> D{Có tài xế phù hợp?}
    
    D -- Không --> E{Đạt bán kính tối đa R3 = 5.0 km?}
    E -- Chưa --> F[Mở rộng sang Tier tiếp theo: R2 = 3.0 km / R3 = 5.0 km]
    F --> C
    E -- Đã vượt quá --> G[Hết thời gian: Trả về NO_DRIVER_FOUND]
    
    D -- Có --> H[Tính toán điểm số ứng viên: Score Calculation]
    H --> I[Xếp hạng danh sách ứng viên giảm dần]
    I --> J[Chọn tài xế điểm cao nhất: Best Driver]
    J --> K[Acquire Distributed Lock: lock:driver:dispatch]
    K --> L[Đẩy WebSocket Event /user/queue/ride-dispatch đến Tài xế]
    L --> M{Tài xế phản hồi trong 15s?}
    M -- Chấp nhận --> N[Chuyển trạng thái: MATCHED, Bắt đầu đón khách]
    M -- Từ chối / Timeout --> O[Release Lock, Thêm driverId vào Blacklist của Trip này]
    O --> C
```

#### Công thức Tính điểm Ưu tiên (Driver Candidate Scoring Function):
$$\text{Score}(d) = w_1 \cdot \left(1 - \frac{\text{Distance}}{\text{MaxRadius}}\right) + w_2 \cdot \left(\frac{\text{BatteryPercent}}{100}\right) + w_3 \cdot \left(\frac{\text{Rating}}{5.0}\right) - w_4 \cdot \text{DeclinedCountToday}$$
*Trọng số chuẩn hóa*: $w_1 = 0.50$ (khoảng cách là yếu tố quan trọng nhất), $w_2 = 0.20$ (ưu tiên pin khỏe), $w_3 = 0.20$ (chất lượng phục vụ), $w_4 = 0.10$ (phạt tài xế hay hủy cuốc).

---

## 5. Đặc tả Chi tiết REST API (OpenAPI Format)

Base URL: `https://api.greenmobility.vn/api/v1`

### 5.1. Authentication & KYC Module (`/auth`, `/driver`)

#### `POST /auth/register`
* **Mô tả**: Đăng ký tài khoản khách hàng hoặc tài xế.
* **Request Body**:
```json
{
  "phoneNumber": "+84901234567",
  "password": "SecurePassword123@",
  "fullName": "Phạm Hà Anh Thư",
  "email": "anhthu@greenmobility.vn",
  "role": "ROLE_CUSTOMER"
}
```
* **Response (201 Created)**:
```json
{
  "success": true,
  "message": "Đăng ký tài khoản thành công",
  "data": {
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "d8e32904-8b1c-4b68-80f2-e5672c4789b1",
    "expiresIn": 86400
  }
}
```

#### `POST /driver/kyc/submit`
* **Mô tả**: Tài xế nộp hồ sơ KYC và giấy tờ xe điện.
* **Headers**: `Authorization: Bearer <JWT>`
* **Request Body (Multipart / Form-Data)**:
  - `citizenId`: "079203001234"
  - `licenseNumber`: "790123456789"
  - `licenseClass`: "A1"
  - `vehicleType`: "ELECTRIC_MOTORBIKE"
  - `make`: "VinFast"
  - `model`: "Feliz S"
  - `licensePlate`: "59-P1 987.65"
  - `batteryCapacityKwh`: 3.5
  - `rangePerChargeKm`: 198
  - `inspectionExpiryDate`: "2027-12-31"
  - `citizenCardFrontImage`: [File]
  - `citizenCardBackImage`: [File]
  - `driverLicenseImage`: [File]
  - `vehicleRegistrationImage`: [File]
  - `facePortraitImage`: [File] (Lưu trữ ảnh chuẩn để tạo Face Vector 512d)
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Hồ sơ KYC đã được gửi, vui lòng chờ duyệt",
  "data": {
    "kycStatus": "PENDING"
  }
}
```

#### `POST /driver/shift/face-verify`
* **Mô tả**: Tài xế chụp ảnh selfie trước khi bật ca làm việc để kiểm tra Face Verification.
* **Request Body (Multipart)**:
  - `selfie`: [Image File]
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Xác thực khuôn mặt thành công",
  "data": {
    "isPassed": true,
    "similarityScore": 0.8842,
    "shiftActive": true
  }
}
```

---

### 5.2. Chuyến đi & Đặt xe (`/trips`)

#### `POST /trips/estimate`
* **Mô tả**: Ước tính cước phí và lượng CO2 giảm được trước khi đặt xe.
* **Request Body**:
```json
{
  "pickupLat": 10.776530,
  "pickupLng": 106.700981,
  "pickupAddress": "Nhà hát Thành phố, Quận 1, TP.HCM",
  "dropoffLat": 10.870020,
  "dropoffLng": 106.803054,
  "dropoffAddress": "Trường ĐH Công nghệ Thông tin, TP. Thủ Đức, TP.HCM",
  "vehicleType": "ELECTRIC_MOTORBIKE"
}
```
* **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "estimatedDistanceKm": 16.2,
    "estimatedDurationMinutes": 32,
    "estimatedFareVnd": 85000,
    "carbonEstimate": {
      "co2SavedGrams": 735.48,
      "treeAbsorptionDaysEquiv": 12.26,
      "baselineGasolineCo2Grams": 1109.70,
      "evEmittedCo2Grams": 374.22
    }
  }
}
```

#### `POST /trips/request`
* **Mô tả**: Khách hàng xác nhận tạo yêu cầu đặt xe.
* **Request Body**:
```json
{
  "pickupLat": 10.776530,
  "pickupLng": 106.700981,
  "pickupAddress": "Nhà hát Thành phố, Quận 1, TP.HCM",
  "dropoffLat": 10.870020,
  "dropoffLng": 106.803054,
  "dropoffAddress": "Trường ĐH Công nghệ Thông tin, TP. Thủ Đức, TP.HCM",
  "vehicleType": "ELECTRIC_MOTORBIKE",
  "paymentMethod": "MOMO"
}
```
* **Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "tripId": "7bb192a0-4318-4a92-b68e-5b1287c80521",
    "tripCode": "GM-20260907-8891",
    "status": "SEARCHING",
    "requestedAt": "2026-09-07T14:30:00Z"
  }
}
```

#### `GET /trips/{tripId}/impact-receipt`
* **Mô tả**: Lấy chi tiết hóa đơn tác động xanh sau khi cuốc xe kết thúc.
* **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "tripCode": "GM-20260907-8891",
    "completedAt": "2026-09-07T15:05:22Z",
    "actualDistanceKm": 16.5,
    "totalFareVnd": 85000,
    "co2SavedGrams": 749.10,
    "treeDaysEquiv": 12.48,
    "ledHoursEquiv": 103.74,
    "carbonCreditsEarned": 0.7491,
    "loyaltyPointsEarned": 85,
    "shareUrl": "https://greenmobility.vn/impact/GM-20260907-8891"
  }
}
```

---

### 5.3. Ví Carbon & Đổi thưởng (`/incentive`)

#### `GET /incentive/wallet`
* **Mô tả**: Xem số dư ví tín chỉ carbon và điểm thưởng.
* **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "carbonCreditBalance": 45.8250,
    "loyaltyPointBalance": 1250,
    "lifetimeCo2SavedKg": 45.825,
    "currentTier": "GOLD_ECO_WARRIOR",
    "badgeCount": 6
  }
}
```

#### `POST /incentive/rewards/{rewardId}/redeem`
* **Mô tả**: Đổi điểm lấy voucher hoặc đóng góp trồng cây xanh.
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Đổi quà thành công",
  "data": {
    "redemptionCode": "VOUCHER-TREE-50K",
    "remainingPoints": 750,
    "remainingCarbonCredits": 45.8250
  }
}
```

---

## 6. Giao thức Thời gian thực (WebSocket / STOMP Specifications)

* **WebSocket Handshake URL**: `wss://api.greenmobility.vn/ws-connect`
* **Xác thực**: JWT Bearer Token truyền trong STOMP Connect Header:
  ```stomp
  CONNECT
  Authorization:Bearer eyJhbGciOiJIUzI1Ni...
  accept-version:1.2,1.1,1.0
  heart-beat:10000,10000
  ^@
  ```

### Các Kênh Đăng ký (STOMP Destinations):

1. **Khách hàng theo dõi vị trí tài xế đang tới đón / đang chở**:
   - `SUBSCRIBE` $\rightarrow$ `/topic/driver-location/{driverId}`
   - Payload nhận được:
     ```json
     {
       "driverId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
       "lat": 10.777120,
       "lng": 106.701540,
       "bearing": 92.4,
       "speedKmh": 28.5,
       "timestamp": 1788791400000
     }
     ```

2. **Khách hàng và Tài xế lắng nghe biến động trạng thái cuốc xe**:
   - `SUBSCRIBE` $\rightarrow$ `/topic/trip/{tripId}`
   - Payload nhận được:
     ```json
     {
       "tripId": "7bb192a0-4318-4a92-b68e-5b1287c80521",
       "status": "ARRIVED",
       "message": "Tài xế đã đến điểm đón",
       "etaSeconds": 0
     }
     ```

3. **Tài xế lắng nghe đơn hàng được chỉ định (Dispatching Channel)**:
   - `SUBSCRIBE` $\rightarrow$ `/user/queue/ride-dispatch`
   - Payload nhận được:
     ```json
     {
       "tripId": "7bb192a0-4318-4a92-b68e-5b1287c80521",
       "pickupAddress": "Nhà hát Thành phố, Quận 1",
       "pickupLat": 10.776530,
       "pickupLng": 106.700981,
       "dropoffAddress": "ĐH Công nghệ Thông tin, TP. Thủ Đức",
       "distanceKm": 16.2,
       "fareVnd": 85000,
       "countdownSeconds": 15
     }
     ```

4. **Tài xế gửi cập nhật GPS (Client Stream to Server)**:
   - `SEND` $\rightarrow$ `/app/driver/location-update`
   - Payload:
     ```json
     {
       "lat": 10.776890,
       "lng": 106.701120,
       "speed": 31.0,
       "heading": 85.0,
       "batteryPercent": 82,
       "tripId": "7bb192a0-4318-4a92-b68e-5b1287c80521"
     }
     ```

---

## 7. Kiến trúc Xử lý Sự kiện Bất đồng bộ (Event-Driven RabbitMQ)

Mô hình Publish/Subscribe sử dụng **RabbitMQ Topic Exchange** tên `greenmobility.topic.exchange`:

| Routing Key | Producer | Consumer | Mô tả Nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `trip.event.requested` | `module-trip` | `module-matching` | Kích hoạt Matching Engine tìm tài xế |
| `trip.event.completed` | `module-trip` | `module-carbon` | Chốt quãng đường thực tế và tính $CO_2$ |
| `carbon.event.calculated` | `module-carbon` | `module-incentive` | Kích hoạt Sổ cái kép sinh Tín chỉ & Điểm |
| `trip.event.completed` | `module-trip` | `module-fraud` | Đưa vệt GPS vào mô hình Isolation Forest |
| `trip.event.dispatched` | `module-matching` | `notification-service` | Gửi Push Notification (FCM) nếu app nền |

### Payload Mẫu: `carbon.event.calculated`
```json
{
  "eventId": "evt-8b3879a1-50e3-4d43-9ce5-123456789abc",
  "tripId": "7bb192a0-4318-4a92-b68e-5b1287c80521",
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "driverId": "9c12b7a8-1234-5678-9abc-def012345678",
  "actualDistanceKm": 16.5,
  "co2SavedGrams": 749.10,
  "timestamp": "2026-09-07T15:05:25Z"
}
```

---

## 8. Quy tắc Bất biến của Sổ cái Kép (Double-Entry Ledger Invariants)

1. **Nguyên tắc Cân bằng Bút toán**:
   $$\sum_{i \in \text{Entries}} \text{Debit}_i = \sum_{j \in \text{Entries}} \text{Credit}_j$$
   Mọi transaction vi phạm nguyên tắc này sẽ bị Database Trigger / Application Check ném ngoại lệ `LedgerImbalanceException` và rollback toàn bộ giao dịch.
2. **Kịch bản Cộng thưởng sau Chuyến đi (Trip Reward Transaction)**:
   - **Tín chỉ Carbon**:
     - *Bút toán 1*: `DEBIT` tài khoản `SYS_CARBON_RESERVE` giá trị $+0.7491$ PCC.
     - *Bút toán 2*: `CREDIT` tài khoản `USER_CARBON_WALLET:{customerId}` giá trị $+0.7491$ PCC.
   - **Điểm thưởng (Loyalty Points)**:
     - *Bút toán 1*: `DEBIT` tài khoản `SYS_LOYALTY_EXPENSE` giá trị $+85$ Points.
     - *Bút toán 2*: `CREDIT` tài khoản `USER_POINTS_WALLET:{customerId}` giá trị $+85$ Points.
3. **Kịch bản Đổi quà (Redemption Transaction)**:
   - *Bút toán 1*: `DEBIT` tài khoản `USER_POINTS_WALLET:{customerId}` giá trị $-500$ Points.
   - *Bút toán 2*: `CREDIT` tài khoản `PARTNER_REDEMPTION_POOL` giá trị $+500$ Points.
