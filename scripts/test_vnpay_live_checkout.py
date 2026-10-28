import time
import os
import requests
from playwright.sync_api import sync_playwright

output_dir = os.path.abspath(r"docs\sprints\assets\SPRINT_5")
os.makedirs(output_dir, exist_ok=True)

# 1. Login to get JWT
login_res = requests.post("http://localhost:8080/api/v1/auth/login", json={
    "phoneNumber": "0900000001",
    "password": "Admin@123456"
})
token = login_res.json()["data"]["token"]

# 2. Call create topup via VNPAY
topup_res = requests.post(
    "http://localhost:8080/api/v1/wallet/topups",
    json={"amountVnd": 100000, "gateway": "VNPAY"},
    headers={"Authorization": f"Bearer {token}"}
)
topup_data = topup_res.json()["data"]
pay_url = topup_data["payUrl"]

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(
        viewport={"width": 1280, "height": 900},
        device_scale_factor=2
    )
    page = context.new_page()

    # 1. Main portal
    page.goto(pay_url, wait_until="networkidle", timeout=30000)
    time.sleep(2)

    # 2. Click "Thẻ nội địa và tài khoản ngân hàng"
    atm_opt = page.locator("text='Thẻ nội địa và tài khoản ngân hàng'").first
    if atm_opt.count() > 0:
        atm_opt.click()
        time.sleep(2)

        # 3. Click NCB bank test
        ncb_btn = page.locator("text='NCB', [alt*='NCB']").first
        if ncb_btn.count() > 0:
            ncb_btn.click()
            time.sleep(3)
            screenshot_3 = os.path.join(output_dir, "vnpay_sandbox_card_input.png")
            page.screenshot(path=screenshot_3)
            print("Saved card input screenshot")

    browser.close()
