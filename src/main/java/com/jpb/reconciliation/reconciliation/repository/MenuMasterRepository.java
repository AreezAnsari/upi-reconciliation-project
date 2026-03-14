package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.dto.ReconMenuMasterDto;
import com.jpb.reconciliation.reconciliation.entity.ReconMenuMaster;
import com.jpb.reconciliation.reconciliation.entity.Role;

@Repository
public interface MenuMasterRepository extends JpaRepository<ReconMenuMaster, Long> {

	void save(ReconMenuMasterDto menuDto);

	Optional<ReconMenuMaster> findByMenuId(Long menuId);

	List<ReconMenuMaster> getByInsertUserId(Long userId);

	ReconMenuMaster findByMenuName(String menuName);

	List<ReconMenuMaster> findByParentMenuCode(String menuType);

	ReconMenuMaster findByMenuNameAndInsertUserId(String menuName, Long long1);

	List<ReconMenuMaster> getByRoleId(Long roleId);

	ReconMenuMaster findByMenuNameAndRoleId(String menuName, Long roleId);

	ReconMenuMaster findByMenuNameAndRoleIdAndParentMenuCode(String menuName, Long roleId, String parentMenuCode);

	Role findByRoleId(Long verifiedRoleId);

//	int assignMenusToRole(Long roleId, List<Long> menuIds);
}
