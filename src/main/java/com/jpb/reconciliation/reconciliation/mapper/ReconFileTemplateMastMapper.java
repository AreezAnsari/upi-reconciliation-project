
package com.jpb.reconciliation.reconciliation.mapper;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.jpb.reconciliation.reconciliation.dto.ReconFileTemplateMastDto;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplateConfigRequest;
import com.jpb.reconciliation.reconciliation.dto.ReconTemplatesDetailsDTO;
import com.jpb.reconciliation.reconciliation.entity.ReconFileTmpltMast;
import com.jpb.reconciliation.reconciliation.entity.ReconTmpltFieldDtls;

/**
 * Static mapper — all four methods used by ReconTemplateDetailsServiceImpl.
 *
 *  mapToReconFileTmpltMast(dto, entity)           ← addTemplate
 *  mapTemplateDtoToFileTmpltMast(request, entity) ← saveTemplateAndFields
 *  toDTOList(entities)                            ← viewTemplate, searchTemplate
 *  toDTO(entity)                                  ← getTemplateById, toDTOList
 *
 * toFieldDTO() entity access aligned to ACTUAL ReconTmpltFieldDtls fields:
 *   getTemplateMast()     → templateId
 *   getFieldTypeMast()    → data type code
 *   getFieldFormatMast()  → format description
 *   getColPosn()          → sequenceOrder
 *   getMaxLength()        → fieldLength
 *   getShortName()        → fieldLabel
 */
public class ReconFileTemplateMastMapper {

    private ReconFileTemplateMastMapper() {}

    // =========================================================================
    // METHOD 1: mapToReconFileTmpltMast
    // Called by: addTemplate(ReconTemplateDetailsDto)
    // =========================================================================

//    public static ReconFileTmpltMast mapToReconFileTmpltMast(ReconTemplateDetailsDto dto,
//                                                               ReconFileTmpltMast entity) {
//        if (dto == null) return null;
//
//        if (dto.getTemplateName()      != null) entity.setTemplateName(dto.getTemplateName());
//        if (dto.getTemplateType()      != null) entity.setTemplateType(dto.getTemplateType());
//        if (dto.getDescription()       != null) entity.setDescription(dto.getDescription());
//        if (dto.getFilePath()          != null) entity.setFilePath(dto.getFilePath());
//        if (dto.getFileEncoding()      != null) entity.setFileEncoding(dto.getFileEncoding());
//        if (dto.getHasHeader()         != null) entity.setHasHeader(dto.getHasHeader());
//        if (dto.getHasTrailer()        != null) entity.setHasTrailer(dto.getHasTrailer());
//        if (dto.getHeaderLineCount()   != null) entity.setHeaderLineCount(dto.getHeaderLineCount());
//        if (dto.getTrailerLineCount()  != null) entity.setTrailerLineCount(dto.getTrailerLineCount());
//        if (dto.getFrequencyType()     != null) entity.setFrequencyType(dto.getFrequencyType());
//        if (dto.getFileNamePattern()   != null) entity.setFileNamePattern(dto.getFileNamePattern());
//        if (dto.getDateFormat()        != null) entity.setDateFormat(dto.getDateFormat());
//        if (dto.getAmountFormat()      != null) entity.setAmountFormat(dto.getAmountFormat());
//        if (dto.getDupCheckFlag()      != null) entity.setDupCheckFlag(dto.getDupCheckFlag());
//        if (dto.getReversalHandling()  != null) entity.setReversalHandling(dto.getReversalHandling());
//        if (dto.getDelimiter()         != null) entity.setDelimiter(dto.getDelimiter());
//        if (dto.getTextQualifier()     != null) entity.setTextQualifier(dto.getTextQualifier());
//        if (dto.getRecordLength()      != null) entity.setRecordLength(dto.getRecordLength());
//        if (dto.getPaddingChar()       != null) entity.setPaddingChar(dto.getPaddingChar());
//        if (dto.getXmlRootTag()        != null) entity.setXmlRootTag(dto.getXmlRootTag());
//        if (dto.getXmlRowTag()         != null) entity.setXmlRowTag(dto.getXmlRowTag());
//        if (dto.getXmlNamespace()      != null) entity.setXmlNamespace(dto.getXmlNamespace());
//        if (dto.getSubTemplateId()     != null) entity.setSubTemplateId(dto.getSubTemplateId());
//        if (dto.getTypeId()            != null) entity.setTypeId(dto.getTypeId());
//        if (dto.getColumnCount()       != null) entity.setColumnCount(dto.getColumnCount());
//        if (dto.getReversalIndicator() != null) entity.setReversalIndicator(dto.getReversalIndicator());
//        if (dto.getDataReferenceFlag() != null) entity.setDataReferenceFlag(dto.getDataReferenceFlag());
//        if (dto.getOnlRefundFlag()     != null) entity.setOnlRefundFlag(dto.getOnlRefundFlag());
//        if (dto.getIssacqFlag()        != null) entity.setIssacqFlag(dto.getIssacqFlag());
//        if (dto.getDataTableInd()      != null) entity.setDataTableInd(dto.getDataTableInd());
//        if (dto.getMasterFlag()        != null) entity.setMasterFlag(dto.getMasterFlag());
//        if (dto.getMasterTemplateId()  != null) entity.setMasterTemplateId(dto.getMasterTemplateId());
//        if (dto.getSettlementFlag()    != null) entity.setSettlementFlag(dto.getSettlementFlag());
//        if (dto.getProductType()       != null) entity.setProductType(dto.getProductType());
//        if (dto.getTenantId()          != null) entity.setTenantId(dto.getTenantId());
//
//        // Safe defaults
//        if (entity.getStatus()           == null) entity.setStatus("DRAFT");
//        if (entity.getVersion()          == null) entity.setVersion(1);
//        if (entity.getIsDeleted()        == null) entity.setIsDeleted("N");
//        if (entity.getFileEncoding()     == null) entity.setFileEncoding("UTF-8");
//        if (entity.getHasHeader()        == null) entity.setHasHeader("Y");
//        if (entity.getHasTrailer()       == null) entity.setHasTrailer("N");
//        if (entity.getHeaderLineCount()  == null) entity.setHeaderLineCount(1);
//        if (entity.getTrailerLineCount() == null) entity.setTrailerLineCount(0);
//        if (entity.getDupCheckFlag()     == null) entity.setDupCheckFlag("N");
//
//        entity.setCreatedAt(LocalDateTime.now());
//        return entity;
//    }

    // =========================================================================
    // METHOD 2: mapTemplateDtoToFileTmpltMast
    // Called by: saveTemplateAndFields(TemplateFieldDto) — configure/create flow
    // =========================================================================

    public static ReconFileTmpltMast mapTemplateDtoToFileTmpltMast(ReconTemplateConfigRequest request,
                                                                    ReconFileTmpltMast entity) {
        if (request == null) return null;

        if (request.getTemplateName()      != null) entity.setTemplateName(request.getTemplateName());
        if (request.getTemplateType()      != null) entity.setTemplateType(request.getTemplateType());
        if (request.getColumnCount()       != null) entity.setColumnCount(Long.valueOf(request.getColumnCount()));
        if (request.getReversalIndicator() != null) entity.setReversalIndicator(request.getReversalIndicator());
        if (request.getDataReference()     != null) entity.setDataReferenceFlag(request.getDataReference());
        if (request.getOnlineRefund()      != null) entity.setOnlRefundFlag(request.getOnlineRefund());
        if (request.getDelimiter()         != null) entity.setDelimiter(request.getDelimiter());
        if (request.getTextQualifier()     != null) entity.setTextQualifier(request.getTextQualifier());
        if (request.getFileEncoding()      != null) entity.setFileEncoding(request.getFileEncoding());
        if (request.getDateFormat()        != null) entity.setDateFormat(request.getDateFormat());
        if (request.getAmountFormat()      != null) entity.setAmountFormat(request.getAmountFormat());
        if (request.getDupCheckFlag()      != null) entity.setDupCheckFlag(request.getDupCheckFlag());
        if (request.getReversalHandling()  != null) entity.setReversalHandling(request.getReversalHandling());
        if (request.getRecordLength()      != null) entity.setRecordLength(request.getRecordLength());
        if (request.getPaddingChar()       != null) entity.setPaddingChar(request.getPaddingChar());
        if (request.getXmlRootTag()        != null) entity.setXmlRootTag(request.getXmlRootTag());
        if (request.getXmlRowTag()         != null) entity.setXmlRowTag(request.getXmlRowTag());
        if (request.getXmlNamespace()      != null) entity.setXmlNamespace(request.getXmlNamespace());
        if (request.getDescription()      != null) entity.setDescription(request.getDescription());
        if (request.getFilePath()      != null) entity.setFilePath(request.getFilePath());

        if (request.getTenantId()      != null) entity.setTenantId(request.getTenantId());
        if (request.getCreatedBy()      != null) entity.setCreatedBy(request.getCreatedBy());
        
        // Safe defaults
        if (entity.getStatus()           == null) entity.setStatus("ACTIVE");
        if (entity.getVersion()          == null) entity.setVersion(1);
        if (entity.getIsDeleted()        == null) entity.setIsDeleted("N");
        if (entity.getFileEncoding()     == null) entity.setFileEncoding("UTF-8");
        if (entity.getHasHeader()        == null) entity.setHasHeader("Y");
        if (entity.getHasTrailer()       == null) entity.setHasTrailer("N");
        if (entity.getHeaderLineCount()  == null) entity.setHeaderLineCount(1);
        if (entity.getTrailerLineCount() == null) entity.setTrailerLineCount(0);
        if (entity.getDupCheckFlag()     == null) entity.setDupCheckFlag("N");

        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    // =========================================================================
    // METHOD 3: toDTOList
    // Called by: viewTemplate, searchTemplate
    // =========================================================================

    public static List<ReconFileTemplateMastDto> toDTOList(List<ReconFileTmpltMast> entities) {
        if (entities == null || entities.isEmpty()) return Collections.emptyList();
        List<ReconFileTemplateMastDto> result = new ArrayList<>();
        for (ReconFileTmpltMast entity : entities) {
            result.add(toDTO(entity));
        }
        return result;
    }

    // =========================================================================
    // METHOD 4: toDTO
    // Called by: getTemplateById (single), and internally by toDTOList
    // =========================================================================

    public static ReconFileTemplateMastDto toDTO(ReconFileTmpltMast entity) {
        if (entity == null) return null;

        ReconFileTemplateMastDto dto = ReconFileTemplateMastDto.builder()
                .templateId(entity.getTemplateId())
                .templateCode(entity.getTemplateCode())
                .templateName(entity.getTemplateName())
                .templateType(entity.getTemplateType())
                .description(entity.getDescription())
                .stageTabName(entity.getStageTabName())
                .filePath(entity.getFilePath())
                .fileEncoding(entity.getFileEncoding())
                .hasHeader(entity.getHasHeader())
                .hasTrailer(entity.getHasTrailer())
                .headerLineCount(entity.getHeaderLineCount())
                .trailerLineCount(entity.getTrailerLineCount())
                .frequencyType(entity.getFrequencyType())
                .fileNamePattern(entity.getFileNamePattern())
                .dateFormat(entity.getDateFormat())
                .amountFormat(entity.getAmountFormat())
                .dupCheckFlag(entity.getDupCheckFlag())
                .reversalHandling(entity.getReversalHandling())
                .delimiter(entity.getDelimiter())
                .textQualifier(entity.getTextQualifier())
                .recordLength(entity.getRecordLength())
                .paddingChar(entity.getPaddingChar())
                .xmlRootTag(entity.getXmlRootTag())
                .xmlRowTag(entity.getXmlRowTag())
                .xmlNamespace(entity.getXmlNamespace())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .tenantId(entity.getTenantId())
                .subTemplateId(entity.getSubTemplateId())
                .typeId(entity.getTypeId())
                .columnCount(entity.getColumnCount())
                .reversalIndicator(entity.getReversalIndicator())
                .dataReferenceFlag(entity.getDataReferenceFlag())
                .onlRefundFlag(entity.getOnlRefundFlag())
                .settlementFlag(entity.getSettlementFlag())
                .productType(entity.getProductType())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Source system code from FK relationship
        if (entity.getSourceSystem() != null) {
            dto.setSourceSystem(entity.getSourceSystem().getSourceCode());
        }

        // Field definitions — map only active (is_deleted = N) fields
        Set<ReconTmpltFieldDtls> fieldSet = entity.getFieldDetails();
        if (fieldSet != null && !fieldSet.isEmpty()) {
            List<ReconFileTemplateMastDto.FieldDefinitionDTO> fieldDtos = new ArrayList<>();
            fieldSet.stream()
                    .filter(f -> !"Y".equalsIgnoreCase(f.getIsDeleted()))
                    .sorted((a, b) -> {
                        if (a.getColPosition() == null) return 1;
                        if (b.getColPosition() == null) return -1;
                        return Long.compare(a.getColPosition(), b.getColPosition());
                    })
                    .forEach(f -> fieldDtos.add(toFieldDTO(f)));
            dto.setFieldDetails(fieldDtos);
        } else {
            dto.setFieldDetails(Collections.emptyList());
        }

        return dto;
    }

    // =========================================================================
    // PRIVATE — single field entity → FieldDefinitionDTO
    // Uses ACTUAL entity field names from ReconTmpltFieldDtls
    // =========================================================================

    private static ReconFileTemplateMastDto.FieldDefinitionDTO toFieldDTO(ReconTmpltFieldDtls f) {
        return ReconFileTemplateMastDto.FieldDefinitionDTO.builder()
                .fieldId(f.getFieldId())
                // templateId via relationship — getTemplateMast() not getTemplate()
                .templateId(f.getTemplate() != null
                        ? f.getTemplate().getTemplateId() : null)
                .fieldName(f.getFieldName())
                // fieldLabel — stored in shortName column
                .fieldLabel(f.getShortName())
                // data type — via getFieldTypeMast() not getFieldType()
                .dataType(f.getFieldType()   != null
                        ? f.getFieldType().getFieldTypeCode()       : null)
                // format — via getFieldFormatMast() not getFieldFormat()
                .format(f.getFieldFormat()   != null
                        ? f.getFieldFormat().getFieldFormatDesc()   : null)
                // fieldLength — stored in maxLength column
                .fieldLength(f.getFieldLength())
                .fieldScale(null)         // no scale column in entity — not mapped
                // sequenceOrder — stored in colPosn column
                .sequenceOrder(f.getColPosition())
                // positionFrom/To — stored as VARCHAR, parsed to Integer
                .positionFrom(parseIntSafe(f.getFromPosition()))
                .positionTo(parseIntSafe(f.getToPosition()))
                // isPrimaryKey — derived from keyIdentifier (Long 1/0)
                .isPrimaryKey(f.getKeyIdentifier() != null && f.getKeyIdentifier() > 0 ? "Y" : "N")
                // isReconKey — new canonical column
                .isReconKey(f.getIsReconKey())
                // isMandatory — mandatoryFlag column
                .isMandatory(f.getIsMandatory())
                // trimFlag — new canonical column
                .trimFlag(f.getTrimFlag())
                .defaultValue(f.getDefaultValue())
                .validationRegex(f.getValidationRegex())
                .xmlXpath(f.getXmlXpath())
                .excelColIndex(f.getExcelColIndex())
                .build();
    }

    private static Integer parseIntSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
