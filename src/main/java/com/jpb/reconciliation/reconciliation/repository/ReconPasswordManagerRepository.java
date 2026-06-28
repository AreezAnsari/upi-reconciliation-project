package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.ReconPasswordManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReconPasswordManagerRepository extends JpaRepository<ReconPasswordManager, Long> {

    Optional<ReconPasswordManager> findByReconUser_UserId(Long userId);

    Optional<ReconPasswordManager> findByToken(String token);

    void deleteByReconUser_UserId(Long userId);
}
