package com.jpb.reconciliation.reconciliation.service;


import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.ReconTemplateConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;

public interface ReconFileTemplateConfigService {

//    ResponseEntity<RestWithMapStatusList> addTemplate(ReconTemplateDetailsDto dto);

    ResponseEntity<RestWithMapStatusList> configureTemplateAndFieldData(ReconTemplateConfigRequest request);

    ResponseEntity<RestWithMapStatusList> updateTemplate(Long templateId, ReconTemplateConfigRequest request);

    ResponseEntity<RestWithMapStatusList> viewTemplate(int page, int size);

    ResponseEntity<RestWithMapStatusList> deleteTemplate(Long templateId);

    ResponseEntity<RestWithMapStatusList> searchTemplate(String name, String type, int page, int size);

    ResponseEntity<RestWithMapStatusList> getTemplateById(Long templateId);
}
