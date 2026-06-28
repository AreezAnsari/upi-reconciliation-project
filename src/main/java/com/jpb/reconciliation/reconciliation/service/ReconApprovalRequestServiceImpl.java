package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconApprovalRequest;
import com.jpb.reconciliation.reconciliation.repository.ReconApprovalRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ReconApprovalRequestServiceImpl implements ReconApprovalRequestService {

    private static final Logger logger = LoggerFactory.getLogger(ReconApprovalRequestServiceImpl.class);

    @Autowired
    private ReconApprovalRequestRepository reconApprovalRequestRepository;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> submitRequest(ReconApprovalRequest request) {
        if (request.getEntityType() == null || request.getEntityId() == null || request.getMakerId() == null) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Entity type, entity ID, and maker ID are required.", null));
        }
        request.setStatus("PENDING");
        request.setSubmittedAt(LocalDateTime.now());
        ReconApprovalRequest saved = reconApprovalRequestRepository.save(request);
        logger.info("ReconApprovalRequest submitted: entityType={}, entityId={}, makerId={}",
                saved.getEntityType(), saved.getEntityId(), saved.getMakerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "Approval request submitted.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getPendingRequests() {
        List<ReconApprovalRequest> requests = reconApprovalRequestRepository.findByStatus("PENDING");
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Pending approval requests fetched.", requests));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRequestById(Long requestId) {
        Optional<ReconApprovalRequest> opt = reconApprovalRequestRepository.findById(requestId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Approval request not found with ID: " + requestId, null));
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Request found.", Collections.singletonList(opt.get())));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRequestsByEntityType(String entityType) {
        List<ReconApprovalRequest> requests = reconApprovalRequestRepository.findByEntityTypeAndStatus(entityType, "PENDING");
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Requests fetched by entity type.", requests));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRequestsByMakerId(Long makerId) {
        List<ReconApprovalRequest> requests = reconApprovalRequestRepository.findByMakerId(makerId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Requests fetched by maker.", requests));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRequestsByCheckerId(Long checkerId) {
        List<ReconApprovalRequest> requests = reconApprovalRequestRepository.findByCheckerId(checkerId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Requests fetched by checker.", requests));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> approveRequest(Long requestId, Long checkerId, String remarks) {
        Optional<ReconApprovalRequest> opt = reconApprovalRequestRepository.findById(requestId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Approval request not found with ID: " + requestId, null));
        }
        ReconApprovalRequest existing = opt.get();
        existing.setStatus("APPROVED");
        existing.setDecision("APPROVED");
        existing.setCheckerId(checkerId);
        existing.setCheckedAt(LocalDateTime.now());
        existing.setRemarks(remarks);
        reconApprovalRequestRepository.save(existing);
        logger.info("ReconApprovalRequest approved: requestId={}, checkerId={}", requestId, checkerId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Request approved.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> rejectRequest(Long requestId, Long checkerId, String remarks) {
        Optional<ReconApprovalRequest> opt = reconApprovalRequestRepository.findById(requestId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Approval request not found with ID: " + requestId, null));
        }
        ReconApprovalRequest existing = opt.get();
        existing.setStatus("REJECTED");
        existing.setDecision("REJECTED");
        existing.setCheckerId(checkerId);
        existing.setCheckedAt(LocalDateTime.now());
        existing.setRemarks(remarks);
        reconApprovalRequestRepository.save(existing);
        logger.info("ReconApprovalRequest rejected: requestId={}, checkerId={}", requestId, checkerId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Request rejected.", null));
    }
}



