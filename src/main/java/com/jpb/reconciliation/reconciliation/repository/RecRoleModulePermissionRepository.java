package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.RecRoleModulePermission;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecRoleModulePermissionRepository extends JpaRepository<RecRoleModulePermission, Long> {
    List<RecRoleModulePermission> findByRoleId(Long roleId);
    Optional<RecRoleModulePermission> findByRoleIdAndModuleId(Long roleId, Long moduleId);
    void deleteByRoleId(Long roleId);
}
