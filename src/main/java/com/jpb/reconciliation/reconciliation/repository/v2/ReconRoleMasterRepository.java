package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.ReconRoleMaster;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconRoleMasterRepository extends JpaRepository<ReconRoleMaster, Long> {

    boolean existsByRoleCode(String roleCode);

    boolean existsByRoleName(String roleName);

    Optional<ReconRoleMaster> findByRoleCode(String roleCode);

    List<ReconRoleMaster> findByStatus(String status);

    List<ReconRoleMaster> findByRoleType(String roleType);
}
