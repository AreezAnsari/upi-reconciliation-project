package com.jpb.reconciliation.reconciliation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.RecModule;

import java.util.List;

@Repository
public interface RecModuleRepository extends JpaRepository<RecModule, Long> {
    List<RecModule> findAllByOrderByDisplayOrderAsc();
}

