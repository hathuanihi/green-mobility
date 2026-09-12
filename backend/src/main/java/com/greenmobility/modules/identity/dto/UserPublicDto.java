package com.greenmobility.modules.identity.dto;

import java.util.UUID;

public class UserPublicDto {

    private UUID id;
    private String phoneNumber;
    private String fullName;
    private String email;
    private String role;
    private String status;

    public UserPublicDto() {}

    public UserPublicDto(UUID id, String phoneNumber, String fullName, String email, String role, String status) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
