package com.jpb.reconciliation.reconciliation.dto;

public class ForgotPasswordResponseDto {

    private String message;

    public ForgotPasswordResponseDto() {
    }

    public ForgotPasswordResponseDto(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}