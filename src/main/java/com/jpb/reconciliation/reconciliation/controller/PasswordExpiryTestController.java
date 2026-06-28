package com.jpb.reconciliation.reconciliation.controller;

import com.jpb.reconciliation.reconciliation.entity.BranchAdmin;
import com.jpb.reconciliation.reconciliation.entity.MainAdmin;
import com.jpb.reconciliation.reconciliation.entity.KalAdmin;
import com.jpb.reconciliation.reconciliation.repository.BranchAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.MainAdminRepository;
import com.jpb.reconciliation.reconciliation.repository.KalAdminRepository;
import com.jpb.reconciliation.reconciliation.service.PasswordandSecurityExpiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/test/api/v1/dev")
@CrossOrigin(origins = "*")
public class PasswordExpiryTestController {

    @Autowired private PasswordandSecurityExpiryService expiryService;
    @Autowired private BranchAdminRepository branchAdminRepository;
    @Autowired private MainAdminRepository   mainAdminRepository;
    @Autowired private KalAdminRepository    kalAdminRepository;

    // ✅ Trigger full scheduler
    @GetMapping("/trigger-expiry-check")
    public ResponseEntity<Map<String, Object>> triggerExpiryCheck() {
        Map<String, Object> res = new HashMap<>();
        try {
            expiryService.runExpiryCheck();
            res.put("success", true);
            res.put("message", "Expiry check executed. Check backend logs.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ✅ Single user mail test
    @PostMapping("/test-expiry-mail")
    public ResponseEntity<Map<String, Object>> testExpiryMail(
            @RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            String email    = (String) body.get("email");
            String username = (String) body.get("username");
            int daysUsed    = Integer.parseInt(body.get("daysUsed").toString());
            expiryService.testSingleUserMail(email, username, daysUsed);
            res.put("success", true);
            res.put("message", "Test mail triggered for days=" + daysUsed);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ✅ NEW — User ka status check karo
    @GetMapping("/check-status")
    public ResponseEntity<Map<String, Object>> checkUserStatus(
            @RequestParam String userType,
            @RequestParam String username,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String bankCode) {

        Map<String, Object> res = new HashMap<>();

        try {
            if ("BRANCH_ADMIN".equalsIgnoreCase(userType)) {

                Optional<BranchAdmin> opt = branchAdminRepository
                        .findByBranchCodeAndUsername(branchCode, username);

                if (!opt.isPresent()) {
                    res.put("success", false);
                    res.put("message", "Branch Admin not found");
                    return ResponseEntity.status(404).body(res);
                }

                BranchAdmin user = opt.get();
                res.put("success",          true);
                res.put("username",         user.getUsername());
                res.put("status",           user.getStatus());
                res.put("email",            user.getEmail());
                res.put("passwordUpdatedAt", user.getPasswordUpdatedAt() != null
                        ? user.getPasswordUpdatedAt().toString() : "NOT SET");
                res.put("createdAt",        user.getCreatedAt() != null
                        ? user.getCreatedAt().toString() : "NOT SET");

                // ✅ Days calculation
                if (user.getPasswordUpdatedAt() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(
                            user.getPasswordUpdatedAt().toLocalDate(),
                            java.time.LocalDate.now());
                    res.put("daysUsed",      days);
                    res.put("daysRemaining", Math.max(90 - days, 0));
                    res.put("willGetMail",   days >= 80 && !"BLOCKED".equalsIgnoreCase(user.getStatus())
                                             && !"INACTIVE".equalsIgnoreCase(user.getStatus()));
                }

            } else if ("SUPER_USER".equalsIgnoreCase(userType)) {

                Optional<MainAdmin> opt = mainAdminRepository
                        .findByBankCodeAndUsername(bankCode, username);

                if (!opt.isPresent()) {
                    res.put("success", false);
                    res.put("message", "Bank Admin not found");
                    return ResponseEntity.status(404).body(res);
                }

                MainAdmin user = opt.get();
                res.put("success",          true);
                res.put("username",         user.getUsername());
                res.put("status",           user.getStatus());
                res.put("email",            user.getEmail());
                res.put("passwordUpdatedAt", user.getPasswordUpdatedAt() != null
                        ? user.getPasswordUpdatedAt().toString() : "NOT SET");
                res.put("createdAt",        user.getCreatedAt() != null
                        ? user.getCreatedAt().toString() : "NOT SET");

                if (user.getPasswordUpdatedAt() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(
                            user.getPasswordUpdatedAt().toLocalDate(),
                            java.time.LocalDate.now());
                    res.put("daysUsed",      days);
                    res.put("daysRemaining", Math.max(90 - days, 0));
                    res.put("willGetMail",   days >= 80 && !"BLOCKED".equalsIgnoreCase(user.getStatus())
                                             && !"INACTIVE".equalsIgnoreCase(user.getStatus()));
                }

            } else {

                Optional<KalAdmin> opt = kalAdminRepository.findByUserName(username);

                if (!opt.isPresent()) {
                    res.put("success", false);
                    res.put("message", "Kal Admin not found");
                    return ResponseEntity.status(404).body(res);
                }

                KalAdmin user = opt.get();
                res.put("success",          true);
                res.put("username",         user.getUserName());
                res.put("status",           user.getUserStatus());
                res.put("email",            user.getEmailId());
                res.put("passwordUpdatedAt", user.getPasswordUpdatedAt() != null
                        ? user.getPasswordUpdatedAt().toString() : "NOT SET");

                if (user.getPasswordUpdatedAt() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(
                            user.getPasswordUpdatedAt().toLocalDate(),
                            java.time.LocalDate.now());
                    res.put("daysUsed",      days);
                    res.put("daysRemaining", Math.max(90 - days, 0));
                    res.put("willGetMail",   days >= 80 && !"BLOCKED".equalsIgnoreCase(user.getUserStatus())
                                             && !"INACTIVE".equalsIgnoreCase(user.getUserStatus()));
                }
            }

            return ResponseEntity.ok(res);

        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}