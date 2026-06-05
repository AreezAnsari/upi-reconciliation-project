package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.MainAdminVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.KalUserDto;

public interface KalUserService {

    MainAdminVerifyDto register(KalUserDto dto);

    MainAdminVerifyDto login(String username, String password);
}
