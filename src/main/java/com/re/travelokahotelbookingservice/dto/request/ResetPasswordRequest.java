package com.re.travelokahotelbookingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "Token không được bỏ trống")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{6,}$",
            message = "Mật khẩu phải có ít nhất 1 chữ cái viết hoa và 1 chữ số")
    private String newPassword;
}
