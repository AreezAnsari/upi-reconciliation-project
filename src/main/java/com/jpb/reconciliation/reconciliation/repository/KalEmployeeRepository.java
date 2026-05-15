package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jpb.reconciliation.reconciliation.entity.KalEmployeeAdmin;

@Repository
public interface KalEmployeeRepository extends JpaRepository<KalEmployeeAdmin, Long> {

    Optional<KalEmployeeAdmin> findByUsername(String username);

    Optional<KalEmployeeAdmin> findByEmail(String email);
    
    @Query("SELECT e FROM KalEmployeeAdmin e WHERE LOWER(e.email) = LOWER(:email)")
    Optional<KalEmployeeAdmin> findByEmailIgnoreCase(@Param("email") String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}