package com.jpb.reconciliation.reconciliation.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.Data;

@Data
public class KalUserDto {

    @NotBlank
    @Pattern(regexp = "^[a-z]+\\.[a-z]+$", message = "Username must be firstname.lastname")
    private String username;

    @Email
    @Pattern(regexp = ".*@kalinfotech.com$", message = "Only KalInfotech email allowed")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[@#$%^&+=])(?=.*[0-9]).{8,}$",
        message = "Password must contain 8 chars, 1 uppercase, 1 number, 1 special char"
    )
    private String password;
}