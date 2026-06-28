package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.ReconPasswordManager;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReconPasswordManagerRepository extends JpaRepository<ReconPasswordManager, Long> {

    Optional<ReconPasswordManager> findByReconUser_UserId(Long userId);

    List<ReconPasswordManager> findByReconUser(ReconUser reconUser);

    Optional<ReconPasswordManager> findByToken(String token);

    void deleteByReconUser_UserId(Long userId);
}
