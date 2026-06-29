package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.RecModule;
import com.jpb.reconciliation.reconciliation.entity.RecRole;

@Repository
public interface RecRoleRepository extends JpaRepository<RecRole, Long> {
    boolean existsByRoleName(String roleName);
    
    @Query("SELECT r FROM RecRole r LEFT JOIN FETCH r.permissions p LEFT JOIN FETCH p.module WHERE r.id = :id")
    Optional<RecRole> findByIdWithPermissions(@Param("id") Long id);
 
    // Fetch all roles with their masters for list view
    @Query("SELECT DISTINCT r FROM RecRole r LEFT JOIN FETCH r.roleMasters")
    List<RecRole> findAllWithMasters();
    
 // ── Find by unique roleCode (4-digit string) ──────────────────────────────
    Optional<RecRole> findByRoleCode(String roleCode);
 
    // ── Find all roles in a category, e.g. all MAKER-family roles ────────────
    List<RecRole> findAllByRoleMasterNameIgnoreCase(String roleMasterName);
 
    // ── Check for duplicate roleName within the same roleType ─────────────────
    //    Prevents creating MAKER twice with identical roleType + roleName combo.
    boolean existsByRoleNameIgnoreCaseAndRoleType(String roleName, String roleType);
 
    // ── Find all roles assigned to a user ─────────────────────────────────────
    List<RecRole> findAllByAssignedUserId(Long userId);
 
    // ── Search by name fragment (for the frontend search box) ─────────────────
    @Query("SELECT r FROM RecRole r WHERE LOWER(r.roleName) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<RecRole> searchByName(@Param("term") String term);
 
    // ── All roles for a given roleType ────────────────────────────────────────
    List<RecRole> findAllByRoleTypeIgnoreCase(String roleType);
 
    // ── Count how many MAKER roles exist (useful for dashboards) ──────────────
    long countByRoleMasterNameIgnoreCase(String roleMasterName);

    // ── Bank / Branch scoped queries ──────────────────────────────────────────
    // Branch Admin: roles belonging to their specific branch
    List<RecRole> findByBranchCode(String branchCode);

    // Bank Admin: roles they created (no branch scope — bank-level only)
    List<RecRole> findByBankCodeAndBranchCodeIsNull(String bankCode);
    
    Optional<RecRole> findByRoleNameIgnoreCaseAndRoleType(String roleName, String roleType);
    
 // Add this alongside it (keep the old one if other code still uses it expecting single):
    List<RecRole> findAllByRoleNameIgnoreCaseAndRoleTypeOrderByCreatedAtDesc(String roleName, String roleType);
    
    boolean existsByRoleCode(String roleCode);
    

}
