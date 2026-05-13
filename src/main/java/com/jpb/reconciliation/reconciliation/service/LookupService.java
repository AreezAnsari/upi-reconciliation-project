package com.jpb.reconciliation.reconciliation.service;

import com.jpb.reconciliation.reconciliation.dto.LookupRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;

public interface LookupService {

    RestWithStatusList create(LookupRequestDTO dto);

    RestWithStatusList getById(Long id);

    RestWithStatusList getAllActive();

    RestWithStatusList getByName(String name);

    RestWithStatusList delete(Long id);
}