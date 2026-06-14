package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.dto.AddUserRequest;
import com.jpb.reconciliation.reconciliation.dto.RestWithStatusList;
import com.jpb.reconciliation.reconciliation.service.AddUserService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class AddUserController {

    private final AddUserService userService;

    @PostMapping("/create")
    public RestWithStatusList createUser(@Valid @RequestBody AddUserRequest request, Authentication authentication) {
        return userService.createUser(
                request,
                authentication.getName(),
                extractBankCode(authentication)
        );
    }

    @GetMapping("/{id}")
    public RestWithStatusList getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping
    public RestWithStatusList listUsers(Authentication authentication) {
        return userService.getUsersByBank(extractBankCode(authentication));
    }

    @PutMapping("/{id}")
    public RestWithStatusList updateUser(@PathVariable Long id, @Valid @RequestBody AddUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    public RestWithStatusList deactivateUser(@PathVariable Long id) {
        return userService.deactivateUser(id);
    }

    @GetMapping("/search")
    public RestWithStatusList search(@RequestParam("q") String term, Authentication authentication) {
        return userService.searchUsers(extractBankCode(authentication), term);
    }

    private String extractBankCode(Authentication authentication) {
        if (authentication.getDetails() instanceof String) {
            return (String) authentication.getDetails();
        }
        return "DEFAULT_BANK";
    }
}
