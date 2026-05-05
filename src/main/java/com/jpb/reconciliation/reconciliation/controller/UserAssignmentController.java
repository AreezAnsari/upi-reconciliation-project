package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.dto.UserAssignmentDto;
import com.jpb.reconciliation.reconciliation.service.UserAssignmentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-assignments")
public class UserAssignmentController {

    @Autowired
    private UserAssignmentService service;

    @PostMapping
    public UserAssignmentDto create(@RequestBody UserAssignmentDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public UserAssignmentDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<UserAssignmentDto> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public UserAssignmentDto update(@PathVariable Long id,
                                    @RequestBody UserAssignmentDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}