package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.AdminReplacementRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import org.springframework.http.ResponseEntity;

public interface AdminReplacementService {

    ResponseEntity<RestWithStatusList> replace(AdminReplacementRequest request, String replacedBy);
}
