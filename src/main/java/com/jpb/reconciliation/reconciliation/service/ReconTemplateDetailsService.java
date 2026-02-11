package com.jpb.reconciliation.reconciliation.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;

@Service
public interface ReconTemplateDetailsService {

	ResponseEntity<?> addTemplate(ReconTemplateDetailsDto reconTemplateDetailsDto);

}
