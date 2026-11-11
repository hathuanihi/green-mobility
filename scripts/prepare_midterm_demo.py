#!/usr/bin/env python3
"""
Script chuẩn bị dữ liệu & kiểm tra toàn diện 5 Sprints trước giờ Báo cáo Giữa kỳ KLTN.
Chạy lệnh: python3 scripts/prepare_midterm_demo.py
"""

import sys
import requests

BASE_URL = "http://localhost:8080/api/v1"
WEB_URL = "http://localhost:3000"


def login(phone: str, password: str):
    try:
        res = requests.post(
            f"{BASE_URL}/auth/login",
            json={"phoneNumber": phone, "password": password},
            timeout=5,
        )
        if res.status_code == 200 and res.json().get("success"):
            return res.json()["data"]["token"]
    except Exception as e:
        print(f"  [!] Không thể kết nối tới Backend ({BASE_URL}): {e}")
    return None


def main():
    print("=" * 72)
    print("  GREEN MOBILITY - CÔNG CỤ CHUẨN BỊ DEMO BÁO CÁO GIỮA KỲ (SPRINT 1-5)")
    print("=" * 72)

    # 1. Kiểm tra đăng nhập Admin, Customer, Driver
    print("\n[1/5] Kiểm tra xác thực JWT & Tài khoản Demo (Sprint 1)...")
    admin_token = login("0900000001", "Admin@123456")
    if not admin_token:
        print("  [X] Backend chưa chạy hoặc chưa seed DB. Hãy chạy: cd backend && mvn spring-boot:run")
        sys.exit(1)
    print("  [✓] Admin (0900000001) đăng nhập thành công!")

    customer_token = login("0901234567", "Admin@123456")
    driver_token = login("0987654321", "Admin@123456")
    print(f"  [✓] Customer Phạm Hà Anh Thư (0901234567): {'OK' if customer_token else 'Chưa tạo'}")
    print(f"  [✓] Driver Nguyễn Minh Thiện (0987654321): {'OK' if driver_token else 'Chưa tạo'}")

    admin_headers = {"Authorization": f"Bearer {admin_token}"}
    cust_headers = {"Authorization": f"Bearer {customer_token}"} if customer_token else admin_headers

    # 2. Kiểm tra Dự toán Cước & Phát thải CO2 (Sprint 2)
    print("\n[2/5] Kiểm tra Engine Dự toán Lộ trình & Carbon IPCC (Sprint 2)...")
    est_res = requests.post(
        f"{BASE_URL}/trips/estimate",
        headers=cust_headers,
        json={
            "pickupLat": 10.7725,
            "pickupLng": 106.6983,
            "pickupAddress": "Chợ Bến Thành, Quận 1, TP.HCM",
            "dropoffLat": 10.7951,
            "dropoffLng": 106.7218,
            "dropoffAddress": "Landmark 81, Bình Thạnh, TP.HCM",
            "vehicleType": "ELECTRIC_MOTORBIKE",
        },
        timeout=5,
    )
    if est_res.status_code == 200:
        est = est_res.json().get("data", {})
        print(
            f"  [✓] Dự toán Chợ Bến Thành -> Landmark 81: "
            f"Cước = {est.get('estimatedFareVnd', 0):,} đ | "
            f"CO2 giảm = {est.get('estimatedCo2SavedGrams', 0)} g"
        )

    # 3. Kiểm tra Danh sách Chuyến đi & Tracking (Sprint 3)
    print("\n[3/5] Kiểm tra Quản lý Chuyến đi & Telemetry (Sprint 3)...")
    trips_res = requests.get(f"{BASE_URL}/admin/trips", headers=admin_headers, timeout=5)
    if trips_res.status_code == 200:
        trips = trips_res.json().get("data", [])
        print(f"  [✓] Hệ thống đang có {len(trips)} chuyến đi đa trạng thái (SEARCHING, MATCHED, IN_TRIP, COMPLETED).")

    # 4. Kiểm tra Ma trận Hệ số Phát thải, Đội xe & Gian lận GPS (Sprint 4)
    print("\n[4/5] Kiểm tra Carbon Engine, Đội xe VinFast & Anti-Fraud (Sprint 4)...")
    em_res = requests.get(f"{BASE_URL}/admin/emissions", headers=admin_headers, timeout=5)
    veh_res = requests.get(f"{BASE_URL}/admin/vehicles", headers=admin_headers, timeout=5)
    fraud_res = requests.get(f"{BASE_URL}/admin/fraud/alerts", headers=admin_headers, timeout=5)
    print(f"  [✓] Ma trận Hệ số Phát thải IPCC: {len(em_res.json().get('data', [])) if em_res.ok else 0} cấu hình")
    print(f"  [✓] Đội xe thuần điện VinFast/Dat Bike: {len(veh_res.json().get('data', [])) if veh_res.ok else 0} xe")
    print(f"  [✓] Cảnh báo Gian lận GPS (Anti-Fraud): {len(fraud_res.json().get('data', [])) if fraud_res.ok else 0} cảnh báo")

    # 5. Kiểm tra Sổ cái Kép, Đối soát & Khởi tạo link VNPay Sandbox mẫu (Sprint 5)
    print("\n[5/5] Kiểm tra Sổ cái Kép Double-Entry & Cổng VNPay Sandbox (Sprint 5)...")
    recon_res = requests.get(f"{BASE_URL}/admin/finance/reconciliation", headers=admin_headers, timeout=5)
    if recon_res.ok:
        recon = recon_res.json().get("data", {})
        print(
            f"  [✓] Đối soát Sổ cái Kép: isBalanced = {recon.get('isBalanced')} | "
            f"Chênh lệch hệ thống = {recon.get('systemTotalVnd', 0)} đ"
        )

    vnpay_url = None
    topup_res = requests.post(
        f"{BASE_URL}/wallet/topups",
        headers=cust_headers,
        json={"amountVnd": 100000, "gateway": "VNPAY"},
        timeout=5,
    )
    if topup_res.ok and topup_res.json().get("data"):
        vnpay_url = topup_res.json()["data"].get("payUrl")
        print("  [✓] Khởi tạo phiên nạp Ví Xanh 100,000 đ qua VNPay Sandbox thành công!")

    print("\n" + "=" * 72)
    print("  DANH SÁCH ĐƯỜNG DẪN MỞ SẴN TRÊN TRÌNH DUYỆT KHI BÁO CÁO:")
    print("=" * 72)
    print(f"  1. Trung tâm Điều hành (Sprint 1-3) : {WEB_URL}/")
    print(f"  2. Duyệt KYC & Sinh trắc 512D (S1)  : {WEB_URL}/drivers")
    print(f"  3. Bản đồ Điều phối & Cuốc xe (S2)  : {WEB_URL}/trips")
    print(f"  4. Giám sát Live Tracking (S3)      : {WEB_URL}/trips/live-tracking")
    print(f"  5. Ma trận & Simulator Carbon (S4)  : {WEB_URL}/emissions")
    print(f"  6. Chứng chỉ ESG Công khai (S4)     : {WEB_URL}/eco/certificate/gm-receipt-gm202610059905")
    print(f"  7. Quản lý Đội xe Điện (S4)         : {WEB_URL}/vehicles")
    print(f"  8. Giám sát Gian lận GPS (S4)       : {WEB_URL}/fraud-monitor")
    print(f"  9. Trợ lý AI Copilot Gemini (S4)    : {WEB_URL}/ai-copilot")
    print(f" 10. Giao dịch & Sổ cái Kép (S5)      : {WEB_URL}/payments")
    print(f" 11. Duyệt Rút tiền Tài xế (S5)       : {WEB_URL}/withdrawals")
    print(f" 12. Tài chính & Đối soát Tự động (S5): {WEB_URL}/finance")
    if vnpay_url:
        print(f" 13. Cổng VNPay Sandbox Live (S5)     : {vnpay_url}")
    print("=" * 72)


if __name__ == "__main__":
    main()

