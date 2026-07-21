package com.jpb.reconciliation.reconciliation.service;

import java.util.List;

import com.jpb.reconciliation.reconciliation.dto.ReconConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconConfigResponse;

public interface ReconConfigService {
    
    ReconConfigResponse createReconConfig(ReconConfigRequest req);
    
    ReconConfigResponse getById(Long processId);
    
    List<ReconConfigResponse> getAll();
    
    ReconConfigResponse updateReconConfig(Long processId, ReconConfigRequest req);
    
    void deleteReconConfig(Long processId);
}