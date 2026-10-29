package com.re.travelokahotelbookingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterRequest {
    @NotBlank(message = "Họ tên không được bỏ trống")
    private String fullName;

    @NotBlank(message = "Email không được bỏ trống")
    private String email;

    @NotBlank(message = "Phone không được để trống")
    @Pattern(regexp = "^(0|\\\\+84)(3|5|7|8|9)[0-9]{8}$",
            message = "Số điện thoại không hợp lệ")
    private String phone;

    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).*$",
            message = "Mật khẩu phải có ít nhất 1 chữ cái viết hoa và 1 chữ số")
    private String password;

    private String confirmPassword;
}
