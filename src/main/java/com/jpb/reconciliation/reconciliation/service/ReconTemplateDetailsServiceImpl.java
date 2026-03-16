package com.jpb.reconciliation.reconciliation.service;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	private SimpleJdbcCall simpleJdbcCall;
	
    @Autowired
    private ReconFieldDtlMastService reconFieldDtlMastService;
	
	private final JdbcTemplate jdbcTemplate;

	public ReconTemplateDetailsServiceImpl(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	

	@Override
	public ResponseEntity<?> addTemplate(ReconTemplateDetailsDto reconTemplateDetailsDto) {
		ReconTemplateDetails templateDetails = ReconTemplateDetailsMapper
				.mapToReconTemplateDetails(reconTemplateDetailsDto, new ReconTemplateDetails());

		if (templateDetails != null) {
			reconTemplateDetailsRepository.save(templateDetails);
			return new ResponseEntity<>(new ResponseDto(MenuConstants.STATUS_201, "Template Successfully Configurd."),
					HttpStatus.CREATED);
		}

		return new ResponseEntity<>(new ResponseDto(MenuConstants.STATUS_417, "Template not Configured."),
				HttpStatus.BAD_REQUEST);
	}

	@Override
	@Transactional
	public ResponseEntity<RestWithStatusList> configureTemplateAndFieldData(TemplateFieldDto request) {
		RestWithStatusList restWithStatusList = null;
		ReconTemplateDetails templateExists = reconTemplateDetailsRepository
				.findByTemplateName(request.getTemplateName());

		if (templateExists != null) {
			restWithStatusList = new RestWithStatusList("FAILURE", "Template already configured.", null);
			return new ResponseEntity<>(restWithStatusList, HttpStatus.BAD_REQUEST);
		}

		// Save Template
		ReconTemplateDetails template = ReconTemplateDetailsMapper.mapTemplateDtoToTemplate(request,
				new ReconTemplateDetails());
		String stageTableName = generateStagetableName(template);
		template.setStageTabName(stageTableName);
		reconTemplateDetailsRepository.save(template);
		logger.info("Template Details Id ::::::::::::::::" + template.getReconTemplateId());
		// Save Fields
		List<ReconFieldDetailsMaster> fieldEntities = new ArrayList<>();

		for (FieldConfigurationDto fieldDto : request.getFieldDetails()) {

			ReconFieldTypeMaster fieldType = reconFieldTypeRepository.findByFieldTypeDes(fieldDto.getFieldtype())
					.orElseThrow(() -> new IllegalArgumentException("Invalid Field Type: " + fieldDto.getFieldtype()));

			ReconFieldFormatMaster fieldFormat = reconFieldFormatRepository
					.findByReconFieldFormatDesc(fieldDto.getFieldFormat()).orElseThrow(
							() -> new IllegalArgumentException("Invalid Field Format: " + fieldDto.getFieldFormat()));

			ReconFieldDetailsMaster fieldEntity = ReconFieldDetailsMapper.mapFieldDtoToEntity(fieldDto, template,
					fieldType, fieldFormat);

			fieldEntities.add(fieldEntity);
		}
		logger.info("Template Detials :::::::" + fieldEntities);
		reconFieldDetailsRepository.saveAll(fieldEntities);

		Boolean configureTemplateStatus = configureTemplate(template);
		logger.info("Template Stage Table Configuration Status :::::::" + fieldEntities);

		return ResponseEntity.ok(new RestWithStatusList("SUCCESS", "Template configured successfully", null));
	}

	private String generateStagetableName(ReconTemplateDetails template) {
		String stageTblName = "REC_" + template.getTemplateName() + "STAGE_T";
		return stageTblName;
	}

	@Transactional
	private Boolean configureTemplate(ReconTemplateDetails template) {
		String segretionMsg = null;
		Long templateId = template.getReconTemplateId();

		simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_STAGE_TAB_CREATION").declareParameters(
				new SqlParameter("Prm_tmplt_Id", Types.NUMERIC), new SqlOutParameter("Prm_Error", Types.VARCHAR));

		Map<String, Object> inputParameter = new HashMap<>();
		inputParameter.put("Prm_tmplt_Id", templateId);
		logger.info("INPUT PARAMETER LOG INFO :::::::::::::::" + inputParameter);

		Map<String, Object> result = null;
		try {
			result = simpleJdbcCall.execute(inputParameter);
			logger.info("Result from SP_STAGE_TAB_CREATION::::::::::::::: {}", result);
			segretionMsg = (String) result.get("Prm_Error");
			logger.info("Result from segretionMsg::::::::::::::: {}", segretionMsg);
		} catch (Exception e) {
			logger.warn("Transient error during SP_PROCESS_DATA execution for Template ID {}. Retrying. Error: {}",
					templateId, e.getMessage());
			throw e;
		}

		if (segretionMsg != null && segretionMsg.equalsIgnoreCase("OK")) {
			logger.info("Stage table created successfully by SP_STAGE_TAB_CREATION for Template ID: {}", templateId,
					template.getStageTabName());
			return true;
		} else {
			logger.info("Stage table not created by SP_STAGE_TAB_CREATION for Template ID: {}", templateId,
					template.getStageTabName());
			return false;
		}
	}

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

	        // STEP 1: Fetch paginated templates (without fetch joins)
	        Page<ReconTemplateDetails> templatesPage =
	                reconTemplateDetailsRepository.findTemplates(pageable);

	        if (templatesPage.isEmpty()) {

	            logger.warn("No templates found for page: {}", page);

	            return ResponseEntity.ok(
	                    RestWithStatusListPagination.builder()
	                            .status("SUCCESS")
	                            .statusMsg("No templates available")
	                            .data(Collections.emptyList())
	                            .pageMetadata(PageMetadata.builder()
	                                    .currentPage(page)
	                                    .pageSize(size)
	                                    .totalElements(0L)
	                                    .totalPages(0)
	                                    .isFirst(true)
	                                    .isLast(true)
	                                    .hasNext(false)
	                                    .hasPrevious(false)
	                                    .build())
	                            .build());
	        }

	        // STEP 2: Fetch child relationships
	        List<ReconTemplateDetails> templatesWithDetails =
	                reconTemplateDetailsRepository.fetchTemplateDetails(templatesPage.getContent());

	        // Convert to DTOs
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
	                templateDTOs.size(),
	                templatesPage.getTotalElements(),
	                page);

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

//	@Override
//	@Transactional
//	public ResponseEntity<RestWithStatusList> updateTemplate(Long templateId, TemplateFieldDto templateFieldRequest) {
//		
//		try {
//		Optional<ReconTemplateDetails> templateOpt = reconTemplateDetailsRepository.findById(templateId);
//		if (!templateOpt.isPresent()) {
//			return new ResponseEntity<>(
//					new RestWithStatusList("FAILURE", "Template not found with ID: " + templateId, null),
//					HttpStatus.NOT_FOUND);
//		}
//
//		ReconTemplateDetails existingTemplate = templateOpt.get();
//
//		if (templateFieldRequest.getTemplateName() != null)
//			existingTemplate.setTemplateName(templateFieldRequest.getTemplateName());
//		if (templateFieldRequest.getTemplateType() != null)
//			existingTemplate.setTemplateType(templateFieldRequest.getTemplateType());
//		if (templateFieldRequest.getColumnCount() != null)
//			existingTemplate.setColumnCount(templateFieldRequest.getColumnCount());
//		if (templateFieldRequest.getReversalIndicator() != null)
//			existingTemplate.setReversalIndicator(templateFieldRequest.getReversalIndicator());
//		if (templateFieldRequest.getDataReference() != null)
//			existingTemplate.setDataReferenceFlag(templateFieldRequest.getDataReference());
//		if (templateFieldRequest.getOnlineRefund() != null)
//			existingTemplate.setOnlRefundFlag(templateFieldRequest.getOnlineRefund());
//
//		reconTemplateDetailsRepository.save(existingTemplate);
//
//		reconFieldDetailsRepository.deleteByTemplateId(existingTemplate.getReconTemplateId());
//		
//		entityManager.flush();
////		entityManager.clear(); 
//		
//		reconFieldDetailsRepository.flush();
//		
//
//
//		List<ReconFieldDetailsMaster> newFields = new ArrayList<>();
//		for (FieldConfigurationDto fieldDto : templateFieldRequest.getFieldDetails()) {
//
//			ReconFieldTypeMaster fieldType = reconFieldTypeRepository.findByFieldTypeDes(fieldDto.getFieldtype())
//					.orElseThrow(() -> new IllegalArgumentException("Invalid Field Type: " + fieldDto.getFieldtype()));
//
//			ReconFieldFormatMaster fieldFormat = reconFieldFormatRepository
//					.findByReconFieldFormatDesc(fieldDto.getFieldFormat()).orElseThrow(
//							() -> new IllegalArgumentException("Invalid Field Format: " + fieldDto.getFieldFormat()));
//
//			ReconFieldDetailsMaster fieldEntity = ReconFieldDetailsMapper.mapFieldDtoToEntity(fieldDto,
//					existingTemplate, fieldType, fieldFormat);
//
//			newFields.add(fieldEntity);
//			logger.info("Filed Entity "+ 		fieldEntity	);
//		}
//
//		reconFieldDetailsRepository.saveAll(newFields);
//
////		return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Template updated successfully", null),
////				HttpStatus.OK);
//		
//		}catch (Exception e) {
//			// TODO: handle exception
//			logger.error("Ëxception while updating template "+ e.getMessage(),e);
//			return new ResponseEntity<>(
//					new RestWithStatusList("ERROR", "Exceptions " + templateId, null),
//					HttpStatus.NOT_FOUND);
//		}
//		return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Template updated successfully", null),
//				HttpStatus.OK);
//	}
	
	
	
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

	            ReconFieldDetailsMaster fieldEntity = ReconFieldDetailsMapper
	                    .mapFieldDtoToEntity(fieldDto, existingTemplate, fieldType, fieldFormat);

	            newFields.add(fieldEntity);
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

		return new ResponseEntity<>(new RestWithStatusList("SUCCESS", "Template deleted successfully", null),
				HttpStatus.OK);
	}

	@Override
	public ResponseEntity<RestWithStatusListPagination> searchTemplate(String templateName, String templateType,
			int page, int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<ReconTemplateDetails> results;

		if (templateName != null && templateType != null) {
			results = reconTemplateDetailsRepository.findByTemplateNameContainingIgnoreCaseAndTemplateType(templateName,
					templateType, pageable);
		} else if (templateName != null) {
			results = reconTemplateDetailsRepository.findByTemplateNameContainingIgnoreCase(templateName, pageable);
		} else if (templateType != null) {
			results = reconTemplateDetailsRepository.findByTemplateType(templateType, pageable);
		} else {
			results = reconTemplateDetailsRepository.findAll(pageable);
		}

		List<ReconTemplatesDetailsDTO> templateDTOs = ReconTemplateDetailsMapper.toDTOList(results.getContent());

		PageMetadata pageMetadata = PageMetadata.builder().currentPage(results.getNumber()).pageSize(results.getSize())
				.totalElements(results.getTotalElements()).totalPages(results.getTotalPages())
				.isFirst(results.isFirst()).isLast(results.isLast()).hasNext(results.hasNext())
				.hasPrevious(results.hasPrevious()).build();

		return ResponseEntity
				.ok(RestWithStatusListPagination.builder().status("SUCCESS").statusMsg("Templates fetched successfully")
						.data(new ArrayList<>(templateDTOs)).pageMetadata(pageMetadata).build());
	}

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
