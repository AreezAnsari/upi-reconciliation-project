package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.entity.ReconBankMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import com.jpb.reconciliation.reconciliation.repository.ReconBankMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableScheduling
public class StatusSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(StatusSchedulerService.class);

    @Autowired
    private ReconUserRepository reconUserRepository;

    @Autowired
    private ReconBankMasterRepository reconBankMasterRepository;

    // Runs every 60 seconds
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processScheduledUserStatusChanges() {
        LocalDateTime now = LocalDateTime.now();

        // --- INACTIVATE ---
        List<ReconUser> toInactivate = reconUserRepository.findByInactivateScheduledAtBefore(now);
        for (ReconUser user : toInactivate) {
            if (!"INACTIVE".equals(user.getStatus()) && !"BLOCKED".equals(user.getStatus())) {
                logger.info("[Scheduler] Inactivating user: userId={}", user.getUserId());
                user.setStatus("INACTIVE");
            }
            user.setInactivateScheduledAt(null);
            user.setInactivateScheduledBy(null);
            user.setUpdatedAt(now);
            user.setUpdatedBy("SCHEDULER");
            reconUserRepository.save(user);
        }

        // --- REACTIVATE ---
        List<ReconUser> toReactivate = reconUserRepository.findByReactivateScheduledAtBefore(now);
        for (ReconUser user : toReactivate) {
            if (!"ACTIVE".equals(user.getStatus())) {
                logger.info("[Scheduler] Reactivating user: userId={}", user.getUserId());
                user.setStatus("ACTIVE");
            }
            user.setReactivateScheduledAt(null);
            user.setReactivateScheduledBy(null);
            user.setUpdatedAt(now);
            user.setUpdatedBy("SCHEDULER");
            reconUserRepository.save(user);
        }

        // --- BLOCK ---
        List<ReconUser> toBlock = reconUserRepository.findByBlockScheduledAtBefore(now);
        for (ReconUser user : toBlock) {
            if (!"BLOCKED".equals(user.getStatus())) {
                logger.info("[Scheduler] Blocking user: userId={}", user.getUserId());
                user.setPreBlockStatus(user.getStatus());
                user.setStatus("BLOCKED");
            }
            user.setBlockScheduledAt(null);
            user.setBlockScheduledBy(null);
            user.setUpdatedAt(now);
            user.setUpdatedBy("SCHEDULER");
            reconUserRepository.save(user);
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processScheduledBankStatusChanges() {
        LocalDateTime now = LocalDateTime.now();

        // --- INACTIVATE ---
        List<ReconBankMaster> toInactivate = reconBankMasterRepository.findByInactivateScheduledAtBefore(now);
        for (ReconBankMaster bank : toInactivate) {
            if (!"INACTIVE".equals(bank.getStatus()) && !"BLOCKED".equals(bank.getStatus())) {
                logger.info("[Scheduler] Inactivating bank: bankId={}", bank.getBankId());
                bank.setStatus("INACTIVE");
            }
            bank.setInactivateScheduledAt(null);
            bank.setInactivateScheduledBy(null);
            bank.setUpdatedAt(now);
            bank.setUpdatedBy("SCHEDULER");
            reconBankMasterRepository.save(bank);
        }

        // --- REACTIVATE ---
        List<ReconBankMaster> toReactivate = reconBankMasterRepository.findByReactivateScheduledAtBefore(now);
        for (ReconBankMaster bank : toReactivate) {
            if (!"ACTIVE".equals(bank.getStatus())) {
                logger.info("[Scheduler] Reactivating bank: bankId={}", bank.getBankId());
                bank.setStatus("ACTIVE");
            }
            bank.setReactivateScheduledAt(null);
            bank.setReactivateScheduledBy(null);
            bank.setUpdatedAt(now);
            bank.setUpdatedBy("SCHEDULER");
            reconBankMasterRepository.save(bank);
        }

        // --- BLOCK ---
        List<ReconBankMaster> toBlock = reconBankMasterRepository.findByBlockScheduledAtBefore(now);
        for (ReconBankMaster bank : toBlock) {
            if (!"BLOCKED".equals(bank.getStatus())) {
                logger.info("[Scheduler] Blocking bank: bankId={}", bank.getBankId());
                bank.setPreBlockStatus(bank.getStatus());
                bank.setStatus("BLOCKED");
            }
            bank.setBlockScheduledAt(null);
            bank.setBlockScheduledBy(null);
            bank.setUpdatedAt(now);
            bank.setUpdatedBy("SCHEDULER");
            reconBankMasterRepository.save(bank);
        }
    }
}
