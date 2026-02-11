package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.constants.CommonConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.service.ReconTemplateDetailsService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping(path = "/api/v1/template/")
public class ReconTemplateDetailsController {
	
	@Autowired
	ReconTemplateDetailsService reconTemplateDetailsService;
      
	@PostMapping(value = "add-template", produces = CommonConstants.APPLICATION_JSON)
	public ResponseEntity<?> addTemplate(@RequestBody ReconTemplateDetailsDto reconTemplateDetailsDto){
		return reconTemplateDetailsService.addTemplate(reconTemplateDetailsDto);
	}
 }
