package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;

@Repository
public interface MenuMasterRepository extends JpaRepository<ReconMenuMaster, Long> {

    Optional<ReconMenuMaster> findByMenuId(Long menuId);

    List<ReconMenuMaster> getByInsertUserId(Long userId);

    ReconMenuMaster findByMenuName(String menuName);

    List<ReconMenuMaster> findByParentMenuCode(String menuType);

    ReconMenuMaster findByMenuNameAndInsertUserId(String menuName, Long userId);

    List<ReconMenuMaster> getByRoleId(Long roleId);

    List<ReconMenuMaster> getByRoleIdAndStatus(Long roleId, String status);

    ReconMenuMaster findByMenuNameAndRoleId(String menuName, Long roleId);

    ReconMenuMaster findByMenuNameAndRoleIdAndParentMenuCode(String menuName, Long roleId, String parentMenuCode);

    List<ReconMenuMaster> findByRoleIdIn(List<Long> roleIds);

    // Identifies a /user-portal "twin" of an Admin-portal menu regardless of which role first
    // created it, so it can be reused across multiple custom roles instead of duplicated.
    Optional<ReconMenuMaster> findByMenuNameAndMenuUrl(String menuName, String menuUrl);

    // Master menu names ("My Organization"/"Administration") aren't unique — Kal Admin, each
    // Bank Admin, and each Branch Admin all have their own bootstrap row with the same name.
    // Returns every match so the caller can pick any active one purely for nesting purposes.
    List<ReconMenuMaster> findAllByMenuNameAndMenuType(String menuName, String menuType);
}
