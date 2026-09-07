# Green Mobility Platform - Mobile Application Specification

> **Tài liệu**: Đặc tả Kỹ thuật Ứng dụng Di động (Mobile App Specification)  
> **Dự án**: Nền tảng gọi xe điện tích hợp hệ thống đo lường, báo cáo và khuyến khích giảm phát thải carbon  
> **Nền tảng**: Flutter 3.22+ / Dart 3.4+ (Hỗ trợ iOS 14+ và Android 8.0+)  
> **Kiến trúc**: Clean Architecture + BLoC Pattern (Business Logic Component)  
> **Ứng dụng**: Phục vụ song song 2 vai trò Khách hàng (Customer) và Tài xế (Driver)  
> **Phiên bản**: 1.0.0 (Production Blueprint)

---

## 1. Kiến trúc Tổng thể Ứng dụng Di động (Clean Architecture + BLoC)

Để tối ưu hóa mã nguồn và chia sẻ dùng chung các thư viện mạng, bản đồ, mô hình dữ liệu giữa ứng dụng Khách hàng và Tài xế, dự án được tổ chức theo mô hình **Mono-repository với Flutter Flavors** (hoặc Modular Packages):

```text
mobile/
├── packages/
│   ├── core_network/              # Dio HTTP client, JWT Interceptor, WebSocket/STOMP client
│   ├── core_ui/                   # Green Design System, Custom Theme, Colors, Fonts, Reusable Widgets
│   ├── core_map/                  # Mapbox / Google Maps wrapper, Polylines, Marker Smooth Interpolation
│   └── core_model/                # DTOs, Trip, User, CarbonReceipt, Wallet Models
│
├── apps/
│   ├── customer_app/              # Ứng dụng Khách hàng Green Mobility
│   │   ├── lib/
│   │   │   ├── presentation/      # UI Screens, Widgets, BLoC State Management
│   │   │   │   ├── auth/
│   │   │   │   ├── home_map/
│   │   │   │   ├── booking/
│   │   │   │   ├── active_trip/
│   │   │   │   ├── impact_receipt/
│   │   │   │   ├── carbon_wallet/
│   │   │   │   └── leaderboard/
│   │   │   ├── domain/            # UseCases, Repositories Interfaces
│   │   │   └── data/              # Repository Implementations, Remote DataSources
│   │   └── pubspec.yaml
│   │
│   └── driver_app/                # Ứng dụng Tài xế Green Driver
│       ├── lib/
│       │   ├── presentation/
│       │   │   ├── auth_kyc/
│       │   │   ├── face_verify/   # Face Liveness Check & Camera Capture
│       │   │   ├── home_shift/    # Bật/Tắt ca trực tuyến, Battery Guard
│       │   │   ├── dispatch_pop/  # Hộp thoại nhận cuốc đếm ngược 15s
│       │   │   ├── navigation/    # Điều hướng dẫn đường, Chuyển trạng thái cuốc
│       │   │   └── earnings_eco/  # Thống kê thu nhập & lượng CO2 giảm
│       │   ├── domain/
│       │   └── data/
│       └── pubspec.yaml
```

---

## 2. Hệ thống Thiết kế Giao diện Xanh (Green Design System)

Ứng dụng hướng tới trải nghiệm hiện đại, truyền cảm hứng về môi trường và công nghệ cao:
* **Bảng màu chủ đạo (Color Palette)**:
  - `Primary (Emerald Green)`: `#10B981` (Sắc xanh lá biểu tượng của năng lượng sạch, Net Zero).
  - `Primary Dark`: `#047857` (Dành cho button hover, status bar).
  - `Secondary (Electric Cyan)`: `#06B6D4` (Biểu tượng của xe điện và công nghệ).
  - `Background Dark (Dark Mode)`: `#0F172A` (Slate 900 - Nền tối hiện đại, tiết kiệm pin màn hình OLED).
  - `Surface Dark`: `#1E293B` (Slate 800 - Thẻ card, modal pop-up).
  - `Text Primary`: `#F8FAFC` (Trắng sáng độ tương phản cao).
  - `Accent Gold (Loyalty Points)`: `#F59E0B` (Điểm thưởng).
  - `Accent Emerald (Carbon Credits)`: `#34D399` (Tín chỉ carbon).
* **Typography**:
  - Font chính: `Outfit` kết hợp `Inter` (Google Fonts), hiển thị rõ ràng thông số cây xanh và quãng đường.

---

## 3. Đặc tả Chi tiết Ứng dụng Khách hàng (Customer App)

### 3.1. Luồng Xác thực & Hồ sơ (Authentication)
* Đăng ký/Đăng nhập bằng Số điện thoại + OTP Firebase.
* Lưu trữ an toàn JWT Token trong `flutter_secure_storage`.
* Cấu hình hồ sơ cá nhân: Tên, email, ảnh đại diện, phương thức thanh toán mặc định (VNPay, MoMo, Tiền mặt).

### 3.2. Màn hình Trang chủ & Bản đồ Sống (Home & Live Map Screen)
* **Hiển thị Bản đồ**:
  - Tích hợp Google Maps SDK / Mapbox. Tự động định vị GPS của khách hàng.
  - Render các biểu tượng xe điện xung quanh bán kính 2km (icon xe máy điện màu xanh ngọc, icon ô tô điện).
  - **Làm mượt xe di chuyển (Vehicle Marker Interpolation)**: Lắng nghe vị trí xe qua WebSocket `/topic/nearby-drivers` và sử dụng thuật toán nội suy Lerp (Linear Interpolation) để xe xoay đầu theo `bearing` và trượt mượt mà trên mặt đường, không bị giật cục.
* **Thanh tìm kiếm điểm đến**:
  - Gợi ý địa điểm tự động (Google Places Autocomplete API / Nominatim).
  - Lưu lịch sử tìm kiếm: "Nhà", "Công ty", "Trường học".

### 3.3. Luồng Đặt xe & Xem trước Tác động Môi trường (Ride Preview & Carbon Estimate)
* **Lộ trình Dự kiến**: Vẽ đường Polyline màu xanh Cyan từ điểm đón tới điểm trả.
* **Lựa chọn Phương tiện**:
  1. **E-Bike (Xe máy điện)**: VinFast Feliz S / Klara S. Hiển thị: Thời gian đến đón (3 phút) | Giá cước: 25.000đ | **Huy hiệu: Giảm 320g CO2**.
  2. **E-Car 4 chỗ (Ô tô điện Compact)**: VinFast VF e34 / VF 5. Hiển thị: Thời gian đón (5 phút) | Giá cước: 65.000đ | **Huy hiệu: Giảm 850g CO2**.
  3. **E-Car 7 chỗ (Ô tô điện SUV)**: VinFast VF 8. Hiển thị: Thời gian đón (8 phút) | Giá cước: 95.000đ | **Huy hiệu: Giảm 1.250g CO2**.
* **Hộp thoại Thông điệp Xanh (Green Impact Tooltip)**:
  - Khi bấm vào huy hiệu giảm CO2, pop-up hiển thị: *"Chuyến đi 12km bằng xe điện giúp ngăn phát thải 850g CO2 ra khí quyển, tương đương 14 ngày hấp thụ của một cây xanh thành phố!"*

### 3.4. Trạng thái Tìm kiếm & Theo dõi Cuốc xe Thời gian thực (Matching & Live Trip)
* **Radar Tìm kiếm (Dispatching Wave)**:
  - Hiệu ứng sóng radar đồng tâm màu ngọc lục bảo phát ra từ vị trí điểm đón.
  - Đồng hồ đếm ngược tối đa 30 giây: *"Đang kết nối với tài xế xe điện gần nhất..."*
* **Khi ghép thành công (Matched State)**:
  - Hộp thông tin trượt từ đáy (Bottom Sheet): Ảnh đại diện tài xế, Tên tài xế, Biển số xe, Dòng xe (VD: *VinFast VF e34 - Trắng*), Đánh giá 4.9⭐.
  - Nút gọi điện / nhắn tin nội bộ với tài xế.
  - Nút **Chia sẻ Hành trình Xanh (Share Trip)**: Gửi link web cho người thân xem trực tiếp vị trí xe đang chạy vì mục đích an toàn.
  - Cập nhật liên tục khoảng cách và ETA đón khách qua WebSocket `/topic/driver-location/{driverId}`.

### 3.5. Hóa đơn Tác động Môi trường Số (Impact Receipt Screen)
Xuất hiện ngay khi cuốc xe kết thúc và thanh toán hoàn tất:
* **Thiết kế Thẻ Hóa đơn (Green Impact Card)**:
  - Biểu tượng cây xanh 3D phát sáng.
  - Dòng chữ chúc mừng: *"Cảm ơn bạn đã lựa chọn di chuyển xanh!"*
  - **Thông số cốt lõi**:
    - Lượng $CO_2$ giảm được: **`+749.1 g CO2`** (Chữ to nổi bật).
    - Cây xanh tương đương: **`~ 12.5 Ngày cây xanh hấp thụ`** 🌲.
    - Đèn LED tương đương: **`~ 104 Giờ thắp sáng bóng LED 10W`** 💡.
  - **Phần thưởng nhận được**:
    - **`+0.7491 Tín chỉ Carbon (PCC)`** được cộng vào ví.
    - **`+85 Điểm Loyalty Points`**.
* **Nút bấm Hành động**:
  - **"Chia sẻ Tác động Xanh"**: Tự động chụp ảnh thẻ Impact Receipt đẹp mắt chia sẻ lên Story Instagram / Facebook / Zalo kèm hashtag `#GreenMobility #NetZeroVN`.
  - **"Về Trang chủ"**.

### 3.6. Ví Carbon Cá nhân & Cửa hàng Đổi quà Xanh (Carbon Wallet & Eco-Store)
* **Giao diện Ví**:
  - Thẻ tín dụng xanh số (Virtual Green Card) hiển thị:
    - Tổng $CO_2$ đã giảm lũy kế cuộc đời (Lifetime $CO_2$ Offset): `45.8 kg`.
    - Số dư Tín chỉ Carbon hiện có: `45.8250 PCC`.
    - Số dư Điểm thưởng khả dụng: `1,250 Points`.
* **Cửa hàng Quà tặng (Eco-Store)**:
  - Danh mục đổi quà:
    - Voucher giảm giá 20.000đ cước chuyến đi (Tiêu 200 điểm).
    - Đóng góp 1 cây rừng ngập mặn Cần Giờ (Tiêu 5.0 PCC + 500 điểm).
    - Voucher cà phê hữu cơ Highland/The Coffee House.
* **Bảng xếp hạng (Green Leaderboard)**:
  - Tab tuần / tháng.
  - Xếp hạng người dùng giảm phát thải nhiều nhất khu vực TP.HCM.
  - Hệ thống huy hiệu (Badges):
    - *Eco Seedling* (Hoàn thành cuốc đầu tiên).
    - *Carbon Crusher* (Giảm được 10kg $CO_2$).
    - *Net Zero Pioneer* (Đi trên 100km xe điện).

---

## 4. Đặc tả Chi tiết Ứng dụng Tài xế (Driver App)

### 4.1. Luồng Đăng ký & Nộp hồ sơ Xe điện (Driver KYC Flow)
* Chụp ảnh CCCD 2 mặt (tự động nhận diện OCR họ tên, số CCCD).
* Chụp ảnh Giấy phép lái xe (GPLX).
* Nhập thông số kỹ thuật xe điện:
  - Chọn Hãng & Dòng xe từ danh mục (VinFast, Dat Bike, Yadea...).
  - Nhập dung lượng pin thiết kế ($kWh$) và biển số xe.
  - Chụp ảnh Cà vẹt xe và Giấy chứng nhận kiểm định an toàn kỹ thuật.
* Chụp ảnh chân dung góc chính diện (dùng làm ảnh Face Embedding gốc).

### 4.2. Xác minh Khuôn mặt Sinh trắc học khi Bắt đầu Ca (Face Verification Check)
* **Yêu cầu Bắt buộc**: Tài xế không thể gạt nút "Trực tuyến" (Go Online) nếu chưa vượt qua bước này trong ngày.
* **Quy trình Thực hiện**:
  1. Màn hình mở camera trước với khung hình bầu dục căn khuôn mặt.
  2. Hướng dẫn tài xế: *"Vui lòng giữ thẳng khuôn mặt và chớp mắt nhẹ"* (Phát hiện chuyển động sống Liveness Detection chống dùng ảnh in sẵn).
  3. Ứng dụng chụp ảnh và gọi API `POST /driver/shift/face-verify`.
  4. Nếu khớp với độ chính xác $\ge 75\%$, hiển thị thông báo thành công và chuyển sang giao diện Bật ca.
  5. Nếu thất bại sau 3 lần, tạm khóa tính năng bật ca trong 15 phút và gửi cảnh báo lên Web Admin.

### 4.3. Bảng điều khiển Bật/Tắt Ca & Giám sát Pin (Shift Control & Battery Guard)
* Nút gạt chuyển trạng thái: `OFFLINE` $\leftrightarrow$ `ONLINE`.
* **Battery Safeguard (Bảo vệ Pin)**:
  - Ứng dụng đọc dung lượng pin của điện thoại và yêu cầu tài xế khai báo % pin xe điện hiện tại.
  - Nếu pin xe dưới $20\%$, hệ thống cảnh báo và không điều phối các chuyến đi đường dài ($>10km$) để tránh chết máy giữa đường.

### 4.4. Hộp thoại Nhận cuốc xe (Ride Dispatching Pop-up)
Khi hệ thống đẩy cuốc xe qua WebSocket `/user/queue/ride-dispatch`:
* Ứng dụng phát âm thanh chuông báo lớn và rung điện thoại liên tục.
* Bật sáng màn hình (kể cả khi app đang chạy nền).
* **Nội dung hiển thị trên Pop-up**:
  - Vòng tròn đếm ngược màu đỏ cam **15 giây**.
  - Khoảng cách từ vị trí hiện tại đến điểm đón khách (VD: `1.2 km - 4 phút`).
  - Lộ trình: Điểm đón $\rightarrow$ Điểm trả.
  - Tổng thu nhập chuyến đi dự kiến: `+68.000đ` (đã trừ phí nền tảng).
  - Lượng $CO_2$ xe điện sẽ đóng góp giảm: `+740 g CO2`.
* **2 Nút bấm**:
  - **Nút "Chấp nhận" (Màu xanh Emerald)**: Nhận cuốc xe ngay lập tức, chuyển sang màn hình điều hướng.
  - **Nút "Từ chối" (Màu xám)**: Bỏ qua cuốc xe, hệ thống tự động chuyển sang tài xế kế tiếp.

### 4.5. Màn hình Điều hướng & Các Bước Thực hiện Cuốc xe (Trip Execution)
* **Giai đoạn 1: Đang đến đón khách (`DRIVER_ARRIVING`)**:
  - Bản đồ hiển thị đường đi ngắn nhất đến điểm đón khách.
  - Tích hợp nút mở Google Maps ngoài hoặc điều hướng trực tiếp trong app qua OSRM Turn-by-Turn.
  - Khi xe đi vào bán kính Geofence 50m quanh điểm đón, app tự động kích hoạt hoặc tài xế bấm nút: **"Đã đến điểm đón"** $\rightarrow$ Hệ thống chuyển sang trạng thái `ARRIVED` và gửi thông báo cho khách.
* **Giai đoạn 2: Bắt đầu chở khách (`IN_TRIP`)**:
  - Khi khách lên xe, tài xế bấm nút trượt: **"Bắt đầu chuyến đi"** (Slide to Start Trip).
  - Bản đồ chuyển sang lộ trình dẫn đến điểm trả khách.
* **Giai đoạn 3: Trả khách & Hoàn tất (`COMPLETED`)**:
  - Khi tới điểm trả, tài xế bấm nút trượt: **"Hoàn thành chuyến đi"** (Slide to Complete Trip).
  - Màn hình tổng kết thu nhập hiển thị: Cước chuyến đi, Thu nhập thực nhận, $CO_2$ đã giúp giảm, Điểm đánh giá từ khách.

### 4.6. Dịch vụ Định vị Chạy nền & Tối ưu Pin (Background Geolocation Tracking)
* Sử dụng package `flutter_background_geolocation` hoặc Foreground Service trên Android / CoreLocation Always trên iOS.
* **Cơ chế Tiết kiệm Pin**:
  - Khi xe dừng đèn đỏ hoặc đứng yên $>30$ giây: Giảm tần suất phát tọa độ xuống 15 giây/lần.
  - Khi xe di chuyển tốc độ $>15\,km/h$: Tăng tần suất phát tọa độ lên 3 giây/lần để đảm bảo bản đồ của khách hàng cập nhật tức thì.
  - Thuật toán tự động bù gói tin (Offline Sync): Khi tài xế đi vào hầm chui hoặc mất sóng 4G, các tọa độ GPS được lưu tạm trong SQLite cục bộ và tự động đồng bộ lên MongoDB khi có sóng trở lại.
