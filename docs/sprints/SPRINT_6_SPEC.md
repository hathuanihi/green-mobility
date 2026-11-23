# Sprint 6 Specification: Multi-Currency Incentive Ledger, Carbon Wallet, Eco-Store & Vouchers

> **Tài liệu**: Đặc tả Kỹ thuật Chi tiết Sprint 6 (Sprint 6 Specification)  
> **Dự án**: Nền tảng gọi xe điện tích hợp hệ thống đo lường, báo cáo và khuyến khích giảm phát thải carbon (Green Mobility)  
> **Thời gian thực hiện theo Đề cương KLTN**: 23/11/2026 – 06/12/2026  
> **Mục tiêu**: Xây dựng Hệ thống Khuyến khích hoàn chỉnh trên nền tảng **Sổ cái Kế toán Kép Đa tiền tệ** (`VND`, `PCC`, `ECO_POINT`): tự động tích lũy Tín chỉ Carbon cá nhân (`PCC`) và Điểm thưởng Xanh (`EcoPoints`) cho Khách hàng & Tài xế theo cơ chế chống đếm trùng (70/30) qua Join-Barrier (`CarbonCalculated` + `PaymentSucceeded` + sạch `FraudAlert`); Cửa hàng Quà xanh (`Eco-Store`) đổi thưởng nguyên tử; áp dụng thật Voucher giảm giá vào luồng đặt chuyến đi với hạch toán trợ giá `SYS:PROMO_EXPENSE:VND`; quy đổi `PCC → VND` về Ví Xanh / Thu nhập tài xế; Cổng Kiểm toán Sổ cái Kép & Quản trị danh mục trên Web Admin; đồng bộ và hoàn thiện giao diện Mobile (`customer_app`, `driver_app`) theo Dark Mode; đồng thời xử lý triệt để toàn bộ nợ kỹ thuật & lỗi tồn đọng từ Sprint 4–5.  
> **Phiên bản**: 1.0.0 (Production Blueprint)  
> **Phụ thuộc**: Sprint 4 (Carbon Engine), Sprint 5 (Payment, Double-Entry Ledger, Outbox). **Bàn giao cho**: Sprint 7 (`incentive.event.credited`, `incentive.event.redeemed` phục vụ Leaderboard Redis ZSET & Gamification Badges).

---

## 0. Quy ước Bắt buộc (Invariants R1–R15)

> [!IMPORTANT]
> Mọi quy tắc dưới đây là **bất biến**. Vi phạm bất kỳ điều nào là lỗi Blocker và phải được kiểm chứng bằng cả Database Constraint/Trigger và Automated Test.

| # | Quy tắc | Lý do / Lỗi ẩn được ngăn chặn |
| :-- | :--- | :--- |
| **R1** | **Chuẩn hóa Đơn vị & Độ chính xác**: `1 PCC = 1 kg CO₂ = 1,000 g CO₂` (`scale = 4`, `BigDecimal`). `ECO_POINT` và `VND` dùng số nguyên (`scale = 0`). Tuyệt đối không dùng `double/float` trong tính toán tài chính hay sổ cái. | Khắc phục lỗi làm tròn `0.0000` trên cuốc ngắn khi dùng `NUMERIC(18,4)` và đồng bộ với `rewards.carbon_credit_cost`. |
| **R2** | **Đơn tiền tệ & Bất biến Sổ cái**: Mọi bút toán trong 1 `LedgerTransaction` bắt buộc dùng **cùng 1 `currency`** (kiểm tra ở Java **và** DB constraint trigger `trg_check_tx_single_currency`). Chuẩn hóa scale theo `LedgerCurrency` **trước** khi kiểm tra `amount > 0` và `Σ Debit = Σ Credit`. Không `UPDATE/DELETE` bảng `ledger_entries`/`ledger_transactions`. | Ngăn trộn lẫn tiền tệ hoặc lệch làm tròn giữa Java và DB trigger `trg_check_tx_balanced`. |
| **R3** | **Khóa tăng dần & Ví đóng băng**: Khóa `ledger_accounts` bằng `SELECT ... FOR UPDATE` theo `id` tăng dần. Chặn mọi giao dịch rút/tiêu trên tài khoản có `status = 'FROZEN'` (`409 WALLET_FROZEN`), trừ cờ `bypassFrozenCheck` dành riêng cho `ADMIN_REVERSAL` và `INCENTIVE_CLAWBACK`. | Chống deadlock, số dư âm và ngăn tẩu tán điểm khi ví đang bị phong tỏa gian lận. |
| **R4** | **Chống đếm trùng Carbon (Anti-Double-Counting)**: Tổng PCC phát hành cho 1 chuyến đi từ `SYS:CARBON_RESERVE:PCC` **bằng đúng 100%** lượng CO₂ thực giảm (`T`). Chia cho Khách hàng `C = floor(T × pcc_customer_share_ratio, 4)` (mặc định `0.70`) và Tài xế `D = T − C`. Bỏ qua mọi bút toán có giá trị `0`. | Đảm bảo tính khoa học & minh bạch ESG, tổng PCC lưu hành không vượt tổng CO₂ thực tế đã giảm. |
| **R5** | **Join-Barrier Idempotent**: Giải ngân thưởng chuyến đi chỉ xảy ra khi `carbon_ready = TRUE` **VÀ** `payment_ready = TRUE` (không phụ thuộc thứ tự event đến trước/sau) **VÀ** không có `FraudAlert` đang mở. Dòng `trip_incentive_awards` tạo bằng `INSERT ... ON CONFLICT (trip_id) DO NOTHING` rồi `SELECT ... FOR UPDATE`. Ledger dùng idempotency key cố định `INC:{tripId}:PCC`, `INC:{tripId}:PTS`, `CLAWBACK:{tripId}:*`. | Chống race condition khi `CarbonCalculatedEvent` và `PaymentSucceededEvent` về đồng thời hoặc bị RabbitMQ redelivery. |
| **R6** | **Hệ số Phát thải Chuẩn hóa Từ DB**: Áp dụng chính thức **Công văn số 11232/BNNMT-BĐKH ngày 30/09/2026** của Bộ Nông nghiệp và Môi trường (hệ số lưới điện VN năm 2024: **`618.70 gCO₂/kWh`**). Bảng `emission_factors` có `CHECK` constraint cưỡng chế công thức `EV = ROUND(SEC × EF_grid, 2)` và `Net = Baseline − EV`. Cả ước tính (`/trips/estimate`) và tính toán sau chuyến đều đọc từ DB. | Loại bỏ hoàn toàn tình trạng lệch 3 bộ số (`580` vs `722.1` vs `0.6552`) giữa ước tính, DB và Web. |
| **R7** | **Nguồn bù Trợ giá Voucher (`SYS:PROMO_EXPENSE:VND`)**: Hoa hồng nền tảng (`C`) và Thu nhập tài xế (`D`) luôn tính trên **giá cước gốc `fare_amount`**. Phần giảm giá (`discount_amount`) do nền tảng hạch toán chi phí từ `SYS:PROMO_EXPENSE:VND` bằng bút toán đối ứng cho mọi phương thức (kể cả `CASH` và chuyến giảm 100% `final_amount = 0`). | Bảo vệ thu nhập tài xế khi khách dùng voucher và đảm bảo kế toán $\Delta = 0$ ở mọi giao dịch. |
| **R8** | **Xác nhận Outbox Thật (Publisher Confirms)**: `OutboxPublisher` phải đợi `CorrelationData.Confirm` ACK thành công (và không bị RabbitMQ return) mới đánh dấu `SENT`. Bản ghi chuyển `FAILED` sau 10 lần lỗi sẽ **dừng poll tự động** và chỉ chạy lại khi Admin kích hoạt re-drive. | Ngăn mất sự kiện khi broker gián đoạn và sửa triệt để lỗi vòng lặp retry vô hạn ở Sprint 5. |
| **R9** | **Kho quà & Voucher Nguyên tử**: Khi đổi quà, kiểm tra Circuit-Breaker `redemptions_frozen = false`, khóa dòng `rewards` bằng `SELECT ... FOR UPDATE`, kiểm tra `quantity_available > 0` và `max_redemptions_per_user`, trừ kho và trừ sổ cái trong cùng DB transaction. | Chống bán lố tồn kho (oversell) dưới tải cao (kiểm chứng với 200 luồng đồng thời). |
| **R10** | **Quy đổi `PCC → VND` Nguyên tử 2 Giao dịch**: Khóa ví `WALLET:PCC:{user}` `FOR UPDATE` **trước** khi kiểm tra hạn mức tháng (`GMT+7`), sau đó ghi 2 giao dịch sổ cái độc lập (`PCC_CONVERT_BURN` trên `PCC` và `PCC_CONVERT_PAYOUT` trên `VND`) trong cùng `@Transactional`. `pcc_to_vnd_rate` được định nghĩa là **trợ giá marketing xanh** kèm trần quy đổi tháng. | Vừa giữ tuyệt đối quy tắc 1 giao dịch = 1 tiền tệ, vừa chống race vượt trần tháng. |
| **R11** | **Xử lý Trường hợp Biên Kế toán**: (1) Hoàn tiền một phần (`PARTIALLY_REFUNDED`) **không** thu hồi thưởng (chỉ ghi `audit_logs`); (2) Hoàn tiền toàn phần sau khi user đã tiêu/quy đổi PCC → chuyển `CLAWBACK_SHORTFALL_AUDIT`, **không** tự động trừ ví `VND`; (3) Voucher hết hạn trong lúc đang `RESERVED` cho chuyến đang chạy → vẫn cho hoàn thành và đánh dấu `USED`; (4) Fraud alert phát sinh sau khi đã `CREDITED` → chỉ xử lý qua `ADMIN_REVERSAL`. | Ngăn lỗi runtime bất ngờ và giữ minh bạch sổ cái khi tranh chấp phát sinh muộn. |
| **R12** | **Ranh giới Modular Monolith**: Tách lõi sổ cái đa tiền tệ vào `modules.ledger` và hạ tầng dùng chung (`Outbox`, `Idempotency`, `AuditLog`, `PlatformSettings`) vào `common.platform`. Các module `payment`, `incentive`, `carbon`, `trip`, `fraud` giao tiếp qua `*ModuleApi` và RabbitMQ Events; cấm gọi chéo Repository của module khác. | Kiến trúc sạch, không circular dependency, dễ tách microservice về sau. |
| **R13** | **Idempotency Bắt buộc trên Endpoint Ghi**: Đổi thưởng (`/redeem`), quy đổi PCC (`/convert-pcc`), hoàn tiền (`/refund`) bắt buộc header `Idempotency-Key` (`422 IDEMPOTENCY_KEY_REQUIRED` nếu thiếu, `422 IDEMPOTENCY_KEY_REUSED` nếu khác payload). | Chống double-click / retry mạng làm đổi quà hoặc quy đổi 2 lần. |
| **R14** | **RBAC & IDOR Chặt chẽ**: `ROLE_ADMIN` và `ROLE_FINANCE` có toàn quyền quản trị & ghi (đảo sổ cái, đóng băng ví, duyệt `HELD_FOR_FRAUD`, sửa settings, CRUD quà, re-drive outbox). `ROLE_OPERATOR` **chỉ được xem (`GET`)** — mọi thao tác ghi trả `403`. Endpoint `/incentive/**` chỉ trả dữ liệu thuộc sở hữu caller (`users.id`). | Phân tách nhiệm vụ vận hành vs tài chính, chống leo thang đặc quyền và IDOR. |
| **R15** | **Quản lý Ví Người dùng Nhất quán**: Tạo đủ bộ ví (`WALLET:VND`, `WALLET:PCC`, `WALLET:PTS`, và `EARN:VND` cho tài xế) ngay khi đăng ký (`AuthService.register`). Endpoint `GET` chỉ đọc (`readOnly = true`), tuyệt đối không `INSERT` ngầm trong giao dịch `readOnly`. | Khắc phục triệt để lỗi PostgreSQL `cannot execute INSERT in a read-only transaction` ở Sprint 5. |

---

## 1. Cơ sở Khoa học & Hệ số Phát thải Chuẩn hóa (2024 – CV 11232/BNNMT-BĐKH)

### 1.1. Trích dẫn Văn bản Chính thức
- **Hệ số phát thải lưới điện Việt Nam năm 2024**: **`0.6187 tCO₂/MWh` = `618.70 gCO₂/kWh`**  
  *Căn cứ pháp lý*: **Công văn số 11232/BNNMT-BĐKH** ban hành ngày **30/09/2026** bởi **Bộ Nông nghiệp và Môi trường** (Bộ Công Thương chủ trì nghiên cứu và tính toán), áp dụng làm hệ số chuẩn cho kiểm kê khí nhà kính Scope 2 và tính toán giảm phát thải/tín chỉ carbon tại Việt Nam.
- **Tham chiếu lịch sử (năm 2023)**: **`0.6592 tCO₂/MWh` = `659.20 gCO₂/kWh`** theo **Công văn số 1726/BĐKH-PTCBT** ngày **03/12/2024** của Cục Biến đổi khí hậu (Bộ TN&MT).

### 1.2. Ma trận Hệ số Phát thải 5 Cột Chuẩn hóa
Mọi bản ghi `emission_factors` trong PostgreSQL phải thỏa mãn ràng buộc ở mức DB (`CHECK`):
$$\text{EF}_{\text{EV}} = \text{ROUND}(\text{SEC} \times \text{EF}_{\text{grid}}, 2), \qquad \text{Net Saving} = \text{EF}_{\text{baseline}} - \text{EF}_{\text{EV}}$$

| Danh mục (`vehicle_category`) | Xe xăng đối chứng (`EF_baseline`) | Suất tiêu thụ điện (`SEC`) | Lưới điện VN 2024 (`EF_grid`) | Phát thải EV (`calculated_ev_factor`) | Giảm ròng (`net_co2_saving_per_km`) |
| :--- | ---: | ---: | ---: | ---: | ---: |
| `ELECTRIC_MOTORBIKE` | `68.50` $g/km$ | `0.0320` $kWh/km$ | `618.70` $g/kWh$ | `19.80` $g/km$ | **`48.70` $g/km$** |
| `ELECTRIC_CAR_4SEAT` | `152.00` $g/km$ | `0.1350` $kWh/km$ | `618.70` $g/kWh$ | `83.52` $g/km$ | **`68.48` $g/km$** |
| `ELECTRIC_CAR_7SEAT` | `210.00` $g/km$ | `0.1800` $kWh/km$ | `618.70` $g/kWh$ | `111.37` $g/km$ | **`98.63` $g/km$** |

- **Quy đổi sinh thái tương đương**:
  - Ngày cây xanh hấp thụ: `co2SavedGrams / 60.0` (làm tròn 2 chữ số).
  - Giờ thắp bóng LED 10W: `co2SavedGrams / (0.01 × EF_grid)` = `co2SavedGrams / 6.187` (làm tròn 2 chữ số).
  - Lần sạc đầy smartphone: `co2SavedGrams / 8.22` (làm tròn 1 chữ số).

### 1.3. Công thức Chia sẻ Tín chỉ Carbon (`PCC`) & Điểm thưởng (`EcoPoints`)
- **Tổng PCC tạo ra của chuyến đi**:
  $$T = \text{ROUND}\left(\frac{\Delta E_{\text{CO}_2}}{1000.0}, 4\right)$$
- **Chia PCC giữa Khách hàng & Tài xế** (`pcc_customer_share_ratio` mặc định `0.70`):
  - Nếu có tài xế (`driverUserId != null`):
    $$C = \text{FLOOR}(T \times \text{ratio}, 4), \qquad D = T - C$$
    *(Ví dụ $T = 0.4870\text{ PCC}$: $C = \text{FLOOR}(0.3409, 4) = 0.3409$, $D = 0.1461$, tổng $C + D = 0.4870 = T$. Mọi entry có giá trị `0` đều được bỏ qua khi ghi sổ).*
  - Nếu không xác định được `driverUserId`: $C = T, D = 0$.
- **EcoPoints**:
  - **Khách hàng**: $P_c = \left\lfloor \frac{\Delta E_{\text{CO}_2}}{\text{eco\_point\_grams\_per\_point}} \right\rfloor$ (mặc định chia `100.0`), nếu $\Delta E_{\text{CO}_2} > 0$ và $P_c = 0$ thì gán tối thiểu `1`.
  - **Tài xế**: `driver_eco_point_ratio` (mặc định `0.50`) được **tính trên `customerPoints` ($P_c$)** để đảm bảo tỷ lệ thưởng rõ ràng, không sai số làm tròn kép:
    $$P_d = \left\lfloor P_c \times \text{driver\_eco\_point\_ratio} \right\rfloor$$
    Nếu $P_c > 0$, `driver_eco_point_ratio > 0`, và `driverUserId != null` thì tối thiểu $P_d = 1$; ngược lại $P_d = 0$.

---

## 2. Mô hình Sổ cái Kép Đa tiền tệ & Bút toán Chuẩn

### 2.1. Danh mục Tài khoản Sổ cái (`ledger_accounts`)
Quy ước thống nhất toàn hệ thống: **Credit-Normal** (`balance = Σ Credit − Σ Debit`).

| `account_type` | Tiền tệ | `account_number` | `allow_negative` | Vai trò |
| :--- | :--- | :--- | :---: | :--- |
| `USER_GREEN_WALLET` | `VND` | `WALLET:VND:{userId}` | `false` | Ví tiền mặt nội bộ của Khách/Tài xế |
| `DRIVER_EARNINGS` | `VND` | `EARN:VND:{userId}` | `true` | Thu nhập khả dụng rút & đối trừ nợ tiền mặt |
| `PLATFORM_REVENUE` | `VND` | `SYS:REVENUE:VND` | `true` | Hoa hồng nền tảng |
| `GATEWAY_CLEARING` | `VND` | `SYS:GATEWAY_CLEARING:VND` | `true` | Tiền chờ đối soát tại VNPay/MoMo |
| `PAYOUT_CLEARING` | `VND` | `SYS:PAYOUT_CLEARING:VND` | `true` | Quỹ phong tỏa chờ chuyển khoản rút tiền |
| `EXTERNAL_BANK` | `VND` | `SYS:EXTERNAL_BANK:VND` | `true` | Đối ứng ngân hàng bên ngoài |
| **`PROMO_EXPENSE`** *(mới)* | `VND` | `SYS:PROMO_EXPENSE:VND` | `true` | Chi phí trợ giá voucher/khuyến mãi của nền tảng |
| **`CARBON_BUYBACK_POOL`** *(mới)* | `VND` | `SYS:CARBON_BUYBACK:VND` | `true` | Quỹ ngân sách marketing mua lại PCC (`PCC → VND`) |
| **`USER_CARBON_WALLET`** *(mới)* | `PCC` | `WALLET:PCC:{userId}` | `false` | Ví Tín chỉ Carbon cá nhân (Khách hàng & Tài xế) |
| **`SYSTEM_CARBON_RESERVE`** *(mới)* | `PCC` | `SYS:CARBON_RESERVE:PCC` | `true` | Nguồn phát hành PCC từ giảm phát thải thực tế |
| **`SYSTEM_CARBON_RETIRED`** *(mới)* | `PCC` | `SYS:CARBON_RETIRED:PCC` | `true` | Tài khoản tiêu hủy/bù trừ vĩnh viễn PCC (trồng cây, quy đổi VND) |
| **`PARTNER_REDEMPTION_POOL`** *(mới)* | `PCC` | `SYS:PARTNER_POOL:PCC` | `true` | Quỹ đối soát đối tác đổi quà bằng PCC |
| **`USER_POINTS_WALLET`** *(mới)* | `ECO_POINT` | `WALLET:PTS:{userId}` | `false` | Ví Điểm thưởng EcoPoints (Khách hàng & Tài xế) |
| **`SYSTEM_LOYALTY_RESERVE`** *(mới)* | `ECO_POINT` | `SYS:LOYALTY_RESERVE:ECO_POINT` | `true` | Nguồn phát hành EcoPoints |
| **`PARTNER_REDEMPTION_POOL`** *(mới)* | `ECO_POINT` | `SYS:PARTNER_POOL:ECO_POINT` | `true` | Quỹ đối soát voucher/quà đối tác bằng EcoPoints |
| **`SYSTEM_BURN_ACCOUNT`** *(mới)* | `ECO_POINT` | `SYS:ECO_BURN:ECO_POINT` | `true` | Tài khoản tiêu hủy EcoPoints (trồng cây/hết hạn) |

### 2.2. Bảng Quy tắc Bút toán Đầy đủ (Sprint 5 + Sprint 6)

Ký hiệu thanh toán chuyến đi: `F_gross` = `fare_amount`, `V` = `discount_amount` ($\ge 0$), `F_net` = `final_amount` $= F_{\text{gross}} - V$.  
Hoa hồng `C = round(F_gross × commission_rate)` và Thu nhập tài xế `D = F_gross − C` **luôn tính trên `F_gross`**.

| Nghiệp vụ | `transaction_type` | Tiền tệ | Bút toán (Debit → Credit) |
| :--- | :--- | :--- | :--- |
| Thanh toán Ví Xanh (`F_net > 0`) | `TRIP_PAYMENT_WALLET` | `VND` | Dr `WALLET:VND:{cust}` `F_net` + [nếu `V>0`: Dr `SYS:PROMO_EXPENSE:VND` `V`] / Cr `EARN:VND:{drv}` `D` + Cr `SYS:REVENUE:VND` `C` |
| Thanh toán Cổng (`F_net > 0`) | `TRIP_PAYMENT_GATEWAY` | `VND` | Dr `SYS:GATEWAY_CLEARING:VND` `F_net` + [nếu `V>0`: Dr `SYS:PROMO_EXPENSE:VND` `V`] / Cr `EARN:VND:{drv}` `D` + Cr `SYS:REVENUE:VND` `C` |
| Thanh toán Tiền mặt (`F_net > 0`) | `TRIP_PAYMENT_CASH` + [`VOUCHER_SUBSIDY`] | `VND` | Tx 1: Dr `EARN:VND:{drv}` `C` / Cr `SYS:REVENUE:VND` `C`<br/>Tx 2 (nếu `V > 0`): Dr `SYS:PROMO_EXPENSE:VND` `V` / Cr `EARN:VND:{drv}` `V` |
| Chuyến giảm 100% (`F_net = 0, V > 0`) | `VOUCHER_SUBSIDY` | `VND` | Dr `SYS:PROMO_EXPENSE:VND` `V` / Cr `EARN:VND:{drv}` `D` + Cr `SYS:REVENUE:VND` `C` |
| Thưởng PCC sau chuyến (70/30) | `TRIP_CARBON_REWARD` | `PCC` | Dr `SYS:CARBON_RESERVE:PCC` `T` / Cr `WALLET:PCC:{cust}` `C` + [nếu `D>0`: Cr `WALLET:PCC:{drv}` `D`] |
| Thưởng EcoPoints sau chuyến | `TRIP_POINTS_REWARD` | `ECO_POINT` | Dr `SYS:LOYALTY_RESERVE:ECO_POINT` `Pc+Pd` / Cr `WALLET:PTS:{cust}` `Pc` + [nếu `Pd>0`: Cr `WALLET:PTS:{drv}` `Pd`] |
| Đổi quà bằng PCC | `REWARD_REDEEM_PCC` | `PCC` | Dr `WALLET:PCC:{user}` `X` / Cr `SYS:CARBON_RETIRED:PCC` (`TREE_PLANTING`) hoặc `SYS:PARTNER_POOL:PCC` `X` |
| Đổi quà bằng EcoPoints | `REWARD_REDEEM_POINTS` | `ECO_POINT` | Dr `WALLET:PTS:{user}` `Y` / Cr `SYS:ECO_BURN:ECO_POINT` (`TREE_PLANTING`) hoặc `SYS:PARTNER_POOL:ECO_POINT` `Y` |
| Hủy đổi quà (`AVAILABLE`) | `REWARD_CANCEL_REVERSAL` | `PCC` / `ECO_POINT` | Đảo đối ứng từ `SYS:*_POOL` → `WALLET:*:{user}` (`reversal_of` trỏ tx gốc) |
| Quy đổi PCC → VND (Tx 1) | `PCC_CONVERT_BURN` | `PCC` | Dr `WALLET:PCC:{user}` `X` / Cr `SYS:CARBON_RETIRED:PCC` `X` |
| Quy đổi PCC → VND (Tx 2) | `PCC_CONVERT_PAYOUT` | `VND` | Dr `SYS:CARBON_BUYBACK:VND` `Vnd` / Cr `EARN:VND:{user}` (Driver) hoặc `WALLET:VND:{user}` (Customer) `Vnd` |
| Thu hồi thưởng (Refund toàn phần) | `INCENTIVE_CLAWBACK` | `PCC` / `ECO_POINT` | Dr `WALLET:*:{cust/drv}` / Cr `SYS:*_RESERVE` (nếu thiếu số dư → `CLAWBACK_SHORTFALL_AUDIT`) |
| Admin Đảo giao dịch | `ADMIN_REVERSAL` | Cùng tiền tệ tx gốc | Đảo ngược toàn bộ DEBIT ↔ CREDIT của giao dịch gốc (`reversal_of = originalTxId`) |

---

## 3. Cấu hình Tham số (`platform_settings`)

| Key | Mặc định | Giới hạn hợp lệ | Ý nghĩa & Ghi chú |
| :--- | :--- | :--- | :--- |
| `pcc_grams_per_unit` | `1000` | `100 – 1000000` | Số gam CO₂ cho 1 PCC (`1000` = 1 kg CO₂) |
| `pcc_customer_share_ratio` | `0.70` | `0.00 – 1.00` | Tỷ lệ chia PCC cho Khách hàng (Tài xế nhận `1 - ratio`) |
| `eco_point_grams_per_point` | `100` | `1 – 10000` | Số gam CO₂ quy đổi 1 EcoPoint cho Khách hàng |
| `driver_eco_point_ratio` | `0.50` | `0.00 – 2.00` | Tỷ lệ EcoPoints của Tài xế tính trên `customerPoints` |
| `pcc_to_vnd_rate` | `500` | `100 – 50000` | **Trợ giá marketing xanh**: 500đ / 1 PCC (= 500.000đ/tấn CO₂), kèm trần tháng |
| `pcc_convert_min` | `0.5000` | `>= 0.0100` | Số PCC tối thiểu mỗi lần quy đổi sang VND |
| `pcc_convert_monthly_cap` | `100.0000` | `>= pcc_convert_min` | Trần quy đổi tối đa PCC/người/tháng theo `Asia/Ho_Chi_Minh` (tối đa 50.000đ/tháng ở mức mặc định) |
| `redemptions_frozen` | `false` | `true / false` | Circuit-Breaker khóa đổi thưởng & quy đổi khi phát hiện lệch sổ cái ($\Delta \ne 0$) |
| `incentive_reconcile_minutes` | `5` | `1 – 1440` | Ngưỡng phút để `IncentiveReconciliationJob` tự đối soát award bị kẹt |

---

## 4. Kiến trúc Cơ sở Dữ liệu (`V6__sprint6_incentive_ledger.sql`)

1. **`ledger_accounts`**: thêm `frozen_reason TEXT`, `frozen_by UUID`, `frozen_at TIMESTAMPTZ`, ràng buộc `chk_ledger_account_status CHECK (status IN ('ACTIVE','FROZEN','CLOSED'))`.
2. **DB Constraint Trigger `trg_check_tx_single_currency`**: kiểm tra sau `INSERT ON ledger_entries` (`DEFERRABLE INITIALLY DEFERRED`) rằng mọi bút toán trong 1 `transaction_id` thuộc **cùng 1 `currency`**.
3. **`emission_factors`**: thêm 2 `CHECK` constraint cho `calculated_ev_factor_gco2_km` và `net_co2_saving_per_km`, đóng hiệu lực dòng `580.00` cũ, thêm 3 dòng chuẩn 2024 (`618.70 g/kWh`).
4. **`rewards` & `reward_redemptions`**: nâng cấp `carbon_credit_cost` và `carbon_spent` sang `NUMERIC(18,4)`; thêm metadata voucher (`discount_type`, `discount_value`, `max_discount_vnd`, `min_fare_vnd`, `validity_days`, `max_redemptions_per_user`) và lifecycle redemption (`pcc_ledger_tx_id`, `points_ledger_tx_id`, `reversal_pcc_tx_id`, `reversal_points_tx_id`, `applied_trip_id`, `discount_applied_vnd`, `expires_at`, `used_at`, `cancelled_at`, `cancel_reason`, `certificate_metadata JSONB`, `version`).
5. **`trip_incentive_awards`**: bảng Join-Barrier với `UNIQUE(trip_id)`, `carbon_ready`, `payment_ready`, `status` (`PENDING_CONDITIONS | HELD_FOR_FRAUD | CREDITED | VOIDED_FRAUD | CLAWED_BACK | CLAWBACK_SHORTFALL_AUDIT`), tham chiếu tx ledger, thông tin duyệt fraud của Admin.
6. **`carbon_conversions`**: lưu giao dịch `PCC → VND` (role, tài khoản đích, số PCC, tỷ giá, số VND, 2 ledger tx IDs).
7. **`payments` & `trips`**: thêm `gross_fare_vnd`, `discount_vnd` vào `payments`; thêm `voucher_redemption_id`, `voucher_code` vào `trips`.

---

## 5. Đặc tả REST API & Mã lỗi

Base: `/api/v1` — Mọi response bọc `ApiResponse<T>`.

### 5.1. Khách hàng & Tài xế (`/incentive/**` — Authenticated)
- **`GET /incentive/wallet`**: Số dư `pccBalance`, `ecoPointBalance`, `lifetimeCo2SavedKg`, `pccWalletStatus`, `pointsWalletStatus`, `frozenReason`, `pccToVndRate`, `pccConvertMin`, `pccConvertMonthlyCap`, `pccConvertedThisMonth`, `pccRemainingMonthlyCap`, `redemptionsFrozen`.
- **`GET /incentive/wallet/transactions?currency=ALL|PCC|ECO_POINT&type=&from=&to=&limit=20&cursor=`**: Lịch sử bút toán sổ cái (cursor pagination).
- **`POST /incentive/wallet/convert-pcc`** *(Header `Idempotency-Key` bắt buộc)*: `{ "pccAmount": 2.5000 }` → trả `CarbonConversionResponse`.
- **`GET /incentive/wallet/conversions?limit=20&cursor=`**: Lịch sử quy đổi PCC → VND.
- **`GET /incentive/rewards?category=&affordableOnly=`**: Danh sách quà tặng đang kích hoạt trong Eco-Store.
- **`GET /incentive/rewards/{id}`**: Chi tiết 1 phần quà.
- **`POST /incentive/rewards/{id}/redeem`** *(Header `Idempotency-Key` bắt buộc)*: Đổi quà nguyên tử.
- **`GET /incentive/redemptions?status=&category=&limit=20&cursor=`**: Kho quà/voucher của tôi.
- **`POST /incentive/redemptions/{id}/cancel`** *(Header `Idempotency-Key` bắt buộc)*: Hủy voucher đang `AVAILABLE` → hoàn kho + đảo sổ cái.
- **`GET /incentive/vouchers/available?fareVnd=`**: Danh sách voucher hợp lệ để chọn khi đặt chuyến (`Trip` screen).

### 5.2. Tích hợp Voucher vào Đặt chuyến (`/trips/**`)
- **`POST /trips/estimate`**: nhận thêm `voucherCode` (optional) → trả `fareAmountVnd`, `discountAmountVnd`, `finalAmountVnd`, `appliedVoucherCode`, `voucherError` (nếu mã không hợp lệ/chưa đủ tối thiểu, không chặn estimate nhưng báo rõ).
- **`POST /trips/request`**: nhận thêm `voucherCode` (optional) → reserve voucher nguyên tử (`AVAILABLE → RESERVED`), trừ `discountAmount`, gán `finalAmount`. Nếu hủy chuyến trước `IN_TRIP` → trả về `AVAILABLE` (hoặc `EXPIRED` nếu đã quá `expiresAt`).

### 5.3. Quản trị Web Admin (`/admin/**`)
**Phân quyền**: `ROLE_ADMIN` & `ROLE_FINANCE` có toàn quyền `GET` + ghi; `ROLE_OPERATOR` **chỉ `GET`** (`POST`/`PUT`/`PATCH`/`DELETE` trả `403 FORBIDDEN`).
- **Eco-Store & Khuyến khích (`/admin/incentives/**`)**:
  - `GET /rewards`, `POST /rewards`, `PUT /rewards/{id}`, `PATCH /rewards/{id}/status`, `POST /rewards/{id}/restock`
  - `GET /redemptions`, `POST /redemptions/{id}/mark-used`, `POST /redemptions/{id}/cancel`
  - `GET /held-awards`, `POST /held-awards/{id}/approve`, `POST /held-awards/{id}/void`
  - `GET /settings`, `PUT /settings`
  - `POST /wallets/{userId}/freeze`, `POST /wallets/{userId}/unfreeze`
- **Kiểm toán Sổ cái (`/admin/ledger/**`)**:
  - `GET /audit` (báo cáo Zero-Sum đa tiền tệ `VND`, `PCC`, `ECO_POINT` + trạng thái Circuit-Breaker)
  - `GET /transactions?currency=&type=&txCode=&userId=&tripId=&page=0&size=20`
  - `GET /transactions/{id}` (kèm danh sách `entries` Debit/Credit)
  - `POST /transactions/{id}/reverse` *(Header `Idempotency-Key` bắt buộc, `reason` ≥ 10 ký tự)*
  - `POST /circuit-breaker` (`{ "frozen": true|false, "reason": "..." }`)
- **Outbox Re-drive (`/admin/outbox/**`)**:
  - `GET /failed`
  - `POST /{id}/redrive`, `POST /redrive-all`

### 5.4. Bảng Mã lỗi Nghiệp vụ Sprint 6
| HTTP | Code | Tình huống |
| :---: | :--- | :--- |
| 400 | `VOUCHER_INVALID` / `VOUCHER_MIN_FARE_NOT_MET` / `LEDGER_INVALID_CURRENCY` / `LEDGER_UNBALANCED` | Voucher không hợp lệ/chưa đạt cước tối thiểu; sai tiền tệ hoặc mất cân bằng sổ cái |
| 403 | `FORBIDDEN` | `ROLE_OPERATOR` gọi API ghi hoặc truy cập chéo |
| 404 | `REWARD_NOT_FOUND` / `REDEMPTION_NOT_FOUND` / `VOUCHER_NOT_FOUND` / `LEDGER_TX_NOT_FOUND` | Không tìm thấy bản ghi |
| 409 | `WALLET_FROZEN` / `WALLET_INSUFFICIENT_BALANCE` / `REWARD_OUT_OF_STOCK` / `REWARD_LIMIT_EXCEEDED` / `REDEMPTION_FROZEN_CIRCUIT_BREAKER` / `PCC_CONVERT_LIMIT_EXCEEDED` / `VOUCHER_EXPIRED` / `VOUCHER_ALREADY_USED` / `LEDGER_ALREADY_REVERSED` | Vi phạm số dư, tồn kho, trần tháng, ví đóng băng, circuit-breaker, đảo 2 lần |
| 422 | `IDEMPOTENCY_KEY_REQUIRED` / `IDEMPOTENCY_KEY_REUSED` | Thiếu hoặc dùng lại `Idempotency-Key` khác payload |

---

## 6. Kiến trúc RabbitMQ, DLQ & Đối soát Tự động

- **Queues mới + DLQ (bind về `dlx.greenmobility`)**:
  - `q.trip.status.completed` → DLQ `q.trip.status.completed.dlq`
  - `q.carbon.calculated` (`carbon.event.calculated`) → DLQ `q.carbon.calculated.dlq`
  - `q.payment.succeeded.incentive` (`payment.event.succeeded`) → DLQ `q.payment.succeeded.incentive.dlq`
  - `q.payment.refunded.incentive` (`payment.event.refunded`) → DLQ `q.payment.refunded.incentive.dlq`
  - `q.incentive.events` (`incentive.event.credited`, `incentive.event.redeemed`) — sẵn sàng cho Sprint 7.
- **Outbox Publisher Confirm**: đợi `Confirm` ACK + kiểm tra không bị return trước khi đánh dấu `SENT`; dừng poll các bản ghi `FAILED`; hỗ trợ Admin re-drive.
- **Scheduled Jobs (`@SchedulerLock`)**:
  - `RedemptionExpiryJob` (15 phút): chuyển `AVAILABLE` quá `expires_at` sang `EXPIRED`.
  - `IncentiveReconciliationJob` (5 phút): quét các `trip_incentive_awards` đang `PENDING_CONDITIONS` quá `incentive_reconcile_minutes` hoặc các chuyến `COMPLETED` + `PAID` đã có `trip_impact_receipts` nhưng chưa có award → tự đối soát và giải ngân.
  - `LedgerIntegrityJob` (02:00 GMT+7): kiểm tra Zero-Sum cả 3 tiền tệ, tự bật `redemptions_frozen = true` nếu phát hiện bất thường.

---

## 7. Definition of Done (Đo lường được)

Sprint 6 được nghiệm thu khi đạt đồng thời toàn bộ 4 điều kiện:
1. **24/24 Tiêu chí Chấp thuận (`AC-01`..`AC-24`) PASS** qua bộ kiểm thử tự động Unit + Testcontainers Integration Test (PostgreSQL/PostGIS + RabbitMQ).
2. **Backend (`mvn test`)**: 100% test suite pass (0 failures, 0 errors, 0 skipped Testcontainers).
3. **Web Admin (`npm run lint && npx tsc --noEmit && npm run build`)**: 0 ESLint errors, 0 TypeScript errors, production build thành công.
4. **Mobile (`flutter analyze && flutter test`)**: 0 analyzer issues, 100% tests pass trên `core_model`, `core_network`, `core_ui`, `customer_app`, `driver_app`.
5. **Kiểm toán Sổ cái (`GET /api/v1/admin/ledger/audit`)**: `isBalanced = true` và `Δ = 0.0000` cho cả 3 loại tiền `VND`, `PCC`, `ECO_POINT` trên dữ liệu đã migrate & seed demo.

---

## 8. Bảng 24 Tiêu chí Chấp thuận (Acceptance Criteria)

| Mã AC | Tiêu chí | Kết quả mong đợi |
| :---: | :--- | :--- |
| **AC-01** | Hệ số phát thải 2024 CV 11232/BNNMT-BĐKH | Bảng `emission_factors` dùng `618.70 g/kWh`, thỏa mãn DB `CHECK` 5 cột (`48.70 / 68.48 / 98.63`), ước tính & tính sau chuyến đọc cùng nguồn DB. |
| **AC-02** | Sổ cái đa tiền tệ tách biệt | DB trigger `trg_check_tx_single_currency` và `LedgerService` chặn trộn tiền tệ, kiểm tra cân bằng sau scale (`VND=0, PCC=4, ECO_POINT=0`). |
| **AC-03** | Bất biến sổ cái DB | `UPDATE`/`DELETE` trên `ledger_entries`/`ledger_transactions` bị DB trigger từ chối 100%. |
| **AC-04** | Join-Barrier thứ tự Carbon trước Payment | Event Carbon đến trước → `PENDING_CONDITIONS` (`carbon_ready=true`); Payment đến sau → `CREDITED` đủ PCC & EcoPoints. |
| **AC-05** | Join-Barrier thứ tự Payment trước Carbon | Event Payment đến trước → `PENDING_CONDITIONS` (`payment_ready=true`); Carbon đến sau → `CREDITED` chính xác, idempotent khi lặp event. |
| **AC-06** | Chia PCC 70/30 & EcoPoints không đếm trùng | `C = floor(T*0.7, 4)`, `D = T - C`, `C + D = T`, bỏ qua entry `0`; `driverPoints = floor(customerPoints * 0.5)` (min 1 nếu > 0). |
| **AC-07** | Tạm giữ khi có Fraud Alert | Chuyến có `FraudAlert` `PENDING`/`CONFIRMED` → award vào trạng thái `HELD_FOR_FRAUD`, chưa cộng ví. |
| **AC-08** | Admin duyệt / hủy `HELD_FOR_FRAUD` | Approve → giải ngân vào ví + `audit_logs`; Void → `VOIDED_FRAUD` không cộng điểm. |
| **AC-09** | Hoàn tiền toàn phần vs một phần | Refund toàn phần → `INCENTIVE_CLAWBACK`; thiếu số dư (đã tiêu/đổi VND) → `CLAWBACK_SHORTFALL_AUDIT`, không trừ VND; Refund 1 phần → không thu hồi, chỉ ghi `audit_logs`. |
| **AC-10** | Fraud alert đến muộn sau khi đã `CREDITED` | Không tự clawback; Admin xử lý qua `ADMIN_REVERSAL` + đóng băng ví. |
| **AC-11** | Đổi quà Eco-Store nguyên tử (200 luồng) | 200 luồng tranh mua 50 quà → đúng 50 thành công, 0 oversell, 0 âm ví, 0 deadlock, `Δ = 0`. |
| **AC-12** | Hủy đổi quà `AVAILABLE` | Trả `quantity_available + 1`, ghi `REWARD_CANCEL_REVERSAL` hoàn lại `PCC`/`ECO_POINT` đúng số dư. |
| **AC-13** | Áp dụng Voucher `VOUCHER_RIDE` khi đặt chuyến | `estimate` & `request` trừ `discount_amount`, reserve voucher; hủy trước `IN_TRIP` → trả voucher; hoàn thành → `USED` (kể cả vừa quá `expires_at` khi đang `RESERVED`). |
| **AC-14** | Hạch toán trợ giá Voucher `SYS:PROMO_EXPENSE:VND` | Cả 4 phương thức (kể cả `CASH` và giảm 100% `final_amount=0`) tính hoa hồng & thu nhập tài xế trên `fare_amount`, `SYS:PROMO_EXPENSE:VND` bù `discount_amount`, giảm 100% vẫn kích hoạt thưởng Carbon. |
| **AC-15** | Quy đổi `PCC → VND` nguyên tử & trần tháng | Ghi 2 tx (`PCC_CONVERT_BURN` + `PCC_CONVERT_PAYOUT`) vào `USER_GREEN_WALLET` (Khách) hoặc `DRIVER_EARNINGS` (Tài xế); chặn vượt `pcc_convert_monthly_cap` khi chạy đồng thời. |
| **AC-16** | Đóng băng Ví Carbon & `WALLET_FROZEN` | Ví bị Admin đóng băng (từ `/fraud-monitor` hoặc `/ledger-audit`) bị chặn đổi quà/quy đổi `409 WALLET_FROZEN`. |
| **AC-17** | Circuit-Breaker khi lệch sổ cái | Khi `redemptions_frozen = true` (hoặc audit phát hiện lệch) → mọi lệnh đổi quà/quy đổi trả `409 REDEMPTION_FROZEN_CIRCUIT_BREAKER`. |
| **AC-18** | Admin Đảo giao dịch `ADMIN_REVERSAL` | Đảo giao dịch thành công kèm lý do ≥ 10 ký tự & `audit_logs`; chặn đảo lần 2 (`409 LEDGER_ALREADY_REVERSED`). |
| **AC-19** | Outbox Publisher Confirm & Admin Re-drive | `OutboxPublisher` chỉ đánh dấu `SENT` sau broker ACK; không retry vô hạn dòng `FAILED`; `/admin/outbox/{id}/redrive` đẩy lại thành công. |
| **AC-20** | `IncentiveReconciliationJob` | Tự phát hiện & giải ngân award bị kẹt `PENDING_CONDITIONS` quá ngưỡng phút cấu hình. |
| **AC-21** | RBAC & IDOR & Idempotency-Key | `OPERATOR` gọi API ghi → `403`; user A xem ví/redemption/hủy của B → `403/404`; thiếu `Idempotency-Key` → `422`. |
| **AC-22** | Web Admin Sprint 6 & dọn nợ Sprint 4–5 | `/incentives/catalog`, `/incentives/ledger-audit`, `/eco/certificate/[slug]` thật, `/emissions`/`/finance`/`/payments`/`/withdrawals` không còn mock; `lint`, `tsc`, `build` xanh. |
| **AC-23** | Mobile Customer & Driver End-to-End | 4-tab `BottomNavigationBar` Dark Mode hoạt động; API Sprint 5 sửa đúng endpoint; áp voucher đặt xe → thanh toán → Impact Receipt thật → Ví Carbon & Eco-Store & Quy đổi PCC; `flutter analyze && flutter test` xanh. |
| **AC-24** | Seed Demo & Kiểm toán Tổng thể | Chạy `seed_sprint6_demo_data.py` + `GET /admin/ledger/audit` trả về `isBalanced = true`, `Δ = 0.0000` cho `VND`, `PCC`, `ECO_POINT`. |
