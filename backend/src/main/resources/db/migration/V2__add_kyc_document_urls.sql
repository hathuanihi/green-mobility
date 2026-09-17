-- V2__add_kyc_document_urls.sql: Bổ sung các cột lưu trữ URL ảnh tài liệu KYC cho bảng driver_profiles

ALTER TABLE driver_profiles
    ADD COLUMN IF NOT EXISTS citizen_card_front_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS citizen_card_back_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS driver_license_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS face_portrait_url VARCHAR(500);
