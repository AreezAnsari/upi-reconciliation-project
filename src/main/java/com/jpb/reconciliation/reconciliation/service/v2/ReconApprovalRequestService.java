package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconApprovalRequest;

import org.springframework.http.ResponseEntity;

public interface ReconApprovalRequestService {

    ResponseEntity<RestWithStatusList> submitRequest(ReconApprovalRequest request);

    ResponseEntity<RestWithStatusList> getPendingRequests();

    ResponseEntity<RestWithStatusList> getRequestById(Long requestId);

    ResponseEntity<RestWithStatusList> getRequestsByEntityType(String entityType);

    ResponseEntity<RestWithStatusList> getRequestsByMakerId(Long makerId);

    ResponseEntity<RestWithStatusList> getRequestsByCheckerId(Long checkerId);

    ResponseEntity<RestWithStatusList> approveRequest(Long requestId, Long checkerId, String remarks);

    ResponseEntity<RestWithStatusList> rejectRequest(Long requestId, Long checkerId, String remarks);
}
