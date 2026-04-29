package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.RecRole;

@Repository
public interface RecRoleRepository extends JpaRepository<RecRole, Long> {
    boolean existsByRoleName(String roleName);
}
