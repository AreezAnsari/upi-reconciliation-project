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

    @GetMapping("/get-by-bank/{bankCode}")
    public RestWithStatusList getUsersByBank(@PathVariable String bankCode) {
        return userService.getUsersByBankCode(bankCode);
    }

    @GetMapping("/get-by-branch/{branchCode}")
    public RestWithStatusList getUsersByBranch(@PathVariable String branchCode) {
        return userService.getUsersByBranchCode(branchCode);
    }

    @DeleteMapping("/{id}")
    public RestWithStatusList deactivateUser(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }

    // POST /api/v1/user/{id}/schedule-inactivate
    @PostMapping("/{id}/schedule-inactivate")
    public RestWithStatusList scheduleInactivate(@PathVariable Long id,
                                                  Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return userService.scheduleInactivateUser(id, by);
    }

    // POST /api/v1/user/{id}/undo-inactivate
    @PostMapping("/{id}/undo-inactivate")
    public RestWithStatusList undoInactivate(@PathVariable Long id,
                                              Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return userService.undoInactivateUser(id, by);
    }

    // POST /api/v1/user/{id}/schedule-reactivate
    @PostMapping("/{id}/schedule-reactivate")
    public RestWithStatusList scheduleReactivate(@PathVariable Long id,
                                                  Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return userService.scheduleReactivateUser(id, by);
    }

    // POST /api/v1/user/{id}/undo-reactivate
    @PostMapping("/{id}/undo-reactivate")
    public RestWithStatusList undoReactivate(@PathVariable Long id,
                                              Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return userService.undoReactivateUser(id, by);
    }

    // POST /api/v1/user/{id}/schedule-block
    @PostMapping("/{id}/schedule-block")
    public RestWithStatusList scheduleBlock(@PathVariable Long id,
                                             @org.springframework.web.bind.annotation.RequestParam(value = "reason", required = false, defaultValue = "") String reason,
                                             Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return userService.scheduleBlockUser(id, by, reason);
    }

    // POST /api/v1/user/{id}/undo-block
    @PostMapping("/{id}/undo-block")
    public RestWithStatusList undoBlock(@PathVariable Long id,
                                         Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return userService.undoBlockUser(id, by);
    }

    private RestWithStatusList forbidden() {
        return RestWithStatusList.builder()
                .status("FAILURE")
                .statusMsg("Access denied: only Bank Admin or Branch Admin can perform this action")
                .data(Collections.emptyList())
                .build();
    }
}
