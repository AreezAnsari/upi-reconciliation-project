package com.jpb.reconciliation.reconciliation.repository;

import com.jpb.reconciliation.reconciliation.entity.UserAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// package: repository
@Repository
public interface UserAssignmentRepository extends JpaRepository<UserAssignment, Long> {
}
