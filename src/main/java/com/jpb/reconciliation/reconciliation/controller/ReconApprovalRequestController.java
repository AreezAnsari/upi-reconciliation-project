package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconApprovalRequest;
import com.jpb.reconciliation.reconciliation.service.ReconApprovalRequestService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/approval")
@CrossOrigin(origins = "*")
public class ReconApprovalRequestController {

    private static final Logger logger = LoggerFactory.getLogger(ReconApprovalRequestController.class);

    @Autowired
    private ReconApprovalRequestService reconApprovalRequestService;

    @Operation(summary = "Submit an approval request")
    @PostMapping(value = "/submit", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> submitRequest(@RequestBody ReconApprovalRequest request) {
        logger.info("Approval request submitted: entityType={}, entityId={}", request.getEntityType(), request.getEntityId());
        return reconApprovalRequestService.submitRequest(request);
    }

    @Operation(summary = "Get all pending approval requests")
    @GetMapping(value = "/pending", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getPendingRequests() {
        return reconApprovalRequestService.getPendingRequests();
    }

    @Operation(summary = "Get approval request by ID")
    @GetMapping(value = "/get/{requestId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getRequestById(@PathVariable Long requestId) {
        return reconApprovalRequestService.getRequestById(requestId);
    }

    @Operation(summary = "Get approval requests by entity type")
    @GetMapping(value = "/get-by-entity-type", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getRequestsByEntityType(@RequestParam String entityType) {
        return reconApprovalRequestService.getRequestsByEntityType(entityType);
    }

    @Operation(summary = "Get approval requests by maker ID")
    @GetMapping(value = "/get-by-maker/{makerId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getRequestsByMakerId(@PathVariable Long makerId) {
        return reconApprovalRequestService.getRequestsByMakerId(makerId);
    }

    @Operation(summary = "Get approval requests by checker ID")
    @GetMapping(value = "/get-by-checker/{checkerId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> getRequestsByCheckerId(@PathVariable Long checkerId) {
        return reconApprovalRequestService.getRequestsByCheckerId(checkerId);
    }

    @Operation(summary = "Approve an approval request")
    @PostMapping(value = "/approve/{requestId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> approveRequest(
            @PathVariable Long requestId,
            @RequestParam Long checkerId,
            @RequestParam(required = false, defaultValue = "") String remarks) {
        logger.info("Approve request: {} by checkerId={}", requestId, checkerId);
        return reconApprovalRequestService.approveRequest(requestId, checkerId, remarks);
    }

    @Operation(summary = "Reject an approval request")
    @PostMapping(value = "/reject/{requestId}", produces = CommonConstants.APPLICATION_JSON)
    public ResponseEntity<RestWithStatusList> rejectRequest(
            @PathVariable Long requestId,
            @RequestParam Long checkerId,
            @RequestParam(required = false, defaultValue = "") String remarks) {
        logger.info("Reject request: {} by checkerId={}", requestId, checkerId);
        return reconApprovalRequestService.rejectRequest(requestId, checkerId, remarks);
    }
}
