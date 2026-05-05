package com.jpb.reconciliation.reconciliation.controller.v2;


import java.util.Collections;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.ScheduleConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.ScheduleConfigResponse;
import com.jpb.reconciliation.reconciliation.service.ReconExecScheduleConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for RECON_EXEC_SCHEDULE_CONFIG.
 *
 * Base URL : /api/v1/templates/{templateId}/schedule-config Service :
 * ReconExecScheduleConfigService (impl: ReconExecScheduleConfigServiceImpl)
 * Response : RestWithStatusList — { status, statusMsg, List<Object> data }
 *
 * Endpoints: POST /api/v1/templates/{templateId}/schedule-config → save
 * (upsert) GET /api/v1/templates/{templateId}/schedule-config → fetch by
 * templateId PATCH /api/v1/templates/{templateId}/schedule-config/activate →
 * set IS_ACTIVE='Y' PATCH
 * /api/v1/templates/{templateId}/schedule-config/deactivate→ set IS_ACTIVE='N'
 * DELETE /api/v1/templates/{templateId}/schedule-config → delete
 */
@RestController
@RequestMapping("/api/v1/templates/{templateId}/schedule-config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Execution Schedule Config", description = "Manage CRON / CYCLE / EVENT_BASED execution schedules per template")
public class ReconExecScheduleConfigController {

	private final ReconExecScheduleConfigService scheduleService;

	// ─────────────────────────────────────────────────────────────────────────
	// POST /api/v1/templates/{templateId}/schedule-config
	// Save or update schedule config for a template (upsert).
	//
	// Request body — ScheduleConfigRequest:
	// scheduleType String CRON|CYCLE|EVENT_BASED
	// cronExpression String required when scheduleType=CRON
	// execWindowStart LocalDateTime
	// execWindowEnd LocalDateTime
	// maxRetryCount Integer default 3 (0–999)
	// retryIntervalMins Integer default 15 (1–99999)
	// dependencyTmpltIds String comma-separated e.g. "1001,1002"
	// timezone String default "Asia/Kolkata"
	// isActive String Y|N default N
	// ─────────────────────────────────────────────────────────────────────────
	@PostMapping
	@Operation(summary = "Save or update schedule config for a template (upsert)")
	public ResponseEntity<RestWithStatusList> save(@PathVariable Long templateId,
			@Valid @RequestBody ScheduleConfigRequest request) {

		log.info("POST /schedule-config templateId={}", templateId);
		ScheduleConfigResponse response = scheduleService.saveScheduleConfig(templateId, request);
		return ok("Schedule config saved successfully.", response);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// GET /api/v1/templates/{templateId}/schedule-config
	// Fetch the schedule config for a template.
	// ─────────────────────────────────────────────────────────────────────────
	@GetMapping
	@Operation(summary = "Get schedule config for a template")
	public ResponseEntity<RestWithStatusList> get(@PathVariable Long templateId) {

		log.info("GET /schedule-config templateId={}", templateId);
		ScheduleConfigResponse response = scheduleService.getScheduleConfig(templateId);
		return ok("Schedule config fetched successfully.", response);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// PATCH /api/v1/templates/{templateId}/schedule-config/activate
	// Sets IS_ACTIVE = 'Y' — enables automatic execution of the schedule.
	// ─────────────────────────────────────────────────────────────────────────
	@PatchMapping("/activate")
	@Operation(summary = "Activate schedule — sets IS_ACTIVE='Y'")
	public ResponseEntity<RestWithStatusList> activate(@PathVariable Long templateId) {

		log.info("PATCH /schedule-config/activate templateId={}", templateId);
		scheduleService.activateSchedule(templateId);
		return ok("Schedule activated successfully.", null);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// PATCH /api/v1/templates/{templateId}/schedule-config/deactivate
	// Sets IS_ACTIVE = 'N' — pauses automatic execution.
	// ─────────────────────────────────────────────────────────────────────────
	@PatchMapping("/deactivate")
	@Operation(summary = "Deactivate schedule — sets IS_ACTIVE='N'")
	public ResponseEntity<RestWithStatusList> deactivate(@PathVariable Long templateId) {

		log.info("PATCH /schedule-config/deactivate templateId={}", templateId);
		scheduleService.deactivateSchedule(templateId);
		return ok("Schedule deactivated successfully.", null);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// DELETE /api/v1/templates/{templateId}/schedule-config
	// Hard-delete the schedule config row for this template.
	// (DB-level ON DELETE CASCADE on FK_RSCHED_TMPLT handles the case where the
	// parent template is deleted at the DB level directly.)
	// ─────────────────────────────────────────────────────────────────────────
	@DeleteMapping
	@Operation(summary = "Delete schedule config for a template")
	public ResponseEntity<RestWithStatusList> delete(@PathVariable Long templateId) {

		log.info("DELETE /schedule-config templateId={}", templateId);
		scheduleService.deleteScheduleConfig(templateId);
		return ok("Schedule config deleted successfully.", null);
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Private helper — builds the standard RestWithStatusList response
	// ─────────────────────────────────────────────────────────────────────────
	private ResponseEntity<RestWithStatusList> ok(String msg, Object data) {
		return ResponseEntity.ok(RestWithStatusList.builder().status("SUCCESS").statusMsg(msg)
				//.data(data != null ? List.of(data) : List.of()).build());   old line Akash
				.data(data != null ? Collections.singletonList(data) : Collections.emptyList())
				.build());
	}
}
