package com.jpb.reconciliation.reconciliation.service.v2;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.v2.CBankProductMap;
import com.jpb.reconciliation.reconciliation.repository.v2.CBankProductMapRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CBankProductMapServiceImpl implements CBankProductMapService {

    private static final Logger logger = LoggerFactory.getLogger(CBankProductMapServiceImpl.class);

    @Autowired
    private CBankProductMapRepository cBankProductMapRepository;

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> mapBankProduct(CBankProductMap mapping) {
        if (mapping.getBankId() == null || mapping.getProductId() == null) {
            return ResponseEntity.badRequest()
                    .body(new RestWithStatusList("FAILURE", "Bank ID and Product ID are required.", null));
        }
        if (cBankProductMapRepository.existsByBankIdAndProductId(mapping.getBankId(), mapping.getProductId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RestWithStatusList("FAILURE", "Mapping already exists for this bank and product.", null));
        }
        if (mapping.getValidFrom() == null) {
            mapping.setValidFrom(LocalDate.now());
        }
        if (mapping.getStatus() == null) {
            mapping.setStatus("ACTIVE");
        }
        CBankProductMap saved = cBankProductMapRepository.save(mapping);
        logger.info("CBankProductMap created: bankId={}, productId={}", saved.getBankId(), saved.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RestWithStatusList("SUCCESS", "Bank-product mapping created.", Collections.singletonList(saved)));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMappingsByBankId(Long bankId) {
        List<CBankProductMap> mappings = cBankProductMapRepository.findByBankId(bankId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Mappings fetched.", mappings));
    }

    @Override
    public ResponseEntity<RestWithStatusList> getMappingsByProductId(Long productId) {
        List<CBankProductMap> mappings = cBankProductMapRepository.findByProductId(productId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Mappings fetched.", mappings));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> removeMappingsByBankId(Long bankId) {
        cBankProductMapRepository.deleteByBankId(bankId);
        logger.info("CBankProductMap removed for bankId={}", bankId);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Bank product mappings removed.", null));
    }

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateMappingStatus(Long id, String status) {
        Optional<CBankProductMap> opt = cBankProductMapRepository.findById(id);
        if (!opt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestWithStatusList("FAILURE", "Mapping not found with ID: " + id, null));
        }
        CBankProductMap existing = opt.get();
        existing.setStatus(status);
        cBankProductMapRepository.save(existing);
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Mapping status updated.", null));
    }
}



