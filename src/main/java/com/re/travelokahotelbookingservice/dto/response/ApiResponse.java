package com.re.travelokahotelbookingservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private int code;
    private String status;
    private String message;
    private T result;

    public ApiResponse(int code, String status, T result) {
        this.code = code;
        this.status = status;
        this.result = result;
    }
}

