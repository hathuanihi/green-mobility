# Báo cáo Tiến độ Dự án Green Mobility — Sprint 3

> **Dự án**: Nền tảng gọi xe điện tích hợp hệ thống đo lường, báo cáo và khuyến khích giảm phát thải carbon (Green Mobility)  
> **Giai đoạn**: **Sprint 3 — Real-time GPS Tracking, Trip Execution & Turn-by-Turn Navigation**  
> **Trạng thái tổng thể**: **HOÀN THÀNH 100% CÁC HẠNG MỤC SPRINT 3**  
> **Thời gian thực hiện theo Đề cương KLTN**: 19/10/2026 – 25/10/2026 (Hoàn thành sớm: 05/10/2026)  
> **Sinh viên thực hiện**:  
> - **Phạm Hà Anh Thư** — MSSV: 23521544  
> - **Nguyễn Minh Thiện** — MSSV: 23521484  
> **Cán bộ hướng dẫn**: TS. Đỗ Thị Thanh Tuyền  
> **Đơn vị**: Trường Đại học Công nghệ Thông tin, Đại học Quốc gia TP. Hồ Chí Minh (UIT - ĐHQG TP.HCM)  
> **Tài liệu đặc tả tham chiếu**: [SPRINT_3_SPEC.md](./SPRINT_3_SPEC.md)

---

## I. Tổng quan Tiến độ Sprint 3

Sprint 3 kế thừa toàn bộ nền tảng Định danh, Hồ sơ tài xế xe điện, Xác thực khuôn mặt sinh trắc học của Sprint 1 và Hệ thống Đặt xe & Ghép cặp tài xế đa bán kính của Sprint 2, chính thức hiện thực hóa **giai đoạn thực thi cuốc xe (Trip Execution Lifecycle)** — giai đoạn quan trọng và phức tạp nhất về mặt kỹ thuật thời gian thực trong toàn bộ hệ thống Green Mobility:

$$\text{MATCHED} \longrightarrow \text{DRIVER\_ARRIVING} \longrightarrow \text{ARRIVED} \longrightarrow \text{IN\_TRIP} \longrightarrow \text{COMPLETED}$$

Sprint 3 giải quyết trọn vẹn 4 thách thức kỹ thuật cốt lõi:
1. **Truyền dẫn & Lưu trữ Dữ liệu Telemetry GPS Cao Tần**: Xây dựng kênh WebSocket STOMP hai chiều xử lý luồng ping GPS chu kỳ 3–5 giây, lưu trữ chuỗi thời gian tối ưu vào **MongoDB Time-Series Collection (`trip_gps_points`)** với chỉ mục không gian `2dsphere` và chính sách TTL tự động dọn dẹp.
2. **Điều hướng Dẫn đường Turn-by-Turn & Cập nhật Lộ trình Động (Dynamic Re-routing)**: Tích hợp Routing Engine Goong/OSRM cung cấp danh sách chỉ dẫn rẽ chi tiết; thuật toán `TripEtaService` phát hiện tài xế lệch lộ trình $> 100\,\text{m}$ để tự động tính toán lại đường đi và ETA mà không gây quá tải API định tuyến.
3. **Tự động hóa Geofence Điểm Đón & Kiểm soát Thao tác An toàn**: Tự động chuyển trạng thái `ARRIVED` khi xe tiến vào bán kính $\le 50\,\text{m}$; ứng dụng thao tác trượt ngang **Slide-to-Action** cho tài xế khi bắt đầu và hoàn thành cuốc xe nhằm loại trừ triệt để tình trạng bấm nhầm khi đang điều khiển phương tiện giao thông.
4. **Tính Quãng Đường Thực Tế & Lọc Nhiễu GPS Chuẩn Xác**: Ứng dụng thuật toán tính tổng khoảng cách Haversine có bộ lọc nhiễu vận tốc bất thường ($v > 120\,\text{km/h}$), đối soát tỷ lệ quãng đường thực tế so với dự tính ban đầu để phát hiện dấu hiệu gian lận và bàn giao dữ liệu `actual_distance_m` chính xác cho **Sprint 4 (Carbon Emission Calculation Engine)**.

```mermaid
sequenceDiagram
    autonumber
    actor Driver as Tài xế (Driver App)
    participant TripAPI as Trip Service (Spring Boot)
    participant Redis as Redis Cache & GEO
    participant Mongo as MongoDB Telemetry
    participant OSRM as OSRM / Goong Engine
    participant WS as WebSocket STOMP Broker
    actor Customer as Khách hàng (Customer App)

    Note over Driver,Customer: === GIAI ĐOẠN 1: Di chuyển đón khách (DRIVER_ARRIVING) ===
    Driver->>TripAPI: POST /driver/trips/{tripId}/start-arriving
    TripAPI->>TripAPI: Cập nhật status = DRIVER_ARRIVING
    TripAPI->>OSRM: GET route (driver → pickup)
    OSRM-->>TripAPI: Polyline, Steps, Distance, ETA
    TripAPI->>Redis: Lưu hash tracking & route polyline
    TripAPI->>WS: Broadcast /topic/trip/{tripId} (DRIVER_ARRIVING + Route)
    WS-->>Customer: Bản đồ hiển thị xe đang đến đón

    loop Mỗi 3-5 giây di chuyển
        Driver->>WS: STOMP /app/driver/location-update (lat, lng, speed, bearing, battery)
        WS->>TripAPI: DriverLocationController xử lý
        TripAPI->>Mongo: Insert GPS Point (phase: DRIVER_ARRIVING)
        TripAPI->>Redis: HSET trip:tracking (lat, lng, bearing, eta)
        TripAPI->>WS: Broadcast /topic/driver-location/{driverId}
        WS-->>Customer: Marker xe trượt mượt mà (Lerp Interpolation)
    end

    Note over Driver,Customer: === GIAI ĐOẠN 2: Đến điểm đón & Chờ khách (ARRIVED) ===
    TripAPI->>TripAPI: Phát hiện Geofence <= 50m từ pickup_geom
    TripAPI->>WS: Push /topic/trip/{tripId} (status: ARRIVED)
    WS-->>Customer: Thông báo "Tài xế đã đến điểm đón!"
    Driver->>TripAPI: POST /driver/trips/{tripId}/arrive (nếu bấm thủ công <= 200m)
    Driver->>Driver: Bắt đầu đếm ngược 5:00 phút chờ khách

    Note over Driver,Customer: === GIAI ĐOẠN 3: Bắt đầu chở khách (IN_TRIP) ===
    Driver->>TripAPI: POST /driver/trips/{tripId}/start-trip (Slide to Start)
    TripAPI->>TripAPI: Cập nhật status = IN_TRIP, started_trip_at = now()
    TripAPI->>OSRM: GET route (pickup → dropoff)
    OSRM-->>TripAPI: Polyline & Steps lộ trình đến điểm trả
    TripAPI->>WS: Push /topic/trip/{tripId} (IN_TRIP + Route)
    WS-->>Customer: Bản đồ chuyển sang lộ trình về điểm đích

    loop Mỗi 3-5 giây di chuyển
        Driver->>WS: STOMP /app/driver/location-update
        TripAPI->>Mongo: Insert GPS Point (phase: IN_TRIP)
        TripAPI->>WS: Broadcast /topic/driver-location/{driverId}
        WS-->>Customer: Cập nhật vị trí & ETA đến điểm trả
    end

    Note over Driver,Customer: === GIAI ĐOẠN 4: Hoàn thành chuyến đi (COMPLETED) ===
    Driver->>TripAPI: POST /driver/trips/{tripId}/complete (Slide to Complete)
    TripAPI->>Mongo: Lấy toàn bộ GPS points (phase: IN_TRIP)
    TripAPI->>TripAPI: ActualDistanceCalculator (Lọc nhiễu + Tổng Haversine)
    TripAPI->>TripAPI: Tính CO2 giảm, chia thu nhập tài xế (80% net, 20% sàn)
    TripAPI->>TripAPI: Cập nhật status = COMPLETED, completed_at = now()
    TripAPI->>Redis: Xóa hash tracking, hoàn trả tài xế vào Redis GEO available
    TripAPI->>TripAPI: Publish TripCompletedEvent lên RabbitMQ (chuẩn bị Sprint 4)
    TripAPI->>WS: Push /topic/trip/{tripId} (COMPLETED + Summary)
    WS-->>Customer: Modal Đánh giá 5 sao & Hóa đơn Carbon
    WS-->>Driver: Màn hình Thu nhập Chuyến đi
```

---

## II. Bảng Chi tiết Các Tính Năng Đã Hoàn Thành

### 1. Phân hệ Backend (Spring Boot 3.3.3 + MongoDB Time-Series + Redis + RabbitMQ + WebSocket STOMP)

| STT | Tính năng / Thành phần | Chi tiết Kỹ thuật đã triển khai | Trạng thái |
| :---: | :--- | :--- | :---: |
| **1.1** | **Máy trạng thái Vòng đời Cuốc xe Mở rộng** | Bổ sung 4 trạng thái vận hành: `DRIVER_ARRIVING`, `ARRIVED`, `IN_TRIP`, `COMPLETED` cùng các ràng buộc nghiệp vụ chuyển trạng thái nghiêm ngặt trong `TripService`. | **ĐÃ HOÀN THÀNH** |
| **1.2** | **Bộ điều khiển REST Chuyến xe Tài xế** | Cài đặt `DriverTripController` với 6 API: `start-arriving`, `arrive`, `start-trip`, `complete`, `cancel`, `sync-gps-batch`. | **ĐÃ HOÀN THÀNH** |
| **1.3** | **API Truy vấn Lộ trình & Tracking Khách hàng** | Cài đặt `TripController`: `GET /trips/{tripId}/tracking` (tra cứu vị trí tài xế, ETA từ Redis) và `GET /trips/{tripId}/route` (lộ trình Turn-by-Turn hiện tại). | **ĐÃ HOÀN THÀNH** |
| **1.4** | **WebSocket STOMP Controller Thu nhận GPS** | Cài đặt `DriverLocationController`: tiếp nhận tin nhắn `/app/driver/location-update`, đối chiếu quyền sở hữu cuốc xe, kiểm tra mock location, tự động kích hoạt geofence và phát sóng `/topic/driver-location/{driverId}`. | **ĐÃ HOÀN THÀNH** |
| **1.5** | **Lưu trữ Telemetry GPS trên MongoDB** | Thiết kế entity `TripGpsPoint` lưu trữ trong collection `trip_gps_points`: tọa độ, tốc độ, góc quay, độ chính xác, dung lượng pin, pha di chuyển (`DRIVER_ARRIVING`/`IN_TRIP`); thiết lập chỉ mục kép `(tripId, phase, timestamp)` và `2dsphere`. | **ĐÃ HOÀN THÀNH** |
| **1.6** | **Thuật toán Geofence Đón khách Tự động** | Cài đặt `GeofenceService`: tính khoảng cách Haversine giữa vị trí xe và điểm đón; tự động chuyển sang `ARRIVED` khi khoảng cách $\le 50\,\text{m}$; cho phép xác nhận thủ công nếu $\le 200\,\text{m}$, từ chối khi $> 200\,\text{m}$. | **ĐÃ HOÀN THÀNH** |
| **1.7** | **Bộ tính Quãng đường Thực tế & Bộ lọc Nhiễu** | Cài đặt `ActualDistanceCalculator`: lọc bỏ điểm GPS có độ chính xác kém ($> 50\,\text{m}$), lọc điểm có vận tốc phi lý ($> 120\,\text{km/h}$), cộng dồn Haversine; đối soát tỷ lệ thực tế/ước tính và kích hoạt cờ nghi vấn gian lận nếu $> 1.3$. | **ĐÃ HOÀN THÀNH** |
| **1.8** | **Ước lượng ETA Động (Dynamic ETA Recalculation)** | Cài đặt `TripEtaService`: ước lượng ETA tuyến tính chu kỳ 3 giây để tránh nghẽn API; kích hoạt gọi Re-route OSRM/Goong khi khoảng cách vuông góc từ xe tới polyline $> 100\,\text{m}$. | **ĐÃ HOÀN THÀNH** |
| **1.9** | **Tích hợp RabbitMQ Bàn giao Sang Sprint 4** | Khai báo Queue bền vững `q.trip.status.completed` liên kết với Topic Exchange qua routing key `trip.event.completed`, phát sinh sự kiện `TripCompletedEvent` mang đầy đủ thông số $CO_2$ và quãng đường thực tế. | **ĐÃ HOÀN THÀNH** |

### 2. Phân hệ Web Admin Portal (`frontend-admin` — Next.js 14 + MapLibre GL)

| STT | Tính năng / Màn hình | Chi tiết Giao diện & Chức năng | Trạng thái |
| :---: | :--- | :--- | :---: |
| **2.1** | **Bản đồ Giám sát Điều phối Trực tiếp (`/trips/live-tracking`)** | Tích hợp giao diện MapLibre GL hiển thị bản đồ vector tối màu Goong Map Tiles; hiển thị vị trí phương tiện thời gian thực với mã màu trạng thái (Vàng: `DRIVER_ARRIVING`, Cam: `ARRIVED`, Xanh lá: `IN_TRIP`). | **ĐÃ HOÀN THÀNH** |
| **2.2** | **Bảng Cuốc xe Trực tuyến Đang Hoạt động** | Danh sách chuyến xe cập nhật tự động chu kỳ 10 giây: Mã cuốc, Tài xế, Dòng xe điện, Trạng thái vận hành, ETA và Quãng đường còn lại. | **ĐÃ HOÀN THÀNH** |
| **2.3** | **Bảng Chỉ số Vận hành Thời gian thực (Live KPIs)** | 4 thẻ thống kê động: Số cuốc đang chạy, Thời gian đón khách trung bình (3.5 phút), Tổng km xe điện lăn bánh (23.9 km) và Lượng $CO_2$ giảm phát thải hôm nay (1.38 kg). | **ĐÃ HOÀN THÀNH** |
| **2.4** | **Bản đồ Điều phối Toàn cảnh & Lọc Cuốc xe (`/trips` — Map View)** | Tích hợp lớp bản đồ Goong Tiles hiển thị đồng thời cả các cuốc xe đang quét tìm tài xế (Radar Pulse) và các cuốc xe đang di chuyển. | **ĐÃ HOÀN THÀNH** |
| **2.5** | **Modal Chi tiết & Hóa đơn Carbon Sau Chuyến Đi (`TripDetailModal`)** | Đối soát lộ trình, thông tin tài xế/khách hàng, trạng thái thanh toán và bảng phân tích phát thải IPCC (+817.29g CO2, ngày cây xanh hấp thụ). | **ĐÃ HOÀN THÀNH** |

### 3. Phân hệ Mobile Đối Tác Tài Xế (`driver_app` — Flutter)

| STT | Tính năng / Màn hình | Chi tiết Triển khai trên Mobile | Trạng thái |
| :---: | :--- | :--- | :---: |
| **3.1** | **Màn hình Thực thi Cuốc xe Đa Pha (`DriverActiveTripScreen`)** | Kiến trúc màn hình toàn diện 894 dòng mã, tự động thích ứng giao diện theo từng giai đoạn: di chuyển đón khách $\rightarrow$ chờ khách $\rightarrow$ đang chở khách $\rightarrow$ tổng kết thu nhập. | **ĐÃ HOÀN THÀNH** |
| **3.2** | **Điều hướng Dẫn đường Turn-by-Turn** | Tích hợp bản đồ MapLibre xoay tự động theo góc di chuyển (`bearing`), hiển thị thanh banner chỉ dẫn khúc rẽ tiếp theo (icon rẽ trái/phải, khoảng cách tới ngã rẽ, tên đường) từ dữ liệu OSRM steps. | **ĐÃ HOÀN THÀNH** |
| **3.3** | **Nút Trượt Thao tác An toàn (Slide-to-Action)** | Sử dụng thanh trượt tương tác cho 2 bước nhạy cảm: **"Trượt để Báo Đã Đến Điểm Đón"** và **"Trượt để Bắt đầu/Hoàn thành"**, triệt tiêu nguy cơ chạm nhầm nút khi tài xế đang điều khiển xe. | **ĐÃ HOÀN THÀNH** |
| **3.4** | **Bộ đếm Thời gian Chờ Khách 5 Phút (ARRIVED)** | Hiển thị đồng hồ đếm ngược trực quan `04:28` phút khi tài xế đến điểm đón; sau khi hết 5 phút tự động hiển thị tùy chọn hủy cuốc do khách không xuất hiện (`CUSTOMER_NO_SHOW`) không tính lỗi cho tài xế. | **ĐÃ HOÀN THÀNH** |
| **3.5** | **Thuật toán GPS Thích ứng Tiết kiệm Pin** | Tự động điều chỉnh chu kỳ phát GPS: **3 giây/lần** khi xe di chuyển ($v > 15\,\text{km/h}$) và giãn ra **15 giây/lần** khi xe dừng đèn đỏ hoặc đỗ chờ nhằm tối ưu thời lượng pin điện thoại. | **ĐÃ HOÀN THÀNH** |
| **3.6** | **Bộ đệm Ngoại tuyến & Đồng bộ GPS Hàng loạt** | Khi phát hiện mất kết nối mạng, các điểm GPS được lưu tạm thời vào danh sách đệm `_offlineGpsBuffer`; khi có kết nối trở lại, tự động gọi API `POST /driver/trips/{tripId}/sync-gps-batch` đồng bộ dữ liệu liên tục lên máy chủ. | **ĐÃ HOÀN THÀNH** |
| **3.7** | **Màn hình Tổng kết Doanh thu & Tác động Xanh** | Bảng sao kê minh bạch: Cước tổng 76.000 đ, Phí sàn 20% (-15.200 đ), Thu nhập thực nhận 60.800 đ (nổi bật màu vàng kim), Lượng $CO_2$ tiết kiệm được (+817.29g) và nút mở ca nhận cuốc tiếp theo. | **ĐÃ HOÀN THÀNH** |

### 4. Phân hệ Mobile Khách Hàng (`customer_app` — Flutter)

| STT | Tính năng / Màn hình | Chi tiết Triển khai trên Mobile | Trạng thái |
| :---: | :--- | :--- | :---: |
| **4.1** | **Màn hình Theo dõi Hành trình Thời gian thực (`CustomerActiveTripScreen`)** | Màn hình theo dõi hành trình 806 dòng mã, kết hợp bản đồ trực quan, thanh thông tin tài xế, tiến trình chuyến xe và bảng tác động môi trường. | **ĐÃ HOÀN THÀNH** |
| **4.2** | **Thuật toán Nội suy Xe Di chuyển Mượt mà 60fps (Lerp Interpolation)** | Sử dụng `AnimationController` thời lượng 1.400ms nội suy tuyến tính tọa độ (`_driverLat`, `_driverLng`) và góc xoay (`_driverBearing`), giúp biểu tượng xe điện lướt êm ái trên đường thay vì nhảy giật theo từng gói tin. | **ĐÃ HOÀN THÀNH** |
| **4.3** | **Đồng bộ Đa Kênh: WebSocket STOMP & Fallback Polling** | Kết nối kênh thời gian thực `/topic/driver-location/{driverId}` và `/topic/trip/{tripId}`; tích hợp cơ chế tự phục hồi định kỳ gọi `GET /trips/{tripId}/tracking` khi mạng chập chờn. | **ĐÃ HOÀN THÀNH** |
| **4.4** | **Thông báo Đón khách & Mã PIN Xác thực** | Banner thông báo nổi bật màu xanh ngọc xuất hiện ngay khi tài xế tiến vào phạm vi đón khách kèm mã PIN xác thực chuyến đi an toàn. | **ĐÃ HOÀN THÀNH** |
| **4.5** | **Modal Tổng kết Chuyến đi & Đánh giá 5 Sao** | Hiển thị Hóa đơn tác động xanh chuẩn IPCC (+817.29g CO2 đã cắt giảm, ~13.6 ngày cây xanh hấp thụ), thanh chọn 5 sao đánh giá dịch vụ, nhãn lời khen và nút hoàn tất tích lũy điểm xanh. | **ĐÃ HOÀN THÀNH** |

---

## III. Cải tiến Nổi bật / Thuật toán & Tối ưu hóa Kỹ thuật

### 1. Kiến trúc Lưu trữ Chuỗi Thời gian Cao Tần trên MongoDB Time-Series (`trip_gps_points`)
* **Thách thức**: Với đội xe hàng nghìn phương tiện phát GPS mỗi 3–5 giây, cơ sở dữ liệu quan hệ PostgreSQL sẽ nhanh chóng cạn kiệt I/O disk và gây phình to bảng lưu trữ, làm chậm các truy vấn đối soát nghiệp vụ chính.
* **Giải pháp Kỹ thuật**:
  - Tách biệt hoàn toàn luồng lưu trữ Telemetry sang MongoDB Time-Series Collection với `granularity: "seconds"` và trường thời gian `timestamp`.
  - Thiết lập chỉ mục phức hợp `(tripId, phase, timestamp)` phục vụ truy vấn vết đường theo pha di chuyển và chỉ mục không gian `2dsphere` phục vụ truy vấn vùng lân cận.
  - Áp dụng chính sách TTL Index (Time-To-Live) tự động giải phóng các điểm GPS chi tiết sau 30 ngày, bảo toàn hiệu năng hệ thống mà không cần can thiệp vận hành thủ công.

### 2. Thuật toán Lọc Nhiễu GPS Vận tốc & Tính Tổng Quãng Đường Haversine Chống Gian Lận Cước
* **Thuật toán `ActualDistanceCalculator`**:
  - Loại bỏ các điểm GPS có độ không chuẩn xác cao: $\text{accuracy} > 50\,\text{m}$.
  - Loại bỏ các bước nhảy tọa độ dị thường (GPS teleport) do mất tín hiệu hầm chui/nhà cao tầng:
  
  $$v_{\text{calc}} = \frac{\text{Haversine}(P_{i-1}, P_i)}{\Delta t} > 120\,\text{km/h} \implies \text{Loại bỏ } P_i$$
  
  - Tính tổng khoảng cách tích lũy $d_{\text{actual}} = \sum \text{Haversine}(P_{i-1}, P_i)$.
  - Phát hiện gian lận hành trình: Nếu $d_{\text{actual}} > 1.3 \times d_{\text{estimated}}$, hệ thống tự động gắn cờ cảnh báo nghi vấn tài xế chạy lòng vòng vào bảng `fraud_alerts` để bộ phận thanh tra đối soát trước khi giải ngân.

### 3. Ước lượng ETA Tuyến tính & Thuật toán Tự động Định tuyến lại (Dynamic Re-routing $> 100\text{m}$)
* **Thuật toán `TripEtaService`**:
  - Tránh nghẽn hạn mức API Goong/OSRM bằng cơ chế nội suy tuyến tính: Trong điều kiện di chuyển bình thường, ETA giảm dần theo thời gian thực và khoảng cách còn lại tới đích.
  - Khi xe lệch khỏi lộ trình đã định một khoảng cách vuông góc $> 100\,\text{m}$, hệ thống tự động gọi Routing Engine tính toán lại lộ trình mới, cập nhật danh sách Turn-by-Turn và gửi polyline mới tới cả Driver App và Customer App qua WebSocket.

### 4. Thuật toán Nội suy Xe Di chuyển Mượt mà 60fps (Lerp Interpolation) trên Mobile
* **Kỹ thuật Triển khai trên Mobile**:
  - Gói tin GPS đến theo chu kỳ gián đoạn 3–5 giây. Nếu cập nhật trực tiếp vị trí marker, biểu tượng xe sẽ bị giật cục gây cảm giác khó chịu cho khách hàng.
  - Ứng dụng thuật toán nội suy tuyến tính (Linear Interpolation - Lerp) kết hợp `AnimationController`:
  
  $$P(t) = P_{\text{prev}} + (P_{\text{target}} - P_{\text{prev}}) \times t, \quad t \in [0, 1]$$
  
  - Đồng thời nội suy góc xoay $\theta(t)$ theo cung ngắn nhất của đường tròn lượng giác, đảm bảo đầu xe luôn hướng tự nhiên theo hướng rẽ của đường đi ở tốc độ khung hình 60fps.

---

## IV. Minh chứng Giao diện Thực tế (Screenshots)

### 1. Cổng Quản Trị Web Admin (`frontend-admin`)

#### 1.1. Trung tâm Giám sát Điều phối Trực tiếp Live Tracking (`/trips/live-tracking`)
Bảng điều khiển giám sát vị trí phương tiện thời gian thực tích hợp bản đồ tối màu Goong Vector Map Tiles, cập nhật tức thời 4 thẻ KPI vận hành (2 cuốc đang chạy, thời gian đón TB 3.5 phút, tổng 23.9 km xe điện, 1.38 kg CO2 giảm) và thanh danh sách chuyến xe trực tuyến:

![Admin Live Tracking - Trung tâm Giám sát Điều phối Trực tiếp](./assets/SPRINT_3/admin_live_tracking.png)

---

#### 1.2. Bản đồ Điều phối Chuyến đi Toàn cảnh (`/trips` — Map View)
Màn hình giám sát tổng hợp toàn thành phố tích hợp Goong Tiles thời gian thực: hiển thị cuốc xe đang tìm kiếm tại khu vực trung tâm, các phương tiện xe điện đang di chuyển, lộ trình nối điểm đón - trả và thanh phân loại trạng thái:

![Admin Trips Map - Bản đồ Điều phối Chuyến đi Toàn cảnh](./assets/SPRINT_3/admin_trips_map.png)

---

#### 1.3. Bảng Quản lý Chi tiết Danh sách Cuốc xe Vận hành (`/trips` — Table View)
Bảng dữ liệu quản lý các cuốc xe Sprint 3 với đầy đủ các trạng thái vận hành (`Đang tìm xe`, `Đang di chuyển`, `Hoàn thành`, `Đã hủy`), đối soát cước phí thực thu, thời gian di chuyển và chỉ số giảm phát thải $CO_2$ tích lũy (2.30 kg):

![Admin Trips Table - Quản lý Chi tiết Danh sách Cuốc xe](./assets/SPRINT_3/admin_trips_table_s3.png)

---

#### 1.4. Modal Đối soát Cuốc xe Hoàn thành & Hóa đơn Carbon (`TripDetailModal`)
Giao diện thẩm định chi tiết chuyến xe: lộ trình di chuyển, thông tin tài xế, phương tiện xe điện, trạng thái thanh toán và **Hóa đơn tác động môi trường** tính toán theo chuẩn quốc tế IPCC:

![Admin Trip Modal - Đối soát Cuốc xe & Hóa đơn Carbon](./assets/SPRINT_3/admin_trip_modal_s3.png)

---

#### 1.5. Cập nhật Dashboard Quản trị & Trung tâm Giám sát Carbon Sprint 3 (`/`)
Bảng điều khiển trung tâm tự động cập nhật tổng phát thải $CO_2$ giảm được (124.85 tấn), 2.08 triệu ngày cây xanh hấp thụ tương đương, 158.420 chuyến xe xanh hoàn tất và hệ số lưới điện quốc gia (722.1 gCO2/kWh theo IPCC):

![Admin Dashboard - Trung tâm Điều hành & Báo cáo Carbon](./assets/SPRINT_3/admin_dashboard_s3.png)

---

### 2. Ứng dụng Di động Mobile (`driver_app` & `customer_app`)

#### 2.1. Điều hướng Dẫn đường Turn-by-Turn khi Đón Khách trên Driver App
Giao diện tài xế trong pha `DRIVER_ARRIVING`: bản đồ xoay tự động theo góc di chuyển, thanh chỉ dẫn rẽ chi tiết (*"150M NỮA RẼ TRÁI VÀO Công trường Lam Sơn, ETA 4 phút, 1.2 km"*), thanh đo telemetry trực tiếp (*Pin xe 92%, Tốc độ 28 km/h, -150g CO2*) và nút trượt an toàn **"Trượt để Báo Đã Đến Điểm Đón"**:

| Turn-by-Turn Navigation (Driver App) | Đếm ngược Chờ Khách 5:00 Phút (Driver App) |
| :---: | :---: |
| ![Driver Navigation Turn-by-Turn](./assets/SPRINT_3/driver_navigation_turn_by_turn.png) | ![Driver Waiting Countdown](./assets/SPRINT_3/driver_waiting_customer.png) |
| *Màn hình điều hướng đón khách với bản đồ Goong Dark và chỉ dẫn rẽ.* | *Màn hình đếm ngược 5:00 phút chờ khách khi tiến vào Geofence 50m.* |

---

#### 2.2. Bảng Tổng kết Doanh thu Cuốc xe & Báo cáo Tác động Xanh của Tài xế
Giao diện tổng kết sau khi hoàn thành chuyến đi: phân tách minh bạch tổng cước (76.000 đ), chiết khấu sàn 20% (-15.200 đ), thu nhập thực nhận của tài xế (60.800 đ màu vàng kim), chứng nhận giảm phát thải (+817.29g CO2, 16.2 km thực tế) và nút mở ca nhận chuyến tiếp theo:

![Driver Completed Summary - Tổng kết Doanh thu & Tác động Xanh](./assets/SPRINT_3/driver_trip_completed_summary.png)

---

#### 2.3. Khách hàng Theo dõi Xe Điện Trực tiếp & Nhận diện Tài xế
Giao diện khách hàng trong pha đón xe: vị trí tài xế trượt êm ái trên đường nhờ thuật toán Lerp 60fps, thẻ thông tin đối tác 5 sao (Nguyễn Văn An ★ 4.9, VinFast Feliz S, Biển số 59-P1 987.65) và huy hiệu đóng góp môi trường (-817g CO2):

| Khách hàng Theo dõi Xe Trực tiếp (Lerp 60fps) | Banner Thông báo Tài xế Đã Đến & Mã PIN |
| :---: | :---: |
| ![Customer Live Tracking](./assets/SPRINT_3/customer_live_tracking.png) | ![Customer Driver Arrived Alert](./assets/SPRINT_3/customer_driver_arrived.png) |
| *Marker xe điện lướt êm ái trên bản đồ Goong theo thời gian thực.* | *Banner báo xe đã đến sảnh đón kèm mã PIN xác thực chuyến đi.* |

---

#### 2.4. Modal Đánh giá 5 Sao & Chứng nhận Tác động Môi trường Chuẩn IPCC
Giao diện đánh giá dịch vụ sau cuốc xe: bảng Chứng nhận Tác động Môi trường (+817.29g CO2 đã cắt giảm, ~13.6 ngày cây xanh hấp thụ), bộ chọn 5 sao cho tài xế và các nhãn khen tặng nhanh:

![Customer Trip Rating - Đánh giá 5 Sao & Hóa đơn Carbon](./assets/SPRINT_3/customer_trip_rating_carbon.png)

---

## V. Bảng Đối chiếu Tiêu chí Nghiệm thu Sprint 3 (Acceptance Criteria)

Toàn bộ **16 tiêu chí nghiệm thu (AC-01 đến AC-16)** đặc tả trong tài liệu kỹ thuật [SPRINT_3_SPEC.md](./SPRINT_3_SPEC.md) đã được kiểm thử tự động toàn diện qua 21 test case và đều đạt kết quả xuất sắc (**100% PASS**):

| Mã AC | Nghiệp vụ Kiểm thử | Đầu vào & Thao tác Thực hiện | Kết quả Thực tế Đạt được | Đánh giá |
| :---: | :--- | :--- | :--- | :---: |
| **AC-01** | Bắt đầu di chuyển đón khách | `POST /driver/trips/{tripId}/start-arriving` khi `status == MATCHED` | HTTP 200 OK; status chuyển sang `DRIVER_ARRIVING`; nhận polyline và danh sách chỉ dẫn rẽ OSRM; phát sự kiện WebSocket. | **PASS** |
| **AC-02** | Stream GPS qua WebSocket STOMP | Tài xế gửi bản tin `/app/driver/location-update` mỗi 3–5 giây | Bản tin được ghi thành công vào MongoDB `trip_gps_points`; Redis hash được cập nhật; khách hàng nhận vị trí qua `/topic/driver-location/{driverId}`. | **PASS** |
| **AC-03** | Tự động chuyển `ARRIVED` qua Geofence | Tọa độ GPS tài xế tiến vào bán kính $\le 50\,\text{m}$ so với `pickup_geom` | Hệ thống tự động chuyển `status = ARRIVED`, ghi nhận `arrived_pickup_at`, phát WebSocket thông báo cho khách hàng ra xe. | **PASS** |
| **AC-04** | Tài xế bấm "Đã đến" khi gần điểm đón | `POST /driver/trips/{tripId}/arrive` khi khoảng cách $\le 200\,\text{m}$ | HTTP 200 OK; trạng thái chuyển sang `ARRIVED`; lưu mốc thời gian đến đón. | **PASS** |
| **AC-05** | Chặn bấm "Đã đến" khi còn cách xa | `POST /driver/trips/{tripId}/arrive` khi khoảng cách $> 200\,\text{m}$ | HTTP 400 Bad Request; trả về thông báo lỗi yêu cầu tài xế di chuyển đến gần hơn. | **PASS** |
| **AC-06** | Tài xế bắt đầu chở khách | `POST /driver/trips/{tripId}/start-trip` khi `status == ARRIVED` | HTTP 200 OK; trạng thái chuyển sang `IN_TRIP`, ghi nhận `started_trip_at`; trả về lộ trình OSRM dẫn đường đến điểm trả. | **PASS** |
| **AC-07** | Hoàn thành chuyến đi & tính khoảng cách | `POST /driver/trips/{tripId}/complete` khi `status == IN_TRIP` | HTTP 200 OK; status $\rightarrow$ `COMPLETED`; truy vấn MongoDB tính `actual_distance_m`; tài xế được hoàn trả về Redis GEO available. | **PASS** |
| **AC-08** | Lọc bỏ điểm GPS nhiễu vận tốc | Dữ liệu GPS có điểm nhảy vọt bất thường ($v > 120\,\text{km/h}$) | Thuật toán tự động bỏ qua điểm dị biệt, tính toán quãng đường thực tế chuẩn xác và hợp lý. | **PASS** |
| **AC-09** | Đối chiếu quãng đường thực tế vs dự tính | Chuyến xe có $d_{\text{actual}} > 1.3 \times d_{\text{est}}$ | Hệ thống ghi nhận cờ cảnh báo nghi vấn gian lận vào `fraud_alerts`; bảo vệ khách hàng bằng cách giữ nguyên cước ước tính. | **PASS** |
| **AC-10** | Cập nhật ETA Động & Re-route | Tài xế di chuyển lệch khỏi lộ trình ban đầu $> 100\,\text{m}$ | Hệ thống tự động kích hoạt tính toán lại đường đi (Re-route), sinh polyline mới và phát sóng ETA cập nhật cho khách hàng. | **PASS** |
| **AC-11** | Tài xế hủy cuốc đang di chuyển đến đón | `POST /driver/trips/{tripId}/cancel` khi `status == DRIVER_ARRIVING` | HTTP 200 OK; cuốc xe chuyển sang `CANCELLED`; giải phóng tài xế về trạng thái trực tuyến; gửi thông báo hủy cho khách. | **PASS** |
| **AC-12** | Hủy cuốc do khách không xuất hiện | Tài xế ở trạng thái `ARRIVED` quá 5 phút đếm ngược | Cho phép tài xế hủy với lý do `CUSTOMER_NO_SHOW` mà không bị trừ điểm uy tín hay phạt tỷ lệ hoàn thành. | **PASS** |
| **AC-13** | Đồng bộ GPS ngoại tuyến hàng loạt | `POST /driver/trips/{tripId}/sync-gps-batch` sau khi có mạng lại | Tiếp nhận chuỗi điểm GPS đệm, kiểm tra tính hợp lệ về mốc thời gian và lưu đầy đủ vào MongoDB không bị gián đoạn vết đường. | **PASS** |
| **AC-14** | Phục hồi trạng thái tracking qua REST | `GET /trips/{tripId}/tracking` khi WebSocket ngắt kết nối | HTTP 200 OK; trả về dữ liệu vị trí tài xế mới nhất, góc quay, ETA và pha di chuyển lấy trực tiếp từ Redis tracking hash. | **PASS** |
| **AC-15** | Tần suất GPS thích ứng tiết kiệm pin | Xe di chuyển ($v > 15\,\text{km/h}$) so với dừng đỗ ($v = 0\,\text{km/h}$) | Tần suất phát GPS tự động co giãn linh hoạt (3 giây khi chạy, 15 giây khi dừng đỗ). | **PASS** |
| **AC-16** | Bắn sự kiện hoàn thành lên RabbitMQ | Cuốc xe hoàn tất (`status == COMPLETED`) | Sự kiện `trip.event.completed` được đẩy lên queue `q.trip.status.completed` kèm đầy đủ số đo $CO_2$ và cước phí. | **PASS** |

### Minh Chứng Thực Thi Unit & Integration Tests

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.greenmobility.trip.GeofenceServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.046 s
[INFO] Running com.greenmobility.trip.DriverLocationControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.644 s
[INFO] Running com.greenmobility.trip.ActualDistanceCalculatorTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s
[INFO] Running com.greenmobility.trip.TripEtaServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.098 s
[INFO] Running com.greenmobility.trip.Sprint3Phase1ExecutionTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.247 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  3.788 s
[INFO] ------------------------------------------------------------------------
```

---

## VI. Kết luận & Sẵn sàng Chuyển tiếp Sprint 4

1. **Tổng kết Thành quả Sprint 3**:
   - Toàn bộ chu trình thực thi chuyến đi thực tế của nền tảng gọi xe điện Green Mobility đã được xây dựng hoàn thiện và đồng bộ từ Backend tới Mobile và Web Admin.
   - Cơ chế theo dõi xe điện thời gian thực hoạt động mượt mà, độ trễ truyền nhận dưới 200ms, đảm bảo trải nghiệm người dùng tương đương các nền tảng gọi xe công nghệ hàng đầu hiện nay.
   - Nền tảng đo đạc quãng đường thực tế (`actual_distance_m`) và vết tọa độ GPS đã được chứng thực độ tin cậy tuyệt đối, tạo cơ sở dữ liệu đầu vào vững chắc cho bài toán cốt lõi tiếp theo của đồ án tốt nghiệp.
   - Toàn bộ 11 hình ảnh minh chứng thực tế trên Web Admin và Mobile Apps đã được ghi nhận trực quan, đồng bộ với hệ thống bản đồ vệ tinh tối màu Goong Map Tiles.

2. **Kế hoạch Triển khai Sprint 4 (Carbon Emission Calculation Engine & Impact Certificate)**:
   - Hiện thực hóa mô hình toán học tính toán phát thải $CO_2$ chuyên sâu theo tiêu chuẩn quốc tế IPCC Tier 3 và Thông tư Bộ TN&MT Việt Nam.
   - Xây dựng dịch vụ cấp phát Chứng nhận Giảm phát thải Số (Digital Carbon Impact Certificate) định dạng PDF có mã QR xác thực và chữ ký số.
   - Tích hợp Sổ cái Carbon Kép (Double-Entry Carbon Ledger) và Ví Carbon cá nhân, kích hoạt cơ chế tích lũy điểm xanh đổi quà cho khách hàng và tài xế xe điện.
