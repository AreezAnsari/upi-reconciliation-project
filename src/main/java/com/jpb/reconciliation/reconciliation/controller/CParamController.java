package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.web.bind.annotation.*;

import com.jpb.reconciliation.reconciliation.dto.CParamRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.CParamService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/params")
@RequiredArgsConstructor
public class CParamController {

    private final CParamService service;

    @PostMapping
    public RestWithStatusList create(@RequestBody CParamRequestDTO dto) {
        return service.create(dto);
    }

    @GetMapping("/{name}")
    public RestWithStatusList get(@PathVariable String name) {
        return service.getByName(name);
    }

    @PutMapping("/{id}")
    public RestWithStatusList update(@PathVariable Long id,
                                     @RequestBody CParamRequestDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public RestWithStatusList delete(@PathVariable Long id) {
        return service.delete(id);
    }
}