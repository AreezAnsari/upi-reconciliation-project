package com.jpb.reconciliation.reconciliation.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.repository.ReconFileDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconProcessDefMasterRepository;
import com.jpb.reconciliation.reconciliation.service.ProcessMasterService;


@RestController
@RequestMapping("/api/v1")
public class ProcessMasterController {

	@Autowired
	ProcessMasterService processMasterService;

	@Autowired
	ReconFileDetailsMasterRepository fileDetailsMasterRepository;

	@Autowired
	ReconProcessDefMasterRepository processDefMasterRepository;

	@GetMapping(value = "get-process", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getAllProcessData(@RequestParam String menuFlag){
		return processMasterService.getAllProcessData(menuFlag);
	}

	// Flat file/process pickers for Add Menu's Extraction/Reconciliation Submenu step —
	// deliberately independent of PROCESS_MASTER_TBL (the get-process endpoint above's grouping
	// parent), since that table is a separate, often-unseeded concept unrelated to which
	// files/processes actually exist. menuProcessId is always the file's/process's own PK
	// (RFD_FILE_ID / RPM_PROCESS_ID) — this never changes, only how it's picked.
	@GetMapping(value = "get-files", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getAllFiles() {
		List<Object> files = fileDetailsMasterRepository.findAll().stream()
				.map(f -> {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("reconFileId", f.getReconFileId());
					m.put("reconFileName", f.getReconFileName());
					return (Object) m;
				})
				.collect(Collectors.toList());
		return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Files found successfully", files), HttpStatus.OK);
	}

	@GetMapping(value = "get-recon-processes", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getAllReconProcesses() {
		List<Object> processes = processDefMasterRepository.findAll().stream()
				.map(p -> {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("reconProcessId", p.getReconProcessId());
					m.put("reconProcessName", p.getReconProcessName());
					return (Object) m;
				})
				.collect(Collectors.toList());
		return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Processes found successfully", processes), HttpStatus.OK);
	}
}
