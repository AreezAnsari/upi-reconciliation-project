package com.jpb.reconciliation.reconciliation.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordAndSecurityUserAccessValidator {
	 public void validate(String status) {

	        if (status != null && status.equals("BLOCKED")) {
	            throw new RuntimeException("Password expired. Please reset password.");
	        }
	    }
}
