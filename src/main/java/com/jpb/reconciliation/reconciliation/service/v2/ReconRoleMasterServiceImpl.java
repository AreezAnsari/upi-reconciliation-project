package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;
import com.jpb.reconciliation.reconciliation.repository.v2.ReconRoleMasterRepository;

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
public class ReconRoleMasterServiceImpl implements ReconRoleMasterService {

    private static final Logger logger = LoggerFactory.getLogger(ReconRoleMasterServiceImpl.class);

    @Autowired
    private ReconRoleMasterRepository reconRoleMasterRepository;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> createRole(ReconRoleMaster role, String createdBy) {
        if (role.getRoleCode() == null || role.getRoleCode().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Role code is required.", null));
        }
        if (reconRoleMasterRepository.existsByRoleCode(role.getRoleCode().trim().toUpperCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "Role code already exists.", null));
        }
        if (reconRoleMasterRepository.existsByRoleName(role.getRoleName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "Role name already exists.", null));
        }
        role.setRoleCode(role.getRoleCode().trim().toUpperCase());
        role.setStatus("DRAFT");
        role.setCreatedAt(LocalDateTime.now());
        role.setCreatedBy(createdBy);
        ReconRoleMaster saved = reconRoleMasterRepository.save(role);
        logger.info("ReconRoleMaster created: {} by {}", saved.getRoleCode(), createdBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "Role created successfully.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getAllRoles() {
        List<ReconRoleMaster> roles = reconRoleMasterRepository.findAll();
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Roles fetched.", roles));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRoleById(Long roleId) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role found.", Collections.singletonList(opt.get())));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRolesByStatus(String status) {
        List<ReconRoleMaster> roles = reconRoleMasterRepository.findByStatus(status);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Roles fetched by status.", roles));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getRolesByType(String roleType) {
        List<ReconRoleMaster> roles = reconRoleMasterRepository.findByRoleType(roleType);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Roles fetched by type.", roles));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateRole(Long roleId, ReconRoleMaster role, String updatedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        if (role.getRoleName() != null) existing.setRoleName(role.getRoleName());
        if (role.getRoleDesc() != null) existing.setRoleDesc(role.getRoleDesc());
        if (role.getRoleType() != null) existing.setRoleType(role.getRoleType());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role updated successfully.", Collections.singletonList(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> submitForApproval(Long roleId, String submittedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        existing.setStatus("PENDING");
        existing.setSubmittedBy(submittedBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(submittedBy);
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role submitted for approval.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> approveRole(Long roleId, String approvedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        existing.setStatus("ACTIVE");
        existing.setApprovedBy(approvedBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(approvedBy);
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role approved and activated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateStatus(Long roleId, String status, String updatedBy) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(updatedBy);
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role status updated.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteRole(Long roleId) {
        Optional<ReconRoleMaster> opt = reconRoleMasterRepository.findById(roleId);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Role not found with ID: " + roleId, null));
        }
        ReconRoleMaster existing = opt.get();
        existing.setStatus("INACTIVE");
        existing.setUpdatedAt(LocalDateTime.now());
        reconRoleMasterRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Role deactivated successfully.", null));
    }

    @Override
    public ResponseEntity<RestWithStatusList> checkRoleCodeExists(String roleCode) {
        boolean exists = reconRoleMasterRepository.existsByRoleCode(roleCode);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", exists ? "EXISTS" : "AVAILABLE", null));
    }
}
