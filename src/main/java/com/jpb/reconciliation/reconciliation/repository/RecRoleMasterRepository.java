package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.RecRoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecRoleMasterRepository extends JpaRepository<RecRoleMaster, Long> {

    // Looks up master row by exact role name from dropdown e.g. "MAKER"
    Optional<RecRoleMaster> findByRoleName(String roleName);

    // Guards against duplicate custom master rows
    boolean existsByRoleName(String roleName);

    // Generates next custom role code (9001, 9002 ...)
    // 9000 = OTHER enum base, 1001-1007 = standard roles
    @Query("SELECT MAX(r.roleCode) FROM RecRoleMaster r WHERE r.roleCode >= 9000")
    Optional<Integer> findMaxCustomRoleCode();
}