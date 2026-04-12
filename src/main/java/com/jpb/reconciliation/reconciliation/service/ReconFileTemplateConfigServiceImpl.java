
package com.jpb.reconciliation.reconciliation.service;


import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpb.reconciliation.reconciliation.dto.ReconFieldConfigurationDto;
import com.jpb.reconciliation.reconciliation.dto.ReconFileTemplateMastDto;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplatesDetailsDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithMapStatusList;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldFormatMast;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldTypeMast;
import com.jpb.reconciliation.reconciliation.entity.ReconFileTmpltMast;
import com.jpb.reconciliation.reconciliation.entity.ReconSftpServerMast;
import com.jpb.reconciliation.reconciliation.entity.ReconTmpltFieldDtls;
import com.jpb.reconciliation.reconciliation.mapper.ReconFieldDetailsMapper;
import com.jpb.reconciliation.reconciliation.mapper.ReconFileTemplateMastMapper;
import com.jpb.reconciliation.reconciliation.mapper.ReconSftpServerMapper;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldFormatMastRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldTypeMastRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFileTmpltMastRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconSftpServerMastRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconTmpltFieldDtlsRepository;
import com.jpb.reconciliation.reconciliation.util.CommonUtil;
import com.jpb.reconciliation.reconciliation.util.ResponseBuilder;

@Service
@Transactional(readOnly = true)
public class ReconFileTemplateConfigServiceImpl implements ReconFileTemplateConfigService {

    Logger logger = LoggerFactory.getLogger(ReconFileTemplateConfigServiceImpl.class);

    @Autowired ReconFileTmpltMastRepository    templateRepository;
    @Autowired ReconFieldFormatMastRepository  fieldFormatRepository;
    @Autowired ReconFieldTypeMastRepository    fieldTypeRepository;
    @Autowired ReconTmpltFieldDtlsRepository   fieldDetailsRepository;
    @Autowired ReconFieldDtlMastService        reconFieldDtlMastService;
    @Autowired ObjectMapper                    objectMapper;
    
    @Autowired
    ReconSftpServerService  reconSftpServerService;
    
    
    @Autowired
    ReconSftpServerMastRepository sftpServerRepo;
    
    @Autowired
    ReconSftpServerMapper reconSftpServerMapper;

    @Autowired @Lazy
    private ReconFileTemplateConfigServiceImpl self;

    private final JdbcTemplate jdbcTemplate;

    public ReconFileTemplateConfigServiceImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // =========================================================================
    // ADD TEMPLATE
    // =========================================================================

//    @Override
//    public ResponseEntity<RestWithMapStatusList> addTemplate(ReconTemplateDetailsDto dto) {
//        ReconFileTmpltMast template = ReconFileTemplateMastMapper
//                .mapToReconFileTmpltMast(dto, new ReconFileTmpltMast());
//
//        if (template == null) {
//            return ResponseEntity.badRequest().body(
//                    ResponseBuilder.failure("Template could not be configured."));
//        }
//
//        templateRepository.save(template);
//
//        Map<String, Object> row = new LinkedHashMap<>();
//        row.put("templateId",   template.getTemplateId());
//        row.put("templateCode", template.getTemplateCode());
//        row.put("templateName", template.getTemplateName());
//        row.put("status",       template.getStatus());
//
//        return new ResponseEntity<>(
//                ResponseBuilder.ok("Template successfully configured.", "template", List.of(row)),
//                HttpStatus.CREATED);
//    }

    // =========================================================================
    // CONFIGURE TEMPLATE + FIELDS (CREATE)
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> configureTemplateAndFieldData(ReconTemplateConfigRequest request) {

        ReconFileTmpltMast existing = templateRepository.findByTemplateName(request.getTemplateName());
        if (existing != null) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("Template already configured."),
                    HttpStatus.BAD_REQUEST);
        }

        ReconFileTmpltMast savedTemplate;
        try {
            savedTemplate = self.saveTemplateAndFields(request);
            logger.info("Template saved. ID: {}, Code: {}",
                    savedTemplate.getTemplateId(), savedTemplate.getTemplateCode());
            
            
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.failure(e.getMessage()),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error saving template: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to save template: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

//        String spResult;
//        try {
//            spResult = self.callStageTableProcedure(savedTemplate);
//            logger.info("SP result for [{}]: {}", savedTemplate.getTemplateCode(), spResult);
//        } catch (Exception e) {
//            logger.error("SP exception for [{}]. Rolling back.", savedTemplate.getTemplateCode(), e);
//            self.rollbackTemplateAndFields(savedTemplate.getTemplateId());
//            return new ResponseEntity<>(
//                    ResponseBuilder.error(
//                            "Stage table creation failed. Template rolled back. " + e.getMessage()),
//                    HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//
//        if (spResult == null || !spResult.equalsIgnoreCase("OK")) {
//            logger.warn("SP returned [{}] for [{}]. Rolling back.",
//                    spResult, savedTemplate.getTemplateCode());
//            self.rollbackTemplateAndFields(savedTemplate.getTemplateId());
//            return new ResponseEntity<>(
//                    ResponseBuilder.error(
//                            "Stage table creation failed. Template rolled back. Reason: " + spResult),
//                    HttpStatus.INTERNAL_SERVER_ERROR);
//        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("templateId",   savedTemplate.getTemplateId());
        row.put("templateCode", savedTemplate.getTemplateCode());
        row.put("templateName", savedTemplate.getTemplateName());
        row.put("stageTable",   savedTemplate.getStageTabName());

        return ResponseEntity.ok(
                ResponseBuilder.ok("Template configured successfully.", "template", List.of(row)));
    }

    // =========================================================================
    // UPDATE TEMPLATE + FIELDS
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> updateTemplate(Long templateId,
                                                                 ReconTemplateConfigRequest request) {
        ReconFileTmpltMast existing = templateRepository
                .findByTemplateIdAndIsDeleted(templateId, "N")
                .orElse(null);
        if (existing == null) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("Template not found: " + templateId),
                    HttpStatus.NOT_FOUND);
        }

        List<ReconTmpltFieldDtls> prevFields =
                fieldDetailsRepository.findActiveFieldsByTemplateId(existing.getTemplateId());
        ReconFileTmpltMast prevSnapshot = copyTemplateSnapshot(existing);

        ReconFileTmpltMast updatedTemplate;
        try {
            updatedTemplate = self.updateTemplateAndFields(templateId, request);
            logger.info("Template updated. ID: {}", updatedTemplate.getTemplateId());
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ResponseBuilder.error("Failed to update template: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String spResult;
        try {
            spResult = self.callStageTableProcedure(updatedTemplate);
        } catch (Exception e) {
            self.restoreTemplateAndFields(prevSnapshot, prevFields);
            return new ResponseEntity<>(
                    ResponseBuilder.error(
                            "Stage table update failed. Previous state restored. " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (spResult == null || !spResult.equalsIgnoreCase("OK")) {
            self.restoreTemplateAndFields(prevSnapshot, prevFields);
            return new ResponseEntity<>(
                    ResponseBuilder.error(
                            "Stage table update failed. Previous state restored. Reason: " + spResult),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("templateId",   updatedTemplate.getTemplateId());
        row.put("templateCode", updatedTemplate.getTemplateCode());
        row.put("templateName", updatedTemplate.getTemplateName());

        return ResponseEntity.ok(
                ResponseBuilder.ok("Template updated successfully.", "template", List.of(row)));
    }

    // =========================================================================
    // VIEW TEMPLATE — paginated
    //
    // FIX 1: empty page now returns "pagination" key too (consistent with non-empty)
    // FIX 2: removed duplicate buildPaginationMap — uses ResponseBuilder.okPaged()
    // FIX 3: input validation (page < 0, size out of range) preserved
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> viewTemplate(int page, int size) {
        try {
            if (page < 0) {
                return ResponseEntity.badRequest().body(
                        ResponseBuilder.failure("Page number cannot be negative."));
            }
            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest().body(
                        ResponseBuilder.failure("Size must be between 1 and 100."));
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<ReconFileTmpltMast> templatePage = templateRepository.findTemplates(pageable);

            // FIX: empty path now goes through okPaged so "pagination" key is always present
            if (templatePage.isEmpty()) {
                return ResponseEntity.ok(
                        ResponseBuilder.okPaged(
                                "No templates available.",
                                "templates",
                                Collections.emptyList(),
                                templatePage));        // ← pagination metadata included
            }

            List<ReconFileTmpltMast> withDetails =
                    templateRepository.fetchTemplateDetails(templatePage.getContent());
            List<ReconFileTemplateMastDto> dtos = ReconFileTemplateMastMapper.toDTOList(withDetails);
            List<Map<String, Object>> rows = ResponseBuilder.toMapList(new ArrayList<>(dtos), objectMapper);

            return ResponseEntity.ok(
                    ResponseBuilder.okPaged(
                            "Templates retrieved successfully.",
                            "templates",
                            rows,
                            templatePage));

        } catch (Exception e) {
            logger.error("Error fetching templates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseBuilder.error("Error retrieving templates: " + e.getMessage()));
        }
    }

    // =========================================================================
    // DELETE TEMPLATE
    // =========================================================================

    @Override
    @Transactional
    public ResponseEntity<RestWithMapStatusList> deleteTemplate(Long templateId) {
        Optional<ReconFileTmpltMast> opt =
                templateRepository.findByTemplateIdAndIsDeleted(templateId, "N");
        if (!opt.isPresent()) {
            return new ResponseEntity<>(
                    ResponseBuilder.failure("Template not found: " + templateId),
                    HttpStatus.NOT_FOUND);
        }

        fieldDetailsRepository.deleteByTemplateId(opt.get().getTemplateId());
        templateRepository.deleteById(templateId);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("deletedTemplateId", templateId);

        return ResponseEntity.ok(
                ResponseBuilder.ok("Template deleted successfully.", "deleted", List.of(row)));
    }

    // =========================================================================
    // SEARCH TEMPLATE — paginated
    //
    // FIX 1: empty results now returns "pagination" key (consistent with viewTemplate)
    // FIX 2: removed duplicate buildPaginationMap — uses ResponseBuilder.okPaged()
    // FIX 3: added input validation matching viewTemplate
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> searchTemplate(String name, String type,
                                                                 int page, int size) {
        if (page < 0) {
            return ResponseEntity.badRequest().body(
                    ResponseBuilder.failure("Page number cannot be negative."));
        }
        if (size <= 0 || size > 100) {
            return ResponseEntity.badRequest().body(
                    ResponseBuilder.failure("Size must be between 1 and 100."));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ReconFileTmpltMast> results;

        if (name != null && type != null) {
            results = templateRepository
                    .findByTemplateNameContainingIgnoreCaseAndTemplateType(name, type, pageable);
        } else if (name != null) {
            results = templateRepository
                    .findByTemplateNameContainingIgnoreCase(name, pageable);
        } else if (type != null) {
            results = templateRepository.findByTemplateType(type, pageable);
        } else {
            results = templateRepository.findTemplates(pageable);
        }

        // FIX: empty results now consistent — "pagination" key always present
        if (results.isEmpty()) {
            return ResponseEntity.ok(
                    ResponseBuilder.okPaged(
                            "No templates found matching the search criteria.",
                            "templates",
                            Collections.emptyList(),
                            results));
        }

        List<ReconFileTemplateMastDto> dtos = ReconFileTemplateMastMapper.toDTOList(results.getContent());
        List<Map<String, Object>> rows = ResponseBuilder.toMapList(new ArrayList<>(dtos), objectMapper);

        return ResponseEntity.ok(
                ResponseBuilder.okPaged("Templates fetched successfully.", "templates", rows, results));
    }

    // =========================================================================
    // GET TEMPLATE BY ID
    // =========================================================================

    @Override
    public ResponseEntity<RestWithMapStatusList> getTemplateById(Long templateId) {
        try {
            Optional<ReconFileTmpltMast> opt =
                    templateRepository.findByTemplateIdAndIsDeleted(templateId, "N");
            if (!opt.isPresent()) {
                return new ResponseEntity<>(
                        ResponseBuilder.failure("Template not found: " + templateId),
                        HttpStatus.NOT_FOUND);
            }

            ReconFileTemplateMastDto dto = ReconFileTemplateMastMapper.toDTO(opt.get());
            Map<String, Object> row = ResponseBuilder.toMap(dto, objectMapper);

            return ResponseEntity.ok(
                    ResponseBuilder.ok("Template fetched successfully.", "template", List.of(row)));

        } catch (Exception e) {
            logger.error("Error fetching template {}: {}", templateId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseBuilder.error("Error fetching template: " + e.getMessage()));
        }
    }

    // =========================================================================
    // INNER TRANSACTIONAL HELPERS (called via self-proxy)
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconFileTmpltMast saveTemplateAndFields(ReconTemplateConfigRequest request) {
    	
    	 ReconSftpServerMast reconSftpServerMast = reconSftpServerMapper.toEntity(request.getSftpServerDetails());
         sftpServerRepo.save(reconSftpServerMast);
    	
        ReconFileTmpltMast template = ReconFileTemplateMastMapper
                .mapTemplateDtoToFileTmpltMast(request, new ReconFileTmpltMast());
        template.setTemplateCode(generateTemplateCode());
        template.setStageTabName(generateStageTableName(template));
        template.setStatus("ACTIVE");
        template.setIsDeleted("N");
        template.setSftpServerId(reconSftpServerMast.getServerId());
        templateRepository.save(template);
        List<ReconTmpltFieldDtls> fields = buildFieldEntities(request.getFieldDetails(), template);
        fieldDetailsRepository.saveAll(fields);
        logger.info("Saved {} fields for Template [{}]", fields.size(), template.getTemplateCode());
        
        
        
        
        
        return template;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconFileTmpltMast updateTemplateAndFields(Long templateId, ReconTemplateConfigRequest request) {
        ReconFileTmpltMast template = templateRepository
                .findByTemplateIdAndIsDeleted(templateId, "N")
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        if (request.getTemplateName()      != null) template.setTemplateName(request.getTemplateName());
        if (request.getTemplateType()      != null) template.setTemplateType(request.getTemplateType());
        if (request.getColumnCount()       != null) template.setColumnCount(Long.valueOf(request.getColumnCount()));
        if (request.getReversalIndicator() != null) template.setReversalIndicator(request.getReversalIndicator());
        if (request.getDataReference()     != null) template.setDataReferenceFlag(request.getDataReference());
        if (request.getOnlineRefund()      != null) template.setOnlRefundFlag(request.getOnlineRefund());

        templateRepository.save(template);
        reconFieldDtlMastService.deleteFieldsByTemplateId(template.getTemplateId());
        fieldDetailsRepository.saveAll(buildFieldEntities(request.getFieldDetails(), template));
        return template;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String callStageTableProcedure(ReconFileTmpltMast template) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_STAGE_TAB_CREATION")
                .declareParameters(
                        new SqlParameter("Prm_tmplt_Id", Types.NUMERIC),
                        new SqlOutParameter("Prm_Error", Types.VARCHAR));
        Map<String, Object> params = new HashMap<>();
        params.put("Prm_tmplt_Id", template.getTemplateId());
        Map<String, Object> result = call.execute(params);
        return (String) result.get("Prm_Error");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackTemplateAndFields(Long templateId) {
        try {
            fieldDetailsRepository.deleteByTemplateId(templateId);
            templateRepository.deleteById(templateId);
            logger.info("Rolled back template {}", templateId);
        } catch (Exception e) {
            logger.error("Rollback failed for template {}: {}", templateId, e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreTemplateAndFields(ReconFileTmpltMast snapshot,
                                          List<ReconTmpltFieldDtls> prevFields) {
        try {
            templateRepository.save(snapshot);
            fieldDetailsRepository.deleteByTemplateId(snapshot.getTemplateId());
            if (prevFields != null && !prevFields.isEmpty()) {
                prevFields.forEach(f -> f.setFieldId(null));
                fieldDetailsRepository.saveAll(prevFields);
            }
            logger.info("Restored template {}", snapshot.getTemplateId());
        } catch (Exception e) {
            logger.error("Restore failed for template {}: {}", snapshot.getTemplateId(), e.getMessage(), e);
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private List<ReconTmpltFieldDtls> buildFieldEntities(List<ReconFieldConfigurationDto> dtos,
                                                           ReconFileTmpltMast template) {
        List<ReconTmpltFieldDtls> fields = new ArrayList<>();
        for (ReconFieldConfigurationDto dto : dtos) {
            ReconFieldTypeMast type = fieldTypeRepository
                    .findByFieldTypeDes(dto.getFieldtype())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid Field Type: " + dto.getFieldtype()));
            ReconFieldFormatMast format = fieldFormatRepository
                    .findByReconFieldFormatDesc(dto.getFieldFormat())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid Field Format: " + dto.getFieldFormat()));
            fields.add(ReconFieldDetailsMapper.mapFieldDtoToEntity(dto, template, type, format));
        }
        return fields;
    }

    private ReconFileTmpltMast copyTemplateSnapshot(ReconFileTmpltMast src) {
        ReconFileTmpltMast snap = new ReconFileTmpltMast();
        snap.setTemplateId(src.getTemplateId());
        snap.setTemplateName(src.getTemplateName());
        snap.setTemplateType(src.getTemplateType());
        snap.setTemplateCode(src.getTemplateCode());
        snap.setColumnCount(src.getColumnCount());
        snap.setReversalIndicator(src.getReversalIndicator());
        snap.setDataReferenceFlag(src.getDataReferenceFlag());
        snap.setOnlRefundFlag(src.getOnlRefundFlag());
        snap.setStageTabName(src.getStageTabName());
        snap.setSubTemplateId(src.getSubTemplateId());
        snap.setStatus(src.getStatus());
        snap.setTenantId(src.getTenantId());
        snap.setIsDeleted(src.getIsDeleted());
        return snap;
    }

    private String generateTemplateCode() {
        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        long seq = templateRepository.countByTemplateCodeStartingWith("TMPLT-" + date);
        return String.format("TMPLT-%s-%04d", date, seq + 1);
    }

    private String generateStageTableName(ReconFileTmpltMast t) {
        return "RECON_" + CommonUtil.removeAllWhitespace(
                t.getTemplateCode()).toUpperCase().replace("-", "_") + "_STAGE_T";
    }
}

