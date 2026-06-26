package com.jpb.reconciliation.reconciliation.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jpb.reconciliation.reconciliation.service.PasswordandSecurityExpiryService;

@Component
public class PasswordandSecurityMailScheduler {

    @Autowired
    private PasswordandSecurityExpiryService service;

    // Runs every day at 01:00 AM
    @Scheduled(cron = "0 0 1 * * ?")
    public void runJob() {
        service.runExpiryCheck();
    }
}