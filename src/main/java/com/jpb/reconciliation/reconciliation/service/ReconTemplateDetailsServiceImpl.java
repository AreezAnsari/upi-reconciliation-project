package com.jpb.reconciliation.reconciliation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.constants.MenuConstants;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.mapper.ReconTemplateDetailsMapper;
import com.jpb.reconciliation.reconciliation.repository.ReconTemplateDetailsRepository;

@Service
public class ReconTemplateDetailsServiceImpl implements ReconTemplateDetailsService {

	@Autowired
	ReconTemplateDetailsRepository reconTemplateDetailsRepository;

	@Override
	public ResponseEntity<?> addTemplate(ReconTemplateDetailsDto reconTemplateDetailsDto) {
		ReconTemplateDetails templateDetails = ReconTemplateDetailsMapper
				.mapToReconTemplateDetails(reconTemplateDetailsDto, new ReconTemplateDetails());

		if (templateDetails != null) {
			reconTemplateDetailsRepository.save(templateDetails);
			return new ResponseEntity<>(new ResponseDto(MenuConstants.STATUS_201, "Template Successfully created"),
					HttpStatus.CREATED);
		}

		return new ResponseEntity<>(new ResponseDto(MenuConstants.STATUS_417, "Template not created"),
				HttpStatus.BAD_REQUEST);
	}

}
