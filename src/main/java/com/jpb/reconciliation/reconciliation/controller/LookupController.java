package com.jpb.reconciliation.reconciliation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpb.reconciliation.reconciliation.dto.LookupRequestDTO;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.LookupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
public class LookupController {

    private final LookupService service;

    @PostMapping
    public RestWithStatusList create(@RequestBody LookupRequestDTO dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public RestWithStatusList getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public RestWithStatusList getAll() {
        return service.getAllActive();
    }

    @GetMapping("/lookup-name/{name}")
    public RestWithStatusList getByName(@PathVariable String name) {
        return service.getByName(name);
    }

    @DeleteMapping("/{id}")
    public RestWithStatusList delete(@PathVariable Long id) {
        return service.delete(id);
    }
}
