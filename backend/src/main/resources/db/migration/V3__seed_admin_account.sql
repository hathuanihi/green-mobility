-- V3__seed_admin_account.sql: Seed Default Administrator Account for Platform Management
-- Phone: 0900000001
-- Password: Admin@123456
-- BCrypt Hash: $2a$12$JCA1zawBxCOUWZw7NzaX5eVR3waCxB3TjBg7.gweyHP/dPDdo5/Ga

INSERT INTO users (id, phone_number, email, password_hash, full_name, role, status)
VALUES (
    uuid_generate_v4(),
    '0900000001',
    'admin@greenmobility.vn',
    '$2a$12$JCA1zawBxCOUWZw7NzaX5eVR3waCxB3TjBg7.gweyHP/dPDdo5/Ga',
    'Quản trị viên Hệ thống (Demo)',
    'ROLE_ADMIN',
    'ACTIVE'
) ON CONFLICT (phone_number) DO UPDATE 
SET password_hash = EXCLUDED.password_hash,
    role = 'ROLE_ADMIN',
    status = 'ACTIVE';
