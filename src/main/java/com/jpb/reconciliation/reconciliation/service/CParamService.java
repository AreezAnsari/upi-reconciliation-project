package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.CParamRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface CParamService {

    RestWithStatusList create(CParamRequestDTO dto);

    RestWithStatusList getByName(String name);

    RestWithStatusList update(Long id, CParamRequestDTO dto);

    RestWithStatusList delete(Long id);
}