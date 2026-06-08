package com.jpb.reconciliation.reconciliation.service;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;

@Service
public interface AuditLogManagerService {

	void loginAudit(KalAdmin user, String token, String refreshToken);

	void extractionAudit(ReconBatchProcessEntity reconProcessManager, KalAdmin userData);

	void commonAudit(KalAdmin userData, String string, Object addNewMenu);
	
}
