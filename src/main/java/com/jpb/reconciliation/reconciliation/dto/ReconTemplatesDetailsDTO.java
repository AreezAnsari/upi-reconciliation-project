package com.jpb.reconciliation.reconciliation.dto;


import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned by viewTemplate, searchTemplate, and getTemplateById.
 * Contains the template header plus all its active field definitions.
 * Serialised to Map<String,Object> by ResponseBuilder.toMap() before being placed
 * into RestWithMapStatusList.data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReconTemplatesDetailsDTO {

    // ── Template header ───────────────────────────────────────────────────────

    private Long   templateId;
    private String templateCode;
    private String templateName;
    private String templateType;
    private String description;
    private String sourceSystem;
    private String stageTabName;
    private String filePath;
    private String fileEncoding;
    private String hasHeader;
    private String hasTrailer;
    private Integer headerLineCount;
    private Integer trailerLineCount;
    private String frequencyType;
    private String fileNamePattern;
    private String dateFormat;
    private String amountFormat;
    private String dupCheckFlag;
    private String reversalHandling;

    // CSV
    private String delimiter;
    private String textQualifier;

    // Fixed Width
    private Integer recordLength;
    private String  paddingChar;

    // XML
    private String xmlRootTag;
    private String xmlRowTag;
    private String xmlNamespace;

    // Lifecycle
    private String  status;
    private Integer version;
    private Long    tenantId;

    // Legacy fields
    private Long   subTemplateId;
    private Long   typeId;
    private Long   columnCount;
    private String reversalIndicator;
    private String dataReferenceFlag;
    private String onlRefundFlag;
    private String settlementFlag;
    private String productType;

    // Audit
    private String        createdBy;
    private LocalDateTime createdAt;
    private String        updatedBy;
    private LocalDateTime updatedAt;

    // ── Field definitions (child rows) ───────────────────────────────────────

    private List<FieldDefinitionDTO> fieldDetails;

    // ── Inner DTO for a single field definition ───────────────────────────────

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldDefinitionDTO {

        private Long    fieldId;
        private Long    templateId;
        private String  fieldName;
        private String  fieldLabel;
        private String  dataType;        // field_type_code from recon_field_type_mast
        private String  format;          // field_format_desc from recon_field_format_mast
        private Long    fieldLength;
        private Integer fieldScale;
        private Long    sequenceOrder;
        private Integer positionFrom;
        private Integer positionTo;
        private String  isPrimaryKey;    // Y / N
        private String  isReconKey;      // Y / N
        private String  isMandatory;     // Y / N
        private String  trimFlag;        // Y / N
        private String  defaultValue;
        private String  validationRegex;
        private String  xmlXpath;
        private Integer excelColIndex;
    }
}
