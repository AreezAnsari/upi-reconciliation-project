package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.RecRoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecRoleMasterRepository extends JpaRepository<RecRoleMaster, Long> {

    Optional<RecRoleMaster> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);

    @Query("SELECT MAX(r.roleCode) FROM RecRoleMaster r WHERE r.roleCode >= 9000")
    Optional<Integer> findMaxCustomRoleCode();
}
