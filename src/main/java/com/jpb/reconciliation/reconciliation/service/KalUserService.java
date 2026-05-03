package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.KalApiResponseDto;
import com.jpb.reconciliation.reconciliation.dto.KalUserDto;

public interface KalUserService {

    KalApiResponseDto register(KalUserDto dto);

    KalApiResponseDto login(String username, String password);
}
