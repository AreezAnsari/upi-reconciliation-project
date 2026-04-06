package com.jpb.reconciliation.reconciliation.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input DTO for the addTemplate endpoint.
 * Maps directly to recon_file_tmplt_mast header fields.
 * Field-level definitions are NOT included here — use TemplateFieldDto for that.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReconTemplateDetailsDto {

    private String templateName;
    private String templateType;       // CSV | XML | EXCEL | FIXED_WIDTH
    private String description;
    private String sourceSystem;       // source_sys_code from recon_source_system_mast
    private String filePath;
    private String fileEncoding;       // default UTF-8
    private String hasHeader;          // Y / N
    private String hasTrailer;         // Y / N
    private Integer headerLineCount;
    private Integer trailerLineCount;
    private String frequencyType;      // DAILY | T_PLUS_1 | WEEKLY | MONTHLY | ON_DEMAND
    private String fileNamePattern;
    private String dateFormat;
    private String amountFormat;
    private String dupCheckFlag;       // Y / N
    private String reversalHandling;   // NONE | MARK_REVERSAL | EXCLUDE

    // CSV specific
    private String delimiter;
    private String textQualifier;

    // Fixed Width specific
    private Integer recordLength;
    private String  paddingChar;

    // XML specific
    private String xmlRootTag;
    private String xmlRowTag;
    private String xmlNamespace;

    // Legacy fields — kept for backward compatibility
    private Long   subTemplateId;
    private Long   typeId;
    private Long   columnCount;
    private String reversalIndicator;
    private String dataReferenceFlag;
    private String onlRefundFlag;
    private String issacqFlag;
    private String dataTableInd;
    private String masterFlag;
    private String masterTemplateId;
    private String settlementFlag;
    private String productType;
    private Long   tenantId;
}
