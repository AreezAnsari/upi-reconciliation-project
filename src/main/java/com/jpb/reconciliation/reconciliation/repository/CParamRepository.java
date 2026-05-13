package com.jpb.reconciliation.reconciliation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpb.reconciliation.reconciliation.entity.CParam;

public interface CParamRepository extends JpaRepository<CParam, Long> {

    Optional<CParam> findByParamNameAndActiveYn(String paramName, String activeYn);

    boolean existsByParamName(String paramName);
}