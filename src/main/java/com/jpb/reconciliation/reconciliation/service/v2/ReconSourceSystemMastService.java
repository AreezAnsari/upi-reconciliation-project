package com.jpb.reconciliation.reconciliation.service.v2;

import java.util.List;

import com.jpb.reconciliation.reconciliation.entity.ReconSourceSystemMast;

public interface ReconSourceSystemMastService {

    List<ReconSourceSystemMast> getActiveSources();

}