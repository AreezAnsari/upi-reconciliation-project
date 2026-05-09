package com.jpb.reconciliation.reconciliation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpb.reconciliation.reconciliation.entity.MLookup;

@Repository
public interface MLookupRepository extends JpaRepository<MLookup, Long> {

    List<MLookup> findByActiveYn(String activeYn);

    List<MLookup> findByLookupNameAndActiveYn(String name, String activeYn);
}
