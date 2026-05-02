package com.jpb.reconciliation.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class KalEmployeeDto {

    // firstname.lastname — all lowercase, validated in service
    private String username;

    // @kalinfotech.com only
    private String email;

    // 10-digit mobile — matches Create.jsx "phone" field
    private String phone;

    // Raw password — will be encoded before saving
    private String password;

    // Optional — designation like "Admin", "Manager"
    private String designation;

    // Optional — full name (can be derived from username if not provided)
    private String fullName;
}