# Báo cáo Tiến độ Dự án Green Mobility — Sprint 2

> **Dự án**: Nền tảng gọi xe điện tích hợp hệ thống đo lường, báo cáo và khuyến khích giảm phát thải carbon (Green Mobility)  
> **Giai đoạn**: **Sprint 2 — Ride Booking & Real-time Driver Matching Engine**  
> **Trạng thái tổng thể**: **HOÀN THÀNH 100% CÁC HẠNG MỤC SPRINT 2**  
> **Thời gian thực hiện**: 05/10/2026 – 18/10/2026  
> **Ngày báo cáo**: 05/10/2026  
> **Tài liệu đặc tả tham chiếu**: [SPRINT_2_SPEC.md](./SPRINT_2_SPEC.md)

---

## I. Tổng quan Tiến độ Sprint 2

Sprint 2 kế thừa toàn bộ nền tảng Định danh, Hồ sơ tài xế xe điện và Xác thực khuôn mặt vào ca của Sprint 1, kích hoạt luồng nghiệp vụ trung tâm và cốt lõi nhất của toàn hệ sinh thái Green Mobility: **Kết nối Khách hàng có nhu cầu di chuyển xanh với Đối tác Tài xế xe điện gần nhất theo thời gian thực**.

Trọng tâm triển khai trong Sprint 2 bao gồm 4 trụ cột kỹ thuật chính:
1. **Dự toán Lộ trình & Lượng $CO_2$ Giảm Tức thì**: Tích hợp thuật toán định tuyến và công thức kiểm kê khí nhà kính theo chuẩn IPCC, tính toán chính xác lượng phát thải giảm được so với phương tiện xăng tương đương trước khi khách bấm đặt xe.
2. **Thuật toán Ghép cặp Tài xế Đa Bán kính (EV Matching Engine)**: Quét tài xế qua cấu trúc không gian `Redis GEO`, mở rộng linh hoạt theo mô hình bậc thang 3 tầng (Tier 1: 1.5 km $\rightarrow$ Tier 2: 3.0 km $\rightarrow$ Tier 3: 5.0 km), kết hợp bộ lọc an toàn dung lượng pin xe điện ($B(d) \ge 20\%$) và hàm chấm điểm tối ưu $S(d)$.
3. **Cơ chế Phân phối Đơn Phân tán & Chống Race Condition**: Ứng dụng **Redisson Distributed Lock** (`lock:driver:{driverId}`) có TTL 15 giây kết hợp hàng đợi sự kiện **RabbitMQ Topic Exchange**, loại bỏ triệt để hiện tượng 2 cuốc xe cùng điều phối cho một tài xế.
4. **Hệ thống Giao tiếp Thời gian thực WebSocket STOMP**: Đẩy thông báo điều phối tức thời trong 15 giây đếm ngược đến Driver App, đồng thời cập nhật trạng thái cuốc xe cho Customer App và Web Admin Portal không độ trễ.

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Khách hàng (Customer App)
    participant TripAPI as Trip Service (Spring Boot)
    participant Redis as Redis Cache & GEO
    participant Engine as Matching Engine
    participant Rabbit as RabbitMQ
    participant WS as WebSocket STOMP Broker
    actor Driver as Tài xế (Driver App)

    Customer->>TripAPI: POST /trips/estimate (Điểm đón, Điểm trả, Loại xe điện)
    TripAPI-->>Customer: Cước phí, Quãng đường, Thời gian & Lượng CO2 giảm (IPCC)
    
    Customer->>TripAPI: POST /trips/request (Tạo yêu cầu đặt xe)
    TripAPI->>TripAPI: Lưu DB trạng thái SEARCHING
    TripAPI->>Rabbit: Publish event "trip.event.requested"
    TripAPI-->>Customer: HTTP 201 Created (tripId, status: SEARCHING)
    
    Rabbit->>Engine: Consume "trip.event.requested"
    Engine->>Redis: GEOSEARCH drivers:geo:available:{type} (Bán kính Tier 1, 2, 3)
    Redis-->>Engine: Danh sách ứng viên (candidate driverIds + tọa độ)
    
    Engine->>Engine: Tính điểm Matching Score S(d) & Lọc Pin xe >= 20%
    Engine->>Redis: Redisson Lock "lock:driver:{driverId}" (TTL 15s)
    
    alt Khóa thành công (Tài xế đang sẵn sàng)
        Engine->>WS: Push /user/queue/ride-dispatch (Lộ trình, Thu nhập, 15s đếm ngược)
        WS->>Driver: Pop-up nhận cuốc 15s (Rung & Chuông cảnh báo)
        
        alt Tài xế bấm "Chấp nhận" (trong 15s)
            Driver->>TripAPI: POST /driver/trips/{tripId}/accept
            TripAPI->>TripAPI: Cập nhật status MATCHED, driver_id, vehicle_id
            TripAPI->>Redis: Xóa driver khỏi Redis GEO available
            TripAPI->>Redis: Giải phóng Redisson Lock
            TripAPI->>WS: Push /topic/trip/{tripId} (status: MATCHED, driverInfo)
            WS->>Customer: Màn hình chuyển sang Đã tìm thấy tài xế!
        else Tài xế bấm "Từ chối" hoặc Hết 15s Timeout
            Driver->>TripAPI: POST /driver/trips/{tripId}/decline
            TripAPI->>Redis: Lưu Cooldown 60s & Giải phóng Redisson Lock
            Engine->>Engine: Chuyển sang tài xế có điểm số cao kế tiếp
        end
    else Khóa thất bại (Đang phân phối cuốc khác)
        Engine->>Engine: Bỏ qua, xét tài xế tiếp theo
    end
```

---

## II. Bảng Chi tiết Các Tính Năng Đã Hoàn Thành

### 1. Phân hệ Backend (Spring Boot 3.3.3 + PostgreSQL/PostGIS + Redis GEO & Redisson + RabbitMQ + WebSocket STOMP)

| STT | Tính năng / Thành phần | Chi tiết Kỹ thuật đã triển khai | Trạng thái |
| :---: | :--- | :--- | :---: |
| **1.1** | **Mô hình Vòng đời Chuyến xe & State Machine** | Hiện thực máy trạng thái chuẩn cho bảng `trips`: `REQUESTED` $\rightarrow$ `SEARCHING` $\rightarrow$ `MATCHED` $\rightarrow$ `CANCELLED` (và dự phòng `DRIVER_ARRIVING`, `ARRIVED`, `IN_TRIP`, `COMPLETED` cho Sprint 3). Ràng buộc không gian PostGIS `geometry(Point, 4326)`. | **ĐÃ HOÀN THÀNH** |
| **1.2** | **Bộ máy Ước tính Cước phí & CO2 (`CarbonEstimateService`)** | Áp dụng công thức kiểm kê IPCC: Xe máy điện giảm thuần **$50.45\,\text{gCO}_2/\text{km}$**, Ô tô điện giảm **$60.30\,\text{gCO}_2/\text{km}$**. Quy đổi trực quan số ngày cây xanh thành phố hấp thụ ($60.0\,\text{g/ngày}$) và giờ thắp sáng đèn LED 10W ($7.221\,\text{g/giờ}$). | **ĐÃ HOÀN THÀNH** |
| **1.3** | **Quản lý Vị trí Xe điện Trực tuyến trên Redis GEO** | Cài đặt `DriverGeoRedisRepository`: lệnh `GEOADD` lưu tọa độ theo phương tiện (`ELECTRIC_MOTORBIKE`, `ELECTRIC_CAR_4SEAT`), `HSET` lưu metadata pin và đánh giá sao, `GEOSEARCH` quét ứng viên bán kính cực nhanh ($< 3\text{ms}$). | **ĐÃ HOÀN THÀNH** |
| **1.4** | **Thuật toán Ghép cặp Thông minh Đa Bán kính (3-Tier Matching)** | Mở rộng theo 3 bậc thang: **Tier 1 (1.5 km)** $\rightarrow$ **Tier 2 (3.0 km)** $\rightarrow$ **Tier 3 (5.0 km)**. Hàm tính điểm chuẩn hóa $S(d) = 0.50 \cdot S_{\text{dist}} + 0.30 \cdot S_{\text{bat}} + 0.20 \cdot S_{\text{rate}}$. | **ĐÃ HOÀN THÀNH** |
| **1.5** | **Rào chắn An toàn Pin Xe điện (Battery Safeguard)** | Tự động loại trừ các xe điện có mức pin hiện tại $B(d) < 20\%$ khỏi danh sách điều phối; yêu cầu pin $\ge 35\%$ đối với chuyến đi dài $> 15\,\text{km}$. | **ĐÃ HOÀN THÀNH** |
| **1.6** | **Khóa Phân tán Redisson chống Race Condition** | Khóa `lock:driver:{driverId}` với thời gian chờ phản hồi tối đa 15 giây (`driver-response-timeout-seconds: 15`). Tự động giải phóng lock khi tài xế nhận/từ chối hoặc khi timeout. | **ĐÃ HOÀN THÀNH** |
| **1.7** | **Hàng đợi Sự kiện RabbitMQ & Điều phối Bất đồng bộ** | Thiết lập Topic Exchange `greenmobility.topic.exchange` với các routing key: `trip.event.requested`, `trip.event.dispatched`, `trip.event.matched`, `trip.event.cancelled`. | **ĐÃ HOÀN THÀNH** |
| **1.8** | **Kênh WebSocket STOMP Broker hai chiều** | Endpoint `/ws-connect`: kênh `/user/queue/ride-dispatch` gửi yêu cầu nhận chuyến riêng cho tài xế và kênh `/topic/trip/{tripId}` phát sóng cập nhật trạng thái chuyến xe. | **ĐÃ HOÀN THÀNH** |

### 2. Phân hệ Web Admin Portal (`frontend-admin` — Next.js 14 + TailwindCSS + MapLibre GL)

| STT | Tính năng / Màn hình | Chi tiết Giao diện & Chức năng | Trạng thái |
| :---: | :--- | :--- | :---: |
| **2.1** | **Bản đồ Điều phối Trực tiếp (`/trips` - Map View)** | Tích hợp Goong Maps Dark Tiles qua MapLibre GL; hiển thị marker xe điện trực tuyến (E-Bike, E-Car 4S), điểm đón khách, bán kính quét và chế độ Tactical Radar Grid. | **ĐÃ HOÀN THÀNH** |
| **2.2** | **Quản lý Toàn bộ Cuốc xe Đa Trạng thái (`/trips` - Table View)** | Hiển thị bảng dữ liệu thời gian thực với 4 thẻ KPI: Đang tìm xe, Đang di chuyển, Đã hoàn thành, Lũy kế CO2 đã giảm. Hỗ trợ bộ lọc theo tab: Tất cả, Đang tìm xe, Đang chạy, Hoàn thành, Đã hủy và tìm kiếm tức thì. | **ĐÃ HOÀN THÀNH** |
| **2.3** | **Modal Chi tiết Cuốc xe & Đối soát Tác động Môi trường** | Click vào từng chuyến xe để hiển thị: Lộ trình đón - trả, thông tin tài xế & dòng xe điện, biển số, hình thức thanh toán và **Hóa đơn tác động môi trường** (+g CO2 giảm, số ngày cây xanh, giờ đèn LED). | **ĐÃ HOÀN THÀNH** |
| **2.4** | **Trung tâm Giám sát Trực tiếp (`/trips/live-tracking`)** | Bảng điều khiển vận hành thời gian thực: Cuốc xe đang chạy, Thời gian đón trung bình ($6.5\text{m}$), Tổng quãng đường xe điện chạy ($20.9\text{km}$), CO2 giảm thiểu trong ngày ($1.38\text{kg}$), đồng bộ danh sách chuyến xe trực tuyến. | **ĐÃ HOÀN THÀNH** |
| **2.5** | **Cập nhật Bảng Điều hành Carbon (`/`)** | Bảng điều khiển quản trị tổng hợp cập nhật chỉ số CO2 giảm phát thải, đội xe điện trực tuyến và chuyến xe xanh sau khi hoàn tất các cuốc xe Sprint 2. | **ĐÃ HOÀN THÀNH** |

### 3. Phân hệ Mobile Apps (Flutter — `customer_app` & `driver_app`)

| STT | Tính năng / Màn hình | Chi tiết Triển khai trên Mobile | Trạng thái |
| :---: | :--- | :--- | :---: |
| **3.1** | **Màn hình Đặt xe & Ước tính Carbon (`RideBookingScreen`)** | Cho phép khách chọn điểm đón/trả; lựa chọn phương tiện xe điện (E-Bike, E-Car 4 chỗ); hiển thị thẻ "Dự toán Giảm phát thải CO2" theo chuẩn IPCC (CO2 giảm được, ngày cây xanh, giờ đèn LED) và cước phí cạnh tranh. | **ĐÃ HOÀN THÀNH** |
| **3.2** | **Màn hình Radar Quét Tìm Tài xế 15s (`SearchingRadarScreen`)** | Hiệu ứng sóng radar tỏa tròn 3 bán kính (Tier 1 $\rightarrow$ Tier 3); đồng hồ đếm ngược 15 giây điều phối; hiển thị thông tin cuốc xe và nút "Hủy tìm kiếm cuốc xe" an toàn. | **ĐÃ HOÀN THÀNH** |
| **3.3** | **Pop-up Nhận Cuốc Xe Điện 15s (`RideDispatchModal`)** | Khi có cuốc xe phù hợp: điện thoại rung, phát chuông báo động, hiển thị modal toàn màn hình với thanh tiến trình đếm ngược 15 giây, thu nhập cuốc xe, lượng CO2 xanh, vị trí đón/trả và cặp nút "Chấp nhận" / "Từ chối". | **ĐÃ HOÀN THÀNH** |
| **3.4** | **Màn hình Chuyến đi Đang Di chuyển (`DriverActiveTripScreen`)** | Điều hướng turn-by-turn đón khách/chở khách, hiển thị chỉ số telemetry thời gian thực: % pin xe điện, tốc độ di chuyển, lượng CO2 đang tiết kiệm, nút liên hệ khách hàng và nút "Hoàn Thành Chuyến Đi". | **ĐÃ HOÀN THÀNH** |

---

## III. Hoàn Thành Các Hạng Mục Cải Tiến Theo Góp Ý Của Giảng Viên Hướng Dẫn từ Sprint 1

Tiếp thu và hoàn thiện các nhận xét, góp ý chuyên môn từ Giảng viên Hướng dẫn tại buổi báo cáo Sprint 1, nhóm đã hoàn thiện trọn vẹn 3 hạng mục nâng cấp kiến trúc then chốt:

```mermaid
flowchart TD
    subgraph Feedback1["1. Token Security (JWT + Refresh Token)"]
        AT["Rút ngắn Access Token\n(Xuống 15 phút / 900s)"]
        RT["Refresh Token Xoay vòng\n(Thời hạn 7 ngày / 604800s)"]
        ROT["Endpoint POST /auth/refresh\nCấp mới token an toàn"]
        AT --- RT --- ROT
    end

    subgraph Feedback2["2. Anti-Spoofing (Liveness Detection)"]
        LAP["Phân tích Kết cấu Vi mô\n(Laplacian Texture Variance)"]
        SPEC["Phát hiện Phản xạ Màn hình\n(Specular Reflection Analysis)"]
        LIV["Ngưỡng Liveness Score >= 0.80\nChặn 100% ảnh in / màn hình chụp lại"]
        LAP --- SPEC --- LIV
    end

    subgraph Feedback3["3. Smart Image Compression"]
        DIM["Giảm kích thước Tỷ lệ\n(Max Dimension Clamping <= 1920px)"]
        LOSSY["Nén JPEG/WebP Chất lượng cao\n(Hệ số nén Q = 0.80)"]
        SAVE["Tiết kiệm 88% dung lượng lưu trữ\n(4.5 MB -> 350 KB - 500 KB)"]
        DIM --- LOSSY --- SAVE
    end
```

### 1. Rút ngắn Thời hạn Access Token kết hợp Cơ chế Refresh Token Xoay vòng (Refresh Token Rotation)
* **Vấn đề tồn tại từ Sprint 1**: Access Token có thời hạn dài 24 giờ (`86400` giây), tiềm ẩn nguy cơ bảo mật nếu token bị rò rỉ trên đường truyền hoặc bị đánh cắp qua tấn công giả mạo (Session Hijacking).
* **Giải pháp & Kết quả triển khai**:
  - Rút ngắn thời hạn Access Token xuống **15 phút** (`900` giây) trong cấu hình `security.jwt.expiration-seconds`.
  - Bổ sung **Refresh Token** có thời hạn **7 ngày** (`604800` giây) với claim phân định `type: "REFRESH"`.
  - Xây dựng API `POST /auth/refresh` tiếp nhận `RefreshTokenRequest`, kiểm tra tính toàn vẹn và trạng thái hoạt động của tài khoản người dùng (`UserStatus.ACTIVE`), sau đó cấp phát cặp Access Token mới cùng Refresh Token mới theo nguyên tắc **Refresh Token Rotation (RTR)**.
  - Được kiểm chứng tự động qua unit test `testShortenedAccessTokenAndRefreshToken` trong `Sprint1FeedbackImprovementsTest.java` (**PASS**).

### 2. Nghiên cứu & Ứng dụng Cơ chế Liveness Detection (Chống giả mạo ảnh chụp trong xác thực khuôn mặt)
* **Vấn đề tồn tại từ Sprint 1**: Thuật toán so khớp vector Cosine Similarity 512D chỉ đối soát đặc trưng khuôn mặt tĩnh, có rủi ro bị vượt qua nếu tài xế sử dụng điện thoại khác chụp lại chân dung hoặc đưa bản in CCCD trước camera (Presentation Attack).
* **Giải pháp & Kết quả triển khai**:
  - Xây dựng phương thức `evaluateLiveness(MultipartFile imageFile)` trong `FaceVerificationService`:
    + **Phân tích Phương sai Toán tử Laplacian (Laplacian Texture Variance)**: Da mặt người sống có cấu trúc lỗ chân lông vi mô tự nhiên tạo phương sai biến thiên biên cạnh trong khoảng $[150, 4500]$. Trong khi đó, màn hình phẳng hoặc ảnh in lại bị nén nhòe hoặc xuất hiện vân sọc chu kỳ Moiré bất thường.
    + **Phát hiện Điểm Chói Phản quang (Specular Reflection)**: Kính cường lực màn hình điện thoại/máy tính bảng phát lại luôn tạo các điểm hotspot phản chiếu ánh sáng môi trường đặc trưng.
    + **Chấm điểm Liveness Score**: Đạt ngưỡng an toàn khi $L_{\text{score}} \ge 0.80$. Loại trừ ngay các trường hợp giả mạo màn hình (Screen replay attack).
  - Được kiểm chứng tự động qua unit test `testLivenessDetection` trong `Sprint1FeedbackImprovementsTest.java` (**PASS**).

### 3. Tối ưu hóa Nén ảnh Thông minh trước khi Lưu trữ (Two-Stage Image Compression Pipeline)
* **Vấn đề tồn tại từ Sprint 1**: Các ảnh chụp giấy tờ (CCCD 2 mặt, Giấy phép lái xe, Cà vẹt xe, Ảnh selfie) chụp từ camera di động có độ phân giải rất cao (3MB – 8MB / ảnh), gây tốn kém dung lượng lưu trữ trên MinIO S3 và làm chậm thời gian tải trang thẩm định KYC trên Web Admin.
* **Giải pháp & Kết quả triển khai**:
  - Nâng cấp dịch vụ `FileStorageService` với quy trình nén 2 tầng tự động:
    + **Tầng 1 - Giới hạn Kích thước Tỷ lệ (Smart Dimension Clamping)**: Nếu ảnh có chiều rộng hoặc chiều cao $> 1920\text{px}$ (Full HD), hệ thống tự động scale thu nhỏ giữ nguyên tỷ lệ khung hình với thuật toán nội suy làm mịn `RenderingHints.VALUE_INTERPOLATION_BILINEAR`.
    + **Tầng 2 - Nén có Kiểm soát Chất lượng (Lossy Compression with Quality Tuning)**: Sử dụng `ImageWriter` nén sang định dạng JPEG tối ưu với hệ số chất lượng $Q = 0.80$ (80%).
  - **Hiệu quả đo đạc thực tế**: Ảnh gốc dung lượng **12.96 MB** sau khi xử lý nén tự động chỉ còn **1.54 MB** (tiết kiệm **$88.1\%$** dung lượng lưu trữ và băng thông truyền tải mạng), trong khi độ sắc nét của chữ số, hoa văn bảo an và chữ ký vẫn được bảo toàn nguyên vẹn phục vụ kiểm tra đối soát.
  - Được kiểm chứng tự động qua unit test `testSmartImageCompression` trong `Sprint1FeedbackImprovementsTest.java` (**PASS**).

---

## IV. Minh chứng Giao diện Thực tế (Screenshots)

### 1. Cổng Quản Trị Web Admin (`frontend-admin`)

#### 1.1. Bản đồ Điều phối Chuyến đi Xanh (`/trips` — Map View)
Màn hình điều phối chuyến xe trực tiếp tích hợp bản đồ vệ tinh Goong Dark Tiles thời gian thực, hiển thị vị trí các đối tác xe điện (E-Bike, E-Car 4 chỗ), các cuốc xe đang tìm kiếm và bán kính điều phối:

![Admin Trips Map - Bản đồ Điều phối Chuyến đi Xanh](./assets/SPRINT_2/admin_trips_map.png)

---

#### 1.2. Danh sách Quản lý Cuốc xe Đa Trạng thái & Bộ lọc Phân loại (`/trips` — Table View)
Bảng quản lý cuốc xe điện trực quan với 4 thẻ KPI thời gian thực, bảng phân loại chi tiết đầy đủ 5 trạng thái (`SEARCHING`, `MATCHED`, `IN_TRIP`, `COMPLETED`, `CANCELLED`), cước phí và lượng $CO_2$ giảm phát thải:

![Admin Trips Table - Quản lý Chi tiết Danh sách Cuốc xe](./assets/SPRINT_2/admin_trips_table.png)

---

#### 1.3. Modal Chi tiết Cuốc xe & Hóa đơn Tác động Môi trường (`TripDetailModal`)
Giao diện thẩm định chi tiết chuyến xe: đối soát lộ trình đón/trả, thông tin tài xế xe điện (VinFast Feliz S, Biển số 59-P1 987.65), cước phí thực trả và **Hóa đơn tác động môi trường** (+228g CO2, ~3.8 ngày cây xanh hấp thụ, ~31.6 giờ đèn LED 10W):

![Admin Trip Detail Modal - Thẩm định Cuốc xe & Tác động CO2](./assets/SPRINT_2/admin_trip_detail_modal.png)

---

#### 1.4. Trung tâm Giám sát Trực tiếp Live Tracking & Phân tích Đội xe (`/trips/live-tracking`)
Bảng điều khiển giám sát vận hành thời gian thực: Thống kê cuốc xe đang chạy, thời gian đón trung bình (6.5 phút), tổng quãng đường xe điện chạy (20.9 km), CO2 giảm thiểu (1.38 kg hôm nay) và danh sách chuyến xe trực tuyến:

![Admin Live Tracking - Trung tâm Giám sát Trực tiếp](./assets/SPRINT_2/admin_live_tracking.png)

---

#### 1.5. Cập nhật Trung tâm Giám sát Carbon sau Sprint 2 (`/`)
Bảng điều khiển trung tâm tự động cập nhật tổng phát thải $CO_2$ giảm được, số ngày cây xanh tương đương theo chuẩn IPCC và đội xe điện trực tuyến sau khi hoàn thành các chu kỳ chuyến đi Sprint 2:

![Admin Dashboard - Trung tâm Giám sát Carbon cập nhật](./assets/SPRINT_2/admin_dashboard_s2.png)

---

### 2. Ứng dụng Di động Mobile (`customer_app` & `driver_app`)

#### 2.1. Đặt xe & Dự toán Phát thải Carbon tức thì trên Customer App
Giao diện ứng dụng khách hàng: Chọn điểm đón/trả, lựa chọn dòng xe điện (E-Bike VinFast Feliz S/Klara S, E-Car VF e34) và hiển thị thẻ **Dự toán Giảm phát thải CO2** theo chuẩn IPCC trước khi đặt xe:

![Customer App - Đặt xe & Ước tính Carbon](./assets/SPRINT_2/customer_booking_estimate.png)

---

#### 2.2. Màn hình Radar Quét Tìm Tài xế Xe điện Đa Bán kính 15s
Màn hình tìm kiếm đối tác với hiệu ứng sóng radar 3 bán kính (Tier 1 $\rightarrow$ Tier 2 $\rightarrow$ Tier 3), hiển thị các tài xế xe điện xung quanh kèm đồng hồ đếm ngược 15 giây điều phối:

![Customer App - Radar Quét Tìm Tài xế](./assets/SPRINT_2/customer_searching_radar.png)

---

#### 2.3. Modal Pop-up Nhận Cuốc Xe Điện 15s cho Đối tác Tài xế
Khi Matching Engine quét trúng tài xế, ứng dụng Driver App lập tức rung, phát chuông báo động và mở pop-up nhận đơn 15 giây với thanh tiến trình đếm ngược, thu nhập ước tính và cự ly đón:

![Driver App - Pop-up Nhận Cuốc 15s](./assets/SPRINT_2/driver_ride_dispatch_modal.png)

---

#### 2.4. Màn hình Điều hướng Đón Khách & Chuyến Đi Xanh
Màn hình thực hiện cuốc xe dành cho tài xế: Điều hướng turn-by-turn theo thời gian thực, hiển thị chỉ số pin xe điện (88%), tốc độ di chuyển (32 km/h), lượng $CO_2$ giảm được (-410g) và thông tin hành khách:

![Driver App - Chuyến Đi Xanh Đang Thực Hiện](./assets/SPRINT_2/driver_active_trip.png)

---

## V. Bảng Đối chiếu Tiêu chí Nghiệm thu Sprint 2 (Acceptance Criteria)

Tất cả 13 tiêu chí nghiệm thu đặc tả trong tài liệu kỹ thuật Sprint 2 (`SPRINT_2_SPEC.md`) và các hạng mục bổ sung theo góp ý của Giảng viên Hướng dẫn đều đã được kiểm thử toàn diện trên hệ thống thực tế và đạt kết quả mong đợi:

| Mã AC | Nghiệp vụ Kiểm thử | Đầu vào & Thao tác Thực hiện | Kết quả Thực tế Đạt được | Đánh giá |
| :---: | :--- | :--- | :--- | :---: |
| **AC-01** | Ước tính cước & CO2 | `POST /trips/estimate` với lộ trình 16.2km | Trả về `HTTP 200 OK`, khoảng cách 16.2km, cước 76.000đ, CO2 giảm 817.29g | **PASS** |
| **AC-02** | Khách tạo cuốc xe mới | `POST /trips/request` với tọa độ hợp lệ | Trả về `HTTP 201 Created`, sinh mã `GM-20261005-xxxx`, trạng thái `SEARCHING`, publish RabbitMQ | **PASS** |
| **AC-03** | Khách tạo cuốc khi đang có cuốc dở dang | `POST /trips/request` khi đã có cuốc `SEARCHING` | Trả về `HTTP 400 Bad Request` chặn trùng lặp, bảo toàn tính toàn vẹn phiên đặt | **PASS** |
| **AC-04** | Tài xế trực tuyến đẩy vị trí vào Redis GEO | `POST /driver/location/ping` khi `is_active_shift = true` | Tọa độ lưu vào Redis GEO `drivers:geo:available:{type}`, truy vấn `GEOSEARCH` tức thì | **PASS** |
| **AC-05** | Tài xế tắt ca không được ghép đơn | Gạt tắt ca (`is_active_shift = false`) | Xóa tài xế khỏi Redis GEO, Matching Engine không quét trúng tài xế ngoại tuyến | **PASS** |
| **AC-06** | Lọc xe điện pin yếu (Battery Safeguard) | Tài xế có pin xe điện $< 20\%$ | Bị Matching Engine tự động loại bỏ khỏi danh sách ứng viên điều phối | **PASS** |
| **AC-07** | Mở rộng đa bán kính (Tier Expansion) | Điểm đón không có tài xế trong 1.5km, có tài xế cách 2.5km | Matching Engine tự động mở rộng sang Tier 2 (3.0km) và ghép nối thành công | **PASS** |
| **AC-08** | Khóa phân tán Redisson chống Race Condition | 2 cuốc xe cùng tìm kiếm 1 tài xế rảnh | Chỉ 1 cuốc xe lấy được `lock:driver:{driverId}`, cuốc còn lại chuyển sang tài xế kế tiếp | **PASS** |
| **AC-09** | Dispatch qua WebSocket STOMP | Khớp được tài xế ứng viên | Tin nhắn đẩy về `/user/queue/ride-dispatch`, Driver App rung và hiện Pop-up 15s | **PASS** |
| **AC-10** | Tài xế bấm Chấp nhận nhận cuốc | `POST /driver/trips/{tripId}/accept` trong vòng 15s | Chuyển `status = 'MATCHED'`, gán `driver_id`, gửi thông báo cho khách, xóa khỏi Redis GEO | **PASS** |
| **AC-11** | Tài xế Từ chối hoặc Timeout 15s | Bấm "Từ chối" hoặc để hết 15s đếm ngược | Giải phóng lock Redisson, lưu Cooldown 60s, Engine chuyển ngay sang tài xế kế tiếp | **PASS** |
| **AC-12** | Khách hàng bấm Hủy khi đang tìm xe | `POST /trips/{tripId}/cancel` | Cuốc xe chuyển thành `CANCELLED`, giải phóng mọi lock đang chờ, dừng radar quét | **PASS** |
| **AC-13** | Quét hết 3 Tier không tìm thấy tài xế | Hết bán kính 5.0km không ai nhận | Chuyển cuốc sang `CANCELLED` với lý do `NO_DRIVER_AVAILABLE` | **PASS** |
| **FB-01** | Rút ngắn Access Token & Refresh Token | `POST /auth/login` và `POST /auth/refresh` | Access Token rút ngắn xuống 15m (900s), Refresh Token 7 ngày xoay vòng an toàn | **PASS** |
| **FB-02** | Liveness Detection chống giả mạo ảnh | `FaceVerificationService.evaluateLiveness` | Phương sai Laplacian đạt chuẩn $L_{\text{score}} \ge 0.80$, chặn 100% ảnh chụp lại từ màn hình | **PASS** |
| **FB-03** | Tối ưu nén ảnh trước khi lưu trữ | `FileStorageService.storeFile` | Tự động resize $\le 1920\text{px}$, nén JPEG Q=0.80, giảm $88.1\%$ dung lượng lưu trữ | **PASS** |

---

## VI. Kết luận & Kế hoạch Tiếp theo (Sprint 3)

- **Kết quả Sprint 2**: Đã hoàn thành 100% mục tiêu cốt lõi của nền tảng Green Mobility về cơ chế Đặt xe điện (Ride Booking), Thuật toán Ghép cặp Thông minh Đa Bán kính (Matching Engine), Cơ chế Phân phối Đơn không nghẽn với Redisson Distributed Lock và Hàng đợi RabbitMQ, cùng hệ thống WebSocket STOMP thời gian thực.
- Đồng thời, toàn bộ 3 nội dung đóng góp ý kiến từ Giảng viên Hướng dẫn tại Sprint 1 (Rút ngắn thời hạn Token kết hợp Refresh Token, Liveness Detection chống gian lận ảnh chụp và Tối ưu nén ảnh lưu trữ) đều đã được hiện thực hóa và kiểm thử tự động thành công.
- **Kế hoạch Sprint 3**: Tiếp tục kích hoạt các giai đoạn di chuyển tiếp theo của hành trình chuyến xe xanh:
  1. Luồng định vị GPS thời gian thực (Live GPS Tracking Stream) với tần suất 3 giây/lần.
  2. Tính toán lại lộ trình động (Dynamic Rerouting & ETA Calculation) khi tài xế đi lệch tuyến $> 100\text{m}$.
  3. Cơ chế Geofencing tự động nhận diện tài xế đã đến điểm đón (bán kính $50\text{m}$).
  4. Thuật toán đo lường lượng phát thải thực tế sau cuốc xe (Actual Emission Reduction Measurement) và tích lũy Tín chỉ Carbon / Điểm thưởng Xanh cho hành khách.
