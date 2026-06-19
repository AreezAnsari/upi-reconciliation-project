package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.http.ResponseEntity;

public interface UserAuthService {

    ResponseEntity<RestWithStatusList> verifyCredentials(String bankCode, String username, String defaultPassword);

    ResponseEntity<RestWithStatusList> setPassword(String bankCode, String username, String newPassword);

    ResponseEntity<RestWithStatusList> login(String bankCode, String username, String password);

    ResponseEntity<RestWithStatusList> verifyOtp(String email, String otp);

    ResponseEntity<RestWithStatusList> checkStatus(String bankCode, String username);

    ResponseEntity<RestWithStatusList> getAccountStatus(String email);

    ResponseEntity<RestWithStatusList> forgotPassword(String email);

    ResponseEntity<RestWithStatusList> resetPassword(String email, String otp, String newPassword, String confirmNewPassword);
}
