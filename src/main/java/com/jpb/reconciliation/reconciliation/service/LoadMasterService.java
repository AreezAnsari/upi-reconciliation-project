package com.jpb.reconciliation.reconciliation.service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.jpb.reconciliation.reconciliation.entity.LoadMasterEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconFileDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTmpltFieldDtls;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;

public interface LoadMasterService {

	CompletableFuture<String> startDataLoading(Long processId, ReconFileDetailsMaster reconFileDetails,
			ReconUser userData, ReconBatchProcessEntity extractionStatus, LoadMasterEntity loadMasterEntity);

	public ReconBatchProcessEntity extractionRunningStatusforGlFlagY(Long processId,
			ReconFileDetailsMaster reconFileDetails, ReconUser userData);

	ReconBatchProcessEntity extractionRunningStatusforGlFlagYNewV2(Long templateId,
			Optional<ReconTmpltFieldDtls> reconTemplateFileDetails, ReconUser userData);

	CompletableFuture<String> startDataLoadingNewV2(Long templateId,
			Optional<ReconTmpltFieldDtls> reconTemplateFileDetails, ReconUser userData,
			ReconBatchProcessEntity extractionStatus, LoadMasterEntity loadMasterEntity);

}
