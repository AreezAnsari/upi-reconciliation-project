package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.ReconBankMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconBankMasterRepository extends JpaRepository<ReconBankMaster, Long> {

    Optional<ReconBankMaster> findByBankCode(String bankCode);

    boolean existsByBankCode(String bankCode);

    boolean existsByBankName(String bankName);

    List<ReconBankMaster> findByStatus(String status);

    List<ReconBankMaster> findByBankType(String bankType);

    List<ReconBankMaster> findByParentBankId(Long parentBankId);

    List<ReconBankMaster> findByCreatedBy(String createdBy);

    Optional<ReconBankMaster> findByBankName(String bankName);

    List<ReconBankMaster> findByInactivateScheduledAtBefore(LocalDateTime time);

    List<ReconBankMaster> findByReactivateScheduledAtBefore(LocalDateTime time);

    List<ReconBankMaster> findByBlockScheduledAtBefore(LocalDateTime time);
}
