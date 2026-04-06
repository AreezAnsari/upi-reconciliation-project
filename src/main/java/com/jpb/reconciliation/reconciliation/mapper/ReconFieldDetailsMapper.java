package com.jpb.reconciliation.reconciliation.mapper;


import java.time.LocalDateTime;
import java.util.Date;

import com.jpb.reconciliation.reconciliation.dto.FieldConfigurationDto;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldFormatMast;
import com.jpb.reconciliation.reconciliation.entity.ReconFieldTypeMast;
import com.jpb.reconciliation.reconciliation.entity.ReconFileTmpltMast;
import com.jpb.reconciliation.reconciliation.entity.ReconTmpltFieldDtls;

/**
 * Static mapper for field definition rows.
 *
 * All setters are aligned to ACTUAL entity fields in ReconTmpltFieldDtls:
 *   templateMast, fieldTypeMast, fieldFormatMast  — relationship fields
 *   colPosn        — sequence_order (NOT NULL Long)
 *   maxLength      — field_length
 *   keyIdentifier  — is_primary_key stored as Long 1/0
 *   mandatoryFlag  — is_mandatory Y/N
 *   shortName      — field label / display name
 *   fromPosition, toPosition — VARCHAR position columns
 *   fromPosn, toPosn         — duplicate VARCHAR position legacy columns
 *   matchingFieldFlag        — mirrors isReconKey
 *   alterFlag                — mirrors trimFlag
 *   colPosition              — VARCHAR display column
 *
 * New canonical columns (proper typed columns added in migration):
 *   fieldName, isReconKey, trimFlag, defaultValue,
 *   validationRegex, xmlXpath, excelColIndex, isDeleted
 */
public class ReconFieldDetailsMapper {

    private ReconFieldDetailsMapper() {}

    public static ReconTmpltFieldDtls mapFieldDtoToEntity(FieldConfigurationDto dto,
                                                           ReconFileTmpltMast template,
                                                           ReconFieldTypeMast  fieldType,
                                                           ReconFieldFormatMast fieldFormat) {
        if (dto == null) return null;

        ReconTmpltFieldDtls field = new ReconTmpltFieldDtls();

        // Relationships
        field.setTemplate(template);
        field.setFieldType(fieldType);
        field.setFieldFormat(fieldFormat);

        // New canonical columns
        field.setFieldName(dto.getFieldName());
        field.setIsReconKey(normaliseFlag(dto.getIsReconKey()));
        field.setTrimFlag(normaliseFlag(dto.getTrimFlag()));
        field.setDefaultValue(dto.getDefaultValue());
        field.setValidationRegex(dto.getValidationRegex());
        field.setXmlXpath(dto.getXmlXpath());
        field.setExcelColIndex(dto.getExcelColIndex());
        field.setIsDeleted("N");

        // Legacy: shortName = field label
        field.setShortName(dto.getFieldName());

        // Legacy: colPosn is NOT NULL Long — must always be set
        field.setColPosition(dto.getFieldSequence() != null
                ? Long.valueOf(dto.getFieldSequence()) : 0L);
        
             

        // Legacy: max length
        field.setFieldLength(dto.getFieldLength() != null
                ? Long.valueOf(dto.getFieldLength()) : null);

        // Legacy: primary key stored as Long 1 / 0
        field.setKeyIdentifier(
                "Y".equalsIgnoreCase(dto.getIsPrimaryKey()) ? 1L : 0L);

        // Legacy: mandatory flag Y/N
        field.setIsMandatory(normaliseFlag(dto.getIsMandatory()));

        // Legacy: position columns stored as VARCHAR
        field.setFromPosition(dto.getPositionFrom() != null
                ? String.valueOf(dto.getPositionFrom()) : null);
        field.setToPosition(dto.getPositionTo() != null
                ? String.valueOf(dto.getPositionTo()) : null);

        // Legacy: duplicate posn columns
        field.setFromPosition(field.getFromPosition());
        field.setToPosition(field.getToPosition());

        // Legacy: matching field = recon key, alter flag = trim flag
        field.setMatchingFieldFlag(normaliseFlag(dto.getIsReconKey()));
        field.setAlterFlag(normaliseFlag(dto.getTrimFlag()));

        // Audit from parent template
        field.setCreatedAt(LocalDateTime.now());
        if (template != null) {
            field.setCreatedBy(template.getCreatedBy());
//            field.setIn(template.getInsertCode());
        }
        
        
        field.setColPosn(dto.getFieldSequence() != null
                ? Long.valueOf(dto.getFieldSequence()) : 0L);
        field.setSequenceOrder(dto.getFieldSequence() != null
                ? Long.valueOf(dto.getFieldSequence()) : 0L);
        return field;
    }

    private static String normaliseFlag(String value) {
        if (value == null) return "N";
        String v = value.trim().toUpperCase();
        return (v.equals("Y") || v.equals("1") || v.equals("YES") || v.equals("TRUE"))
                ? "Y" : "N";
    }
}
