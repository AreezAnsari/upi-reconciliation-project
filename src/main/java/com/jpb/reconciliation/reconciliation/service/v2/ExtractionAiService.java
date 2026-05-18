package com.jpb.reconciliation.reconciliation.service.v2;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.entity.ReconBatchProcessEntity;
import com.jpb.reconciliation.reconciliation.entity.ReconTmpltFieldDtls;
import com.jpb.reconciliation.reconciliation.entity.ReconUser;

@Service
public interface ExtractionAiService {

	List<ReconBatchProcessEntity> extractionRunningStatus(List<File> processedFiles,
			Optional<ReconTmpltFieldDtls> reconTemplateFileDetails, ReconUser userData);

	CompletableFuture<String> startExtraction(Optional<ReconTmpltFieldDtls> reconTemplateFileDetails,
			List<ReconBatchProcessEntity> runningExtraction, List<File> processedFiles, ReconUser userData);

}
