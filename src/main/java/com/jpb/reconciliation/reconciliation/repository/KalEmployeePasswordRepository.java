package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.KalEmployeeAdmin;
import com.jpb.reconciliation.reconciliation.entity.KalEmployeePassword;

@Repository
public interface KalEmployeePasswordRepository extends JpaRepository<KalEmployeePassword, Long> {

    Optional<KalEmployeePassword> findByKalEmployee(KalEmployeeAdmin kalEmployee);
}