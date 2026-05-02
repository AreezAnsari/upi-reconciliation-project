package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.KalEmployeeDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public interface KalAuthService {

    ResponseEntity<RestWithStatusList> register(KalEmployeeDto dto);
}