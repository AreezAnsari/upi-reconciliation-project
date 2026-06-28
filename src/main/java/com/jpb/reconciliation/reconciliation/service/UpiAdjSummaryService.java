package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

import java.util.List;
import java.util.Map;

@Service
public interface UpiAdjSummaryService {

    ResponseEntity<RestWithStatusList> getAdjSummaryByType();

}