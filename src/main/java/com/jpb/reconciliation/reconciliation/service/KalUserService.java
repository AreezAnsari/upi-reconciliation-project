package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.KalUserDto;

public interface KalUserService {
    String register(KalUserDto dto);
    String login(String username, String password);
}
