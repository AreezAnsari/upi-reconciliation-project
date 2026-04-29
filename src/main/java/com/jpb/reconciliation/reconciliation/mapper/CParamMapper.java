package com.jpb.reconciliation.reconciliation.mapper;

import com.jpb.reconciliation.reconciliation.dto.CParamRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.CParamResponseDTO;
import com.jpb.reconciliation.reconciliation.entity.CParam;

public class CParamMapper {

    public static CParam toEntity(CParamRequestDTO dto) {
        CParam p = new CParam();
        p.setParamName(dto.getParamName());
        p.setParamValue(dto.getParamValue());
        return p;
    }

    public static CParamResponseDTO toDTO(CParam p) {
        return CParamResponseDTO.builder()
                .id(p.getId())
                .paramName(p.getParamName())
                .paramValue(p.getParamValue())
                .activeYn(p.getActiveYn())
                .build();
    }
}