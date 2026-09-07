# Green Mobility Platform

> **Đề tài Khóa luận tốt nghiệp**: Nền tảng gọi xe điện tích hợp hệ thống đo lường, báo cáo và khuyến khích giảm phát thải carbon  
> **Sinh viên thực hiện**:  
> - **Phạm Hà Anh Thư** — MSSV: 23521544  
> - **Nguyễn Minh Thiện** — MSSV: 23521484  
> **Cán bộ hướng dẫn**: ThS. Trần Thị Hồng Yến  
> **Đơn vị**: Trường Đại học Công nghệ Thông tin, Đại học Quốc gia TP. Hồ Chí Minh (UIT - ĐHQG TP.HCM)  
> **Thời gian thực hiện**: 07/09/2026 – 11/01/2027

---

## 📚 Bộ Tài liệu Đặc tả Kỹ thuật (Technical Documentation)

Dự án có đầy đủ tài liệu kiến trúc, database schema, API contracts và cẩm nang lập trình trong thư mục [`docs/`](./docs):

* 📐 **[docs/01_MASTER_ARCHITECTURE.md](./docs/01_MASTER_ARCHITECTURE.md)**: Kiến trúc tổng thể hệ thống, Modular Monolith Spring Boot, luồng sự kiện RabbitMQ, Polyglot Persistence và mô hình toán học tính phát thải $CO_2$ theo IPCC/Bộ TN&MT.
* ⚙️ **[docs/02_BACKEND_SPEC.md](./docs/02_BACKEND_SPEC.md)**: Thiết kế CSDL chi tiết (PostgreSQL DDL + PostGIS GIST index + MongoDB Telemetry), REST API OpenAPI contracts, WebSocket/STOMP topics, thuật toán Matching Engine (Redis GEO) và Sổ cái kép (Double-Entry Ledger).
* 📱 **[docs/03_MOBILE_SPEC.md](./docs/03_MOBILE_SPEC.md)**: Đặc tả kỹ thuật ứng dụng di động Flutter (Clean Architecture + BLoC), trải nghiệm Khách hàng (Bản đồ mượt mà, Impact Receipt, Ví Carbon) và Tài xế (Face Verification, Pop-up nhận cuốc 15s, Background GPS).
* 💻 **[docs/04_ADMIN_SPEC.md](./docs/04_ADMIN_SPEC.md)**: Đặc tả Web Admin Portal (Next.js App Router, Tailwind CSS, Shadcn UI), Dashboard CO2, duyệt KYC xe điện, cấu hình hệ số phát thải, bản đồ Replay hành trình GPS, giám sát gian lận ML (Isolation Forest) và AI Copilot Chatbot.
* 🤖 **[docs/05_AGENT_PLAYBOOK.md](./docs/05_AGENT_PLAYBOOK.md)**: Cẩm nang hướng dẫn cho AI Coding Agent với 6 nguyên tắc bất biến, lộ trình 8 Sprint, bộ Mock Data tọa độ TP.HCM và Prompt Templates chuẩn.

---

## 🏗️ Cấu trúc Monorepo

```text
green-mobility/
├── docs/                      # Tài liệu đặc tả kiến trúc & đề cương KLTN
├── backend/                   # Spring Boot 3.3+ (Java 21) Modular Monolith
│   └── src/main/java/com/greenmobility/
│       ├── common/            # DTOs, Exceptions, ApiResponse, Utils
│       ├── config/            # Security, WebSocket, Redis, RabbitMQ
│       └── modules/           # 8 Bounded Contexts
│           ├── identity/      # Quản lý tài khoản, JWT, RBAC
│           ├── drivervehicle/ # Hồ sơ tài xế, xe điện, KYC, Face Verification
│           ├── matching/      # Matching Engine (Redis GEO)
│           ├── trip/          # Quản lý cuốc xe, State Machine, Tracking
│           ├── carbon/        # Engine tính phát thải CO2 & Impact Receipt
│           ├── incentive/     # Sổ cái kép, Ví Carbon, Điểm thưởng, Quà tặng
│           ├── payment/       # VNPay, MoMo Sandbox
│           └── fraud/         # Giám sát gian lận GPS & Anomaly Detection
├── frontend-admin/            # Web Admin Portal Next.js 14+ (TypeScript + Tailwind)
├── mobile/                    # Ứng dụng di động Flutter (Customer App & Driver App)
│   ├── packages/              # Thư viện dùng chung (core_network, core_ui, core_map...)
│   └── apps/                  # customer_app & driver_app
└── docker-compose.yml         # Cụm hạ tầng Local (PostGIS, Redis, Mongo, RabbitMQ, MinIO)
```

---

## 🚀 Khởi chạy Hạ tầng Cục bộ (Local Development)

### 1. Khởi động toàn bộ CSDL và Message Broker
```bash
docker-compose up -d
```
Cụm dịch vụ bao gồm:
* **PostgreSQL + PostGIS**: `localhost:5432` (User: `green_admin`, DB: `greenmobility_db`)
* **Redis**: `localhost:6379`
* **MongoDB**: `localhost:27017` (DB: `greenmobility_telemetry`)
* **RabbitMQ Management Dashboard**: `http://localhost:15672` (User/Pass: `rabbit_admin` / `rabbit_password_2026`)
* **MinIO Console**: `http://localhost:9001` (User/Pass: `minio_admin` / `minio_password_2026`)

### 2. Khởi chạy Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```

### 3. Khởi chạy Web Admin (Next.js)
```bash
cd frontend-admin
npm install
npm run dev
```
Truy cập: `http://localhost:3000`
