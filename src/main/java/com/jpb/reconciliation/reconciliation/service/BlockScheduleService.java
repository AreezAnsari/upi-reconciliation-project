package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface BlockScheduleService {

    ResponseEntity<RestWithStatusList> scheduleBlock(Long bankId, String scheduledBy, String reason);

    ResponseEntity<RestWithStatusList> undoBlock(Long bankId, String undoneBy);
}