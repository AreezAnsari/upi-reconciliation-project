package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface RetireScheduleService {

    ResponseEntity<RestWithStatusList> scheduleRetire(Long institutionId, String scheduledBy);

    ResponseEntity<RestWithStatusList> undoRetire(Long institutionId, String undoneBy);
}