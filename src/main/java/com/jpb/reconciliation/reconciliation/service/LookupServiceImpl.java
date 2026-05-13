package com.jpb.reconciliation.reconciliation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.jpb.reconciliation.reconciliation.dto.LookupRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.LookupResponseDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.entity.MLookup;
import com.jpb.reconciliation.reconciliation.mapper.MLookupMapper;
import com.jpb.reconciliation.reconciliation.repository.MLookupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LookupServiceImpl implements LookupService {

    private final MLookupRepository repository;

    @Override
    public RestWithStatusList create(LookupRequestDTO dto) {

        MLookup parent = null;
        if (dto.getParentLookupId() != null) {
            parent = repository.findById(dto.getParentLookupId())
                    .orElseThrow(() -> new RuntimeException("Parent not found"));
        }

        MLookup entity = MLookupMapper.toEntity(dto, parent);

        // 🔥 REQUIRED FIELDS (to avoid ORA-01400)
        entity.setCreatedBy("ADMIN"); // TODO: replace with logged-in user
        entity.setActiveYn("Y");

        // Default values if not provided
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 1);

        entity.setLookupCode(
                dto.getLookupCode() != null && !dto.getLookupCode().isEmpty()
                        ? dto.getLookupCode()
                        : "AUTO_" + System.currentTimeMillis()
        );

        MLookup saved = repository.save(entity);

        return response("SUCCESS", "Lookup created",
                Arrays.asList(MLookupMapper.toDTO(saved)));
    }

    @Override
    public RestWithStatusList getById(Long id) {

        MLookup lookup = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        return RestWithStatusList.builder()
                .status("SUCCESS")
                .statusMsg("Fetched")
                .data(Arrays.asList(MLookupMapper.toDTO(lookup)))
                .build();
    }

    @Override
    public RestWithStatusList getAllActive() {

        List<LookupResponseDTO> list = repository.findByActiveYn("Y")
                .stream()
                .map(MLookupMapper::toDTO)
                .collect(Collectors.toList());

        return response("SUCCESS", "All active lookups", list);
    }

    @Override
    public RestWithStatusList getByName(String name) {

        List<LookupResponseDTO> list = repository
                .findByLookupNameAndActiveYn(name, "Y")
                .stream()
                .map(MLookupMapper::toDTO)
                .collect(Collectors.toList());

        return response("SUCCESS", "Filtered lookups", list);
    }

    @Override
    public RestWithStatusList delete(Long id) {

        MLookup lookup = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        lookup.setActiveYn("N");
        lookup.setUpdatedBy("ADMIN"); // optional audit

        repository.save(lookup);

        return response("SUCCESS", "Soft deleted",
                Arrays.asList(MLookupMapper.toDTO(lookup)));
    }

    private RestWithStatusList response(String status, String msg, List<LookupResponseDTO> data) {

        return RestWithStatusList.builder()
                .status(status)
                .statusMsg(msg)
                .data(new ArrayList<Object>(data))
                .build();
    }
}