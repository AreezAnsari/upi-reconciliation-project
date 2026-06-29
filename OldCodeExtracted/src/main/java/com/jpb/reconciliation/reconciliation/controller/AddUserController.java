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

    @GetMapping("/children")
    public RestWithStatusList getUsersByCreatorUsername(@RequestParam String createdBy) {
        return userService.getUsersByCreatorUsername(createdBy);
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

    // GET /api/v1/user/hierarchy — for authenticated user who created sub-users
    @GetMapping("/hierarchy")
    public RestWithStatusList getHierarchy(Authentication authentication) {
        if (!contextResolver.isAllowed(authentication)) return forbidden();
        return userService.getUserHierarchy(authentication);
    }

    // GET /api/v1/user/hierarchy/bank/{bankCode}
    @GetMapping("/hierarchy/bank/{bankCode}")
    public RestWithStatusList getHierarchyByBank(@PathVariable String bankCode) {
        return userService.getUserHierarchyByBankCode(bankCode);
    }

    // GET /api/v1/user/hierarchy/branch/{branchCode}
    @GetMapping("/hierarchy/branch/{branchCode}")
    public RestWithStatusList getHierarchyByBranch(@PathVariable String branchCode) {
        return userService.getUserHierarchyByBranchCode(branchCode);
    }

    // GET /api/v1/user/hierarchy/bank/{bankCode}/direct — bank-level users only (no branch), full recursive tree
    @GetMapping("/hierarchy/bank/{bankCode}/direct")
    public RestWithStatusList getHierarchyByBankDirect(@PathVariable String bankCode) {
        return userService.getUserHierarchyByBankDirect(bankCode);
    }

    // POST /api/v1/user/{id}/delegate — delegate user's work to a selected ancestor
    @PostMapping("/{id}/delegate")
    public RestWithStatusList delegateUser(@PathVariable Long id,
                                           @RequestParam Long delegateeId,
                                           @RequestParam(required = false, defaultValue = "") String reason,
                                           Authentication authentication) {
        String by = authentication != null ? authentication.getName() : "UNKNOWN";
        return userService.delegateUser(id, delegateeId, reason, by);
    }

    // GET /api/v1/user/{id}/ancestors — fetch ancestor chain (closest first) for delegation UI
    @GetMapping("/{id}/ancestors")
    public RestWithStatusList getUserAncestors(@PathVariable Long id) {
        return userService.getUserAncestors(id);
    }

    private RestWithStatusList forbidden() {
        return RestWithStatusList.builder()
                .status("FAILURE")
                .statusMsg("Access denied: only Bank Admin or Branch Admin can perform this action")
                .data(Collections.emptyList())
                .build();
    }
}
