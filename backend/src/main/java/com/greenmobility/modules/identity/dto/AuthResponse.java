package com.greenmobility.modules.identity.dto;

import java.util.UUID;

public class AuthResponse {

    private UUID userId;
    private String phoneNumber;
    private String fullName;
    private String role;
    private String token;
    private long expiresIn;
    private String kycStatus; // Dành cho Driver (PENDING, APPROVED, REJECTED hoặc null nếu là Customer)

    public AuthResponse() {}

    public AuthResponse(UUID userId, String phoneNumber, String fullName, String role, String token, long expiresIn, String kycStatus) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
        this.role = role;
        this.token = token;
        this.expiresIn = expiresIn;
        this.kycStatus = kycStatus;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }
}
