package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpb.reconciliation.reconciliation.entity.KalAdminPasswordManager;

public interface PasswordManagerRepository extends JpaRepository<KalAdminPasswordManager, Long> {



}
