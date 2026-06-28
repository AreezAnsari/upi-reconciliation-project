package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.AuditLog;
import com.jpb.reconciliation.reconciliation.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void log(String tableName, Long recordId, String operation, Long actorUserId, String actorUsername,
                    String actorType, String entityType, Long bankId, String oldValue, String newValue,
                    String actionLabel, String ipAddress, String remarks) {
        try {
            AuditLog entry = new AuditLog();
            entry.setTableName(tableName);
            entry.setRecordId(recordId);
            entry.setOperation(operation);
            entry.setActorUserId(actorUserId);
            entry.setActorUsername(actorUsername);
            entry.setActorType(actorType);
            entry.setEntityType(entityType);
            entry.setBankId(bankId);
            entry.setOldValue(oldValue);
            entry.setNewValue(newValue);
            entry.setActionLabel(actionLabel);
            entry.setIpAddress(ipAddress);
            entry.setRemarks(remarks);
            entry.setChangedAt(LocalDateTime.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            logger.error("Failed to write audit log: {}", e.getMessage(), e);
        }
    }

    @Override
    public ResponseEntity<RestWithStatusList> getLogsByTable(String tableName) {
        List<AuditLog> logs = auditLogRepository.findByTableName(tableName);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Audit logs fetched.", logs));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getLogsByRecord(String tableName, Long recordId) {
        List<AuditLog> logs = auditLogRepository.findByTableNameAndRecordId(tableName, recordId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Audit logs fetched.", logs));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getLogsByActor(Long actorUserId) {
        List<AuditLog> logs = auditLogRepository.findByActorUserId(actorUserId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Audit logs fetched.", logs));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getLogsByBankId(Long bankId) {
        List<AuditLog> logs = auditLogRepository.findByBankId(bankId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Audit logs fetched.", logs));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getLogsByDateRange(String from, String to) {
        LocalDateTime fromDt = LocalDateTime.parse(from);
        LocalDateTime toDt = LocalDateTime.parse(to);
        List<AuditLog> logs = auditLogRepository.findByChangedAtBetween(fromDt, toDt);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Audit logs fetched.", logs));
    }
}



