package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.AddUserService;
import com.jpb.reconciliation.reconciliation.service.AdminContextResolver;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class AddUserController {

    private final AddUserService       userService;
    private final AdminContextResolver contextResolver;

    @PostMapping("/create")
    public RestWithStatusList createUser(@Valid @RequestBody AddUserRequest request,
                                         Authentication authentication) {
        if (!contextResolver.isAllowed(authentication)) return forbidden();
        return userService.createUser(request, authentication);
    }

    @GetMapping
    public RestWithStatusList listUsers(Authentication authentication) {
        if (!contextResolver.isAllowed(authentication)) return forbidden();
        return userService.getUsersByCreator(authentication);
    }

    @GetMapping("/search")
    public RestWithStatusList search(@RequestParam("q") String term,
                                      Authentication authentication) {
        if (!contextResolver.isAllowed(authentication)) return forbidden();
        return userService.searchByCreator(authentication, term);
    }

    @GetMapping("/{id}")
    public RestWithStatusList getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public RestWithStatusList updateUser(@PathVariable Long id,
                                          @Valid @RequestBody AddUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public RestWithStatusList deactivateUser(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }

    private RestWithStatusList forbidden() {
        return RestWithStatusList.builder()
                .status("FAILURE")
                .statusMsg("Access denied: only Bank Admin or Branch Admin can perform this action")
                .data(Collections.emptyList())
                .build();
    }
}
