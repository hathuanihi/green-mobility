package com.greenmobility.modules.identity.dto;

import java.time.Instant;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String phoneNumber;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;
    private String status;
    private Instant createdAt;

    public UserResponse() {}

    public UserResponse(UUID id, String phoneNumber, String email, String fullName, String avatarUrl, String role, String status, Instant createdAt) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
