package com.jpb.reconciliation.reconciliation.service;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

import com.jpb.reconciliation.reconciliation.constants.MenuConstants;
import com.jpb.reconciliation.reconciliation.dto.FieldConfigurationDto;
import com.jpb.reconciliation.reconciliation.dto.PageMetadata;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateDetailsDto;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplatesDetailsDTO;
import com.jpb.reconciliation.reconciliation.dto.ResponseDto;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusListPagination;
import com.jpb.reconciliation.reconciliation.dto.TemplateFieldDto;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldDetailsMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldFormatMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldTypeMaster;
import com.jpb.reconciliation.reconciliation.entity.ReconTemplateDetails;
import com.jpb.reconciliation.reconciliation.mapper.ReconFieldDetailsMapper;
import com.jpb.reconciliation.reconciliation.mapper.ReconTemplateDetailsMapper;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldDetailsMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldFormatMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconFieldTypeMasterRepository;
import com.jpb.reconciliation.reconciliation.repository.ReconTemplateDetailsRepository;
import com.jpb.reconciliation.reconciliation.util.CommonUtil;

@Service
@Transactional(readOnly = true)
public class ReconTemplateDetailsServiceImpl implements ReconTemplateDetailsService {

    Logger logger = LoggerFactory.getLogger(ReconTemplateDetailsServiceImpl.class);

    @Autowired
    ReconTemplateDetailsRepository reconTemplateDetailsRepository;

    @Autowired
    ReconFieldFormatMasterRepository reconFieldFormatRepository;

    @Autowired
    ReconFieldTypeMasterRepository reconFieldTypeRepository;

    @Autowired
    ReconFieldDetailsMasterRepository reconFieldDetailsRepository;

    @Autowired
    private ReconFieldDtlMastService reconFieldDtlMastService;

    // ✅ Self-injection via @Lazy to allow REQUIRES_NEW proxy calls within same bean
    @Autowired
    @Lazy
    private ReconTemplateDetailsServiceImpl self;

    private final JdbcTemplate jdbcTemplate;

    public ReconTemplateDetailsServiceImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ADD TEMPLATE (simple, unchanged)
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<?> addTemplate(ReconTemplateDetailsDto reconTemplateDetailsDto) {
        ReconTemplateDetails templateDetails = ReconTemplateDetailsMapper
                .mapToReconTemplateDetails(reconTemplateDetailsDto, new ReconTemplateDetails());

        if (templateDetails != null) {
            reconTemplateDetailsRepository.save(templateDetails);
            return new ResponseEntity<>(
                    new ResponseDto(MenuConstants.STATUS_201, "Template Successfully Configured."),
                    HttpStatus.CREATED);
        }

        return new ResponseEntity<>(
                new ResponseDto(MenuConstants.STATUS_417, "Template not Configured."),
                HttpStatus.BAD_REQUEST);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CONFIGURE TEMPLATE + FIELDS  →  Then call stored procedure
    //
    // FLOW:
    //   Step 1 [REQUIRES_NEW tx] → Save template + fields → commit immediately
    //   Step 2 [REQUIRES_NEW tx] → Call SP_STAGE_TAB_CREATION (now sees committed data)
    //   Step 3                   → If SP fails → rollback template + fields manually
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    // ✅ NOT @Transactional here — we orchestrate two separate inner transactions
    public ResponseEntity<RestWithStatusList> configureTemplateAndFieldData(TemplateFieldDto request) {

        // ── Duplicate check ──────────────────────────────────────────────────────
        ReconTemplateDetails templateExists = reconTemplateDetailsRepository
                .findByTemplateName(request.getTemplateName());
        if (templateExists != null) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Template already configured.", null),
                    HttpStatus.BAD_REQUEST);
        }

        // ── Step 1: Save template + fields in their OWN committed transaction ───
        ReconTemplateDetails savedTemplate;
        try {
            savedTemplate = self.saveTemplateAndFields(request);
            logger.info("Template and fields saved. Template ID: {}", savedTemplate.getReconTemplateId());
        } catch (IllegalArgumentException e) {
            logger.error("Validation error while saving template/fields: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", e.getMessage(), null),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error while saving template/fields: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR", "Failed to save template data: " + e.getMessage(), null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // ── Step 2: Call stored procedure in its OWN transaction ────────────────
        // Data is now committed → SP can read it correctly
        String procedureResult;
        try {
            procedureResult = self.callStageTableProcedure(savedTemplate);
            logger.info("SP_STAGE_TAB_CREATION result for Template ID {}: {}", savedTemplate.getReconTemplateId(), procedureResult);
        } catch (Exception e) {
            // ── Step 3: SP call itself threw an exception → rollback saved data ─
            logger.error("Exception during SP_STAGE_TAB_CREATION for Template ID {}. Rolling back template and fields. Error: {}",
                    savedTemplate.getReconTemplateId(), e.getMessage(), e);
            self.rollbackTemplateAndFields(savedTemplate.getReconTemplateId());
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR",
                            "Stage table creation failed (exception). Template data rolled back. Error: " + e.getMessage(),
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // ── Step 3: SP returned an error message → rollback saved data ──────────
        if (procedureResult == null || !procedureResult.equalsIgnoreCase("OK")) {
            logger.warn("SP_STAGE_TAB_CREATION returned failure for Template ID {}. Reason: {}. Rolling back.",
                    savedTemplate.getReconTemplateId(), procedureResult);
            self.rollbackTemplateAndFields(savedTemplate.getReconTemplateId());
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR",
                            "Stage table creation failed. Template data rolled back. Reason: " + procedureResult,
                            null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        logger.info("Template configured successfully. Template ID: {}", savedTemplate.getReconTemplateId());
        return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Template configured successfully", null));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // STEP 1 HELPER — Save template + fields, commit immediately via REQUIRES_NEW
    // ─────────────────────────────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconTemplateDetails saveTemplateAndFields(TemplateFieldDto request) {

        // Map and save template
        ReconTemplateDetails template = ReconTemplateDetailsMapper
                .mapTemplateDtoToTemplate(request, new ReconTemplateDetails());
        template.setStageTabName(generateStagetableName(template));
        reconTemplateDetailsRepository.save(template);
        logger.info("Template saved with ID: {}", template.getReconTemplateId());

        // Map and save fields
        List<ReconFieldDetailsMaster> fieldEntities = new ArrayList<>();
        for (FieldConfigurationDto fieldDto : request.getFieldDetails()) {

            ReconFieldTypeMaster fieldType = reconFieldTypeRepository
                    .findByFieldTypeDes(fieldDto.getFieldtype())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid Field Type: " + fieldDto.getFieldtype()));

            ReconFieldFormatMaster fieldFormat = reconFieldFormatRepository
                    .findByReconFieldFormatDesc(fieldDto.getFieldFormat())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid Field Format: " + fieldDto.getFieldFormat()));

            fieldEntities.add(ReconFieldDetailsMapper.mapFieldDtoToEntity(fieldDto, template, fieldType, fieldFormat));
        }

        reconFieldDetailsRepository.saveAll(fieldEntities);
        logger.info("Saved {} field(s) for Template ID: {}", fieldEntities.size(), template.getReconTemplateId());

        // Transaction commits here → data visible to SP
        return template;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // STEP 2 HELPER — Call stored procedure in its own transaction
    //                 Returns "OK" on success, or the error string on failure
    // ─────────────────────────────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String callStageTableProcedure(ReconTemplateDetails template) {

        Long templateId = template.getReconTemplateId();

        // ✅ Create SimpleJdbcCall as local variable (not a shared field — thread-safe)
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_STAGE_TAB_CREATION")
                .declareParameters(
                        new SqlParameter("Prm_tmplt_Id", Types.NUMERIC),
                        new SqlOutParameter("Prm_Error", Types.VARCHAR));

        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("Prm_tmplt_Id", templateId);
        logger.info("Calling SP_STAGE_TAB_CREATION with Template ID: {}", templateId);

        Map<String, Object> result = jdbcCall.execute(inputParams);
        String procedureMsg = (String) result.get("Prm_Error");
        logger.info("SP_STAGE_TAB_CREATION returned: {} for Template ID: {}", procedureMsg, templateId);

        return procedureMsg;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // STEP 3 HELPER — Rollback: delete fields + template if SP fails
    // ─────────────────────────────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackTemplateAndFields(Long templateId) {
        try {
            reconFieldDetailsRepository.deleteByTemplateId(templateId);
            logger.info("Rolled back field details for Template ID: {}", templateId);

            reconTemplateDetailsRepository.deleteById(templateId);
            logger.info("Rolled back template for Template ID: {}", templateId);

        } catch (Exception e) {
            // Log but do not rethrow — rollback is best-effort at this point
            logger.error("Failed to rollback template/fields for Template ID {}: {}", templateId, e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // STAGE TABLE NAME GENERATOR
    // ─────────────────────────────────────────────────────────────────────────────

    private String generateStagetableName(ReconTemplateDetails template) {
        return "REC_" + CommonUtil.removeAllWhitespace(template.getTemplateName()).toUpperCase() + "_STAGE_T";
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // VIEW TEMPLATE (unchanged)
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RestWithStatusListPagination> viewTemplate(int page, int size) {
        try {
            logger.info("Fetching templates - Page: {}, Size: {}", page, size);

            if (page < 0) {
                return ResponseEntity.badRequest().body(
                        RestWithStatusListPagination.builder()
                                .status("ERROR")
                                .statusMsg("Page number cannot be negative")
                                .data(Collections.emptyList())
                                .build());
            }

            if (size <= 0 || size > 100) {
                return ResponseEntity.badRequest().body(
                        RestWithStatusListPagination.builder()
                                .status("ERROR")
                                .statusMsg("Size must be between 1 and 100")
                                .data(Collections.emptyList())
                                .build());
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<ReconTemplateDetails> templatesPage = reconTemplateDetailsRepository.findTemplates(pageable);

            if (templatesPage.isEmpty()) {
                logger.warn("No templates found for page: {}", page);
                return ResponseEntity.ok(
                        RestWithStatusListPagination.builder()
                                .status("SUCCESS")
                                .statusMsg("No templates available")
                                .data(Collections.emptyList())
                                .pageMetadata(PageMetadata.builder()
                                        .currentPage(page).pageSize(size)
                                        .totalElements(0L).totalPages(0)
                                        .isFirst(true).isLast(true)
                                        .hasNext(false).hasPrevious(false)
                                        .build())
                                .build());
            }

            List<ReconTemplateDetails> templatesWithDetails =
                    reconTemplateDetailsRepository.fetchTemplateDetails(templatesPage.getContent());

            List<ReconTemplatesDetailsDTO> templateDTOs =
                    ReconTemplateDetailsMapper.toDTOList(templatesWithDetails);

            PageMetadata pageMetadata = PageMetadata.builder()
                    .currentPage(templatesPage.getNumber())
                    .pageSize(templatesPage.getSize())
                    .totalElements(templatesPage.getTotalElements())
                    .totalPages(templatesPage.getTotalPages())
                    .isFirst(templatesPage.isFirst())
                    .isLast(templatesPage.isLast())
                    .hasNext(templatesPage.hasNext())
                    .hasPrevious(templatesPage.hasPrevious())
                    .build();

            logger.info("Successfully retrieved {} templates out of {} total on page {}",
                    templateDTOs.size(), templatesPage.getTotalElements(), page);

            return ResponseEntity.ok(
                    RestWithStatusListPagination.builder()
                            .status("SUCCESS")
                            .statusMsg("Templates retrieved successfully")
                            .data(new ArrayList<>(templateDTOs))
                            .pageMetadata(pageMetadata)
                            .build());

        } catch (Exception e) {
            logger.error("Error fetching templates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RestWithStatusListPagination.builder()
                            .status("ERROR")
                            .statusMsg("Error retrieving templates: " + e.getMessage())
                            .data(Collections.emptyList())
                            .build());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // UPDATE TEMPLATE (unchanged)
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> updateTemplate(Long templateId, TemplateFieldDto templateFieldRequest) {
        try {
            ReconTemplateDetails existingTemplate = reconTemplateDetailsRepository.findById(templateId)
                    .orElse(null);
            if (existingTemplate == null) {
                return new ResponseEntity<>(
                        new RestWithStatusList("FAILURE", "Template not found with ID: " + templateId, null),
                        HttpStatus.NOT_FOUND);
            }

            if (templateFieldRequest.getTemplateName() != null)
                existingTemplate.setTemplateName(templateFieldRequest.getTemplateName());
            if (templateFieldRequest.getTemplateType() != null)
                existingTemplate.setTemplateType(templateFieldRequest.getTemplateType());
            if (templateFieldRequest.getColumnCount() != null)
                existingTemplate.setColumnCount(templateFieldRequest.getColumnCount());
            if (templateFieldRequest.getReversalIndicator() != null)
                existingTemplate.setReversalIndicator(templateFieldRequest.getReversalIndicator());
            if (templateFieldRequest.getDataReference() != null)
                existingTemplate.setDataReferenceFlag(templateFieldRequest.getDataReference());
            if (templateFieldRequest.getOnlineRefund() != null)
                existingTemplate.setOnlRefundFlag(templateFieldRequest.getOnlineRefund());

            reconTemplateDetailsRepository.save(existingTemplate);

            reconFieldDtlMastService.deleteFieldsByTemplateId(existingTemplate.getReconTemplateId());

            List<ReconFieldDetailsMaster> newFields = new ArrayList<>();
            for (FieldConfigurationDto fieldDto : templateFieldRequest.getFieldDetails()) {

                ReconFieldTypeMaster fieldType = reconFieldTypeRepository
                        .findByFieldTypeDes(fieldDto.getFieldtype())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Invalid Field Type: " + fieldDto.getFieldtype()));

                ReconFieldFormatMaster fieldFormat = reconFieldFormatRepository
                        .findByReconFieldFormatDesc(fieldDto.getFieldFormat())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Invalid Field Format: " + fieldDto.getFieldFormat()));

                newFields.add(ReconFieldDetailsMapper.mapFieldDtoToEntity(
                        fieldDto, existingTemplate, fieldType, fieldFormat));
            }

            reconFieldDetailsRepository.saveAll(newFields);

        } catch (IllegalArgumentException e) {
            logger.error("Validation error while updating template fields: {}", e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", e.getMessage(), null),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Exception while updating template fields for ID {}: {}", templateId, e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR", "Failed to update template fields for ID: " + templateId, null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Template fields updated successfully", null),
                HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DELETE TEMPLATE (unchanged)
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ResponseEntity<RestWithStatusList> deleteTemplate(Long templateId) {
        Optional<ReconTemplateDetails> templateOpt = reconTemplateDetailsRepository.findById(templateId);
        if (!templateOpt.isPresent()) {
            return new ResponseEntity<>(
                    new RestWithStatusList("FAILURE", "Template not found with ID: " + templateId, null),
                    HttpStatus.NOT_FOUND);
        }

        reconFieldDetailsRepository.deleteByTemplateId(templateOpt.get().getReconTemplateId());
        reconTemplateDetailsRepository.deleteById(templateId);

        return new ResponseEntity<>(
                new RestWithStatusList("SUCCESS", "Template deleted successfully", null),
                HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SEARCH TEMPLATE (unchanged)
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<RestWithStatusListPagination> searchTemplate(
            String templateName, String templateType, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ReconTemplateDetails> results;

        if (templateName != null && templateType != null) {
            results = reconTemplateDetailsRepository
                    .findByTemplateNameContainingIgnoreCaseAndTemplateType(templateName, templateType, pageable);
        } else if (templateName != null) {
            results = reconTemplateDetailsRepository
                    .findByTemplateNameContainingIgnoreCase(templateName, pageable);
        } else if (templateType != null) {
            results = reconTemplateDetailsRepository.findByTemplateType(templateType, pageable);
        } else {
            results = reconTemplateDetailsRepository.findAll(pageable);
        }

        List<ReconTemplatesDetailsDTO> templateDTOs =
                ReconTemplateDetailsMapper.toDTOList(results.getContent());

        PageMetadata pageMetadata = PageMetadata.builder()
                .currentPage(results.getNumber())
                .pageSize(results.getSize())
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .isFirst(results.isFirst())
                .isLast(results.isLast())
                .hasNext(results.hasNext())
                .hasPrevious(results.hasPrevious())
                .build();

        return ResponseEntity.ok(
                RestWithStatusListPagination.builder()
                        .status("SUCCESS")
                        .statusMsg("Templates fetched successfully")
                        .data(new ArrayList<>(templateDTOs))
                        .pageMetadata(pageMetadata)
                        .build());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // GET TEMPLATE BY ID (unchanged)
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public ResponseEntity<?> getTemplateById(Long templateId) {
        try {
            Optional<ReconTemplateDetails> templateOpt = reconTemplateDetailsRepository.findById(templateId);

            if (!templateOpt.isPresent()) {
                return new ResponseEntity<>(
                        new RestWithStatusList("FAILURE", "Template not found with ID: " + templateId, null),
                        HttpStatus.NOT_FOUND);
            }

            ReconTemplatesDetailsDTO templateDTO = ReconTemplateDetailsMapper.toDTO(templateOpt.get());

            List<Object> responseData = new ArrayList<>();
            responseData.add(templateDTO);

            return new ResponseEntity<>(
                    new RestWithStatusList("SUCCESS", "Template fetched successfully", responseData),
                    HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error fetching template by ID {}: {}", templateId, e.getMessage(), e);
            return new ResponseEntity<>(
                    new RestWithStatusList("ERROR", "Error fetching template: " + e.getMessage(), null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}