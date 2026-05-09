package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.KalSuperUserVerifyDto;
import com.jpb.reconciliation.reconciliation.dto.KalUserDto;

public interface KalUserService {

    KalSuperUserVerifyDto register(KalUserDto dto);

    KalSuperUserVerifyDto login(String username, String password);
}
