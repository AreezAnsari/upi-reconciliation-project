package com.jpb.reconciliation.reconciliation.service;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;

@Service
public interface AuditLogManagerService {

    void extractionAudit(ReconBatchProcessEntity reconProcessManager, ReconUser userData);

    void commonAudit(ReconUser userData, String module, Object data);

}
