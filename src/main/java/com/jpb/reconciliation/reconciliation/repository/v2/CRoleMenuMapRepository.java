package com.jpb.reconciliation.reconciliation.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.v2.CRoleMenuMap;

import java.util.List;

@Repository
public interface CRoleMenuMapRepository extends JpaRepository<CRoleMenuMap, CRoleMenuMap.RoleMenuMapId> {

    @Query("SELECT r FROM CRoleMenuMap r WHERE r.id.roleId = :roleId")
    List<CRoleMenuMap> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT r FROM CRoleMenuMap r WHERE r.id.menuId = :menuId")
    List<CRoleMenuMap> findByMenuId(@Param("menuId") Long menuId);
}
