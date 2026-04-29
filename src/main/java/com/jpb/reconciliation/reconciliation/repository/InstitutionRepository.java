package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.constants.EnableStatus;
import com.jpb.reconciliation.reconciliation.entity.Institution;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByInstitutionName(String institutionName);

    Optional<Institution> findByInstitutionUserId(String institutionUserId);

    List<Institution> findByEnableStatus(EnableStatus enableStatus);

    boolean existsByInstitutionName(String institutionName);

    boolean existsByInstitutionUserId(String institutionUserId);
}