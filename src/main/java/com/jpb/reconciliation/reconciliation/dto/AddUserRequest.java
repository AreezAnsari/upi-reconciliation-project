package com.jpb.reconciliation.reconciliation.dto;

import javax.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddUserRequest {
 
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;
 
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;
 
    private String department;
 
    private String designation;
 
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Mobile number must be 10-15 digits")
    private String mobileNumber;
 
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "MAKER|CHECKER", message = "Role must be MAKER or CHECKER")
    private String role;                    // "MAKER" or "CHECKER"
 
    @Pattern(regexp = "INTERNAL|EXTERNAL", message = "User type must be INTERNAL or EXTERNAL")
    @Builder.Default
    private String userType = "INTERNAL";   // "INTERNAL" or "EXTERNAL"
 
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
