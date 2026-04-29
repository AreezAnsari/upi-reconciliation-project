package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jpb.reconciliation.reconciliation.entity.TestRole;

public interface TestRoleRepo extends JpaRepository<TestRole, Long> {
}