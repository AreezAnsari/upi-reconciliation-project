/*
 * package com.jpb.reconciliation.reconciliation.controller;
 * 
 * import com.jpb.reconciliation.reconciliation.service.
 * PasswordandSecurityExpiryService; import
 * org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.http.ResponseEntity; import
 * org.springframework.web.bind.annotation.*;
 * 
 * import java.util.HashMap; import java.util.Map;
 * 
 * @RestController
 * 
 * @RequestMapping("/test/api/v1/dev")
 * 
 * @CrossOrigin(origins = "*") public class PasswordExpiryTestController {
 * 
 * @Autowired private PasswordandSecurityExpiryService expiryService;
 * 
 * // ✅ Trigger full scheduler logic manually
 * 
 * @GetMapping("/trigger-expiry-check") public ResponseEntity<Map<String,
 * Object>> triggerExpiryCheck() { Map<String, Object> res = new HashMap<>();
 * try { expiryService.runExpiryCheck(); res.put("success", true);
 * res.put("message",
 * "Expiry check executed successfully. Check backend logs and your email.");
 * return ResponseEntity.ok(res); } catch (Exception e) { res.put("success",
 * false); res.put("message", "Failed to execute expiry check: " +
 * e.getMessage()); return ResponseEntity.status(500).body(res); } }
 * 
 * // ✅ Test single user mail without DB dependency
 * 
 * @PostMapping("/test-expiry-mail") public ResponseEntity<Map<String, Object>>
 * testExpiryMail(@RequestBody Map<String, Object> body) { Map<String, Object>
 * res = new HashMap<>(); try { String email = (String) body.get("email");
 * String username = (String) body.get("username"); int daysUsed =
 * Integer.parseInt(body.get("daysUsed").toString());
 * 
 * expiryService.testSingleUserMail(email, username, daysUsed);
 * 
 * res.put("success", true); res.put("message",
 * "Test expiry mail triggered successfully."); return ResponseEntity.ok(res); }
 * catch (Exception e) { res.put("success", false); res.put("message",
 * "Failed to send test expiry mail: " + e.getMessage()); return
 * ResponseEntity.status(500).body(res); } } }
 */