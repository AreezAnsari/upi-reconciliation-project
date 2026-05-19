package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KalVerifyEmailResponseDto {

    private String userStatus;
    private String institutionCode;
    private String username;
}