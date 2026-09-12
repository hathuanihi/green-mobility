package com.greenmobility.modules.identity.dto;

import com.greenmobility.modules.identity.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class RegisterRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^((\\+84)|0)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam (10 chữ số)")
    private String phoneNumber;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=])[A-Za-z\\d@$!%*?&#^()_+\\-=]{8,}$",
            message = "Mật khẩu phải có tối thiểu 8 ký tự, chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String email;

    @NotNull(message = "Vai trò (Role) không được để trống")
    private Role role;

    public RegisterRequest() {}

    public RegisterRequest(String phoneNumber, String password, String fullName, String email, Role role) {
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
