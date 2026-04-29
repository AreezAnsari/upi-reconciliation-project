package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.LookupRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.LookupResponseDTO;
import com.jpb.reconciliation.reconciliation.entity.MLookup;

public class MLookupMapper {

    public static MLookup toEntity(LookupRequestDTO dto, MLookup parent) {

        MLookup e = new MLookup();

        e.setLookupName(dto.getLookupName());
        e.setLookupValue(dto.getLookupValue());
        e.setLookupCode(dto.getLookupCode());
        e.setSortOrder(dto.getSortOrder());

        e.setLookupDesc(dto.getLookupDesc());
        e.setShortName(dto.getShortName());
        e.setLongName(dto.getLongName());

        e.setParentLookup(parent);

        return e;
    }

    public static LookupResponseDTO toDTO(MLookup e) {

        return LookupResponseDTO.builder()
                .id(e.getId())
                .lookupName(e.getLookupName())
                .lookupValue(e.getLookupValue())
                .activeYn(e.getActiveYn())
                .parentId(e.getParentLookup() != null ? e.getParentLookup().getId() : null)
                .build();
    }
}