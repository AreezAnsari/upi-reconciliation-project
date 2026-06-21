package com.jpb.reconciliation.reconciliation.dto;

import lombok.Data;

@Data
public class PasswordAndSecurityExpiryDTO {
    private String email;
    private long days;
    private String type;
}
