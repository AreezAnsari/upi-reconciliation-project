package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.ProcessMasterService;


@RestController
@RequestMapping("/api/v1/")
public class ProcessMasterController {
	
	@Autowired
	ProcessMasterService processMasterService;
	
	@GetMapping(value = "get-process", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<RestWithStatusList> getAllProcessData(@RequestParam String menuFlag){
		return processMasterService.getAllProcessData(menuFlag);
	}
}
