package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;

public class ReconTemplateDetailsMapper {

	public static ReconTemplateDetails mapToReconTemplateDetails(ReconTemplateDetailsDto reconTemplateDetailsDto,
			ReconTemplateDetails reconTemplateDetails) {
		
		reconTemplateDetails.setInsertCode(reconTemplateDetailsDto.getInsertCode());
		reconTemplateDetails.setInsertUser(reconTemplateDetailsDto.getInsertUser());
		reconTemplateDetails.setReconColumnCnt(reconTemplateDetailsDto.getReconColumnCnt());
		reconTemplateDetails.setReconDataTableInd(reconTemplateDetailsDto.getReconDataTableInd());
		reconTemplateDetails.setReconExistFlag(reconTemplateDetailsDto.getReconExistFlag());
		reconTemplateDetails.setReconInsertDate(reconTemplateDetailsDto.getReconInsertDate());
		reconTemplateDetails.setReconIssacqFlag(reconTemplateDetailsDto.getReconIssacqFlag());
		reconTemplateDetails.setReconLastUpdatedDate(reconTemplateDetailsDto.getReconLastUpdatedDate());
		reconTemplateDetails.setReconLastUpdatedUser(reconTemplateDetailsDto.getReconLastUpdatedUser());
		reconTemplateDetails.setReconMasterFlag(reconTemplateDetailsDto.getReconMasterFlag());
		reconTemplateDetails.setReconMasterTemplateId(reconTemplateDetailsDto.getReconMasterTemplateId());
		reconTemplateDetails.setReconOnlRefFlag(reconTemplateDetailsDto.getReconOnlRefFlag());
		reconTemplateDetails.setReconRefFlag(reconTemplateDetailsDto.getReconRefFlag());
		reconTemplateDetails.setReconReversalInd(reconTemplateDetailsDto.getReconReversalInd());
		reconTemplateDetails.setReconStageTabName(reconTemplateDetailsDto.getReconStageTabName());
		reconTemplateDetails.setReconTemplateId(reconTemplateDetailsDto.getReconTemplateId());
		reconTemplateDetails.setReconTemplateName(reconTemplateDetailsDto.getReconTemplateName());
		reconTemplateDetails.setReconTypeId(reconTemplateDetailsDto.getReconTypeId());
		reconTemplateDetails.setSubTemplateId(reconTemplateDetailsDto.getSubTemplateId());
		return reconTemplateDetails;
	}

	public static ReconTemplateDetailsDto mapToReconTemplateDetailsDto(ReconTemplateDetails reconTemplateDetails,
			ReconTemplateDetailsDto reconTemplateDetailsDto) {

		return reconTemplateDetailsDto;
	}
}

