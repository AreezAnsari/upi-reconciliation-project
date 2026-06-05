package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;

@Repository
public interface BranchAdminRepository extends JpaRepository<BranchAdmin, Long> {

    Optional<BranchAdmin> findByInstitutionCodeAndUsername(String institutionCode, String username);

    boolean existsByInstitutionCodeAndUsername(String institutionCode, String username);

    Optional<BranchAdmin> findFirstByEmail(String email);

    Optional<BranchAdmin> findFirstByEmailOrderByIdAsc(String email);

    Optional<BranchAdmin> findFirstByUsername(String username);
}
