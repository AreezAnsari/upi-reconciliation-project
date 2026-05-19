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
	
	// NEW: full name — required, used to auto-generate username
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 200, message = "Full name must be between 2 and 200 characters")
    private String fullName;
 
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Pattern(
            regexp = "^[A-Z][a-zA-Z]*\\.[A-Z][a-zA-Z]*$",
            message = "Username must be in First.Last format (e.g. Karan.Joshi)"
        )
    private String username;
 
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;
 
    private String department;
 
    private String designation;
 
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Mobile number must be 10-15 digits")
    private String mobileNumber;
 
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "MAKER|CHECKER|WORKER|AUDITOR|IT_OPS|SUPERVISOR|RCC_CXO", message = "Role must be MAKER or CHECKER")
    private String role;                    // "MAKER" or "CHECKER"
 
    @Pattern(regexp = "INTERNAL|EXTERNAL", message = "User type must be INTERNAL or EXTERNAL")
    @Builder.Default
    private String userType = "INTERNAL";   // "INTERNAL" or "EXTERNAL"
 
 // NEW: external org fields — validated conditionally in service
    private String externalDepartmentName;
    private String externalSupervisorName;
    private String externalSupervisorEmail;
    private String externalSupervisorPhone;
}
