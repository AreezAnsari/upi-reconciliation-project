package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.EjFileLoadService;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/atmej")
@RequiredArgsConstructor
public class AtmEjLoaderController {

	private static final Logger LOG = LoggerFactory.getLogger(AtmEjLoaderController.class);

	private final EjFileLoadService ejFileLoadService;

	@PostMapping("/load")
	public ResponseEntity<RestWithStatusList> loadEjFiles(@RequestBody Map<String, Object> request) {

		EjFileLoadService.EjLoadResult result = ejFileLoadService.loadEjFiles(request);

		if (result.errorMessage != null) {
			LOG.error("NCR EJ load failed: {}", result.errorMessage);
			boolean isValidation = result.errorMessage.contains("required")
					|| result.errorMessage.contains("does not exist");
			if (isValidation) {
				return ResponseEntity.badRequest().body(ResponseBuilder.errorList(result.errorMessage));
			}
			if (result.errorMessage.startsWith("No files found")) {
				return ResponseEntity.ok(ResponseBuilder.okEmpty(result.errorMessage));
			}
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseBuilder.errorList(result.errorMessage));
		}

		LOG.info("NCR EJ load complete. batchId={}, status={}, inserted={}", result.batchId, result.overallStatus(),
				result.inserted);

		HttpStatus httpStatus = result.failedFiles == 0 ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
		return ResponseEntity.status(httpStatus).body(ResponseBuilder.okSingle(result.overallStatus(), result));
	}
}