package com.jpb.reconciliation.reconciliation.controller.forcematch;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.forcematch.ForceMatchExecutionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller — Execute SP_FORCE_MATCH
 *
 * Base URL : /api/v1/force-match/execute
 *
 * ┌──────────┬───────────────────────────────────┬──────────────────────────────────┐
 * │ Method │ URL │ Purpose │
 * ├──────────┼───────────────────────────────────┼──────────────────────────────────┤
 * │ POST │ / │ Execute SP_FORCE_MATCH │
 * └──────────┴───────────────────────────────────┴──────────────────────────────────┘
 *
 * Request body: { "processId": "1001", "userId": 99 }
 *
 * Response (RestWithStatusList): { "status": "SUCCESS", "statusMsg": "Force
 * match executed successfully", "data": [{ "processId": "1001", "error": "OK",
 * "knockoffFlag": "A", "ttumId": 12345, "ttumError": 0 }] }
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/force-match/execute")
@RequiredArgsConstructor
@Tag(name = "Force Match - Execute", description = "Executes SP_FORCE_MATCH stored procedure")
public class ForceMatchController {

	private final ForceMatchExecutionService executionService;

	@Operation(summary = "Execute SP_FORCE_MATCH for a given process ID and user")
	@PostMapping
	public ResponseEntity<RestWithStatusList> execute(@RequestBody ExecuteRequest request) {
		log.info("POST /api/v1/force-match/execute | processId={} | userId={}", request.getProcessId(),
				request.getUserId());

		if (request.getProcessId() == null || request.getProcessId().trim().isEmpty()) {
			return ResponseEntity.badRequest()
					.body(RestWithStatusList.builder().status("FAILURE").statusMsg("processId is required").build());
		}
		if (request.getUserId() == null) {
			return ResponseEntity.badRequest()
					.body(RestWithStatusList.builder().status("FAILURE").statusMsg("userId is required").build());
		}

		return ResponseEntity.ok(executionService.execute(request.getProcessId(), request.getUserId()));
	}

	// ── Inner request DTO (kept close to the controller; no extra file needed) ──
	@lombok.Data
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	public static class ExecuteRequest {
		private String processId;
		private Integer userId;
	}
}