package com.jpb.reconciliation.reconciliation.service;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.jpb.reconciliation.reconciliation.entity.LoadMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;

public interface LoadMasterService {
	
			CompletableFuture<String> startDataLoading(Long processId, ReconFileDetailsMaster reconFileDetails,
			ReconUser userData, ReconBatchProcessEntity extractionStatus, LoadMasterEntity loadMasterEntity);
	public ReconBatchProcessEntity extractionRunningStatusforGlFlagY( Long processId,
			ReconFileDetailsMaster reconFileDetails, ReconUser userData);
	
	
}
