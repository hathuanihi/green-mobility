# Green Mobility Platform - Web Admin Portal Specification

> **Tài liệu**: Đặc tả Kỹ thuật Cổng Quản trị Web (Admin Portal Specification)  
> **Dự án**: Nền tảng gọi xe điện tích hợp hệ thống đo lường, báo cáo và khuyến khích giảm phát thải carbon  
> **Nền tảng**: Next.js 14+ (App Router), React 18, TypeScript, Tailwind CSS  
> **Thư viện UI & Trực quan hóa**: Shadcn UI (Radix Primitives), Lucide React, Recharts, Mapbox GL JS / Leaflet  
> **Phiên bản**: 1.0.0 (Production Blueprint)

---

## 1. Kiến trúc Cổng Quản trị Web Admin (Next.js App Router)

Giao diện Web Admin được xây dựng theo chuẩn hiện đại, responsive, hỗ trợ Dark Mode tối ưu cho các trung tâm điều hành vận hành (Operations Center):

```text
frontend-admin/
├── src/
│   ├── app/                           # Next.js 14 App Router
│   │   ├── (auth)/
│   │   │   └── login/page.tsx
│   │   │
│   │   ├── (dashboard)/               # Dashboard Layout dùng chung Sidebar & Header
│   │   │   ├── layout.tsx
│   │   │   ├── page.tsx               # Tổng quan Hệ thống (Green Overview)
│   │   │   ├── drivers/               # Quản lý tài xế, duyệt KYC & Biometrics
│   │   │   │   ├── page.tsx
│   │   │   │   └── [id]/page.tsx
│   │   │   ├── vehicles/              # Quản lý phương tiện xe điện, Pin & Đăng kiểm
│   │   │   ├── trips/                 # Giám sát chuyến đi & Replay hành trình GPS
│   │   │   │   ├── page.tsx
│   │   │   │   └── [id]/replay/page.tsx
│   │   │   ├── emissions/             # Cấu hình Hệ số Phát thải Carbon (EF Matrix)
│   │   │   │   └── page.tsx
│   │   │   ├── incentives/            # Quản lý Đổi thưởng & Đối soát Sổ cái kép
│   │   │   │   ├── catalog/page.tsx
│   │   │   │   └── ledger-audit/page.tsx
│   │   │   ├── fraud-monitor/         # Giám sát Gian lận GPS & Isolation Forest
│   │   │   │   └── page.tsx
│   │   │   └── ai-copilot/            # Trợ lý ảo Chatbot quản trị viên
│   │   │       └── page.tsx
│   │   │
│   │   └── api/auth/[...nextauth]/    # NextAuth.js session handler
│   │
│   ├── components/
│   │   ├── ui/                        # Button, Dialog, Table, Badge, Card (Shadcn UI)
│   │   ├── layout/Sidebar.tsx, Header.tsx
│   │   ├── charts/EmissionAreaChart.tsx, FleetPieChart.tsx
│   │   ├── maps/LiveOperationsMap.tsx, TripReplayMap.tsx
│   │   └── chatbot/CopilotChatWindow.tsx
│   │
│   ├── lib/
│   │   ├── api-client.ts              # Axios instance với JWT Interceptor
│   │   ├── formatters.ts              # Format tiền tệ VND, định dạng kg CO2, ngày giờ
│   │   └── websocket.ts               # SockJS / STOMP client kết nối Backend
│   │
│   └── types/                         # TypeScript interfaces (Driver, Trip, EmissionFactor)
```

---

## 2. Phân quyền Người dùng Quản trị (Role-Based Access Control - RBAC)

Hệ thống Web Admin định nghĩa 4 vai trò rõ rệt:
1. **`SUPER_ADMIN`**: Toàn quyền cấu hình hệ thống, quản lý tài khoản quản trị, thiết lập cơ chế bảo mật và phê duyệt các giao dịch đảo sổ cái kép.
2. **`OPERATIONS_MANAGER`**: Quản lý điều phối chuyến đi, theo dõi bản đồ trực tiếp toàn thành phố, can thiệp sự cố cuốc xe, phê duyệt/từ chối hồ sơ KYC của tài xế.
3. **`EMISSION_AUDITOR`**: Chuyên viên môi trường, chịu trách nhiệm cấu hình và cập nhật ma trận hệ số phát thải carbon ($EF$), xuất báo cáo ESG và kiểm kê khí nhà kính.
4. **`SUPPORT_AGENT`**: Nhân viên chăm sóc khách hàng, tiếp nhận khiếu nại chuyến đi, kiểm tra lịch sử cuốc xe và xử lý yêu cầu hoàn cước.

---

## 3. Đặc tả Chi tiết Các Phân hệ Chức năng (Functional Modules)

### 3.1. Phân hệ 1: Bảng điều khiển Trung tâm & Báo cáo Xanh (Green Executive Dashboard)
URL: `/` (Trang chủ sau đăng nhập)

* **Hàng Thẻ Chỉ số Thời gian thực (KPI Cards)**:
  - **Tổng $CO_2$ Đã Giảm Toàn Hệ Thống**: `124.85 Tấn CO2` (Tăng $+18.2\%$ so với tháng trước).
  - **Tương Đương Cây Xanh Hấp Thụ**: `2,080,833 Ngày cây xanh` 🌲.
  - **Tổng Số Chuyến Xe Xanh Thành Công**: `158,420 Chuyến`.
  - **Tài Xế Xe Điện Đang Trực Tuyến**: `1,420 Xe` (Xe máy: 980 | Ô tô: 440).
  - **Tỷ Lệ Ghép Cuốc Thành Công**: `96.4%` (Thời gian ghép TB: 18.2 giây).
* **Biểu đồ Trực quan hóa Cao cấp**:
  - **Biểu đồ Vùng Lượng Giảm Phát Thải Theo Thời Gian (Carbon Offset Trend)**: Trục X (Thời gian), Trục Y (kg $CO_2$). Thể hiện đường phát thải cơ sở của xe xăng (Baseline) so với lượng phát thải thực tế của đội xe điện.
  - **Bản đồ Nhiệt (Heatmap TP.HCM)**: Hiển thị các điểm nóng phát sinh chuyến đi xanh (Quận 1, Khu Công nghệ cao TP. Thủ Đức, Phú Mỹ Hưng Quận 7).
  - **Biểu đồ Cơ cấu Phương tiện (Fleet Distribution)**: Tỷ trọng giữa Xe máy điện (Feliz S/Klara), Ô tô 4 chỗ (VF e34/VF 5) và Ô tô 7 chỗ (VF 8).

---

### 3.2. Phân hệ 2: Quản lý Tài xế, Xe điện & Xác thực Sinh trắc học (Driver & EV Fleet Management)
URL: `/drivers`, `/vehicles`

* **Quy trình Duyệt Hồ sơ KYC (Side-by-Side Verification Screen)**:
  - Cửa sổ đối chiếu 2 cột:
    - *Cột trái*: Dữ liệu trích xuất tự động (Họ tên, Số CCCD, Hạng bằng lái, Biển số xe, Dung lượng pin).
    - *Cột phải*: Khung phóng to ảnh chụp gốc CCCD mặt trước/sau, GPLX, Cà vẹt xe và Giấy kiểm định.
  - Các nút hành động:
    - **"Phê duyệt" (Approve)**: Tự động gửi SMS/Push Notification báo tài xế có thể bật ca nhận cuốc.
    - **"Yêu cầu nộp lại" (Reject)**: Hộp thoại chọn lý do (Ảnh mờ, Giấy tờ hết hạn, Biển số không trùng khớp).
* **Giám sát Thông số Kỹ thuật Xe điện**:
  - Quản lý danh mục phương tiện: Dung lượng pin danh định ($kWh$), Suất tiêu thụ điện công bố ($kWh/km$), Ngày hết hạn đăng kiểm an toàn kỹ thuật.
  - Cảnh báo tự động trước 15 ngày khi xe sắp hết hạn kiểm định.
* **Nhật ký Xác thực Khuôn mặt Ca làm việc (Biometric Face Logs)**:
  - Danh sách từng lượt bật ca của tài xế kèm: Ảnh chụp selfie khi bật app, Ảnh chân dung gốc khi đăng ký, Điểm số tương đồng Cosine (Similarity Score $\ge 0.75$).
  - Gắn cờ cảnh báo đỏ nếu phát hiện tài xế cố tình vượt qua bằng hình ảnh tĩnh hoặc ảnh không khớp.

---

### 3.3. Phân hệ 3: Cấu hình Hệ số Phát thải Carbon (Emission Factor Configurator)
URL: `/emissions`

* **Mục tiêu**: Cung cấp công cụ cấu hình linh hoạt, minh bạch và có căn cứ khoa học cho chuyên viên môi trường.
* **Ma trận Cấu hình**:
  - Bảng dữ liệu bao gồm: Phân khúc xe, Hệ số xe xăng cơ sở ($gCO_2/km$), Suất tiêu hao điện trung bình ($kWh/km$), Hệ số phát thải lưới điện quốc gia ($gCO_2/kWh$).
  - Tự động tính toán trước kết quả: $EF_{\text{EV\_adjusted}} = SEC \times EF_{\text{grid}}$ và Lượng $CO_2$ giảm tịnh trên mỗi km.
* **Quy trình Quản lý Phiên bản (Versioning & Audit Trail)**:
  - Khi cập nhật hệ số mới, Admin bắt buộc nhập: *Văn bản / Căn cứ pháp lý* (Ví dụ: *"Cập nhật theo Báo cáo kết quả tính toán hệ số phát thải của lưới điện Việt Nam năm 2025 - Cục Biến đổi khí hậu"*).
  - Chọn ngày bắt đầu có hiệu lực (`effective_from`). Toàn bộ các chuyến đi diễn ra trước ngày này vẫn giữ nguyên kết quả tính toán cũ để đảm bảo tính bất biến lịch sử.

---

### 3.4. Phân hệ 4: Giám sát Vận hành & Tua lại Hành trình GPS (Live Map & Trip Replay)
URL: `/trips`, `/trips/[id]/replay`

* **Bản đồ Vận hành Trực tiếp (City Live Operations Map)**:
  - Tích hợp Mapbox GL JS render hàng nghìn biểu tượng xe điện đang chạy trên bản đồ TP.HCM qua WebSocket.
  - Lọc nhanh theo trạng thái xe: *Đang chở khách (Màu xanh dương)*, *Đang rảnh (Màu xanh ngọc)*, *Mất tín hiệu (Màu vàng)*.
* **Công cụ Tua lại Hành trình Chuyến đi (Trip Breadcrumbs Replay Tool)**:
  - Được sử dụng khi khách hàng khiếu nại tài xế đi đường vòng hoặc nghi ngờ gian lận.
  - Kéo thanh trượt thời gian (Timeline Scrubber) để xem lại từng giây chuyển động của xe trên bản đồ dựa vào tọa độ lưu trong MongoDB.
  - Biểu đồ tốc độ theo thời gian: Phát hiện tài xế dừng đỗ bất thường hay chạy quá tốc độ quy định.

---

### 3.5. Phân hệ 5: Quản trị Gian lận bằng Machine Learning (Fraud Detection Console)
URL: `/fraud-monitor`

* **Hệ thống Cảnh báo Bất thường (Anomaly Alert Feed)**:
  - Lắng nghe sự kiện từ mô hình **Isolation Forest** và Rule Engine.
  - Phân loại cấp độ nguy cơ:
    - 🔴 **Cực kỳ nguy hiểm (Risk Score $> 85$)**: Phát hiện Dịch chuyển tức thời (Teleportation) hoặc Sử dụng phần mềm Fake GPS (Mock Location Provider = true).
    - 🟡 **Nghi vấn cao (Risk Score $60 - 85$)**: Hai tài xế và khách hàng liên tục tạo cuốc xe trùng khớp nhau trong thời gian ngắn (Dấu hiệu cày điểm thưởng / cày tín chỉ Carbon ảo).
    - 🟢 **Bình thường / Cảnh báo nhẹ**: Sai lệch vận tốc nhẹ do nghẽn mạng.
* **Hành động Can thiệp của Admin**:
  - Nút **"Khóa tài khoản khẩn cấp (1-Click Ban)"**: Ngắt kết nối WebSocket của tài xế/khách hàng ngay lập tức.
  - Nút **"Đóng băng ví tín chỉ carbon"**: Khóa toàn bộ số dư và giao dịch đổi thưởng liên quan đến cuốc xe vi phạm.

---

### 3.6. Phân hệ 6: Quản trị Ví Tín chỉ & Đối soát Sổ cái kép (Ledger Governance)
URL: `/incentives/ledger-audit`

* **Bảng Kiểm toán Sổ cái (Double-Entry Audit Console)**:
  - Cho phép kiểm tra trạng thái toàn vẹn của toàn bộ hệ thống:
    $$\Delta = \sum \text{All Debits} - \sum \text{All Credits} \stackrel{?}{=} 0.0000$$
  - Nếu $\Delta \ne 0$, toàn màn hình nhấp nháy cảnh báo đỏ và khóa tự động tính năng đổi thưởng để ngăn thất thoát tài sản số.
* **Lịch sử Giao dịch**:
  - Tra cứu chi tiết từng bút toán: Mã giao dịch, Cuốc xe liên quan, Tài khoản nguồn, Tài khoản đích, Số lượng Tín chỉ Carbon / Điểm thưởng.

---

### 3.7. Phân hệ 7: Trợ lý AI Quản trị viên (AI Admin Copilot Chatbot)
URL: `/ai-copilot` hoặc Hộp thoại trượt góc dưới màn hình

* **Kiến trúc Trợ lý Ảo**:
  - Tích hợp mô hình ngôn ngữ lớn (LLM) kết hợp cơ chế Text-to-SQL / Function Calling an toàn để truy vấn số liệu phân tích từ cơ sở dữ liệu.
* **Kịch bản Hỏi - Đáp Hỗ trợ Vận hành**:
  - *Admin hỏi*: *"Tổng lượng CO2 giảm được tại khu vực TP. Thủ Đức trong tuần qua là bao nhiêu và tăng giảm thế nào so với tuần trước?"*
  - *AI Copilot phân tích*: Tạo câu truy vấn aggregation trên PostgreSQL `trips` $\rightarrow$ Tính toán và hiển thị câu trả lời bằng văn bản ngắn gọn kèm biểu đồ mini: *"Trong tuần qua, TP. Thủ Đức đã giảm được 14.2 tấn CO2 từ 18,320 chuyến xe điện, tăng 12.4% so với tuần trước đó."*
  - *Admin hỏi*: *"Liệt kê top 5 tài xế xe điện có quãng đường di chuyển và đóng góp giảm phát thải cao nhất hôm nay?"*
  - *AI Copilot*: Trả về bảng danh sách 5 tài xế kèm số km, số kg CO2 và biển số xe tương ứng.
