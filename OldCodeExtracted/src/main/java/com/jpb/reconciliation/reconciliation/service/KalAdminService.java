package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.KalAdminDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

@Service
public interface KalAdminService {

    ResponseEntity<RestWithStatusList> register(KalAdminDto dto);
}
