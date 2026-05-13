package com.jpb.reconciliation.reconciliation.service;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.CParamRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.CParamResponseDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.CParam;
import com.jpb.reconciliation.reconciliation.mapper.CParamMapper;
import com.jpb.reconciliation.reconciliation.repository.CParamRepository;
import com.jpb.reconciliation.reconciliation.service.CParamService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CParamServiceImpl implements CParamService {

    private final CParamRepository repository;

    @Override
    public RestWithStatusList create(CParamRequestDTO dto) {

        if (repository.existsByParamName(dto.getParamName())) {
            throw new RuntimeException("PARAM_NAME must be unique");
        }

        CParam saved = repository.save(CParamMapper.toEntity(dto));

        return response("SUCCESS", "Parameter created",
                Arrays.asList(CParamMapper.toDTO(saved)));
    }

    @Override
    public RestWithStatusList getByName(String name) {

        CParam param = repository.findByParamNameAndActiveYn(name, "Y")
                .orElseThrow(() -> new RuntimeException("Not found"));

        return response("SUCCESS", "Fetched",
                Arrays.asList(CParamMapper.toDTO(param)));
    }

    @Override
    public RestWithStatusList update(Long id, CParamRequestDTO dto) {

        CParam param = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        param.setParamValue(dto.getParamValue()); // only value update
        repository.save(param);

        return response("SUCCESS", "Updated",
                Arrays.asList(CParamMapper.toDTO(param)));
    }

    @Override
    public RestWithStatusList delete(Long id) {

        CParam param = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        param.setActiveYn("N"); // soft delete
        repository.save(param);

        return response("SUCCESS", "Deleted",
                Arrays.asList(CParamMapper.toDTO(param)));
    }

    private RestWithStatusList response(String status, String msg, java.util.List<CParamResponseDTO> data) {

        return RestWithStatusList.builder()
                .status(status)
                .statusMsg(msg)
                .data(new ArrayList<Object>(data))
                .build();
    }
}