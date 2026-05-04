package com.jpb.reconciliation.reconciliation.repository;

import java.util.List; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.TestRole;

@Repository
public interface TestRoleManageRepository extends JpaRepository<TestRole, Long> {

    TestRole findByRoleId(Long roleId);

    boolean existsByRoleCode(String roleCode);

    boolean existsByRoleCodeAndRoleIdNot(String newCode, Long roleId);

    List<TestRole> findByApprovedYn(String approvedYn);
}
