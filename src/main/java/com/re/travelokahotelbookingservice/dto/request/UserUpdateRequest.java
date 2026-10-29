package com.re.travelokahotelbookingservice.dto.request;

import com.re.travelokahotelbookingservice.entity.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank(message = "Tên không được để trống")
    private String fullName;

    @Pattern(regexp = "^$|(0|\\\\+84)(3|5|7|8|9)[0-9]{8}$",
            message = "Số điện thoại không hợp lệ")
    private String phone;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate yob;

    private Gender gender;

    private String address;
}
