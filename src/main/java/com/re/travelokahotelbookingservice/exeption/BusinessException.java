package com.re.travelokahotelbookingservice.exeption;

public class BusinessException extends RuntimeException{
    public BusinessException(String message) {
        super(message);
    }
}
