package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.RecRole;

@Repository
public interface RecRoleRepository extends JpaRepository<RecRole, Long> {
    boolean existsByRoleName(String roleName);
    
    @Query("SELECT r FROM RecRole r LEFT JOIN FETCH r.permissions p LEFT JOIN FETCH p.module WHERE r.id = :id")
    Optional<RecRole> findByIdWithPermissions(@Param("id") Long id);
 
    // Fetch all roles with their masters for list view
    @Query("SELECT DISTINCT r FROM RecRole r LEFT JOIN FETCH r.roleMasters")
    List<RecRole> findAllWithMasters();}
