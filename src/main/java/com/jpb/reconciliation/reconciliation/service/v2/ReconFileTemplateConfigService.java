package com.jpb.reconciliation.reconciliation.service.v2;


import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.v2.ReconTemplateConfigRequest;

public interface ReconFileTemplateConfigService {

//    ResponseEntity<RestWithMapStatusList> addTemplate(ReconTemplateDetailsDto dto);

    ResponseEntity<RestWithMapStatusList> configureTemplateAndFieldData(ReconTemplateConfigRequest request);

    ResponseEntity<RestWithMapStatusList> updateTemplateById(Long templateId, ReconTemplateConfigRequest request);

    ResponseEntity<RestWithMapStatusList> viewTemplate(String action, int page, int size);

    ResponseEntity<RestWithMapStatusList> deleteTemplate(Long templateId);

    ResponseEntity<RestWithMapStatusList> searchTemplate(String name, String type, int page, int size);

    ResponseEntity<RestWithMapStatusList> getTemplateById(Long templateId);
}
