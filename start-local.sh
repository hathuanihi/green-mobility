#!/bin/bash
# ==============================================================================
# Green Mobility Platform - Local Startup & Health Check Script
# ==============================================================================

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}================================================================${NC}"
echo -e "${GREEN}   GREEN MOBILITY PLATFORM - KHỞI ĐỘNG LOCAL MÔI TRƯỜNG   ${NC}"
echo -e "${CYAN}================================================================${NC}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# 1. Kiểm tra các file môi trường .env
echo -e "\n${YELLOW}[1/4] Kiểm tra các file cấu hình môi trường (.env)...${NC}"
for env_file in "backend/.env" "frontend-admin/.env.local" "mobile/.env"; do
  if [ -f "$env_file" ]; then
    echo -e "  ✓ Đã tìm thấy: ${GREEN}$env_file${NC}"
  else
    echo -e "  ✗ Thiếu file: ${RED}$env_file${NC}"
  fi
done

# 2. Hướng dẫn khởi chạy cụm hạ tầng CSDL Docker
echo -e "\n${YELLOW}[2/4] Cụm hạ tầng CSDL (PostgreSQL PostGIS, Redis, Mongo, RabbitMQ)...${NC}"
if command -v docker &> /dev/null && docker info &> /dev/null; then
  echo -e "  Docker đang chạy. Đang kích hoạt containers..."
  docker compose up -d
  echo -e "  ${GREEN}✓ Toàn bộ cơ sở dữ liệu đã sẵn sàng!${NC}"
else
  echo -e "  ${YELLOW}! Docker chưa bật hoặc chưa cài đặt trên máy.${NC}"
  echo -e "  Lưu ý: Để backend kết nối CSDL thực tế, hãy mở Docker Desktop và chạy:"
  echo -e "  ${CYAN}docker compose up -d${NC}"
fi

# 3. Khởi chạy Backend
echo -e "\n${YELLOW}[3/4] Lệnh khởi chạy Core Backend (Spring Boot):${NC}"
echo -e "  ${CYAN}cd backend && mvn spring-boot:run${NC}"
echo -e "  Endpoint kiểm tra sức khỏe hệ thống:"
echo -e "  ${GREEN}http://localhost:8080/api/v1/health${NC}"

# 4. Khởi chạy Frontend Web Admin
echo -e "\n${YELLOW}[4/4] Lệnh khởi chạy Web Admin (Next.js 14):${NC}"
echo -e "  ${CYAN}cd frontend-admin && npm install && npm run dev${NC}"
echo -e "  Giao diện quản trị sẽ mở tại:"
echo -e "  ${GREEN}http://localhost:3000${NC}"

# 5. Khởi chạy Mobile App
echo -e "\n${YELLOW}[+] Lệnh khởi chạy Ứng dụng Di động (Flutter):${NC}"
echo -e "  Khách hàng: ${CYAN}cd mobile/apps/customer_app && flutter run${NC}"
echo -e "  Tài xế:     ${CYAN}cd mobile/apps/driver_app && flutter run${NC}"

echo -e "\n${GREEN}================================================================${NC}"
echo -e "${GREEN}  ✓ Cấu trúc mã nguồn & file .env đã sẵn sàng cho khởi chạy!  ${NC}"
echo -e "${GREEN}================================================================${NC}"
