package com.jpb.reconciliation.reconciliation.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.AuditLogManager;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.entity.Role;
import com.jpb.reconciliation.reconciliation.repository.AuditLogManagerRepository;

@Service
public class AuditLogManagerServiceImpl implements AuditLogManagerService {

	Logger logger = LoggerFactory.getLogger(AuditLogManagerServiceImpl.class);

	@Autowired
	private AuditLogManagerRepository auditLogManagerRepository;

	@Override
	public void loginAudit(ReconUser user, String accessToken, String refreshToken) {
		AuditLogManager audit = new AuditLogManager();
		Role role = user.getRole();
		audit.setModule("User Login");
		audit.setSubModule("-");
		audit.setEvent("Login");
		audit.setEventData("UserId:" + user.getUserName() + "|" + "LoginAs:" + "|" + "Response:-" + "accessToken :"
				+ accessToken + "refreshToken :" + refreshToken);
		audit.setEventStatus("SUCCESS");
		audit.setUserId(user.getUserId());
		audit.setUserIp("-");
		audit.setAuditDateTime(new Date());
		audit.setOldData("-");
		audit.setRoleId(role.getRoleId());
		logger.info("LOGIN AUDIT LOG :::::::::" + audit);
		auditLogManagerRepository.save(audit);
	}

	@Override
	public void extractionAudit(ReconBatchProcessEntity reconProcessManager, ReconUser userData) {
		AuditLogManager audit = new AuditLogManager();
		Role role = userData.getRole();
		audit.setModule(reconProcessManager.getProcessType());
		audit.setSubModule("-");
		audit.setEvent(reconProcessManager.getProcessType());
		audit.setEventData("ProcessID:" + reconProcessManager.getProcessId() + "|" + "ProcessName:"
				+ reconProcessManager.getProcessType() + "|" + "ProcessType:-" + reconProcessManager.getProcessType());
		audit.setEventStatus(reconProcessManager.getStatus());
		audit.setUserId(reconProcessManager.getInsertUser());
		audit.setUserIp("-");
		audit.setAuditDateTime(new Date());
		audit.setOldData("-");
		audit.setRoleId(role.getRoleId());
		logger.info("Extraction AUDIT LOG :::::::::" + audit);
		auditLogManagerRepository.save(audit);
	}

	@Override
	public void commonAudit(ReconUser userData, String module, Object addNewMenu) {
		AuditLogManager audit = new AuditLogManager();
		Role role = userData.getRole();

		ReconMenuMaster menu = (ReconMenuMaster) addNewMenu;
		audit.setModule(module);
		audit.setSubModule("-");
		audit.setEvent(menu.getMenuName());
		audit.setEventData("MENUID:" + menu.getMenuId() + "|" + "MENUNAME:" + menu.getMenuName() + "|" + "ProcessType:-"
				+ menu.getProcessType());
		audit.setEventStatus("Created");
		audit.setUserId(menu.getInsertUserId());
		audit.setUserIp("-");
		audit.setAuditDateTime(new Date());
		audit.setOldData("-");
		audit.setRoleId(role.getRoleId());
		auditLogManagerRepository.save(audit);
	}

}
