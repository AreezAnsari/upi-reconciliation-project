package com.jpb.reconciliation.reconciliation.service.v2;

import java.util.List;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.entity.ReconSourceSystemMast;
import com.jpb.reconciliation.reconciliation.repository.ReconSourceSystemMastRepository;

@Service
public class ReconSourceSystemMastServiceImpl implements ReconSourceSystemMastService {

    private final ReconSourceSystemMastRepository repository;

    public ReconSourceSystemMastServiceImpl(ReconSourceSystemMastRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ReconSourceSystemMast> getActiveSources() {
        return repository.findByIsActive(CommonConstants.ACTIVE);
    }

}