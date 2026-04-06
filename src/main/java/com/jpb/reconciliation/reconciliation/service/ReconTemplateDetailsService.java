package com.jpb.reconciliation.reconciliation.service;


import org.springframework.http.ResponseEntity;

import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.dto.TemplateFieldDto;

public interface ReconTemplateDetailsService {

    ResponseEntity<RestWithMapStatusList> addTemplate(ReconTemplateDetailsDto dto);

    ResponseEntity<RestWithMapStatusList> configureTemplateAndFieldData(TemplateFieldDto request);

    ResponseEntity<RestWithMapStatusList> updateTemplate(Long templateId, TemplateFieldDto request);

    ResponseEntity<RestWithMapStatusList> viewTemplate(int page, int size);

    ResponseEntity<RestWithMapStatusList> deleteTemplate(Long templateId);

    ResponseEntity<RestWithMapStatusList> searchTemplate(String name, String type, int page, int size);

    ResponseEntity<RestWithMapStatusList> getTemplateById(Long templateId);
}
