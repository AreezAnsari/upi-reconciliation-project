
package com.jpb.reconciliation.reconciliation.dto;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReconTemplateConfigRequest {

    // ── Template header ───────────────────────────────────────────────────────

    /** Unique template name */
    private String templateName;

    /** CSV | XML | EXCEL | FIXED_WIDTH */
    private String templateType;

    /** Number of data columns in the file */
    private Integer columnCount;

    /** Y / N — whether reversals are present in the file */
    private String reversalIndicator;

    /** Y / N — data reference flag (legacy) */
    private String dataReference;

    /** Y / N — online refund flag (legacy) */
    private String onlineRefund;

    /** CSV delimiter  e.g. "," */
    private String delimiter;

    /** CSV text qualifier  e.g. "\"" */
    private String textQualifier;

    /** File encoding  e.g. "UTF-8" */
    private String fileEncoding;

    /** Date format pattern  e.g. "dd-MM-yyyy" */
    private String dateFormat;

    /** Amount format pattern  e.g. "##.##" */
    private String amountFormat;

    /** Source system code  e.g. "CBS", "NPCI" — maps to recon_source_system_mast */
    private String sourceSystem;

    /** Y / N — duplicate check flag */
    private String dupCheckFlag;

    /** NONE | MARK_REVERSAL | EXCLUDE */
    private String reversalHandling;

    /** Fixed Width: record length */
    private Integer recordLength;

    /** Fixed Width: padding character */
    private String paddingChar;

    /** XML root tag */
    private String xmlRootTag;

    /** XML row tag */
    private String xmlRowTag;

    /** XML namespace */
    private String xmlNamespace;
    
    private Long tenantId;	
    private String createdBy;

    // ── Field definitions ────────────────────────────────────────────────────

    /** List of field configurations — must not be null or empty */
    private List<ReconFieldConfigurationDto> fieldDetails;
    
    private SftpServerRequestDTO sftpServerDetails;
}
