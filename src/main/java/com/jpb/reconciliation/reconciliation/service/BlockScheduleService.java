package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface BlockScheduleService {

    ResponseEntity<RestWithStatusList> scheduleBlock(Long institutionId, String scheduledBy);

    ResponseEntity<RestWithStatusList> undoBlock(Long institutionId, String undoneBy);
}