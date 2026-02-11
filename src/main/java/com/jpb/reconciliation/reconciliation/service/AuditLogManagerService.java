package com.jpb.reconciliation.reconciliation.service;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;

@Service
public interface AuditLogManagerService {

	void loginAudit(ReconUser user, String token, String refreshToken);

	void extractionAudit(ReconBatchProcessEntity reconProcessManager, ReconUser userData);

	void commonAudit(ReconUser userData, String string, Object addNewMenu);
	
}
